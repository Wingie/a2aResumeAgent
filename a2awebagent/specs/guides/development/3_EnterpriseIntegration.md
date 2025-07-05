# Tutorial 3: Enterprise Integration with a2acore Framework and Spring Boot

## 1. Introduction

This comprehensive guide demonstrates how to integrate a2aTravelAgent with enterprise systems using the a2acore framework, Spring Boot, and modern integration patterns. Learn how to create MCP tools that interact with REST APIs, microservices, databases, and external enterprise systems while maintaining security and scalability.

## 2. Architecture Overview

### 2.1 Enterprise Integration Stack
```
a2aTravelAgent Enterprise Integration:
├── a2acore Framework          # MCP tool foundation
│   ├── @EnableA2ACore        # Auto-configuration
│   ├── A2aCoreController     # Unified MCP endpoints
│   └── JsonRpcHandler        # Protocol processing
├── Spring Boot Integration    # Enterprise patterns
│   ├── RestTemplate/WebClient # HTTP client libraries
│   ├── Spring Security       # Authentication/authorization
│   ├── Spring Data JPA       # Database integration
│   └── Spring Cloud          # Microservices support
├── External System Adapters  # Enterprise connectors
│   ├── REST API Clients      # OpenAPI/Swagger integration
│   ├── Database Connectors   # Multiple database support
│   ├── Message Queues        # RabbitMQ, Apache Kafka
│   └── Enterprise Service Bus # ESB integration
└── Monitoring & Observability # Production monitoring
    ├── Spring Boot Actuator  # Health checks
    ├── Micrometer Metrics    # Performance monitoring
    └── Distributed Tracing   # Request tracking
```

### 2.2 Integration Benefits
- **Unified Interface**: Single MCP endpoint for all enterprise systems
- **Type-Safe APIs**: Automatic schema generation from @Action methods
- **Security Integration**: Spring Security for authentication and authorization
- **Performance Optimization**: Caching, connection pooling, async processing
- **Monitoring**: Built-in metrics and health checks

## 3. REST API Integration

### 3.1 Modern REST Client Configuration
```java
package io.wingie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .defaultHeader("User-Agent", "a2aTravelAgent/1.0")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024));
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(30))
            .additionalInterceptors(loggingInterceptor())
            .build();
    }
    
    @Bean
    public ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.info("REST Request: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        };
    }
}
```

### 3.2 Enterprise API Integration Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.annotation.Cacheable;
import reactor.core.publisher.Mono;

@Service
@Agent(name = "enterprise-api", 
       description = "Enterprise system API integration and automation")
public class EnterpriseApiService {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ApiSecurityService securityService;
    
    @Action(description = "Fetch user details from enterprise user management system")
    @Cacheable(value = "user-details", key = "#userId")
    public String getUserDetails(
        @Parameter(description = "Employee user ID") String userId,
        @Parameter(description = "API endpoint base URL") String baseUrl) {
        
        try {
            WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + securityService.getApiToken())
                .build();
            
            UserDetails user = client.get()
                .uri("/api/v1/users/{userId}", userId)
                .retrieve()
                .bodyToMono(UserDetails.class)
                .block();
                
            return formatUserDetails(user);
            
        } catch (Exception e) {
            log.error("Failed to fetch user details for: {}", userId, e);
            return "Error fetching user details: " + e.getMessage();
        }
    }
    
    @Action(description = "Create support ticket in enterprise ticketing system")
    public String createSupportTicket(
        @Parameter(description = "Support ticket details") SupportTicketRequest request) {
        
        try {
            WebClient client = webClientBuilder
                .baseUrl("https://api.company.com/")
                .defaultHeader("X-API-Key", securityService.getApiKey())
                .build();
            
            SupportTicketResponse response = client.post()
                .uri("/api/v1/tickets")
                .body(Mono.just(request), SupportTicketRequest.class)
                .retrieve()
                .bodyToMono(SupportTicketResponse.class)
                .block();
                
            // Save ticket locally for tracking
            ticketRepository.save(convertToEntity(response));
            
            return "Support ticket created successfully. Ticket ID: " + response.getTicketId();
            
        } catch (Exception e) {
            log.error("Failed to create support ticket", e);
            return "Error creating support ticket: " + e.getMessage();
        }
    }
    
    @Action(description = "Query enterprise inventory system for product information")
    public String queryInventory(
        @Parameter(description = "Product search criteria") ProductSearchCriteria criteria) {
        
        try {
            WebClient client = webClientBuilder
                .baseUrl("https://inventory.company.com/")
                .build();
            
            List<Product> products = client.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/products/search")
                    .queryParam("category", criteria.getCategory())
                    .queryParam("location", criteria.getLocation())
                    .queryParam("minStock", criteria.getMinStockLevel())
                    .build())
                .headers(headers -> headers.setBearerAuth(securityService.getInventoryToken()))
                .retrieve()
                .bodyToFlux(Product.class)
                .collectList()
                .block();
                
            return formatInventoryResults(products);
            
        } catch (Exception e) {
            log.error("Inventory query failed", e);
            return "Error querying inventory: " + e.getMessage();
        }
    }
}
```

### 3.3 Dynamic API Configuration
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;
import java.util.List;

@Service
@Agent(name = "dynamic-api-config", 
       description = "Dynamic configuration and management of enterprise API endpoints")
@ConfigurationProperties(prefix = "enterprise.apis")
public class DynamicApiConfigService {
    
    private Map<String, ApiEndpointConfig> endpoints;
    
    @Action(description = "Execute API call with dynamic endpoint configuration")
    public String executeApiCall(
        @Parameter(description = "API endpoint identifier") String endpointId,
        @Parameter(description = "API parameters as JSON") String parametersJson,
        @Parameter(description = "HTTP method (GET, POST, PUT, DELETE)") String method) {
        
        ApiEndpointConfig config = endpoints.get(endpointId);
        if (config == null) {
            return "Error: Unknown endpoint ID: " + endpointId;
        }
        
        try {
            Map<String, Object> parameters = objectMapper.readValue(parametersJson, Map.class);
            
            WebClient client = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .build();
                
            // Add authentication headers
            WebClient.RequestHeadersSpec<?> request = buildRequest(client, config, method, parameters);
            
            String response = request.retrieve()
                .bodyToMono(String.class)
                .block();
                
            // Log API call for monitoring
            apiCallLogger.logCall(endpointId, method, parameters, response);
            
            return response;
            
        } catch (Exception e) {
            log.error("Dynamic API call failed for endpoint: {}", endpointId, e);
            return "Error executing API call: " + e.getMessage();
        }
    }
    
    @Action(description = "List all configured enterprise API endpoints")
    public String listAvailableEndpoints() {
        StringBuilder result = new StringBuilder("Available Enterprise API Endpoints:\n");
        
        endpoints.forEach((id, config) -> {
            result.append(String.format("- %s: %s (%s)\n", 
                id, config.getDescription(), config.getBaseUrl()));
        });
        
        return result.toString();
    }
}

@Data
public class ApiEndpointConfig {
    private String baseUrl;
    private String description;
    private String authType; // Bearer, ApiKey, Basic, None
    private String authHeader;
    private Map<String, String> defaultHeaders;
    private int timeoutSeconds;
}
```

## 4. Database Integration

### 4.1 Multi-Database Configuration
```java
package io.wingie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

@Configuration
public class DatabaseConfig {
    
    @Primary
    @Bean
    @ConfigurationProperties("app.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("app.datasource.enterprise")
    public DataSource enterpriseDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("app.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### 4.2 Enterprise Database Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@Agent(name = "enterprise-database", 
       description = "Enterprise database integration and data management")
public class EnterpriseDatabaseService {
    
    @Autowired
    @Qualifier("primaryDataSource")
    private JdbcTemplate primaryJdbcTemplate;
    
    @Autowired
    @Qualifier("enterpriseDataSource") 
    private JdbcTemplate enterpriseJdbcTemplate;
    
    @Action(description = "Query employee information from enterprise HR database")
    public String queryEmployeeData(
        @Parameter(description = "Employee ID or search criteria") String searchCriteria,
        @Parameter(description = "Department filter (optional)") String department) {
        
        try {
            String sql = buildEmployeeQuery(searchCriteria, department);
            
            List<Map<String, Object>> employees = enterpriseJdbcTemplate.queryForList(sql);
            
            return formatEmployeeResults(employees);
            
        } catch (Exception e) {
            log.error("Employee query failed", e);
            return "Error querying employee data: " + e.getMessage();
        }
    }
    
    @Action(description = "Execute custom SQL query on enterprise database")
    @Transactional(readOnly = true)
    public String executeCustomQuery(
        @Parameter(description = "SQL query to execute") String sqlQuery,
        @Parameter(description = "Database identifier (primary, enterprise, analytics)") String database) {
        
        // Security validation
        if (!queryValidator.isQuerySafe(sqlQuery)) {
            return "Error: Query contains potentially unsafe operations";
        }
        
        try {
            JdbcTemplate template = getJdbcTemplate(database);
            
            List<Map<String, Object>> results = template.queryForList(sqlQuery);
            
            return formatQueryResults(results);
            
        } catch (Exception e) {
            log.error("Custom query execution failed", e);
            return "Error executing query: " + e.getMessage();
        }
    }
    
    @Action(description = "Generate analytics report from multiple database sources")
    public String generateAnalyticsReport(
        @Parameter(description = "Report type and parameters") AnalyticsReportRequest request) {
        
        try {
            // Query primary database for current data
            String currentData = queryCurrentData(request);
            
            // Query analytics database for historical trends
            String historicalData = queryHistoricalData(request);
            
            // Combine and format results
            AnalyticsReport report = analyticsProcessor.generateReport(
                currentData, historicalData, request);
            
            // Save report for future reference
            reportRepository.save(convertToEntity(report));
            
            return formatAnalyticsReport(report);
            
        } catch (Exception e) {
            log.error("Analytics report generation failed", e);
            return "Error generating analytics report: " + e.getMessage();
        }
    }
}
```

## 5. Message Queue Integration

### 5.1 RabbitMQ Configuration
```java
package io.wingie.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {
    
    public static final String ENTERPRISE_EVENTS_QUEUE = "enterprise.events";
    public static final String INTEGRATION_RESPONSES_QUEUE = "integration.responses";
    
    @Bean
    public Queue enterpriseEventsQueue() {
        return QueueBuilder.durable(ENTERPRISE_EVENTS_QUEUE).build();
    }
    
    @Bean
    public Queue integrationResponsesQueue() {
        return QueueBuilder.durable(INTEGRATION_RESPONSES_QUEUE).build();
    }
    
    @Bean
    public TopicExchange enterpriseExchange() {
        return new TopicExchange("enterprise.exchange");
    }
    
    @Bean
    public Binding enterpriseEventsBinding() {
        return BindingBuilder
            .bind(enterpriseEventsQueue())
            .to(enterpriseExchange())
            .with("enterprise.events.*");
    }
}
```

### 5.2 Message Queue Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@Service
@Agent(name = "message-queue", 
       description = "Enterprise message queue integration for async processing")
public class MessageQueueService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Action(description = "Send message to enterprise event queue")
    public String sendEnterpriseEvent(
        @Parameter(description = "Event type") String eventType,
        @Parameter(description = "Event data as JSON") String eventData,
        @Parameter(description = "Priority level (LOW, MEDIUM, HIGH)") String priority) {
        
        try {
            EnterpriseEvent event = EnterpriseEvent.builder()
                .eventType(eventType)
                .eventData(eventData)
                .priority(Priority.valueOf(priority.toUpperCase()))
                .timestamp(LocalDateTime.now())
                .source("a2aTravelAgent")
                .build();
            
            String routingKey = "enterprise.events." + eventType.toLowerCase();
            rabbitTemplate.convertAndSend("enterprise.exchange", routingKey, event);
            
            log.info("Enterprise event sent: {}", event.getEventId());
            return "Event sent successfully with ID: " + event.getEventId();
            
        } catch (Exception e) {
            log.error("Failed to send enterprise event", e);
            return "Error sending event: " + e.getMessage();
        }
    }
    
    @Action(description = "Query message queue status and pending messages")
    public String getQueueStatus(
        @Parameter(description = "Queue name to check") String queueName) {
        
        try {
            Properties queueInfo = rabbitTemplate.execute(channel -> {
                AMQP.Queue.DeclareOk response = channel.queueDeclarePassive(queueName);
                Properties props = new Properties();
                props.setProperty("messageCount", String.valueOf(response.getMessageCount()));
                props.setProperty("consumerCount", String.valueOf(response.getConsumerCount()));
                return props;
            });
            
            return String.format("Queue %s: %s messages, %s consumers", 
                queueName, 
                queueInfo.getProperty("messageCount"),
                queueInfo.getProperty("consumerCount"));
                
        } catch (Exception e) {
            log.error("Failed to get queue status", e);
            return "Error getting queue status: " + e.getMessage();
        }
    }
    
    @RabbitListener(queues = INTEGRATION_RESPONSES_QUEUE)
    public void handleIntegrationResponse(IntegrationResponse response) {
        log.info("Received integration response: {}", response);
        
        // Process response and update local state
        integrationResponseProcessor.process(response);
    }
}
```

## 6. Security and Authentication

### 6.1 OAuth2 and JWT Integration
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

@Service
@Agent(name = "enterprise-auth", 
       description = "Enterprise authentication and authorization management")
public class EnterpriseAuthService {
    
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Action(description = "Authenticate with enterprise SSO system")
    public String authenticateWithSSO(
        @Parameter(description = "Username") String username,
        @Parameter(description = "Enterprise domain") String domain,
        @Parameter(description = "Authentication method (SAML, OAuth2, LDAP)") String authMethod) {
        
        try {
            switch (authMethod.toUpperCase()) {
                case "OAUTH2":
                    return authenticateOAuth2(username, domain);
                case "SAML":
                    return authenticateSAML(username, domain);
                case "LDAP":
                    return authenticateLDAP(username, domain);
                default:
                    return "Error: Unsupported authentication method: " + authMethod;
            }
        } catch (Exception e) {
            log.error("Enterprise authentication failed", e);
            return "Authentication failed: " + e.getMessage();
        }
    }
    
    @Action(description = "Validate JWT token and extract user permissions")
    public String validateJwtToken(
        @Parameter(description = "JWT token to validate") String jwtToken) {
        
        try {
            if (jwtTokenService.validateToken(jwtToken)) {
                UserPrincipal user = jwtTokenService.extractUserPrincipal(jwtToken);
                List<String> permissions = jwtTokenService.extractPermissions(jwtToken);
                
                return String.format("Token valid. User: %s, Permissions: %s", 
                    user.getUsername(), String.join(", ", permissions));
            } else {
                return "Token validation failed: Invalid or expired token";
            }
        } catch (Exception e) {
            log.error("JWT token validation failed", e);
            return "Token validation error: " + e.getMessage();
        }
    }
    
    private String authenticateOAuth2(String username, String domain) {
        // OAuth2 authentication logic
        OAuth2AuthorizedClient client = authorizedClientService
            .loadAuthorizedClient("enterprise", username);
            
        if (client != null) {
            return "OAuth2 authentication successful. Access token expires: " + 
                   client.getAccessToken().getExpiresAt();
        } else {
            return "OAuth2 authentication failed";
        }
    }
}
```

## 7. Microservices Integration

### 7.1 Service Discovery and Load Balancing
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

@Service
@Agent(name = "microservices", 
       description = "Enterprise microservices integration and orchestration")
public class MicroservicesIntegrationService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    @LoadBalanced
    private RestTemplate loadBalancedRestTemplate;
    
    @Action(description = "Discover and list available enterprise microservices")
    public String discoverServices() {
        try {
            List<String> services = discoveryClient.getServices();
            StringBuilder result = new StringBuilder("Available Enterprise Services:\n");
            
            for (String serviceName : services) {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                result.append(String.format("- %s: %d instances\n", serviceName, instances.size()));
                
                for (ServiceInstance instance : instances) {
                    result.append(String.format("  * %s:%d (health: %s)\n", 
                        instance.getHost(), 
                        instance.getPort(),
                        checkServiceHealth(instance)));
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Service discovery failed", e);
            return "Error discovering services: " + e.getMessage();
        }
    }
    
    @Action(description = "Execute distributed transaction across multiple microservices")
    @Transactional
    public String executeDistributedTransaction(
        @Parameter(description = "Transaction request details") DistributedTransactionRequest request) {
        
        try {
            String transactionId = UUID.randomUUID().toString();
            log.info("Starting distributed transaction: {}", transactionId);
            
            // Step 1: Reserve inventory
            String inventoryResult = callMicroservice("inventory-service", 
                "/api/v1/inventory/reserve", request.getInventoryRequest());
            
            // Step 2: Process payment
            String paymentResult = callMicroservice("payment-service",
                "/api/v1/payments/process", request.getPaymentRequest());
            
            // Step 3: Create booking
            String bookingResult = callMicroservice("booking-service",
                "/api/v1/bookings/create", request.getBookingRequest());
            
            // Step 4: Send confirmation
            String notificationResult = callMicroservice("notification-service",
                "/api/v1/notifications/send", request.getNotificationRequest());
            
            return String.format("Distributed transaction completed successfully. ID: %s\n" +
                "Inventory: %s\nPayment: %s\nBooking: %s\nNotification: %s",
                transactionId, inventoryResult, paymentResult, bookingResult, notificationResult);
                
        } catch (Exception e) {
            log.error("Distributed transaction failed", e);
            // Implement compensation logic here
            return "Transaction failed: " + e.getMessage();
        }
    }
    
    private String callMicroservice(String serviceName, String endpoint, Object request) {
        String url = "http://" + serviceName + endpoint;
        return loadBalancedRestTemplate.postForObject(url, request, String.class);
    }
}
```

## 8. Monitoring and Observability

### 8.1 Enterprise Monitoring Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;

@Service
@Agent(name = "enterprise-monitoring", 
       description = "Enterprise system monitoring and observability")
public class EnterpriseMonitoringService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    @Action(description = "Check health status of all enterprise integrations")
    public String checkSystemHealth() {
        StringBuilder report = new StringBuilder("Enterprise System Health Report:\n");
        report.append("Generated at: ").append(LocalDateTime.now()).append("\n\n");
        
        // Check database connections
        report.append("Database Connections:\n");
        report.append(checkDatabaseHealth());
        
        // Check external APIs
        report.append("\nExternal APIs:\n");
        report.append(checkApiHealth());
        
        // Check message queues
        report.append("\nMessage Queues:\n");
        report.append(checkQueueHealth());
        
        // Check microservices
        report.append("\nMicroservices:\n");
        report.append(checkMicroservicesHealth());
        
        return report.toString();
    }
    
    @Action(description = "Generate performance metrics report")
    public String generateMetricsReport(
        @Parameter(description = "Time period for metrics (HOUR, DAY, WEEK)") String period) {
        
        try {
            Duration duration = Duration.ofDays(1);
            switch (period.toUpperCase()) {
                case "HOUR": duration = Duration.ofHours(1); break;
                case "WEEK": duration = Duration.ofDays(7); break;
            }
            
            StringBuilder report = new StringBuilder("Performance Metrics Report:\n");
            report.append("Period: ").append(period).append("\n\n");
            
            // API call metrics
            Counter apiCallsCounter = meterRegistry.find("api.calls").counter();
            if (apiCallsCounter != null) {
                report.append("API Calls: ").append(apiCallsCounter.count()).append("\n");
            }
            
            // Database query metrics
            Timer dbQueryTimer = meterRegistry.find("database.queries").timer();
            if (dbQueryTimer != null) {
                report.append("Database Queries: ").append(dbQueryTimer.count())
                      .append(" (avg duration: ").append(dbQueryTimer.mean(TimeUnit.MILLISECONDS))
                      .append("ms)\n");
            }
            
            // Integration response times
            Timer integrationTimer = meterRegistry.find("integration.response.time").timer();
            if (integrationTimer != null) {
                report.append("Integration Response Time: avg ")
                      .append(integrationTimer.mean(TimeUnit.MILLISECONDS)).append("ms\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            log.error("Failed to generate metrics report", e);
            return "Error generating metrics report: " + e.getMessage();
        }
    }
}
```

## 9. Configuration Management

### 9.1 Dynamic Configuration Service
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Service
@RefreshScope
@ConfigurationProperties(prefix = "enterprise")
@Agent(name = "config-management", 
       description = "Enterprise configuration management and updates")
public class ConfigurationManagementService {
    
    private Map<String, ApiConfig> apis = new HashMap<>();
    private Map<String, DatabaseConfig> databases = new HashMap<>();
    
    @Action(description = "Update enterprise API configuration")
    public String updateApiConfig(
        @Parameter(description = "API identifier") String apiId,
        @Parameter(description = "Configuration updates as JSON") String configJson) {
        
        try {
            ApiConfig newConfig = objectMapper.readValue(configJson, ApiConfig.class);
            
            // Validate configuration
            if (configValidator.validateApiConfig(newConfig)) {
                apis.put(apiId, newConfig);
                
                // Trigger configuration refresh
                configurationRefreshService.refreshApiConfig(apiId);
                
                return "API configuration updated successfully for: " + apiId;
            } else {
                return "Configuration validation failed";
            }
            
        } catch (Exception e) {
            log.error("Failed to update API configuration", e);
            return "Error updating configuration: " + e.getMessage();
        }
    }
    
    @Action(description = "List all enterprise configurations")
    public String listConfigurations() {
        StringBuilder result = new StringBuilder("Enterprise Configurations:\n\n");
        
        result.append("API Configurations:\n");
        apis.forEach((id, config) -> {
            result.append(String.format("- %s: %s (enabled: %s)\n", 
                id, config.getBaseUrl(), config.isEnabled()));
        });
        
        result.append("\nDatabase Configurations:\n");
        databases.forEach((id, config) -> {
            result.append(String.format("- %s: %s:%d (pool size: %d)\n", 
                id, config.getHost(), config.getPort(), config.getPoolSize()));
        });
        
        return result.toString();
    }
}
```

## 10. Error Handling and Resilience

### 10.1 Circuit Breaker Implementation
```java
package io.wingie.service;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

@Service
@Agent(name = "resilient-integration", 
       description = "Resilient enterprise integration with fault tolerance")
public class ResilientIntegrationService {
    
    @Action(description = "Call external API with circuit breaker protection")
    @CircuitBreaker(name = "external-api", fallbackMethod = "fallbackApiCall")
    @Retry(name = "external-api")
    @Bulkhead(name = "external-api")
    public String callExternalApiWithResilience(
        @Parameter(description = "API endpoint URL") String apiUrl,
        @Parameter(description = "Request payload") String payload) {
        
        try {
            WebClient client = WebClient.builder()
                .baseUrl(apiUrl)
                .build();
                
            String response = client.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
                
            // Record successful call
            meterRegistry.counter("api.calls.success", "endpoint", apiUrl).increment();
            
            return response;
            
        } catch (Exception e) {
            // Record failed call
            meterRegistry.counter("api.calls.failure", "endpoint", apiUrl, 
                "error", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }
    
    public String fallbackApiCall(String apiUrl, String payload, Exception ex) {
        log.warn("API call failed, executing fallback for: {}", apiUrl, ex);
        
        // Try to get cached response
        String cachedResponse = cacheService.getCachedResponse(apiUrl, payload);
        if (cachedResponse != null) {
            return "Cached response: " + cachedResponse;
        }
        
        return "Service temporarily unavailable. Error: " + ex.getMessage();
    }
    
    @Action(description = "Get circuit breaker status for enterprise integrations")
    public String getCircuitBreakerStatus() {
        StringBuilder status = new StringBuilder("Circuit Breaker Status:\n");
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            status.append(String.format("- %s: %s (failure rate: %.2f%%)\n",
                cb.getName(), 
                cb.getState(), 
                cb.getMetrics().getFailureRate()));
        });
        
        return status.toString();
    }
}
```

## 11. Testing Enterprise Integrations

### 11.1 Integration Testing Framework
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "enterprise.apis.test-api.base-url=http://localhost:${wiremock.server.port}",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class EnterpriseIntegrationTest {
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8089))
        .build();
    
    @Autowired
    private EnterpriseApiService enterpriseApiService;
    
    @Test
    void testUserDetailsIntegration() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/v1/users/12345"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"12345\",\"name\":\"John Doe\",\"department\":\"Engineering\"}")));
        
        // When
        String result = enterpriseApiService.getUserDetails("12345", "http://localhost:8089");
        
        // Then
        assertThat(result).contains("John Doe");
        assertThat(result).contains("Engineering");
    }
}
```

## 12. Best Practices

### 12.1 Integration Guidelines
- **Error Handling**: Implement comprehensive error handling with meaningful messages
- **Security**: Always validate inputs and use proper authentication
- **Performance**: Use caching, connection pooling, and async processing
- **Monitoring**: Implement detailed logging and metrics collection
- **Resilience**: Use circuit breakers, retries, and fallback mechanisms

### 12.2 Configuration Management
- **Environment Separation**: Use profiles for different environments
- **Secrets Management**: Store sensitive data in secure vaults
- **Dynamic Updates**: Support runtime configuration updates
- **Validation**: Validate all configuration changes before applying

### 12.3 Security Considerations
- **Input Validation**: Validate all external inputs
- **Authentication**: Implement proper authentication for all integrations
- **Authorization**: Check permissions before executing operations
- **Audit Logging**: Log all integration activities for compliance

## 13. Conclusion

The a2acore framework provides a powerful foundation for enterprise integration with Spring Boot. Key advantages include:

### 13.1 Integration Benefits
- **Unified Interface**: Single MCP endpoint for all enterprise systems
- **Type Safety**: Automatic schema generation and validation
- **Performance**: Built-in caching and optimization
- **Security**: Integrated authentication and authorization

### 13.2 Enterprise Features
- **Scalability**: Support for microservices and distributed systems
- **Resilience**: Circuit breakers, retries, and fallback mechanisms
- **Monitoring**: Comprehensive metrics and health checks
- **Flexibility**: Support for multiple integration patterns

### 13.3 Production Readiness
- **Docker Support**: Container-based deployment
- **Configuration Management**: Dynamic configuration updates
- **Error Handling**: Comprehensive exception management
- **Testing**: Robust integration testing framework

This enterprise integration approach enables organizations to leverage AI-powered automation while maintaining security, performance, and reliability standards required for production environments.