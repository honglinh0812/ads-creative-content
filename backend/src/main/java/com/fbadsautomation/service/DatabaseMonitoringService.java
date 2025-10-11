package com.fbadsautomation.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMonitoringService {

    private final DataSource dataSource;

    public Map<String, Object> getHealthStatus() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                Map<String, Object> details = new HashMap<>();
                details.put("poolName", hikariDataSource.getPoolName());
                details.put("activeConnections", poolBean.getActiveConnections());
                details.put("idleConnections", poolBean.getIdleConnections());
                details.put("totalConnections", poolBean.getTotalConnections());
                details.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
                details.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                details.put("minimumIdle", hikariDataSource.getMinimumIdle());

                // Calculate utilization percentage
                double utilization = (double) poolBean.getActiveConnections() / hikariDataSource.getMaximumPoolSize() * 100;
                details.put("utilizationPercentage", Math.round(utilization * 100.0) / 100.0);

                // Health status based on utilization
                String status;
                String message;
                if (utilization > 90) {
                    status = "DOWN";
                    message = "Connection pool utilization is critically high";
                } else if (utilization > 80) {
                    status = "WARNING";
                    message = "Connection pool utilization is high";
                } else if (poolBean.getThreadsAwaitingConnection() > 0) {
                    status = "WARNING";
                    message = "Threads are waiting for database connections";
                } else {
                    status = "UP";
                    message = "Connection pool is healthy";
                }
                details.put("status", status);
                details.put("message", message);
                return details;
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "UNKNOWN");
                result.put("message", "DataSource is not HikariCP");
                return result;
            }
        } catch (Exception e) {
            log.error("Error checking database health", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "DOWN");
            result.put("message", "Failed to check database health: " + e.getMessage());
            return result;
        }
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void logConnectionPoolMetrics() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                int activeConnections = poolBean.getActiveConnections();
                int idleConnections = poolBean.getIdleConnections();
                int totalConnections = poolBean.getTotalConnections();
                int threadsAwaitingConnection = poolBean.getThreadsAwaitingConnection();
                int maximumPoolSize = hikariDataSource.getMaximumPoolSize();

                double utilization = (double) activeConnections / maximumPoolSize * 100;

                // Log metrics at different levels based on utilization
                if (utilization > 80 || threadsAwaitingConnection > 0) {
                    log.warn("🔥 HIGH DB Connection Pool Utilization - Pool: {}, Active: {}, Idle: {}, Total: {}, Waiting: {}, Max: {}, Utilization: {}%",
                            hikariDataSource.getPoolName(), activeConnections, idleConnections, totalConnections,
                            threadsAwaitingConnection, maximumPoolSize, Math.round(utilization * 100.0) / 100.0);
                } else if (utilization > 60) {
                    log.info("⚠️ MEDIUM DB Connection Pool Utilization - Pool: {}, Active: {}, Idle: {}, Total: {}, Max: {}, Utilization: {}%",
                            hikariDataSource.getPoolName(), activeConnections, idleConnections, totalConnections,
                            maximumPoolSize, Math.round(utilization * 100.0) / 100.0);
                } else {
                    log.debug("✅ DB Connection Pool Status - Pool: {}, Active: {}, Idle: {}, Total: {}, Max: {}, Utilization: {}%",
                            hikariDataSource.getPoolName(), activeConnections, idleConnections, totalConnections,
                            maximumPoolSize, Math.round(utilization * 100.0) / 100.0);
                }
            }
        } catch (Exception e) {
            log.error("Error logging connection pool metrics", e);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logDetailedDatabaseStats() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                log.info("📊 Database Connection Pool Detailed Stats:");
                log.info("   🏊 Pool Name: {}", hikariDataSource.getPoolName());
                log.info("   🔗 Active Connections: {}", poolBean.getActiveConnections());
                log.info("   💤 Idle Connections: {}", poolBean.getIdleConnections());
                log.info("   📊 Total Connections: {}", poolBean.getTotalConnections());
                log.info("   ⏳ Threads Awaiting Connection: {}", poolBean.getThreadsAwaitingConnection());
                log.info("   🔝 Maximum Pool Size: {}", hikariDataSource.getMaximumPoolSize());
                log.info("   🔄 Minimum Idle: {}", hikariDataSource.getMinimumIdle());
                log.info("   ⏱️ Connection Timeout: {}ms", hikariDataSource.getConnectionTimeout());
                log.info("   💨 Idle Timeout: {}ms", hikariDataSource.getIdleTimeout());
                log.info("   ⏰ Max Lifetime: {}ms", hikariDataSource.getMaxLifetime());
                log.info("   🔍 Leak Detection Threshold: {}ms", hikariDataSource.getLeakDetectionThreshold());

                double utilization = (double) poolBean.getActiveConnections() / hikariDataSource.getMaximumPoolSize() * 100;
                log.info("   📈 Pool Utilization: {}%", Math.round(utilization * 100.0) / 100.0);

                // Health recommendations
                if (utilization > 90) {
                    log.warn("🚨 CRITICAL: Consider increasing maximum pool size or investigating connection leaks");
                } else if (utilization > 80) {
                    log.warn("⚠️ WARNING: High pool utilization - monitor for connection issues");
                } else if (poolBean.getThreadsAwaitingConnection() > 0) {
                    log.warn("⏳ WARNING: {} threads waiting for connections - possible bottleneck", poolBean.getThreadsAwaitingConnection());
                } else if (utilization < 10 && poolBean.getTotalConnections() > hikariDataSource.getMinimumIdle()) {
                    log.info("💡 INFO: Low utilization - consider reducing minimum idle connections");
                }
            }
        } catch (Exception e) {
            log.error("Error logging detailed database stats", e);
        }
    }

    public Map<String, Object> getConnectionPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                metrics.put("poolName", hikariDataSource.getPoolName());
                metrics.put("activeConnections", poolBean.getActiveConnections());
                metrics.put("idleConnections", poolBean.getIdleConnections());
                metrics.put("totalConnections", poolBean.getTotalConnections());
                metrics.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
                metrics.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                metrics.put("minimumIdle", hikariDataSource.getMinimumIdle());
                metrics.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                metrics.put("idleTimeout", hikariDataSource.getIdleTimeout());
                metrics.put("maxLifetime", hikariDataSource.getMaxLifetime());
                metrics.put("leakDetectionThreshold", hikariDataSource.getLeakDetectionThreshold());

                double utilization = (double) poolBean.getActiveConnections() / hikariDataSource.getMaximumPoolSize() * 100;
                metrics.put("utilizationPercentage", Math.round(utilization * 100.0) / 100.0);

                String healthStatus;
                if (utilization > 90 || poolBean.getThreadsAwaitingConnection() > 0) {
                    healthStatus = "CRITICAL";
                } else if (utilization > 80) {
                    healthStatus = "WARNING";
                } else if (utilization > 60) {
                    healthStatus = "MEDIUM";
                } else {
                    healthStatus = "HEALTHY";
                }
                metrics.put("healthStatus", healthStatus);
            } else {
                metrics.put("error", "DataSource is not HikariCP");
            }
        } catch (Exception e) {
            metrics.put("error", "Failed to get metrics: " + e.getMessage());
        }

        return metrics;
    }
}