package com.marketinghub.facebookadsworker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Isola a renovação de credenciais preservando a execução serial das demais rotinas Meta. */
@Configuration(proxyBeanMethods = false)
public class FacebookSchedulingConfiguration {

    /** Mantém as operações de campanha na fila única que protege o contexto compartilhado. */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler operationalScheduler() {
        return scheduler("facebook-operations-");
    }

    /** Reserva uma fila independente para verificar e renovar credenciais antes do vencimento. */
    @Bean(name = "facebookTokenRenewalTaskScheduler")
    public ThreadPoolTaskScheduler renewalScheduler() {
        return scheduler("facebook-token-renewal-");
    }

    /** Cria um agendador serial com nome rastreável e remoção de tarefas canceladas. */
    private ThreadPoolTaskScheduler scheduler(String prefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(prefix);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
