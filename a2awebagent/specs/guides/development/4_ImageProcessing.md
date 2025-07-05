# Tutorial 4: Image Processing and Visual Content Analysis with a2acore Framework

## 1. Introduction

The a2aTravelAgent platform demonstrates advanced image processing and visual content analysis capabilities using the a2acore framework with Playwright browser automation and AI-powered image recognition. This tutorial shows how to build MCP tools that can capture, analyze, and act upon visual content from web pages, enabling sophisticated automation workflows that respond to visual cues.

## 2. Architecture Overview

### 2.1 Visual Processing Stack
```
a2aTravelAgent Image Processing Architecture:
├── a2acore Framework          # MCP tool foundation
│   ├── @EnableA2ACore        # Auto-configuration  
│   ├── A2aCoreController     # Unified MCP endpoints
│   └── JsonRpcHandler        # Protocol processing
├── Playwright Integration     # Browser automation
│   ├── Page Screenshots      # Full-page and element capture
│   ├── Element Selection     # Visual element targeting
│   ├── Image Optimization    # Quality and size optimization
│   └── Multi-format Support  # PNG, JPEG, WebP formats
├── AI Visual Analysis        # Image understanding
│   ├── OpenAI Vision API     # GPT-4 Vision integration
│   ├── Google Gemini Vision  # Gemini Pro Vision
│   ├── Claude Vision         # Anthropic Claude 3
│   └── Local Models          # On-premise processing
└── Action Triggering         # Visual-based automation
    ├── Screenshot Analysis   # Visual content understanding
    ├── Element Detection     # UI component recognition
    ├── Content Extraction    # Text and data extraction
    └── Workflow Automation   # Visual-triggered actions
```

### 2.2 Key Capabilities
- **Real-time Screenshot Capture**: High-quality webpage and element screenshots
- **AI-Powered Visual Analysis**: Multi-provider image understanding
- **Visual Content Extraction**: Text, data, and structure recognition
- **Automated Response**: Actions triggered by visual content
- **Multi-format Support**: Various image formats and optimization

## 3. Screenshot Capture and Processing

### 3.1 Advanced Screenshot Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.Paths;
import java.util.Base64;

@Service
@Agent(name = "screenshot-processor", 
       description = "Advanced screenshot capture and image processing for web automation")
public class ScreenshotProcessorService {
    
    @Autowired
    private Browser browser;
    
    @Autowired
    private ImageOptimizationService imageOptimizer;
    
    @Action(description = "Capture high-quality screenshot of webpage with optimization")
    public String captureOptimizedScreenshot(
        @Parameter(description = "Website URL to capture") String url,
        @Parameter(description = "Image quality (LOW, MEDIUM, HIGH)") String quality,
        @Parameter(description = "Capture full page or viewport only") boolean fullPage) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            
            // Configure viewport for consistent captures
            page.setViewportSize(1920, 1080);
            
            // Navigate and wait for complete loading
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Wait for fonts and images to load
            page.waitForFunction("() => document.fonts.ready");
            page.waitForTimeout(2000);
            
            // Configure screenshot options based on quality
            Page.ScreenshotOptions options = new Page.ScreenshotOptions()
                .setFullPage(fullPage)
                .setType(ScreenshotType.PNG);
                
            // Set quality-specific options
            switch (quality.toUpperCase()) {
                case "HIGH":
                    // No compression, full quality
                    break;
                case "MEDIUM":
                    options.setQuality(85);
                    options.setType(ScreenshotType.JPEG);
                    break;
                case "LOW":
                    options.setQuality(60);
                    options.setType(ScreenshotType.JPEG);
                    break;
            }
            
            // Capture screenshot
            String timestamp = String.valueOf(System.currentTimeMillis());
            String screenshotPath = "/app/screenshots/optimized_" + timestamp + ".png";
            
            byte[] screenshotBytes = page.screenshot(options);
            
            // Optimize image if needed
            if (!"HIGH".equals(quality.toUpperCase())) {
                screenshotBytes = imageOptimizer.optimizeImage(screenshotBytes, quality);
            }
            
            // Save to file
            java.nio.file.Files.write(Paths.get(screenshotPath), screenshotBytes);
            
            // Convert to base64 for response
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            return String.format("Screenshot captured successfully:\nPath: %s\nSize: %d bytes\nBase64: %s", 
                screenshotPath, screenshotBytes.length, base64Screenshot);
                
        } catch (Exception e) {
            log.error("Screenshot capture failed for URL: {}", url, e);
            return "Error capturing screenshot: " + e.getMessage();
        }
    }
    
    @Action(description = "Capture screenshot of specific element on webpage")
    public String captureElementScreenshot(
        @Parameter(description = "Website URL") String url,
        @Parameter(description = "CSS selector for target element") String selector,
        @Parameter(description = "Padding around element in pixels") int padding) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Find the target element
            Locator element = page.locator(selector);
            
            if (element.count() == 0) {
                return "Error: Element not found with selector: " + selector;
            }
            
            // Scroll element into view
            element.first().scrollIntoViewIfNeeded();
            page.waitForTimeout(1000);
            
            // Configure screenshot options with padding
            ElementHandle.ScreenshotOptions options = new ElementHandle.ScreenshotOptions()
                .setType(ScreenshotType.PNG);
            
            // Capture element screenshot
            byte[] screenshotBytes = element.first().screenshot(options);
            
            // Add padding if requested
            if (padding > 0) {
                screenshotBytes = imageOptimizer.addPadding(screenshotBytes, padding);
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String screenshotPath = "/app/screenshots/element_" + timestamp + ".png";
            
            java.nio.file.Files.write(Paths.get(screenshotPath), screenshotBytes);
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            return String.format("Element screenshot captured:\nPath: %s\nSelector: %s\nBase64: %s", 
                screenshotPath, selector, base64Screenshot);
                
        } catch (Exception e) {
            log.error("Element screenshot failed", e);
            return "Error capturing element screenshot: " + e.getMessage();
        }
    }
    
    @Action(description = "Capture multiple screenshots with different viewport sizes")
    public String captureResponsiveScreenshots(
        @Parameter(description = "Website URL") String url,
        @Parameter(description = "Device types (DESKTOP, TABLET, MOBILE)") String deviceTypes) {
        
        try (BrowserContext context = browser.newContext()) {
            StringBuilder results = new StringBuilder("Responsive Screenshots:\n");
            String[] devices = deviceTypes.split(",");
            
            for (String device : devices) {
                Page page = context.newPage();
                
                // Set viewport based on device type
                switch (device.trim().toUpperCase()) {
                    case "DESKTOP":
                        page.setViewportSize(1920, 1080);
                        break;
                    case "TABLET":
                        page.setViewportSize(768, 1024);
                        break;
                    case "MOBILE":
                        page.setViewportSize(375, 667);
                        break;
                }
                
                page.navigate(url);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                page.waitForTimeout(2000);
                
                // Capture screenshot
                String timestamp = String.valueOf(System.currentTimeMillis());
                String screenshotPath = "/app/screenshots/" + device.toLowerCase() + "_" + timestamp + ".png";
                
                byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setType(ScreenshotType.PNG));
                
                java.nio.file.Files.write(Paths.get(screenshotPath), screenshotBytes);
                
                results.append(String.format("- %s: %s (%d bytes)\n", 
                    device, screenshotPath, screenshotBytes.length));
                
                page.close();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            log.error("Responsive screenshots failed", e);
            return "Error capturing responsive screenshots: " + e.getMessage();
        }
    }
}
```

### 3.2 Image Optimization Service
```java
package io.wingie.service;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class ImageOptimizationService {
    
    public byte[] optimizeImage(byte[] imageBytes, String quality) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            // Apply optimization based on quality setting
            BufferedImage optimizedImage = switch (quality.toUpperCase()) {
                case "MEDIUM" -> resizeImage(originalImage, 0.8f);
                case "LOW" -> resizeImage(originalImage, 0.6f);
                default -> originalImage;
            };
            
            // Convert back to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, "PNG", outputStream);
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Image optimization failed", e);
            return imageBytes; // Return original if optimization fails
        }
    }
    
    public byte[] addPadding(byte[] imageBytes, int padding) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            int newWidth = originalImage.getWidth() + (padding * 2);
            int newHeight = originalImage.getHeight() + (padding * 2);
            
            BufferedImage paddedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = paddedImage.createGraphics();
            
            // Fill with transparent background
            g2d.setColor(new Color(255, 255, 255, 0));
            g2d.fillRect(0, 0, newWidth, newHeight);
            
            // Draw original image with padding offset
            g2d.drawImage(originalImage, padding, padding, null);
            g2d.dispose();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(paddedImage, "PNG", outputStream);
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Adding padding failed", e);
            return imageBytes;
        }
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, float scaleFactor) {
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
}
```

## 4. AI-Powered Visual Analysis

### 4.1 Multi-Provider Vision Analysis Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Agent(name = "visual-analysis", 
       description = "AI-powered visual content analysis and understanding")
public class VisualAnalysisService {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Action(description = "Analyze image content using AI vision models")
    public String analyzeImageContent(
        @Parameter(description = "Base64 encoded image") String base64Image,
        @Parameter(description = "Analysis prompt/question") String analysisPrompt,
        @Parameter(description = "AI provider (OPENAI, GEMINI, CLAUDE)") String provider) {
        
        try {
            switch (provider.toUpperCase()) {
                case "OPENAI":
                    return analyzeWithOpenAI(base64Image, analysisPrompt);
                case "GEMINI":
                    return analyzeWithGemini(base64Image, analysisPrompt);
                case "CLAUDE":
                    return analyzeWithClaude(base64Image, analysisPrompt);
                default:
                    return "Error: Unsupported AI provider: " + provider;
            }
        } catch (Exception e) {
            log.error("Image analysis failed with provider: {}", provider, e);
            return "Error analyzing image: " + e.getMessage();
        }
    }
    
    @Action(description = "Extract text content from images using OCR")
    public String extractTextFromImage(
        @Parameter(description = "Base64 encoded image") String base64Image,
        @Parameter(description = "Language code (en, es, fr, de, etc.)") String language) {
        
        try {
            String prompt = "Extract all visible text from this image. " +
                           "Return only the text content, maintaining the original formatting where possible. " +
                           "Language: " + language;
            
            // Use OpenAI Vision for text extraction
            return analyzeWithOpenAI(base64Image, prompt);
            
        } catch (Exception e) {
            log.error("Text extraction failed", e);
            return "Error extracting text: " + e.getMessage();
        }
    }
    
    @Action(description = "Detect and analyze UI elements in screenshots")
    public String analyzeUIElements(
        @Parameter(description = "Base64 encoded screenshot") String base64Image,
        @Parameter(description = "UI analysis type (BUTTONS, FORMS, NAVIGATION, ALL)") String analysisType) {
        
        try {
            String prompt = buildUIAnalysisPrompt(analysisType);
            
            // Use Gemini Vision for UI analysis
            String analysis = analyzeWithGemini(base64Image, prompt);
            
            // Parse and structure the response
            return structureUIAnalysis(analysis, analysisType);
            
        } catch (Exception e) {
            log.error("UI element analysis failed", e);
            return "Error analyzing UI elements: " + e.getMessage();
        }
    }
    
    @Action(description = "Compare two images and identify differences")
    public String compareImages(
        @Parameter(description = "Base64 encoded first image") String image1,
        @Parameter(description = "Base64 encoded second image") String image2,
        @Parameter(description = "Comparison focus (LAYOUT, CONTENT, COLORS, ALL)") String focus) {
        
        try {
            String prompt = String.format(
                "Compare these two images and identify the differences. " +
                "Focus on: %s. " +
                "Provide a detailed analysis of what has changed between the images.",
                focus);
            
            // Use Claude Vision for image comparison
            return analyzeImagesWithClaude(image1, image2, prompt);
            
        } catch (Exception e) {
            log.error("Image comparison failed", e);
            return "Error comparing images: " + e.getMessage();
        }
    }
    
    private String analyzeWithOpenAI(String base64Image, String prompt) {
        WebClient client = webClientBuilder
            .baseUrl("https://api.openai.com/v1/")
            .defaultHeader("Authorization", "Bearer " + apiKeyService.getOpenAIKey())
            .build();
        
        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4-vision-preview",
            "messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                    Map.of("type", "text", "text", prompt),
                    Map.of("type", "image_url", "image_url", Map.of(
                        "url", "data:image/png;base64," + base64Image
                    ))
                )
            )),
            "max_tokens", 1000
        );
        
        Map<String, Object> response = client.post()
            .uri("chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        return extractOpenAIResponse(response);
    }
    
    private String analyzeWithGemini(String base64Image, String prompt) {
        WebClient client = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .build();
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", prompt),
                    Map.of("inline_data", Map.of(
                        "mime_type", "image/png",
                        "data", base64Image
                    ))
                )
            ))
        );
        
        Map<String, Object> response = client.post()
            .uri("models/gemini-pro-vision:generateContent")
            .header("x-goog-api-key", apiKeyService.getGeminiKey())
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        return extractGeminiResponse(response);
    }
    
    private String buildUIAnalysisPrompt(String analysisType) {
        return switch (analysisType.toUpperCase()) {
            case "BUTTONS" -> "Identify and describe all buttons in this interface. " +
                             "Include their text, position, and apparent function.";
            case "FORMS" -> "Analyze all form elements in this interface. " +
                           "Describe input fields, labels, validation, and submission elements.";
            case "NAVIGATION" -> "Identify navigation elements including menus, links, " +
                                "breadcrumbs, and navigation patterns.";
            case "ALL" -> "Provide a comprehensive analysis of all UI elements including " +
                         "buttons, forms, navigation, content areas, and interactive elements.";
            default -> "Analyze the user interface elements in this image.";
        };
    }
}
```

## 5. Visual Content Extraction

### 5.1 Data Extraction from Screenshots
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Agent(name = "content-extraction", 
       description = "Extract structured data from visual content and screenshots")
public class ContentExtractionService {
    
    @Autowired
    private VisualAnalysisService visualAnalysisService;
    
    @Action(description = "Extract travel booking information from website screenshots")
    public String extractTravelBookingData(
        @Parameter(description = "Base64 encoded screenshot of booking page") String base64Image,
        @Parameter(description = "Data extraction focus (FLIGHTS, HOTELS, PRICES, ALL)") String focus) {
        
        try {
            String extractionPrompt = buildTravelExtractionPrompt(focus);
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, extractionPrompt, "OPENAI");
            
            // Structure the extracted data
            return structureTravelData(analysis, focus);
            
        } catch (Exception e) {
            log.error("Travel booking data extraction failed", e);
            return "Error extracting travel data: " + e.getMessage();
        }
    }
    
    @Action(description = "Extract table data from screenshots")
    public String extractTableData(
        @Parameter(description = "Base64 encoded screenshot containing table") String base64Image,
        @Parameter(description = "Table structure hint (HEADERS, ROWS, COLUMNS)") String structureHint) {
        
        try {
            String prompt = "Extract all data from the table in this image. " +
                           "Format the output as a structured table with clear headers and rows. " +
                           "Maintain the original data relationships and formatting. " +
                           "Structure hint: " + structureHint;
            
            String tableData = visualAnalysisService.analyzeImageContent(
                base64Image, prompt, "GEMINI");
            
            return formatTableData(tableData);
            
        } catch (Exception e) {
            log.error("Table data extraction failed", e);
            return "Error extracting table data: " + e.getMessage();
        }
    }
    
    @Action(description = "Extract pricing information from e-commerce screenshots")
    public String extractPricingData(
        @Parameter(description = "Base64 encoded screenshot") String base64Image,
        @Parameter(description = "Currency code (USD, EUR, GBP, etc.)") String currency) {
        
        try {
            String prompt = String.format(
                "Extract all pricing information from this image. " +
                "Look for prices, discounts, shipping costs, taxes, and total amounts. " +
                "Expected currency: %s. " +
                "Format the output as structured data with clear labels.",
                currency);
            
            String pricingAnalysis = visualAnalysisService.analyzeImageContent(
                base64Image, prompt, "CLAUDE");
            
            return parsePricingData(pricingAnalysis, currency);
            
        } catch (Exception e) {
            log.error("Pricing data extraction failed", e);
            return "Error extracting pricing data: " + e.getMessage();
        }
    }
    
    @Action(description = "Extract contact information from business cards or websites")
    public String extractContactInformation(
        @Parameter(description = "Base64 encoded image") String base64Image,
        @Parameter(description = "Information types (EMAIL, PHONE, ADDRESS, ALL)") String infoTypes) {
        
        try {
            String prompt = buildContactExtractionPrompt(infoTypes);
            
            String contactAnalysis = visualAnalysisService.analyzeImageContent(
                base64Image, prompt, "OPENAI");
            
            return structureContactData(contactAnalysis);
            
        } catch (Exception e) {
            log.error("Contact information extraction failed", e);
            return "Error extracting contact information: " + e.getMessage();
        }
    }
    
    private String buildTravelExtractionPrompt(String focus) {
        return switch (focus.toUpperCase()) {
            case "FLIGHTS" -> "Extract flight information including airlines, flight numbers, " +
                             "departure/arrival times, airports, and seat classes.";
            case "HOTELS" -> "Extract hotel information including names, ratings, prices, " +
                            "amenities, and location details.";
            case "PRICES" -> "Extract all pricing information including base prices, taxes, " +
                            "fees, discounts, and total amounts.";
            case "ALL" -> "Extract comprehensive travel booking information including flights, " +
                         "hotels, car rentals, activities, and all associated pricing.";
            default -> "Extract travel-related information from this booking page.";
        };
    }
    
    private String structureTravelData(String analysis, String focus) {
        // Parse and structure the analysis results
        StringBuilder structured = new StringBuilder();
        structured.append("Travel Booking Data Extraction:\n");
        structured.append("Focus: ").append(focus).append("\n\n");
        
        // Process the analysis and extract structured data
        String[] lines = analysis.split("\n");
        for (String line : lines) {
            if (isRelevantTravelData(line, focus)) {
                structured.append("- ").append(line.trim()).append("\n");
            }
        }
        
        return structured.toString();
    }
    
    private boolean isRelevantTravelData(String line, String focus) {
        String lowerLine = line.toLowerCase();
        return switch (focus.toUpperCase()) {
            case "FLIGHTS" -> lowerLine.contains("flight") || lowerLine.contains("airline") ||
                             lowerLine.contains("departure") || lowerLine.contains("arrival");
            case "HOTELS" -> lowerLine.contains("hotel") || lowerLine.contains("room") ||
                            lowerLine.contains("rating") || lowerLine.contains("amenity");
            case "PRICES" -> lowerLine.contains("price") || lowerLine.contains("cost") ||
                            lowerLine.contains("fee") || lowerLine.contains("total");
            default -> true;
        };
    }
}
```

## 6. Visual-Triggered Automation

### 6.1 Emergency Detection and Response
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Agent(name = "emergency-detection", 
       description = "Automated emergency detection and response from visual content")
public class EmergencyDetectionService {
    
    @Autowired
    private VisualAnalysisService visualAnalysisService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Action(description = "Analyze image for emergency situations and trigger appropriate response")
    public String analyzeForEmergencies(
        @Parameter(description = "Base64 encoded image from security camera or upload") String base64Image,
        @Parameter(description = "Location information") String location,
        @Parameter(description = "Enable automatic emergency response") boolean autoResponse) {
        
        try {
            String emergencyPrompt = 
                "Analyze this image for any emergency situations including: " +
                "- Vehicle accidents or collisions " +
                "- Fires or smoke " +
                "- Medical emergencies " +
                "- Security threats or suspicious activities " +
                "- Natural disasters or severe weather " +
                "- Infrastructure damage or hazards. " +
                "If an emergency is detected, specify the type and severity level.";
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, emergencyPrompt, "GEMINI");
            
            EmergencyAssessment assessment = parseEmergencyAnalysis(analysis);
            
            if (assessment.isEmergencyDetected()) {
                String response = handleEmergencyDetection(assessment, location, autoResponse);
                return String.format("Emergency detected: %s\nLocation: %s\nResponse: %s", 
                    assessment.getDescription(), location, response);
            } else {
                return "No emergency situations detected in the image.";
            }
            
        } catch (Exception e) {
            log.error("Emergency analysis failed", e);
            return "Error analyzing image for emergencies: " + e.getMessage();
        }
    }
    
    @Action(description = "Monitor vehicle condition and detect maintenance needs")
    public String analyzeVehicleCondition(
        @Parameter(description = "Base64 encoded image of vehicle") String base64Image,
        @Parameter(description = "Vehicle identification (license plate, VIN, etc.)") String vehicleId) {
        
        try {
            String vehiclePrompt = 
                "Analyze this vehicle image for: " +
                "- Tire condition and potential flats " +
                "- Body damage or dents " +
                "- Broken lights or indicators " +
                "- Fluid leaks or mechanical issues " +
                "- General maintenance needs. " +
                "Provide specific recommendations for any issues found.";
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, vehiclePrompt, "OPENAI");
            
            VehicleConditionReport report = parseVehicleAnalysis(analysis, vehicleId);
            
            if (report.hasMaintenanceNeeds()) {
                scheduleVehicleMaintenance(report);
                return String.format("Vehicle %s requires maintenance: %s", 
                    vehicleId, report.getMaintenanceNeeds());
            } else {
                return String.format("Vehicle %s appears to be in good condition.", vehicleId);
            }
            
        } catch (Exception e) {
            log.error("Vehicle condition analysis failed", e);
            return "Error analyzing vehicle condition: " + e.getMessage();
        }
    }
    
    @Action(description = "Detect and analyze fire hazards from surveillance images")
    public String detectFireHazards(
        @Parameter(description = "Base64 encoded surveillance image") String base64Image,
        @Parameter(description = "Building or area identifier") String areaId,
        @Parameter(description = "Alert threshold (LOW, MEDIUM, HIGH)") String threshold) {
        
        try {
            String firePrompt = 
                "Analyze this image for fire hazards and signs of fire including: " +
                "- Visible flames or fire " +
                "- Smoke or unusual vapor " +
                "- Overheated equipment " +
                "- Electrical sparks or arcing " +
                "- Combustible materials in dangerous proximity " +
                "- Emergency exit blockages. " +
                "Rate the risk level and urgency of response needed.";
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, firePrompt, "CLAUDE");
            
            FireHazardReport hazardReport = parseFireAnalysis(analysis, areaId);
            
            if (hazardReport.getRiskLevel().ordinal() >= RiskLevel.valueOf(threshold).ordinal()) {
                String alertResponse = triggerFireAlert(hazardReport);
                return String.format("Fire hazard detected in %s: %s\nAlert triggered: %s", 
                    areaId, hazardReport.getDescription(), alertResponse);
            } else {
                return String.format("No significant fire hazards detected in %s.", areaId);
            }
            
        } catch (Exception e) {
            log.error("Fire hazard detection failed", e);
            return "Error detecting fire hazards: " + e.getMessage();
        }
    }
    
    private String handleEmergencyDetection(EmergencyAssessment assessment, String location, boolean autoResponse) {
        StringBuilder response = new StringBuilder();
        
        if (autoResponse) {
            switch (assessment.getEmergencyType()) {
                case VEHICLE_ACCIDENT:
                    response.append(callEmergencyServices("AMBULANCE", location));
                    response.append(callEmergencyServices("POLICE", location));
                    break;
                case FIRE:
                    response.append(callEmergencyServices("FIRE_DEPARTMENT", location));
                    break;
                case MEDICAL:
                    response.append(callEmergencyServices("AMBULANCE", location));
                    break;
                case SECURITY:
                    response.append(callEmergencyServices("POLICE", location));
                    break;
            }
        } else {
            response.append("Emergency detected but auto-response disabled. Manual intervention required.");
        }
        
        // Log incident for tracking
        incidentLogger.logEmergency(assessment, location);
        
        return response.toString();
    }
    
    private String callEmergencyServices(String serviceType, String location) {
        // In a real implementation, this would integrate with emergency services APIs
        log.info("Emergency service called: {} for location: {}", serviceType, location);
        notificationService.sendEmergencyAlert(serviceType, location);
        return String.format("%s dispatched to %s. ", serviceType, location);
    }
}
```

## 7. Travel-Specific Image Processing

### 7.1 Travel Content Analysis Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Agent(name = "travel-image-analysis", 
       description = "Specialized image analysis for travel and booking websites")
public class TravelImageAnalysisService {
    
    @Autowired
    private VisualAnalysisService visualAnalysisService;
    
    @Autowired
    private ContentExtractionService contentExtractionService;
    
    @Action(description = "Analyze hotel booking pages and extract detailed information")
    public String analyzeHotelBookingPage(
        @Parameter(description = "Base64 encoded screenshot of hotel booking page") String base64Image,
        @Parameter(description = "Analysis depth (BASIC, DETAILED, COMPREHENSIVE)") String depth) {
        
        try {
            String hotelPrompt = buildHotelAnalysisPrompt(depth);
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, hotelPrompt, "OPENAI");
            
            return structureHotelAnalysis(analysis, depth);
            
        } catch (Exception e) {
            log.error("Hotel booking page analysis failed", e);
            return "Error analyzing hotel booking page: " + e.getMessage();
        }
    }
    
    @Action(description = "Compare flight prices across multiple booking websites")
    public String compareFlightPrices(
        @Parameter(description = "Comma-separated base64 images of different booking sites") String base64Images,
        @Parameter(description = "Flight route (e.g., 'JFK to LAX')") String route) {
        
        try {
            String[] images = base64Images.split(",");
            StringBuilder comparison = new StringBuilder();
            comparison.append("Flight Price Comparison for ").append(route).append(":\n\n");
            
            for (int i = 0; i < images.length; i++) {
                String pricePrompt = 
                    "Extract flight pricing information from this booking page. " +
                    "Focus on: base price, taxes, fees, total price, airline, flight times, " +
                    "and any special offers or restrictions.";
                
                String priceAnalysis = visualAnalysisService.analyzeImageContent(
                    images[i].trim(), pricePrompt, "GEMINI");
                
                comparison.append("Site ").append(i + 1).append(":\n");
                comparison.append(priceAnalysis).append("\n\n");
            }
            
            // Add summary comparison
            comparison.append(generatePriceComparisonSummary(images));
            
            return comparison.toString();
            
        } catch (Exception e) {
            log.error("Flight price comparison failed", e);
            return "Error comparing flight prices: " + e.getMessage();
        }
    }
    
    @Action(description = "Analyze travel destination images for attraction recommendations")
    public String analyzeDestinationImages(
        @Parameter(description = "Base64 encoded image of travel destination") String base64Image,
        @Parameter(description = "Travel preferences (CULTURE, NATURE, ADVENTURE, FOOD, etc.)") String preferences) {
        
        try {
            String destinationPrompt = String.format(
                "Analyze this travel destination image and provide recommendations based on preferences: %s. " +
                "Identify: landmarks, activities, attractions, local culture, dining options, " +
                "accessibility, best times to visit, and photography opportunities. " +
                "Tailor recommendations to the specified preferences.",
                preferences);
            
            String analysis = visualAnalysisService.analyzeImageContent(
                base64Image, destinationPrompt, "CLAUDE");
            
            return structureDestinationRecommendations(analysis, preferences);
            
        } catch (Exception e) {
            log.error("Destination image analysis failed", e);
            return "Error analyzing destination image: " + e.getMessage();
        }
    }
    
    @Action(description = "Validate travel document images for completeness and accuracy")
    public String validateTravelDocuments(
        @Parameter(description = "Base64 encoded image of travel document") String base64Image,
        @Parameter(description = "Document type (PASSPORT, VISA, TICKET, INSURANCE)") String documentType) {
        
        try {
            String validationPrompt = buildDocumentValidationPrompt(documentType);
            
            String validation = visualAnalysisService.analyzeImageContent(
                base64Image, validationPrompt, "OPENAI");
            
            DocumentValidationResult result = parseValidationResult(validation, documentType);
            
            return formatValidationReport(result);
            
        } catch (Exception e) {
            log.error("Travel document validation failed", e);
            return "Error validating travel document: " + e.getMessage();
        }
    }
    
    private String buildHotelAnalysisPrompt(String depth) {
        return switch (depth.toUpperCase()) {
            case "BASIC" -> "Extract basic hotel information: name, price, rating, and location.";
            case "DETAILED" -> "Extract comprehensive hotel information: name, price breakdown, " +
                              "rating, amenities, room types, policies, and guest reviews summary.";
            case "COMPREHENSIVE" -> "Provide complete hotel analysis: all pricing details, " +
                                   "amenities breakdown, room specifications, policies, reviews, " +
                                   "location advantages, nearby attractions, and booking conditions.";
            default -> "Analyze the hotel booking information in this image.";
        };
    }
    
    private String buildDocumentValidationPrompt(String documentType) {
        return switch (documentType.toUpperCase()) {
            case "PASSPORT" -> "Validate this passport image. Check for: validity dates, " +
                              "clear photo, readable text, security features, and any damage.";
            case "VISA" -> "Validate this visa document. Check for: validity period, " +
                          "correct country, clear stamps, and readable information.";
            case "TICKET" -> "Validate this travel ticket. Check for: complete routing information, " +
                            "valid dates, passenger name, and booking confirmation.";
            case "INSURANCE" -> "Validate this travel insurance document. Check for: coverage period, " +
                               "policy details, coverage amounts, and contact information.";
            default -> "Validate this travel document for completeness and accuracy.";
        };
    }
}
```

## 8. Performance Optimization

### 8.1 Image Processing Performance Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service
@Agent(name = "image-performance", 
       description = "Performance optimization for image processing operations")
public class ImageProcessingPerformanceService {
    
    @Action(description = "Process multiple images concurrently for batch analysis")
    @Async
    public CompletableFuture<String> processBatchImages(
        @Parameter(description = "Comma-separated base64 images") String base64Images,
        @Parameter(description = "Analysis type for all images") String analysisType,
        @Parameter(description = "AI provider preference") String provider) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] images = base64Images.split(",");
                StringBuilder results = new StringBuilder("Batch Image Processing Results:\n\n");
                
                // Process images in parallel using streams
                List<CompletableFuture<String>> futures = Arrays.stream(images)
                    .map(image -> processImageAsync(image.trim(), analysisType, provider))
                    .collect(Collectors.toList());
                
                // Wait for all processing to complete
                List<String> processedResults = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                for (int i = 0; i < processedResults.size(); i++) {
                    results.append("Image ").append(i + 1).append(":\n");
                    results.append(processedResults.get(i)).append("\n\n");
                }
                
                return results.toString();
                
            } catch (Exception e) {
                log.error("Batch image processing failed", e);
                return "Error processing batch images: " + e.getMessage();
            }
        });
    }
    
    @Action(description = "Cache frequently analyzed image patterns for faster processing")
    @Cacheable(value = "image-analysis", key = "#imageHash + '-' + #analysisType")
    public String getCachedImageAnalysis(
        @Parameter(description = "Hash of the image content") String imageHash,
        @Parameter(description = "Analysis type") String analysisType,
        @Parameter(description = "Base64 image (used if cache miss)") String base64Image) {
        
        // If cache miss, perform actual analysis
        return visualAnalysisService.analyzeImageContent(base64Image, 
            buildAnalysisPrompt(analysisType), "OPENAI");
    }
    
    @Action(description = "Generate performance metrics for image processing operations")
    public String generatePerformanceMetrics(
        @Parameter(description = "Time period for metrics (HOUR, DAY, WEEK)") String period) {
        
        try {
            StringBuilder metrics = new StringBuilder("Image Processing Performance Metrics:\n");
            metrics.append("Period: ").append(period).append("\n\n");
            
            // Collect metrics from various components
            ImageProcessingMetrics imageMetrics = metricsCollector.collectImageMetrics(period);
            
            metrics.append("Screenshots Captured: ").append(imageMetrics.getScreenshotCount()).append("\n");
            metrics.append("Images Analyzed: ").append(imageMetrics.getAnalysisCount()).append("\n");
            metrics.append("Average Processing Time: ").append(imageMetrics.getAverageProcessingTime()).append("ms\n");
            metrics.append("Cache Hit Rate: ").append(imageMetrics.getCacheHitRate()).append("%\n");
            metrics.append("Error Rate: ").append(imageMetrics.getErrorRate()).append("%\n");
            
            // Provider-specific metrics
            metrics.append("\nAI Provider Performance:\n");
            imageMetrics.getProviderMetrics().forEach((provider, metric) -> {
                metrics.append("- ").append(provider).append(": ")
                       .append(metric.getAverageResponseTime()).append("ms avg, ")
                       .append(metric.getSuccessRate()).append("% success\n");
            });
            
            return metrics.toString();
            
        } catch (Exception e) {
            log.error("Failed to generate performance metrics", e);
            return "Error generating performance metrics: " + e.getMessage();
        }
    }
    
    private CompletableFuture<String> processImageAsync(String base64Image, String analysisType, String provider) {
        return CompletableFuture.supplyAsync(() -> {
            String prompt = buildAnalysisPrompt(analysisType);
            return visualAnalysisService.analyzeImageContent(base64Image, prompt, provider);
        });
    }
}
```

## 9. Testing and Validation

### 9.1 Image Processing Test Framework
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "playwright.headless=true",
    "ai.vision.test-mode=true"
})
class ImageProcessingIntegrationTest {
    
    @Autowired
    private ScreenshotProcessorService screenshotService;
    
    @Autowired
    private VisualAnalysisService visualAnalysisService;
    
    @Test
    void testScreenshotCapture() {
        String result = screenshotService.captureOptimizedScreenshot(
            "https://www.google.com", "HIGH", true);
        
        assertThat(result).contains("Screenshot captured successfully");
        assertThat(result).contains("Base64:");
    }
    
    @Test
    void testImageAnalysis() {
        String testImage = loadTestImage("test-travel-page.png");
        
        String analysis = visualAnalysisService.analyzeImageContent(
            testImage, "Describe the travel booking interface", "OPENAI");
        
        assertThat(analysis).isNotEmpty();
        assertThat(analysis).containsIgnoringCase("travel");
    }
    
    private String loadTestImage(String filename) {
        // Load test image from resources and convert to base64
        try {
            byte[] imageBytes = getClass().getResourceAsStream("/test-images/" + filename).readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test image: " + filename, e);
        }
    }
}
```

## 10. Best Practices

### 10.1 Image Processing Guidelines
- **Quality Management**: Balance image quality with processing speed
- **Format Selection**: Choose appropriate formats (PNG for screenshots, JPEG for photos)
- **Caching Strategy**: Cache analysis results for frequently processed content
- **Error Handling**: Implement robust fallbacks for AI service failures
- **Performance Monitoring**: Track processing times and success rates

### 10.2 AI Provider Management
- **Provider Rotation**: Use multiple AI providers for redundancy
- **Cost Optimization**: Choose cost-effective providers for bulk processing
- **Rate Limiting**: Respect API rate limits and implement backoff strategies
- **Quality Assessment**: Monitor and compare provider accuracy

### 10.3 Security and Privacy
- **Data Protection**: Secure handling of sensitive visual content
- **Temporary Storage**: Clean up temporary image files promptly
- **Access Control**: Restrict access to image processing endpoints
- **Audit Logging**: Log all image processing activities

## 11. Conclusion

The a2aTravelAgent image processing capabilities demonstrate how modern AI-powered visual analysis can enhance web automation and travel research. Key benefits include:

### 11.1 Advanced Capabilities
- **Real-time Visual Analysis**: Instant understanding of web content
- **Multi-provider AI Integration**: Robust and redundant analysis options
- **Automated Response**: Actions triggered by visual content
- **Performance Optimization**: Efficient batch processing and caching

### 11.2 Travel Industry Applications
- **Booking Verification**: Automated validation of booking information
- **Price Comparison**: Visual comparison across multiple travel sites
- **Document Processing**: Travel document validation and extraction
- **Destination Analysis**: Intelligent travel recommendations

### 11.3 Production Features
- **Scalable Processing**: Concurrent image analysis capabilities
- **Error Recovery**: Comprehensive fallback mechanisms
- **Performance Monitoring**: Detailed metrics and optimization
- **Security**: Secure handling of visual content

This image processing framework enables sophisticated visual automation workflows that can understand and respond to complex visual content, making it ideal for travel research and booking automation applications.