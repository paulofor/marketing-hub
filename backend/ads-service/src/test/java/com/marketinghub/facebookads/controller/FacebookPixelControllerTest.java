package com.marketinghub.facebookads.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.facebookads.service.FacebookPixelConversionService;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/** Valida o contrato HTTP da fila e da confirmação de compras PDE no CAPI. */
@WebMvcTest(FacebookPixelController.class)
class FacebookPixelControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean MarketNicheService marketNicheService;

  @MockBean MarketNicheMapper marketNicheMapper;

  @MockBean FacebookPixelConversionService conversionService;

  /** Expõe valor, identidade hashada, URL e evento financeiro estável sem dados em texto puro. */
  @Test
  void listsPdeConversionsReady() throws Exception {
    when(conversionService.listApprovedPdePurchasesPendingPixel(50))
        .thenReturn(
            List.of(
                new FacebookPixelConversionService.PdePixelConversion(
                    "MERCADO_PAGO:mp-mira-1",
                    93L,
                    "Mira",
                    "pixel-mira",
                    "pde:MERCADO_PAGO:mp-mira-1",
                    new BigDecimal("49.00"),
                    "brl",
                    Instant.parse("2026-09-29T11:00:00Z"),
                    "a".repeat(64),
                    "https://mira.digicomdigital.com.br/")));

    mockMvc
        .perform(get("/api/facebook-pixels/pde-conversions-ready"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sourceReference").value("MERCADO_PAGO:mp-mira-1"))
        .andExpect(jsonPath("$[0].experimentId").value(93))
        .andExpect(jsonPath("$[0].eventId").value("pde:MERCADO_PAGO:mp-mira-1"))
        .andExpect(jsonPath("$[0].amount").value(49.00))
        .andExpect(jsonPath("$[0].currency").value("BRL"))
        .andExpect(jsonPath("$[0].hashedEmail").value("a".repeat(64)))
        .andExpect(jsonPath("$[0].eventSourceUrl").value("https://mira.digicomdigital.com.br/"));
  }

  /** Encaminha o ACK somente com a referência financeira e o pixel confirmados pelo worker. */
  @Test
  void acknowledgesPdeConversion() throws Exception {
    mockMvc
        .perform(
            post("/api/facebook-pixels/pde-conversions/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceReference":"MERCADO_PAGO:mp-mira-1","pixelId":"pixel-mira"}
                    """))
        .andExpect(status().isNoContent());

    verify(conversionService).markPdeConversionRecorded("MERCADO_PAGO:mp-mira-1", "pixel-mira");
  }

  /** Rejeita ACK vazio antes que uma referência ambígua alcance a persistência. */
  @Test
  void rejectsBlankPdeConversionAck() throws Exception {
    mockMvc
        .perform(
            post("/api/facebook-pixels/pde-conversions/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceReference\":\"\",\"pixelId\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  /** Responde HTTP 400 quando a referência financeira não pertence ao pixel informado. */
  @Test
  void rejectsPdeConversionAckForStalePixel() throws Exception {
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "pixel vigente"))
        .when(conversionService)
        .markPdeConversionRecorded("MERCADO_PAGO:mp-mira-1", "pixel-antigo");

    mockMvc
        .perform(
            post("/api/facebook-pixels/pde-conversions/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceReference":"MERCADO_PAGO:mp-mira-1","pixelId":"pixel-antigo"}
                    """))
        .andExpect(status().isBadRequest());
  }
}
