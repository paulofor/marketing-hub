package com.marketinghub.businessprocess.independent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.independent.IndependentBusinessProcessExecution;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputOptionResponse;
import com.marketinghub.businessprocess.independent.service.startExecution.StartIndependentBusinessProcessExecutionRequest;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.IndependentBusinessProcessExecutionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar governança, idempotência e auditoria das execuções sem produto. */
class IndependentBusinessProcessExecutionServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-30T14:00:00Z");
  private final IndependentBusinessProcessExecutionRepository executions =
      mock(IndependentBusinessProcessExecutionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository activities =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final FakeHandler handler = new FakeHandler();

  /** Configura persistência simulada suficiente para observar a transação funcional. */
  @BeforeEach
  void setUp() {
    when(executions.saveAndFlush(any(IndependentBusinessProcessExecution.class)))
        .thenAnswer(
            invocation -> {
              IndependentBusinessProcessExecution execution = invocation.getArgument(0);
              execution.setId(91L);
              return execution;
            });
    when(executions.save(any(IndependentBusinessProcessExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Comprova que o contexto Spring seleciona o construtor operacional com todas as portas. */
  @Test
  void startsInsideSpringContext() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(IndependentBusinessProcessExecutionRepository.class, () -> executions);
      context.registerBean(BusinessProcessDefinitionRepository.class, () -> processes);
      context.registerBean(BusinessProcessActivityDefinitionRepository.class, () -> activities);
      context.registerBean(AgentTaskRepository.class, () -> tasks);
      context.registerBean(ObjectMapper.class, () -> mapper);
      context.registerBean(IndependentBusinessProcessExecutionHandler.class, () -> handler);
      context.register(IndependentBusinessProcessExecutionService.class);

      context.refresh();

      assertThat(context.getBean(IndependentBusinessProcessExecutionService.class)).isNotNull();
    }
  }

  /** Cria ciclo e tarefa uma única vez, aplica defaults e devolve progresso persistido. */
  @Test
  void startsCanonicalExecutionWithoutProduct() throws Exception {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    when(processes.findById(52L)).thenReturn(Optional.of(process));
    when(activities.findAllByProcessDefinitionIdOrderByIdAsc(52L))
        .thenReturn(List.of(activity(process)));
    AgentTask task = task(process, "PENDING", null);
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:77"))
        .thenReturn(List.of(task));
    var service = service(List.of(handler));
    UUID requestKey = UUID.fromString("b82df168-e383-4acd-8ca4-ab858b39fd3e");

    var result =
        service.start(
            new StartIndependentBusinessProcessExecutionRequest(
                requestKey,
                52L,
                "Operação",
                mapper.readTree("{\"theme\":\"agenda vazia para manicures\"}")));

    assertThat(result.execution().id()).isEqualTo(91L);
    assertThat(result.execution().sourceReference()).isEqualTo("product-discovery-cycle:77");
    assertThat(result.execution().status()).isEqualTo("PENDING");
    assertThat(result.execution().input().path("country").asText()).isEqualTo("BR");
    assertThat(result.execution().input().path("researchMode").asText())
        .isEqualTo("DISCOVER_MARKETS");
    assertThat(handler.lastInput.get().path("theme").asText())
        .isEqualTo("agenda vazia para manicures");
    assertThat(handler.starts.get()).isEqualTo(1);
    verify(executions).saveAndFlush(any(IndependentBusinessProcessExecution.class));
  }

  /** Reutiliza a solicitação existente e não cria um segundo ciclo nem custo duplicado. */
  @Test
  void reusesExistingRequestKey() {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    IndependentBusinessProcessExecution existing = execution(process);
    when(executions.findByRequestKey(existing.getRequestKey())).thenReturn(Optional.of(existing));
    when(activities.findAllByProcessDefinitionIdOrderByIdAsc(52L))
        .thenReturn(List.of(activity(process)));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(existing.getSourceReference()))
        .thenReturn(List.of(task(process, "COMPLETED", null)));
    var service = service(List.of(handler));

    var result =
        service.start(
            new StartIndependentBusinessProcessExecutionRequest(
                UUID.fromString(existing.getRequestKey()),
                52L,
                "Outra tentativa",
                mapper.createObjectNode().put("theme", "entrada ignorada pela idempotência")));

    assertThat(result.execution().id()).isEqualTo(91L);
    assertThat(result.execution().status()).isEqualTo("COMPLETED");
    assertThat(handler.starts.get()).isZero();
    verify(executions, never()).saveAndFlush(any());
  }

  /** Rejeita campos estranhos, inclusive productId, antes de persistir qualquer execução. */
  @Test
  void rejectsUnknownOrArtificialProductInput() throws Exception {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    when(processes.findById(52L)).thenReturn(Optional.of(process));
    var service = service(List.of(handler));

    assertThatThrownBy(
            () ->
                service.start(
                    new StartIndependentBusinessProcessExecutionRequest(
                        UUID.randomUUID(),
                        52L,
                        "Operação",
                        mapper.readTree("{\"theme\":\"dor real\",\"productId\":99}"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("productId");
    verify(executions, never()).saveAndFlush(any());
    assertThat(handler.starts.get()).isZero();
  }

  /** Rejeita valor fora das opções publicadas antes de criar ciclo ou tarefa. */
  @Test
  void rejectsUnknownSelectOption() throws Exception {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    when(processes.findById(52L)).thenReturn(Optional.of(process));
    var service = service(List.of(handler));

    assertThatThrownBy(
            () ->
                service.start(
                    new StartIndependentBusinessProcessExecutionRequest(
                        UUID.randomUUID(),
                        52L,
                        "Operação",
                        mapper.readTree(
                            "{\"theme\":\"dor real\",\"researchMode\":\"INVENT_MARKET\"}"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("possui uma opção inválida");
    verify(executions, never()).saveAndFlush(any());
    assertThat(handler.starts.get()).isZero();
  }

  /** Bloqueia versão vinculada a produto mesmo que exista adaptador com o mesmo código. */
  @Test
  void rejectsProductScopedProcess() {
    when(processes.findById(52L)).thenReturn(Optional.of(process("PRODUCT", "PUBLISHED")));
    var service = service(List.of(handler));

    assertThatThrownBy(
            () ->
                service.start(
                    new StartIndependentBusinessProcessExecutionRequest(
                        UUID.randomUUID(),
                        52L,
                        "Operação",
                        mapper.createObjectNode().put("theme", "dor real"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exige vínculo com produto");
    verify(executions, never()).saveAndFlush(any());
  }

  /** Expõe processo sem adaptador como indisponível em vez de permitir um disparo incompleto. */
  @Test
  void catalogExplainsMissingAdapter() {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    process.setProcessCode("independent-without-adapter");
    when(processes.findAllByStatusAndExecutionScopeInOrderByNameAscVersionNumberDesc(
            "PUBLISHED", List.of("INDEPENDENT", "PRODUCT_OR_INDEPENDENT")))
        .thenReturn(List.of(process));

    var catalog = service(List.of(handler)).catalog();

    assertThat(catalog)
        .singleElement()
        .satisfies(item -> assertThat(item.executionAvailable()).isFalse());
    assertThat(catalog.getFirst().executionAvailabilityReason()).contains("não implementado");
  }

  /** Consolida bloqueio, causa e ausência de custo sem convertê-los em sucesso comercial. */
  @Test
  void reportsPersistedBlockerAndUnknownCost() {
    BusinessProcessDefinition process = process("INDEPENDENT", "PUBLISHED");
    IndependentBusinessProcessExecution execution = execution(process);
    AgentTask blocked = task(process, "BLOCKED", "Fonte pública recusou a consulta.");
    when(executions.findById(91L)).thenReturn(Optional.of(execution));
    when(activities.findAllByProcessDefinitionIdOrderByIdAsc(52L))
        .thenReturn(List.of(activity(process)));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(execution.getSourceReference()))
        .thenReturn(List.of(blocked));

    var result = service(List.of(handler)).get(91L);

    assertThat(result.execution().status()).isEqualTo("BLOCKED");
    assertThat(result.execution().latestError()).contains("Fonte pública");
    assertThat(result.execution().estimatedCostUsd()).isNull();
    assertThat(result.execution().costCoverage()).isEqualTo("NOT_REPORTED");
  }

  /** Monta o serviço com relógio fixo para evitar timestamps flutuantes. */
  private IndependentBusinessProcessExecutionService service(
      List<IndependentBusinessProcessExecutionHandler> handlers) {
    return new IndependentBusinessProcessExecutionService(
        executions,
        processes,
        activities,
        tasks,
        mapper,
        handlers,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** Cria uma definição mínima coerente com o catálogo publicado. */
  private BusinessProcessDefinition process(String executionScope, String status) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(52L);
    process.setProcessCode("pde-opportunity-discovery");
    process.setName("Descoberta de oportunidades PDE");
    process.setPurpose("Reunir evidências factuais antes de criar produto.");
    process.setOwnerName("Argos");
    process.setTriggerDescription("Pergunta real de mercado.");
    process.setOutcomeDescription("Dossiê factual auditável.");
    process.setVersionNumber(6);
    process.setStatus(status);
    process.setExecutionScope(executionScope);
    return process;
  }

  /** Cria a atividade relacional usada no cálculo de progresso. */
  private BusinessProcessActivityDefinition activity(BusinessProcessDefinition process) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(401L);
    activity.setProcessDefinition(process);
    activity.setActivityId("marketEvidence");
    activity.setName("Reunir evidências factuais de mercado");
    return activity;
  }

  /** Cria a correlação persistida de uma solicitação já materializada. */
  private IndependentBusinessProcessExecution execution(BusinessProcessDefinition process) {
    IndependentBusinessProcessExecution execution = new IndependentBusinessProcessExecution();
    execution.setId(91L);
    execution.setRequestKey("b82df168-e383-4acd-8ca4-ab858b39fd3e");
    execution.setProcessDefinition(process);
    execution.setSourceReference("product-discovery-cycle:77");
    execution.setDisplayName("agenda vazia para manicures");
    execution.setRequestedByName("Operação");
    execution.setInputJson("{\"theme\":\"agenda vazia para manicures\",\"country\":\"BR\"}");
    execution.setCreatedAt(NOW);
    return execution;
  }

  /** Cria uma tentativa BPM com agente, estado e auditoria mínimos. */
  private AgentTask task(BusinessProcessDefinition process, String status, String executionError) {
    Agent agent = Agent.builder().id(7L).agentKey("market-radar").nickname("Argos").build();
    AgentTask task = new AgentTask();
    task.setId(271L);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId("marketEvidence");
    task.setProcessActivityName("Reunir evidências factuais de mercado");
    task.setTitle("Reunir evidências factuais");
    task.setStatus(status);
    task.setExecutionError(executionError);
    task.setCostEstimationStatus("NOT_REPORTED");
    task.setCreatedAt(NOW);
    task.setUpdatedAt(NOW.plusSeconds(30));
    if (!"PENDING".equals(status)) task.setReceivedAt(NOW.plusSeconds(5));
    if ("COMPLETED".equals(status)) task.setDeliveredAt(NOW.plusSeconds(30));
    return task;
  }

  /** Adaptador controlado que comprova quantas entidades técnicas foram criadas. */
  private static final class FakeHandler implements IndependentBusinessProcessExecutionHandler {
    private final AtomicInteger starts = new AtomicInteger();
    private final AtomicReference<JsonNode> lastInput = new AtomicReference<>();

    /** Identifica o processo de teste. */
    @Override
    public String processCode() {
      return "pde-opportunity-discovery";
    }

    /** Declara tema, modo seletivo e país para exercitar obrigatoriedade e defaults. */
    @Override
    public List<IndependentBusinessProcessInputFieldResponse> inputFields() {
      return List.of(
          new IndependentBusinessProcessInputFieldResponse(
              "theme", "Tema", "TEXT", true, 191, null, null),
          new IndependentBusinessProcessInputFieldResponse(
              "researchMode",
              "Modo",
              "SELECT",
              true,
              32,
              "DISCOVER_MARKETS",
              null,
              List.of(
                  new IndependentBusinessProcessInputOptionResponse(
                      "DISCOVER_MARKETS", "Descobrir mercados"),
                  new IndependentBusinessProcessInputOptionResponse(
                      "VALIDATE_MARKET", "Validar mercado"))),
          new IndependentBusinessProcessInputFieldResponse(
              "country", "País", "TEXT", true, 16, "BR", null));
    }

    /** Registra a chamada e simula a referência retornada pelo ciclo real. */
    @Override
    public IndependentBusinessProcessStartedExecution start(JsonNode input) {
      starts.incrementAndGet();
      lastInput.set(input);
      return new IndependentBusinessProcessStartedExecution(
          "product-discovery-cycle:77", input.path("theme").asText());
    }
  }
}
