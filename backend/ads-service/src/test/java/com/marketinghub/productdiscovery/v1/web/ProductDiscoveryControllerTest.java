package com.marketinghub.productdiscovery.v1.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryCustomerInterviewService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryGapDeepeningResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMarketplaceEvidenceService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMaturityItemResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMaturityRankingResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMetaAdBrowserCollectionService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMetaAdEvidenceListResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMetaAdEvidenceService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryResearchTrackResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoverySupervisedMetaSessionResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoverySupervisedMetaSessionService;
import com.marketinghub.productdiscovery.v1.service.resumePrivateValidationHandoff.ProductDiscoveryPrivateValidationHandoffResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: valida o contrato REST da descoberta de produtos PDE. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryControllerTest {

  private MockMvc mockMvc;

  @Mock private ProductDiscoveryService service;
  @Mock private ProductDiscoveryMarketplaceEvidenceService marketplaceEvidenceService;
  @Mock private ProductDiscoveryMetaAdEvidenceService metaAdEvidenceService;
  @Mock private ProductDiscoveryMetaAdBrowserCollectionService metaAdBrowserCollectionService;
  @Mock private ProductDiscoverySupervisedMetaSessionService supervisedMetaSessionService;
  @Mock private ProductDiscoveryCustomerInterviewService customerInterviewService;

  /** Monta o controller isolado para testar as rotas do módulo. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProductDiscoveryController(
                    service,
                    marketplaceEvidenceService,
                    metaAdEvidenceService,
                    metaAdBrowserCollectionService,
                    supervisedMetaSessionService,
                    customerInterviewService))
            .build();
  }

  /** Repassa a capacidade declarada no pending antes de o serviço reservar trabalho. */
  @Test
  void pendingRequiresExplicitPublicPolicyCapability() throws Exception {
    when(service.pending("PUBLIC_SOURCES_V1")).thenReturn(List.of());
    when(service.pendingGapDeepening(null)).thenReturn(List.of());
    mockMvc.perform(get("/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending")
            .param("supportedEvidencePolicy", "PUBLIC_SOURCES_V1"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/internal/product-discovery/productdiscovery/v1/candidate-gap-deepening/stage-executions/pending"))
        .andExpect(status().isOk());
    org.mockito.Mockito.verify(service).pending("PUBLIC_SOURCES_V1");
    org.mockito.Mockito.verify(service).pendingGapDeepening(null);
  }

  /** Adota a política pelo endpoint do próprio módulo e devolve o estado sem simular entrevistas. */
  @Test
  void adoptsPublicResearchThroughCanonicalEndpoint() throws Exception {
    when(customerInterviewService.adoptPublicEvidence(901L)).thenReturn(
        new ProductDiscoveryGapDeepeningResponse(901L, true,
            ProductDiscoveryCycleStatus.READY_FOR_RESEARCH, "candidate-gap-deepening",
            0, 8, 0, 0, 0, List.of(), List.of(), true, 12, 2, 4,
            new BigDecimal("0.12"), "ESTIMATED_SEARCH_ONLY", "AGENT_TASK_AUDIT_AFTER_CALLBACK",
            "https://brave.com/search/api/", LocalDate.of(2026, 9, 23),
            "Pesquisa pública sem entrevistas obrigatórias.", List.of(), "PUBLIC_SOURCES_V1", false));
    mockMvc.perform(post("/api/product-discovery/v1/cycles/901/gap-deepening/public-research"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evidencePolicy").value("PUBLIC_SOURCES_V1"))
        .andExpect(jsonPath("$.interviewCount").value(0))
        .andExpect(jsonPath("$.maximumSearchCostUsd").value(0.12));
    org.mockito.Mockito.verify(customerInterviewService).adoptPublicEvidence(901L);
  }

  /** Deve expor o gate e rejeitar entrevista sem os dois consentimentos explícitos. */
  @Test
  void exposesGapDeepeningAndValidatesInterviewConsent() throws Exception {
    when(customerInterviewService.get(65L))
        .thenReturn(
            new ProductDiscoveryGapDeepeningResponse(
                65L,
                true,
                ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE,
                "customer-evidence",
                5,
                8,
                2,
                1,
                1,
                List.of(701L),
                List.of(702L),
                false,
                12,
                2,
                4,
                new BigDecimal("0.12000000"),
                "ESTIMATED_SEARCH_ONLY",
                "AGENT_TASK_AUDIT_AFTER_CALLBACK",
                "https://brave.com/search/api/",
                LocalDate.of(2026, 9, 23),
                "Falta cobrir uma candidata.",
                List.of(),
                "CONSENTED_INTERVIEWS_V1",
                true));

    mockMvc
        .perform(get("/api/product-discovery/v1/cycles/65/gap-deepening"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.minimumInterviews").value(5))
        .andExpect(jsonPath("$.maximumSearchCostUsd").value(0.12));

    mockMvc
        .perform(
            post("/api/product-discovery/v1/cycles/65/gap-deepening/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "opportunityId":701,
                      "anonymousParticipantCode":"P01",
                      "outcome":"PURCHASED",
                      "occurredOn":"2026-09-01",
                      "purchaseSituation":"Ocasião marcada.",
                      "desiredResult":"Sentir segurança.",
                      "difficulty":"Escolher entre opções.",
                      "alternativeTried":"Referências gratuitas.",
                      "remainingDifficulty":"Montar a decisão.",
                      "consentConfirmed":false,
                      "noPersonalDataConfirmed":true
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  /** Deve receber o desfecho auditável do Chromium pelo endpoint interno versionado. */
  @Test
  void recordsPublicMetaBrowserCollection() throws Exception {
    when(metaAdBrowserCollectionService.record(
            org.mockito.ArgumentMatchers.eq(81L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new ProductDiscoveryMetaAdEvidenceListResponse(
                81L,
                "guarda roupa cápsula climatério",
                "BR",
                "INSTAGRAM",
                "OBSERVED",
                "PUBLIC_BROWSER",
                91L,
                "https://www.facebook.com/ads/library/?country=BR&q=guarda+roupa",
                1,
                1,
                1,
                java.time.Instant.parse("2026-08-30T12:00:02Z"),
                "Cobertura observada sem inferir vendas.",
                List.of()));

    mockMvc
        .perform(
            post("/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/81/meta-ad-browser-collection")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "executionLeaseId":"lease-81",
                      "attemptNumber":1,
                      "investigationId":91,
                      "collectorRunId":"argos-browser-81-lease-81",
                      "searchUrl":"https://www.facebook.com/ads/library/?country=BR&q=guarda+roupa",
                      "outcome":"EMPTY",
                      "httpStatus":403,
                      "platformFilterConfirmed":true,
                      "pageTitle":"Biblioteca de Anúncios",
                      "startedAt":"2026-08-30T12:00:00Z",
                      "finishedAt":"2026-08-30T12:00:02Z",
                      "observations":[]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.collectionMode").value("PUBLIC_BROWSER"))
        .andExpect(jsonPath("$.sourceStatus").value("OBSERVED"));
  }

  /** Deve expor o ranking por maturidade comercial para a tela administrativa. */
  @Test
  void getMaturityRanking() throws Exception {
    var response =
        new ProductDiscoveryMaturityRankingResponse(
            "Ranking por maturidade comercial",
            "Dor concreta, lacuna clara e microexperiência rápida.",
            "Começar por renda extra.",
            List.of(
                new ProductDiscoveryMaturityItemResponse(
                    1,
                    "Renda extra",
                    "Oportunidade promissora",
                    "Mercado grande.",
                    "Dor urgente sem promessa garantida.",
                    "Abrir ciclo de pesquisa.",
                    List.of("Encaixe com WhatsApp"),
                    List.of("Sem ganho garantido"))),
            List.of(
                new ProductDiscoveryResearchTrackResponse(
                    "Renda extra para autônomos/MEIs",
                    "WhatsApp e primeira venda.",
                    "Maior chance de compra rápida.",
                    "renda extra para autonomos e MEIs",
                    "Autônomos e MEIs",
                    "TikTok, Reels e WhatsApp",
                    "Encontrar dor concreta.",
                    "Baixo esforço.",
                    "Promessa de renda garantida.")));

    when(service.getMaturityRanking()).thenReturn(response);

    mockMvc
        .perform(get("/api/product-discovery/v1/maturity-ranking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.strategyName").value("Ranking por maturidade comercial"))
        .andExpect(jsonPath("$.items[0].niche").value("Renda extra"))
        .andExpect(jsonPath("$.items[0].maturity").value("Oportunidade promissora"))
        .andExpect(
            jsonPath("$.recommendedTracks[0].theme").value("renda extra para autonomos e MEIs"));
  }

  /** Deve expor o comando administrativo que retoma Atena sem repetir Argos. */
  @Test
  void resumesPrivateValidationHandoff() throws Exception {
    when(service.resumePrivateValidationHandoff(77L))
        .thenReturn(
            new ProductDiscoveryPrivateValidationHandoffResponse(
                77L,
                "product-discovery-cycle:77",
                2,
                "QUEUED_FOR_PRIVATE_VALIDATION",
                "ATENA_PRIVATE_PROTOTYPE_SELECTION",
                "Atena recebeu os dossiês atuais."));

    mockMvc
        .perform(post("/api/product-discovery/v1/cycles/77/private-validation-handoff"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleId").value(77))
        .andExpect(jsonPath("$.dossierReadyCount").value(2))
        .andExpect(jsonPath("$.nextActivity").value("ATENA_PRIVATE_PROTOTYPE_SELECTION"));
  }

  /** Deve correlacionar a solicitação Meta ao lease vigente e expor a cobertura Instagram. */
  @Test
  void requestMetaAdEvidenceForActiveCycle() throws Exception {
    when(metaAdEvidenceService.requestAndSearch(
            org.mockito.ArgumentMatchers.eq(81L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new ProductDiscoveryMetaAdEvidenceListResponse(
                81L,
                "entrevista emprego",
                "BR",
                "INSTAGRAM",
                "AWAITING_SUPERVISED_OBSERVATION",
                "SUPERVISED",
                7L,
                "https://www.facebook.com/ads/library/?q=entrevista+emprego",
                0,
                0,
                0,
                null,
                "Cobertura aguardando observação",
                List.of()));

    mockMvc
        .perform(
            post("/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/81/meta-ad-evidence")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "executionLeaseId":"lease-81",
                      "attemptNumber":1,
                      "query":"entrevista emprego",
                      "country":"BR",
                      "publisherPlatform":"INSTAGRAM",
                      "limit":25
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleId").value(81))
        .andExpect(jsonPath("$.publisherPlatform").value("INSTAGRAM"))
        .andExpect(jsonPath("$.sourceStatus").value("AWAITING_SUPERVISED_OBSERVATION"));

    org.mockito.Mockito.verify(service).validateActiveExecution(81L, "lease-81");
  }

  /** Deve expor e registrar a sessão oficial sem aceitar uma URL externa à Biblioteca Meta. */
  @Test
  void exposesValidatedSupervisedMetaSession() throws Exception {
    ProductDiscoverySupervisedMetaSessionResponse response = supervisedSessionResponse();
    when(supervisedMetaSessionService.get(77L)).thenReturn(response);
    when(supervisedMetaSessionService.observe(
            org.mockito.ArgumentMatchers.eq(77L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(response);

    mockMvc
        .perform(get("/api/product-discovery/v1/cycles/77/supervised-meta-session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.investigationId").value(72))
        .andExpect(jsonPath("$.publisherPlatform").value("INSTAGRAM"));

    mockMvc
        .perform(
            post("/api/product-discovery/v1/cycles/77/supervised-meta-session/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adReference":"ad-72",
                      "advertiserName":"Marca observada",
                      "adLibraryUrl":"https://business.facebook.com/ads/library/?id=ad-72",
                      "adText":"Seu ritual de cinco minutos começa agora.",
                      "publisherPlatforms":["INSTAGRAM"],
                      "formatType":"VIDEO",
                      "pageActive":true,
                      "commercialSignal":true
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleId").value(77));

    mockMvc
        .perform(
            post("/api/product-discovery/v1/cycles/77/supervised-meta-session/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adReference":"ad-falso",
                      "advertiserName":"Origem inválida",
                      "adLibraryUrl":"https://example.com/anuncio",
                      "adText":"Texto não auditável.",
                      "publisherPlatforms":["INSTAGRAM"],
                      "pageActive":true,
                      "commercialSignal":false
                    }
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/product-discovery/v1/cycles/77/supervised-meta-session/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adReference":"ad-futuro",
                      "advertiserName":"Origem inválida",
                      "adLibraryUrl":"https://www.facebook.com/ads/library/?id=ad-futuro",
                      "adText":"Texto ainda não observado.",
                      "publisherPlatforms":["INSTAGRAM"],
                      "pageActive":true,
                      "commercialSignal":false,
                      "observedAt":"2099-01-01T00:00:00Z"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  /** Monta o contrato resumido usado pela rota administrativa da sessão supervisionada. */
  private ProductDiscoverySupervisedMetaSessionResponse supervisedSessionResponse() {
    return new ProductDiscoverySupervisedMetaSessionResponse(
        77L,
        72L,
        "COMPLETED",
        "autocuidado feminino visual",
        "BR",
        "INSTAGRAM",
        "AWAITING_SUPERVISED_OBSERVATION",
        "SUPERVISED",
        "Observação humana na fonte oficial.",
        "https://www.facebook.com/ads/library/?q=autocuidado",
        null,
        0,
        0,
        0,
        null,
        "Cobertura aguardando observação; isso não significa ausência de mercado.",
        true,
        false,
        "Registre um anúncio atual no Instagram.",
        List.of());
  }
}
