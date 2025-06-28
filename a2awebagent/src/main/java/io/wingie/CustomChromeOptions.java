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
            String commandLineArgs = System.getProperty("chrome.options", "");
            if (!commandLineArgs.isEmpty()) {
                String[] args = commandLineArgs.split(",");
                options.addArguments(args);
                System.out.println(commandLineArgs);
            }
        } else {
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-background-timer-throttling");
            options.addArguments("--disable-renderer-backgrounding");
            options.addArguments("--disable-backgrounding-occluded-windows");
            
            // Window and viewport configuration for proper screenshot rendering
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--start-maximized");
            options.addArguments("--force-device-scale-factor=1");
            options.addArguments("--high-dpi-support=1");
            
            // GPU and rendering fixes for blank screenshots
            options.addArguments("--disable-gpu");
            options.addArguments("--use-gl=swiftshader");
            options.addArguments("--disable-software-rasterizer");
            options.addArguments("--disable-gpu-sandbox");
            
            // Alternative GL implementations for ARM64 compatibility
            options.addArguments("--use-angle=swiftshader");
            
            // Enable better rendering for screenshots
            options.addArguments("--enable-features=VizDisplayCompositor");
            options.addArguments("--run-all-compositor-stages-before-draw");
            options.addArguments("--disable-features=VizDisplayCompositor");
            
            // Font and rendering improvements
            options.addArguments("--font-render-hinting=none");
            options.addArguments("--disable-font-subpixel-positioning");
            options.addArguments("--disable-partial-raster");
            
            // Additional stability options
            options.addArguments("--disable-logging");
            options.addArguments("--disable-gpu-early-init");
            options.addArguments("--disable-dev-tools");
            options.addArguments("--disable-ipc-flooding-protection");
            
            // Additional Docker-specific options
            if (isRunningInDocker()) {
                options.addArguments("--disable-setuid-sandbox");
                options.addArguments("--single-process"); // For containers with limited resources
                options.addArguments("--no-zygote"); // Disable zygote process for containers
                options.addArguments("--disable-web-security"); // May help with rendering in containers
                System.out.println("Docker environment detected - added container-specific Chrome options");
            }
        }

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
            "/opt/google/chrome/chrome",        // Google Chrome alternative
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",  // macOS
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