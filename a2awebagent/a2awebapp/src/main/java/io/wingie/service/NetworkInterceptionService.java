package io.wingie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for intercepting and analyzing network requests and responses
 * Captures API calls, JSON responses, and dynamic content loading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkInterceptionService {

    private final ObjectMapper objectMapper;
    
    // Store network activity per page session
    private final Map<String, List<NetworkCapture>> pageNetworkActivity = new ConcurrentHashMap<>();
    
    // Store API responses for analysis
    private final Map<String, List<ApiResponse>> pageApiResponses = new ConcurrentHashMap<>();
    
    /**
     * Enable network interception for a page
     */
    public void enableNetworkInterception(Page page, String sessionId) {
        log.info("Enabling network interception for session: {}", sessionId);
        
        // Initialize storage for this session
        pageNetworkActivity.put(sessionId, new CopyOnWriteArrayList<>());
        pageApiResponses.put(sessionId, new CopyOnWriteArrayList<>());
        
        // Intercept all network requests
        page.route("**/*", route -> {
            Request request = route.request();
            
            // Log the request
            NetworkCapture capture = NetworkCapture.builder()
                .sessionId(sessionId)
                .method(request.method())
                .url(request.url())
                .headers(request.headers())
                .postData(request.postData())
                .resourceType(request.resourceType())
                .timestamp(System.currentTimeMillis())
                .build();
            
            pageNetworkActivity.get(sessionId).add(capture);
            
            // Continue the request and capture response
            route.resume();
        });
        
        // Capture responses
        page.onResponse(response -> {
            try {
                String url = response.url();
                String contentType = response.headers().get("content-type");
                
                // Focus on API responses (JSON content)
                if (contentType != null && contentType.contains("application/json")) {
                    captureApiResponse(sessionId, response);
                }
                
                // Log interesting responses
                if (isInterestingResponse(response)) {
                    log.debug("Captured response: {} {} ({})", 
                        response.status(), response.url(), contentType);
                }
                
            } catch (Exception e) {
                log.warn("Error capturing response for session {}: {}", sessionId, e.getMessage());
            }
        });
        
        // Monitor request failures
        page.onRequestFailed(request -> {
            log.warn("Request failed for session {}: {} - {}", 
                sessionId, request.url(), request.failure());
        });
    }
    
    /**
     * Capture and analyze API responses
     */
    private void captureApiResponse(String sessionId, Response response) {
        try {
            String bodyText = response.text();
            
            // Parse JSON response
            Object jsonData = null;
            try {
                jsonData = objectMapper.readValue(bodyText, Object.class);
            } catch (Exception e) {
                log.debug("Response is not valid JSON: {}", response.url());
                return;
            }
            
            ApiResponse apiResponse = ApiResponse.builder()
                .sessionId(sessionId)
                .url(response.url())
                .status(response.status())
                .headers(response.headers())
                .contentType(response.headers().get("content-type"))
                .jsonData(jsonData)
                .rawBody(bodyText)
                .timestamp(System.currentTimeMillis())
                .build();
            
            pageApiResponses.get(sessionId).add(apiResponse);
            
            // Analyze for important data
            analyzeApiResponse(apiResponse);
            
        } catch (Exception e) {
            log.warn("Error capturing API response for session {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Analyze API response for important data patterns
     */
    private void analyzeApiResponse(ApiResponse apiResponse) {
        try {
            String url = apiResponse.getUrl().toLowerCase();
            Object jsonData = apiResponse.getJsonData();
            
            // Detect authentication responses
            if (url.contains("login") || url.contains("auth") || url.contains("token")) {
                log.info("Detected authentication API response: {}", apiResponse.getUrl());
                extractAuthenticationData(apiResponse);
            }
            
            // Detect search results
            if (url.contains("search") || url.contains("query")) {
                log.info("Detected search API response: {}", apiResponse.getUrl());
                extractSearchResults(apiResponse);
            }
            
            // Detect dynamic content loading
            if (url.contains("api") && apiResponse.getStatus() == 200) {
                log.debug("Detected API data load: {}", apiResponse.getUrl());
                extractDynamicContent(apiResponse);
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing API response: {}", e.getMessage());
        }
    }
    
    /**
     * Extract authentication data from API responses
     */
    private void extractAuthenticationData(ApiResponse apiResponse) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) apiResponse.getJsonData();
            
            if (data.containsKey("token") || data.containsKey("access_token")) {
                log.info("Authentication token detected in API response");
                // Store token for potential use in subsequent requests
            }
            
            if (data.containsKey("user") || data.containsKey("profile")) {
                log.info("User profile data detected in API response");
                // Store user context information
            }
            
        } catch (Exception e) {
            log.debug("Error extracting authentication data: {}", e.getMessage());
        }
    }
    
    /**
     * Extract search results from API responses
     */
    private void extractSearchResults(ApiResponse apiResponse) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) apiResponse.getJsonData();
            
            // Common search result patterns
            if (data.containsKey("results") || data.containsKey("items") || data.containsKey("data")) {
                log.info("Search results detected: {} items", getResultCount(data));
            }
            
        } catch (Exception e) {
            log.debug("Error extracting search results: {}", e.getMessage());
        }
    }
    
    /**
     * Extract dynamic content from API responses
     */
    private void extractDynamicContent(ApiResponse apiResponse) {
        try {
            String rawBody = apiResponse.getRawBody();
            
            // Detect pagination data
            if (rawBody.contains("\"page\"") || rawBody.contains("\"offset\"") || rawBody.contains("\"limit\"")) {
                log.debug("Pagination data detected in API response");
            }
            
            // Detect form data
            if (rawBody.contains("\"form\"") || rawBody.contains("\"fields\"")) {
                log.debug("Form data detected in API response");
            }
            
        } catch (Exception e) {
            log.debug("Error extracting dynamic content: {}", e.getMessage());
        }
    }
    
    /**
     * Get network activity for a session
     */
    public List<NetworkCapture> getNetworkActivity(String sessionId) {
        return pageNetworkActivity.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * Get API responses for a session
     */
    public List<ApiResponse> getApiResponses(String sessionId) {
        return pageApiResponses.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * Get JSON data from API responses matching a pattern
     */
    public List<Object> getJsonDataMatching(String sessionId, String urlPattern) {
        return pageApiResponses.getOrDefault(sessionId, new ArrayList<>())
            .stream()
            .filter(response -> response.getUrl().toLowerCase().contains(urlPattern.toLowerCase()))
            .map(ApiResponse::getJsonData)
            .toList();
    }
    
    /**
     * Clean up network data for a session
     */
    public void cleanupSession(String sessionId) {
        pageNetworkActivity.remove(sessionId);
        pageApiResponses.remove(sessionId);
        log.debug("Cleaned up network data for session: {}", sessionId);
    }
    
    /**
     * Check if a response is interesting enough to log
     */
    private boolean isInterestingResponse(Response response) {
        String contentType = response.headers().get("content-type");
        String url = response.url();
        
        // API responses
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }
        
        // Authentication or important endpoints
        if (url.contains("login") || url.contains("auth") || url.contains("api")) {
            return true;
        }
        
        // Error responses
        if (response.status() >= 400) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get result count from search API response
     */
    private int getResultCount(Map<String, Object> data) {
        try {
            if (data.containsKey("results") && data.get("results") instanceof List) {
                return ((List<?>) data.get("results")).size();
            }
            if (data.containsKey("items") && data.get("items") instanceof List) {
                return ((List<?>) data.get("items")).size();
            }
            if (data.containsKey("total")) {
                return Integer.parseInt(data.get("total").toString());
            }
        } catch (Exception e) {
            log.debug("Error counting results: {}", e.getMessage());
        }
        return 0;
    }
    
    // Data classes for network capture
    @lombok.Data
    @lombok.Builder
    public static class NetworkCapture {
        private String sessionId;
        private String method;
        private String url;
        private Map<String, String> headers;
        private String postData;
        private String resourceType;
        private long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ApiResponse {
        private String sessionId;
        private String url;
        private int status;
        private Map<String, String> headers;
        private String contentType;
        private Object jsonData;
        private String rawBody;
        private long timestamp;
    }
}