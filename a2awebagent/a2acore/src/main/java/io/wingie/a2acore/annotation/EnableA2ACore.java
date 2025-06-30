package io.wingie.a2acore.annotation;

import io.wingie.a2acore.config.A2aCoreAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable A2ACore framework for AI tool integration and MCP protocol support.
 * 
 * This annotation automatically configures:
 * - Tool discovery and registration
 * - MCP protocol endpoints (/v1/tools, /v1/tools/call)
 * - JSON-RPC 2.0 request handling
 * - Performance optimizations (<5 second startup)
 * - Static tool descriptions (no AI calls during startup)
 * 
 * Usage:
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableA2ACore
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }
 * </pre>
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(A2aCoreAutoConfiguration.class)
public @interface EnableA2ACore {
    
    /**
     * Base packages to scan for @Action annotated methods.
     * If empty, will scan the package of the annotated class.
     */
    String[] scanPackages() default {};
    
    /**
     * Whether to enable performance logging.
     */
    boolean enablePerformanceLogging() default true;
    
    /**
     * Whether to enable tool discovery caching.
     */
    boolean enableCaching() default true;
}