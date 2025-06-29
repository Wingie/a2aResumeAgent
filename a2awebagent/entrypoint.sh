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

# Start Xvfb in the background for headless Chrome with enhanced options
echo "Starting Xvfb on display :${DISPLAY_NUM}..."
Xvfb :${DISPLAY_NUM} \
  -screen 0 1920x1080x24 \
  -ac \
  +extension GLX \
  +extension RANDR \
  +extension RENDER \
  +extension MIT-SHM \
  +extension XTEST \
  -dpi 96 \
  -noreset \
  -nolisten tcp \
  -maxclients 1024 &
XVFB_PID=$!

echo "Xvfb started with PID: $XVFB_PID"

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

# Set up XAUTHORITY for enhanced security and compatibility
export XAUTHORITY=/tmp/.X${DISPLAY_NUM}-auth
echo "XAUTHORITY set to: $XAUTHORITY"

# Create XAUTHORITY file with proper permissions
touch $XAUTHORITY
chmod 600 $XAUTHORITY
chown $(id -u):$(id -g) $XAUTHORITY 2>/dev/null || true

# Generate and add X11 authentication cookie for better security
echo "Setting up X11 authentication..."
XAUTH_COOKIE=$(mcookie 2>/dev/null || echo "$(date +%s)$(dd if=/dev/urandom bs=1 count=16 2>/dev/null | xxd -p)")
xauth -f $XAUTHORITY add :${DISPLAY_NUM} . $XAUTH_COOKIE
echo "X11 authentication cookie generated and added"

# Verify X11 display is available with enhanced validation
echo "Verifying X11 display :${DISPLAY_NUM}..."
if xdpyinfo -display :${DISPLAY_NUM} >/dev/null 2>&1; then
    echo "X11 display :${DISPLAY_NUM} is available and working"
    
    # Get comprehensive display info for debugging
    echo "Display dimensions: $(xdpyinfo -display :${DISPLAY_NUM} | grep dimensions | head -1)"
    echo "Display depth: $(xdpyinfo -display :${DISPLAY_NUM} | grep 'depth of root window' | head -1)"
    echo "X11 server vendor: $(xdpyinfo -display :${DISPLAY_NUM} | grep 'vendor string' | head -1)"
    
    # Test basic X11 operations
    echo "Testing basic X11 operations..."
    if xwininfo -root -display :${DISPLAY_NUM} >/dev/null 2>&1; then
        echo "X11 window operations: WORKING"
    else
        echo "X11 window operations: LIMITED (may affect some browser features)"
    fi
else 
    echo "ERROR: X11 display :${DISPLAY_NUM} is not available!"
    echo "Attempting X11 diagnostic tests..."
    
    # Diagnostic information
    echo "DISPLAY variable: $DISPLAY"
    echo "XAUTHORITY: $XAUTHORITY" 
    echo "X11 socket check: $(ls -la /tmp/.X11-unix/ 2>/dev/null || echo 'No X11 sockets found')"
    echo "Xvfb process status: $(ps aux | grep Xvfb | grep -v grep || echo 'Xvfb not found in process list')"
    
    exit 1
fi

# Test if Chrome/Chromium is available and can run
echo "Testing Chrome/Chromium availability..."
if command -v google-chrome >/dev/null 2>&1; then
    echo "Found Google Chrome:"
    google-chrome --version
    # Test Chrome can start in headless mode
    echo "Testing Chrome headless mode..."
    # Try new headless format first, then fallback to old format
    if google-chrome --headless=new --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-setuid-sandbox --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chrome headless mode test: PASSED (new format)"
    elif google-chrome --headless --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-setuid-sandbox --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chrome headless mode test: PASSED (legacy format)"
    else
        echo "Chrome headless mode test: FAILED"
        echo "Attempting diagnostic test with minimal options..."
        google-chrome --version 2>&1 || echo "Chrome version check failed"
        google-chrome --headless --no-sandbox --dump-dom about:blank 2>&1 | head -10 || echo "Minimal Chrome test failed"
    fi
elif command -v chromium >/dev/null 2>&1; then
    echo "Found Chromium:"
    chromium --version
    # Test Chromium can start in headless mode  
    echo "Testing Chromium headless mode..."
    # Try new headless format first, then fallback to old format
    if chromium --headless=new --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-setuid-sandbox --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chromium headless mode test: PASSED (new format)"
    elif chromium --headless --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-setuid-sandbox --virtual-time-budget=1000 --run-all-compositor-stages-before-draw --dump-dom about:blank >/dev/null 2>&1; then
        echo "Chromium headless mode test: PASSED (legacy format)"
    else
        echo "Chromium headless mode test: FAILED"
        echo "Attempting diagnostic test with minimal options..."
        chromium --version 2>&1 || echo "Chromium version check failed"
        chromium --headless --no-sandbox --dump-dom about:blank 2>&1 | head -10 || echo "Minimal Chromium test failed"
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
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens jdk.proxy2/jdk.proxy2=ALL-UNNAMED \
  --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-exports java.base/sun.security.util=ALL-UNNAMED \
  --add-exports jdk.unsupported/sun.misc=ALL-UNNAMED \
  -jar /app/target/a2awebagent-0.0.1.jar
