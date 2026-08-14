package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: proteger a independência das rotinas operacionais de Dédalo. */
class LandingGeneratorSchedulingConfigTest {

  /** Garante threads separadas para produção, reconexão Codex e telemetria. */
  @Test
  void shouldKeepLandingProductionIndependentFromCodexReconnect() {
    ThreadPoolTaskScheduler scheduler = new LandingGeneratorSchedulingConfig().taskScheduler(1);
    scheduler.initialize();

    try {
      assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(3);
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("landing-generator-scheduled-");
    } finally {
      scheduler.shutdown();
    }
  }
}
