package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: provar recuperação explícita e idempotente sem reescrever o parecer pago. */
class AtenaPreservedStrategyRecoveryTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Mantém custos e auditoria após callback repetido, sem permitir nova tentativa após outra
   * recusa.
   */
  @ParameterizedTest
  @ValueSource(longs = {97004L, 98004L})
  void reopensOnlyForSameCallbackAndPreservesOriginalUsage(long productId) throws Exception {
    var task = task(productId);
    var repository = mock(AgentTaskRepository.class);
    when(repository.findById(task.getId())).thenReturn(Optional.of(task));
    when(repository.findLockedById(task.getId())).thenReturn(Optional.of(task));
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var service = recoveryService(task, repository, false);
    String prompt = task.getExecutionPrompt();
    String raw = task.getResultJson();
    String proof = task.getEvidenceJson();
    service.updateStatus(task.getId(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    assertThat(task.getExecutionError()).startsWith("AUTO_RETRY_ONCE|");
    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "experiment-strategist",
                    task.getId(),
                    new CompleteAgentTaskRequest(raw + " ", proof)))
        .hasMessageContaining("análise e sua auditoria originais");
    var callback = new CompleteAgentTaskRequest(raw, proof);
    service.completeClaimedProcessTask("experiment-strategist", task.getId(), callback);
    service.completeClaimedProcessTask("experiment-strategist", task.getId(), callback);
    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    assertThat(task.getExecutionPrompt()).isEqualTo(prompt);
    assertThat(task.getResultJson()).isEqualTo(raw);
    assertThat(task.getEvidenceJson()).isEqualTo(proof);
    assertThat(task.getInputTokens()).isEqualTo(12345L);
    assertThat(task.getCachedInputTokens()).isEqualTo(10000L);
    assertThat(task.getOutputTokens()).isEqualTo(111L);
    assertThat(task.getEstimatedCostUsd()).isEqualByComparingTo("2.689692");
    task.setStatus("IN_PROGRESS");
    task.setExecutionError(
        "AUTO_RETRY_ONCE|Atena exigiu comparação fora do protocolo da candidata");
    service.failClaimedProcessTask(
        "experiment-strategist",
        task.getId(),
        new FailAgentTaskRequest(
            "A análise paga foi preservada, mas ainda não atende aos critérios desta candidata."));
    service.failClaimedProcessTask(
        "experiment-strategist",
        task.getId(),
        new FailAgentTaskRequest(
            "A análise paga foi preservada, mas ainda não atende aos critérios desta candidata."));
    assertThat(task.getResultJson()).isEqualTo(raw);
    assertThat(task.getEvidenceJson()).isEqualTo(proof);
    assertThat(task.getEstimatedCostUsd()).isEqualByComparingTo("2.689692");
    assertThatThrownBy(
            () ->
                service.updateStatus(task.getId(), new UpdateAgentTaskStatusRequest("IN_PROGRESS")))
        .hasMessageContaining("não pode ser reaproveitada");
    assertThat(task.getStatus()).isEqualTo("BLOCKED");
  }

  /** Recusa mudança de contexto, obrigação real, saída parcial e tentativa substituída. */
  @Test
  void rejectsRealObligationMissingAuditAndReplacedAttempt() throws Exception {
    var task = task(97004L);
    assertThat(AtenaPreservedStrategyRecovery.available(task, json)).isTrue();
    String raw = task.getResultJson();
    task.setResultJson(
        raw.replace("Nenhuma condição REFERENCE/REDUCED", "Exigir REFERENCE/REDUCED"));
    assertThat(AtenaPreservedStrategyRecovery.available(task, json)).isFalse();
    task.setResultJson(raw);
    String prompt = task.getExecutionActivityPrompt();
    task.setExecutionActivityPrompt(null);
    assertThat(AtenaPreservedStrategyRecovery.available(task, json)).isFalse();
    task.setExecutionActivityPrompt(prompt);
    var repository = mock(AgentTaskRepository.class);
    when(repository.findById(task.getId())).thenReturn(Optional.of(task));
    when(repository.existsNewerAttemptForCallback(any(), any(), any(), any())).thenReturn(true);
    var service =
        new AgentTaskService(
            repository,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            json,
            Clock.systemUTC());
    assertThatThrownBy(
            () ->
                service.updateStatus(task.getId(), new UpdateAgentTaskStatusRequest("IN_PROGRESS")))
        .hasMessageContaining("não pode ser reaproveitada");
    verify(repository, never()).save(any());
  }

  /** Confere identidade e requisitos atuais antes de oferecer o replay pela fila normal. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "productId",
        "cycleId",
        "experimentId",
        "productVersion",
        "agentValidationExecution"
      })
  void rejectsCurrentContextDrift(String field) throws Exception {
    var task = task(97004L);
    var original = original(task);
    var context = json.readTree(original.path("processContextJson").asText());
    var target = json.treeToValue(original.path("taskTarget"), AgentTaskTargetResponse.class);
    assertThatCode(
            () ->
                AtenaPreservedStrategyRecovery.requireCurrentContext(
                    task, target, context.toString(), json))
        .doesNotThrowAnyException();
    ((com.fasterxml.jackson.databind.node.ObjectNode) context.path("learningSalesCycle"))
        .put(field, "outro-contexto");
    assertThatThrownBy(
            () ->
                AtenaPreservedStrategyRecovery.requireCurrentContext(
                    task, target, context.toString(), json))
        .hasMessageContaining("contexto mudou");
  }

  /** Impede mudar para andamento quando a fonte atual já demonstra incompatibilidade. */
  @Test
  void refusesResumeBeforeChangingStatusWhenCurrentVersionChanged() throws Exception {
    var task = task(97004L);
    var repository = mock(AgentTaskRepository.class);
    when(repository.findById(task.getId())).thenReturn(Optional.of(task));
    var service = recoveryService(task, repository, true);
    assertThatThrownBy(
            () ->
                service.updateStatus(task.getId(), new UpdateAgentTaskStatusRequest("IN_PROGRESS")))
        .hasMessageContaining("contexto mudou");
    assertThat(task.getStatus()).isEqualTo("BLOCKED");
    assertThat(task.getExecutionError()).doesNotStartWith("AUTO_RETRY_ONCE|");
    verify(repository, never()).save(any());
  }

  /** Separa o status operacional da própria tarefa das fontes estratégicas imutáveis. */
  @Test
  void preservesModelingAuthorityAcrossOwnStatusTransition() throws Exception {
    var task = task(97004L);
    var original = (com.fasterxml.jackson.databind.node.ObjectNode) original(task);
    var before =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(original.path("processContextJson").asText());
    before
        .putObject("referenceModeling")
        .put("contractVersion", "MARKET_REFERENCE_MODELING_V1")
        .putObject("strategicDecision")
        .put("taskId", task.getId())
        .put("status", "IN_PROGRESS");
    original.put("processContextJson", before.toString());
    task.setExecutionActivityPrompt("Contexto da tarefa:\n" + original);
    var current = before.deepCopy();
    current.withObject("/referenceModeling/strategicDecision").put("status", "BLOCKED");
    var target = json.treeToValue(original.path("taskTarget"), AgentTaskTargetResponse.class);
    assertThatCode(
            () ->
                AtenaPreservedStrategyRecovery.requireCurrentContext(
                    task, target, current.toString(), json))
        .doesNotThrowAnyException();
    current.withObject("/referenceModeling").put("contractVersion", "OUTRA_FONTE");
    assertThatThrownBy(
            () ->
                AtenaPreservedStrategyRecovery.requireCurrentContext(
                    task, target, current.toString(), json))
        .hasMessageContaining("contexto mudou");
  }

  /**
   * Revalida preparação de recuperação com auditoria real completa, sem comando ou persistência.
   */
  @Test
  void validatesCompletePreservedAuditWhenProvided() throws Exception {
    String auditFile = System.getProperty("atena.replay.audit");
    org.junit.jupiter.api.Assumptions.assumeTrue(
        auditFile != null, "Exportação integral da auditoria não fornecida.");
    var row = json.readTree(Files.readString(Path.of(auditFile)));
    var task = task(row.path("id").asLong());
    task.setId(row.path("id").asLong());
    task.getProcessDefinition().setId(row.path("processDefinitionId").asLong());
    task.getProcessDefinition().setVersionNumber(row.path("processVersionNumber").asInt());
    task.setSourceReference(row.path("sourceReference").asText());
    task.setExecutionActivityPrompt(row.path("executionActivityPrompt").asText());
    task.setExecutionPrompt(row.path("executionPrompt").asText());
    task.setExecutionAgentPrompt(row.path("executionAgentPrompt").asText());
    task.setResultJson(row.path("resultJson").asText());
    task.setEvidenceJson(row.path("evidenceJson").asText());
    task.setExecutionError(row.path("executionError").asText());
    task.setInputTokens(row.path("inputTokens").asLong());
    task.setCachedInputTokens(row.path("cachedInputTokens").asLong());
    task.setOutputTokens(row.path("outputTokens").asLong());
    task.setEstimatedCostUsd(row.path("estimatedCostUsd").decimalValue());
    String before = task.getResultJson() + task.getEvidenceJson() + task.getExecutionPrompt();
    assertThat(AtenaPreservedStrategyRecovery.available(task, json)).isTrue();
    var original = original(task);
    var target = json.treeToValue(original.path("taskTarget"), AgentTaskTargetResponse.class);
    assertThatCode(
            () ->
                AtenaPreservedStrategyRecovery.requireCurrentContext(
                    task, target, original.path("processContextJson").asText(), json))
        .doesNotThrowAnyException();
    assertThat(task.getResultJson() + task.getEvidenceJson() + task.getExecutionPrompt())
        .isEqualTo(before);
    assertThat(task.getStatus()).isEqualTo("BLOCKED");
    assertThat(task.getInputTokens()).isEqualTo(row.path("inputTokens").asLong());
    assertThat(task.getEstimatedCostUsd())
        .isEqualByComparingTo(row.path("estimatedCostUsd").decimalValue());
  }

  /** Lê o envelope original fixado no prompt sem reconstruir ou atualizar o contexto. */
  private com.fasterxml.jackson.databind.JsonNode original(AgentTask task) throws Exception {
    return json.readTree(
        task.getExecutionActivityPrompt().split("Contexto da tarefa:", 2)[1].stripLeading());
  }

  /**
   * Usa o serviço real com fontes atuais controladas para validar a retomada antes da persistência.
   */
  private AgentTaskService recoveryService(
      AgentTask task, AgentTaskRepository repository, boolean changed) throws Exception {
    var original = original(task);
    var current = json.readTree(original.path("processContextJson").asText());
    if (changed)
      ((com.fasterxml.jackson.databind.node.ObjectNode) current.path("learningSalesCycle"))
          .put("productVersion", "outra-versao");
    var targets = mock(AgentTaskTargetContextProvider.class);
    when(targets.resolve(task.getSourceReference(), task.getProcessDefinition().getProcessCode()))
        .thenReturn(
            Optional.of(
                json.treeToValue(original.path("taskTarget"), AgentTaskTargetResponse.class)));
    var service =
        new AgentTaskService(
            repository,
            null,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            null,
            null,
            json,
            null,
            Clock.systemUTC(),
            MarketStrategicContextProvider.empty(),
            targets);
    var cycles =
        mock(
            com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleTaskContext
                .class);
    java.util.Map<String, Object> cycle =
        json.convertValue(
            current.path("learningSalesCycle"),
            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
    when(cycles.resolve(task.getSourceReference(), task.getCreatedAt()))
        .thenReturn(Optional.of(cycle));
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "learningCycleTaskContext", cycles);
    return service;
  }

  /** Cria tarefa isolada com direção, escopo, auditoria e custo já persistidos em memória. */
  private AgentTask task(long productId) throws Exception {
    var task = new AgentTask();
    var agent = new Agent();
    agent.setId(4L);
    agent.setAgentKey("experiment-strategist");
    agent.setNickname("Atena");
    var process = new BusinessProcessDefinition();
    process.setId(91026L);
    process.setProcessCode("pde-commercial-plan-offer");
    process.setVersionNumber(13);
    task.setId(productId + 1000);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId("marketStrategy");
    task.setTaskKind("WORK");
    task.setStatus("BLOCKED");
    task.setSourceReference("experiment:" + (productId + 200));
    task.setCreatedAt(Instant.parse("2026-11-06T00:00:00Z"));
    task.setExecutionMode("MODEL");
    task.setExecutionModelCode("fixture-no-model");
    task.setExecutionReasoningEffort("max");
    task.setExecutionAgentPrompt("Agente sintético.");
    var context = json.createObjectNode();
    var cycle = context.putObject("learningSalesCycle");
    cycle
        .put("productId", productId)
        .put("cycleId", productId + 100)
        .put("experimentId", productId + 200)
        .put("productVersion", "fixture-v4");
    cycle
        .putObject("agentValidationExecution")
        .put("scopeContractVersion", "PDE_VALIDATION_SCOPE_V1")
        .put("productId", productId)
        .put("cycleId", productId + 100)
        .put("experimentId", productId + 200)
        .put("prototypeVersion", "fixture-v4")
        .put("inputComparisonApplicable", false);
    var original =
        json.createObjectNode()
            .put("taskId", task.getId())
            .put("sourceReference", task.getSourceReference())
            .put("processVersion", 13);
    original
        .putObject("taskTarget")
        .put("productId", productId)
        .put("experimentId", productId + 200);
    original.put("processContextJson", context.toString());
    task.setExecutionActivityPrompt("# Prompt original\nContexto da tarefa:\n" + original);
    task.setExecutionPrompt(
        task.getExecutionAgentPrompt() + "\n\n" + task.getExecutionActivityPrompt());
    task.setResultJson(
        "{\"activity\":\"marketStrategy\",\"decision\":\"APPROVE\",\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V4\",\"status\":\"READY_FOR_AGENT_VALIDATION\",\"agentValidationPlan\":{\"continueCriteria\":\"Nenhuma"
            + " condição REFERENCE/REDUCED\"}}}");
    task.setEvidenceJson(
        json.writeValueAsString(
            java.util.Map.of("agent", "Atena", "sourceReference", task.getSourceReference())));
    task.setExecutionError(
        "Atena não concluiu a estratégia: Atena exigiu comparação fora do protocolo da candidata;"
            + " alinhe a hipótese à base disponível.");
    task.setInputTokens(12345L);
    task.setCachedInputTokens(10000L);
    task.setOutputTokens(111L);
    task.setEstimatedCostUsd(new BigDecimal("2.689692"));
    return task;
  }
}
