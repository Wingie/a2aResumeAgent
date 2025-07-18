# Async + Transactional Anti-Pattern Fix Summary

## Problem
The combination of `@Async` and `@Transactional` annotations on the same method can cause:
- Transaction context issues
- Database connection leaks
- Unpredictable transaction boundaries
- Potential deadlocks

## Files Fixed

### 1. ToolDescriptionCacheService.java
**Method**: `updateUsageStatsAsync(Long descriptionId)`
- **Original**: Combined `@Async` and `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- **Fixed**: Separated into:
  - `updateUsageStatsAsync()` - Async wrapper method
  - `updateUsageStatsById()` - Transactional method with REQUIRES_NEW propagation

### 2. TaskGraphService.java
**Method**: `logTaskToGraph(TaskExecution taskExecution)`
- **Original**: Combined `@Async("taskExecutor")` and `@Transactional`
- **Fixed**: Separated into:
  - `logTaskToGraph()` - Async wrapper method
  - `performGraphLogging()` - Transactional method

### 3. ScreenshotEmbeddingService.java
**Method**: `processScreenshotEmbedding(String screenshotId, String screenshotUrl)`
- **Original**: Combined `@Async("taskExecutor")` and `@Transactional`
- **Fixed**: Separated into:
  - `processScreenshotEmbedding()` - Async wrapper method
  - `performEmbeddingProcessing()` - Transactional method

## Pattern Applied
For each fix, we:
1. Kept the async method as a public entry point
2. Created a new protected/public transactional method
3. The async method delegates to the transactional method
4. Proper exception handling maintained in both methods

## Benefits
- Clear separation of concerns
- Predictable transaction boundaries
- No risk of connection leaks
- Maintains original functionality while fixing the anti-pattern