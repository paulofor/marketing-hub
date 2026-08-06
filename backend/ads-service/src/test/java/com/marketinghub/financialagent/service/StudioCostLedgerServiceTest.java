package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
