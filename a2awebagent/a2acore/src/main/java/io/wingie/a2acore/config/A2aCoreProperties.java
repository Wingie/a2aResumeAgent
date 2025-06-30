package io.wingie.a2acore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for A2ACore framework.
 * 
 * Allows customization of tool discovery, execution, and caching behavior
 * through application.yml or application.properties.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "a2acore")
public class A2aCoreProperties {
    
    private boolean enabled = true;
    
    private Discovery discovery = new Discovery();
    private Execution execution = new Execution();
    private Cache cache = new Cache();
    private Logging logging = new Logging();
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Discovery getDiscovery() {
        return discovery;
    }
    
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }
    
    public Execution getExecution() {
        return execution;
    }
    
    public void setExecution(Execution execution) {
        this.execution = execution;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public void setCache(Cache cache) {
        this.cache = cache;
    }
    
    public Logging getLogging() {
        return logging;
    }
    
    public void setLogging(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Tool discovery configuration.
     */
    public static class Discovery {
        private List<String> scanPackages = Arrays.asList("io.wingie");
        private boolean enableAutoScan = true;
        private int maxInitializationTimeMs = 5000;
        
        public List<String> getScanPackages() {
            return scanPackages;
        }
        
        public void setScanPackages(List<String> scanPackages) {
            this.scanPackages = scanPackages;
        }
        
        public boolean isEnableAutoScan() {
            return enableAutoScan;
        }
        
        public void setEnableAutoScan(boolean enableAutoScan) {
            this.enableAutoScan = enableAutoScan;
        }
        
        public int getMaxInitializationTimeMs() {
            return maxInitializationTimeMs;
        }
        
        public void setMaxInitializationTimeMs(int maxInitializationTimeMs) {
            this.maxInitializationTimeMs = maxInitializationTimeMs;
        }
    }
    
    /**
     * Tool execution configuration.
     */
    public static class Execution {
        private long defaultTimeoutMs = 30000;
        private int maxConcurrentExecutions = 10;
        private boolean enableAsync = true;
        private boolean enableValidation = true;
        
        public long getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }
        
        public void setDefaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }
        
        public int getMaxConcurrentExecutions() {
            return maxConcurrentExecutions;
        }
        
        public void setMaxConcurrentExecutions(int maxConcurrentExecutions) {
            this.maxConcurrentExecutions = maxConcurrentExecutions;
        }
        
        public boolean isEnableAsync() {
            return enableAsync;
        }
        
        public void setEnableAsync(boolean enableAsync) {
            this.enableAsync = enableAsync;
        }
        
        public boolean isEnableValidation() {
            return enableValidation;
        }
        
        public void setEnableValidation(boolean enableValidation) {
            this.enableValidation = enableValidation;
        }
    }
    
    /**
     * Caching configuration.
     */
    public static class Cache {
        private String provider = "none";
        private boolean enabled = false;
        private boolean enableDescriptionCaching = true;
        private long defaultTtlMs = 3600000; // 1 hour
        
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isEnableDescriptionCaching() {
            return enableDescriptionCaching;
        }
        
        public void setEnableDescriptionCaching(boolean enableDescriptionCaching) {
            this.enableDescriptionCaching = enableDescriptionCaching;
        }
        
        public long getDefaultTtlMs() {
            return defaultTtlMs;
        }
        
        public void setDefaultTtlMs(long defaultTtlMs) {
            this.defaultTtlMs = defaultTtlMs;
        }
    }
    
    /**
     * Logging configuration.
     */
    public static class Logging {
        private boolean enableMetrics = true;
        private boolean logToolDiscovery = true;
        private boolean logToolExecution = true;
        private boolean logPerformanceMetrics = true;
        
        public boolean isEnableMetrics() {
            return enableMetrics;
        }
        
        public void setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }
        
        public boolean isLogToolDiscovery() {
            return logToolDiscovery;
        }
        
        public void setLogToolDiscovery(boolean logToolDiscovery) {
            this.logToolDiscovery = logToolDiscovery;
        }
        
        public boolean isLogToolExecution() {
            return logToolExecution;
        }
        
        public void setLogToolExecution(boolean logToolExecution) {
            this.logToolExecution = logToolExecution;
        }
        
        public boolean isLogPerformanceMetrics() {
            return logPerformanceMetrics;
        }
        
        public void setLogPerformanceMetrics(boolean logPerformanceMetrics) {
            this.logPerformanceMetrics = logPerformanceMetrics;
        }
    }
    
    @Override
    public String toString() {
        return "A2aCoreProperties{" +
                "enabled=" + enabled +
                ", discovery=" + discovery +
                ", execution=" + execution +
                ", cache=" + cache +
                '}';
    }
}