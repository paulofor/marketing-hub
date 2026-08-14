package com.marketinghub.videomanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: isolar as rotinas agendadas de produção, autenticação e saúde de Apolo. */
@Configuration
public class VideoManagementSchedulingConfig {

    /** Cria concorrência suficiente para uma rotina lenta não ocultar a prontidão nem paralisar as demais. */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(
            @Value("${video.scheduling.pool-size:3}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(3, poolSize));
        scheduler.setThreadNamePrefix("video-management-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
