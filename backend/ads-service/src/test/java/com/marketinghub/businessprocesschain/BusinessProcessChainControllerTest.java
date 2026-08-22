package com.marketinghub.businessprocesschain;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocesschain.controller.BusinessProcessChainController;
import com.marketinghub.businessprocesschain.service.BusinessProcessChainService;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainDetailResponse;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainProcessResponse;
import com.marketinghub.businessprocesschain.service.listChains.BusinessProcessChainSummaryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar os contratos HTTP de leitura das cadeias de valor. */
@ExtendWith(MockitoExtension.class)
class BusinessProcessChainControllerTest {
  private MockMvc mockMvc;

  @Mock private BusinessProcessChainService service;

  /** Monta o controller isolado com o service governado. */
  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new BusinessProcessChainController(service)).build();
  }

  /** Expõe lista operacional de cadeias publicadas e a contagem oficial de processos. */
  @Test
  void listsChains() throws Exception {
    when(service.listChains())
        .thenReturn(
            List.of(
                new BusinessProcessChainSummaryResponse(
                    1L,
                    "pde-value-creation-delivery",
                    "Cadeia PDE",
                    "Criar valor.",
                    "Venda entregue.",
                    "Tempo até venda entregue com satisfação",
                    1,
                    "PUBLISHED",
                    6,
                    Instant.parse("2026-08-20T10:00:00Z"))));

    mockMvc
        .perform(get("/api/business-process-chains"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].chainCode").value("pde-value-creation-delivery"))
        .andExpect(jsonPath("$[0].processCount").value(6));
  }

  /** Expõe as cadeias às quais pertence uma versão exata de processo. */
  @Test
  void listsChainsByProcess() throws Exception {
    when(service.listChainsByProcess(22L))
        .thenReturn(
            List.of(
                new BusinessProcessChainSummaryResponse(
                    1L,
                    "pde-value-creation-delivery",
                    "Cadeia PDE",
                    "Criar valor.",
                    "Venda entregue.",
                    "Tempo até venda entregue com satisfação",
                    1,
                    "PUBLISHED",
                    6,
                    Instant.parse("2026-08-20T10:00:00Z"))));

    mockMvc
        .perform(get("/api/business-process-chains/by-process/22"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Cadeia PDE"));
  }

  /** Expõe objetivo, resultado e contribuição do processo no detalhe. */
  @Test
  void getsChainDetail() throws Exception {
    var process =
        new BusinessProcessChainProcessResponse(
            1,
            "Escolhe uma dor real.",
            11L,
            "pde-opportunity-discovery",
            "Descoberta PDE",
            "Comprovar demanda.",
            "Inteligência de Mercado",
            "Sinais reais.",
            "Oportunidade aprovada.",
            1,
            "PUBLISHED");
    when(service.getChain(1L))
        .thenReturn(
            new BusinessProcessChainDetailResponse(
                1L,
                "pde-value-creation-delivery",
                "Cadeia PDE",
                "Criar valor.",
                "Venda entregue.",
                "Tempo até venda entregue com satisfação",
                1,
                "PUBLISHED",
                1,
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-20T10:00:00Z"),
                List.of(process)));

    mockMvc
        .perform(get("/api/business-process-chains/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processes[0].sequenceNumber").value(1))
        .andExpect(jsonPath("$.processes[0].valueContribution").value("Escolhe uma dor real."))
        .andExpect(jsonPath("$.processes[0].outcomeDescription").value("Oportunidade aprovada."));
  }
}
