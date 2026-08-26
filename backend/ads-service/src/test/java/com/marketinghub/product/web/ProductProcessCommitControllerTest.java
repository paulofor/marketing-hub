package com.marketinghub.product.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.product.service.processcommit.ProductProcessCommitRegistrationResult;
import com.marketinghub.product.service.processcommit.ProductProcessCommitResponse;
import com.marketinghub.product.service.processcommit.ProductProcessCommitService;
import com.marketinghub.web.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato HTTP dos commits por produto e processo. */
class ProductProcessCommitControllerTest {
  private static final String SHA = "a".repeat(40);

  /** Lista os vínculos persistidos com identidade da versão do processo. */
  @Test
  void listsProductProcessCommits() throws Exception {
    ProductProcessCommitService service = mock(ProductProcessCommitService.class);
    when(service.list(9L)).thenReturn(List.of(response()));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductProcessCommitController(service)).build();

    mockMvc
        .perform(get("/api/products/9/process-commits"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].productId").value(9L))
        .andExpect(jsonPath("$[0].processDefinitionId").value(43L))
        .andExpect(jsonPath("$[0].processVersion").value(4))
        .andExpect(jsonPath("$[0].commitSha").value(SHA));
  }

  /** Detalha o recurso criado na localização informada pelo POST. */
  @Test
  void getsProductProcessCommit() throws Exception {
    ProductProcessCommitService service = mock(ProductProcessCommitService.class);
    when(service.get(9L, 71L)).thenReturn(response());
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductProcessCommitController(service)).build();

    mockMvc
        .perform(get("/api/products/9/process-commits/71"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(71L))
        .andExpect(jsonPath("$.productId").value(9L));
  }

  /** Responde 201 e localização canônica quando cria um novo vínculo. */
  @Test
  void createsProductProcessCommit() throws Exception {
    ProductProcessCommitService service = mock(ProductProcessCommitService.class);
    when(service.register(any(), any()))
        .thenReturn(new ProductProcessCommitRegistrationResult(response(), true));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductProcessCommitController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(
            post("/api/products/9/process-commits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/products/9/process-commits/71"))
        .andExpect(jsonPath("$.recordedBy").value("time@marketinghub.io"));
  }

  /** Responde 200 quando o mesmo vínculo já existe e não fabrica outra criação. */
  @Test
  void returnsExistingIdempotentCommit() throws Exception {
    ProductProcessCommitService service = mock(ProductProcessCommitService.class);
    when(service.register(any(), any()))
        .thenReturn(new ProductProcessCommitRegistrationResult(response(), false));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductProcessCommitController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(
            post("/api/products/9/process-commits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(71L));
  }

  /** Rejeita SHA abreviado antes de chegar ao serviço de persistência. */
  @Test
  void rejectsAbbreviatedCommitSha() throws Exception {
    ProductProcessCommitService service = mock(ProductProcessCommitService.class);
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductProcessCommitController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(
            post("/api/products/9/process-commits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace(SHA, "abc1234")))
        .andExpect(status().isBadRequest());
  }

  /** Monta o JSON válido usado nos cenários HTTP. */
  private String validRequest() {
    return """
        {
          "processDefinitionId": 43,
          "repositoryName": "paulofor/marketing-hub",
          "commitSha": "%s",
          "commitSummary": "Registra commits no histórico do produto",
          "commitUrl": "https://github.com/paulofor/marketing-hub/commit/%s",
          "recordedBy": "time@marketinghub.io"
        }
        """
        .formatted(SHA, SHA);
  }

  /** Monta a resposta representativa do vínculo criado. */
  private ProductProcessCommitResponse response() {
    return new ProductProcessCommitResponse(
        71L,
        9L,
        43L,
        "pde-communication-sales-journey",
        "Comunicação e jornada de venda do PDE",
        4,
        "paulofor/marketing-hub",
        SHA,
        "Registra commits no histórico do produto",
        "https://github.com/paulofor/marketing-hub/commit/" + SHA,
        "time@marketinghub.io",
        Instant.parse("2026-08-26T12:30:00Z"));
  }
}
