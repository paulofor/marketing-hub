package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMetaAttempt;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryMetaAttemptRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: comprovar a correlação persistida entre ciclo e investigação Meta. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryMetaAdSessionLinkServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;
  @Mock private ProductDiscoveryMetaAttemptRepository metaAttemptRepository;
  @Mock private MoisMetaAdInvestigationService investigationService;

  /** Deve selecionar o último vínculo Instagram sem aceitar uma cobertura Facebook posterior. */
  @Test
  void findsLatestInstagramInvestigationFromEvidenceReport() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(44L);
    cycle.setResearchEvidenceReportJson(
        """
        {"metaCoverage":[
          {"publisherPlatform":"INSTAGRAM","investigationId":71},
          {"publisherPlatform":"FACEBOOK","investigationId":99},
          {"publisherPlatform":"INSTAGRAM","investigationId":72}
        ]}
        """);
    MoisMetaAdDtos.InvestigationResponse investigation = investigation(72L);
    when(cycleRepository.findById(44L)).thenReturn(Optional.of(cycle));
    when(investigationService.get(72L)).thenReturn(Optional.of(investigation));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    Optional<MoisMetaAdDtos.InvestigationResponse> result = service.linkedInvestigation(44L);

    assertThat(result).contains(investigation);
  }

  /** Deve preservar ausência real quando a tentativa ainda não gerou cobertura Meta. */
  @Test
  void keepsMissingSessionExplicit() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(45L);
    when(cycleRepository.findById(45L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    assertThat(service.linkedInvestigation(45L)).isEmpty();
  }

  /** Deve congelar a investigação somente quando o callback usa o lease ativo. */
  @Test
  void bindsInvestigationToActiveLease() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(46L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setExecutionLeaseId("lease-46");
    MoisMetaAdDtos.InvestigationResponse investigation = investigation(73L);
    when(cycleRepository.findByIdForUpdate(46L)).thenReturn(Optional.of(cycle));
    when(investigationService.get(73L)).thenReturn(Optional.of(investigation));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    assertThat(
            service.bindAttemptInvestigation(
                46L, 1, 73L, "lease-46", "autocuidado feminino visual"))
        .isEqualTo(investigation);
    assertThat(cycle.getMetaAdInvestigationId()).isEqualTo(73L);
    org.mockito.Mockito.verify(metaAttemptRepository)
        .save(org.mockito.ArgumentMatchers.any(ProductDiscoveryMetaAttempt.class));
    org.mockito.Mockito.verify(cycleRepository).save(cycle);
  }

  /** Deve recuperar investigações diferentes para tentativas diferentes do mesmo ciclo. */
  @Test
  void resolvesInvestigationByAttempt() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(47L);
    ProductDiscoveryMetaAttempt first =
        new ProductDiscoveryMetaAttempt(cycle, 1, 73L, "lease-47", "beleza bem estar");
    ProductDiscoveryMetaAttempt second =
        new ProductDiscoveryMetaAttempt(cycle, 2, 74L, "lease-47", "consultoria imagem");
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(47L, 1))
        .thenReturn(Optional.of(first));
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(47L, 2))
        .thenReturn(Optional.of(second));
    when(investigationService.get(73L)).thenReturn(Optional.of(investigation(73L)));
    when(investigationService.get(74L)).thenReturn(Optional.of(investigation(74L)));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    assertThat(service.linkedAttemptInvestigation(cycle, 1).map(item -> item.id())).contains(73L);
    assertThat(service.linkedAttemptInvestigation(cycle, 2).map(item -> item.id())).contains(74L);
  }

  /** Deve criar a segunda investigação somente depois de preservar a primeira. */
  @Test
  void bindsDistinctSecondAttemptAfterFirstAttempt() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(48L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setExecutionLeaseId("lease-48");
    ProductDiscoveryMetaAttempt first =
        new ProductDiscoveryMetaAttempt(cycle, 1, 73L, "lease-48", "beleza bem estar");
    when(cycleRepository.findByIdForUpdate(48L)).thenReturn(Optional.of(cycle));
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(48L, 2)).thenReturn(Optional.empty());
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(48L, 1))
        .thenReturn(Optional.of(first));
    when(investigationService.get(74L)).thenReturn(Optional.of(investigation(74L)));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    service.bindAttemptInvestigation(48L, 2, 74L, "lease-48", "consultoria imagem");

    ArgumentCaptor<ProductDiscoveryMetaAttempt> saved =
        ArgumentCaptor.forClass(ProductDiscoveryMetaAttempt.class);
    org.mockito.Mockito.verify(metaAttemptRepository).save(saved.capture());
    assertThat(saved.getValue().getAttemptNumber()).isEqualTo(2);
    assertThat(saved.getValue().getInvestigationId()).isEqualTo(74L);
    assertThat(saved.getValue().getSearchQuery()).isEqualTo("consultoria imagem");
    assertThat(cycle.getMetaAdInvestigationId()).isEqualTo(74L);
  }

  /** Deve impedir que uma nova tentativa repita a mesma consulta com outro identificador. */
  @Test
  void rejectsRepeatedQueryAcrossAttempts() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(49L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setExecutionLeaseId("lease-49");
    ProductDiscoveryMetaAttempt first =
        new ProductDiscoveryMetaAttempt(cycle, 1, 73L, "lease-49", "beleza bem estar");
    when(cycleRepository.findByIdForUpdate(49L)).thenReturn(Optional.of(cycle));
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(49L, 2)).thenReturn(Optional.empty());
    when(metaAttemptRepository.findByCycleIdAndAttemptNumber(49L, 1))
        .thenReturn(Optional.of(first));
    when(metaAttemptRepository.existsByCycleIdAndSearchQueryIgnoreCase(49L, "beleza bem estar"))
        .thenReturn(true);
    when(investigationService.get(74L)).thenReturn(Optional.of(investigation(74L)));
    ProductDiscoveryMetaAdSessionLinkService service =
        new ProductDiscoveryMetaAdSessionLinkService(
            cycleRepository, metaAttemptRepository, investigationService, new ObjectMapper());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.bindAttemptInvestigation(49L, 2, 74L, "lease-49", "  beleza   bem estar "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("repetiu a consulta Meta");
  }

  /** Monta a investigação canônica usada pelo vínculo do relatório. */
  private MoisMetaAdDtos.InvestigationResponse investigation(long id) {
    Instant now = Instant.parse("2026-08-30T20:00:00Z");
    return new MoisMetaAdDtos.InvestigationResponse(
        id,
        "workspace-001",
        "autocuidado feminino visual",
        "BR",
        "INSTAGRAM",
        "ACTIVE_SUPERVISED",
        new MoisMetaAdDtos.CollectionState(
            "SUPERVISED",
            "Observação humana na fonte oficial.",
            "https://www.facebook.com/ads/library/?q=autocuidado",
            now),
        "INVESTIGAR",
        List.of(),
        List.of(),
        MoisMetaAdDtos.EthicalModelingCard.empty(),
        MoisMetaAdDtos.CreativeIntelligenceBrief.unavailable(),
        0,
        now,
        now);
  }
}
