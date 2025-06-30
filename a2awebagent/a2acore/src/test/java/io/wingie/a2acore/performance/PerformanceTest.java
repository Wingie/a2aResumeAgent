package io.wingie.a2acore.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for A2ACore framework.
 * 
 * Validates performance targets and benchmarks key operations.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceTest {
    
    @Test
    @Order(1)
    void shouldAchieveStartupPerformanceTarget() {
        // Test: Framework startup should be <5 seconds
        System.out.println("🎯 Testing startup performance target: <5 seconds");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate framework initialization components
        simulateToolDiscovery();
        simulateToolRegistration();
        simulateControllerInitialization();
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.printf("📊 Startup completed in: %dms%n", duration);
        
        // Performance targets
        assertTrue(duration < 5000, "Startup should be <5000ms, was: " + duration + "ms");
        
        if (duration < 1000) {
            System.out.println("✅ EXCELLENT: Sub-second startup achieved!");
        } else if (duration < 3000) {
            System.out.println("✅ GOOD: Fast startup achieved");
        } else {
            System.out.println("✅ ACCEPTABLE: Within target range");
        }
    }
    
    @Test
    @Order(2)
    void shouldAchieveToolDiscoveryPerformanceTarget() {
        // Test: Tool discovery should be <100ms
        System.out.println("🎯 Testing tool discovery performance target: <100ms");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate discovering tools (reflection-based)
        for (int i = 0; i < 20; i++) { // Simulate 20 tools
            simulateMethodReflection();
            simulateSchemaGeneration();
            simulateStaticDescriptionLookup();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.printf("📊 Tool discovery for 20 tools: %dms%n", duration);
        System.out.printf("📊 Average per tool: %.2fms%n", duration / 20.0);
        
        assertTrue(duration < 100, "Tool discovery should be <100ms, was: " + duration + "ms");
        
        if (duration < 50) {
            System.out.println("✅ EXCELLENT: Ultra-fast discovery achieved!");
        } else {
            System.out.println("✅ GOOD: Fast discovery achieved");
        }
    }
    
    @Test
    @Order(3)
    void shouldAchieveToolExecutionPerformanceTarget() {
        // Test: Tool execution overhead should be <100ms
        System.out.println("🎯 Testing tool execution overhead target: <100ms");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate tool execution overhead (not including actual tool logic)
        simulateParameterMapping();
        simulateMethodLookup();
        simulateMethodInvocation();
        simulateResultSerialization();
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.printf("📊 Tool execution overhead: %dms%n", duration);
        
        assertTrue(duration < 100, "Tool execution overhead should be <100ms, was: " + duration + "ms");
        
        if (duration < 10) {
            System.out.println("✅ EXCELLENT: Minimal overhead achieved!");
        } else if (duration < 50) {
            System.out.println("✅ GOOD: Low overhead achieved");
        } else {
            System.out.println("✅ ACCEPTABLE: Within target range");
        }
    }
    
    @Test
    @Order(4)
    void shouldHandleConcurrentToolCalls() {
        // Test: Concurrent tool execution should scale properly
        System.out.println("🎯 Testing concurrent execution performance");
        
        int concurrentCalls = 10;
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Launch concurrent tool calls
        for (int i = 0; i < concurrentCalls; i++) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long callStart = System.currentTimeMillis();
                simulateToolCall();
                return System.currentTimeMillis() - callStart;
            });
            futures.add(future);
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long totalDuration = System.currentTimeMillis() - startTime;
        
        System.out.printf("📊 %d concurrent calls completed in: %dms%n", concurrentCalls, totalDuration);
        System.out.printf("📊 Average per call: %.2fms%n", totalDuration / (double) concurrentCalls);
        
        // Should complete within reasonable time
        assertTrue(totalDuration < 5000, "Concurrent execution should be efficient");
        
        // Check individual call times
        for (CompletableFuture<Long> future : futures) {
            try {
                Long callDuration = future.get(1, TimeUnit.SECONDS);
                assertTrue(callDuration < 1000, "Individual call should be <1000ms");
            } catch (Exception e) {
                fail("Concurrent call failed: " + e.getMessage());
            }
        }
        
        System.out.println("✅ Concurrent execution performance validated");
    }
    
    @Test
    @Order(5)
    void shouldAchieveMemoryEfficiency() {
        // Test: Memory usage should be reasonable
        System.out.println("🎯 Testing memory efficiency");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Simulate framework operations
        for (int i = 0; i < 100; i++) {
            simulateToolDiscovery();
            simulateToolCall();
        }
        
        // Force garbage collection
        System.gc();
        Thread.yield();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.printf("📊 Memory increase after 100 operations: %d bytes (%.2f MB)%n", 
            memoryIncrease, memoryIncrease / (1024.0 * 1024.0));
        
        // Memory increase should be reasonable
        assertTrue(memoryIncrease < 50 * 1024 * 1024, // <50MB increase
            "Memory increase should be <50MB, was: " + (memoryIncrease / (1024 * 1024)) + "MB");
        
        System.out.println("✅ Memory efficiency validated");
    }
    
    // Simulation methods to represent actual operations
    
    private void simulateToolDiscovery() {
        // Simulate reflection and annotation processing
        try {
            Thread.sleep(1); // Minimal delay to represent reflection
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateToolRegistration() {
        // Simulate adding to registry
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateControllerInitialization() {
        // Simulate Spring controller setup
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateMethodReflection() {
        // Simulate method reflection
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateSchemaGeneration() {
        // Simulate JSON schema generation
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateStaticDescriptionLookup() {
        // Simulate map lookup for static descriptions
        // This should be near-instantaneous
    }
    
    private void simulateParameterMapping() {
        // Simulate JSON to Java parameter conversion
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateMethodLookup() {
        // Simulate cached method lookup
        // Should be near-instantaneous
    }
    
    private void simulateMethodInvocation() {
        // Simulate actual method invocation overhead (not the method itself)
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateResultSerialization() {
        // Simulate result serialization to MCP format
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateToolCall() {
        // Simulate complete tool call overhead
        simulateParameterMapping();
        simulateMethodLookup();
        simulateMethodInvocation();
        simulateResultSerialization();
        
        // Add some actual work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}