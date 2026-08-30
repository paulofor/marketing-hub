package com.marketinghub.productdiscovery.v1.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                    supervisedMetaSessionService))
            .build();
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
