package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: impedir duplicação paga por retry genérico sem retirar acabamento válido. */
class SalesVideoReprocessPolicyTest {
  private final SalesVideoReprocessPolicy policy = new SalesVideoReprocessPolicy(2, 2, 3, 1);

  /**
   * Rejeita replay pago do contrato antigo em duas identidades e também quando o custo é conhecido.
   */
  @ParameterizedTest
  @CsvSource({"441,RUNWAY,PROVIDER_RESULT_UNCERTAIN", "922,RUNWAY_VEO_3_1,PROVIDER_RENDER_FAILED"})
  void cannotClonePaidRenderContract(Long id, String provider, String failure) {
    var job =
        SalesVideoJob.builder()
            .id(id)
            .providerName(provider)
            .jobType(SalesVideoJobType.RENDER)
            .retryAttempt(0)
            .failureCode(failure)
            .build();
    assertThat(policy.hasAttemptsRemaining(job)).isFalse();
    assertThatThrownBy(
            () -> policy.ensureRetryAllowed(job, SalesVideoRetryReason.QUALITY_ASSURANCE))
        .hasMessageContaining("preflight", "Conciliar", "contrato financeiro anterior");
  }

  /** Preserva pós-produção sem inferência e reprocessamentos anteriores fora de geração Runway. */
  @Test
  void keepsExistingAssetFinishingAndOtherValidPaths() {
    for (var type :
        new SalesVideoJobType[] {SalesVideoJobType.POST_PRODUCTION, SalesVideoJobType.SCRIPT}) {
      var job =
          SalesVideoJob.builder()
              .id(923L)
              .providerName("RUNWAY")
              .jobType(type)
              .retryAttempt(0)
              .build();
      assertThatCode(() -> policy.ensureRetryAllowed(job, SalesVideoRetryReason.QUALITY_ASSURANCE))
          .doesNotThrowAnyException();
      assertThat(policy.hasAttemptsRemaining(job)).isTrue();
    }
    var legacy =
        SalesVideoJob.builder()
            .providerName("LUMA_RAY_3_2")
            .jobType(SalesVideoJobType.RENDER)
            .retryAttempt(0)
            .build();
    assertThat(policy.hasAttemptsRemaining(legacy)).isTrue();
    legacy.setRetryAttempt(3);
    assertThatThrownBy(
            () -> policy.ensureRetryAllowed(legacy, SalesVideoRetryReason.QUALITY_ASSURANCE))
        .hasMessageContaining("Limite");
  }
}
