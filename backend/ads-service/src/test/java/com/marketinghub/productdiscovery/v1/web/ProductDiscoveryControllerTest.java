package com.marketinghub.productdiscovery.v1.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMarketplaceEvidenceService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMaturityItemResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMaturityRankingResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryResearchTrackResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: valida o contrato REST da descoberta de produtos PDE. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryControllerTest {

  private MockMvc mockMvc;

  @Mock private ProductDiscoveryService service;
  @Mock private ProductDiscoveryMarketplaceEvidenceService marketplaceEvidenceService;

  /** Monta o controller isolado para testar as rotas do módulo. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProductDiscoveryController(service, marketplaceEvidenceService))
            .build();
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
}
