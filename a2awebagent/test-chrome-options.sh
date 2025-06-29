#!/bin/bash

# Test script to validate Chrome options changes
echo "=== Chrome Options Test Script ==="

# Test if we can find Chrome/Chromium
echo "1. Testing Chrome/Chromium binary detection..."

if command -v chromium >/dev/null 2>&1; then
    CHROME_BINARY="chromium"
    echo "✓ Found Chromium: $(which chromium)"
elif command -v google-chrome >/dev/null 2>&1; then
    CHROME_BINARY="google-chrome"
    echo "✓ Found Google Chrome: $(which google-chrome)"
else
    echo "✗ No Chrome/Chromium found"
    exit 1
fi

echo "Chrome version: $($CHROME_BINARY --version)"

# Test basic options that should NOT cause immediate exit
echo ""
echo "2. Testing basic headless mode..."
if $CHROME_BINARY --headless --no-sandbox --disable-gpu --dump-dom about:blank >/dev/null 2>&1; then
    echo "✓ Basic headless mode works"
else
    echo "✗ Basic headless mode failed"
fi

# Test new headless format
echo ""
echo "3. Testing new headless format..."
if $CHROME_BINARY --headless=new --no-sandbox --disable-gpu --dump-dom about:blank >/dev/null 2>&1; then
    echo "✓ New headless format works"
else
    echo "✗ New headless format failed, falling back to legacy"
fi

# Test problematic options individually
echo ""
echo "4. Testing individual problematic options..."

# Test --single-process (should be avoided)
echo "Testing --single-process..."
if $CHROME_BINARY --headless --no-sandbox --disable-gpu --single-process --dump-dom about:blank >/dev/null 2>&1; then
    echo "✓ --single-process works (but can be problematic)"
else
    echo "✗ --single-process causes Chrome to exit (this is expected)"
fi

# Test --no-zygote (should be avoided)
echo "Testing --no-zygote..."
if $CHROME_BINARY --headless --no-sandbox --disable-gpu --no-zygote --dump-dom about:blank >/dev/null 2>&1; then
    echo "✓ --no-zygote works (but can be problematic)"
else
    echo "✗ --no-zygote causes Chrome to exit (this is expected)"
fi

# Test our Docker-specific options
echo ""
echo "5. Testing Docker-specific options..."
DOCKER_OPTIONS="--headless --no-sandbox --disable-gpu --disable-dev-shm-usage --disable-setuid-sandbox --disable-web-security --no-first-run --no-default-browser-check --disable-crash-reporter --disable-in-process-stack-traces --disable-breakpad --remote-debugging-port=0"

if $CHROME_BINARY $DOCKER_OPTIONS --dump-dom about:blank >/dev/null 2>&1; then
    echo "✓ Docker-specific options work correctly"
else
    echo "✗ Docker-specific options failed"
    echo "Trying with minimal options..."
    if $CHROME_BINARY --headless --no-sandbox --disable-gpu --dump-dom about:blank >/dev/null 2>&1; then
        echo "✓ Minimal options work - issue is with specific Docker options"
    else
        echo "✗ Even minimal options fail"
    fi
fi

echo ""
echo "=== Test Complete ==="