# Tutorial 8: Human-in-the-Loop Integration

## Overview

Human-in-the-Loop (HitL) integration is critical for building trustworthy AI automation systems. This tutorial demonstrates how to implement comprehensive HitL workflows using the a2acore framework with Spring Boot 3.2.4, Playwright 1.51.0, and PostgreSQL for state management. We'll cover approval systems, decision checkpoints, escalation patterns, and real-time notifications in the context of travel research automation.

The a2acore framework provides native support for human oversight through annotations, workflow state management, and integration with Spring Security for authorization controls.

## 1. Understanding Human-in-the-Loop Architecture

### 1.1 Core Components

The HitL system consists of several interconnected components:

```java
// Core interfaces for human interaction
HumanInLoop - Main interface for human validation
ExplainDecision - AI decision explanation interface
ApprovalWorkflow - State machine for approval processes
NotificationService - Real-time user notifications
AuditLogger - Comprehensive action tracking
```

### 1.2 Integration Points

Human oversight integrates at multiple levels:
- **Action Level**: Individual @Action methods requiring approval
- **Workflow Level**: Multi-step processes with checkpoints
- **System Level**: Emergency stops and escalation protocols
- **Audit Level**: Comprehensive logging and compliance tracking

## 2. Basic Human-in-the-Loop Setup

### 2.1 Enable HitL in Spring Boot Configuration

First, configure the HitL system in your Spring Boot application:

```java
package io.wingie.config;

import io.wingie.a2acore.annotation.EnableA2ACore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableA2ACore
@EnableAsync
@EnableWebSecurity
public class HumanInLoopConfig {
    
    @Bean
    public HumanInLoopService humanInLoopService() {
        return new SpringBootHumanInLoopService();
    }
    
    @Bean
    public ApprovalWorkflowEngine approvalWorkflowEngine() {
        return new PostgresApprovalWorkflowEngine();
    }
    
    @Bean
    public NotificationService notificationService() {
        return new WebSocketNotificationService();
    }
}
```

### 2.2 Database Schema for Workflow State

Create the necessary database tables for tracking approval workflows:

```sql
-- Approval workflows table
CREATE TABLE approval_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id VARCHAR(255) UNIQUE NOT NULL,
    action_method VARCHAR(255) NOT NULL,
    action_parameters JSONB NOT NULL,
    requester_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    approval_context JSONB
);

-- Individual approval decisions
CREATE TABLE approval_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id VARCHAR(255) REFERENCES approval_workflows(workflow_id),
    approver_id VARCHAR(255) NOT NULL,
    decision VARCHAR(20) NOT NULL, -- APPROVE, REJECT, DELEGATE
    decision_reason TEXT,
    decision_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    additional_context JSONB
);

-- Notification tracking
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    workflow_id VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);
```

## 3. Implementing Actions with Human Approval

### 3.1 Low-Risk Actions (Automatic Execution)

For low-risk travel research actions, no human approval is required:

```java
package io.wingie.service;

import io.wingie.a2acore.annotation.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "travel-research", description = "Safe travel research operations")
public class TravelResearchService {
    
    @Action(description = "Search for flight information without booking")
    public String searchFlights(
        @Parameter(description = "Origin city") String origin,
        @Parameter(description = "Destination city") String destination,
        @Parameter(description = "Travel date") String travelDate) {
        
        // Low-risk: read-only flight search
        try {
            FlightSearchResult result = flightSearchEngine.search(origin, destination, travelDate);
            auditLogger.logFlightSearch(getCurrentUser(), origin, destination, travelDate);
            
            return String.format("Found %d flights from %s to %s on %s. " +
                "Price range: %s - %s", 
                result.getFlightCount(), origin, destination, travelDate,
                result.getMinPrice(), result.getMaxPrice());
                
        } catch (Exception e) {
            log.error("Flight search failed", e);
            return "Error searching flights: " + e.getMessage();
        }
    }
}
```

### 3.2 Medium-Risk Actions (Supervisor Approval)

Medium-risk actions require supervisor approval before execution:

```java
@Service
@Agent(name = "booking-agent", description = "Travel booking operations requiring approval")
public class TravelBookingService {
    
    @Autowired
    private HumanInLoopService humanInLoopService;
    
    @Action(description = "Hold flight reservation temporarily")
    @PreAuthorize("hasRole('BOOKING_AGENT')")
    public String holdFlightReservation(
        @Parameter(description = "Flight reservation details") FlightReservationRequest request) {
        
        // Medium-risk: Creates temporary reservation, requires approval
        try {
            // Check if human approval is required
            ApprovalContext approvalContext = ApprovalContext.builder()
                .actionMethod("holdFlightReservation")
                .requesterId(getCurrentUserId())
                .actionParameters(Map.of("request", request))
                .riskLevel("MEDIUM")
                .estimatedCost(request.getTotalCost())
                .build();
            
            if (requiresApproval(approvalContext)) {
                // Submit for human approval
                String workflowId = humanInLoopService.submitForApproval(
                    "Flight Reservation Hold Request",
                    String.format("Hold reservation for %s to %s on %s. Cost: %s",
                        request.getOrigin(), request.getDestination(), 
                        request.getTravelDate(), request.getTotalCost()),
                    approvalContext
                );
                
                auditLogger.logApprovalRequest(getCurrentUserId(), "holdFlightReservation", workflowId);
                
                return String.format("Reservation hold request submitted for approval. " +
                    "Workflow ID: %s. Please check approval status via dashboard.", workflowId);
            } else {
                // Direct execution for authorized users
                return executeFlightReservationHold(request);
            }
            
        } catch (Exception e) {
            log.error("Flight reservation hold failed", e);
            auditLogger.logActionFailure(getCurrentUserId(), "holdFlightReservation", e);
            return "Error holding reservation: " + e.getMessage();
        }
    }
    
    private boolean requiresApproval(ApprovalContext context) {
        // Business logic for approval requirements
        return context.getEstimatedCost() > 500.0 || 
               !userService.hasAuthorization(context.getRequesterId(), "AUTO_HOLD");
    }
}
```

### 3.3 High-Risk Actions (Multi-Level Approval)

High-risk actions require multiple approvals and enhanced security:

```java
@Action(description = "Process payment and confirm booking")
@PreAuthorize("hasRole('BOOKING_AGENT') and hasPermission(#paymentRequest, 'PROCESS_PAYMENT')")
public String processPaymentAndConfirmBooking(
    @Parameter(description = "Payment and booking confirmation details") PaymentBookingRequest paymentRequest) {
    
    try {
        // Enhanced approval context for high-risk actions
        ApprovalContext approvalContext = ApprovalContext.builder()
            .actionMethod("processPaymentAndConfirmBooking")
            .requesterId(getCurrentUserId())
            .actionParameters(Map.of("paymentRequest", paymentRequest))
            .riskLevel("HIGH")
            .estimatedCost(paymentRequest.getTotalAmount())
            .paymentMethod(paymentRequest.getPaymentMethod())
            .customerInfo(paymentRequest.getCustomerInfo())
            .build();
        
        // Mandatory approval for high-risk financial transactions
        if (paymentRequest.getTotalAmount() > 1000.0) {
            String workflowId = humanInLoopService.submitForMultiLevelApproval(
                "High-Value Payment Processing",
                String.format("Process payment of %s for booking %s. " +
                    "Customer: %s, Payment method: %s",
                    paymentRequest.getTotalAmount(),
                    paymentRequest.getBookingReference(),
                    paymentRequest.getCustomerInfo().getName(),
                    paymentRequest.getPaymentMethod()),
                approvalContext,
                List.of(
                    ApprovalLevel.SUPERVISOR,
                    ApprovalLevel.FINANCIAL_CONTROLLER
                )
            );
            
            // Log high-risk approval request
            auditLogger.logHighRiskApprovalRequest(getCurrentUserId(), "processPaymentAndConfirmBooking", 
                workflowId, paymentRequest.getTotalAmount());
            
            // Send notifications to required approvers
            notificationService.notifyApprovers(workflowId, approvalContext);
            
            return String.format("High-value payment requires multi-level approval. " +
                "Workflow ID: %s. Supervisor and Financial Controller approval required.", workflowId);
        } else {
            // Standard approval process for medium amounts
            return processStandardPaymentApproval(paymentRequest, approvalContext);
        }
        
    } catch (Exception e) {
        log.error("Payment processing approval failed", e);
        auditLogger.logCriticalActionFailure(getCurrentUserId(), "processPaymentAndConfirmBooking", e);
        return "Error processing payment: " + e.getMessage();
    }
}
```

## 4. Real-Time Approval Dashboard

### 4.1 Approval Dashboard Controller

Create a REST controller for the approval dashboard:

```java
package io.wingie.controller;

import io.wingie.service.ApprovalWorkflowService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1/approvals")
@PreAuthorize("hasRole('SUPERVISOR') or hasRole('MANAGER')")
public class ApprovalDashboardController {
    
    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;
    
    @GetMapping("/pending")
    public ResponseEntity<List<PendingApprovalSummary>> getPendingApprovals() {
        List<PendingApprovalSummary> pendingApprovals = 
            approvalWorkflowService.getPendingApprovalsForUser(getCurrentUserId());
        return ResponseEntity.ok(pendingApprovals);
    }
    
    @GetMapping("/{workflowId}")
    public ResponseEntity<ApprovalWorkflowDetails> getApprovalDetails(@PathVariable String workflowId) {
        ApprovalWorkflowDetails details = approvalWorkflowService.getWorkflowDetails(workflowId);
        return ResponseEntity.ok(details);
    }
    
    @PostMapping("/{workflowId}/approve")
    public ResponseEntity<String> approveAction(
        @PathVariable String workflowId,
        @RequestBody ApprovalDecisionRequest decision) {
        
        String result = approvalWorkflowService.processApproval(workflowId, 
            getCurrentUserId(), decision);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{workflowId}/reject")
    public ResponseEntity<String> rejectAction(
        @PathVariable String workflowId,
        @RequestBody ApprovalDecisionRequest decision) {
        
        String result = approvalWorkflowService.processRejection(workflowId, 
            getCurrentUserId(), decision);
        return ResponseEntity.ok(result);
    }
}
```

### 4.2 WebSocket Real-Time Updates

Implement WebSocket support for real-time approval notifications:

```java
package io.wingie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ApprovalNotificationHandler(), "/ws/approvals")
                .setAllowedOrigins("*");
    }
}

@Component
public class ApprovalNotificationHandler extends TextWebSocketHandler {
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connection established: {}", session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }
    
    public void broadcastApprovalNotification(ApprovalNotification notification) {
        String message = objectMapper.writeValueAsString(notification);
        sessions.parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send WebSocket message", e);
                }
            });
    }
}
```

## 5. Travel-Specific Human-in-the-Loop Scenarios

### 5.1 Interactive Travel Planning with Human Oversight

```java
@Service
@Agent(name = "interactive-travel-planner", description = "Travel planning with human decision points")
public class InteractiveTravelPlannerService {
    
    @Action(description = "Plan comprehensive travel itinerary with human checkpoints")
    public String planTravelItinerary(
        @Parameter(description = "Travel planning request") TravelPlanningRequest request) {
        
        try {
            TravelPlanningSession session = new TravelPlanningSession(request);
            
            // Step 1: Initial research (automated)
            session.setStep("INITIAL_RESEARCH");
            TravelResearchResult research = conductInitialResearch(request);
            session.addResult("research", research);
            
            // Checkpoint 1: Review research results
            if (research.hasComplexOptions() || research.getEstimatedCost() > 2000) {
                String checkpointId = submitHumanCheckpoint(
                    "Travel Research Review",
                    "Review initial travel research and provide guidance on next steps",
                    session,
                    List.of("CONTINUE_AS_PLANNED", "MODIFY_CRITERIA", "CANCEL_PLANNING")
                );
                
                session.setCurrentCheckpoint(checkpointId);
                return String.format("Travel research completed. Human review required. " +
                    "Checkpoint ID: %s. Found %d flight options, %d hotel options. " +
                    "Estimated cost: %s", checkpointId, research.getFlightOptions().size(),
                    research.getHotelOptions().size(), research.getEstimatedCost());
            }
            
            // Step 2: Option refinement (continue if approved)
            session.setStep("OPTION_REFINEMENT");
            TravelOptions refinedOptions = refineOptions(research, request.getPreferences());
            session.addResult("options", refinedOptions);
            
            // Checkpoint 2: Final approval before booking
            if (refinedOptions.getTotalCost() > 1500) {
                String checkpointId = submitHumanCheckpoint(
                    "Final Travel Plan Approval",
                    "Approve final travel plan before proceeding to booking",
                    session,
                    List.of("APPROVE_AND_BOOK", "APPROVE_HOLD_ONLY", "REQUEST_CHANGES", "CANCEL")
                );
                
                return String.format("Travel plan ready for final approval. " +
                    "Checkpoint ID: %s. Total cost: %s. Ready to proceed with booking.",
                    checkpointId, refinedOptions.getTotalCost());
            }
            
            return completeTravelPlanning(session);
            
        } catch (Exception e) {
            log.error("Travel planning failed", e);
            return "Error planning travel: " + e.getMessage();
        }
    }
    
    private String submitHumanCheckpoint(String title, String description, 
                                       TravelPlanningSession session,
                                       List<String> allowedActions) {
        
        CheckpointContext context = CheckpointContext.builder()
            .sessionId(session.getSessionId())
            .title(title)
            .description(description)
            .allowedActions(allowedActions)
            .sessionData(session.toMap())
            .requesterId(getCurrentUserId())
            .build();
        
        return humanInLoopService.submitCheckpoint(context);
    }
}
```

### 5.2 Emergency Intervention System

```java
@Service
@Agent(name = "emergency-intervention", description = "Emergency stops and escalation")
public class EmergencyInterventionService {
    
    @Action(description = "Monitor automation for emergency situations")
    public String monitorAutomationSafety(
        @Parameter(description = "Monitoring session ID") String sessionId,
        @Parameter(description = "Monitoring duration in minutes") int durationMinutes) {
        
        try {
            SafetyMonitoringSession monitoringSession = 
                safetyMonitoringService.startMonitoring(sessionId, durationMinutes);
            
            // Set up real-time monitoring with human escalation
            CompletableFuture.runAsync(() -> {
                while (monitoringSession.isActive()) {
                    try {
                        List<SafetyIndicator> indicators = 
                            safetyDetectionService.checkSafetyIndicators(sessionId);
                        
                        for (SafetyIndicator indicator : indicators) {
                            if (indicator.getSeverity() == SafetySeverity.CRITICAL) {
                                // Immediate human intervention required
                                triggerEmergencyIntervention(sessionId, indicator);
                            } else if (indicator.getSeverity() == SafetySeverity.HIGH) {
                                // Human review required
                                submitSafetyReview(sessionId, indicator);
                            }
                        }
                        
                        Thread.sleep(10000); // Check every 10 seconds
                        
                    } catch (Exception e) {
                        log.error("Safety monitoring error", e);
                    }
                }
            });
            
            return String.format("Safety monitoring started for session %s. " +
                "Monitoring for %d minutes with emergency intervention enabled.",
                sessionId, durationMinutes);
                
        } catch (Exception e) {
            log.error("Failed to start safety monitoring", e);
            return "Error starting safety monitoring: " + e.getMessage();
        }
    }
    
    private void triggerEmergencyIntervention(String sessionId, SafetyIndicator indicator) {
        // Immediately halt all automation
        automationController.emergencyStop(sessionId);
        
        // Create emergency intervention workflow
        EmergencyInterventionContext context = EmergencyInterventionContext.builder()
            .sessionId(sessionId)
            .indicator(indicator)
            .triggeredAt(LocalDateTime.now())
            .severity(indicator.getSeverity())
            .build();
        
        String interventionId = humanInLoopService.triggerEmergencyIntervention(context);
        
        // Immediate notifications to all stakeholders
        notificationService.sendEmergencyAlert(
            "Emergency Automation Stop",
            String.format("Automation session %s stopped due to %s. " +
                "Immediate human intervention required. Intervention ID: %s",
                sessionId, indicator.getDescription(), interventionId),
            List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.SLACK)
        );
        
        // Log emergency intervention
        auditLogger.logEmergencyIntervention(sessionId, indicator, interventionId);
    }
}
```

## 6. Playwright Integration with Human Checkpoints

### 6.1 Enhanced Web Automation with Human Oversight

```java
@Service
@Agent(name = "supervised-web-automation", description = "Web automation with human oversight")
public class SupervisedWebAutomationService {
    
    @Autowired
    private Browser playwrightBrowser;
    
    @Action(description = "Perform supervised web automation with human checkpoints")
    public String performSupervisedWebAutomation(
        @Parameter(description = "Web automation steps with approval points") String automationPlan) {
        
        try (BrowserContext context = playwrightBrowser.newContext()) {
            Page page = context.newPage();
            SupervisedAutomationSession session = new SupervisedAutomationSession(automationPlan);
            
            // Parse automation plan into steps
            List<AutomationStep> steps = parseAutomationPlan(automationPlan);
            
            for (AutomationStep step : steps) {
                // Check if step requires human approval
                if (step.requiresApproval()) {
                    String checkpointId = submitAutomationCheckpoint(
                        "Web Automation Approval",
                        String.format("Approve execution of: %s", step.getDescription()),
                        step,
                        session
                    );
                    
                    // Wait for human decision
                    CheckpointDecision decision = waitForCheckpointDecision(checkpointId, 300); // 5 min timeout
                    
                    if (decision.getAction().equals("REJECT")) {
                        auditLogger.logAutomationRejection(getCurrentUserId(), step, decision.getReason());
                        return String.format("Automation stopped by human decision. " +
                            "Reason: %s", decision.getReason());
                    } else if (decision.getAction().equals("MODIFY")) {
                        // Apply human modifications
                        step = applyHumanModifications(step, decision.getModifications());
                    }
                }
                
                // Execute the step
                AutomationResult stepResult = executeAutomationStep(page, step);
                session.addStepResult(stepResult);
                
                // Capture screenshot for human review
                if (step.requiresScreenshot()) {
                    String screenshotPath = captureScreenshot(page, step.getName());
                    session.addScreenshot(screenshotPath);
                }
                
                // Check for unexpected page changes that might require human intervention
                if (detectUnexpectedPageChange(page, step.getExpectedState())) {
                    String emergencyCheckpointId = submitEmergencyCheckpoint(
                        "Unexpected Page State",
                        "Page state differs from expected. Human intervention may be required.",
                        capturePageState(page),
                        session
                    );
                    
                    CheckpointDecision emergencyDecision = 
                        waitForCheckpointDecision(emergencyCheckpointId, 180); // 3 min timeout
                    
                    if (emergencyDecision.getAction().equals("ABORT")) {
                        return "Automation aborted due to unexpected page state.";
                    }
                }
            }
            
            return String.format("Supervised automation completed successfully. " +
                "Executed %d steps with human oversight. " +
                "Screenshots captured: %d",
                steps.size(), session.getScreenshots().size());
                
        } catch (Exception e) {
            log.error("Supervised web automation failed", e);
            return "Error in supervised automation: " + e.getMessage();
        }
    }
    
    private CheckpointDecision waitForCheckpointDecision(String checkpointId, int timeoutSeconds) {
        // Poll for human decision with timeout
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            CheckpointDecision decision = humanInLoopService.getCheckpointDecision(checkpointId);
            if (decision != null) {
                return decision;
            }
            
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Timeout reached, use default safe action
        return CheckpointDecision.builder()
            .action("REJECT")
            .reason("Timeout waiting for human decision")
            .build();
    }
}
```

## 7. Advanced Approval Workflows

### 7.1 Multi-Stage Approval with Delegation

```java
@Service
public class AdvancedApprovalWorkflowService {
    
    @Action(description = "Process multi-stage approval with delegation options")
    public String processMultiStageApproval(
        @Parameter(description = "Complex action requiring multiple approvals") ComplexActionRequest request) {
        
        try {
            // Create multi-stage workflow
            MultiStageWorkflow workflow = MultiStageWorkflow.builder()
                .workflowId(UUID.randomUUID().toString())
                .requesterId(getCurrentUserId())
                .actionRequest(request)
                .stages(buildApprovalStages(request))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
            
            // Start the workflow
            workflowEngine.startWorkflow(workflow);
            
            // Process first stage
            ApprovalStage firstStage = workflow.getCurrentStage();
            String stageResult = processApprovalStage(workflow, firstStage);
            
            return String.format("Multi-stage approval workflow started. " +
                "Workflow ID: %s. Current stage: %s. %s",
                workflow.getWorkflowId(), firstStage.getName(), stageResult);
                
        } catch (Exception e) {
            log.error("Multi-stage approval failed", e);
            return "Error processing multi-stage approval: " + e.getMessage();
        }
    }
    
    private List<ApprovalStage> buildApprovalStages(ComplexActionRequest request) {
        List<ApprovalStage> stages = new ArrayList<>();
        
        // Stage 1: Technical Review
        if (request.requiresTechnicalReview()) {
            stages.add(ApprovalStage.builder()
                .stageName("TECHNICAL_REVIEW")
                .description("Technical feasibility and safety review")
                .requiredApprovers(List.of(
                    getApprover("TECHNICAL_LEAD"),
                    getApprover("SECURITY_OFFICER")
                ))
                .approvalCriteria(buildTechnicalCriteria(request))
                .timeoutHours(4)
                .build());
        }
        
        // Stage 2: Business Review
        if (request.requiresBusinessReview()) {
            stages.add(ApprovalStage.builder()
                .stageName("BUSINESS_REVIEW")
                .description("Business impact and compliance review")
                .requiredApprovers(List.of(
                    getApprover("BUSINESS_MANAGER"),
                    getApprover("COMPLIANCE_OFFICER")
                ))
                .approvalCriteria(buildBusinessCriteria(request))
                .timeoutHours(8)
                .build());
        }
        
        // Stage 3: Financial Review (if high cost)
        if (request.getEstimatedCost() > 5000) {
            stages.add(ApprovalStage.builder()
                .stageName("FINANCIAL_REVIEW")
                .description("Financial approval for high-cost action")
                .requiredApprovers(List.of(
                    getApprover("FINANCIAL_CONTROLLER"),
                    getApprover("CFO")
                ))
                .approvalCriteria(buildFinancialCriteria(request))
                .timeoutHours(12)
                .build());
        }
        
        return stages;
    }
    
    private String processApprovalStage(MultiStageWorkflow workflow, ApprovalStage stage) {
        // Send notifications to all required approvers
        for (Approver approver : stage.getRequiredApprovers()) {
            ApprovalNotification notification = ApprovalNotification.builder()
                .workflowId(workflow.getWorkflowId())
                .stageName(stage.getStageName())
                .approverId(approver.getId())
                .title(String.format("Approval Required: %s", stage.getDescription()))
                .message(buildApprovalMessage(workflow, stage))
                .urgency(calculateUrgency(workflow, stage))
                .expiresAt(stage.getExpiresAt())
                .build();
            
            notificationService.sendApprovalNotification(notification);
        }
        
        // Set up timeout handling
        scheduleStageTimeout(workflow.getWorkflowId(), stage);
        
        return String.format("Approval requests sent to %d approvers. " +
            "Timeout: %d hours", stage.getRequiredApprovers().size(), stage.getTimeoutHours());
    }
}
```

### 7.2 Escalation and Delegation Handling

```java
@Component
public class EscalationHandler {
    
    @EventListener
    public void handleApprovalTimeout(ApprovalTimeoutEvent event) {
        log.warn("Approval timeout for workflow: {}, stage: {}", 
            event.getWorkflowId(), event.getStageName());
        
        ApprovalStage stage = workflowEngine.getCurrentStage(event.getWorkflowId());
        EscalationRule rule = escalationRuleService.getEscalationRule(stage);
        
        if (rule != null) {
            processEscalation(event.getWorkflowId(), rule);
        } else {
            // Default escalation: notify manager
            notifyManagerOfTimeout(event);
        }
    }
    
    private void processEscalation(String workflowId, EscalationRule rule) {
        switch (rule.getEscalationType()) {
            case AUTO_APPROVE:
                // Automatic approval for low-risk timeouts
                workflowEngine.autoApproveStage(workflowId, "ESCALATION_AUTO_APPROVE");
                break;
                
            case DELEGATE_TO_MANAGER:
                // Delegate to next level manager
                Approver manager = userService.getManager(rule.getOriginalApproverId());
                workflowEngine.delegateApproval(workflowId, rule.getOriginalApproverId(), manager.getId());
                break;
                
            case ESCALATE_TO_EMERGENCY:
                // Convert to emergency approval
                workflowEngine.convertToEmergencyApproval(workflowId);
                break;
                
            case REJECT_ON_TIMEOUT:
                // Automatic rejection for high-risk actions
                workflowEngine.rejectWorkflow(workflowId, "TIMEOUT_AUTO_REJECT");
                break;
        }
        
        auditLogger.logEscalation(workflowId, rule);
    }
}
```

## 8. Audit and Compliance Integration

### 8.1 Comprehensive Audit Logging

```java
@Service
@Agent(name = "audit-service", description = "Comprehensive audit logging for human decisions")
public class AuditService {
    
    @Action(description = "Generate human-in-the-loop audit report")
    public String generateHumanInLoopAuditReport(
        @Parameter(description = "Audit period start date") String startDate,
        @Parameter(description = "Audit period end date") String endDate,
        @Parameter(description = "Include workflow details") boolean includeDetails) {
        
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            HumanInLoopAuditReport report = auditReportGenerator.generateReport(start, end, includeDetails);
            
            StringBuilder auditSummary = new StringBuilder();
            auditSummary.append("Human-in-the-Loop Audit Report\n");
            auditSummary.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");
            
            auditSummary.append("Summary Statistics:\n");
            auditSummary.append("- Total Workflows: ").append(report.getTotalWorkflows()).append("\n");
            auditSummary.append("- Approved Actions: ").append(report.getApprovedActions()).append("\n");
            auditSummary.append("- Rejected Actions: ").append(report.getRejectedActions()).append("\n");
            auditSummary.append("- Timed Out Actions: ").append(report.getTimedOutActions()).append("\n");
            auditSummary.append("- Average Approval Time: ").append(report.getAverageApprovalTime()).append("\n");
            auditSummary.append("- Escalations: ").append(report.getEscalationCount()).append("\n\n");
            
            auditSummary.append("Risk Level Breakdown:\n");
            for (Map.Entry<String, Integer> entry : report.getRiskLevelBreakdown().entrySet()) {
                auditSummary.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            auditSummary.append("\nApprover Performance:\n");
            for (ApproverMetrics metrics : report.getApproverMetrics()) {
                auditSummary.append("- ").append(metrics.getApproverName())
                          .append(": ").append(metrics.getApprovalCount()).append(" approvals, ")
                          .append(metrics.getAverageResponseTime()).append(" avg response time\n");
            }
            
            if (includeDetails) {
                auditSummary.append("\nDetailed Workflow Analysis:\n");
                for (WorkflowAuditDetail detail : report.getWorkflowDetails()) {
                    auditSummary.append(formatWorkflowDetail(detail));
                }
            }
            
            // Store audit report
            String reportId = auditReportRepository.saveReport(report);
            auditSummary.append("\nAudit report saved with ID: ").append(reportId);
            
            return auditSummary.toString();
            
        } catch (Exception e) {
            log.error("Audit report generation failed", e);
            return "Error generating audit report: " + e.getMessage();
        }
    }
}
```

## 9. Testing Human-in-the-Loop Workflows

### 9.1 Integration Testing Framework

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "human-in-loop.test-mode=true",
    "workflow.timeout.test-override=10" // Short timeouts for testing
})
class HumanInLoopIntegrationTest {
    
    @Autowired
    private TravelBookingService travelBookingService;
    
    @Autowired
    private HumanInLoopService humanInLoopService;
    
    @Autowired
    private TestDataHelper testDataHelper;
    
    @Test
    @WithMockUser(roles = {"BOOKING_AGENT"})
    void testMediumRiskActionRequiresApproval() {
        // Given
        FlightReservationRequest request = testDataHelper.createHighValueReservationRequest();
        
        // When
        String result = travelBookingService.holdFlightReservation(request);
        
        // Then
        assertThat(result).contains("approval");
        assertThat(result).contains("Workflow ID");
        
        // Verify workflow was created
        String workflowId = extractWorkflowId(result);
        ApprovalWorkflowDetails workflow = humanInLoopService.getWorkflowDetails(workflowId);
        
        assertThat(workflow.getStatus()).isEqualTo("PENDING");
        assertThat(workflow.getActionMethod()).isEqualTo("holdFlightReservation");
        
        // Verify notifications were sent
        verify(notificationService).sendApprovalNotification(any());
        verify(auditLogger).logApprovalRequest(any(), eq("holdFlightReservation"), eq(workflowId));
    }
    
    @Test
    @WithMockUser(roles = {"SUPERVISOR"})
    void testApprovalProcessCompleteFlow() {
        // Given
        String workflowId = setupPendingApprovalWorkflow();
        ApprovalDecisionRequest approvalDecision = ApprovalDecisionRequest.builder()
            .decision("APPROVE")
            .reason("Request approved after review")
            .build();
        
        // When
        String result = humanInLoopService.processApproval(workflowId, getCurrentUserId(), approvalDecision);
        
        // Then
        assertThat(result).contains("approved");
        
        // Verify workflow status updated
        ApprovalWorkflowDetails workflow = humanInLoopService.getWorkflowDetails(workflowId);
        assertThat(workflow.getStatus()).isEqualTo("APPROVED");
        
        // Verify action was executed
        verify(actionExecutorService).executeApprovedAction(any());
        verify(auditLogger).logApprovalDecision(any(), eq(workflowId), any());
    }
    
    @Test
    void testEmergencyInterventionTriggered() {
        // Given
        String sessionId = "test-session-123";
        SafetyIndicator criticalIndicator = SafetyIndicator.builder()
            .severity(SafetySeverity.CRITICAL)
            .description("Suspicious payment pattern detected")
            .build();
        
        // When
        emergencyInterventionService.triggerEmergencyIntervention(sessionId, criticalIndicator);
        
        // Then
        verify(automationController).emergencyStop(sessionId);
        verify(notificationService).sendEmergencyAlert(any(), any(), any());
        verify(auditLogger).logEmergencyIntervention(eq(sessionId), eq(criticalIndicator), any());
    }
}
```

## 10. Best Practices and Guidelines

### 10.1 Human-in-the-Loop Design Principles

1. **Graduated Automation**: Start with high human involvement, gradually increase automation as trust is built
2. **Clear Decision Points**: Make it obvious when and why human input is needed
3. **Contextual Information**: Provide all necessary context for informed human decisions
4. **Reasonable Timeouts**: Set appropriate timeout periods based on action criticality
5. **Fallback Mechanisms**: Always have safe defaults when human input is unavailable

### 10.2 Security Considerations

```java
@Component
public class HumanInLoopSecurityValidator {
    
    public boolean validateApprovalRequest(ApprovalContext context) {
        // Validate requester authorization
        if (!userService.hasPermission(context.getRequesterId(), context.getActionMethod())) {
            auditLogger.logUnauthorizedApprovalRequest(context);
            return false;
        }
        
        // Check for approval bypass attempts
        if (suspiciousActivityDetector.detectBypassAttempt(context)) {
            auditLogger.logSuspiciousApprovalActivity(context);
            securityIncidentService.reportIncident(context);
            return false;
        }
        
        // Validate approval chain integrity
        if (!approvalChainValidator.validateChain(context.getWorkflowId())) {
            auditLogger.logApprovalChainViolation(context);
            return false;
        }
        
        return true;
    }
}
```

### 10.3 Performance Optimization

```java
@Service
public class HumanInLoopOptimizationService {
    
    @Cacheable(value = "approval-decisions", key = "#approverId + '-' + #actionType")
    public ApprovalPattern getApprovalPattern(String approverId, String actionType) {
        // Cache approval patterns to predict likelihood of approval
        return approvalPatternRepository.findByApproverAndActionType(approverId, actionType);
    }
    
    @Async
    public CompletableFuture<Void> preloadApprovalContext(String workflowId) {
        // Preload related data for faster approval processing
        ApprovalWorkflow workflow = workflowRepository.findById(workflowId);
        contextPreloader.preloadContext(workflow);
        return CompletableFuture.completedFuture(null);
    }
}
```

## 11. Conclusion

Human-in-the-Loop integration is essential for building trustworthy AI automation systems. This tutorial has covered:

### 11.1 Key Capabilities Implemented

- **Multi-Level Approval Workflows**: Graduated approval requirements based on risk
- **Real-Time Decision Points**: Interactive checkpoints during automation
- **Emergency Intervention**: Immediate human override capabilities
- **Comprehensive Audit Trails**: Full tracking of human decisions and AI actions
- **WebSocket Notifications**: Real-time updates for pending approvals

### 11.2 Travel Industry Benefits

- **Risk Mitigation**: Human oversight for high-value bookings and payments
- **Regulatory Compliance**: Audit trails for financial transactions
- **Customer Protection**: Human review of complex travel arrangements
- **Quality Assurance**: Manual verification of critical booking details

### 11.3 Production Readiness

- **Scalable Architecture**: PostgreSQL-backed workflow state management
- **Security Integration**: Spring Security authorization controls
- **Performance Optimization**: Caching and async processing
- **Error Handling**: Comprehensive exception handling and recovery

The a2acore framework with Spring Boot provides a robust foundation for implementing sophisticated Human-in-the-Loop workflows that balance automation efficiency with human oversight and control.