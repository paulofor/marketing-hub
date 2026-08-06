package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.financialagent.StudioProviderEfficiencyProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: proteger a interpretacao da cobertura financeira das tentativas do Estudio. */
class StudioCostLedgerServiceTest {
  /** Confirma que um ledger vazio nao e apresentado como custo integralmente conhecido. */
  @Test
  void deveDistinguirAusenciaDeTentativasDeCoberturaCompleta() {
    StudioCostLedgerEntryRepository repository = mock(StudioCostLedgerEntryRepository.class);
    when(repository.findByCommercialPlanIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());

    var coverage = new StudioCostLedgerService(repository).coverage(2L);

    assertThat(coverage.get("status")).isEqualTo("NO_ATTEMPTS_RECORDED");
    assertThat(coverage.get("totalAttempts")).isEqualTo(0);
  }

  /** Confirma que tentativas sem preco conhecido reduzem explicitamente a cobertura. */
  @Test
  void deveExporTentativasComCustoDesconhecido() {
    StudioCostLedgerEntry image = new StudioCostLedgerEntry();
    image.setAssetType("IMAGE");
    StudioCostLedgerEntry video = new StudioCostLedgerEntry();
    video.setAssetType("VIDEO");
    video.setEstimatedCostUsd(new BigDecimal("1.20"));
    StudioCostLedgerEntryRepository repository = mock(StudioCostLedgerEntryRepository.class);
    when(repository.findByCommercialPlanIdOrderByCreatedAtAsc(2L))
        .thenReturn(List.of(image, video));

    var coverage = new StudioCostLedgerService(repository).coverage(2L);

    assertThat(coverage.get("status")).isEqualTo("PARTIAL");
    assertThat(coverage.get("knownCostAttempts")).isEqualTo(1L);
    assertThat(coverage.get("unknownCostAttempts")).isEqualTo(1L);
    assertThat(coverage.get("imageAttempts")).isEqualTo(1L);
    assertThat(coverage.get("videoAttempts")).isEqualTo(1L);
    assertThat(coverage.get("audioAttempts")).isEqualTo(0L);
  }

  /** Confirma que áudio entra no ledger antes de existir custo ou resposta do provedor. */
  @Test
  void deveRegistrarTentativaDeAudioSemInventarCustoZero() {
    StudioCostLedgerEntryRepository repository = mock(StudioCostLedgerEntryRepository.class);
    when(repository.findBySourceTypeAndSourceId("MEDIA_ASSET", "91"))
        .thenReturn(java.util.Optional.empty());
    var service = new StudioCostLedgerService(repository);

    service.recordMedia(
        91L, 4L, 2L, 81L, "AUDIO", "ELEVENLABS", null, "PENDING", null, false, null, null);

    ArgumentCaptor<StudioCostLedgerEntry> captor =
        ArgumentCaptor.forClass(StudioCostLedgerEntry.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getAssetType()).isEqualTo("AUDIO");
    assertThat(captor.getValue().getProviderCostUsd()).isNull();
    assertThat(captor.getValue().getEstimatedCostUsd()).isNull();
    assertThat(captor.getValue().getCostEvidence()).isEqualTo("PROVIDER_COST_NOT_REPORTED");
  }

  /** Confirma que custos sem plano permanecem visíveis sem contaminar outro planejamento. */
  @Test
  void deveExporCustosSemAtribuicaoComercial() {
    StudioCostLedgerEntry video = new StudioCostLedgerEntry();
    video.setAssetType("VIDEO");
    video.setProviderCostUsd(new BigDecimal("1.20"));
    StudioCostLedgerEntry unknown = new StudioCostLedgerEntry();
    unknown.setAssetType("VIDEO");
    StudioCostLedgerEntryRepository repository = mock(StudioCostLedgerEntryRepository.class);
    when(repository.totalUnassignedCostUsd()).thenReturn(new BigDecimal("1.20"));
    when(repository.findByCommercialPlanIdIsNullOrderByCreatedAtAsc())
        .thenReturn(List.of(video, unknown));

    var service = new StudioCostLedgerService(repository);

    assertThat(service.totalUnassignedCostUsd()).isEqualByComparingTo("1.20");
    assertThat(service.unassignedCoverage().get("status")).isEqualTo("PARTIAL");
    assertThat(service.unassignedCoverage().get("totalAttempts")).isEqualTo(2);
  }

  /** Confirma que custo-beneficio usa somente aprovacoes comerciais e explicita lacunas. */
  @Test
  void deveCalcularEficienciaPorProvedorSemInventarCobertura() {
    StudioProviderEfficiencyProjection projection = mock(StudioProviderEfficiencyProjection.class);
    when(projection.getProvider()).thenReturn("RUNWAY");
    when(projection.getTotalAttempts()).thenReturn(10L);
    when(projection.getKnownCostAttempts()).thenReturn(9L);
    when(projection.getKnownCostUsd()).thenReturn(new BigDecimal("9.60"));
    when(projection.getReviewedAssets()).thenReturn(8L);
    when(projection.getApprovedAssets()).thenReturn(6L);
    when(projection.getPendingReviewAssets()).thenReturn(1L);
    StudioCostLedgerEntryRepository repository = mock(StudioCostLedgerEntryRepository.class);
    when(repository.providerEfficiencyByPlanId(2L)).thenReturn(List.of(projection));

    Map<String, Object> efficiency =
        new StudioCostLedgerService(repository).providerEfficiency(2L).getFirst();

    assertThat(efficiency.get("commercialApprovalRatePercent")).isEqualTo(new BigDecimal("75.00"));
    assertThat(efficiency.get("knownCostPerApprovedAssetUsd"))
        .isEqualTo(new BigDecimal("1.600000"));
    assertThat(efficiency.get("decisionCoverage")).isEqualTo("INCOMPLETE_COSTS");
  }
}
