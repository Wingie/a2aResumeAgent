package io.wingie;

import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CustomChromeOptions {
    public static ChromeOptions createOptions() {
        System.out.println("=== CustomChromeOptions.createOptions() Starting ===");
        ChromeOptions options = new ChromeOptions();
        boolean useCommandLineOptions = Boolean.getBoolean("driverOptions");
        System.out.println("Use command line options: " + useCommandLineOptions);
        
        // Log environment information for debugging
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("Is Docker environment: " + isRunningInDocker());
        
        // Detect and set Chrome/Chromium binary path
        String binaryPath = detectChromeBinaryPath();
        if (binaryPath != null) {
            options.setBinary(binaryPath);
            System.out.println("Chrome/Chromium binary found and set: " + binaryPath);
        } else {
            System.err.println("WARNING: No Chrome/Chromium binary found!");
            System.err.println("ChromeDriver will try to find Chrome in default locations");
        }

        if (useCommandLineOptions) {
            // Use custom command-line options when specified
            String commandLineArgs = System.getProperty("chrome.options", "");
            if (!commandLineArgs.isEmpty()) {
                String[] args = commandLineArgs.split(",");
                options.addArguments(args);
                System.out.println("Using command line Chrome options: " + commandLineArgs);
                
                // Only add debugging options if not already specified
                if (!commandLineArgs.contains("--remote-debugging")) {
                    // For headless mode, use pipe; for non-headless, use port
                    if (commandLineArgs.contains("--headless")) {
                        options.addArguments("--remote-debugging-pipe");
                        System.out.println("Added --remote-debugging-pipe for headless mode");
                    } else {
                        options.addArguments("--remote-debugging-port=9222");
                        System.out.println("Added --remote-debugging-port=9222 for non-headless mode");
                    }
                }
            }
        } else {
            // Default configuration: headless mode optimized for automation
            System.out.println("Using default Chrome options (headless mode)");
            
            // === CORE HEADLESS OPTIONS ===
            // Use new headless mode for better compatibility (fallback to old if not supported)
            options.addArguments("--headless=new");
            
            // Essential security options for running in containers/automation
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            
            // === WINDOW AND DISPLAY SETTINGS ===
            // Set consistent window size for screenshots and rendering
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--force-device-scale-factor=1");
            
            // === GPU AND RENDERING OPTIONS ===
            // Disable GPU to avoid graphics driver issues in containers
            options.addArguments("--disable-gpu");
            // Use software rendering for compatibility
            options.addArguments("--use-gl=swiftshader");
            
            // === AUTOMATION AND PERFORMANCE OPTIONS ===
            // Disable unnecessary features for automation
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-background-timer-throttling");
            options.addArguments("--disable-renderer-backgrounding");
            options.addArguments("--disable-backgrounding-occluded-windows");
            options.addArguments("--disable-default-apps");
            options.addArguments("--disable-sync");
            options.addArguments("--no-first-run");
            
            // Additional performance optimizations
            options.addArguments("--disable-features=TranslateUI");
            options.addArguments("--disable-features=Translate");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-password-generation");
            options.addArguments("--disable-save-password-bubble");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-web-resources");
            options.addArguments("--aggressive-cache-discard");
            
            // Network and loading optimizations
            options.addArguments("--disable-lazy-loading");
            options.addArguments("--disable-features=LazyImageLoading");
            options.addArguments("--disable-features=LazyFrameLoading");
            
            // === DEBUGGING COMMUNICATION ===
            // Use pipe for headless mode - avoids port conflicts and works better in containers
            options.addArguments("--remote-debugging-pipe");
            
            // === LOGGING CONFIGURATION ===
            // Reduce log verbosity to minimize noise
            options.addArguments("--log-level=3");
            
            // === DOCKER/CONTAINER SPECIFIC OPTIONS ===
            if (isRunningInDocker()) {
                System.out.println("Docker environment detected - adding container-specific options");
                
                // Additional sandbox disabling for containers
                options.addArguments("--disable-setuid-sandbox");
                
                // Security options for container environments
                options.addArguments("--disable-web-security");
                options.addArguments("--ignore-certificate-errors");
                options.addArguments("--ignore-ssl-errors");
                options.addArguments("--ignore-certificate-errors-spki-list");
                
                // Stability options for containers
                options.addArguments("--disable-crash-reporter");
                options.addArguments("--no-default-browser-check");
                options.addArguments("--disable-hang-monitor");
                options.addArguments("--disable-prompt-on-repost");
                options.addArguments("--disable-domain-reliability");
                
                // Memory and resource optimization for containers
                options.addArguments("--memory-pressure-off");
                options.addArguments("--max_old_space_size=4096");
                options.addArguments("--disable-ipc-flooding-protection");
                
                // Network and DNS optimizations
                options.addArguments("--disable-features=VizDisplayCompositor");
                options.addArguments("--disable-background-networking");
                options.addArguments("--disable-client-side-phishing-detection");
                
                // Audio/video processing optimizations (for web automation)
                options.addArguments("--disable-audio-output");
                options.addArguments("--mute-audio");
                options.addArguments("--autoplay-policy=no-user-gesture-required");
                
                // Additional stability flags for headless automation
                options.addArguments("--disable-component-update");
                options.addArguments("--disable-plugins-discovery");
                options.addArguments("--disable-translate");
                options.addArguments("--disable-logging");
                options.addArguments("--disable-login-animations");
                
                // NOTE: Avoided problematic options:
                // --single-process: Can cause Chrome to exit immediately in containers
                // --no-zygote: Can conflict with headless mode and cause crashes
                // --remote-debugging-port: Conflicts with headless mode in newer Chrome versions
                // --disable-shared-memory-usage: Can cause crashes in some Docker configurations
            }
        }
        
        // Log all options for debugging
        System.out.println("=== Final Chrome Options ===");
        if (binaryPath != null) {
            System.out.println("Binary: " + binaryPath);
        }
        System.out.println("Chrome options configured successfully");
        System.out.println("=== CustomChromeOptions.createOptions() Complete ===");

        return options;
    }
    
    /**
     * Detects the Chrome or Chromium binary path based on the environment.
     * Prioritizes Chromium in Docker environments.
     */
    private static String detectChromeBinaryPath() {
        System.out.println("=== Detecting Chrome/Chromium Binary Path ===");
        
        // Check for explicit configuration
        String configuredPath = System.getProperty("chrome.binary.path");
        System.out.println("Configured chrome.binary.path: " + configuredPath);
        if (configuredPath != null && !configuredPath.isEmpty()) {
            boolean exists = Files.exists(Paths.get(configuredPath));
            System.out.println("Configured path exists: " + exists);
            if (exists) {
                return configuredPath;
            }
        }
        
        // Common paths to check (order matters - Docker paths first)
        String[] possiblePaths = {
            "/usr/bin/chromium",                // Docker Debian/Alpine
            "/usr/bin/chromium-browser",        // Alternative name
            "/usr/local/bin/chromium",          // Alternative location
            "/usr/bin/google-chrome",           // Google Chrome on Linux
            "/usr/bin/google-chrome-stable",    // Google Chrome stable
            "/usr/bin/google-chrome-unstable",  // Google Chrome unstable
            "/snap/bin/chromium",               // Snap package
            "/opt/google/chrome/chrome",        // Google Chrome alternative
            "/usr/lib/chromium-browser/chromium-browser", // Debian/Ubuntu
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",  // macOS
            "/Applications/Chromium.app/Contents/MacOS/Chromium",           // macOS Chromium
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",    // Windows
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe" // Windows 32-bit
        };
        
        System.out.println("Checking common Chrome/Chromium paths:");
        for (String path : possiblePaths) {
            boolean exists = Files.exists(Paths.get(path));
            System.out.println("  " + path + " -> " + (exists ? "FOUND" : "not found"));
            if (exists) {
                System.out.println("=== Binary detected at: " + path + " ===");
                return path;
            }
        }
        
        // If running in Docker and no binary found, log warning
        if (isRunningInDocker()) {
            System.err.println("WARNING: Chrome/Chromium binary not found in Docker container!");
            System.err.println("Please ensure chromium package is installed in your Docker image.");
        }
        
        return null;
    }
    
    /**
     * Detects if the application is running inside a Docker container.
     */
    private static boolean isRunningInDocker() {
        // Check for .dockerenv file
        if (new File("/.dockerenv").exists()) {
            return true;
        }
        
        // Check cgroup for docker/containerd
        try {
            String cgroup = Files.readString(Paths.get("/proc/1/cgroup"));
            if (cgroup.contains("docker") || cgroup.contains("containerd")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore, not in container or can't read file
        }
        
        // Check for Docker environment variables
        return System.getenv("DOCKER_CONTAINER") != null ||
               System.getenv("KUBERNETES_SERVICE_HOST") != null;
    }
}