package com.marketinghub.leadportal.config;

import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Responsabilidade: configurar executores assíncronos usados pelo Lead Portal. */
@Configuration
public class LeadPortalAsyncConfig {

    /** Cria o executor dedicado ao upload de imagens sem bloquear a resposta do formulário. */
    @Bean(name = "leadPortalImageUploadExecutor")
    @ConditionalOnMissingBean(name = "leadPortalImageUploadExecutor")
    public Executor leadPortalImageUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("lead-portal-image-upload-");
        executor.initialize();
        return executor;
    }
}
