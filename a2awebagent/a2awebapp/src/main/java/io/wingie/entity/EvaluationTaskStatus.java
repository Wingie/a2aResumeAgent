package io.wingie.entity;

public enum EvaluationTaskStatus {
    PENDING("Waiting to be executed"),
    RUNNING("Currently executing"),
    COMPLETED("Successfully completed"),
    FAILED("Failed with error"),
    SKIPPED("Skipped due to dependency"),
    TIMEOUT("Execution timed out");
    
    private final String description;
    
    EvaluationTaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == SKIPPED || this == TIMEOUT;
    }
    
    public boolean isActive() {
        return this == PENDING || this == RUNNING;
    }
    
    public String getCssClass() {
        return switch (this) {
            case PENDING -> "status-pending";
            case RUNNING -> "status-running";
            case COMPLETED -> "status-completed";
            case FAILED -> "status-failed";
            case SKIPPED -> "status-skipped";
            case TIMEOUT -> "status-timeout";
        };
    }
    
    public String getBootstrapClass() {
        return switch (this) {
            case PENDING -> "badge bg-secondary";
            case RUNNING -> "badge bg-primary";
            case COMPLETED -> "badge bg-success";
            case FAILED -> "badge bg-danger";
            case SKIPPED -> "badge bg-warning";
            case TIMEOUT -> "badge bg-dark";
        };
    }
    
    public String getIcon() {
        return switch (this) {
            case PENDING -> "fas fa-clock";
            case RUNNING -> "fas fa-spinner fa-spin";
            case COMPLETED -> "fas fa-check-circle";
            case FAILED -> "fas fa-exclamation-circle";
            case SKIPPED -> "fas fa-forward";
            case TIMEOUT -> "fas fa-hourglass-end";
        };
    }
}