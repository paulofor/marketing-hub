package com.marketinghub.winningadlibrary.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.winningadlibrary.service.WinningAdLibraryService;
import com.marketinghub.winningadlibrary.service.listWinningAds.WinningAdListResponse;
import com.marketinghub.winningadlibrary.service.listWinningAds.WinningAdResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP da biblioteca de anúncios vencedores. */
@WebMvcTest(WinningAdLibraryController.class)
class WinningAdLibraryControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private WinningAdLibraryService winningAdLibraryService;

  /** Garante que o piloto Costure e Venda é exposto pela biblioteca. */
  @Test
  void listsWinningAdsLibraryPilot() throws Exception {
    when(winningAdLibraryService.listWinningAds("costure-e-venda"))
        .thenReturn(
            new WinningAdListResponse(
                1,
                List.of(
                    new WinningAdResponse(
                        1L,
                        "costure-e-venda",
                        "Costure e Venda",
                        "Costureiras autônomas",
                        "AQUISICAO",
                        "META_ADS",
                        "CARROSSEL",
                        "PILOTO",
                        91,
                        "Sua costura está boa, mas seus pedidos ainda chegam no improviso?",
                        "Mostre o antes e depois da rotina comercial.",
                        "Carrossel com conversa no WhatsApp, peça pronta e agenda organizada.",
                        "Organização de vendas para costureira autônoma.",
                        "Prova visual de agenda e mensagens prontas.",
                        "Piloto sem métrica real ainda.",
                        "Dor de venda improvisada é mais concreta que promessa genérica.",
                        "Publicar teste Meta Ads com orçamento controlado.",
                        "piloto-costure-e-venda",
                        Instant.parse("2026-08-02T00:00:00Z")))));

    mockMvc
        .perform(get("/api/winning-ads-library").param("productSlug", "costure-e-venda"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].productName").value("Costure e Venda"))
        .andExpect(jsonPath("$.items[0].score").value(91));
  }
}
