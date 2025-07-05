package io.wingie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.nio.file.Paths;

/**
 * Web configuration for static resource serving.
 * Enables serving screenshots and other static files via HTTP URLs.
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.screenshots:./screenshots}")
    private String screenshotsPath;
    
    private String absoluteScreenshotsPath;

    @PostConstruct
    public void init() {
        // Convert relative path to absolute path for reliable static resource serving
        absoluteScreenshotsPath = Paths.get(screenshotsPath).toAbsolutePath().toString();
        log.info("Screenshots will be served from absolute path: {}", absoluteScreenshotsPath);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve screenshots from the absolute path (more reliable than relative paths)
        registry.addResourceHandler("/screenshots/**")
                .addResourceLocations("file:" + absoluteScreenshotsPath + "/");
        
        log.debug("Configured static resource handler: /screenshots/** -> file:{}/", absoluteScreenshotsPath);
        
        // Serve static resources from classpath
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}