package com.marketinghub.worker.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: proteger a capacidade concorrente das filas agendadas do AI Worker. */
class SchedulingConfigTest {

    /** Garante que uma integração bloqueante não monopolize o agendador de todas as etapas. */
    @Test
    void shouldConfigureConcurrentScheduler() {
        ThreadPoolTaskScheduler scheduler = new SchedulingConfig().taskScheduler(16);
        scheduler.initialize();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(16);
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("ai-worker-scheduled-");
        } finally {
            scheduler.shutdown();
        }
    }

    /** Garante capacidade mínima mesmo quando a configuração operacional recebe valor inválido. */
    @Test
    void shouldKeepAtLeastTwoSchedulerThreads() {
        ThreadPoolTaskScheduler scheduler = new SchedulingConfig().taskScheduler(0);
        scheduler.initialize();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(2);
        } finally {
            scheduler.shutdown();
        }
    }
}
