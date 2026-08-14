package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: proteger a independência entre revisão de anúncios e reconexão Codex. */
class MetaAdApproverSchedulingConfigTest {

  /** Garante ao menos duas threads para impedir que o device code paralise Têmis. */
  @Test
  void shouldKeepReviewPollingIndependentFromCodexReconnect() {
    ThreadPoolTaskScheduler scheduler = new MetaAdApproverSchedulingConfig().taskScheduler(1);
    scheduler.initialize();

    try {
      assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(2);
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("meta-ad-approver-scheduled-");
    } finally {
      scheduler.shutdown();
    }
  }
}
