package io.wingie.config;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * Transaction configuration to resolve multiple transaction manager conflicts.
 * 
 * This application uses:
 * - JPA (PostgreSQL) for main data persistence  
 * - Redis for caching and pub/sub
 * - Neo4j for future knowledge graph features
 * 
 * The JPA transaction manager is marked as @Primary since this is primarily
 * a JPA-based application with Redis/Neo4j used for specialized features.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Primary transaction manager for JPA operations.
     * This resolves the NoUniqueBeanDefinitionException by explicitly marking
     * the JPA transaction manager as the default choice.
     * 
     * Using the standard name 'transactionManager' ensures Spring Boot 
     * auto-configuration recognizes this as the default.
     */
    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
    
    /**
     * Alias for the primary transaction manager with explicit name
     */
    @Bean("primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            EntityManagerFactory entityManagerFactory) {
        return transactionManager(entityManagerFactory);
    }
    
    /**
     * Neo4j transaction manager for graph database operations.
     * This is available for future Neo4j operations but is not primary.
     * Methods that need this should explicitly specify: @Transactional("neo4jTransactionManager")
     */
    @Bean("neo4jTransactionManager")
    @Qualifier("neo4j")
    public PlatformTransactionManager neo4jTransactionManager(
            @Autowired(required = false) Driver neo4jDriver) {
        if (neo4jDriver != null) {
            return new Neo4jTransactionManager(neo4jDriver);
        }
        // Return null if Neo4j is not configured - Spring will handle gracefully
        return null;
    }
}