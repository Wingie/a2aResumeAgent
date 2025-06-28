package io.wingie;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.detect.ActionCallback;
import com.t4a.predict.PredictionLoader;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.GeminiV2ActionProcessor;
import com.t4a.processor.OpenAiActionProcessor;
import com.t4a.processor.scripts.ScriptProcessor;
import com.t4a.processor.scripts.ScriptResult;
import com.t4a.processor.scripts.SeleniumScriptProcessor;
import com.t4a.processor.selenium.SeleniumOpenAIProcessor;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.vishalmysore.a2a.server.SSEEmitterCallback;
import lombok.extern.java.Log;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

@Service
@Log
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching and validation of web pages ")
public class WebBrowsingAction {
    private SeleniumScriptProcessor script;
    private WebDriver driver;
    private boolean initialized = false;
    
    @Autowired(required = false)
    @Lazy
    private WebDriver webDriverBean;
    
    /**
     * Initialize WebDriver and script processor lazily to avoid startup failures.
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            log.info("=== WebBrowsingAction Initialization Starting ===");
            log.info("Operating System: " + System.getProperty("os.name"));
            log.info("Java Version: " + System.getProperty("java.version"));
            log.info("Working Directory: " + System.getProperty("user.dir"));
            
            // Enable comprehensive ChromeDriver logging
            setupChromeDriverLogging();
            
            try {
                // Setup ChromeDriver - bypass WebDriverManager in Docker when system ChromeDriver exists
                log.info("Setting up ChromeDriver...");
                
                boolean useSystemChromeDriver = false;
                
                // Check if we're in a Docker environment and use system ChromeDriver
                if (isRunningInDocker()) {
                    log.info("Docker environment detected, checking for system ChromeDriver...");
                    Path systemChromeDriver = Paths.get("/usr/bin/chromedriver");
                    if (Files.exists(systemChromeDriver) && Files.isExecutable(systemChromeDriver)) {
                        log.info("System ChromeDriver found at /usr/bin/chromedriver - bypassing WebDriverManager completely");
                        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
                        useSystemChromeDriver = true;
                        log.info("ChromeDriver path set to system binary: /usr/bin/chromedriver");
                    } else {
                        log.warning("System ChromeDriver not found at /usr/bin/chromedriver, falling back to WebDriverManager download");
                    }
                }
                
                // Only use WebDriverManager if we're NOT using system ChromeDriver
                if (!useSystemChromeDriver) {
                    log.info("Using WebDriverManager to setup ChromeDriver...");
                    try {
                        WebDriverManager.chromedriver().setup();
                        log.info("WebDriverManager setup completed successfully");
                        // Log the chromedriver path
                        String chromeDriverPath = System.getProperty("webdriver.chrome.driver");
                        log.info("ChromeDriver path from WebDriverManager: " + chromeDriverPath);
                    } catch (Exception wdmEx) {
                        log.warning("WebDriverManager setup failed: " + wdmEx.getMessage());
                        log.warning("Will try to use system ChromeDriver as fallback");
                        // Fallback to system ChromeDriver if WebDriverManager fails
                        Path systemChromeDriver = Paths.get("/usr/bin/chromedriver");
                        if (Files.exists(systemChromeDriver) && Files.isExecutable(systemChromeDriver)) {
                            log.info("Fallback: Using system ChromeDriver at /usr/bin/chromedriver");
                            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
                        } else {
                            log.severe("No ChromeDriver available - WebDriverManager failed and system ChromeDriver not found");
                            wdmEx.printStackTrace();
                            throw wdmEx;
                        }
                    }
                }
                
                // Log Chrome binary detection
                log.info("Detecting Chrome/Chromium binary...");
                ChromeOptions options = CustomChromeOptions.createOptions();
                log.info("ChromeOptions created successfully");
                log.info("ChromeOptions details: " + options.asMap());
                
                // Try to use injected WebDriver bean first
                if (webDriverBean != null) {
                    driver = webDriverBean;
                    log.info("Using injected WebDriver bean from Spring context");
                } else {
                    // Fallback to creating our own with detailed service logging
                    log.info("WebDriver bean not available from Spring, creating directly");
                    
                    // Create ChromeDriverService with verbose logging
                    ChromeDriverService service = createChromeDriverServiceWithLogging();
                    
                    log.info("Attempting to create ChromeDriver with service and options...");
                    log.info("Service details: " + service.toString());
                    log.info("Options details: " + options.asMap());
                    
                    try {
                        // Start the service first to capture any startup errors
                        log.info("Starting ChromeDriverService...");
                        service.start();
                        log.info("ChromeDriverService started successfully on URL: " + service.getUrl());
                        
                        // Create the driver
                        log.info("Creating ChromeDriver instance...");
                        driver = new ChromeDriver(service, options);
                        log.info("ChromeDriver created successfully");
                        
                        // Test the driver with a simple operation
                        log.info("Testing ChromeDriver with simple operation...");
                        String currentUrl = driver.getCurrentUrl();
                        log.info("ChromeDriver test successful. Current URL: " + currentUrl);
                        
                    } catch (Exception chromeEx) {
                        log.severe("=== ChromeDriver Creation Failed - Detailed Error Analysis ===");
                        log.severe("Exception type: " + chromeEx.getClass().getName());
                        log.severe("Error message: " + chromeEx.getMessage());
                        
                        // Log system environment details
                        logSystemEnvironmentForDebugging();
                        
                        // Log service status
                        if (service != null) {
                            try {
                                log.severe("Service URL: " + service.getUrl());
                                log.severe("Service running: " + service.isRunning());
                            } catch (Exception serviceEx) {
                                log.severe("Could not get service details: " + serviceEx.getMessage());
                            }
                        }
                        
                        log.severe("Full stack trace:");
                        chromeEx.printStackTrace();
                        
                        // Try to stop the service if it was started
                        try {
                            if (service != null && service.isRunning()) {
                                service.stop();
                                log.info("ChromeDriverService stopped after failure");
                            }
                        } catch (Exception stopEx) {
                            log.warning("Failed to stop ChromeDriverService: " + stopEx.getMessage());
                        }
                        
                        throw chromeEx;
                    }
                }
                
                log.info("Creating SeleniumScriptProcessor...");
                script = new SeleniumScriptProcessor(new SeleniumOpenAIProcessor(driver));
                initialized = true;
                log.info("=== WebBrowsingAction Initialization Completed Successfully ===");
                
            } catch (Exception e) {
                log.severe("=== WebBrowsingAction Initialization Failed ===");
                log.severe("Exception type: " + e.getClass().getName());
                log.severe("Error message: " + e.getMessage());
                log.severe("Full stack trace:");
                e.printStackTrace();
                
                initialized = false;
                throw new RuntimeException("WebDriver initialization failed. Ensure Chrome/Chromium is installed and accessible. Error: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Set up comprehensive logging for ChromeDriver debugging
     */
    private void setupChromeDriverLogging() {
        log.info("=== Setting up ChromeDriver Logging ===");
        
        // Enable ChromeDriver service logging
        System.setProperty("webdriver.chrome.verboseLogging", "true");
        System.setProperty("webdriver.chrome.logfile", "/tmp/chromedriver.log");
        System.setProperty("webdriver.chrome.args", "--verbose --log-path=/tmp/chromedriver.log");
        
        // Set ChromeDriver log level to ALL for maximum verbosity
        System.setProperty("webdriver.chrome.loglevel", "ALL");
        
        // Enable Selenium logging
        System.setProperty("selenium.LOGGER.level", "ALL");
        
        log.info("ChromeDriver logging configured:");
        log.info("  webdriver.chrome.verboseLogging: " + System.getProperty("webdriver.chrome.verboseLogging"));
        log.info("  webdriver.chrome.logfile: " + System.getProperty("webdriver.chrome.logfile"));
        log.info("  webdriver.chrome.loglevel: " + System.getProperty("webdriver.chrome.loglevel"));
    }
    
    /**
     * Create a ChromeDriverService with verbose logging enabled
     */
    private ChromeDriverService createChromeDriverServiceWithLogging() {
        log.info("=== Creating ChromeDriverService with Logging ===");
        
        ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder();
        
        // Use the ChromeDriver path that was set (either from system or WebDriverManager)
        String chromeDriverPath = System.getProperty("webdriver.chrome.driver");
        if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
            Path driverPath = Paths.get(chromeDriverPath);
            if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
                serviceBuilder.usingDriverExecutable(driverPath.toFile());
                log.info("ChromeDriverService configured to use driver at: " + chromeDriverPath);
            } else {
                log.warning("ChromeDriver path set but file not found or not executable: " + chromeDriverPath);
            }
        } else {
            log.info("No specific ChromeDriver path set, using default discovery");
        }
        
        // Enable verbose logging
        serviceBuilder.withVerbose(true);
        
        // Set log file
        try {
            Path logFile = Paths.get("/tmp/chromedriver-service.log");
            serviceBuilder.withLogFile(logFile.toFile());
            log.info("ChromeDriverService log file set to: " + logFile);
        } catch (Exception e) {
            log.warning("Could not set ChromeDriverService log file: " + e.getMessage());
        }
        
        // Build the service
        ChromeDriverService service = serviceBuilder.build();
        log.info("ChromeDriverService created with verbose logging enabled");
        
        return service;
    }
    
    /**
     * Check if the application is running in a Docker environment
     */
    private boolean isRunningInDocker() {
        try {
            // Check for .dockerenv file which is typically present in Docker containers
            Path dockerEnvFile = Paths.get("/.dockerenv");
            if (Files.exists(dockerEnvFile)) {
                log.info("Docker environment detected: /.dockerenv file exists");
                return true;
            }
            
            // Check cgroup information for Docker container indicators
            Path cgroupFile = Paths.get("/proc/1/cgroup");
            if (Files.exists(cgroupFile)) {
                String cgroupContent = Files.readString(cgroupFile);
                if (cgroupContent.contains("docker") || cgroupContent.contains("containerd")) {
                    log.info("Docker environment detected: container indicators found in /proc/1/cgroup");
                    return true;
                }
            }
            
            // Check for Docker-specific environment variables
            String[] dockerEnvVars = {"DOCKER_CONTAINER", "CONTAINER_ID", "HOSTNAME"};
            for (String envVar : dockerEnvVars) {
                String value = System.getenv(envVar);
                if (value != null && !value.isEmpty()) {
                    // Additional check for HOSTNAME to see if it looks like a container ID
                    if ("HOSTNAME".equals(envVar) && value.length() == 12 && value.matches("[a-f0-9]+")) {
                        log.info("Docker environment detected: container-like hostname found");
                        return true;
                    } else if (!"HOSTNAME".equals(envVar)) {
                        log.info("Docker environment detected: " + envVar + " environment variable present");
                        return true;
                    }
                }
            }
            
            log.info("No Docker environment indicators found, assuming non-Docker environment");
            return false;
            
        } catch (Exception e) {
            log.warning("Error detecting Docker environment: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Log detailed system environment information for debugging
     */
    private void logSystemEnvironmentForDebugging() {
        log.severe("=== System Environment Debug Information ===");
        
        // Log environment variables
        log.severe("DISPLAY: " + System.getenv("DISPLAY"));
        log.severe("XVFB_PID: " + System.getenv("XVFB_PID"));
        log.severe("PATH: " + System.getenv("PATH"));
        log.severe("HOME: " + System.getenv("HOME"));
        log.severe("USER: " + System.getenv("USER"));
        
        // Log system properties
        log.severe("java.home: " + System.getProperty("java.home"));
        log.severe("java.library.path: " + System.getProperty("java.library.path"));
        log.severe("user.home: " + System.getProperty("user.home"));
        log.severe("webdriver.chrome.driver: " + System.getProperty("webdriver.chrome.driver"));
        
        // Log file system checks
        String[] pathsToCheck = {
            "/usr/bin/chromium",
            "/usr/bin/google-chrome",
            "/usr/bin/chromedriver",
            "/tmp/chromedriver.log",
            "/tmp/chromedriver-service.log"
        };
        
        for (String path : pathsToCheck) {
            boolean exists = Files.exists(Paths.get(path));
            boolean executable = Files.isExecutable(Paths.get(path));
            log.severe("Path check - " + path + ": exists=" + exists + ", executable=" + executable);
        }
        
        // Log process information
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            // Log first few lines of ps output to see running processes
            log.severe("Running processes (partial list):");
            log.severe("Note: Full process list would be too verbose for logs");
        } catch (Exception e) {
            log.severe("Could not get process information: " + e.getMessage());
        }
    }

    private ActionCallback callback;
    private AIProcessor processor;
    
    private AIProcessor getProcessor() {
        if (processor == null) {
            processor = new GeminiV2ActionProcessor();
        }
        return processor;
    }
    @Action(description = "perform actions on the web with selenium and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) throws IOException {
        initializeIfNeeded();
        CustomScriptResult result = new CustomScriptResult();
        A2ASeleniumCallBack seleniumCallBack = new A2ASeleniumCallBack(result,getProcessor());
        if(getProcessor() != null) {
            try {
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(getProcessor().query("Separate the web browsing steps into individual steps  just give me steps without any additional text or bracket. MOST IMP - make sure each step can be processed by selenium webdriver, urls should always start with http or https {"+ webBrowsingSteps+"}"));
                //you can create your own selenium processor which implements SeleniumProcessor
                //SeleniumScriptProcessor script = new SeleniumScriptProcessor(new MyOwnSeleniumScriptProcessor());

                script.process(seperatedWebBrowsingSteps,seleniumCallBack);
                                return result.getLastData();
            } catch (AIProcessingException e) {
                throw new RuntimeException(e);
            }


        }
        return "processing issues";// Process the file

    }

    @Action(description = "perform actions on the web with selenium and return image file path")
    public String browseWebAndReturnImage(String webBrowsingSteps) throws IOException {
        initializeIfNeeded();
        CustomScriptResult result = new CustomScriptResult();
        A2ASeleniumCallBack seleniumCallBack = new A2ASeleniumCallBack(result,getProcessor());
        if(getProcessor() != null) {
            try {
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(getProcessor().query("Separate the web browsing steps into individual steps  just give me steps without any additional text or brackets {"+ webBrowsingSteps+"}"));

                script.process(seperatedWebBrowsingSteps,seleniumCallBack);
                return "Screenshot saved to: " + result.getLastScreenshotAsFile();
            } catch (AIProcessingException e) {
                throw new RuntimeException(e);
            }


        }
        return "processing issues";// Process the file

    }

}
