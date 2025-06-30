package io.wingie;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test-specific Data Configuration - simplified to avoid repository conflicts.
 * 
 * This configuration avoids duplicating repository configurations that are
 * already defined in the main DataConfig.java. In test profile, the main
 * DataConfig is sufficient.
 */
@TestConfiguration
public class TestDataConfig {
    
    /**
     * Empty test configuration - relies on main DataConfig.java for repository setup.
     * This prevents bean definition conflicts during testing.
     */
}