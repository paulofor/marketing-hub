package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentAiPromptSchemaUsageService;
import com.marketinghub.gerasalespage.v1.*;
import com.marketinghub.gerasalespage.v1.service.retry.GeraSalesPageRetryPolicy;
import com.marketinghub.gerasalespage.v1.web.GeraSalesPageController;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: comprovar retomada técnica isolada, identidade das entradas e histórico
 * auditável.
 */
class GeraSalesPageStageRetryTest {
  final ObjectMapper mapper = new ObjectMapper();
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final GeraSalesPageStageExecutionRepository executions =
      mock(GeraSalesPageStageExecutionRepository.class);
  final AiPromptSchemaTemplateRepository templates = mock(AiPromptSchemaTemplateRepository.class);
  final GeraSalesPagePublicationAuditService publications =
      mock(GeraSalesPagePublicationAuditService.class);
  final ExperimentAiPromptSchemaUsageService usages =
      mock(ExperimentAiPromptSchemaUsageService.class);
  final GeraSalesPageStageService service =
      new GeraSalesPageStageService(
          experiments,
          executions,
          templates,
          mock(DeliverablePackageRepository.class),
          publications,
          usages,
          mock(CommercialPlanLandingAssetService.class),
          mapper);
  final List<GeraSalesPageStageExecution> history = new ArrayList<>();
  final Experiment experiment = new Experiment();
  final AiPromptSchemaTemplate template =
      AiPromptSchemaTemplate.builder()
          .templateKey("package-v1")
          .openAiModel("model-test")
          .promptMarkdownContent(
              "Experimento: {{experiment}}\nEtapas anteriores: {{previousStageOutputs}}")
          .schemaJson("{\"type\":\"object\"}")
          .build();
  GeraSalesPageStageExecution failed;

  /** Prepara uma execução sintética sem provedor pago nem alteração de dados comerciais. */
  @BeforeEach
  void setup() throws Exception {
    configure(614L);
  }

  /** Monta seis etapas válidas e um transporte interrompido em identidades independentes. */
  private void configure(long id) throws Exception {
    reset(experiments, executions, templates, publications);
    history.clear();
    experiment.setId(id);
    experiment.setSinglePain("Organizar publicações");
    experiment.setFreeReward("Exemplos reais");
    experiment.setFunnelPromise("Kit personalizado");
    experiment.setPrimaryCta("Comprar kit");
    experiment.setUnitPrice(new BigDecimal("79.00"));
    experiment.setFollowUpActionUrl("https://checkout.example.test/kit");
    when(experiments.findById(id)).thenReturn(Optional.of(experiment));
    when(experiments.findForSalesPageRecovery(id)).thenReturn(Optional.of(experiment));
    when(templates.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            anyString(), anyString()))
        .thenReturn(Optional.of(template));
    when(executions.findTopByExperimentIdOrderByExecutionRequestedAtDesc(id))
        .thenAnswer(
            call ->
                history.isEmpty()
                    ? Optional.empty()
                    : Optional.of(history.get(history.size() - 1)));
    when(executions.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            eq(id), anyString()))
        .thenAnswer(
            call ->
                history.stream()
                    .filter(e -> e.getStageCode().equals(call.getArgument(1)))
                    .reduce((a, b) -> b));
    when(executions.save(any()))
        .thenAnswer(
            call -> {
              GeraSalesPageStageExecution row = call.getArgument(0);
              row.setExperiment(experiment);
              if (!history.contains(row)) history.add(row);
              return row;
            });
    when(executions.findTopByIdJobOrderByExecutionRequestedAtDesc(anyString()))
        .thenAnswer(
            call ->
                history.stream().filter(e -> e.getIdJob().equals(call.getArgument(0))).findFirst());
    when(executions.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            anyString(), anyString()))
        .thenAnswer(
            call ->
                history.stream()
                    .filter(
                        e ->
                            e.getStageCode().equals(call.getArgument(0))
                                && e.getStatus().equals(call.getArgument(1)))
                    .toList());
    for (String code : GeraSalesPageStageCode.orderedCodes()) {
      var row =
          GeraSalesPageStageExecution.builder()
              .idJob(UUID.randomUUID().toString())
              .experimentId(id)
              .experiment(experiment)
              .stageCode(code)
              .status("CONCLUIDO")
              .executionRequestedAt(Instant.now())
              .costUsd(new BigDecimal("0.12"))
              .modelResponse("{\"approved\":true,\"content\":\"approved " + code + "\"}")
              .build();
      history.add(row);
    }
    failed = history.get(6);
    failed.setStatus("INICIADO");
    failed.setModelResponse(null);
    failed.setCostUsd(null);
    var pending = service.pending(failed.getStageCode()).get(0);
    failed.setPrompt(
        template
            .getPromptMarkdownContent()
            .replace("{{experiment}}", mapper.writeValueAsString(pending.experiment()))
            .replace(
                "{{previousStageOutputs}}",
                mapper.writeValueAsString(pending.previousStageOutputs())));
    failed.setPromptMarkdownContent(template.getPromptMarkdownContent());
    failed.setPromptTemplateKey(template.getTemplateKey());
    failed.setSchemaJson(template.getSchemaJson());
    failed.setOpenAiModel(template.getOpenAiModel());
    failed.setStatus("FALHA");
    failed.setErrorMessage("recvAddress(..) failed: Connection reset by peer");
    failed.setErrorDetail(
        "org.springframework.web.reactive.function.client.WebClientRequestException: Connection reset by peer");
  }

  /**
   * Executa HTTP, serviço, fila e retorno, sem repetir predecessores nem o consumo já registrado.
   */
  @ParameterizedTest
  @ValueSource(longs = {614, 9307})
  void resumesOnlyFailedStageAndKeepsHistory(long id) throws Exception {
    configure(id);
    var mvc =
        MockMvcBuilders.standaloneSetup(new GeraSalesPageController(service, publications)).build();
    String path = "/api/experiments/" + id + "/gerasalespage/v1/stage-recovery";
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true));
    mvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("failedJobId", failed.getIdJob()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INICIADO"));
    var retry = history.get(7);
    assertThat(service.retry(id, failed.getIdJob()).idJob()).isEqualTo(retry.getIdJob());
    assertThat(history).hasSize(8);
    assertThat(failed.getStatus()).isEqualTo("FALHA");
    assertThat(history.subList(0, 6))
        .allMatch(
            e ->
                "CONCLUIDO".equals(e.getStatus())
                    && e.getCostUsd().compareTo(new BigDecimal("0.12")) == 0);
    var queue = service.pending(retry.getStageCode());
    assertThat(queue).hasSize(1);
    assertThat(queue.get(0).previousStageOutputs()).hasSize(6);
    service.markRunning(retry.getIdJob());
    service.receiveResult(
        retry.getIdJob(),
        new GeraSalesPageResultRequest(
            id,
            retry.getStageCode(),
            "{\"readyForTraffic\":true}",
            "raw-local",
            5,
            3,
            new BigDecimal("0.01"),
            "response-local",
            null,
            null));
    assertThat(retry.getStatus()).isEqualTo("CONCLUIDO");
    verify(publications).snapshotPublication(retry);
    verify(experiments, times(2)).findForSalesPageRecovery(id);
  }

  /** Recusa resultados potencialmente consumidos, reprovação funcional e mudanças no contexto. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "provider",
        "raw",
        "cost",
        "tokens",
        "functional",
        "input",
        "predecessor",
        "model",
        "schema",
        "template",
        "foreign"
      })
  void rejectsUnsafeRecovery(String cause) {
    switch (cause) {
      case "provider" -> failed.setOpenAiJobId("resp-existing");
      case "raw" -> failed.setRawResponse("existing-response");
      case "cost" -> failed.setCostUsd(BigDecimal.ZERO);
      case "tokens" -> failed.setInputTokens(0);
      case "functional" -> failed.setErrorMessage("Quality review reprovou a página");
      case "input" -> experiment.setUnitPrice(new BigDecimal("89.00"));
      case "predecessor" -> history.get(5).setStatus("FALHA");
      case "model" -> template.setOpenAiModel("different-model");
      case "schema" -> template.setSchemaJson("{\"type\":\"string\"}");
      case "template" -> template.setPromptMarkdownContent("Changed template");
      default -> {}
    }
    assertThatThrownBy(
            () ->
                service.retry(
                    experiment.getId(),
                    cause.equals("foreign") ? "another-job" : failed.getIdJob()))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(history).hasSize(7);
    verify(publications, never()).snapshotPublication(any());
  }

  /** Mudança após enfileiramento bloqueia antes da chamada paga pelo executor. */
  @Test
  void blocksChangedInputBeforeQueueDispatch() {
    service.retry(experiment.getId(), failed.getIdJob());
    experiment.setSinglePain("Outra oferta");
    assertThat(service.pending(failed.getStageCode())).isEmpty();
    assertThat(history.get(7).getStatus()).isEqualTo("FALHA");
    assertThat(history.get(7).getErrorMessage()).contains("chamada de IA bloqueada");
  }

  /** Uma nova falha mantém a origem da retomada e não duplica o comando já recebido. */
  @Test
  void failureKeepsRetryOriginAndOriginalCostUnknown() {
    var retry = service.retry(experiment.getId(), failed.getIdJob());
    service.receiveResult(
        retry.idJob(),
        new GeraSalesPageResultRequest(
            experiment.getId(),
            retry.stageCode(),
            null,
            null,
            null,
            null,
            null,
            null,
            "Connection reset by peer",
            "WebClientRequestException: reset"));
    assertThat(history.get(7).getErrorDetail())
        .contains(failed.getIdJob(), "WebClientRequestException");
    assertThat(history.get(7).getCostUsd()).isNull();
    assertThat(service.retry(experiment.getId(), failed.getIdJob()).idJob())
        .isEqualTo(retry.idJob());
    assertThat(history).hasSize(8);
  }

  /** Usa lock SQL real entre transações concorrentes e comprova uma única tentativa criada. */
  @Test
  void concurrentCommandsKeepOneRetry() throws Exception {
    assertThat(
            ExperimentRepository.class
                .getMethod("findForSalesPageRecovery", Long.class)
                .getAnnotation(org.springframework.data.jpa.repository.Lock.class)
                .value())
        .isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    var dataSource = new org.h2.jdbcx.JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:retry_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbc.execute("CREATE TABLE experiment_lock (id BIGINT PRIMARY KEY)");
    jdbc.update("INSERT INTO experiment_lock (id) VALUES (?)", experiment.getId());
    doAnswer(
            call -> {
              jdbc.queryForObject(
                  "SELECT id FROM experiment_lock WHERE id = ? FOR UPDATE",
                  Long.class,
                  experiment.getId());
              return Optional.of(experiment);
            })
        .when(experiments)
        .findForSalesPageRecovery(experiment.getId());
    var transaction =
        new org.springframework.transaction.support.TransactionTemplate(
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<java.util.concurrent.Future<String>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return transaction.execute(
                      status -> service.retry(experiment.getId(), failed.getIdJob()).idJob());
                }));
      start.countDown();
      var jobs = new HashSet<String>();
      for (var future : futures) jobs.add(future.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(jobs).hasSize(1);
      assertThat(history).hasSize(8);
      assertThat(failed.getStatus()).isEqualTo("FALHA");
    } finally {
      jdbc.execute("SHUTDOWN");
    }
  }

  /** Aceita a representação numérica do transporte JSON sem aceitar mudança de valor. */
  @Test
  void comparesSemanticJsonWithoutIgnoringUnknownFields() {
    failed.setPrompt("Experimento: {\"price\":79.00,\"items\":[{\"x\":1}]}\nEtapas anteriores: {}");
    assertThat(
            GeraSalesPageRetryPolicy.sameInputs(
                failed,
                template,
                Map.of("price", 79.0, "items", List.of(Map.of("x", 1.0))),
                Map.of(),
                mapper))
        .isTrue();
    assertThat(
            GeraSalesPageRetryPolicy.sameInputs(
                failed,
                template,
                Map.of("price", 79.0, "items", List.of(Map.of("x", 2.0))),
                Map.of(),
                mapper))
        .isFalse();
    assertThat(
            GeraSalesPageRetryPolicy.sameInputs(
                failed,
                template,
                Map.of("price", 79.0, "items", List.of(Map.of("x", 1)), "new", true),
                Map.of(),
                mapper))
        .isFalse();
  }
}
