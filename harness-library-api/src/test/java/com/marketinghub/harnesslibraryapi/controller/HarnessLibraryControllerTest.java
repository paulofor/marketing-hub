package com.marketinghub.harnesslibraryapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.harnesslibraryapi.api.CardVersionResponse;
import com.marketinghub.harnesslibraryapi.api.RegisterCardRequest;
import com.marketinghub.harnesslibraryapi.config.ApiKeyAuthenticationFilter;
import com.marketinghub.harnesslibraryapi.config.HarnessLibraryProperties;
import com.marketinghub.harnesslibraryapi.service.HarnessLibraryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Valida autenticação, JSON estrito e contrato HTTP público do gateway. */
@WebMvcTest(HarnessLibraryController.class)
@Import({ApiKeyAuthenticationFilter.class, HarnessLibraryExceptionHandler.class})
@EnableConfigurationProperties(HarnessLibraryProperties.class)
@TestPropertySource(
    properties = "harness-library.api-key=public-api-key-with-more-than-32-characters")
class HarnessLibraryControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private HarnessLibraryService service;

  /** Bloqueia cadastro quando a chave pública não foi apresentada. */
  @Test
  void shouldRejectMissingApiKey() throws Exception {
    mockMvc
        .perform(
            post("/v1/cards")
                .contentType("application/json")
                .header("X-Actor", "codex-homologacao")
                .header("Idempotency-Key", "idem-register-001")
                .content(validJson()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(header().exists("X-Request-ID"));

    verifyNoInteractions(service);
  }

  /** Encaminha JSON autenticado e devolve localização pública da versão criada. */
  @Test
  void shouldRegisterAuthenticatedCard() throws Exception {
    when(service.register(
            any(RegisterCardRequest.class),
            eq("codex-homologacao"),
            eq("idem-register-001"),
            any(String.class)))
        .thenReturn(response());

    mockMvc
        .perform(
            post("/v1/cards")
                .contentType("application/json")
                .header("X-API-Key", "public-api-key-with-more-than-32-characters")
                .header("X-Actor", "codex-homologacao")
                .header("Idempotency-Key", "idem-register-001")
                .content(validJson()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/v1/cards/homologacao-card/versions/1"))
        .andExpect(jsonPath("$.cardKey").value("homologacao-card"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  /** Rejeita campo desconhecido para impedir divergência silenciosa do contrato JSON. */
  @Test
  void shouldRejectUnknownJsonField() throws Exception {
    String invalid = validJson().replace("\n}", ",\n\"inventedField\":true\n}");

    mockMvc
        .perform(
            post("/v1/cards")
                .contentType("application/json")
                .header("X-API-Key", "public-api-key-with-more-than-32-characters")
                .header("X-Actor", "codex-homologacao")
                .header("Idempotency-Key", "idem-register-001")
                .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_JSON"));
  }

  /** Bloqueia corpos excessivos antes de desserializar ou chamar a orquestração. */
  @Test
  void shouldRejectOversizedJsonBeforeDeserialization() throws Exception {
    mockMvc
        .perform(
            post("/v1/cards")
                .contentType("application/json")
                .header("X-API-Key", "public-api-key-with-more-than-32-characters")
                .header("X-Actor", "codex-homologacao")
                .header("Idempotency-Key", "idem-register-001")
                .content("x".repeat(32 * 1024 + 1)))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));

    verifyNoInteractions(service);
  }

  /** Retorna um JSON válido e completo usado nos cenários do controller. */
  private String validJson() {
    return """
        {
          "cardKey":"homologacao-card",
          "collection":"video",
          "title":"Demonstração clara",
          "finding":"Mostrar reduz ambiguidade.",
          "mechanism":"Concretização visual.",
          "commercialApplication":"Comparar retenção e CTA.",
          "evidenceStrength":"Hipótese externa.",
          "publishedOn":"2026-09-04",
          "validUntil":"2026-10-19",
          "experimentHypothesis":"Aumentará CTA.",
          "risks":"Generalização.",
          "limits":"Pagamento comprova venda.",
          "sourceKind":"TEXT",
          "sourceUri":"urn:test:card",
          "sourceTitle":"Fonte sintética",
          "sourceSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        }
        """;
  }

  /** Monta a resposta canônica devolvida pelo service simulado. */
  private CardVersionResponse response() {
    return new CardVersionResponse(
        "homologacao-card",
        1,
        "RI1-AAAAAAAAAAAA",
        "DRAFT",
        "DRAFT",
        "video",
        List.of("videomaker"),
        "Demonstração clara",
        "Mostrar reduz ambiguidade.",
        "Concretização visual.",
        "Comparar retenção e CTA.",
        "Hipótese externa.",
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 10, 19),
        "Aumentará CTA.",
        "Generalização.",
        "Pagamento comprova venda.",
        "TEXT",
        "urn:test:card",
        "Fonte sintética",
        "a".repeat(64),
        "codex-homologacao",
        LocalDateTime.of(2026, 9, 4, 12, 0),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.of(2026, 9, 4, 12, 0));
  }
}
