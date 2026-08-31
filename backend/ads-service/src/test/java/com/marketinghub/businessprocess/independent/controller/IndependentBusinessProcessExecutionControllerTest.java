package com.marketinghub.businessprocess.independent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionService;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessCatalogResponse;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar as rotas HTTP do cockpit de processos independentes. */
class IndependentBusinessProcessExecutionControllerTest {

  /** Expõe disponibilidade e campos declarados pelo backend no catálogo operacional. */
  @Test
  void listsRunnableCatalog() throws Exception {
    var service = mock(IndependentBusinessProcessExecutionService.class);
    when(service.catalog())
        .thenReturn(
            List.of(
                new IndependentBusinessProcessCatalogResponse(
                    52L,
                    "pde-opportunity-discovery",
                    "Descoberta PDE",
                    "Reunir fatos.",
                    "Argos",
                    "Pergunta de mercado.",
                    "Dossiê factual.",
                    6,
                    true,
                    "Pronto para iniciar sem produto.",
                    List.of(
                        new IndependentBusinessProcessInputFieldResponse(
                            "theme", "Tema", "TEXT", true, 191, null, null)))));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new IndependentBusinessProcessExecutionController(service))
            .build();

    mockMvc
        .perform(get("/api/independent-business-process-executions/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].processDefinitionId").value(52))
        .andExpect(jsonPath("$[0].inputFields[0].key").value("theme"));
  }

  /** Inicia a solicitação idempotente e devolve a localização auditável criada. */
  @Test
  void startsIndependentExecution() throws Exception {
    var service = mock(IndependentBusinessProcessExecutionService.class);
    var summary =
        new IndependentBusinessProcessExecutionSummaryResponse(
            91L,
            UUID.fromString("b82df168-e383-4acd-8ca4-ab858b39fd3e"),
            52L,
            "pde-opportunity-discovery",
            "Descoberta PDE",
            6,
            "product-discovery-cycle:77",
            "agenda vazia",
            "Operação",
            new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode()
                .put("theme", "agenda vazia"),
            "PENDING",
            1,
            0,
            null,
            null,
            null,
            null,
            "NOT_REPORTED",
            null,
            Instant.parse("2026-08-30T14:00:00Z"),
            null,
            null);
    when(service.start(any()))
        .thenReturn(new IndependentBusinessProcessExecutionResponse(summary, List.of(), null));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new IndependentBusinessProcessExecutionController(service))
            .build();

    mockMvc
        .perform(
            post("/api/independent-business-process-executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"requestKey\":\"b82df168-e383-4acd-8ca4-ab858b39fd3e\",\"processDefinitionId\":52,\"requestedByName\":\"Operação\",\"input\":{\"theme\":\"agenda vazia\"}}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/independent-business-process-executions/91"))
        .andExpect(jsonPath("$.execution.status").value("PENDING"));
  }
}
