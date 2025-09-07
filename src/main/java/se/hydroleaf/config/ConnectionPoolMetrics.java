package se.hydroleaf.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Periodically logs HikariCP pool metrics so that active/idle connection
 * imbalances can be detected early. A warning is emitted when the pool is
 * exhausted.
 */
@Slf4j
@Component
public class ConnectionPoolMetrics {

    private final HikariDataSource dataSource;
    private final boolean enabled;

    public ConnectionPoolMetrics(
            DataSource dataSource,
            @Value("${metrics.connection-pool.enabled:true}") boolean enabled) {
        this.dataSource = dataSource instanceof HikariDataSource
                ? (HikariDataSource) dataSource
                : null;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${metrics.connection-pool.log-interval:60000}")
    public void logMetrics() {
        if (!enabled || dataSource == null) {
            return; // Logging disabled or DataSource not HikariCP; nothing to log
        }
        HikariPoolMXBean mxBean = dataSource.getHikariPoolMXBean();
        int active = mxBean.getActiveConnections();
        int idle = mxBean.getIdleConnections();
        int total = mxBean.getTotalConnections();
        int awaiting = mxBean.getThreadsAwaitingConnection();
        log.info("HikariCP - active:{} idle:{} total:{} awaiting:{}", active, idle, total, awaiting);
        if (active >= dataSource.getMaximumPoolSize()) {
            log.warn("HikariCP pool exhausted: {} active connections", active);
        }
    }
}
