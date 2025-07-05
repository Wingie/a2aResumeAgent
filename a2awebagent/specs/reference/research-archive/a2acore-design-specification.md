# A2acore Design Specification

**Document Date:** June 30, 2025  
**Project:** a2acore Library Design  
**Version:** 1.0.0-SNAPSHOT  
**Status:** Implementation Ready  

## Executive Summary

This document specifies the design for **a2acore** - a lightweight, fast-starting MCP (Model Context Protocol) library that will replace the a2ajava dependency in a2awebagent. The primary goal is to eliminate startup AI calls while maintaining full MCP protocol compatibility and supporting existing @Action annotated tools.

---

## 1. Project Structure Design

### 1.1 Multi-Module Maven Configuration

```
a2awebagent/                           # Parent project
├── pom.xml                            # Parent POM with modules
├── a2acore/                           # NEW: Core library submodule
│   ├── pom.xml                        # a2acore module POM
│   └── src/main/java/io/wingie/a2acore/
│       ├── annotation/                # @Action and related annotations
│       ├── domain/                    # MCP protocol data models
│       ├── server/                    # MCP server implementation
│       ├── discovery/                 # Tool discovery service
│       ├── execution/                 # Tool execution engine
│       └── cache/                     # Cache integration interface
├── src/main/java/io/wingie/           # Application code (uses a2acore)
└── target/
```

### 1.2 Package Structure

```java
io.wingie.a2acore/
├── annotation/
│   ├── Action.java                    # Simplified @Action annotation
│   ├── Agent.java                     # Optional @Agent grouping
│   └── Parameter.java                 # Parameter metadata
├── domain/
│   ├── Tool.java                      # MCP tool definition
│   ├── ToolInputSchema.java           # Tool parameter schema
│   ├── ToolAnnotations.java           # Tool metadata
│   ├── JsonRpc*.java                  # JSON-RPC protocol classes
│   └── Content*.java                  # MCP content types
├── server/
│   ├── A2aCoreController.java         # Main MCP controller
│   ├── JsonRpcHandler.java            # JSON-RPC processing
│   └── ToolEndpoints.java             # REST endpoints
├── discovery/
│   ├── ToolDiscoveryService.java      # Reflection-based tool finding
│   ├── MethodToolBuilder.java         # Creates tools from methods
│   └── SchemaGenerator.java           # Generates schemas from types
├── execution/
│   ├── ToolExecutor.java              # Executes tool calls
│   ├── ParameterMapper.java           # Maps JSON to Java parameters
│   └── ResultSerializer.java          # Serializes results to MCP format
└── cache/
    ├── ToolCacheProvider.java         # Interface for external caching
    └── NoOpCacheProvider.java         # Default no-cache implementation
```

---

## 2. Core Design Principles

### 2.1 Zero AI Calls During Startup
- **Static tool definitions** based on @Action annotations
- **Lazy description enhancement** only when explicitly requested
- **Cache integration** for enhanced descriptions
- **Fast reflection-based discovery** without AI processing

### 2.2 Minimal Dependencies
```xml
<!-- Core dependencies only -->
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
</dependencies>
```

### 2.3 Spring Boot Compatible but Not Dependent
- Works with or without Spring Boot
- Uses Spring's reflection utilities when available
- Graceful degradation for non-Spring environments

---

## 3. Key Components Design

### 3.1 A2aCoreController - Main MCP Server

```java
@RestController
@RequestMapping("/v1")
@Component
public class A2aCoreController {
    
    private final ToolDiscoveryService discoveryService;
    private final ToolExecutor toolExecutor;
    private final ToolCacheProvider cacheProvider;
    
    @PostConstruct
    public void initialize() {
        // Fast initialization - NO AI CALLS
        long start = System.currentTimeMillis();
        
        List<Tool> tools = discoveryService.discoverTools();
        toolExecutor.registerTools(tools);
        
        long duration = System.currentTimeMillis() - start;
        log.info("A2acore initialized {} tools in {}ms", tools.size(), duration);
        // Target: < 100ms initialization time
    }
    
    @GetMapping("/tools")
    public ResponseEntity<ListToolsResult> listTools() {
        return ResponseEntity.ok(new ListToolsResult(toolExecutor.getAllTools()));
    }
    
    @PostMapping("/tools/call")
    public ResponseEntity<JsonRpcResponse> callTool(@RequestBody JsonRpcRequest request) {
        try {
            Object result = toolExecutor.execute(request.getParams());
            return ResponseEntity.ok(JsonRpcResponse.success(request.getId(), result));
        } catch (Exception e) {
            return ResponseEntity.ok(JsonRpcResponse.error(request.getId(), e.getMessage()));
        }
    }
}
```

### 3.2 ToolDiscoveryService - Fast Reflection-Based Discovery

```java
@Service
public class ToolDiscoveryService {
    
    private final ApplicationContext applicationContext;
    private final MethodToolBuilder toolBuilder;
    
    public List<Tool> discoverTools() {
        List<Tool> tools = new ArrayList<>();
        
        // Scan all Spring beans for @Action methods
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
        
        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Action.class)) {
                    Tool tool = toolBuilder.buildTool(method, bean);
                    tools.add(tool);
                }
            }
        }
        
        log.info("Discovered {} @Action methods", tools.size());
        return tools;
    }
}
```

### 3.3 MethodToolBuilder - Creates Tools from Methods

```java
@Component
public class MethodToolBuilder {
    
    private final SchemaGenerator schemaGenerator;
    
    public Tool buildTool(Method method, Object bean) {
        Action action = method.getAnnotation(Action.class);
        
        return Tool.builder()
            .name(getToolName(method, action))
            .description(getToolDescription(method, action))
            .inputSchema(schemaGenerator.generateSchema(method))
            .annotations(createBasicAnnotations(action))
            .build();
    }
    
    private String getToolName(Method method, Action action) {
        return action.name().isEmpty() ? method.getName() : action.name();
    }
    
    private String getToolDescription(Method method, Action action) {
        return action.description().isEmpty() 
            ? "Tool for " + method.getName()
            : action.description();
    }
}
```

### 3.4 SchemaGenerator - No-AI Schema Generation

```java
@Component
public class SchemaGenerator {
    
    public ToolInputSchema generateSchema(Method method) {
        Map<String, ToolPropertySchema> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        // Use standard provideAllValuesInPlainEnglish parameter
        properties.put("provideAllValuesInPlainEnglish", 
            ToolPropertySchema.builder()
                .type("string")
                .description("Provide instructions for this tool in plain English")
                .build());
        
        required.add("provideAllValuesInPlainEnglish");
        
        return ToolInputSchema.builder()
            .type("object")
            .properties(properties)
            .required(required)
            .additionalProperties(false)
            .build();
    }
}
```

### 3.5 ToolExecutor - Method Invocation Engine

```java
@Component
public class ToolExecutor {
    
    private final Map<String, ToolMethod> toolMethods = new HashMap<>();
    private final ParameterMapper parameterMapper;
    private final ResultSerializer resultSerializer;
    
    public void registerTools(List<Tool> tools) {
        // Register tools and their corresponding methods
        for (Tool tool : tools) {
            ToolMethod toolMethod = findToolMethod(tool.getName());
            toolMethods.put(tool.getName(), toolMethod);
        }
    }
    
    public Object execute(ToolCallRequest request) throws Exception {
        ToolMethod toolMethod = toolMethods.get(request.getName());
        if (toolMethod == null) {
            throw new IllegalArgumentException("Unknown tool: " + request.getName());
        }
        
        // Map JSON arguments to method parameters
        Object[] args = parameterMapper.mapArguments(request.getArguments(), toolMethod);
        
        // Invoke the method
        Object result = toolMethod.getMethod().invoke(toolMethod.getBean(), args);
        
        // Serialize result to MCP format
        return resultSerializer.serialize(result);
    }
}
```

---

## 4. Integration with Existing a2awebagent

### 4.1 Dependency Replacement Strategy

**Before (a2ajava dependency):**
```xml
<dependency>
    <groupId>io.github.vishalmysore</groupId>
    <artifactId>a2ajava</artifactId>
    <version>0.1.9.6</version>
</dependency>
```

**After (a2acore internal module):**
```xml
<dependency>
    <groupId>io.wingie</groupId>
    <artifactId>a2acore</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 4.2 Controller Migration

**Before (Complex inheritance):**
```java
@RestController
public class MCPController extends MCPToolsController {
    // Complex caching logic
    // Component scanning exclusions needed
    // Dual controller conflicts
}
```

**After (Clean composition):**
```java
@RestController
public class WebAgentController {
    
    @Autowired
    private A2aCoreController a2aCore;
    
    // Simple delegation, no inheritance conflicts
    // No component scanning exclusions needed
}
```

### 4.3 Application.java Simplification

**Before (Complex exclusions):**
```java
@SpringBootApplication
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.github\\.vishalmysore\\..*")
})
public class Application {
```

**After (Clean configuration):**
```java
@SpringBootApplication
public class Application {
    // No exclusions needed - we control the library
}
```

---

## 5. Performance Specifications

### 5.1 Startup Performance Targets

| Metric | Current (a2ajava) | Target (a2acore) | Improvement |
|--------|-------------------|------------------|-------------|
| **Startup Time** | 30-60 seconds | <5 seconds | 85-90% faster |
| **Tool Discovery** | 5-10 seconds | <100ms | 98% faster |
| **AI Calls** | 8+ per startup | 0 per startup | 100% elimination |
| **Memory Usage** | 200MB+ | <100MB | 50% reduction |

### 5.2 Runtime Performance Targets

| Metric | Target | Description |
|--------|--------|-------------|
| **Tool Execution** | <100ms | Method invocation overhead |
| **JSON Serialization** | <10ms | Parameter mapping |
| **Cache Lookup** | <5ms | PostgreSQL integration |
| **Error Handling** | <1ms | Exception processing |

---

## 6. Cache Integration Design

### 6.1 Cache Provider Interface

```java
public interface ToolCacheProvider {
    
    Optional<String> getCachedDescription(String toolName, String providerModel);
    
    void cacheDescription(String toolName, String providerModel, 
                         String description, long generationTime);
    
    void updateUsageStats(String toolName, String providerModel);
    
    default boolean isEnabled() { return true; }
}
```

### 6.2 PostgreSQL Cache Integration

```java
@Component
@ConditionalOnBean(ToolDescriptionCacheService.class)
public class PostgreSQLCacheProvider implements ToolCacheProvider {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @Override
    public Optional<String> getCachedDescription(String toolName, String providerModel) {
        return cacheService.getCachedDescription(providerModel, toolName)
            .map(ToolDescription::getDescription);
    }
    
    // Bridge to existing PostgreSQL caching system
}
```

---

## 7. Error Handling Strategy

### 7.1 Graceful Degradation

```java
@Component
public class RobustToolExecutor implements ToolExecutor {
    
    @Override
    public Object execute(ToolCallRequest request) {
        try {
            return doExecute(request);
        } catch (IllegalArgumentException e) {
            return JsonRpcResponse.error(request.getId(), "Invalid arguments: " + e.getMessage());
        } catch (Exception e) {
            log.error("Tool execution failed: {}", request.getName(), e);
            return JsonRpcResponse.error(request.getId(), "Execution failed: " + e.getMessage());
        }
    }
}
```

### 7.2 Validation Framework

```java
@Component
public class ToolValidator {
    
    public void validateTool(Tool tool) {
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
        
        if (tool.getInputSchema() == null) {
            throw new IllegalArgumentException("Tool must have input schema");
        }
        
        // Additional validation rules
    }
}
```

---

## 8. Configuration Design

### 8.1 Application Properties

```yaml
a2acore:
  enabled: true
  discovery:
    scan-packages:
      - "io.wingie"
      - "com.example"
  execution:
    timeout-ms: 30000
    max-concurrent: 10
  cache:
    provider: postgresql
    enabled: true
```

### 8.2 Configuration Properties Class

```java
@ConfigurationProperties(prefix = "a2acore")
@Data
public class A2aCoreProperties {
    
    private boolean enabled = true;
    
    private Discovery discovery = new Discovery();
    private Execution execution = new Execution();
    private Cache cache = new Cache();
    
    @Data
    public static class Discovery {
        private List<String> scanPackages = Arrays.asList("io.wingie");
    }
    
    @Data
    public static class Execution {
        private long timeoutMs = 30000;
        private int maxConcurrent = 10;
    }
    
    @Data
    public static class Cache {
        private String provider = "postgresql";
        private boolean enabled = true;
    }
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ToolDiscoveryServiceTest {
    
    @Mock private ApplicationContext applicationContext;
    @InjectMocks private ToolDiscoveryService discoveryService;
    
    @Test
    void shouldDiscoverActionMethods() {
        // Given
        TestService testService = new TestService();
        when(applicationContext.getBeansOfType(Object.class))
            .thenReturn(Map.of("testService", testService));
        
        // When
        List<Tool> tools = discoveryService.discoverTools();
        
        // Then
        assertThat(tools).hasSize(2);
        assertThat(tools.get(0).getName()).isEqualTo("testAction");
    }
}
```

### 9.2 Integration Tests

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class A2aCoreIntegrationTest {
    
    @Autowired private A2aCoreController controller;
    
    @Test
    @Order(1)
    void shouldInitializeFast() {
        // Test startup time < 5 seconds
        long start = System.currentTimeMillis();
        controller.initialize();
        long duration = System.currentTimeMillis() - start;
        
        assertThat(duration).isLessThan(5000);
    }
    
    @Test
    @Order(2)
    void shouldListTools() {
        ResponseEntity<ListToolsResult> response = controller.listTools();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTools()).isNotEmpty();
    }
}
```

---

## 10. Migration Timeline

### Phase 1: Core Implementation (Days 1-3)
- [ ] Create a2acore module structure
- [ ] Implement domain classes (Tool, schemas, JSON-RPC)
- [ ] Build tool discovery service
- [ ] Create basic MCP controller

### Phase 2: Tool Execution (Days 4-5)
- [ ] Implement tool executor and parameter mapping
- [ ] Add error handling and validation
- [ ] Create cache provider interface
- [ ] Add PostgreSQL cache integration

### Phase 3: Integration (Days 6-7)
- [ ] Replace a2ajava dependency in a2awebagent
- [ ] Update import statements and configurations
- [ ] Remove component scanning exclusions
- [ ] Test with existing @Action tools

### Phase 4: Optimization (Days 8-9)
- [ ] Performance testing and optimization
- [ ] Documentation and examples
- [ ] Final validation and cleanup
- [ ] Delete a2ajava folder

---

## 11. Success Criteria

### 11.1 Functional Requirements
- ✅ Full MCP protocol compatibility
- ✅ Support for all existing @Action tools
- ✅ Zero breaking changes for existing tool implementations
- ✅ PostgreSQL cache integration working

### 11.2 Performance Requirements
- ✅ Startup time < 5 seconds (vs current 30+ seconds)
- ✅ Zero AI calls during startup
- ✅ Tool execution latency < 100ms
- ✅ Memory usage < 100MB

### 11.3 Architectural Requirements
- ✅ No component scanning exclusions needed
- ✅ Single unified MCP controller
- ✅ Clean dependency tree (< 10 dependencies)
- ✅ Spring Boot compatible but not dependent

### 11.4 Maintainability Requirements
- ✅ Clear separation of concerns
- ✅ Comprehensive test coverage (>90%)
- ✅ Documentation and examples
- ✅ Future extensibility

---

This design specification provides the complete blueprint for implementing a2acore as a high-performance, lightweight replacement for the a2ajava dependency that will eliminate startup AI calls while maintaining full MCP protocol compatibility.