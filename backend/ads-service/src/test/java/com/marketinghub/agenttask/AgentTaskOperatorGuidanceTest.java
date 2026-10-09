package com.marketinghub.agenttask;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.service.pending.AgentTaskOperatorGuidance;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: impedir perda ou cruzamento da orientação financeira entregue aos executores.
 */
class AgentTaskOperatorGuidanceTest {
  private static final Instant NOW = Instant.parse("2026-10-09T05:00:00Z");
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentTaskOperatorGuidance guidance =
      new AgentTaskOperatorGuidance(products, Clock.fixed(NOW, ZoneOffset.UTC));

  /** Inicializa o componente no contrato real do backend, que não fornece Clock global. */
  @Test
  void startsWithoutGlobalClockBean() {
    new ApplicationContextRunner()
        .withBean(ProductRepository.class, () -> products)
        .withUserConfiguration(AgentTaskOperatorGuidance.class)
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertNotNull(context.getBean(AgentTaskOperatorGuidance.class));
              assertEquals(0, context.getBeansOfType(Clock.class).size());
            });
  }

  /** Reproduz a autorização de Capella sem transformar o texto em saldo ou orçamento novo. */
  @Test
  void deliversExactDecisionFromCanonicalProduct() {
    String notes =
        "Para Capella, autorizo até US$ 10 de IA para revisar produto e comunicação, sem mídia nem vídeos pagos.\n"
            + "Mesma preparação: contar consumo anterior; não reiniciar o teto no sucessor.";
    when(products.findById(7L)).thenReturn(Optional.of(product(7L, notes)));

    var result = guidance.read(task(668L, 7L, "experiment:104"));

    assertEquals("PRODUCT_OPERATOR_GUIDANCE_V1", result.contractVersion());
    assertEquals(7L, result.productId());
    assertEquals("internal://products/7/commercial-notes", result.evidenceReference());
    assertEquals(NOW, result.retrievedAt());
    assertEquals(notes, result.commercialNotes());
  }

  /** Confirma a mesma regra em outro produto sem copiar sua autorização ou suas notas. */
  @Test
  void keepsDifferentProductsIsolated() {
    when(products.findById(7L))
        .thenReturn(Optional.of(product(7L, "Capella: teto cumulativo US$ 10")));
    when(products.findById(18L))
        .thenReturn(Optional.of(product(18L, "Outro produto: manter pausa")));

    assertEquals(
        "Capella: teto cumulativo US$ 10",
        guidance.read(task(668L, 7L, "experiment:104")).commercialNotes());
    assertEquals(
        "Outro produto: manter pausa",
        guidance.read(task(9018L, 18L, "experiment:9018")).commercialNotes());
  }

  /**
   * Recusa alvo inconsistente sem procurar produto pelo título ou por uma referência alternativa.
   */
  @Test
  void rejectsMismatchedTargetWithoutReadingOtherProducts() {
    var original = task(668L, 7L, "experiment:104");
    var mismatched =
        new AgentTaskPendingResponse(
            original.taskId(),
            original.agentKey(),
            original.processCode(),
            original.processVersion(),
            original.activityId(),
            original.activityName(),
            original.title(),
            original.description(),
            "experiment:999",
            original.receivedAt(),
            null,
            original.taskTarget(),
            "{}",
            null);

    assertNull(guidance.read(mismatched));
    verifyNoInteractions(products);
  }

  /** Preserva ausência explícita quando o alvo ou as notas não foram registrados. */
  @Test
  void preservesMissingGuidanceWithoutInventingAuthorization() {
    when(products.findById(7L)).thenReturn(Optional.of(product(7L, null)));
    assertNull(guidance.read(task(668L, 7L, "experiment:104")));
    when(products.findById(7L)).thenReturn(Optional.of(product(7L, "  ")));
    assertNull(guidance.read(task(668L, 7L, "experiment:104")));
    var noTarget =
        new AgentTaskPendingResponse(
            1L,
            "customer-agent",
            "pde-construction-approval",
            26,
            "psiqueSafety",
            "Segurança",
            "Tarefa",
            "Descrição",
            "experiment:104",
            NOW,
            "{}");
    assertNull(guidance.read(noTarget));
  }

  /** Confere JSON real de pending e lease, incluindo atualização das notas sem nova reserva. */
  @Test
  void transportsNotesAndRefreshesLeaseWithoutChangingOriginalContract() throws Exception {
    var service = mock(AgentTaskService.class);
    var response = task(668L, 7L, "experiment:104");
    when(service.claimEligibleProcessTask("customer-agent", null, null, null, null))
        .thenReturn(Optional.of(response));
    when(service.claimedProcessTask("customer-agent", 668L)).thenReturn(response);
    when(products.findById(7L))
        .thenReturn(
            Optional.of(product(7L, "Teto cumulativo US$ 10")),
            Optional.of(
                product(
                    7L, "Teto cumulativo US$ 10; custo estimado atualizado e saldo conferido")));
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(
                    service, mock(AgentTaskVisualEvidenceService.class), guidance))
            .build();

    var pending =
        mvc.perform(get("/api/internal/agent-tasks/customer-agent/stage-executions/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].taskId").value(668))
            .andExpect(jsonPath("$[0].taskTarget.productId").value(7))
            .andExpect(jsonPath("$[0].processContextJson").value("{}"))
            .andExpect(
                jsonPath("$[0].operatorGuidance.commercialNotes").value("Teto cumulativo US$ 10"))
            .andExpect(jsonPath("$[0].task").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var lease =
        mvc.perform(get("/api/internal/agent-tasks/customer-agent/stage-executions/668"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.operatorGuidance.commercialNotes")
                    .value("Teto cumulativo US$ 10; custo estimado atualizado e saldo conferido"))
            .andExpect(jsonPath("$.operatorGuidance.productId").value(7))
            .andExpect(jsonPath("$.taskTarget.experienceVersion").value("candidate-v2"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    ObjectMapper json = new ObjectMapper();
    var before = json.readTree(pending).get(0).deepCopy();
    var after = json.readTree(lease).deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) before).remove("operatorGuidance");
    ((com.fasterxml.jackson.databind.node.ObjectNode) after).remove("operatorGuidance");
    assertEquals(before, after);
    verify(service, times(1)).claimEligibleProcessTask("customer-agent", null, null, null, null);
    verify(service, times(1)).claimedProcessTask("customer-agent", 668L);
  }

  /** Mantém o formato HTTP anterior quando não existe orientação adicional. */
  @Test
  void preservesPreviouslyValidJsonWithoutNotes() throws Exception {
    var service = mock(AgentTaskService.class);
    when(service.claimedProcessTask("customer-agent", 668L))
        .thenReturn(task(668L, 7L, "experiment:104"));
    when(products.findById(7L)).thenReturn(Optional.empty());
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(
                    service, mock(AgentTaskVisualEvidenceService.class), guidance))
            .build();

    mvc.perform(get("/api/internal/agent-tasks/customer-agent/stage-executions/668"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(668))
        .andExpect(jsonPath("$.operatorGuidance").doesNotExist());
  }

  /** Cria ficha isolada com a mesma fonte usada pela tela de edição do produto. */
  private static Product product(Long id, String notes) {
    return Product.builder().id(id).commercialNotes(notes).build();
  }

  /** Cria tarefa com alvo canônico para reproduzir diferentes produtos e referências. */
  private static AgentTaskPendingResponse task(Long id, Long productId, String source) {
    var target =
        new AgentTaskTargetResponse(
            source,
            104L,
            productId,
            "test-product",
            "Produto",
            "Interno",
            "candidate-v2",
            "https://example.test/private",
            null,
            null,
            null,
            null);
    return new AgentTaskPendingResponse(
        id,
        "customer-agent",
        "pde-construction-approval",
        26,
        "psiqueSafety",
        "Segurança",
        "Tarefa",
        "Descrição",
        source,
        NOW,
        null,
        target,
        "{}",
        null);
  }
}
