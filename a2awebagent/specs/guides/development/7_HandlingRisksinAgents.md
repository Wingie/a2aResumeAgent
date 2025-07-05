# Tutorial 7: Risk Management and Safety in a2acore AI Agents

## 1. Introduction

Risk management is crucial when building AI-powered automation systems like a2aTravelAgent. This tutorial demonstrates how to implement comprehensive risk assessment, mitigation strategies, and safety controls using the a2acore framework. Learn to classify actions by risk level, implement human-in-the-loop verification, and establish robust security frameworks for production AI agent deployments.

## 2. Risk Assessment Framework

### 2.1 a2acore Risk Architecture
```
a2aTravelAgent Risk Management:
├── a2acore Framework          # Risk foundation
│   ├── @Action(riskLevel)    # Built-in risk classification
│   ├── SecurityValidator     # Input validation and sanitization
│   ├── AuthorizationManager  # Permission-based access control
│   └── AuditLogger          # Comprehensive action logging
├── Spring Security           # Enterprise security layer
│   ├── Authentication       # User and system authentication
│   ├── Authorization        # Role-based access control
│   ├── Rate Limiting        # Abuse prevention
│   └── Session Management   # Secure session handling
├── Risk Assessment Engine    # Dynamic risk evaluation
│   ├── Context Analysis     # Situation-aware risk scoring
│   ├── Historical Patterns  # Learning from past incidents
│   ├── External Factors     # Environmental risk considerations
│   └── Real-time Monitoring # Continuous threat detection
└── Human-in-the-Loop        # Human oversight integration
    ├── Approval Workflows   # Structured approval processes
    ├── Escalation Paths     # Automated escalation rules
    ├── Override Mechanisms  # Emergency intervention
    └── Audit Trails         # Complete decision tracking
```

### 2.2 Risk Classification System
The a2acore framework implements a comprehensive risk classification system:

```java
public enum ActionRisk {
    LOW,      // Informational actions, read-only operations
    MEDIUM,   // Actions with moderate impact, reversible changes
    HIGH,     // Critical actions, financial transactions, irreversible operations
    CRITICAL  // Emergency actions, system-wide changes, security-sensitive operations
}
```

## 3. Implementing Risk-Aware Actions

### 3.1 Basic Risk Classification
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import io.wingie.a2acore.security.ActionRisk;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@Agent(name = "travel-booking", 
       description = "Travel booking and reservation management with risk controls")
public class TravelBookingService {
    
    @Autowired
    private BookingSecurityValidator securityValidator;
    
    @Autowired
    private PaymentService paymentService;
    
    @Action(description = "Search for available flights", riskLevel = ActionRisk.LOW)
    public String searchFlights(
        @Parameter(description = "Origin airport code") String origin,
        @Parameter(description = "Destination airport code") String destination,
        @Parameter(description = "Travel date") String date) {
        
        // Low risk: Read-only operation, no side effects
        try {
            // Validate input parameters
            securityValidator.validateAirportCode(origin);
            securityValidator.validateAirportCode(destination);
            securityValidator.validateDate(date);
            
            FlightSearchResults results = flightSearchService.search(origin, destination, date);
            
            // Log the search for analytics (no sensitive data)
            auditLogger.logFlightSearch(origin, destination, date, results.getCount());
            
            return formatFlightResults(results);
            
        } catch (ValidationException e) {
            log.warn("Invalid flight search parameters: {}", e.getMessage());
            return "Error: Invalid search parameters - " + e.getMessage();
        }
    }
    
    @Action(description = "Hold flight reservation temporarily", riskLevel = ActionRisk.MEDIUM)
    @PreAuthorize("hasRole('BOOKING_AGENT') or hasRole('ADMIN')")
    public String holdFlightReservation(
        @Parameter(description = "Flight details for reservation") FlightReservationRequest request) {
        
        // Medium risk: Creates temporary reservation, reversible within time limit
        try {
            // Enhanced validation for medium-risk actions
            securityValidator.validateReservationRequest(request);
            
            // Check user authorization
            if (!authorizationService.canMakeReservation(getCurrentUser(), request)) {
                return "Error: Insufficient authorization for this reservation";
            }
            
            ReservationResult result = reservationService.holdReservation(request);
            
            // Detailed audit logging for medium-risk actions
            auditLogger.logReservationHold(getCurrentUser(), request, result);
            
            return String.format("Reservation held successfully. Confirmation: %s. " +
                "Hold expires: %s", result.getConfirmationCode(), result.getHoldExpiry());
                
        } catch (Exception e) {
            log.error("Flight reservation hold failed", e);
            auditLogger.logReservationFailure(getCurrentUser(), request, e);
            return "Error holding reservation: " + e.getMessage();
        }
    }
    
    @Action(description = "Process payment and confirm booking", riskLevel = ActionRisk.HIGH)
    @PreAuthorize("hasRole('BOOKING_AGENT') and hasPermission(#paymentRequest, 'PROCESS_PAYMENT')")
    public String processPaymentAndConfirmBooking(
        @Parameter(description = "Payment and booking confirmation details") PaymentBookingRequest paymentRequest) {
        
        // High risk: Financial transaction, irreversible
        try {
            // Comprehensive validation for high-risk actions
            securityValidator.validatePaymentRequest(paymentRequest);
            
            // Additional security checks
            if (!fraudDetectionService.validateTransaction(paymentRequest)) {
                auditLogger.logFraudAttempt(getCurrentUser(), paymentRequest);
                return "Error: Transaction flagged by fraud detection system";
            }
            
            // Human verification for high-risk actions
            if (requiresHumanApproval(paymentRequest)) {
                String approvalId = approvalWorkflowService.submitForApproval(paymentRequest);
                auditLogger.logApprovalRequest(getCurrentUser(), paymentRequest, approvalId);
                return String.format("High-value transaction requires human approval. " +
                    "Approval ID: %s. Status can be checked via approval endpoint.", approvalId);
            }
            
            // Process payment with transaction management
            PaymentResult paymentResult = paymentService.processPayment(paymentRequest);
            
            if (paymentResult.isSuccessful()) {
                BookingConfirmation booking = bookingService.confirmBooking(paymentRequest);
                
                // Comprehensive audit for successful high-risk actions
                auditLogger.logSuccessfulBooking(getCurrentUser(), paymentRequest, 
                    paymentResult, booking);
                
                return String.format("Booking confirmed successfully. " +
                    "Booking reference: %s. Payment confirmation: %s", 
                    booking.getBookingReference(), paymentResult.getTransactionId());
            } else {
                auditLogger.logPaymentFailure(getCurrentUser(), paymentRequest, paymentResult);
                return "Payment processing failed: " + paymentResult.getErrorMessage();
            }
            
        } catch (Exception e) {
            log.error("Payment and booking confirmation failed", e);
            auditLogger.logBookingError(getCurrentUser(), paymentRequest, e);
            return "Error processing booking: " + e.getMessage();
        }
    }
    
    @Action(description = "Cancel confirmed booking with refund", riskLevel = ActionRisk.CRITICAL)
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public String cancelBookingWithRefund(
        @Parameter(description = "Booking cancellation and refund details") CancellationRequest cancellationRequest) {
        
        // Critical risk: Financial refund, potential business impact
        try {
            // Critical action validation
            securityValidator.validateCancellationRequest(cancellationRequest);
            
            // Mandatory human approval for critical actions
            if (!hasManagerApproval(cancellationRequest)) {
                String approvalId = managerApprovalService.submitCriticalApproval(cancellationRequest);
                auditLogger.logCriticalApprovalRequest(getCurrentUser(), cancellationRequest, approvalId);
                return String.format("Critical action requires manager approval. " +
                    "Approval ID: %s. Please wait for manager authorization.", approvalId);
            }
            
            // Multi-step verification for critical actions
            BookingDetails booking = bookingService.getBookingDetails(cancellationRequest.getBookingReference());
            
            if (booking == null) {
                auditLogger.logInvalidCancellationAttempt(getCurrentUser(), cancellationRequest);
                return "Error: Booking not found";
            }
            
            // Calculate refund amount with business rules
            RefundCalculation refund = refundCalculationService.calculateRefund(booking, cancellationRequest);
            
            // Process cancellation and refund
            CancellationResult result = cancellationService.processCancellation(booking, refund);
            
            // Comprehensive audit for critical actions
            auditLogger.logCriticalCancellation(getCurrentUser(), cancellationRequest, 
                booking, refund, result);
            
            // Notify management of critical action
            notificationService.notifyManagement("Critical booking cancellation processed", 
                getCurrentUser(), cancellationRequest, result);
            
            return String.format("Booking cancelled successfully. " +
                "Refund amount: %s. Refund reference: %s. " +
                "Refund will be processed within 5-7 business days.", 
                refund.getRefundAmount(), result.getRefundReference());
                
        } catch (Exception e) {
            log.error("Critical booking cancellation failed", e);
            auditLogger.logCriticalActionFailure(getCurrentUser(), cancellationRequest, e);
            notificationService.alertSecurity("Critical action failure", getCurrentUser(), e);
            return "Error processing cancellation: " + e.getMessage();
        }
    }
}
```

### 3.2 Risk Assessment Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Agent(name = "risk-assessment", 
       description = "Dynamic risk assessment and threat analysis for AI actions")
public class RiskAssessmentService {
    
    @Autowired
    private ThreatDetectionService threatDetectionService;
    
    @Autowired
    private ContextAnalysisService contextAnalysisService;
    
    @Action(description = "Assess action risk based on context and user behavior")
    public String assessActionRisk(
        @Parameter(description = "Action details for risk assessment") ActionContext actionContext,
        @Parameter(description = "User context and history") UserContext userContext) {
        
        try {
            RiskAssessment assessment = new RiskAssessment();
            
            // Base risk from action annotation
            ActionRisk baseRisk = actionContext.getBaseRiskLevel();
            assessment.setBaseRisk(baseRisk);
            
            // Context-based risk modifiers
            RiskModifiers modifiers = assessRiskModifiers(actionContext, userContext);
            assessment.setModifiers(modifiers);
            
            // Historical pattern analysis
            UserBehaviorPattern patterns = behaviorAnalysisService.analyzeUserBehavior(userContext);
            if (patterns.hasAnomalousActivity()) {
                modifiers.addModifier("anomalous_behavior", 0.3);
            }
            
            // Time-based risk factors
            if (isOutsideBusinessHours()) {
                modifiers.addModifier("outside_business_hours", 0.2);
            }
            
            // Geographic risk factors
            if (geoLocationService.isHighRiskLocation(userContext.getLocation())) {
                modifiers.addModifier("high_risk_location", 0.4);
            }
            
            // Calculate final risk score
            double finalRiskScore = calculateFinalRiskScore(baseRisk, modifiers);
            assessment.setFinalRiskScore(finalRiskScore);
            
            // Determine required controls
            List<SecurityControl> requiredControls = determineSecurityControls(assessment);
            assessment.setRequiredControls(requiredControls);
            
            // Log risk assessment
            auditLogger.logRiskAssessment(userContext.getUserId(), actionContext, assessment);
            
            return formatRiskAssessment(assessment);
            
        } catch (Exception e) {
            log.error("Risk assessment failed", e);
            return "Error performing risk assessment: " + e.getMessage();
        }
    }
    
    @Action(description = "Monitor ongoing actions for risk escalation")
    public String monitorActionRisk(
        @Parameter(description = "Active session identifier") String sessionId,
        @Parameter(description = "Monitoring duration in minutes") int durationMinutes) {
        
        try {
            MonitoringSession session = riskMonitoringService.startMonitoring(sessionId, durationMinutes);
            
            // Set up real-time monitoring
            CompletableFuture.runAsync(() -> {
                while (session.isActive()) {
                    try {
                        // Check for risk escalation indicators
                        List<RiskIndicator> indicators = threatDetectionService.checkRiskIndicators(sessionId);
                        
                        for (RiskIndicator indicator : indicators) {
                            if (indicator.getSeverity().ordinal() >= RiskSeverity.HIGH.ordinal()) {
                                // Trigger immediate response
                                incidentResponseService.handleHighRiskEvent(sessionId, indicator);
                                
                                // Alert security team
                                notificationService.alertSecurity("High risk event detected", sessionId, indicator);
                            }
                        }
                        
                        Thread.sleep(30000); // Check every 30 seconds
                        
                    } catch (Exception e) {
                        log.error("Error during risk monitoring", e);
                    }
                }
            });
            
            return String.format("Risk monitoring started for session %s. " +
                "Monitoring will continue for %d minutes.", sessionId, durationMinutes);
                
        } catch (Exception e) {
            log.error("Failed to start risk monitoring", e);
            return "Error starting risk monitoring: " + e.getMessage();
        }
    }
    
    private RiskModifiers assessRiskModifiers(ActionContext actionContext, UserContext userContext) {
        RiskModifiers modifiers = new RiskModifiers();
        
        // Velocity-based risk (too many actions too quickly)
        if (actionVelocityService.isHighVelocity(userContext.getUserId())) {
            modifiers.addModifier("high_velocity", 0.3);
        }
        
        // User privilege level
        if (userContext.hasElevatedPrivileges()) {
            modifiers.addModifier("elevated_privileges", 0.2);
        }
        
        // Action complexity and dependencies
        if (actionContext.hasExternalDependencies()) {
            modifiers.addModifier("external_dependencies", 0.1);
        }
        
        // Data sensitivity
        if (actionContext.involvesPersonalData()) {
            modifiers.addModifier("personal_data", 0.2);
        }
        
        return modifiers;
    }
}
```

## 4. Human-in-the-Loop Integration

### 4.1 Approval Workflow Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Agent(name = "approval-workflow", 
       description = "Human approval workflows for high-risk AI actions")
public class ApprovalWorkflowService {
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private NotificationService notificationService;
    
    @Action(description = "Submit action for human approval")
    public String submitForApproval(
        @Parameter(description = "Action requiring approval") ActionApprovalRequest request) {
        
        try {
            // Create approval workflow instance
            ApprovalWorkflow workflow = workflowEngine.createApprovalWorkflow(request);
            
            // Determine approvers based on action risk and context
            List<Approver> requiredApprovers = determineRequiredApprovers(request);
            workflow.setRequiredApprovers(requiredApprovers);
            
            // Set approval criteria
            ApprovalCriteria criteria = buildApprovalCriteria(request);
            workflow.setApprovalCriteria(criteria);
            
            // Start the workflow
            String workflowId = workflowEngine.startWorkflow(workflow);
            
            // Notify approvers
            for (Approver approver : requiredApprovers) {
                notificationService.sendApprovalRequest(approver, request, workflowId);
            }
            
            // Log approval request
            auditLogger.logApprovalRequest(request.getRequesterId(), request, workflowId, requiredApprovers);
            
            return String.format("Approval request submitted. Workflow ID: %s. " +
                "Requires approval from: %s", workflowId, 
                requiredApprovers.stream().map(Approver::getName).collect(Collectors.joining(", ")));
                
        } catch (Exception e) {
            log.error("Failed to submit approval request", e);
            return "Error submitting approval request: " + e.getMessage();
        }
    }
    
    @Action(description = "Process approval decision from human approver")
    public String processApprovalDecision(
        @Parameter(description = "Approval decision details") ApprovalDecision decision) {
        
        try {
            // Validate approval decision
            if (!approvalValidator.validateDecision(decision)) {
                return "Error: Invalid approval decision format";
            }
            
            // Verify approver authority
            if (!authorizationService.canApprove(decision.getApproverId(), decision.getWorkflowId())) {
                auditLogger.logUnauthorizedApproval(decision.getApproverId(), decision.getWorkflowId());
                return "Error: Insufficient authority to approve this request";
            }
            
            // Process the decision
            ApprovalResult result = workflowEngine.processApprovalDecision(decision);
            
            if (result.isWorkflowComplete()) {
                if (result.isApproved()) {
                    // Execute the approved action
                    ActionExecutionResult executionResult = executeApprovedAction(result.getOriginalRequest());
                    
                    // Notify requester of approval and execution
                    notificationService.notifyActionApproved(result.getOriginalRequest().getRequesterId(), 
                        result, executionResult);
                    
                    auditLogger.logApprovedActionExecution(decision.getApproverId(), 
                        result.getOriginalRequest(), executionResult);
                    
                    return String.format("Action approved and executed successfully. " +
                        "Execution result: %s", executionResult.getDescription());
                } else {
                    // Notify requester of rejection
                    notificationService.notifyActionRejected(result.getOriginalRequest().getRequesterId(), 
                        result, decision.getRejectionReason());
                    
                    auditLogger.logActionRejection(decision.getApproverId(), 
                        result.getOriginalRequest(), decision.getRejectionReason());
                    
                    return String.format("Action rejected. Reason: %s", decision.getRejectionReason());
                }
            } else {
                // Workflow continues, waiting for more approvals
                return String.format("Approval recorded. Workflow continues. " +
                    "Still waiting for approval from: %s", 
                    String.join(", ", result.getPendingApprovers()));
            }
            
        } catch (Exception e) {
            log.error("Failed to process approval decision", e);
            return "Error processing approval decision: " + e.getMessage();
        }
    }
    
    @Action(description = "Check status of pending approval requests")
    public String checkApprovalStatus(
        @Parameter(description = "Workflow ID to check") String workflowId) {
        
        try {
            ApprovalWorkflowStatus status = workflowEngine.getWorkflowStatus(workflowId);
            
            if (status == null) {
                return "Error: Workflow not found";
            }
            
            StringBuilder statusReport = new StringBuilder();
            statusReport.append("Approval Workflow Status:\n");
            statusReport.append("Workflow ID: ").append(workflowId).append("\n");
            statusReport.append("Status: ").append(status.getCurrentState()).append("\n");
            statusReport.append("Created: ").append(status.getCreatedAt()).append("\n");
            
            if (status.isComplete()) {
                statusReport.append("Completed: ").append(status.getCompletedAt()).append("\n");
                statusReport.append("Final Decision: ").append(status.getFinalDecision()).append("\n");
            } else {
                statusReport.append("Pending Approvers: ").append(
                    String.join(", ", status.getPendingApprovers())).append("\n");
                statusReport.append("Approval Deadline: ").append(status.getApprovalDeadline()).append("\n");
            }
            
            // Add approval history
            statusReport.append("\nApproval History:\n");
            for (ApprovalDecision historicalDecision : status.getApprovalHistory()) {
                statusReport.append("- ").append(historicalDecision.getApproverName())
                           .append(": ").append(historicalDecision.getDecision())
                           .append(" (").append(historicalDecision.getDecisionTime()).append(")\n");
            }
            
            return statusReport.toString();
            
        } catch (Exception e) {
            log.error("Failed to check approval status", e);
            return "Error checking approval status: " + e.getMessage();
        }
    }
    
    private List<Approver> determineRequiredApprovers(ActionApprovalRequest request) {
        List<Approver> approvers = new ArrayList<>();
        
        // Risk-based approver assignment
        switch (request.getRiskLevel()) {
            case HIGH:
                approvers.add(approverService.getManagerApprover(request.getRequesterId()));
                break;
            case CRITICAL:
                approvers.add(approverService.getManagerApprover(request.getRequesterId()));
                approvers.add(approverService.getSecurityOfficer());
                if (request.involvesMoney() && request.getAmount() > 10000) {
                    approvers.add(approverService.getFinancialOfficer());
                }
                break;
        }
        
        return approvers;
    }
}
```

## 5. Security Framework Integration

### 5.1 Advanced Security Controls
```java
package io.wingie.security;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@Agent(name = "security-controls", 
       description = "Advanced security controls and threat mitigation")
public class SecurityControlsService {
    
    @Action(description = "Implement OWASP Top 10 security checks for AI actions")
    public String performOWASPSecurityCheck(
        @Parameter(description = "Action context for security analysis") ActionSecurityContext context) {
        
        try {
            SecurityCheckResult result = new SecurityCheckResult();
            
            // 1. LLM01: Prompt Injection
            if (promptInjectionDetector.detectInjection(context.getUserInput())) {
                result.addViolation("PROMPT_INJECTION", "Potential prompt injection detected");
            }
            
            // 2. LLM02: Insecure Output Handling
            if (outputValidator.containsSensitiveData(context.getExpectedOutput())) {
                result.addViolation("INSECURE_OUTPUT", "Sensitive data in output detected");
            }
            
            // 3. LLM03: Training Data Poisoning (Historical check)
            if (trainingDataValidator.hasAnomalousPatterns(context.getModelResponses())) {
                result.addViolation("DATA_POISONING", "Anomalous training patterns detected");
            }
            
            // 4. LLM04: Model Denial of Service
            if (resourceMonitor.detectDosPattern(context.getResourceUsage())) {
                result.addViolation("MODEL_DOS", "Denial of service pattern detected");
            }
            
            // 5. LLM05: Supply Chain Vulnerabilities
            if (dependencyScanner.hasVulnerabilities(context.getDependencies())) {
                result.addViolation("SUPPLY_CHAIN", "Vulnerable dependencies detected");
            }
            
            // 6. LLM06: Sensitive Information Disclosure
            if (dataLeakageDetector.detectLeakage(context.getActionParameters())) {
                result.addViolation("INFO_DISCLOSURE", "Potential data leakage detected");
            }
            
            // 7. LLM07: Insecure Plugin Design
            if (pluginSecurityValidator.hasWeakDesign(context.getPluginConfiguration())) {
                result.addViolation("INSECURE_PLUGIN", "Insecure plugin design detected");
            }
            
            // 8. LLM08: Excessive Agency
            if (agencyLimitValidator.exceedsLimits(context.getRequestedPermissions())) {
                result.addViolation("EXCESSIVE_AGENCY", "Excessive agency permissions requested");
            }
            
            // 9. LLM09: Overreliance
            if (humanOversightValidator.lacksOversight(context.getActionType())) {
                result.addViolation("OVERRELIANCE", "Critical action lacks human oversight");
            }
            
            // 10. LLM10: Model Theft
            if (modelTheftDetector.detectTheftAttempt(context.getQueryPatterns())) {
                result.addViolation("MODEL_THEFT", "Potential model theft attempt detected");
            }
            
            // Log security check results
            auditLogger.logSecurityCheck(context.getUserId(), context.getActionId(), result);
            
            if (result.hasViolations()) {
                // Alert security team for violations
                notificationService.alertSecurity("OWASP violations detected", context, result);
                return String.format("Security check failed. Violations: %s", 
                    result.getViolationSummary());
            } else {
                return "Security check passed. No violations detected.";
            }
            
        } catch (Exception e) {
            log.error("Security check failed", e);
            return "Error performing security check: " + e.getMessage();
        }
    }
    
    @Action(description = "Implement NIST AI Risk Management Framework controls")
    public String implementNISTRiskControls(
        @Parameter(description = "AI system context") AISystemContext systemContext,
        @Parameter(description = "Risk assessment results") RiskAssessmentResults riskResults) {
        
        try {
            NISTControlsResult controlsResult = new NISTControlsResult();
            
            // GOVERN Function
            if (!governanceValidator.hasProperGovernance(systemContext)) {
                controlsResult.addRecommendation("GOVERNANCE", 
                    "Establish AI governance framework with clear policies and procedures");
            }
            
            // MAP Function
            if (!contextMapper.hasMappedContext(systemContext)) {
                controlsResult.addRecommendation("CONTEXT_MAPPING", 
                    "Map AI system context including stakeholders, use cases, and impacts");
            }
            
            // MEASURE Function
            if (!metricsValidator.hasProperMetrics(systemContext)) {
                controlsResult.addRecommendation("METRICS", 
                    "Implement comprehensive AI system metrics and monitoring");
            }
            
            // MANAGE Function
            if (!riskManagementValidator.hasRiskManagement(systemContext)) {
                controlsResult.addRecommendation("RISK_MANAGEMENT", 
                    "Establish systematic risk management processes");
            }
            
            // Trustworthiness characteristics assessment
            TrustworthinessAssessment trustAssessment = assessTrustworthiness(systemContext);
            controlsResult.setTrustworthinessAssessment(trustAssessment);
            
            // Generate improvement plan
            ImprovementPlan improvementPlan = generateImprovementPlan(controlsResult);
            controlsResult.setImprovementPlan(improvementPlan);
            
            // Log NIST compliance assessment
            auditLogger.logNISTAssessment(systemContext.getSystemId(), controlsResult);
            
            return formatNISTControlsResult(controlsResult);
            
        } catch (Exception e) {
            log.error("NIST risk controls implementation failed", e);
            return "Error implementing NIST controls: " + e.getMessage();
        }
    }
}
```

### 5.2 Threat Detection and Response
```java
package io.wingie.security;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "threat-detection", 
       description = "Real-time threat detection and automated response")
public class ThreatDetectionService {
    
    @Action(description = "Detect and respond to security threats in real-time")
    public String detectAndRespondToThreats(
        @Parameter(description = "System telemetry data") TelemetryData telemetryData,
        @Parameter(description = "Response mode (MONITOR, ALERT, BLOCK)") String responseMode) {
        
        try {
            ThreatAnalysisResult analysis = performThreatAnalysis(telemetryData);
            
            List<DetectedThreat> threats = analysis.getDetectedThreats();
            ThreatResponseResult responseResult = new ThreatResponseResult();
            
            for (DetectedThreat threat : threats) {
                ThreatResponse response = determineThreatResponse(threat, responseMode);
                
                switch (threat.getSeverity()) {
                    case CRITICAL:
                        // Immediate blocking and alerting
                        securityOrchestrator.blockThreatSource(threat.getSource());
                        incidentResponseService.triggerEmergencyResponse(threat);
                        notificationService.alertSecurityTeam("Critical threat detected", threat);
                        responseResult.addResponse(threat, "BLOCKED_AND_ALERTED");
                        break;
                        
                    case HIGH:
                        // Enhanced monitoring and alerting
                        monitoringService.enhanceMonitoring(threat.getSource());
                        notificationService.alertSecurityTeam("High severity threat", threat);
                        if ("BLOCK".equals(responseMode)) {
                            securityOrchestrator.blockThreatSource(threat.getSource());
                            responseResult.addResponse(threat, "BLOCKED");
                        } else {
                            responseResult.addResponse(threat, "MONITORED_AND_ALERTED");
                        }
                        break;
                        
                    case MEDIUM:
                        // Logging and monitoring
                        auditLogger.logThreatDetection(threat);
                        if ("ALERT".equals(responseMode) || "BLOCK".equals(responseMode)) {
                            notificationService.notifySecurityAnalyst("Medium threat detected", threat);
                            responseResult.addResponse(threat, "ANALYST_NOTIFIED");
                        } else {
                            responseResult.addResponse(threat, "LOGGED");
                        }
                        break;
                        
                    case LOW:
                        // Logging only
                        auditLogger.logThreatDetection(threat);
                        responseResult.addResponse(threat, "LOGGED");
                        break;
                }
            }
            
            // Update threat intelligence
            threatIntelligenceService.updateThreatDatabase(threats);
            
            return formatThreatResponseResult(responseResult);
            
        } catch (Exception e) {
            log.error("Threat detection and response failed", e);
            return "Error in threat detection: " + e.getMessage();
        }
    }
    
    @Action(description = "Analyze patterns for emerging threats")
    public String analyzeEmergingThreats(
        @Parameter(description = "Time window for analysis (hours)") int timeWindowHours,
        @Parameter(description = "Analysis depth (BASIC, ADVANCED, COMPREHENSIVE)") String analysisDepth) {
        
        try {
            // Collect telemetry data for analysis period
            TelemetryDataSet telemetrySet = telemetryCollector.collectData(timeWindowHours);
            
            // Perform pattern analysis
            PatternAnalysisResult patternResult = patternAnalyzer.analyzePatterns(
                telemetrySet, AnalysisDepth.valueOf(analysisDepth.toUpperCase()));
            
            // Identify emerging threats
            List<EmergingThreat> emergingThreats = threatAnalyzer.identifyEmergingThreats(patternResult);
            
            // Assess threat potential
            for (EmergingThreat threat : emergingThreats) {
                ThreatAssessment assessment = threatAssessor.assessThreat(threat);
                threat.setAssessment(assessment);
                
                if (assessment.getPotentialSeverity().ordinal() >= ThreatSeverity.HIGH.ordinal()) {
                    // Proactive measures for high-potential threats
                    preemptiveSecurityService.implementPreventiveMeasures(threat);
                    notificationService.alertThreatAnalysts("Emerging high-severity threat", threat);
                }
            }
            
            // Update threat models
            threatModelingService.updateThreatModels(emergingThreats);
            
            // Generate threat intelligence report
            ThreatIntelligenceReport report = reportGenerator.generateThreatReport(
                emergingThreats, patternResult);
            
            return formatThreatIntelligenceReport(report);
            
        } catch (Exception e) {
            log.error("Emerging threat analysis failed", e);
            return "Error analyzing emerging threats: " + e.getMessage();
        }
    }
}
```

## 6. Audit and Compliance

### 6.1 Comprehensive Audit Service
```java
package io.wingie.compliance;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "audit-compliance", 
       description = "Comprehensive audit trails and compliance monitoring")
public class AuditComplianceService {
    
    @Action(description = "Generate comprehensive audit report for compliance")
    public String generateAuditReport(
        @Parameter(description = "Audit period start date") String startDate,
        @Parameter(description = "Audit period end date") String endDate,
        @Parameter(description = "Compliance framework (GDPR, HIPAA, SOX, etc.)") String framework) {
        
        try {
            AuditPeriod period = new AuditPeriod(LocalDate.parse(startDate), LocalDate.parse(endDate));
            ComplianceFramework complianceFramework = ComplianceFramework.valueOf(framework.toUpperCase());
            
            // Collect audit data
            AuditDataCollection auditData = auditDataCollector.collectAuditData(period);
            
            // Analyze for compliance violations
            ComplianceAnalysisResult complianceAnalysis = complianceAnalyzer.analyzeCompliance(
                auditData, complianceFramework);
            
            // Generate detailed audit report
            AuditReport auditReport = AuditReport.builder()
                .auditPeriod(period)
                .complianceFramework(complianceFramework)
                .auditData(auditData)
                .complianceAnalysis(complianceAnalysis)
                .build();
            
            // Add executive summary
            ExecutiveSummary summary = generateExecutiveSummary(auditReport);
            auditReport.setExecutiveSummary(summary);
            
            // Add detailed findings
            List<ComplianceFinding> findings = generateDetailedFindings(auditReport);
            auditReport.setFindings(findings);
            
            // Add recommendations
            List<ComplianceRecommendation> recommendations = generateRecommendations(findings);
            auditReport.setRecommendations(recommendations);
            
            // Store audit report
            String reportId = auditReportRepository.saveReport(auditReport);
            
            // Notify relevant stakeholders
            notificationService.notifyAuditCompletion(auditReport, reportId);
            
            return String.format("Audit report generated successfully. Report ID: %s. " +
                "Summary: %d violations found, %d recommendations provided. " +
                "Overall compliance score: %.1f%%", 
                reportId, findings.size(), recommendations.size(), 
                complianceAnalysis.getOverallComplianceScore());
                
        } catch (Exception e) {
            log.error("Audit report generation failed", e);
            return "Error generating audit report: " + e.getMessage();
        }
    }
    
    @Action(description = "Monitor real-time compliance violations")
    public String monitorComplianceViolations(
        @Parameter(description = "Monitoring session duration (hours)") int durationHours,
        @Parameter(description = "Violation threshold (LOW, MEDIUM, HIGH)") String threshold) {
        
        try {
            ComplianceMonitoringSession session = complianceMonitor.startMonitoring(
                durationHours, ViolationThreshold.valueOf(threshold.toUpperCase()));
            
            // Set up real-time monitoring
            CompletableFuture.runAsync(() -> {
                while (session.isActive()) {
                    try {
                        // Check for compliance violations
                        List<ComplianceViolation> violations = complianceDetector.detectViolations();
                        
                        for (ComplianceViolation violation : violations) {
                            if (violation.getSeverity().ordinal() >= session.getThreshold().ordinal()) {
                                // Log violation
                                auditLogger.logComplianceViolation(violation);
                                
                                // Immediate remediation for high-severity violations
                                if (violation.getSeverity() == ViolationSeverity.HIGH) {
                                    remediationService.initiateImmediateRemediation(violation);
                                    notificationService.alertComplianceOfficer("High-severity violation", violation);
                                }
                                
                                // Track violation metrics
                                metricsCollector.recordViolation(violation);
                            }
                        }
                        
                        Thread.sleep(60000); // Check every minute
                        
                    } catch (Exception e) {
                        log.error("Error during compliance monitoring", e);
                    }
                }
            });
            
            return String.format("Compliance monitoring started. Session ID: %s. " +
                "Monitoring for %d hours with %s threshold.", 
                session.getSessionId(), durationHours, threshold);
                
        } catch (Exception e) {
            log.error("Failed to start compliance monitoring", e);
            return "Error starting compliance monitoring: " + e.getMessage();
        }
    }
}
```

## 7. Testing and Validation

### 7.1 Security Testing Framework
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "security.testing.enabled=true",
    "risk.assessment.test-mode=true"
})
class SecurityIntegrationTest {
    
    @Autowired
    private TravelBookingService travelBookingService;
    
    @Autowired
    private RiskAssessmentService riskAssessmentService;
    
    @Test
    @WithMockUser(roles = {"BOOKING_AGENT"})
    void testMediumRiskActionRequiresAuthorization() {
        FlightReservationRequest request = createTestReservationRequest();
        
        String result = travelBookingService.holdFlightReservation(request);
        
        assertThat(result).contains("Reservation held successfully");
        verify(auditLogger).logReservationHold(any(), eq(request), any());
    }
    
    @Test
    @WithMockUser(roles = {"USER"})
    void testHighRiskActionBlockedForInsufficientRole() {
        PaymentBookingRequest request = createTestPaymentRequest();
        
        assertThrows(AccessDeniedException.class, () -> {
            travelBookingService.processPaymentAndConfirmBooking(request);
        });
    }
    
    @Test
    void testThreatDetectionAndResponse() {
        TelemetryData suspiciousTelemetry = createSuspiciousTelemetryData();
        
        String result = threatDetectionService.detectAndRespondToThreats(
            suspiciousTelemetry, "BLOCK");
        
        assertThat(result).contains("BLOCKED");
        verify(securityOrchestrator).blockThreatSource(any());
    }
}
```

## 8. Best Practices

### 8.1 Risk Management Guidelines
- **Layered Security**: Implement multiple security controls for defense in depth
- **Principle of Least Privilege**: Grant minimum necessary permissions
- **Continuous Monitoring**: Monitor actions and contexts in real-time
- **Human Oversight**: Require human approval for high-risk actions
- **Audit Everything**: Maintain comprehensive audit trails

### 8.2 Compliance Considerations
- **Regulatory Requirements**: Understand applicable regulations (GDPR, HIPAA, etc.)
- **Data Protection**: Implement proper data handling and privacy controls
- **Incident Response**: Have clear procedures for security incidents
- **Regular Assessments**: Conduct periodic security and compliance assessments

### 8.3 Performance Optimization
- **Risk Caching**: Cache risk assessments for similar contexts
- **Async Processing**: Use asynchronous processing for non-blocking security checks
- **Efficient Logging**: Optimize audit logging for performance
- **Resource Management**: Monitor and manage security service resource usage

## 9. Conclusion

The a2acore framework provides comprehensive risk management capabilities for building secure AI-powered automation systems. Key benefits include:

### 9.1 Security Features
- **Multi-level Risk Classification**: Granular risk assessment and control
- **Human-in-the-Loop Integration**: Structured approval workflows
- **Real-time Threat Detection**: Proactive security monitoring
- **Comprehensive Auditing**: Complete action and decision tracking

### 9.2 Compliance Benefits
- **Framework Agnostic**: Support for multiple compliance frameworks
- **Automated Monitoring**: Real-time compliance violation detection
- **Detailed Reporting**: Comprehensive audit and compliance reports
- **Remediation Support**: Automated and manual remediation capabilities

### 9.3 Production Readiness
- **Enterprise Integration**: Integration with enterprise security systems
- **Scalable Architecture**: Support for high-volume, high-frequency operations
- **Performance Optimization**: Efficient security processing with minimal overhead
- **Extensible Framework**: Easy integration of additional security controls

This risk management framework ensures that AI-powered travel automation systems operate safely, securely, and in compliance with applicable regulations while maintaining operational efficiency.