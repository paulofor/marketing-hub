package com.marketinghub.pde.vega.privateprototype.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.vega.privateprototype.v1.*;
import com.marketinghub.pde.vega.privateprototype.v1.service.contract.VegaPrivateContract.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: prevenir gasto sintético, repetição e execução fora do ciclo vigente. */
class VegaPrivateExecutionSafetyTest {
  private static final String VERSION = "musa-pde-entry-v12-primeiro-ajuste-aplicavel";
  private static final String MODEL = "vega-deterministic-fixture-v1";
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final VegaPrivateSessionRepository sessions = mock(VegaPrivateSessionRepository.class);
  private final VegaAdjustmentExecutionRepository executions =
      mock(VegaAdjustmentExecutionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final VegaPrivateService service =
      new VegaPrivateService(sessions, executions, cycles, products, json);
  private final Input input = new Input("Almoço", "Camisa que já possuo", "");
  private VegaPrivateSession session;
  private LearningSalesCycle cycle;
  private VegaAdjustmentExecution execution;

  /** Isola cada teste em identidades sintéticas sem dependência do cadastro produtivo. */
  @BeforeEach
  void prepare() throws Exception {
    ReflectionTestUtils.setField(service, "version", VERSION);
    cycle = new LearningSalesCycle();
    cycle.setId(91007L);
    cycle.setProductId(91004L);
    cycle.setExperimentId(91100L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setProductVersion(VERSION);
    cycle.setInheritedLearningJson("{}");
    session = new VegaPrivateSession();
    session.setId("fixture-session");
    session.setCycleId(cycle.getId());
    session.setProductId(cycle.getProductId());
    session.setExperimentId(cycle.getExperimentId());
    session.setPrototypeVersion(VERSION);
    session.setOrigin("AGENT_VALIDATION");
    session.setExpiresAt(Instant.now().plusSeconds(3600));
    session.setState("INPUT");
    session.setEventsJson("{\"EXPERIENCE_STARTED\":{}}");
    execution = new VegaAdjustmentExecution();
    execution.setId(91001L);
    execution.setSessionId(session.getId());
    execution.setStatus("RUNNING");
    execution.setCreatedAt(Instant.now());
    execution.setInputJson(
        json.writeValueAsString(Map.of("origin", session.getOrigin(), "input", input)));
    execution.setRequestJson("{\"provider\":\"DETERMINISTIC_FIXTURE\"}");
    when(sessions.findBySessionHash(anyString())).thenReturn(Optional.of(session));
    when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findLockedById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(products.findById(cycle.getProductId()))
        .thenReturn(
            Optional.of(
                Product.builder().id(cycle.getProductId()).slug("metodo-musa-7-dias").build()));
    when(executions.findLocked(execution.getId())).thenReturn(Optional.of(execution));
    when(executions.findById(execution.getId())).thenReturn(Optional.of(execution));
    when(executions.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              VegaAdjustmentExecution row = invocation.getArgument(0);
              row.setId(91002L);
              when(executions.findById(91002L)).thenReturn(Optional.of(row));
              return row;
            });
  }

  /** Mantém o teto de sessões mesmo quando convites anteriores foram revogados. */
  @Test
  void rejectsNineteenthSyntheticSession() {
    when(sessions.countByCycleIdAndPrototypeVersionAndOriginIn(
            eq(cycle.getId()), eq(VERSION), anyList()))
        .thenReturn(18L);
    assertThatThrownBy(
            () -> service.create(new InternalSession(cycle.getId(), VERSION, "QA_INTERNAL", null)))
        .hasMessageContaining("18 sessões");
    verify(sessions, never()).saveAndFlush(any());
  }

  /** Mantém a variante anterior e aceita o sucessor somente no ciclo e versão correspondentes. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
        "musa-pde-entry-v13-primeiro-ajuste-aplicavel"
      })
  void createsImplementedVariantWithItsOwnIdentityAndQuota(String requestedVersion) {
    cycle.setProductVersion(requestedVersion);
    var result =
        service.create(
            new InternalSession(cycle.getId(), requestedVersion, "AGENT_VALIDATION", null));
    assertThat(result.path("prototypeVersion").asText()).isEqualTo(requestedVersion);
    assertThat(result.path("cycleId").asLong()).isEqualTo(cycle.getId());
    verify(sessions)
        .countByCycleIdAndPrototypeVersionAndOriginIn(
            eq(cycle.getId()), eq(requestedVersion), anyList());
    verify(sessions)
        .saveAndFlush(
            argThat(
                row ->
                    row.getExperimentId().equals(cycle.getExperimentId())
                        && row.getPrototypeVersion().equals(requestedVersion)));
  }

  /** Impede que o catálogo de versões substitua a correspondência exata com o ciclo. */
  @Test
  void rejectsImplementedVersionFromAnotherCycleAndUnknownVersion() {
    String successor = "musa-pde-entry-v13-primeiro-ajuste-aplicavel";
    assertThatThrownBy(
            () ->
                service.create(
                    new InternalSession(cycle.getId(), successor, "AGENT_VALIDATION", null)))
        .hasMessageContaining("versão do runtime");
    cycle.setProductVersion("musa-pde-entry-v99-primeiro-ajuste-aplicavel");
    assertThatThrownBy(
            () ->
                service.create(
                    new InternalSession(
                        cycle.getId(), cycle.getProductVersion(), "AGENT_VALIDATION", null)))
        .hasMessageContaining("versão do runtime");
    verify(sessions, never()).saveAndFlush(any());
  }

  /**
   * A nova variante preserva o teto durável e não contorna o limite ao trocar o padrão do runtime.
   */
  @Test
  void rejectsNineteenthSuccessorSession() {
    String successor = "musa-pde-entry-v13-primeiro-ajuste-aplicavel";
    cycle.setProductVersion(successor);
    when(sessions.countByCycleIdAndPrototypeVersionAndOriginIn(
            eq(cycle.getId()), eq(successor), anyList()))
        .thenReturn(18L);
    assertThatThrownBy(
            () ->
                service.create(new InternalSession(cycle.getId(), successor, "QA_INTERNAL", null)))
        .hasMessageContaining("18 sessões");
    verify(sessions, never()).saveAndFlush(any());
  }

  /** O suporte à variante não permite criar sessão ou consumir quota de outro produto. */
  @Test
  void rejectsSuccessorBoundToDifferentProduct() {
    String successor = "musa-pde-entry-v13-primeiro-ajuste-aplicavel";
    cycle.setProductVersion(successor);
    when(products.findById(cycle.getProductId()))
        .thenReturn(
            Optional.of(Product.builder().id(cycle.getProductId()).slug("mira-private").build()));
    assertThatThrownBy(
            () ->
                service.create(
                    new InternalSession(cycle.getId(), successor, "AGENT_VALIDATION", null)))
        .hasMessageContaining("ciclo privado aberto do Vega");
    verify(sessions, never()).saveAndFlush(any());
  }

  /** Conta falhas e bloqueios no teto de duas tentativas, sem nova geração. */
  @Test
  void rejectsThirdAttemptAndPreservesExistingFailure() {
    session.setExecutionId(execution.getId());
    execution.setStatus("FAILED");
    when(executions.countBySessionId(session.getId())).thenReturn(2L);
    assertThatThrownBy(() -> service.generate("synthetic", input))
        .hasMessageContaining("duas tentativas");
    verify(executions, never()).saveAndFlush(any());
  }

  /** Aplica o teto acumulado do mesmo ciclo e versão, além do limite da sessão. */
  @Test
  void rejectsAttemptAboveAccumulatedLimit() {
    when(executions.countSyntheticAttempts(eq(cycle.getId()), eq(VERSION), anyList()))
        .thenReturn(36L);
    assertThatThrownBy(() -> service.generate("synthetic", input))
        .hasMessageContaining("36 tentativas");
    verify(executions, never()).saveAndFlush(any());
  }

  /** Reutiliza o resultado existente e recusa trocar suas entradas silenciosamente. */
  @Test
  void preservesResultAndRejectsChangedInput() throws Exception {
    session.setExecutionId(execution.getId());
    session.setInputJson(json.writeValueAsString(input));
    execution.setStatus("COMPLETED");
    assertThat(service.generate("synthetic", input).path("executionId").asLong())
        .isEqualTo(execution.getId());
    assertThatThrownBy(() -> service.generate("synthetic", new Input("Trabalho", "Outra peça", "")))
        .hasMessageContaining("outra entrada");
    verify(executions, never()).saveAndFlush(any());
  }

  /** Recusa nova execução se o ciclo mudou após a consulta da fila. */
  @Test
  void rejectsClaimAndRequestAfterCycleClosed() {
    cycle.setStatus("CLOSED");
    execution.setStatus("QUEUED");
    assertThatThrownBy(() -> service.claim(execution.getId()))
        .hasMessageContaining("ciclo não permite");
    execution.setStatus("RUNNING");
    assertThatThrownBy(
            () ->
                service.request(
                    execution.getId(), new RequestAudit(json.createObjectNode(), MODEL)))
        .hasMessageContaining("ciclo não permite");
  }

  /** Não permite que a sessão sintética autorize request pago ao provedor. */
  @Test
  void rejectsProviderRequestForSyntheticSession() {
    assertThatThrownBy(
            () ->
                service.request(
                    execution.getId(),
                    new RequestAudit(json.createObjectNode().put("service_tier", "flex"), "gpt")))
        .hasMessageContaining("sem chamada paga");
  }

  /** Rejeita custo ou tokens em callback sintético sem descartar a auditoria recebida. */
  @ParameterizedTest
  @ValueSource(strings = {"cost", "tokens", "model"})
  void rejectsPaidSyntheticResult(String violation) throws Exception {
    var result =
        service.complete(
            execution.getId(),
            result(
                "model".equals(violation) ? "provider-model" : MODEL,
                "cost".equals(violation) ? new BigDecimal("0.01") : BigDecimal.ZERO,
                "tokens".equals(violation) ? 1L : 0L));
    assertThat(result.path("status").asText()).isEqualTo("FAILED");
    assertThat(result.path("rawResponse").path("audit").asText()).isEqualTo("preserved");
    assertThat(result.path("card").isNull()).isTrue();
  }

  /** Recebe replay da mesma execução sem substituir saída ou contabilizar geração nova. */
  @Test
  void completesFixtureAndReplaysWithoutOverwritingAudit() throws Exception {
    var completed = service.complete(execution.getId(), result(MODEL, BigDecimal.ZERO, 0L));
    assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
    var replay = service.complete(execution.getId(), result("other", BigDecimal.ONE, 10L));
    assertThat(replay).isEqualTo(completed);
    verify(executions, never()).saveAndFlush(any());
  }

  /** Executa geração, callback, sinais e retomada do sucessor sem inferência ou compra. */
  @Test
  void successorRunsFixtureThroughSavedCardAndSimulatedContinuation() throws Exception {
    String successor = "musa-pde-entry-v13-primeiro-ajuste-aplicavel";
    cycle.setProductVersion(successor);
    session.setPrototypeVersion(successor);
    var queued = service.generate("synthetic", input);
    Long id = queued.path("executionId").asLong();
    var created = executions.findById(id).orElseThrow();
    when(executions.findLocked(id)).thenReturn(Optional.of(created));
    assertThat(service.claim(id).path("status").asText()).isEqualTo("RUNNING");
    service.request(
        id,
        new RequestAudit(json.createObjectNode().put("provider", "DETERMINISTIC_FIXTURE"), MODEL));
    var template = result(MODEL, BigDecimal.ZERO, 0L);
    var card = (com.fasterxml.jackson.databind.node.ObjectNode) template.card().deepCopy();
    card.put("cardId", String.valueOf(id));
    assertThat(
            service
                .complete(
                    id,
                    new Result(
                        "COMPLETED",
                        card,
                        template.rawResponse(),
                        MODEL,
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        null))
                .path("status")
                .asText())
        .isEqualTo("COMPLETED");
    service.event("synthetic", new Event("VALUE_MOMENT", "Entendi o primeiro ajuste"));
    service.event("synthetic", new Event("READY_RESULT_USED", "Usei com conforto"));
    var simulated =
        service.event("synthetic", new Event("CHECKOUT_STARTED", "Simulação sem cobrança"));
    assertThat(simulated.path("prototypeVersion").asText()).isEqualTo(successor);
    assertThat(simulated.path("card").path("cardId").asText()).isEqualTo(String.valueOf(id));
    service.finish("synthetic");
    assertThat(service.session("synthetic").path("card")).isEqualTo(card);
    assertThat(service.session("synthetic").path("events").has("CHECKOUT_STARTED")).isTrue();
    verify(executions, times(1)).saveAndFlush(any());
  }

  /** Preserva o callback antes válido do provedor em sessão legada distinta de homologação. */
  @Test
  void retainsLegacyProviderResult() throws Exception {
    execution.setInputJson(json.writeValueAsString(Map.of("origin", "HUMAN", "input", input)));
    assertThat(
            service
                .complete(execution.getId(), result("legacy-model", new BigDecimal("0.001"), 10L))
                .path("status")
                .asText())
        .isEqualTo("COMPLETED");
  }

  /** Mantém filas de fixture e provedor separadas e rejeita modalidades desconhecidas. */
  @Test
  void separatesPendingModes() {
    when(executions.pending(any(), anyList(), any())).thenReturn(List.of());
    service.pending();
    verify(executions).pending(any(), eq(List.of("HUMAN")), any());
    service.pending("FIXTURE");
    verify(executions).pending(any(), eq(List.of("QA_INTERNAL", "AGENT_VALIDATION")), any());
    assertThatThrownBy(() -> service.pending("unbounded")).hasMessageContaining("inválida");
  }

  /** Monta resposta completa correlacionada à entrada real da fixture. */
  private Result result(String model, BigDecimal cost, Long tokens) throws Exception {
    var card =
        json.readTree(
            "{\"cardId\":\"91001\",\"action\":\"Alinhe a camisa\",\"application\":\"Acomode o tecido com conforto\",\"occasion\":\"Almoço\",\"selfAssessmentPrompt\":\"Ficou confortável?\",\"usesOnlyAvailableItems\":true}");
    return new Result(
        "COMPLETED",
        card,
        json.createObjectNode().put("audit", "preserved"),
        model,
        tokens,
        tokens,
        cost,
        null);
  }
}
