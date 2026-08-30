package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: comprovar a correlação persistida entre ciclo e investigação Meta. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryMetaAdSessionLinkServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;
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
            cycleRepository, investigationService, new ObjectMapper());

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
            cycleRepository, investigationService, new ObjectMapper());

    assertThat(service.linkedInvestigation(45L)).isEmpty();
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
