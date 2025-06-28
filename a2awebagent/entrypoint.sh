#!/bin/sh

# Function to find an available display number
find_free_display() {
    local display_num=99
    local max_attempts=10
    
    while [ $display_num -lt $((99 + $max_attempts)) ]; do
        # Check if lock file exists
        if [ ! -e "/tmp/.X${display_num}-lock" ] && [ ! -e "/tmp/.X11-unix/X${display_num}" ]; then
            echo $display_num
            return 0
        fi
        
        # Try to clean up stale lock files
        echo "Display :${display_num} appears to be in use, checking if it's stale..."
        if ! xdpyinfo -display :${display_num} >/dev/null 2>&1; then
            echo "Display :${display_num} is stale, cleaning up..."
            rm -f "/tmp/.X${display_num}-lock" "/tmp/.X11-unix/X${display_num}" 2>/dev/null || true
            echo $display_num
            return 0
        fi
        
        display_num=$((display_num + 1))
    done
    
    echo "ERROR: Could not find free display after $max_attempts attempts"
    return 1
}

# Clean up function for graceful shutdown
cleanup() {
    echo "Shutting down..."
    if [ ! -z "$XVFB_PID" ]; then
        echo "Stopping Xvfb (PID: $XVFB_PID)"
        kill -TERM $XVFB_PID 2>/dev/null || true
    fi
    # Clean up our display lock files
    if [ ! -z "$DISPLAY_NUM" ]; then
        rm -f "/tmp/.X${DISPLAY_NUM}-lock" "/tmp/.X11-unix/X${DISPLAY_NUM}" 2>/dev/null || true
    fi
    exit 0
}

# Set up signal handlers
trap cleanup TERM INT EXIT

# Clean up any stale X11 resources from previous runs
echo "Cleaning up stale X11 resources..."
rm -f /tmp/.X*-lock /tmp/.X11-unix/X* 2>/dev/null || true

# Find an available display number
echo "Finding available display number..."
DISPLAY_NUM=$(find_free_display)
if [ $? -ne 0 ]; then
    echo "ERROR: Could not find available display"
    exit 1
fi

echo "Using display :${DISPLAY_NUM}"

# Start Xvfb in the background for headless Chrome
echo "Starting Xvfb on display :${DISPLAY_NUM}..."
Xvfb :${DISPLAY_NUM} -screen 0 1920x1080x24 -ac +extension GLX +render -noreset &
XVFB_PID=$!

# Wait for Xvfb to start and verify it's running
echo "Waiting for Xvfb to start..."
for i in $(seq 1 10); do
    if xdpyinfo -display :${DISPLAY_NUM} >/dev/null 2>&1; then
        echo "Xvfb started successfully on display :${DISPLAY_NUM}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "ERROR: Xvfb failed to start after 10 seconds!"
        exit 1
    fi
    sleep 1
done

# Export DISPLAY variable
export DISPLAY=:${DISPLAY_NUM}
echo "DISPLAY set to :${DISPLAY_NUM}"

# Verify X11 display is available
echo "Verifying X11 display :${DISPLAY_NUM}..."
if xdpyinfo -display :${DISPLAY_NUM} >/dev/null 2>&1; then
    echo "X11 display :${DISPLAY_NUM} is available and working"
    # Get display info for debugging
    echo "Display dimensions: $(xdpyinfo -display :${DISPLAY_NUM} | grep dimensions | head -1)"
else 
    echo "ERROR: X11 display :${DISPLAY_NUM} is not available!"
    exit 1
fi

# Test if Chrome/Chromium is available and can run
echo "Testing Chrome/Chromium availability..."
if command -v google-chrome >/dev/null 2>&1; then
    echo "Found Google Chrome:"
    google-chrome --version
    # Test Chrome can start in headless mode
    echo "Testing Chrome headless mode..."
    if google-chrome --headless --disable-gpu --no-sandbox --disable-dev-shm-usage --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chrome headless mode test: PASSED"
    else
        echo "Chrome headless mode test: FAILED"
    fi
elif command -v chromium >/dev/null 2>&1; then
    echo "Found Chromium:"
    chromium --version
    # Test Chromium can start in headless mode  
    echo "Testing Chromium headless mode..."
    if chromium --headless --disable-gpu --no-sandbox --disable-dev-shm-usage --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chromium headless mode test: PASSED"
    else
        echo "Chromium headless mode test: FAILED"
    fi
else
    echo "ERROR: Neither Chrome nor Chromium found!"
    exit 1
fi

# Create cache directories for WebDriverManager
echo "Creating WebDriverManager cache directories..."
mkdir -p /app/seleniumCache /app/resolutionCache

# Show cache directory contents for debugging
echo "Cache directory contents:"
ls -la /app/seleniumCache /app/resolutionCache 2>/dev/null || echo "Cache directories are empty (expected on first run)"

# Start the Spring Boot application with WebDriverManager cache configuration
echo "Starting Spring Boot application with WebDriverManager cache configuration..."
exec java \
  $JAVA_OPTS \
  -Djava.security.egd=file:/dev/./urandom \
  -Dwdm.cachePath=/app/seleniumCache \
  -Dwdm.resolutionCachePath=/app/resolutionCache \
  -jar /app/target/a2awebagent-0.0.1.jar
