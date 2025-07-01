package io.wingie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for static resource serving.
 * Enables serving screenshots and other static files via HTTP URLs.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.screenshots:./screenshots}")
    private String screenshotsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve screenshots from the configured directory
        registry.addResourceHandler("/screenshots/**")
                .addResourceLocations("file:" + screenshotsPath + "/");
        
        // Serve static resources from classpath
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}