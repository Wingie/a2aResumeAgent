package io.wingie.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Connection Pool Monitor for HikariCP
 * 
 * Provides monitoring and health checks for database connection pool usage.
 * Helps prevent connection pool exhaustion by providing visibility into pool metrics.
 */
@Slf4j
@Component
public class ConnectionPoolMonitor implements HealthIndicator {

    private final HikariDataSource hikariDataSource;

    @Autowired
    public ConnectionPoolMonitor(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            this.hikariDataSource = (HikariDataSource) dataSource;
        } else {
            log.warn("DataSource is not HikariDataSource, connection pool monitoring disabled");
            this.hikariDataSource = null;
        }
    }

    /**
     * Scheduled task to log connection pool statistics
     * Runs every 60 seconds to provide visibility into pool usage
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void logPoolStats() {
        if (hikariDataSource == null) {
            return;
        }

        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            
            int activeConnections = pool.getActiveConnections();
            int idleConnections = pool.getIdleConnections();
            int totalConnections = pool.getTotalConnections();
            int threadsAwaitingConnection = pool.getThreadsAwaitingConnection();
            
            // Calculate pool utilization percentage
            int maxPoolSize = hikariDataSource.getMaximumPoolSize();
            double utilizationPercent = (double) totalConnections / maxPoolSize * 100;
            
            // Log connection pool statistics
            log.info("📊 HikariCP Pool Stats - Active: {}, Idle: {}, Total: {}/{} ({:.1f}%), Waiting: {}", 
                activeConnections, idleConnections, totalConnections, maxPoolSize, 
                utilizationPercent, threadsAwaitingConnection);
            
            // Log warnings for potential issues
            if (utilizationPercent > 80) {
                log.warn("⚠️ High pool utilization: {:.1f}% - Consider increasing pool size or reducing connection usage", 
                    utilizationPercent);
            }
            
            if (threadsAwaitingConnection > 0) {
                log.warn("⚠️ {} threads waiting for connections - potential connection pool exhaustion", 
                    threadsAwaitingConnection);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve connection pool statistics: {}", e.getMessage());
        }
    }

    /**
     * Health check implementation for connection pool
     * Used by Spring Boot Actuator health endpoint
     */
    @Override
    public Health health() {
        if (hikariDataSource == null) {
            return Health.down()
                .withDetail("error", "HikariDataSource not available")
                .build();
        }

        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            
            int activeConnections = pool.getActiveConnections();
            int totalConnections = pool.getTotalConnections();
            int threadsAwaitingConnection = pool.getThreadsAwaitingConnection();
            int maxPoolSize = hikariDataSource.getMaximumPoolSize();
            
            double utilizationPercent = (double) totalConnections / maxPoolSize * 100;
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("activeConnections", activeConnections)
                .withDetail("totalConnections", totalConnections)
                .withDetail("maxPoolSize", maxPoolSize)
                .withDetail("utilizationPercent", String.format("%.1f%%", utilizationPercent))
                .withDetail("threadsAwaitingConnection", threadsAwaitingConnection)
                .withDetail("poolName", hikariDataSource.getPoolName());
            
            // Determine health status based on pool state
            if (threadsAwaitingConnection > 0) {
                return healthBuilder
                    .down()
                    .withDetail("warning", "Threads waiting for connections")
                    .build();
            } else if (utilizationPercent > 90) {
                return healthBuilder
                    .down()
                    .withDetail("warning", "High pool utilization")
                    .build();
            } else if (utilizationPercent > 80) {
                return healthBuilder
                    .up()
                    .withDetail("warning", "Pool utilization approaching maximum")
                    .build();
            } else {
                return healthBuilder.build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Failed to check connection pool health")
                .withDetail("exception", e.getMessage())
                .build();
        }
    }

    /**
     * Get current pool utilization percentage
     * Useful for programmatic monitoring
     */
    public double getPoolUtilizationPercent() {
        if (hikariDataSource == null) {
            return 0.0;
        }

        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            int totalConnections = pool.getTotalConnections();
            int maxPoolSize = hikariDataSource.getMaximumPoolSize();
            return (double) totalConnections / maxPoolSize * 100;
        } catch (Exception e) {
            log.error("Failed to calculate pool utilization: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Check if there are threads waiting for connections
     */
    public boolean hasWaitingThreads() {
        if (hikariDataSource == null) {
            return false;
        }

        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            return pool.getThreadsAwaitingConnection() > 0;
        } catch (Exception e) {
            log.error("Failed to check waiting threads: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get detailed pool statistics as a formatted string
     * Useful for logging or debugging
     */
    public String getDetailedStats() {
        if (hikariDataSource == null) {
            return "HikariDataSource not available";
        }

        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            
            return String.format(
                "HikariCP Pool: Active=%d, Idle=%d, Total=%d, Max=%d, Waiting=%d, Pool=%s",
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getTotalConnections(),
                hikariDataSource.getMaximumPoolSize(),
                pool.getThreadsAwaitingConnection(),
                hikariDataSource.getPoolName()
            );
        } catch (Exception e) {
            return "Failed to retrieve pool statistics: " + e.getMessage();
        }
    }
}