package io.wingie.entity;

public enum EvaluationStatus {
    QUEUED("Waiting in queue"),
    RUNNING("Currently executing"),
    COMPLETED("Successfully completed"),
    FAILED("Failed with error"),
    CANCELLED("Cancelled by user");
    
    private final String description;
    
    EvaluationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    public boolean isActive() {
        return this == QUEUED || this == RUNNING;
    }
    
    public String getCssClass() {
        return switch (this) {
            case QUEUED -> "status-queued";
            case RUNNING -> "status-running";
            case COMPLETED -> "status-completed";
            case FAILED -> "status-failed";
            case CANCELLED -> "status-cancelled";
        };
    }
    
    public String getBootstrapClass() {
        return switch (this) {
            case QUEUED -> "badge bg-secondary";
            case RUNNING -> "badge bg-primary";
            case COMPLETED -> "badge bg-success";
            case FAILED -> "badge bg-danger";
            case CANCELLED -> "badge bg-warning";
        };
    }
    
    public String getIcon() {
        return switch (this) {
            case QUEUED -> "fas fa-clock";
            case RUNNING -> "fas fa-spinner fa-spin";
            case COMPLETED -> "fas fa-check-circle";
            case FAILED -> "fas fa-exclamation-circle";
            case CANCELLED -> "fas fa-ban";
        };
    }
}