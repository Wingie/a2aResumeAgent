package io.wingie.a2acore.tools4ai.predict;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class ConfigManager {
    private Properties properties;

    public ConfigManager() {
        loadProperties("prompt.properties");
    }

    private void loadProperties(String fileName) {
        properties = new Properties();
        try (InputStream inputStream = PredictionLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                log.warn(fileName + " properties not found will use default values");
            }
        } catch (IOException e) {
            log.warn("Failed to load properties: " + e.getMessage());
        }
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}

