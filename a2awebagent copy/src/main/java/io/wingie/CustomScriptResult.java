package io.wingie;

import java.util.ArrayList;
import java.util.Base64;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.ToString;

@ToString
public class CustomScriptResult {
    ArrayList<String> data = new ArrayList<String>();
    ArrayList<byte[]> screenshots = new ArrayList<>();

    public void addBeforeHtml(String html) {
        data.add("Before HTML: " + html);
    }

    public void addAfterHtml(String html) {
        data.add("After HTML: " + html);
    }

    public void addScreenshot(byte[] screenshot) {
        screenshots.add(screenshot);
    }

    public String getLastScreenshotAsBase64() {
        if (screenshots.isEmpty()) {
            return "No screenshot available";
        }
        return Base64.getEncoder().encodeToString(screenshots.get(screenshots.size() - 1));
    }

    public String getLastScreenshotAsFile() throws IOException {
        if (screenshots.isEmpty()) {
            return "No screenshot available";
        }
        
        // Create screenshots directory if it doesn't exist
        Path screenshotsDir = Paths.get("screenshots");
        if (!Files.exists(screenshotsDir)) {
            Files.createDirectories(screenshotsDir);
        }
        
        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "screenshot_" + timestamp + ".png";
        Path filePath = screenshotsDir.resolve(filename);
        
        // Write screenshot to file
        Files.write(filePath, screenshots.get(screenshots.size() - 1));
        
        return filePath.toString();
    }

    public String getLastData() {
        if (data.isEmpty()) {
            return "No data available";
        }
        return data.get(data.size() - 1);
    }
}