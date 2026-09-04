package com.marketinghub.videomanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: isolar as rotinas agendadas de produção, BPM, autenticação e saúde de Apolo. */
@Configuration
public class VideoManagementSchedulingConfig {

    /** Cria concorrência suficiente para produção, BPM, autenticação e saúde avançarem isoladamente. */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(
            @Value("${video.scheduling.pool-size:4}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(4, poolSize));
        scheduler.setThreadNamePrefix("video-management-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
