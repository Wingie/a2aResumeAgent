package io.wingie.a2acore.discovery;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.config.A2aCoreProperties;
import io.wingie.a2acore.domain.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast reflection-based tool discovery service.
 * 
 * Scans Spring ApplicationContext for @Action annotated methods and creates
 * Tool definitions WITHOUT making any AI calls - achieving <100ms discovery time.
 * 
 * This replaces the slow AI-based tool description generation from tools4ai
 * with immediate static analysis of annotations.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Service
public class ToolDiscoveryService {
    
    private static final Logger log = LoggerFactory.getLogger(ToolDiscoveryService.class);
    
    private final ApplicationContext applicationContext;
    private final MethodToolBuilder methodToolBuilder;
    private final A2aCoreProperties properties;
    
    public ToolDiscoveryService(ApplicationContext applicationContext, 
                               MethodToolBuilder methodToolBuilder,
                               A2aCoreProperties properties) {
        this.applicationContext = applicationContext;
        this.methodToolBuilder = methodToolBuilder;
        this.properties = properties;
    }
    
    /**
     * Discovers all tools by scanning for @Action annotated methods.
     * Target performance: <100ms for typical applications.
     * 
     * @return List of discovered tools
     */
    public List<Tool> discoverTools() {
        long startTime = System.currentTimeMillis();
        List<Tool> tools = new ArrayList<>();
        
        if (!properties.getDiscovery().isEnableAutoScan()) {
            log.info("Tool auto-discovery is disabled");
            return tools;
        }
        
        try {
            // Get all Spring beans (this is typically fast - <10ms)
            Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
            
            log.debug("Scanning {} Spring beans for @Action methods", beans.size());
            
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                String beanName = entry.getKey();
                Object bean = entry.getValue();
                
                // Skip a2acore internal beans to avoid conflicts
                if (beanName.startsWith("a2acore") || beanName.contains("A2aCore")) {
                    continue;
                }
                
                List<Tool> beanTools = scanBeanForTools(bean, beanName);
                tools.addAll(beanTools);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (properties.getLogging().isLogToolDiscovery()) {
                log.info("Discovered {} tools in {}ms from {} beans", 
                    tools.size(), duration, beans.size());
                
                if (properties.getLogging().isLogPerformanceMetrics()) {
                    logPerformanceMetrics(duration, tools.size(), beans.size());
                }
            }
            
            // Validate performance target
            if (duration > properties.getDiscovery().getMaxInitializationTimeMs()) {
                log.warn("Tool discovery took {}ms, exceeding target of {}ms", 
                    duration, properties.getDiscovery().getMaxInitializationTimeMs());
            }
            
            return tools;
            
        } catch (Exception e) {
            log.error("Error during tool discovery", e);
            throw new RuntimeException("Tool discovery failed", e);
        }
    }
    
    /**
     * Discovers all tools AND their corresponding Spring bean instances.
     * 
     * This unified method ensures atomic discovery of both Tool metadata and
     * the Spring bean instances needed for tool execution, preventing the
     * tool/bean mapping inconsistencies that cause "No bean found" errors.
     * 
     * Target performance: <100ms for typical applications.
     * 
     * @return ToolDiscoveryResult containing both tools and beans
     */
    public ToolDiscoveryResult discoverToolsAndBeans() {
        long startTime = System.currentTimeMillis();
        
        ToolBeanScanResult aggregateResult = new ToolBeanScanResult();
        
        if (!properties.getDiscovery().isEnableAutoScan()) {
            log.info("Tool auto-discovery is disabled");
            return new ToolDiscoveryResult(aggregateResult.getTools(), aggregateResult.getToolBeans());
        }
        
        try {
            // Get all Spring beans (this is typically fast - <10ms)
            Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
            
            log.debug("Scanning {} Spring beans for @Action methods and bean instances", beans.size());
            
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                String beanName = entry.getKey();
                Object bean = entry.getValue();
                
                // Skip a2acore internal beans to avoid conflicts
                if (beanName.startsWith("a2acore") || beanName.contains("A2aCore")) {
                    continue;
                }
                
                ToolBeanScanResult beanResult = scanBeanForToolsAndBeans(bean, beanName);
                aggregateResult.addAll(beanResult);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Create final result
            ToolDiscoveryResult result = new ToolDiscoveryResult(
                aggregateResult.getTools(), 
                aggregateResult.getToolBeans()
            );
            
            if (properties.getLogging().isLogToolDiscovery()) {
                log.info("Discovered {} tools with {} beans in {}ms from {} beans scanned", 
                    result.getToolCount(), result.getBeanCount(), duration, beans.size());
                
                // Validate consistency
                if (!result.isConsistent()) {
                    log.error("CRITICAL: Tool/Bean discovery inconsistency - {}", result.getSummary());
                    throw new RuntimeException("Tool and bean discovery results are inconsistent");
                }
                
                log.info("✅ Tool/Bean consistency validated: {}", result.getSummary());
                
                if (properties.getLogging().isLogPerformanceMetrics()) {
                    logUnifiedPerformanceMetrics(duration, result, beans.size());
                }
            }
            
            // Validate performance target
            if (duration > properties.getDiscovery().getMaxInitializationTimeMs()) {
                log.warn("Unified tool discovery took {}ms, exceeding target of {}ms", 
                    duration, properties.getDiscovery().getMaxInitializationTimeMs());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during unified tool and bean discovery", e);
            throw new RuntimeException("Unified tool discovery failed", e);
        }
    }
    
    /**
     * Scans a single Spring bean for @Action methods, returning both Tool metadata and bean mapping.
     */
    private ToolBeanScanResult scanBeanForToolsAndBeans(Object bean, String beanName) {
        ToolBeanScanResult result = new ToolBeanScanResult();
        
        try {
            // Get the actual class (unwrap Spring proxies)
            Class<?> clazz = AopUtils.getTargetClass(bean);
            
            // Check if class is in our scan packages
            if (!isInScanPackages(clazz)) {
                return result;
            }
            
            // Get agent metadata if present
            Agent agentAnnotation = clazz.getAnnotation(Agent.class);
            
            // Scan all declared methods
            Method[] methods = clazz.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(Action.class)) {
                    Action actionAnnotation = method.getAnnotation(Action.class);
                    
                    // Skip disabled actions
                    if (!actionAnnotation.enabled()) {
                        log.debug("Skipping disabled action: {}.{}", clazz.getSimpleName(), method.getName());
                        continue;
                    }
                    
                    try {
                        Tool tool = methodToolBuilder.buildTool(method, bean, actionAnnotation, agentAnnotation);
                        result.addTool(tool, bean);
                        
                        log.debug("Discovered tool '{}' with bean from {}.{}", 
                            tool.getName(), clazz.getSimpleName(), method.getName());
                            
                    } catch (Exception e) {
                        log.error("Failed to build tool for method {}.{}", 
                            clazz.getSimpleName(), method.getName(), e);
                        // Continue processing other methods
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error scanning bean '{}' for tools and beans", beanName, e);
        }
        
        return result;
    }
    
    /**
     * Logs detailed performance metrics for unified discovery optimization.
     */
    private void logUnifiedPerformanceMetrics(long durationMs, ToolDiscoveryResult result, int beanCount) {
        log.info("Unified Tool & Bean Discovery Performance Metrics:");
        log.info("  Total time: {}ms", durationMs);
        log.info("  Tools found: {}", result.getToolCount());
        log.info("  Beans mapped: {}", result.getBeanCount());
        log.info("  Beans scanned: {}", beanCount);
        log.info("  Consistency: {}", result.isConsistent() ? "✅ VALID" : "❌ INVALID");
        log.info("  Avg time per bean: {:.2f}ms", (double) durationMs / beanCount);
        log.info("  Avg time per tool: {:.2f}ms", 
            result.getToolCount() > 0 ? (double) durationMs / result.getToolCount() : 0);
        
        // Performance recommendations
        if (durationMs > 1000) {
            log.warn("Consider reducing scan packages or disabling unused beans for faster startup");
        } else if (durationMs < 50) {
            log.info("Excellent unified discovery performance - under 50ms target");
        }
    }
    
    /**
     * Scans a single Spring bean for @Action annotated methods.
     */
    private List<Tool> scanBeanForTools(Object bean, String beanName) {
        List<Tool> tools = new ArrayList<>();
        
        try {
            // Get the actual class (unwrap Spring proxies)
            Class<?> clazz = AopUtils.getTargetClass(bean);
            
            // Check if class is in our scan packages
            if (!isInScanPackages(clazz)) {
                return tools;
            }
            
            // Get agent metadata if present
            Agent agentAnnotation = clazz.getAnnotation(Agent.class);
            
            // Scan all declared methods
            Method[] methods = clazz.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(Action.class)) {
                    Action actionAnnotation = method.getAnnotation(Action.class);
                    
                    // Skip disabled actions
                    if (!actionAnnotation.enabled()) {
                        log.debug("Skipping disabled action: {}.{}", clazz.getSimpleName(), method.getName());
                        continue;
                    }
                    
                    try {
                        Tool tool = methodToolBuilder.buildTool(method, bean, actionAnnotation, agentAnnotation);
                        tools.add(tool);
                        
                        log.debug("Discovered tool '{}' from {}.{}", 
                            tool.getName(), clazz.getSimpleName(), method.getName());
                            
                    } catch (Exception e) {
                        log.error("Failed to build tool for method {}.{}", 
                            clazz.getSimpleName(), method.getName(), e);
                        // Continue processing other methods
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error scanning bean '{}' for tools", beanName, e);
        }
        
        return tools;
    }
    
    /**
     * Checks if a class is within the configured scan packages.
     */
    private boolean isInScanPackages(Class<?> clazz) {
        String className = clazz.getName();
        
        for (String scanPackage : properties.getDiscovery().getScanPackages()) {
            if (className.startsWith(scanPackage)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Logs detailed performance metrics for optimization.
     */
    private void logPerformanceMetrics(long durationMs, int toolCount, int beanCount) {
        log.info("Tool Discovery Performance Metrics:");
        log.info("  Total time: {}ms", durationMs);
        log.info("  Tools found: {}", toolCount);
        log.info("  Beans scanned: {}", beanCount);
        log.info("  Avg time per bean: {:.2f}ms", (double) durationMs / beanCount);
        log.info("  Avg time per tool: {:.2f}ms", toolCount > 0 ? (double) durationMs / toolCount : 0);
        
        // Performance recommendations
        if (durationMs > 1000) {
            log.warn("Consider reducing scan packages or disabling unused beans for faster startup");
        } else if (durationMs < 50) {
            log.info("Excellent discovery performance - under 50ms target");
        }
    }
    
    /**
     * Gets discovery statistics for monitoring and debugging.
     */
    public DiscoveryStatistics getStatistics() {
        // This could be enhanced to track historical performance
        return new DiscoveryStatistics();
    }
    
    /**
     * Statistics about tool discovery performance.
     */
    public static class DiscoveryStatistics {
        private int lastToolCount = 0;
        private long lastDiscoveryTimeMs = 0;
        private int lastBeanCount = 0;
        
        // Getters and setters would be here
        public int getLastToolCount() { return lastToolCount; }
        public long getLastDiscoveryTimeMs() { return lastDiscoveryTimeMs; }
        public int getLastBeanCount() { return lastBeanCount; }
    }
}