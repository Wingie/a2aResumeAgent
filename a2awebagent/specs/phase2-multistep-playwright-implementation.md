# Phase 2: User-Controlled Multi-Step Implementation Specification

## Project Status Overview

**Current Phase**: Phase 1 Complete → Phase 2 Ready  
**Last Updated**: 2025-07-18  
**Implementation Status**: Architecture Ready, Missing Queue Processor  

### Phase 1 Achievements ✅
- ✅ PostgreSQL tool caching system (60-75% faster startup)
- ✅ Connection pool monitoring and leak detection  
- ✅ Async+transactional anti-pattern fixes
- ✅ Complete evaluation entity framework
- ✅ Single-step Playwright action execution
- ✅ Comprehensive benchmarking infrastructure

### Phase 2 Goals 🎯
- 🔄 User-controlled execution parameters (maxSteps, executionMode)
- 🔄 Tool classification: One-shot vs Loopy vs Both
- 🔄 Multi-database state management (Neo4j + PostgreSQL + Redis)
- 🔄 Enhanced progress tracking with step-level granularity
- 🔄 Early completion detection and optimization

---

## Core Architecture Principles

### **User-Controlled Parameters (No AI Classification)**
- **User specifies**: maxSteps, executionMode directly
- **Tool decides**: One-shot vs Loopy execution based on user parameters
- **Early completion**: Agent returns when task complete, regardless of remaining steps
- **No AI inference**: System doesn't guess user intent - user controls explicitly

### **Data Storage Strategy**
- **Neo4j**: State and step tracking, workflow patterns, step relationships
- **PostgreSQL**: Evaluation results, final scores, model comparisons  
- **Redis**: High-level progress tracking, real-time updates, cancellation signals

### **Two Distinct Flows**
1. **MCP Tool Flow**: External clients use tools directly via MCP protocol
2. **Evaluation System**: Internal system tests AI models using those same tools

---

## Tool Classification Framework

### **One-Shot Tools** (Direct Response)
- **Duration**: < 30 seconds expected
- **Complexity**: Single operation or simple sequence
- **Response**: Immediate synchronous return via MCP protocol
- **Storage**: Redis only (optional caching)
- **Examples**: Static data retrieval, single screenshots, template generation

### **Loopy Tools** (Queued Processing)
- **Duration**: > 30 seconds or multi-step workflows
- **Complexity**: Multiple operations with state management
- **Response**: Task ID + async progress monitoring
- **Storage**: Neo4j (state/steps) + PostgreSQL (results) + Redis (progress)
- **Examples**: Multi-step booking workflows, complex research tasks

### **Both Tools** (User Choice)
- **Parameter-driven**: User specifies maxSteps to control execution
- **Adaptive**: Tool detects completion early and returns
- **Flexible**: Same tool endpoint, different execution paths

## Current Tool Classification

### **One-Shot Only**
```java
// Always fast, direct response
- generateMeme                    // Static image generation
- generateMoodMeme               // Template-based generation  
- getMoodGuide                   // Static template data
- getWingstonsProjectsExpertiseResume // Static profile data
```

### **Both (User Controlled)**
```java
// User specifies maxSteps parameter
- browseWebAndReturnText         // maxSteps: 1=one-shot, >1=loopy
- browseWebAndReturnImage        // maxSteps: 1=one-shot, >1=loopy
- browseWebAndReturnImageUrl     // maxSteps: 1=one-shot, >1=loopy
- searchLinkedInProfile          // maxSteps: 1=one-shot, >1=loopy
- askTasteBeforeYouWaste         // maxSteps: 1=one-shot, >1=loopy
- getTasteBeforeYouWasteScreenshot // maxSteps: 1=one-shot, >1=loopy
```

---

## Implementation Architecture

### **Enhanced MCP Action Pattern**

```java
@Action(description = "browse web and return text", name = "browseWebAndReturnText")
public Object browseWebAndReturnText(
    @Parameter(description = "web browsing instructions") String instructions,
    @Parameter(description = "max steps (1=one-shot, >1=loopy)", required = false) Integer maxSteps,
    @Parameter(description = "execution mode: AUTO, CONSERVATIVE, AGGRESSIVE", required = false) String executionMode
) {
    // Default to one-shot if not specified
    int steps = maxSteps != null ? maxSteps : 1;
    
    if (steps == 1) {
        // One-shot execution - immediate response
        return executeOneShot(instructions);
    } else {
        // Loopy execution - queue and return task ID
        return executeLoopy(instructions, steps, executionMode);
    }
}
```

### **One-Shot Execution Flow**
```java
private String executeOneShot(String instructions) {
    // Execute immediately in current thread
    String result = playwrightService.executeSingle(instructions);
    
    // Optional: Cache in Redis for performance
    redisTemplate.opsForValue().set("oneshot:" + taskId, result, Duration.ofMinutes(5));
    
    return result; // Direct string response
}
```

### **Loopy Execution Flow**
```java
private TaskResponse executeLoopy(String instructions, int maxSteps, String mode) {
    String taskId = UUID.randomUUID().toString();
    
    // 1. Create workflow execution in Neo4j
    WorkflowExecution workflow = WorkflowExecution.builder()
        .taskId(taskId)
        .instructions(instructions)
        .maxSteps(maxSteps)
        .executionMode(mode)
        .status(WorkflowStatus.QUEUED)
        .startTime(LocalDateTime.now())
        .build();
    neo4jService.saveWorkflowExecution(workflow);
    
    // 2. Track high-level progress in Redis
    TaskProgress progress = TaskProgress.builder()
        .taskId(taskId)
        .status("QUEUED")
        .currentStep(0)
        .totalSteps(maxSteps)
        .progressPercentage(0.0)
        .canCancel(true)
        .build();
    redisTemplate.opsForValue().set("task:progress:" + taskId, progress);
    
    // 3. Queue for async processing
    taskExecutorService.executeWorkflowAsync(taskId, instructions, maxSteps, mode);
    
    return TaskResponse.builder()
        .taskId(taskId)
        .status("QUEUED")
        .progressUrl("/api/tasks/" + taskId + "/progress")
        .estimatedDuration(estimateDuration(instructions, maxSteps))
        .build();
}
```

---

## Data Storage Implementation

### **Neo4j: Workflow State and Step Tracking**

```java
@Node
public class WorkflowExecution {
    @Id 
    private String taskId;
    private String instructions;
    private int maxSteps;
    private int currentStep;
    private String executionMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private WorkflowStatus status;
    private boolean earlyCompletion;
    
    @Relationship(type = "HAS_STEP", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowStep> steps;
    
    @Relationship(type = "HAS_STATE", direction = Relationship.Direction.OUTGOING)
    private List<StateTransition> stateTransitions;
    
    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.UNDIRECTED)
    private List<WorkflowExecution> similarWorkflows;
}

@Node
public class WorkflowStep {
    @Id 
    private String stepId;
    private String stepDescription;
    private int stepNumber;
    private StepStatus status;
    private String stepResult;
    private LocalDateTime stepStartTime;
    private LocalDateTime stepEndTime;
    private Double confidenceScore;
    private String screenshotPath;
    
    // Browser state at this step
    private Map<String, Object> browserState;
    private String currentUrl;
    private String pageTitle;
    private List<String> availableActions;
}

@Node
public class StateTransition {
    @Id
    private String transitionId;
    private String fromState;
    private String toState;
    private String trigger;
    private LocalDateTime transitionTime;
    private Map<String, Object> context;
}
```

### **PostgreSQL: Evaluation Results and Final Scores**

```java
@Entity
@Table(name = "evaluation_tasks")
public class EvaluationTask {
    @Id
    private String taskId;
    
    @Column(name = "evaluation_id")
    private String evaluationId;
    
    // Final results only - not step-by-step details
    @Enumerated(EnumType.STRING)
    private TaskStatus finalStatus;
    
    @Column(name = "final_score")
    private Double finalScore;
    
    @Column(name = "steps_completed")
    private Integer stepsCompleted;
    
    @Column(name = "max_steps")
    private Integer maxSteps;
    
    @Column(name = "execution_mode")
    private String executionMode;
    
    @Column(name = "early_completion")
    private Boolean earlyCompletion;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Summary results, not detailed step tracking
    @Column(name = "execution_summary", columnDefinition = "TEXT")
    private String executionSummary;
    
    @JdbcTypeCode(java.sql.Types.OTHER)
    @Column(name = "performance_metrics", columnDefinition = "jsonb")
    private Map<String, Object> performanceMetrics;
    
    // Calculated efficiency metrics
    public Double getExecutionEfficiency() {
        if (maxSteps == null || stepsCompleted == null) return null;
        return (double) stepsCompleted / maxSteps;
    }
    
    public String getExecutionSummary() {
        return String.format("Completed %d/%d steps (%s mode) - %s", 
            stepsCompleted, maxSteps, executionMode, 
            earlyCompletion ? "Early completion" : "Full execution");
    }
}
```

### **Redis: Real-Time Progress Tracking**

```java
public class TaskProgress {
    private String taskId;
    private String status; // QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    private int currentStep;
    private int totalSteps;
    private double progressPercentage;
    private String currentStepDescription;
    private LocalDateTime lastUpdate;
    private boolean canCancel;
    private Long estimatedTimeRemaining; // milliseconds
    private String executionMode;
    
    // Real-time update methods
    public void updateProgress(int step, String description) {
        this.currentStep = step;
        this.currentStepDescription = description;
        this.progressPercentage = (double) step / totalSteps * 100;
        this.lastUpdate = LocalDateTime.now();
    }
    
    public void updateEstimatedTime(long remainingMs) {
        this.estimatedTimeRemaining = remainingMs;
    }
}
```

---

## User Examples and API Usage

### **One-Shot Usage**
```bash
# Default one-shot (maxSteps not specified)
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "browseWebAndReturnText", 
  "arguments": {"instructions": "Get the title of LinkedIn profile at https://linkedin.com/in/johndoe"}
}'

# Response: "John Doe - Senior Software Engineer at Google"

# Explicit one-shot
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "browseWebAndReturnText",
  "arguments": {
    "instructions": "Get LinkedIn profile title", 
    "maxSteps": 1
  }
}'
```

### **Loopy Usage**
```bash
# Multi-step workflow
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "browseWebAndReturnText",
  "arguments": {
    "instructions": "Book flight from NYC to Paris with travel insurance and seat selection", 
    "maxSteps": 10,
    "executionMode": "AUTO"
  }
}'

# Response: 
{
  "taskId": "abc123-def456-ghi789",
  "status": "QUEUED",
  "progressUrl": "/api/tasks/abc123-def456-ghi789/progress",
  "estimatedDuration": 420000
}

# Check progress
curl -N http://localhost:7860/api/tasks/abc123-def456-ghi789/progress

# SSE Stream:
data: {"taskId":"abc123","status":"RUNNING","currentStep":2,"totalSteps":10,"progressPercentage":20.0,"currentStepDescription":"Selecting flight options","canCancel":true}
```

### **Real-World Examples**

#### **Travel Search (10 steps max)**
```bash
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "browseWebAndReturnText",
  "arguments": {
    "instructions": "Search for flights from NYC to Paris on booking.com, find best price under $800, check baggage policies",
    "maxSteps": 10,
    "executionMode": "AUTO"
  }
}'
```

#### **LinkedIn Research (5 steps max)**
```bash
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "searchLinkedInProfile",
  "arguments": {
    "instructions": "Find software engineers at Google in NYC, extract their experience and skills",
    "maxSteps": 5,
    "executionMode": "CONSERVATIVE"
  }
}'
```

#### **One-Shot Profile Check**
```bash
curl -X POST http://localhost:7860/v1/tools/call -d '{
  "name": "searchLinkedInProfile",
  "arguments": {
    "instructions": "Get basic info for https://linkedin.com/in/johndoe",
    "maxSteps": 1
  }
}'
```

---

## Step-by-Step Execution Process

### **1. Request Classification and Routing**
```java
public Object routeRequest(String instructions, Integer maxSteps, String executionMode) {
    // User-controlled routing - no AI classification
    int steps = maxSteps != null ? maxSteps : 1;
    
    if (steps == 1) {
        // One-shot: Execute immediately
        return executeOneShot(instructions);
    } else {
        // Loopy: Queue and track
        return executeLoopy(instructions, steps, executionMode);
    }
}
```

### **2. Loopy Execution with Multi-Database Tracking**
```java
private void executeWorkflowSteps(String taskId, String instructions, int maxSteps, String mode) {
    // Load workflow from Neo4j
    WorkflowExecution workflow = neo4jService.findWorkflowExecution(taskId);
    
    // Update status to RUNNING
    workflow.setStatus(WorkflowStatus.RUNNING);
    updateRedisProgress(taskId, "RUNNING", 0, maxSteps);
    
    for (int step = 1; step <= maxSteps; step++) {
        try {
            // Execute single step
            StepResult result = playwrightService.executeStep(instructions, step, workflow.getBrowserState());
            
            // Create step node in Neo4j
            WorkflowStep stepNode = WorkflowStep.builder()
                .stepId(UUID.randomUUID().toString())
                .stepNumber(step)
                .stepDescription(result.getDescription())
                .status(StepStatus.COMPLETED)
                .stepResult(result.getResult())
                .confidenceScore(result.getConfidenceScore())
                .screenshotPath(result.getScreenshotPath())
                .browserState(result.getBrowserState())
                .build();
            
            // Save step to Neo4j
            neo4jService.addStepToWorkflow(taskId, stepNode);
            
            // Update Redis progress
            updateRedisProgress(taskId, "RUNNING", step, maxSteps, result.getDescription());
            
            // Check for early completion
            if (result.isTaskComplete()) {
                workflow.setEarlyCompletion(true);
                break;
            }
            
        } catch (Exception e) {
            handleStepFailure(taskId, step, e);
            break;
        }
    }
    
    // Complete workflow
    completeWorkflow(taskId, workflow);
}
```

### **3. Early Completion Detection**
```java
private boolean checkEarlyCompletion(StepResult result, String instructions) {
    // Confidence-based completion
    if (result.getConfidenceScore() >= 0.95) {
        return true;
    }
    
    // Result quality assessment
    if (result.hasAllRequiredData() && result.getDataQuality() >= 0.9) {
        return true;
    }
    
    // Task-specific completion criteria
    return taskCompletionAnalyzer.isTaskComplete(result, instructions);
}
```

### **4. Multi-Database Completion**
```java
private void completeWorkflow(String taskId, WorkflowExecution workflow) {
    // 1. Update Neo4j workflow status
    workflow.setStatus(WorkflowStatus.COMPLETED);
    workflow.setEndTime(LocalDateTime.now());
    neo4jService.saveWorkflowExecution(workflow);
    
    // 2. Update Redis final status
    updateRedisProgress(taskId, "COMPLETED", workflow.getCurrentStep(), workflow.getMaxSteps());
    
    // 3. Save evaluation results to PostgreSQL (if evaluation)
    if (workflow.isEvaluationTask()) {
        EvaluationTask evalTask = EvaluationTask.builder()
            .taskId(taskId)
            .finalStatus(TaskStatus.COMPLETED)
            .stepsCompleted(workflow.getCurrentStep())
            .maxSteps(workflow.getMaxSteps())
            .executionMode(workflow.getExecutionMode())
            .earlyCompletion(workflow.isEarlyCompletion())
            .completedAt(workflow.getEndTime())
            .finalScore(calculateFinalScore(workflow))
            .build();
        
        evaluationTaskRepository.save(evalTask);
    }
    
    // 4. Cleanup Redis temporary data (after delay)
    scheduleRedisCleanup(taskId, Duration.ofHours(1));
}
```

---

## Implementation Roadmap

### **Phase 2.1: Core Infrastructure (Week 1-2)**

#### **Priority 1: Fix Queue Processor (IMMEDIATE)**
```java
// Add to ModelEvaluationService.java
@Scheduled(fixedRate = 60000) // Every 1 minute
@Transactional(value = "primaryTransactionManager")
public void processQueuedEvaluations() {
    try {
        List<ModelEvaluation> queuedEvaluations = 
            evaluationRepository.findByStatus(EvaluationStatus.QUEUED);
        
        for (ModelEvaluation evaluation : queuedEvaluations) {
            if (!runningEvaluations.containsKey(evaluation.getEvaluationId())) {
                log.info("Starting queued evaluation: {}", evaluation.getEvaluationId());
                CompletableFuture<Void> future = executeEvaluationAsync(evaluation);
                runningEvaluations.put(evaluation.getEvaluationId(), future);
            }
        }
    } catch (Exception e) {
        log.error("Error processing queued evaluations", e);
    }
}
```

#### **Priority 2: Enhance MCP Actions with User Parameters**
```java
// Modify existing PlaywrightWebBrowsingAction
@Action(description = "browse web and return text", name = "browseWebAndReturnText")
public Object browseWebAndReturnText(
    @Parameter(description = "instructions") String instructions,
    @Parameter(description = "max steps (1=one-shot, >1=loopy)", required = false) Integer maxSteps,
    @Parameter(description = "execution mode", required = false) String executionMode
) {
    return routeExecution(instructions, maxSteps, executionMode);
}
```

#### **Priority 3: Neo4j Workflow Entities**
```java
// Create Neo4j entities for workflow tracking
@Repository
public interface WorkflowExecutionRepository extends Neo4jRepository<WorkflowExecution, String> {
    
    @Query("MATCH (w:WorkflowExecution {taskId: $taskId}) RETURN w")
    Optional<WorkflowExecution> findByTaskId(String taskId);
    
    @Query("MATCH (w:WorkflowExecution)-[:HAS_STEP]->(s:WorkflowStep) WHERE w.taskId = $taskId RETURN s ORDER BY s.stepNumber")
    List<WorkflowStep> findStepsByTaskId(String taskId);
    
    @Query("MATCH (w:WorkflowExecution) WHERE w.status = $status RETURN w")
    List<WorkflowExecution> findByStatus(WorkflowStatus status);
}
```

### **Phase 2.2: Enhanced Execution Engine (Week 2-3)**

#### **Step-by-Step Execution Service**
```java
@Service
public class StepExecutionService {
    
    public StepResult executeStep(String instructions, int stepNumber, Map<String, Object> browserState) {
        // Initialize browser with previous state
        Page page = initializeBrowserWithState(browserState);
        
        try {
            // Execute step-specific logic
            String result = executeStepLogic(page, instructions, stepNumber);
            
            // Capture screenshot with step number
            String screenshotPath = captureStepScreenshot(page, stepNumber);
            
            // Assess completion confidence
            double confidence = assessCompletionConfidence(result, instructions);
            
            return StepResult.builder()
                .stepNumber(stepNumber)
                .result(result)
                .confidenceScore(confidence)
                .screenshotPath(screenshotPath)
                .browserState(captureBrowserState(page))
                .isTaskComplete(confidence >= 0.95)
                .build();
                
        } finally {
            // Clean up browser resources
            cleanupBrowser(page);
        }
    }
}
```

#### **Real-Time Progress Service**
```java
@Service
public class ProgressTrackingService {
    
    public void updateProgress(String taskId, String status, int currentStep, int totalSteps, String description) {
        // Update Redis
        TaskProgress progress = TaskProgress.builder()
            .taskId(taskId)
            .status(status)
            .currentStep(currentStep)
            .totalSteps(totalSteps)
            .progressPercentage((double) currentStep / totalSteps * 100)
            .currentStepDescription(description)
            .lastUpdate(LocalDateTime.now())
            .build();
            
        redisTemplate.opsForValue().set("task:progress:" + taskId, progress);
        
        // Broadcast SSE update
        sseService.broadcastProgress(taskId, progress);
    }
}
```

### **Phase 2.3: Database Integration (Week 3-4)**

#### **Neo4j Analytics Queries**
```java
@Service
public class WorkflowAnalyticsService {
    
    // Find similar workflows for optimization
    @Query("MATCH (w:WorkflowExecution) WHERE w.instructions CONTAINS $keyword RETURN w ORDER BY w.successRate DESC LIMIT 5")
    public List<WorkflowExecution> findSimilarWorkflows(String keyword);
    
    // Analyze step success patterns
    @Query("MATCH (w:WorkflowExecution)-[:HAS_STEP]->(s:WorkflowStep) WHERE s.status = 'COMPLETED' RETURN s.stepNumber, COUNT(s) as successCount ORDER BY s.stepNumber")
    public List<StepSuccessStats> getStepSuccessStats();
    
    // Find common failure points
    @Query("MATCH (w:WorkflowExecution)-[:HAS_STEP]->(s:WorkflowStep) WHERE s.status = 'FAILED' RETURN s.stepNumber, s.stepDescription, COUNT(s) as failureCount ORDER BY failureCount DESC")
    public List<StepFailureStats> getCommonFailurePoints();
}
```

#### **PostgreSQL Evaluation Analytics**
```java
@Repository
public interface EvaluationTaskRepository extends JpaRepository<EvaluationTask, String> {
    
    @Query("SELECT e FROM EvaluationTask e WHERE e.executionMode = :mode AND e.finalStatus = 'COMPLETED'")
    List<EvaluationTask> findCompletedTasksByMode(@Param("mode") String executionMode);
    
    @Query("SELECT AVG(e.finalScore) FROM EvaluationTask e WHERE e.maxSteps = :maxSteps")
    Double getAverageScoreByMaxSteps(@Param("maxSteps") Integer maxSteps);
    
    @Query("SELECT e.executionMode, AVG(e.stepsCompleted), COUNT(e) FROM EvaluationTask e WHERE e.earlyCompletion = true GROUP BY e.executionMode")
    List<Object[]> getEarlyCompletionStatsByMode();
}
```

### **Phase 2.4: Testing and Optimization (Week 4-5)**

#### **Integration Testing**
```java
@SpringBootTest
@Testcontainers
public class MultiStepWorkflowIntegrationTest {
    
    @Test
    public void testOneShotExecution() {
        // Test one-shot execution
        String result = (String) browseWebAction.browseWebAndReturnText(
            "Get LinkedIn profile title", 1, "AUTO");
        
        assertThat(result).isNotNull();
        assertThat(result).contains("Software Engineer");
    }
    
    @Test
    public void testLoopyExecution() {
        // Test loopy execution
        TaskResponse response = (TaskResponse) browseWebAction.browseWebAndReturnText(
            "Book flight NYC to Paris", 10, "AUTO");
        
        assertThat(response.getTaskId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(response.getProgressUrl()).contains("/api/tasks/");
    }
    
    @Test
    public void testEarlyCompletion() {
        // Test early completion detection
        TaskResponse response = (TaskResponse) browseWebAction.browseWebAndReturnText(
            "Simple task that should complete early", 10, "AUTO");
        
        // Wait for completion
        await().atMost(Duration.ofMinutes(2))
            .until(() -> getTaskStatus(response.getTaskId()).equals("COMPLETED"));
        
        // Verify early completion
        WorkflowExecution workflow = neo4jService.findWorkflowExecution(response.getTaskId());
        assertThat(workflow.isEarlyCompletion()).isTrue();
        assertThat(workflow.getCurrentStep()).isLessThan(10);
    }
}
```

---

## Success Metrics and Evaluation

### **Technical Performance Metrics**
- **One-Shot Response Time**: < 5 seconds average
- **Loopy Task Initialization**: < 2 seconds to return task ID
- **Step Execution Speed**: < 10 seconds per step average
- **Early Completion Rate**: > 60% for over-specified tasks
- **Database Query Performance**: < 100ms for progress updates

### **User Experience Metrics**
- **Parameter Acceptance Rate**: > 95% valid parameter combinations
- **Task Completion Rate**: > 90% for reasonable task/step combinations
- **Progress Accuracy**: < 10% deviation from actual progress
- **Error Recovery Rate**: > 80% graceful handling of step failures

### **Business Value Metrics**
- **User Control Satisfaction**: Users can specify execution parameters
- **Resource Efficiency**: Early completion reduces unnecessary computation
- **Scalability**: System handles 50+ concurrent loopy tasks
- **Analytics Value**: Rich data for workflow optimization

---

## Quality Gates

### **Phase 2.1 (Infrastructure) - Ready for Phase 2.2**
- [ ] **CRITICAL**: Queue processor fixes evaluation execution (100% success rate)
- [ ] Enhanced MCP actions accept user parameters (maxSteps, executionMode)
- [ ] Neo4j workflow entities created and tested
- [ ] Redis progress tracking working for both one-shot and loopy

### **Phase 2.2 (Execution Engine) - Ready for Phase 2.3**
- [ ] Step-by-step execution service working reliably
- [ ] Early completion detection working (>60% detection rate)
- [ ] Screenshot capture numbered by step
- [ ] Browser state preservation between steps

### **Phase 2.3 (Database Integration) - Ready for Phase 2.4**
- [ ] Neo4j workflow analytics queries working
- [ ] PostgreSQL evaluation results properly stored
- [ ] Redis cleanup and maintenance working
- [ ] Cross-database consistency maintained

### **Phase 2.4 (Testing & Optimization) - Production Ready**
- [ ] All integration tests passing
- [ ] Performance targets met for all execution modes
- [ ] Error handling comprehensive and graceful
- [ ] Documentation complete for all new features

---

## Implementation Checklist

### **Immediate Actions (Week 1)**
- [ ] **CRITICAL**: Add `processQueuedEvaluations()` scheduled method
- [ ] Enhance existing MCP actions with user parameters
- [ ] Create Neo4j workflow entities and repositories
- [ ] Add Redis progress tracking for loopy execution

### **Short Term (Week 2-3)**
- [ ] Implement step-by-step execution service
- [ ] Add early completion detection logic
- [ ] Create real-time progress tracking service
- [ ] Implement screenshot capture with step numbering

### **Medium Term (Week 3-4)**
- [ ] Build Neo4j analytics queries for workflow optimization
- [ ] Enhance PostgreSQL evaluation results storage
- [ ] Add Redis cleanup and maintenance
- [ ] Create comprehensive error handling

### **Long Term (Week 4-5)**
- [ ] Complete integration testing suite
- [ ] Performance optimization and load testing
- [ ] Documentation and API reference
- [ ] User acceptance testing and feedback integration

---

## Risk Mitigation

### **Technical Risks**
1. **Multi-Database Consistency**: Implement transaction boundaries and eventual consistency patterns
2. **Performance Degradation**: Use async processing and optimize database queries
3. **Memory Usage**: Implement cleanup schedules and resource pooling
4. **Browser State Complexity**: Use incremental state capture and validation

### **User Experience Risks**
1. **Parameter Confusion**: Provide clear documentation and validation
2. **Progress Tracking Lag**: Ensure real-time updates with minimal delay
3. **Early Completion Accuracy**: Implement multiple confidence signals
4. **Error Message Clarity**: Provide actionable error messages and recovery suggestions

### **Implementation Risks**
1. **Timeline Pressure**: Prioritize critical path features and defer nice-to-have items
2. **Integration Complexity**: Maintain extensive backward compatibility testing
3. **Performance Bottlenecks**: Implement comprehensive monitoring and alerting
4. **Database Migration**: Plan careful schema updates with rollback capabilities

---

This specification provides a comprehensive roadmap for implementing Phase 2 user-controlled multi-step Playwright workflows with proper tool classification and multi-database state management, while maintaining the existing system's robustness and performance characteristics.