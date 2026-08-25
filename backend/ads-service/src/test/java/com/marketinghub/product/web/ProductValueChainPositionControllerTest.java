package com.marketinghub.product.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionResponse;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import com.marketinghub.web.ApiExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato HTTP da posição do produto na cadeia de valor. */
class ProductValueChainPositionControllerTest {
  /** Expõe posição, nome humano e navegação canônica do processo atual. */
  @Test
  void listsProductValueChainPositions() throws Exception {
    ProductValueChainPositionService service = mock(ProductValueChainPositionService.class);
    when(service.listPositions())
        .thenReturn(
            List.of(
                new ProductValueChainPositionResponse(
                    9L,
                    "COMUNICACAO_E_JORNADA",
                    "IDENTIFIED",
                    "Posição identificada na cadeia de valor vigente.",
                    5L,
                    "Criação e entrega de valor de Produtos Digitais Experienciais",
                    5,
                    43L,
                    "pde-communication-sales-journey",
                    "Comunicação e jornada de venda do PDE",
                    4,
                    4,
                    6,
                    List.of(),
                    null)));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductValueChainPositionController(service)).build();

    mockMvc
        .perform(get("/api/products/value-chain-positions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].productId").value(9L))
        .andExpect(jsonPath("$[0].processDefinitionId").value(43L))
        .andExpect(jsonPath("$[0].processName").value("Comunicação e jornada de venda do PDE"))
        .andExpect(jsonPath("$[0].sequenceNumber").value(4))
        .andExpect(jsonPath("$[0].processCount").value(6));
  }

  /** Expõe o histórico de um produto sem obrigar a tela a carregar todo o catálogo. */
  @Test
  void getsProductValueChainPosition() throws Exception {
    ProductValueChainPositionService service = mock(ProductValueChainPositionService.class);
    when(service.getPosition(9L))
        .thenReturn(
            new ProductValueChainPositionResponse(
                9L,
                "COMUNICACAO_E_JORNADA",
                "IDENTIFIED",
                "Posição identificada na cadeia de valor vigente.",
                5L,
                "Criação e entrega de valor de Produtos Digitais Experienciais",
                5,
                43L,
                "pde-communication-sales-journey",
                "Comunicação e jornada de venda do PDE",
                4,
                4,
                6,
                List.of(),
                null));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductValueChainPositionController(service)).build();

    mockMvc
        .perform(get("/api/products/value-chain-positions/9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(9L))
        .andExpect(jsonPath("$.processName").value("Comunicação e jornada de venda do PDE"))
        .andExpect(jsonPath("$.sequenceNumber").value(4))
        .andExpect(jsonPath("$.processCount").value(6));
  }

  /** Responde 404 quando o produto solicitado não existe, sem fabricar histórico. */
  @Test
  void reportsMissingProduct() throws Exception {
    ProductValueChainPositionService service = mock(ProductValueChainPositionService.class);
    when(service.getPosition(999L))
        .thenThrow(new EntityNotFoundException("Produto não encontrado: 999"));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductValueChainPositionController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(get("/api/products/value-chain-positions/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Recurso não encontrado."));
  }
}
