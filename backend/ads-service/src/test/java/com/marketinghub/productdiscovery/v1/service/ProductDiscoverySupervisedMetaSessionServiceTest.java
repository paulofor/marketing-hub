package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar gates, auditoria e idempotência da sessão Meta de Argos. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoverySupervisedMetaSessionServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;
  @Mock private ProductDiscoveryMetaAdSessionLinkService sessionLinkService;
  @Mock private ProductDiscoveryMetaAdEvidenceService evidenceService;
  @Mock private MoisMetaAdInvestigationService investigationService;
  @Mock private ProductDiscoveryBpmAuditService bpmAuditService;

  private ProductDiscoverySupervisedMetaSessionService service;
  private ProductDiscoveryCycle cycle;
  private MoisMetaAdDtos.InvestigationResponse investigation;

  /** Prepara um ciclo brasileiro B2C/Instagram e sua investigação supervisionada. */
  @BeforeEach
  void setUp() {
    service =
        new ProductDiscoverySupervisedMetaSessionService(
            cycleRepository,
            sessionLinkService,
            evidenceService,
            investigationService,
            bpmAuditService);
    cycle = new ProductDiscoveryCycle();
    cycle.setId(44L);
    cycle.setTheme("autocuidado feminino visual");
    cycle.setCountry("BR");
    cycle.setAcquisitionChannel("Instagram");
    cycle.setMarketType(ProductDiscoveryMarketType.B2C);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    cycle.setStageCode("opportunity-gate");
    Instant now = Instant.parse("2026-08-30T20:00:00Z");
    investigation =
        new MoisMetaAdDtos.InvestigationResponse(
            72L,
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
            1,
            now,
            now);
    when(sessionLinkService.linkedInvestigation(cycle)).thenReturn(Optional.of(investigation));
  }

  /** Deve persistir a observação bruta e devolver a linguagem que ficará disponível ao Argos. */
  @Test
  void recordsSupervisedObservationAndRefreshesEvidence() {
    when(cycleRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(cycle));
    when(investigationService.get(72L)).thenReturn(Optional.of(investigation));
    when(evidenceService.searchInvestigation(44L, investigation, 50))
        .thenReturn(observedEvidence());
    ProductDiscoverySupervisedMetaObservationRequest request =
        new ProductDiscoverySupervisedMetaObservationRequest(
            "ad-72",
            "Marca observada",
            "https://www.facebook.com/ads/library/?id=ad-72",
            "Seu ritual de cinco minutos começa agora.",
            List.of("INSTAGRAM"),
            "VIDEO",
            null,
            "https://example.test/oferta",
            true,
            true,
            Instant.parse("2026-08-30T20:00:00Z"));

    ProductDiscoverySupervisedMetaSessionResponse response = service.observe(44L, request);

    verify(investigationService)
        .ingestSupervised(eq(72L), any(MoisMetaAdDtos.SupervisedObservationRequest.class));
    assertThat(response.canResume()).isTrue();
    assertThat(response.items().getFirst().adTexts())
        .containsExactly("Seu ritual de cinco minutos começa agora.");
  }

  /** Deve abrir uma única tentativa mesmo quando o comando humano for repetido. */
  @Test
  void resumesOnceAndKeepsRepeatedCommandIdempotent() {
    when(cycleRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(cycle));
    when(evidenceService.searchInvestigation(44L, investigation, 50))
        .thenReturn(observedEvidence());
    when(cycleRepository.save(cycle)).thenReturn(cycle);

    ProductDiscoverySupervisedMetaSessionResponse first = service.resume(44L);
    ProductDiscoverySupervisedMetaSessionResponse repeated = service.resume(44L);

    verify(bpmAuditService, times(1)).reopenForSupervisedMetaEvidence(cycle, 72L);
    assertThat(first.cycleStatus()).isEqualTo("READY_FOR_RESEARCH");
    assertThat(repeated.resumeReason()).contains("já está na fila");
  }

  /** Deve bloquear nova tentativa quando a observação não comprovar anúncio ativo no Instagram. */
  @Test
  void rejectsResumeWithoutCurrentInstagramAd() {
    when(cycleRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(cycle));
    when(evidenceService.searchInvestigation(44L, investigation, 50))
        .thenReturn(
            new ProductDiscoveryMetaAdEvidenceListResponse(
                44L,
                investigation.searchTerms(),
                "BR",
                "INSTAGRAM",
                "NO_RELEVANT_PLATFORM_EVIDENCE",
                "SUPERVISED",
                72L,
                investigation.collection().searchUrl(),
                1,
                0,
                1,
                null,
                "O anúncio foi observado fora do Instagram.",
                List.of()));

    assertThatThrownBy(() -> service.resume(44L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("anúncio atual, ativo e distribuído no Instagram");
    verify(bpmAuditService, times(0)).reopenForSupervisedMetaEvidence(any(), eq(72L));
  }

  /** Deve congelar novas observações enquanto a tentativa reaberta estiver em execução. */
  @Test
  void rejectsObservationDuringActiveResearch() {
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    when(cycleRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(cycle));
    ProductDiscoverySupervisedMetaObservationRequest request =
        new ProductDiscoverySupervisedMetaObservationRequest(
            "ad-72",
            "Marca observada",
            "https://business.facebook.com/ads/library/?id=ad-72",
            "Seu ritual de cinco minutos começa agora.",
            List.of("INSTAGRAM"),
            "VIDEO",
            null,
            null,
            true,
            false,
            null);

    assertThatThrownBy(() -> service.observe(44L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Aguarde a tentativa atual");
    verify(investigationService, times(0))
        .ingestSupervised(eq(72L), any(MoisMetaAdDtos.SupervisedObservationRequest.class));
  }

  /** Deve impedir que um relatório corrompido exponha investigação de outro território. */
  @Test
  void rejectsInvestigationFromAnotherTerritory() {
    when(cycleRepository.findById(44L)).thenReturn(Optional.of(cycle));
    MoisMetaAdDtos.InvestigationResponse foreignInvestigation =
        new MoisMetaAdDtos.InvestigationResponse(
            investigation.id(),
            investigation.workspaceId(),
            investigation.searchTerms(),
            "US",
            investigation.publisherPlatform(),
            investigation.status(),
            investigation.collection(),
            investigation.gateDecision(),
            investigation.evidences(),
            investigation.gaps(),
            investigation.ethicalModeling(),
            investigation.creativeBrief(),
            investigation.adsObserved(),
            investigation.createdAt(),
            investigation.updatedAt());
    when(sessionLinkService.linkedInvestigation(cycle))
        .thenReturn(Optional.of(foreignInvestigation));

    assertThatThrownBy(() -> service.get(44L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence ao território");
    verify(evidenceService, times(0)).searchInvestigation(any(), any(), any());
  }

  /** Deve ocultar a sessão humana quando o território possui coleta pela API oficial. */
  @Test
  void rejectsInvestigationThatDoesNotRequireSupervision() {
    when(cycleRepository.findById(44L)).thenReturn(Optional.of(cycle));
    MoisMetaAdDtos.InvestigationResponse officialInvestigation =
        new MoisMetaAdDtos.InvestigationResponse(
            investigation.id(),
            investigation.workspaceId(),
            investigation.searchTerms(),
            investigation.countryCode(),
            investigation.publisherPlatform(),
            "PENDING",
            new MoisMetaAdDtos.CollectionState(
                "OFFICIAL_API",
                "Coleta oficial disponível.",
                investigation.collection().searchUrl(),
                investigation.collection().nextObservationAt()),
            investigation.gateDecision(),
            investigation.evidences(),
            investigation.gaps(),
            investigation.ethicalModeling(),
            investigation.creativeBrief(),
            investigation.adsObserved(),
            investigation.createdAt(),
            investigation.updatedAt());
    when(sessionLinkService.linkedInvestigation(cycle))
        .thenReturn(Optional.of(officialInvestigation));

    assertThatThrownBy(() -> service.get(44L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não exige observação humana");
    verify(evidenceService, times(0)).searchInvestigation(any(), any(), any());
  }

  /** Monta a cobertura ativa usada nos cenários de observação e retomada. */
  private ProductDiscoveryMetaAdEvidenceListResponse observedEvidence() {
    Instant now = Instant.parse("2026-08-30T20:00:00Z");
    return new ProductDiscoveryMetaAdEvidenceListResponse(
        44L,
        investigation.searchTerms(),
        "BR",
        "INSTAGRAM",
        "OBSERVED",
        "SUPERVISED",
        72L,
        investigation.collection().searchUrl(),
        1,
        1,
        1,
        now,
        "Um anúncio ativo observado; isso não comprova venda.",
        List.of(
            new ProductDiscoveryMetaAdEvidenceResponse(
                "ad-72",
                "Marca observada",
                List.of("Seu ritual de cinco minutos começa agora."),
                List.of("INSTAGRAM"),
                List.of("VIDEO"),
                "https://example.test/oferta",
                "https://www.facebook.com/ads/library/?id=ad-72",
                true,
                true,
                1,
                0,
                false,
                "LOW",
                now,
                now)));
  }
}
