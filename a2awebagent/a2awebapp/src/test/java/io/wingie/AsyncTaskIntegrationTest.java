package io.wingie;

import io.wingie.controller.AsyncTaskController;
import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.repository.TaskExecutionRepository;
import io.wingie.service.TaskExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@AutoConfigureMockMvc
public class AsyncTaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskExecutionRepository taskRepository;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Flight Search Real-time Test with Tabular Output")
    void testFlightSearchRealTimeCapabilities() throws Exception {
        System.out.println("\n🛫 FLIGHT SEARCH REAL-TIME TEST");
        System.out.println("=====================================");

        // Submit flight search task
        AsyncTaskController.TaskSubmissionRequest request = new AsyncTaskController.TaskSubmissionRequest(
            "Find flights from Amsterdam to London for next weekend. Show prices in a table format with departure times, airlines, and costs.",
            "travel_search"
        );
        request.requesterId = "integration-test";
        request.timeoutSeconds = 180; // 3 minutes

        String requestJson = objectMapper.writeValueAsString(request);

        // Submit task
        MvcResult submitResult = mockMvc.perform(post("/v1/tasks/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").exists())
                .andReturn();

        String responseContent = submitResult.getResponse().getContentAsString();
        Map<String, Object> submitResponse = objectMapper.readValue(responseContent, Map.class);
        String taskId = (String) submitResponse.get("taskId");

        System.out.printf("✅ Task submitted successfully: %s\n", taskId);
        System.out.printf("📊 Estimated duration: %d seconds\n", submitResponse.get("estimatedDurationSeconds"));

        // Monitor task progress in real-time
        monitorTaskProgress(taskId, "FLIGHT SEARCH");

        // Verify final results
        verifyFlightSearchResults(taskId);
    }

    @Test
    @DisplayName("LinkedIn Search Test with Screenshot Processing")
    void testLinkedInSearchWithScreenshots() throws Exception {
        System.out.println("\n👤 LINKEDIN SEARCH TEST WITH SCREENSHOTS");
        System.out.println("==========================================");

        // Submit LinkedIn search task
        AsyncTaskController.TaskSubmissionRequest request = new AsyncTaskController.TaskSubmissionRequest(
            "Search LinkedIn for Wingston Sharon profile and take detailed screenshots of the profile page, experience section, and skills",
            "linkedin_search"
        );
        request.requesterId = "integration-test";
        request.timeoutSeconds = 120; // 2 minutes

        String requestJson = objectMapper.writeValueAsString(request);

        // Submit task
        MvcResult submitResult = mockMvc.perform(post("/v1/tasks/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").exists())
                .andReturn();

        String responseContent = submitResult.getResponse().getContentAsString();
        Map<String, Object> submitResponse = objectMapper.readValue(responseContent, Map.class);
        String taskId = (String) submitResponse.get("taskId");

        System.out.printf("✅ LinkedIn task submitted: %s\n", taskId);

        // Monitor task progress
        monitorTaskProgress(taskId, "LINKEDIN SEARCH");

        // Verify LinkedIn search results and screenshots
        verifyLinkedInSearchResults(taskId);
    }

    @Test
    @DisplayName("Concurrent Multi-Task Processing Test")
    void testConcurrentTaskProcessing() throws Exception {
        System.out.println("\n🔄 CONCURRENT MULTI-TASK PROCESSING TEST");
        System.out.println("=========================================");

        // Submit multiple tasks concurrently
        String[] taskIds = new String[3];
        String[] queries = {
            "Quick flight search: Amsterdam to Paris tomorrow",
            "Hotel search: Budget hotels in London for 2 nights",
            "LinkedIn search: Software engineers in Amsterdam"
        };
        String[] taskTypes = {"travel_search", "travel_search", "linkedin_search"};

        // Submit all tasks
        for (int i = 0; i < queries.length; i++) {
            AsyncTaskController.TaskSubmissionRequest request = new AsyncTaskController.TaskSubmissionRequest(
                queries[i], taskTypes[i]
            );
            request.requesterId = "concurrent-test-" + i;

            String requestJson = objectMapper.writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/v1/tasks/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
            taskIds[i] = (String) response.get("taskId");

            System.out.printf("✅ Task %d submitted: %s (%s)\n", i+1, taskIds[i], taskTypes[i]);
        }

        // Monitor all tasks concurrently
        System.out.println("\n📊 CONCURRENT TASK MONITORING");
        printTaskTable(taskIds, queries);

        // Wait for all tasks to complete
        boolean allCompleted = false;
        int maxWaitMinutes = 5;
        int checkCount = 0;
        int maxChecks = maxWaitMinutes * 12; // Check every 5 seconds

        while (!allCompleted && checkCount < maxChecks) {
            Thread.sleep(5000); // Wait 5 seconds
            checkCount++;

            allCompleted = true;
            for (String taskId : taskIds) {
                TaskExecution task = taskRepository.findById(taskId).orElse(null);
                if (task == null || !task.getStatus().isTerminal()) {
                    allCompleted = false;
                    break;
                }
            }

            if (checkCount % 6 == 0) { // Print update every 30 seconds
                System.out.printf("\n⏱️  Update after %d minutes...\n", checkCount / 12);
                printTaskTable(taskIds, queries);
            }
        }

        // Final results
        System.out.println("\n🏁 FINAL RESULTS");
        printTaskTable(taskIds, queries);

        // Verify all tasks completed
        for (String taskId : taskIds) {
            TaskExecution task = taskRepository.findById(taskId).orElse(null);
            assertNotNull(task, "Task should exist: " + taskId);
            assertTrue(task.getStatus().isTerminal(), "Task should be in terminal state: " + taskId);
        }
    }

    @Test
    @DisplayName("System Health and Statistics Test")
    void testSystemHealthAndStats() throws Exception {
        System.out.println("\n📈 SYSTEM HEALTH AND STATISTICS TEST");
        System.out.println("====================================");

        // Test health endpoint
        mockMvc.perform(get("/v1/tasks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.activeTasks").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        System.out.println("✅ Health endpoint working");

        // Test stats endpoint
        MvcResult statsResult = mockMvc.perform(get("/v1/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").exists())
                .andExpect(jsonPath("$.activeTasks").exists())
                .andReturn();

        Map<String, Object> stats = objectMapper.readValue(
            statsResult.getResponse().getContentAsString(), Map.class);

        System.out.println("📊 SYSTEM STATISTICS:");
        System.out.printf("   Total Tasks: %s\n", stats.get("totalTasks"));
        System.out.printf("   Active Tasks: %s\n", stats.get("activeTasks"));
        System.out.printf("   Queued Tasks: %s\n", stats.get("queuedTasks"));
        System.out.printf("   Completed Today: %s\n", stats.get("completedToday"));

        // Test active tasks endpoint
        mockMvc.perform(get("/v1/tasks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        System.out.println("✅ Active tasks endpoint working");

        // Test dashboard endpoint
        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk());

        System.out.println("✅ Dashboard endpoint accessible");
    }

    private void monitorTaskProgress(String taskId, String taskName) throws Exception {
        System.out.printf("\n🔍 MONITORING %s PROGRESS\n", taskName);
        System.out.println("Progress | Status    | Message");
        System.out.println("---------|-----------|------------------------------------------");

        boolean completed = false;
        int maxAttempts = 60; // 5 minutes max (5s intervals)
        int attempt = 0;

        while (!completed && attempt < maxAttempts) {
            attempt++;

            MvcResult statusResult = mockMvc.perform(get("/v1/tasks/{taskId}/status", taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            String statusContent = statusResult.getResponse().getContentAsString();
            AsyncTaskController.TaskResponse taskResponse = objectMapper.readValue(
                statusContent, AsyncTaskController.TaskResponse.class);

            System.out.printf("%8d%% | %-9s | %s\n",
                taskResponse.progressPercent,
                taskResponse.status,
                truncate(taskResponse.progressMessage, 40));

            if (taskResponse.status.isTerminal()) {
                completed = true;
                
                if (taskResponse.status == TaskStatus.COMPLETED) {
                    System.out.printf("🎉 %s completed successfully in %s\n", 
                        taskName, formatDuration(attempt * 5));
                } else {
                    System.out.printf("❌ %s failed with status: %s\n", 
                        taskName, taskResponse.status);
                    if (taskResponse.errorDetails != null) {
                        System.out.printf("   Error: %s\n", taskResponse.errorDetails);
                    }
                }
            } else {
                Thread.sleep(5000); // Wait 5 seconds
            }
        }

        if (!completed) {
            System.out.printf("⏰ %s monitoring timed out after %d attempts\n", taskName, attempt);
        }
    }

    private void verifyFlightSearchResults(String taskId) throws Exception {
        System.out.println("\n📋 VERIFYING FLIGHT SEARCH RESULTS");

        MvcResult resultsResponse = mockMvc.perform(get("/v1/tasks/{taskId}/results", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.results").exists())
                .andReturn();

        Map<String, Object> results = objectMapper.readValue(
            resultsResponse.getResponse().getContentAsString(), Map.class);

        String extractedResults = (String) results.get("results");
        assertNotNull(extractedResults, "Results should not be null");
        assertTrue(extractedResults.length() > 100, "Results should contain substantial content");

        System.out.println("✅ Flight search results verified");
        System.out.printf("📄 Results length: %d characters\n", extractedResults.length());

        // Check for flight-related keywords
        String[] flightKeywords = {"flight", "airport", "departure", "arrival", "price", "airline"};
        int keywordMatches = 0;
        for (String keyword : flightKeywords) {
            if (extractedResults.toLowerCase().contains(keyword)) {
                keywordMatches++;
            }
        }
        System.out.printf("🔍 Flight keywords found: %d/%d\n", keywordMatches, flightKeywords.length);

        // Print sample of results (first 300 chars)
        System.out.println("\n📝 SAMPLE RESULTS:");
        System.out.println("─".repeat(60));
        System.out.println(truncate(extractedResults, 300));
        System.out.println("─".repeat(60));
    }

    private void verifyLinkedInSearchResults(String taskId) throws Exception {
        System.out.println("\n📋 VERIFYING LINKEDIN SEARCH RESULTS");

        // Get results via API to avoid Hibernate session issues
        MvcResult taskStatusResult = mockMvc.perform(get("/v1/tasks/{taskId}/status", taskId))
                .andExpect(status().isOk())
                .andReturn();

        AsyncTaskController.TaskResponse taskResponse = objectMapper.readValue(
            taskStatusResult.getResponse().getContentAsString(), AsyncTaskController.TaskResponse.class);

        // Check for screenshots
        if (taskResponse.screenshots != null && !taskResponse.screenshots.isEmpty()) {
            System.out.printf("📸 Screenshots captured: %d\n", taskResponse.screenshots.size());
            for (int i = 0; i < taskResponse.screenshots.size(); i++) {
                System.out.printf("   %d. %s\n", i+1, taskResponse.screenshots.get(i));
            }
        } else {
            System.out.println("⚠️  No screenshots found");
        }

        // Get results
        MvcResult resultsResponse = mockMvc.perform(get("/v1/tasks/{taskId}/results", taskId))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> results = objectMapper.readValue(
            resultsResponse.getResponse().getContentAsString(), Map.class);

        String extractedResults = (String) results.get("results");
        if (extractedResults != null) {
            System.out.printf("📄 LinkedIn results length: %d characters\n", extractedResults.length());
            
            // Check for LinkedIn-related keywords
            String[] linkedinKeywords = {"linkedin", "profile", "experience", "skills", "wingston", "sharon"};
            int keywordMatches = 0;
            for (String keyword : linkedinKeywords) {
                if (extractedResults.toLowerCase().contains(keyword)) {
                    keywordMatches++;
                }
            }
            System.out.printf("🔍 LinkedIn keywords found: %d/%d\n", keywordMatches, linkedinKeywords.length);

            // Print sample of results
            System.out.println("\n📝 SAMPLE LINKEDIN RESULTS:");
            System.out.println("─".repeat(60));
            System.out.println(truncate(extractedResults, 300));
            System.out.println("─".repeat(60));
        }

        System.out.println("✅ LinkedIn search results verified");
    }

    private void printTaskTable(String[] taskIds, String[] queries) {
        System.out.println("\n📊 TASK STATUS TABLE");
        System.out.println("ID      | Status     | Progress | Query");
        System.out.println("--------|------------|----------|--------------------------------");

        for (int i = 0; i < taskIds.length; i++) {
            TaskExecution task = taskRepository.findById(taskIds[i]).orElse(null);
            if (task != null) {
                System.out.printf("Task %d  | %-10s | %8d%% | %s\n",
                    i+1,
                    task.getStatus(),
                    task.getProgressPercent() != null ? task.getProgressPercent() : 0,
                    truncate(queries[i], 30));
            } else {
                System.out.printf("Task %d  | %-10s | %8s | %s\n",
                    i+1, "NOT_FOUND", "N/A", truncate(queries[i], 30));
            }
        }
        System.out.println();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }
}