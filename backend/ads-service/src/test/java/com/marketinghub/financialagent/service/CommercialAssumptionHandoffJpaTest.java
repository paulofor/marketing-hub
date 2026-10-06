package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.experimentstrategist.controller.ExperimentStrategistController;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistContextService;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.CommercialAssumptionsProposed;
import com.marketinghub.financialagent.controller.FinancialAgentController;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Responsabilidade: provar persistência, recuperação e isolamento da passagem entre agentes. */
@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({
  FinancialAgentService.class,
  CommercialAssumptionCoordinator.class,
  ExperimentStrategistExecutionService.class,
  CommercialAssumptionHandoffJpaTest.JsonConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CommercialAssumptionHandoffJpaTest {
  @Autowired private EntityManager em;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private FinancialAgentExecutionRepository financial;
  @Autowired private FinancialAgentService plutus;
  @Autowired private ExperimentStrategistExecutionService atena;

  @Autowired
  private com.marketinghub.repository.jpa.experimentstrategist
          .ExperimentStrategistExecutionRepository
      proposalRepository;

  @MockBean private CommercialPlanService plans;
  @MockBean private CommercialPlanVersionService versions;
  @MockBean private AgentTaskService tasks;
  @MockBean private StudioCostLedgerService costs;
  @MockBean private OpenAiPricingService pricing;
  @MockBean private ExperimentStrategistContextService contexts;
  private final AtomicLong taskCount = new AtomicLong();

  /**
   * Recupera o evento perdido pela conciliação automática, sem novo comando ou pesquisa de Atena.
   */
  @Test
  void automaticRecoveryPersistsOneHandoffForIndependentProducts() {
    for (long productId : new long[] {81011L, 82011L}) {
      var source = seed("QA recuperação automática " + productId);
      var planRepository =
          mock(com.marketinghub.repository.jpa.planning.CommercialPlanRepository.class);
      var experiments = mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class);
      var product = new com.marketinghub.product.Product();
      product.setId(productId);
      product.setAutomaticExecutionEnabled(true);
      var experiment = new com.marketinghub.experiment.Experiment();
      experiment.setId(productId + 86);
      experiment.setProduct(product);
      experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
      when(experiments.findById(experiment.getId())).thenReturn(java.util.Optional.of(experiment));
      when(planRepository.findByExperimentReference(experiment.getId()))
          .thenReturn(java.util.List.of(source.getCommercialPlan()));
      var recovery =
          new com.marketinghub.businessprocess.automation.v1.service.ProcessRunAssumptionRecovery(
              planRepository, experiments, proposalRepository, financial, versions, plutus);
      var run = new com.marketinghub.businessprocess.automation.v1.ProcessRun();
      run.setProductId(productId);
      run.setSourceReference("experiment:" + experiment.getId());
      run.setCreatedAt(source.getCreatedAt().minusSeconds(1));
      run.setStatus("WAITING_INPUT");
      run.setCurrentActivityId("communicationContract");
      var tx = new TransactionTemplate(transactions);
      var result = tx.execute(ignored -> recovery.recover(run, "pde-communication-sales-journey"));
      assertThat(result).isPresent();
      assertThat(queue(source)).hasSize(1);
      var executionId = queue(source).getFirst().getId();
      assertThat(result.orElseThrow().financialExecutionId()).isEqualTo(executionId);
      assertThat(
              financial
                  .findFirstByCommercialPlanIdAndCommercialPlanVersionAndAuthorityModeOrderByCreatedAtDescIdDesc(
                      source.getCommercialPlan().getId(), 3, "COMMERCIAL_ASSUMPTIONS_VALIDATION"))
          .isPresent();
      assertThat(
              financial
                  .findFirstByCommercialPlanIdAndCommercialPlanVersionAndAuthorityModeOrderByCreatedAtDescIdDesc(
                      source.getCommercialPlan().getId(), 2, "COMMERCIAL_ASSUMPTIONS_VALIDATION"))
          .isEmpty();
      var repeated =
          tx.execute(ignored -> recovery.recover(run, "pde-communication-sales-journey"));
      assertThat(repeated).isEmpty();
      tx.executeWithoutResult(
          ignored -> {
            var rejected = financial.findById(executionId).orElseThrow();
            rejected.setStatus(
                com.marketinghub.financialagent.FinancialAgentExecutionStatus.COMPLETED);
            rejected.setReconciliationJson("{\"decision\":\"REJECT\"}");
          });
      var rejectedRecovery =
          tx.execute(ignored -> recovery.recover(run, "pde-communication-sales-journey"));
      assertThat(rejectedRecovery).isEmpty();
      assertThat(queue(source)).hasSize(1);
      var preserved = proposalRepository.findById(source.getId()).orElseThrow();
      assertThat(preserved.getRecommendationJson()).isEqualTo(source.getRecommendationJson());
      assertThat(preserved.getEstimatedCost()).isEqualByComparingTo("0.03124440");
    }
    assertThat(taskCount.get()).isEqualTo(2);
  }

  /** Percorre HTTP, evento e JPA e exporta respostas UTF-8 reais para a matriz da tela. */
  @Test
  void canonicalHttpReplayPreservesPaidSource() throws Exception {
    var source = seed("QA ponta a ponta HTTP");
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new ExperimentStrategistController(contexts, null, atena),
                new FinancialAgentController(plutus, null))
            .setMessageConverters(
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                    new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(
                            com.fasterxml.jackson.databind.SerializationFeature
                                .WRITE_DATES_AS_TIMESTAMPS)))
            .build();
    var path =
        "/api/experiment-strategist/v1/commercial-plans/"
            + source.getCommercialPlan().getId()
            + "/commercial-assumptions";
    com.fasterxml.jackson.databind.JsonNode proposalResponse = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      var response = mvc.perform(MockMvcRequestBuilders.post(path)).andReturn().getResponse();
      assertThat(response.getStatus()).isEqualTo(200);
      proposalResponse =
          new ObjectMapper()
              .readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
      assertThat(proposalResponse.get("id").asLong()).isEqualTo(source.getId());
    }
    var response =
        mvc.perform(
                MockMvcRequestBuilders.get(
                    "/api/financial-agent/v1/commercial-plans/"
                        + source.getCommercialPlan().getId()
                        + "/commercial-assumptions"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    var results =
        new ObjectMapper()
            .readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).get("status").asText()).isEqualTo("PENDING");
    assertThat(taskCount.get()).isEqualTo(1);
    java.nio.file.Files.writeString(
        java.nio.file.Path.of("target/handoff-http-fixture.json"),
        new ObjectMapper()
            .writeValueAsString(
                java.util.Map.of(
                    "planId",
                    source.getCommercialPlan().getId(),
                    "proposal",
                    proposalResponse,
                    "financial",
                    results)));
  }

  /** Confirma uma fila real por proposta em dois planos independentes após o commit. */
  @Test
  void enqueuesDurablyAfterCommit() {
    for (int index = 0; index < 2; index++) {
      var source = seed("QA da passagem " + index);
      new TransactionTemplate(transactions).executeWithoutResult(status -> publish(source));
      assertThat(queue(source)).hasSize(1);
      assertThat(queue(source).getFirst().getStrategistExecutionId()).isEqualTo(source.getId());
    }
    assertThat(taskCount.get()).isEqualTo(2);
  }

  /** Não libera Plutus quando o evento da proposta sofre rollback. */
  @Test
  void doesNotEnqueueRolledBackEvent() {
    var source = seed("QA de rollback");
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              publish(source);
              status.setRollbackOnly();
            });
    assertThat(queue(source)).isEmpty();
    assertThat(taskCount.get()).isZero();
  }

  /** Recupera falha posterior sem apagar resposta, custo ou recriar Atena. */
  @Test
  void replaysPaidProposalAfterDownstreamFailure() {
    var source = seed("QA de recuperação");
    doThrow(new IllegalStateException("Falha simulada da fila")).when(tasks).createByHuman(any());
    new TransactionTemplate(transactions).executeWithoutResult(status -> publish(source));
    assertThat(queue(source)).isEmpty();
    var preserved = em.find(ExperimentStrategistExecution.class, source.getId());
    assertThat(preserved.getStatus()).isEqualTo(ExperimentStrategistExecutionStatus.COMPLETED);
    assertThat(preserved.getRawModelResponse()).isEqualTo(source.getRawModelResponse());
    assertThat(preserved.getEstimatedCost()).isEqualByComparingTo("0.03124440");
    stubTaskCreation();
    assertThat(atena.startCommercialAssumptions(source.getCommercialPlan().getId()).id())
        .isEqualTo(source.getId());
    assertThat(queue(source)).hasSize(1);
    assertThat(taskCount.get()).isEqualTo(1);
  }

  /** Serializa retomadas da mesma proposta sem inferências ou tarefas duplicadas. */
  @Test
  void concurrentReplaysCreateOneValidation() throws Exception {
    var source = seed("QA concorrente");
    try (var pool = Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<java.util.concurrent.Future<Long>>();
      for (int index = 0; index < 8; index++)
        futures.add(
            pool.submit(
                () -> atena.startCommercialAssumptions(source.getCommercialPlan().getId()).id()));
      for (var future : futures)
        assertThat(future.get(15, TimeUnit.SECONDS)).isEqualTo(source.getId());
    }
    assertThat(queue(source)).hasSize(1);
    assertThat(taskCount.get()).isEqualTo(1);
  }

  /** Recusa outro plano, texto substituído, proposta incompleta e versão antiga. */
  @Test
  void rejectsIncompatibleSourceWithoutCreatingTasks() {
    var source = seed("QA de identidade");
    var other = seed("QA independente");
    assertThatThrownBy(
            () ->
                plutus.startAssumptionValidation(
                    other.getCommercialPlan().getId(),
                    source.getId(),
                    source.getRecommendationJson()))
        .hasMessageContaining("versão vigentes");
    assertThatThrownBy(
            () ->
                plutus.startAssumptionValidation(
                    source.getCommercialPlan().getId(), source.getId(), "{}"))
        .hasMessageContaining("versão vigentes");
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status ->
                em.find(ExperimentStrategistExecution.class, source.getId())
                    .setStatus(ExperimentStrategistExecutionStatus.RUNNING));
    assertThatThrownBy(
            () ->
                plutus.startAssumptionValidation(
                    source.getCommercialPlan().getId(),
                    source.getId(),
                    source.getRecommendationJson()))
        .hasMessageContaining("versão vigentes");
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status ->
                em.find(ExperimentStrategistExecution.class, source.getId())
                    .setStatus(ExperimentStrategistExecutionStatus.COMPLETED));
    when(versions.current(source.getCommercialPlan().getId()))
        .thenReturn(
            new CommercialPlanVersionDto(
                601L,
                source.getCommercialPlan().getId(),
                4,
                "{}",
                "QA",
                "Condições alteradas",
                Instant.now().plusSeconds(60)));
    assertThatThrownBy(
            () ->
                plutus.startAssumptionValidation(
                    source.getCommercialPlan().getId(),
                    source.getId(),
                    source.getRecommendationJson()))
        .hasMessageContaining("versão vigentes");
    assertThat(queue(source)).isEmpty();
    assertThat(queue(other)).isEmpty();
    assertThat(taskCount.get()).isZero();
  }

  /** Cria entrada sintética persistida com contexto próprio e nenhum provedor externo. */
  private ExperimentStrategistExecution seed(String label) {
    var tx = new TransactionTemplate(transactions);
    var plan = new CommercialPlan();
    plan.setName(label);
    plan.setNextAction("QA isolada, sem mídia ou consumo externo");
    tx.executeWithoutResult(status -> em.persist(plan));
    when(plans.getPlan(plan.getId())).thenReturn(plan);
    when(plans.getPlanForUpdate(plan.getId()))
        .thenAnswer(
            invocation ->
                em.find(CommercialPlan.class, plan.getId(), LockModeType.PESSIMISTIC_WRITE));
    when(versions.current(plan.getId()))
        .thenReturn(
            new CommercialPlanVersionDto(
                501L,
                plan.getId(),
                3,
                "{}",
                "QA",
                "Contrato isolado",
                Instant.now().minusSeconds(60)));
    var source = new ExperimentStrategistExecution();
    source.setCommercialPlan(plan);
    source.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    source.setAuthorityMode("COMMERCIAL_ASSUMPTIONS_PROPOSAL");
    source.setResearchQuestion("QA isolada");
    source.setEvidenceSnapshot("{}");
    source.setRecommendationJson("{\"proposedAssumptions\":{\"offerPriceBrl\":87}}");
    source.setRawModelResponse(source.getRecommendationJson());
    source.setEstimatedCost(new BigDecimal("0.03124440"));
    tx.executeWithoutResult(status -> em.persist(source));
    stubTaskCreation();
    return source;
  }

  /** Simula apenas a criação da tarefa; persistência e serviços de passagem são reais. */
  private void stubTaskCreation() {
    doAnswer(
            invocation -> {
              var task = mock(AgentTaskResponse.class);
              when(task.id()).thenReturn(800L + taskCount.incrementAndGet());
              return task;
            })
        .when(tasks)
        .createByHuman(any());
  }

  /** Publica exatamente a resposta paga persistida. */
  private void publish(ExperimentStrategistExecution source) {
    events.publishEvent(
        new CommercialAssumptionsProposed(
            source.getCommercialPlan().getId(), source.getId(), source.getRecommendationJson()));
  }

  /** Consulta a fila por contexto em outra transação para comprovar sua durabilidade. */
  private java.util.List<com.marketinghub.financialagent.FinancialAgentExecution> queue(
      ExperimentStrategistExecution source) {
    return financial.findByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDesc(
        source.getCommercialPlan().getId(), "COMMERCIAL_ASSUMPTIONS_VALIDATION");
  }

  /** Responsabilidade: fornecer o serializador produtivo à fixture. */
  @TestConfiguration
  static class JsonConfig {
    /** Mantém os instantes do snapshot real. */
    @Bean
    ObjectMapper mapper() {
      return new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
  }
}
