package io.wingie.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Data configuration to explicitly separate different Spring Data repositories
 * and prevent repository assignment conflicts between JPA, Neo4j, and Redis.
 */
@Configuration
@EntityScan(basePackages = "io.wingie.entity")
@EnableJpaRepositories(
    basePackages = "io.wingie.repository"
)
@EnableNeo4jRepositories(
    basePackages = "io.wingie.repository.neo4j",
    repositoryImplementationPostfix = "Neo4jImpl"
)
public class DataConfig {
    // Configuration class - no additional beans needed
    // Repository scanning and configuration is handled by annotations
}

@Configuration
@Profile("!test")
@EnableRedisRepositories(
    basePackages = "io.wingie.repository.redis",
    repositoryImplementationPostfix = "RedisImpl"
)
class RedisDataConfig {
    // Redis repository configuration - excluded from test profile
}