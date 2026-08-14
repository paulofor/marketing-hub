package com.marketinghub.worker.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: configurar a execução concorrente e controlada das rotinas agendadas do AI Worker. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "worker.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

    /** Cria um pool dedicado para impedir que uma integração lenta paralise todas as filas independentes. */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(
            @Value("${worker.scheduling.pool-size:16}") int poolSize
    ) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(2, poolSize));
        scheduler.setThreadNamePrefix("ai-worker-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
