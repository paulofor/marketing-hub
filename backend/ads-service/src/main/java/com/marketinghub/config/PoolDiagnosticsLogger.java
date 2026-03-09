package com.marketinghub.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically logs pool and runtime diagnostics to help investigate starvation warnings.
 */
@Component
public class PoolDiagnosticsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoolDiagnosticsLogger.class);

    private final HikariDataSource hikariDataSource;
    private final OperatingSystemMXBean operatingSystemMXBean;
    private final int warnActiveConnectionsThresholdPercent;

    public PoolDiagnosticsLogger(
        HikariDataSource hikariDataSource,
        @Value("${diagnostics.hikari.warn-active-threshold-percent:80}") int warnActiveConnectionsThresholdPercent
    ) {
        this.hikariDataSource = hikariDataSource;
        this.operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.warnActiveConnectionsThresholdPercent = warnActiveConnectionsThresholdPercent;
    }

    @Scheduled(fixedDelayString = "${diagnostics.hikari.log-interval-ms:60000}")
    public void logPoolDiagnostics() {
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            LOGGER.debug("Hikari pool MXBean ainda não está disponível para diagnóstico.");
            return;
        }

        int active = pool.getActiveConnections();
        int idle = pool.getIdleConnections();
        int total = pool.getTotalConnections();
        int awaiting = pool.getThreadsAwaitingConnection();
        int max = hikariDataSource.getMaximumPoolSize();
        int threshold = Math.max(1, max * warnActiveConnectionsThresholdPercent / 100);

        long usedMemoryMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        if (awaiting > 0 || active >= threshold) {
            LOGGER.warn(
                "Diagnóstico do pool detectou pressão. pool={}, active={}, idle={}, total={}, max={}, awaiting={}, cpuLoad={}, availableProcessors={}, usedMemoryMb={}, maxMemoryMb={}",
                hikariDataSource.getPoolName(),
                active,
                idle,
                total,
                max,
                awaiting,
                operatingSystemMXBean.getSystemLoadAverage(),
                operatingSystemMXBean.getAvailableProcessors(),
                usedMemoryMb,
                maxMemoryMb
            );
            return;
        }

        LOGGER.info(
            "Diagnóstico periódico do pool. pool={}, active={}, idle={}, total={}, max={}, awaiting={}, cpuLoad={}, usedMemoryMb={}, maxMemoryMb={}",
            hikariDataSource.getPoolName(),
            active,
            idle,
            total,
            max,
            awaiting,
            operatingSystemMXBean.getSystemLoadAverage(),
            usedMemoryMb,
            maxMemoryMb
        );
    }
}
