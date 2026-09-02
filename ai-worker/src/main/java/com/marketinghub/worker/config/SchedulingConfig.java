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

    /** Cria o pool compartilhado para executar concorrentemente as filas independentes do worker. */
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

    /** Cria o agendador exclusivo da fila comercial de conteúdo dos experimentos. */
    @Bean(name = "experimentPipelineTaskScheduler")
    public ThreadPoolTaskScheduler experimentPipelineTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("experiment-pipeline-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
