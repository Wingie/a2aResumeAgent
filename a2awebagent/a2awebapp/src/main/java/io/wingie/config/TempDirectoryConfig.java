package io.wingie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration to handle temporary directory issues in Docker environments
 */
@Configuration
@Slf4j
public class TempDirectoryConfig {

    @Value("${java.io.tmpdir:/tmp}")
    private String tempDir;

    @Value("${app.temp.custom.dir:./temp}")
    private String customTempDir;

    @Value("${app.temp.custom.enabled:true}")
    private boolean useCustomTempDir;

    @PostConstruct
    public void configureTempDirectory() {
        if (useCustomTempDir) {
            try {
                // Create custom temp directory
                Path customTempPath = Paths.get(customTempDir).toAbsolutePath();
                Files.createDirectories(customTempPath);
                
                // Set system property to use custom temp directory
                System.setProperty("java.io.tmpdir", customTempPath.toString());
                
                log.info("📁 Custom temp directory configured: {}", customTempPath);
                
                // Test write access
                File testFile = new File(customTempPath.toFile(), "write-test.tmp");
                if (testFile.createNewFile()) {
                    testFile.delete();
                    log.info("✅ Temp directory write test successful");
                } else {
                    log.warn("⚠️ Could not create test file in temp directory");
                }
                
            } catch (Exception e) {
                log.error("❌ Failed to configure custom temp directory, falling back to system default", e);
                checkDefaultTempDirectory();
            }
        } else {
            checkDefaultTempDirectory();
        }
    }

    private void checkDefaultTempDirectory() {
        try {
            File tempDirFile = new File(tempDir);
            
            if (!tempDirFile.exists()) {
                log.warn("⚠️ Temp directory does not exist: {}", tempDir);
                return;
            }
            
            if (!tempDirFile.canWrite()) {
                log.warn("⚠️ Temp directory is not writable: {}", tempDir);
                return;
            }
            
            // Check available space
            long freeSpace = tempDirFile.getFreeSpace();
            long totalSpace = tempDirFile.getTotalSpace();
            double freeSpaceGB = freeSpace / (1024.0 * 1024.0 * 1024.0);
            double usagePercent = ((double) (totalSpace - freeSpace) / totalSpace) * 100;
            
            log.info("💾 Temp directory: {} | Free: {:.2f} GB | Usage: {:.1f}%", 
                tempDir, freeSpaceGB, usagePercent);
            
            if (freeSpaceGB < 0.1) { // Less than 100MB
                log.error("❌ CRITICAL: Very low disk space in temp directory: {:.2f} GB", freeSpaceGB);
            } else if (freeSpaceGB < 1.0) { // Less than 1GB
                log.warn("⚠️ WARNING: Low disk space in temp directory: {:.2f} GB", freeSpaceGB);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to check temp directory: {}", tempDir, e);
        }
    }

    /**
     * Get configured temp directory path
     */
    public String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Check if temp directory has sufficient space
     */
    public boolean hasSufficientSpace(long requiredBytes) {
        try {
            File tempDirFile = new File(getTempDirectory());
            return tempDirFile.getFreeSpace() >= requiredBytes;
        } catch (Exception e) {
            log.warn("Failed to check temp directory space", e);
            return false;
        }
    }

    /**
     * Clean up old temporary files
     */
    public void cleanupOldTempFiles() {
        try {
            File tempDirFile = new File(getTempDirectory());
            File[] files = tempDirFile.listFiles((dir, name) -> 
                name.startsWith("tomcat.") || name.startsWith("a2a-") || name.endsWith(".tmp"));
            
            if (files != null) {
                int cleaned = 0;
                long now = System.currentTimeMillis();
                long maxAge = 24 * 60 * 60 * 1000; // 24 hours
                
                for (File file : files) {
                    if (now - file.lastModified() > maxAge) {
                        if (file.delete()) {
                            cleaned++;
                        }
                    }
                }
                
                if (cleaned > 0) {
                    log.info("🧹 Cleaned up {} old temp files", cleaned);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp files", e);
        }
    }
}