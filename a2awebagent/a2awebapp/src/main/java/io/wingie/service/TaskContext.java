package io.wingie.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context for tracking current task execution across LLM calls.
 * 
 * This utility provides a way to correlate LLM API calls with the TaskExecution
 * that triggered them, enabling comprehensive tracking of agent workflows.
 */
@Slf4j
public class TaskContext {
    
    private static final ThreadLocal<String> currentTaskId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
    
    /**
     * Set the current task execution ID for this thread
     */
    public static void setCurrentTaskId(String taskId) {
        currentTaskId.set(taskId);
        log.debug("Set task context: {}", taskId);
    }
    
    /**
     * Get the current task execution ID for this thread
     */
    public static String getCurrentTaskId() {
        return currentTaskId.get();
    }
    
    /**
     * Set the current session ID for this thread
     */
    public static void setCurrentSessionId(String sessionId) {
        currentSessionId.set(sessionId);
        log.debug("Set session context: {}", sessionId);
    }
    
    /**
     * Get the current session ID for this thread
     */
    public static String getCurrentSessionId() {
        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            // Generate a session ID based on thread if none set
            sessionId = "session-" + Thread.currentThread().getId();
        }
        return sessionId;
    }
    
    /**
     * Set the current user ID for this thread
     */
    public static void setCurrentUserId(String userId) {
        currentUserId.set(userId);
        log.debug("Set user context: {}", userId);
    }
    
    /**
     * Get the current user ID for this thread
     */
    public static String getCurrentUserId() {
        return currentUserId.get();
    }
    
    /**
     * Clear all context for the current thread (call when task completes)
     */
    public static void clear() {
        String taskId = currentTaskId.get();
        currentTaskId.remove();
        currentSessionId.remove();
        currentUserId.remove();
        log.debug("Cleared task context: {}", taskId);
    }
    
    /**
     * Set complete context for a task execution
     */
    public static void setContext(String taskId, String sessionId, String userId) {
        setCurrentTaskId(taskId);
        setCurrentSessionId(sessionId);
        setCurrentUserId(userId);
    }
    
    /**
     * Check if there's an active task context
     */
    public static boolean hasActiveTask() {
        return getCurrentTaskId() != null;
    }
    
    /**
     * Get a summary of the current context for logging
     */
    public static String getContextSummary() {
        return String.format("Task: %s, Session: %s, User: %s", 
            getCurrentTaskId(), 
            getCurrentSessionId(), 
            getCurrentUserId());
    }
    
    /**
     * Execute a runnable with a specific task context
     */
    public static void executeWithContext(String taskId, String sessionId, String userId, Runnable runnable) {
        String previousTaskId = getCurrentTaskId();
        String previousSessionId = getCurrentSessionId();
        String previousUserId = getCurrentUserId();
        
        try {
            setContext(taskId, sessionId, userId);
            runnable.run();
        } finally {
            // Restore previous context
            if (previousTaskId != null) {
                setCurrentTaskId(previousTaskId);
            } else {
                currentTaskId.remove();
            }
            
            if (previousSessionId != null) {
                setCurrentSessionId(previousSessionId);
            } else {
                currentSessionId.remove();
            }
            
            if (previousUserId != null) {
                setCurrentUserId(previousUserId);
            } else {
                currentUserId.remove();
            }
        }
    }
}