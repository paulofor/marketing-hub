package com.marketinghub.videomanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: proteger a independência das rotinas agendadas do executor de vídeo. */
class VideoManagementSchedulingConfigTest {

    /** Garante threads próprias para produção, BPM, reconexão Codex e health-check de Apolo. */
    @Test
    void shouldKeepProductionReconnectAndHealthCheckIndependent() {
        ThreadPoolTaskScheduler scheduler = new VideoManagementSchedulingConfig().taskScheduler(1);
        scheduler.initialize();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("video-management-scheduled-");
        } finally {
            scheduler.shutdown();
        }
    }
}
