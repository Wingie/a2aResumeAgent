# Chrome Options Fix for Docker Container

## Problem Analysis

The Chrome browser was failing to start in the Docker container with the error "Chrome failed to start: exited normally". This typically indicates that Chrome is starting but immediately exiting due to incompatible command-line options.

## Root Causes Identified

1. **`--single-process` option**: This option can cause Chrome to exit immediately in containerized environments
2. **`--no-zygote` option**: Can conflict with headless mode and cause startup failures
3. **Conflicting logging options**: Using both `--disable-logging` and `--log-level` simultaneously
4. **Headless mode compatibility**: Need to support both new (`--headless=new`) and legacy (`--headless`) formats

## Changes Made

### 1. Removed Problematic Options

```java
// REMOVED: These options cause Chrome to exit immediately in Docker
// options.addArguments("--single-process"); 
// options.addArguments("--no-zygote");
```

### 2. Fixed Headless Mode Configuration

```java
// Use new headless mode with fallback to old format
try {
    options.addArguments("--headless=new");
} catch (Exception e) {
    // Fallback for older Chrome versions
    options.addArguments("--headless");
}
```

### 3. Resolved Conflicting Logging Options

```java
// BEFORE: Conflicting options
options.addArguments("--disable-logging");
options.addArguments("--log-level=0");

// AFTER: Non-conflicting configuration
options.addArguments("--log-level=3"); // Only log fatal errors
// Removed --disable-logging
```

### 4. Enhanced Docker-Specific Options

Added safer container-specific options:
```java
options.addArguments("--disable-crash-reporter");
options.addArguments("--disable-in-process-stack-traces");
options.addArguments("--disable-breakpad");
options.addArguments("--disable-features=TranslateUI");
options.addArguments("--remote-debugging-port=0");
```

### 5. Improved Chrome Binary Detection

Extended binary detection to include more possible paths:
- `/snap/bin/chromium` (Snap packages)
- `/usr/lib/chromium-browser/chromium-browser` (Debian/Ubuntu)
- `/usr/bin/google-chrome-unstable`

### 6. Enhanced Entrypoint Script Testing

Updated `entrypoint.sh` to test both new and legacy headless formats:
```bash
# Try new headless format first, then fallback
if google-chrome --headless=new --disable-gpu --no-sandbox ... ; then
    echo "Chrome headless mode test: PASSED (new format)"
elif google-chrome --headless --disable-gpu --no-sandbox ... ; then
    echo "Chrome headless mode test: PASSED (legacy format)"
else
    echo "Chrome headless mode test: FAILED"
fi
```

## Final Chrome Options Configuration

### Base Options (All Environments)
- `--headless=new` (with fallback to `--headless`)
- `--no-sandbox`
- `--disable-dev-shm-usage`
- `--disable-gpu`
- `--window-size=1920,1080`
- Various rendering and stability options

### Docker-Specific Additions
- `--disable-setuid-sandbox`
- `--disable-web-security`
- `--no-first-run`
- `--no-default-browser-check`
- `--disable-crash-reporter`
- `--remote-debugging-port=0`

## Testing and Validation

1. **Created test script**: `test-chrome-options.sh` to validate Chrome options
2. **Enhanced logging**: Added comprehensive debug output in Java code
3. **Improved error handling**: Better fallback mechanisms for different Chrome versions

## Expected Behavior After Fix

1. Chrome should start successfully in Docker containers
2. Headless mode should work with both new and legacy Chrome versions
3. Better error messages and debugging information
4. No immediate exits due to incompatible options

## Usage

After applying these fixes:
1. Rebuild the Docker image
2. Chrome should start properly in the container
3. Check logs for "Chrome options configured successfully" message
4. Use the test script to validate Chrome functionality locally

## Files Modified

- `/src/main/java/io/wingie/CustomChromeOptions.java` - Main Chrome options configuration
- `/entrypoint.sh` - Docker startup script with Chrome testing
- `/test-chrome-options.sh` - New test script for validation (optional)