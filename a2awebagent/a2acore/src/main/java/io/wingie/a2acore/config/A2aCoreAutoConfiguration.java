package io.wingie.a2acore.config;

import io.wingie.a2acore.discovery.StaticToolRegistry;
import io.wingie.a2acore.discovery.ToolDiscoveryService;
import io.wingie.a2acore.discovery.MethodToolBuilder;
import io.wingie.a2acore.discovery.SchemaGenerator;
import io.wingie.a2acore.execution.ToolExecutor;
import io.wingie.a2acore.execution.ParameterMapper;
import io.wingie.a2acore.execution.ResultSerializer;
import io.wingie.a2acore.integration.ToolExecutionAdapter;
import io.wingie.a2acore.server.A2aCoreController;
import io.wingie.a2acore.server.JsonRpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Auto-configuration for A2ACore framework.
 * 
 * Automatically configures the A2ACore framework when present on the classpath
 * and not disabled via configuration.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "a2acore", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(A2aCoreProperties.class)
@ComponentScan(basePackages = "io.wingie.a2acore")
public class A2aCoreAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(A2aCoreAutoConfiguration.class);
    
    private final A2aCoreProperties properties;
    
    public A2aCoreAutoConfiguration(A2aCoreProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void logConfiguration() {
        if (properties.getLogging().isLogToolDiscovery()) {
            log.info("A2ACore framework initialized");
            log.info("Discovery packages: {}", properties.getDiscovery().getScanPackages());
            log.info("Default execution timeout: {}ms", properties.getExecution().getDefaultTimeoutMs());
            log.info("Cache provider: {}", properties.getCache().getProvider());
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StaticToolRegistry staticToolRegistry() {
        return new StaticToolRegistry();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SchemaGenerator schemaGenerator() {
        return new SchemaGenerator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MethodToolBuilder methodToolBuilder(SchemaGenerator schemaGenerator) {
        return new MethodToolBuilder(schemaGenerator);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ParameterMapper parameterMapper() {
        return new ParameterMapper();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ResultSerializer resultSerializer() {
        return new ResultSerializer();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ToolDiscoveryService toolDiscoveryService(ApplicationContext applicationContext,
                                                     MethodToolBuilder methodToolBuilder,
                                                     A2aCoreProperties properties) {
        return new ToolDiscoveryService(applicationContext, methodToolBuilder, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ToolExecutor toolExecutor(StaticToolRegistry toolRegistry,
                                   ParameterMapper parameterMapper,
                                   ResultSerializer resultSerializer,
                                   A2aCoreProperties properties) {
        return new ToolExecutor(toolRegistry, parameterMapper, resultSerializer, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcHandler jsonRpcHandler(ToolExecutor toolExecutor, ToolExecutionAdapter toolExecutionAdapter) {
        return new JsonRpcHandler(toolExecutor, toolExecutionAdapter);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public A2aCoreController a2aCoreController(ToolDiscoveryService discoveryService,
                                               StaticToolRegistry toolRegistry,
                                               JsonRpcHandler jsonRpcHandler,
                                               A2aCoreProperties properties) {
        return new A2aCoreController(discoveryService, toolRegistry, jsonRpcHandler, properties);
    }
}