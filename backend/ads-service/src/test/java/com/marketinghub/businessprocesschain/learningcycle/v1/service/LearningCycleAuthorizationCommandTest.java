package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: comprovar que a tela oferece uma autorização financeira atômica e segura. */
class LearningCycleAuthorizationCommandTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleEvidence evidence = mock(LearningCycleEvidence.class);
  private final LearningCycleVideoEvidence videoEvidence = mock(LearningCycleVideoEvidence.class);
  private final LearningCycleVideoBudget videoBudget = mock(LearningCycleVideoBudget.class);
  private final BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
  private final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          experiments,
          chains,
          processes,
          null,
          new LearningCycleJson(new ObjectMapper()),
          null,
          evidence,
          videoEvidence,
          null,
          null);
  private LearningSalesCycle cycle;
  private Experiment experiment;

  /** Monta o ciclo realista de autorização com sua homologação vigente. */
  @BeforeEach
  void setUp() {
    Instant now = Instant.parse("2026-09-15T04:00:00Z");
    cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setProcessDefinitionId(75L);
    cycle.setStage("AUTHORIZATION");
    cycle.setStatus("OPEN");
    cycle.setProductVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    cycle.setBudgetLimitBrl(new BigDecimal("100.00"));
    cycle.setRevision(13L);
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    cycle.setVersionChangedAt(now.minusSeconds(60));
    cycle.setWindowStart(Instant.now().minusSeconds(3600));
    cycle.setWindowEnd(Instant.now().plusSeconds(86400));

    experiment = Experiment.builder().id(92L).build();
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(75L);
    process.setVersionNumber(6);
    process.setDiagramJson("{\"nodes\":[]}");
    chain.setId(14L);
    chain.setItems(List.of());
    LearningSalesCycleEvent validation = new LearningSalesCycleEvent();
    validation.setId(16L);
    validation.setCycleId(2L);
    validation.setRevision(12L);
    validation.setFromStage("VALIDATION");
    validation.setToStage("AUTHORIZATION");
    validation.setAction("COMPLETE");
    validation.setOperatorName("Marketing Hub");
    validation.setSummary("Homologação concluída.");
    validation.setEvidenceReference("agent-validation-gate:289");
    validation.setEvidenceJson("{\"approvalInstanceId\":289}");
    validation.setRequestJson("{}");
    validation.setRequestKey("00000000-0000-0000-0000-000000000016");
    validation.setCreatedAt(now);

    when(products.findById(4L)).thenReturn(Optional.of(Product.builder().id(4L).build()));
    when(cycles.findByProductIdOrderByIdDesc(4L)).thenReturn(List.of(cycle));
    when(cycles.findByPreviousCycleId(2L)).thenReturn(Optional.empty());
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    when(processes.findById(75L)).thenReturn(Optional.of(process));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    when(events.findByCycleIdOrderByRevisionAsc(2L)).thenReturn(List.of(validation));
    when(evidence.approvals(cycle))
        .thenReturn(List.of(new LearningCycleResponse.ApprovalOption(289L, "Gate #289")));
    ReflectionTestUtils.setField(service, "videoBudget", videoBudget);
    var readiness = mock(LearningCycleCommercialReadiness.class);
    when(readiness.inspect(cycle))
        .thenReturn(
            new com.marketinghub
                .businessprocesschain
                .learningcycle
                .v1
                .service
                .getCycles
                .LearningCycleCommercialPreparation(
                false, "Checkout e versão comercial pendentes", "/experiments/92", List.of()));
    ReflectionTestUtils.setField(service, "commercialReadiness", readiness);
  }

  /** Permite confirmar o teto na própria ação sem exigir edição duplicada do experimento. */
  @Test
  void exposesAuthorizationCommandWhenExperimentBudgetIsMissing() {
    LearningCycleResponse.CommandOption command = authorizationCommand();

    assertThat(command.available()).isTrue();
    assertThat(command.reason()).contains("backend conferirá");
    assertThat(service.list(4L).getFirst().commercialPreparation().readyForReview()).isFalse();
  }

  /** Aplica a política publicada às novas decisões sem modificar o contrato histórico do ciclo. */
  @Test
  void publishedChangePolicyGovernsFutureChangesWithoutMigratingOldCycle() {
    assertThat(service.list(4L).getFirst().commands())
        .extracting(LearningCycleResponse.CommandOption::action)
        .contains("REWORK");
    var current = new BusinessProcessDefinition();
    current.setDiagramJson("{\"experimentChangePolicy\":\"CHANGE_PER_CYCLE_V1\"}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            LearningCycleRules.PROCESS_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(current));
    assertThat(service.list(4L).getFirst().commands())
        .extracting(LearningCycleResponse.CommandOption::action)
        .contains("ADJUST")
        .doesNotContain("REWORK", "SCALE", "AUTHORIZE_SCALE");
    assertThat(cycle.getProcessDefinitionId()).isEqualTo(75L);
    assertThat(cycle.getRevision()).isEqualTo(13L);
    org.mockito.Mockito.verify(cycles, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.any());
  }

  /** Libera o comando quando o teto operacional coincide exatamente com a decisão do ciclo. */
  @Test
  void exposesAuthorizationCommandWhenExperimentBudgetMatchesCycle() {
    experiment.setMediaSpendLimit(new BigDecimal("100.00"));

    LearningCycleResponse.CommandOption command = authorizationCommand();

    assertThat(command.available()).isTrue();
  }

  /** Janela expirada é bloqueada antes do formulário, sem pedir uma confirmação impossível. */
  @Test
  void expiredWindowDisablesAuthorizationWithoutDiscardingEvidence() {
    cycle.setWindowEnd(Instant.now().minusSeconds(1));
    var response = service.list(4L).getFirst();
    assertThat(authorizationCommand().available()).isFalse();
    assertThat(authorizationCommand().reason()).contains("janela terminou");
    assertThat(response.events()).hasSize(1);
    assertThat(response.authorizationReview().evidenceReference())
        .contains("learning_sales_cycle_event_v1:16");
    assertThat(cycle.getStage()).isEqualTo("AUTHORIZATION");
  }

  /** A síntese usa os dados persistidos e deixa a decisão humana no comando existente. */
  @Test
  void providesAuditableReviewWithoutRedundantInputOrMutation() {
    var response = service.list(4L).getFirst();
    assertThat(response.authorizationReview().summary())
        .contains("100.00", "#92", cycle.getProductVersion());
    assertThat(response.authorizationReview().explanation()).contains("autorização final");
    org.mockito.Mockito.verify(cycles, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.any());
  }

  /** Teto ausente nunca equivale a zero autorizado nem pede edição duplicada do experimento. */
  @Test
  void missingCycleBudgetDisablesAuthorization() {
    cycle.setBudgetLimitBrl(null);
    assertThat(authorizationCommand().available()).isFalse();
    assertThat(authorizationCommand().reason()).contains("teto financeiro válido");
  }

  /** Mantém a publicação orientada ao processo comercial em vez de abandonar o ciclo no detalhe. */
  @Test
  void directsPublicationToCommercialHomologation() {
    cycle.setStage("PUBLICATION");
    BusinessProcessDefinition commercial = new BusinessProcessDefinition();
    commercial.setId(56L);
    commercial.setProcessCode("pde-commercial-homologation-activation");
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setProcessDefinition(commercial);
    chain.setItems(List.of(item));

    LearningCycleResponse response = service.list(4L).getFirst();

    assertThat(response.workUrl())
        .isEqualTo(
            "/products/4/value-chain-history/processes/56/activities?learningCycleId=2&chainId=14");
  }

  /** Deriva metadados do ciclo e encaminha o aceite ao comando protegido já existente. */
  @Test
  void mapsTwoAmountsToCanonicalCommandWithoutUserMetadata() {
    var spy = org.mockito.Mockito.spy(service);
    when(cycles.findLocked(4L, 2L)).thenReturn(Optional.of(cycle));
    org.mockito.Mockito.doReturn(null)
        .when(spy)
        .command(
            org.mockito.ArgumentMatchers.eq(4L),
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.any());
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .AuthorizeCycleBudgetRequest(
            java.util.UUID.randomUUID(), 13, new BigDecimal("25.00"), new BigDecimal("120.00"));
    spy.authorizeBudget(4L, 2L, request, "Operador administrativo · aceite pela tela");
    var captor =
        org.mockito.ArgumentCaptor.forClass(
            com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                .LearningCycleCommand.class);
    org.mockito.Mockito.verify(spy)
        .command(
            org.mockito.ArgumentMatchers.eq(4L),
            org.mockito.ArgumentMatchers.eq(2L),
            captor.capture());
    var command = captor.getValue();
    assertThat(command.requestKey()).isEqualTo(request.requestKey());
    assertThat(command.expectedRevision()).isEqualTo(13);
    assertThat(command.evidence().path("dailyBudgetBrl").decimalValue()).isEqualByComparingTo("25");
    assertThat(command.evidence().path("budgetLimitBrl").decimalValue())
        .isEqualByComparingTo("120");
    assertThat(command.evidence().path("productVersion").asText())
        .isEqualTo(cycle.getProductVersion());
    assertThat(command.evidence().path("confirmed").asBoolean()).isTrue();
  }

  /** O endpoint aceita dois montantes e rejeita valores incompletos antes do serviço. */
  @Test
  void validatesTwoAmountHttpContract() throws Exception {
    var mocked = org.mockito.Mockito.mock(LearningCycleService.class);
    var http =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.marketinghub.businessprocesschain.learningcycle.v1.controller
                    .LearningCycleController(mocked))
            .build();
    String path =
        "/api/business-process-chains/learning-cycles/v1/products/400/200/budget-authorization";
    String body =
        "{\"requestKey\":\"00000000-0000-0000-0000-000000000001\",\"expectedRevision\":3,\"dailyBudgetBrl\":25,\"budgetLimitBrl\":100}";
    http.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType("application/json")
                .content(body))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    for (String invalid : List.of("null", "0", "-1", "1.001")) {
      http.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                  .contentType("application/json")
                  .content(body.replace("\"dailyBudgetBrl\":25", "\"dailyBudgetBrl\":" + invalid)))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isBadRequest());
    }
    org.mockito.Mockito.verify(mocked, org.mockito.Mockito.times(1))
        .authorizeBudget(
            org.mockito.ArgumentMatchers.eq(400L),
            org.mockito.ArgumentMatchers.eq(200L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("Operador administrativo · aceite pela tela"));
  }

  /** Integra os dois montantes ao experimento sem contornar o gate de homologação vigente. */
  @Test
  void appliesBothAmountsAndRejectsRevokedApproval() {
    ReflectionTestUtils.setField(
        service, "commercialAuthorization", new LearningCycleCommercialAuthorization(experiments));
    experiment.setPlatform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var data =
        new ObjectMapper()
            .createObjectNode()
            .put("confirmed", true)
            .put("productVersion", cycle.getProductVersion())
            .put("dailyBudgetBrl", new BigDecimal("25.00"))
            .put("budgetLimitBrl", new BigDecimal("120.00"));
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .LearningCycleCommand(
            java.util.UUID.randomUUID(),
            13,
            com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                .LearningCycleCommand.Action.COMPLETE,
            "Operador de teste",
            "Aceite sintético",
            "internal://teste",
            data);
    ReflectionTestUtils.invokeMethod(service, "apply", cycle, experiment, request, Instant.now());
    assertThat(cycle.getStage()).isEqualTo("PUBLICATION");
    assertThat(cycle.getBudgetLimitBrl()).isEqualByComparingTo("120.00");
    assertThat(experiment.getMediaSpendLimit()).isEqualByComparingTo("120.00");
    assertThat(experiment.getDailyBudget()).isEqualByComparingTo("25.00");
    assertThat(experiment.getStatus())
        .isEqualTo(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    cycle.setStage("AUTHORIZATION");
    when(evidence.approvals(cycle)).thenReturn(List.of());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    service, "apply", cycle, experiment, request, Instant.now()))
        .hasMessageContaining("homologação utilizada");
    org.mockito.Mockito.verify(experiments, org.mockito.Mockito.times(1)).save(experiment);
  }

  /** Persiste e repete o aceite com preparação ausente em ciclos distintos sem ativar mídia. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"4,2,92,20,100", "400,200,920,30.25,120"})
  void persistsBudgetBeforePreparation(
      long productId, long cycleId, long experimentId, String daily, String total) {
    var history = events.findByCycleIdOrderByRevisionAsc(2L);
    cycle.setProductId(productId);
    cycle.setId(cycleId);
    cycle.setExperimentId(experimentId);
    experiment.setId(experimentId);
    experiment.setProduct(Product.builder().id(productId).build());
    experiment.setPlatform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    when(products.findLockedById(productId)).thenReturn(Optional.of(experiment.getProduct()));
    when(cycles.findLocked(productId, cycleId)).thenReturn(Optional.of(cycle));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(events.findByCycleIdOrderByRevisionAsc(cycleId)).thenReturn(history);
    ReflectionTestUtils.setField(service, "ledger", mock(LearningCycleBpmLedger.class));
    ReflectionTestUtils.setField(
        service,
        "decisionApproval",
        mock(
            com.marketinghub.businessprocesschain.learningcycle.v1.decision.service
                .LearningCycleDecisionApproval.class));
    ReflectionTestUtils.setField(
        service, "commercialAuthorization", new LearningCycleCommercialAuthorization(experiments));
    var saved = new java.util.ArrayList<LearningSalesCycleEvent>();
    when(events.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              LearningSalesCycleEvent event = invocation.getArgument(0);
              saved.add(event);
              return event;
            });
    when(events.findByCycleIdAndRequestKey(
            org.mockito.ArgumentMatchers.eq(cycleId), org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(
            invocation ->
                saved.stream()
                    .filter(event -> event.getRequestKey().equals(invocation.getArgument(1)))
                    .findFirst());
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .AuthorizeCycleBudgetRequest(
            java.util.UUID.randomUUID(), 13, new BigDecimal(daily), new BigDecimal(total));
    for (int attempt = 0; attempt < 2; attempt++) {
      var result = service.authorizeBudget(productId, cycleId, request, "Operador sintético");
      assertThat(result.stage()).isEqualTo("PUBLICATION");
      assertThat(result.commercialPreparation().readyForReview()).isFalse();
    }
    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst().getFromStage()).isEqualTo("AUTHORIZATION");
    assertThat(saved.getFirst().getToStage()).isEqualTo("PUBLICATION");
    assertThat(saved.getFirst().getCycleId()).isEqualTo(cycleId);
    assertThat(experiment.getDailyBudget()).isEqualByComparingTo(daily);
    assertThat(experiment.getMediaSpendLimit()).isEqualByComparingTo(total);
    assertThat(experiment.getStatus())
        .isEqualTo(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    assertThat(cycle.getRevision()).isEqualTo(14);
    org.mockito.Mockito.verify(experiments).save(experiment);
    var conflicting =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .AuthorizeCycleBudgetRequest(
            request.requestKey(), 13, BigDecimal.ONE, new BigDecimal(total));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.authorizeBudget(productId, cycleId, conflicting, "Operador sintético"))
        .hasMessageContaining("conteúdo diferente");
    assertThat(saved).hasSize(1);
  }

  /**
   * Renova um ciclo ainda planejado, redistribuindo o teto pela janela sem ampliar verba nem
   * liberar mídia, e preserva o replay após a publicação da política de sucessor.
   */
  @Test
  void revalidatesExpiredWindowBeforePublicationWithoutChangingBudget() {
    cycle.setStage("PUBLICATION");
    cycle.setWindowStart(Instant.parse("2026-09-10T03:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2026-09-17T03:00:00Z"));
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setPlatform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    experiment.setDailyBudget(new BigDecimal("20"));
    assertThat(service.list(4L).getFirst().windowRevalidation().available()).isTrue();
    when(products.findLockedById(4L)).thenReturn(Optional.of(experiment.getProduct()));
    when(cycles.findLocked(4L, 2L)).thenReturn(Optional.of(cycle));
    when(events.findByCycleIdAndRequestKey(
            org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());
    ReflectionTestUtils.setField(
        service, "commercialAuthorization", new LearningCycleCommercialAuthorization(experiments));
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .RevalidateCycleWindowRequest(
            java.util.UUID.randomUUID(),
            13,
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(6),
            "Janela venceu durante a preparação, sem mudança comercial.");

    var response = service.revalidateWindow(4L, 2L, request, "Operador sintético");

    assertThat(response.stage()).isEqualTo("PUBLICATION");
    assertThat(response.revision()).isEqualTo(14);
    assertThat(response.budgetLimitBrl()).isEqualByComparingTo("100");
    assertThat(response.windowRevalidation()).isNull();
    assertThat(experiment.getStatus())
        .isEqualTo(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    assertThat(experiment.getDailyBudget()).isEqualByComparingTo("14.28");
    assertThat(experiment.getMediaSpendLimit()).isEqualByComparingTo("100");
    assertThat(experiment.getStartDate()).isEqualTo(java.time.LocalDate.now());
    assertThat(experiment.getEndDate()).isEqualTo(java.time.LocalDate.now().plusDays(6));
    var eventCaptor = org.mockito.ArgumentCaptor.forClass(LearningSalesCycleEvent.class);
    org.mockito.Mockito.verify(events).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getAction()).isEqualTo("REVALIDATE_WINDOW");
    assertThat(eventCaptor.getValue().getEvidenceJson())
        .contains("\"externalSpendAuthorized\":false");
    when(events.findByCycleIdAndRequestKey(2L, request.requestKey().toString()))
        .thenReturn(Optional.of(eventCaptor.getValue()));
    var current = new BusinessProcessDefinition();
    current.setDiagramJson("{\"experimentChangePolicy\":\"CHANGE_PER_CYCLE_V1\"}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            LearningCycleRules.PROCESS_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(current));
    assertThat(service.revalidateWindow(4L, 2L, request, "Operador sintético").revision())
        .isEqualTo(14);
    org.mockito.Mockito.verify(events, org.mockito.Mockito.times(1))
        .saveAndFlush(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(cycles, org.mockito.Mockito.times(1))
        .saveAndFlush(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(experiments, org.mockito.Mockito.times(1))
        .save(org.mockito.ArgumentMatchers.any());
  }

  /** Recusa renovar depois que a liberação externa começou. */
  @Test
  void rejectsWindowRewriteAfterFacebookReleaseStarted() {
    cycle.setStage("PUBLICATION");
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    experiment.setFacebookReleaseRequestedAt(Instant.now());
    when(products.findLockedById(4L)).thenReturn(Optional.of(experiment.getProduct()));
    when(cycles.findLocked(4L, 2L)).thenReturn(Optional.of(cycle));
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .RevalidateCycleWindowRequest(
            java.util.UUID.randomUUID(),
            13,
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(6),
            "Tentativa tardia.");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.revalidateWindow(4L, 2L, request, "Operador sintético"))
        .hasMessageContaining("já iniciou liberação");
  }

  /** A política publicada bloqueia também a renovação direta de um ciclo legado. */
  @Test
  void currentChangePolicyBlocksWindowRewriteInReadAndCommand() {
    cycle.setWindowEnd(Instant.now().minusSeconds(60));
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var current = new BusinessProcessDefinition();
    current.setDiagramJson("{\"experimentChangePolicy\":\"CHANGE_PER_CYCLE_V1\"}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            LearningCycleRules.PROCESS_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(current));
    var response = service.list(4L).getFirst();
    assertThat(response.windowRevalidation().available()).isFalse();
    assertThat(response.windowRevalidation().reason()).contains("novo ciclo e novo experimento");
    assertWindowRejected("novo ciclo e novo experimento");
  }

  /** A referência histórica permanece imutável mesmo sob uma política legada. */
  @Test
  void historicalBaselineDoesNotOfferOrAcceptWindowRewrite() {
    cycle.setBaseline(true);
    cycle.setStage("DECISION");
    cycle.setWindowEnd(Instant.now().minusSeconds(60));
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.INVALIDATED);
    assertThat(service.list(4L).getFirst().windowRevalidation().available()).isFalse();
    assertWindowRejected("referência histórica");
  }

  /**
   * A exposição comprovada impede renovação mesmo se o status administrativo voltar a planejado.
   */
  @Test
  void recordedPublicationPreventsWindowRewriteWithoutReleaseTimestamp() {
    cycle.setWindowEnd(Instant.now().minusSeconds(60));
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    when(evidence.historicalPublication(experiment))
        .thenReturn(
            Optional.of(
                new com.marketinghub
                    .businessprocesschain
                    .learningcycle
                    .v1
                    .service
                    .getCycles
                    .LearningCycleHistoricalPublication(
                    "PRODUCTION_RUN",
                    92L,
                    "experiment_run:902",
                    Instant.now().minusSeconds(120),
                    true,
                    "Publicação sintética comprovada para este contrato.")));
    assertThat(service.list(4L).getFirst().windowRevalidation().available()).isFalse();
    assertWindowRejected("já iniciou liberação");
  }

  /** Mantém a janela futura fora da interface e recusa o comando que tente antecipar renovação. */
  @Test
  void futureWindowDoesNotOfferOrAcceptRenewal() {
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    assertThat(service.list(4L).getFirst().windowRevalidation()).isNull();
    assertWindowRejected("ainda não encerrou");
  }

  /** Compara estado e ausência de efeitos após recusa do mesmo comando usado pela tela. */
  private void assertWindowRejected(String reason) {
    var previousStart = cycle.getWindowStart();
    var previousEnd = cycle.getWindowEnd();
    when(products.findLockedById(4L)).thenReturn(Optional.of(experiment.getProduct()));
    when(cycles.findLocked(4L, 2L)).thenReturn(Optional.of(cycle));
    var request =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service.command
            .RevalidateCycleWindowRequest(
            java.util.UUID.randomUUID(),
            13,
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(1),
            "Homologação local sintética.");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.revalidateWindow(4L, 2L, request, "Operador sintético"))
        .hasMessageContaining(reason);
    assertThat(cycle.getRevision()).isEqualTo(13);
    assertThat(cycle.getWindowStart()).isEqualTo(previousStart);
    assertThat(cycle.getWindowEnd()).isEqualTo(previousEnd);
    org.mockito.Mockito.verify(experiments, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(events, org.mockito.Mockito.never())
        .saveAndFlush(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(cycles, org.mockito.Mockito.never())
        .saveAndFlush(org.mockito.ArgumentMatchers.any());
  }

  /** Exporta respostas reais do serviço para conferir seu consumo pela interface local. */
  @Test
  void serializesWindowGuidanceForLocalInterface() throws Exception {
    cycle.setProductVersion("contrato-sintetico-v1");
    cycle.setBriefJson(
        """
        {"hypothesis":"Hipótese sintética sem exposição real.",
         "mainChange":"Uma mensagem de teste.",
         "successCriterion":"Preservar a decisão oficial da janela."}
        """);
    cycle.setWindowStart(Instant.parse("2000-12-30T00:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2001-01-01T00:00:00Z"));
    experiment.setProduct(Product.builder().id(4L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var mapper = new ObjectMapper().findAndRegisterModules();
    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var legacy = service.list(4L).getFirst();
    assertThat(mapper.valueToTree(legacy).path("windowRevalidation").path("available").asBoolean())
        .isTrue();
    exportWindowFixture("legacy", legacy, mapper);

    cycle.setBaseline(true);
    cycle.setStage("DECISION");
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.INVALIDATED);
    var historical = service.list(4L).getFirst();
    assertThat(
            mapper.valueToTree(historical).path("windowRevalidation").path("available").asBoolean())
        .isFalse();
    exportWindowFixture("historical", historical, mapper);

    var current = new BusinessProcessDefinition();
    current.setDiagramJson("{\"experimentChangePolicy\":\"CHANGE_PER_CYCLE_V1\"}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            LearningCycleRules.PROCESS_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(current));
    cycle.setBaseline(false);
    cycle.setStage("AUTHORIZATION");
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var governed = service.list(4L).getFirst();
    assertThat(mapper.valueToTree(governed).path("windowRevalidation").path("reason").asText())
        .contains("novo ciclo");
    exportWindowFixture("current-policy", governed, mapper);
  }

  /** Grava apenas fixtures sintéticas quando a homologação integrada solicita um diretório. */
  private void exportWindowFixture(String name, LearningCycleResponse response, ObjectMapper mapper)
      throws Exception {
    String output = System.getProperty("cycle.window.fixtureDir");
    if (output == null) return;
    var directory = java.nio.file.Path.of(output);
    java.nio.file.Files.createDirectories(directory);
    java.nio.file.Files.writeString(
        directory.resolve(name + ".json"), mapper.writeValueAsString(response));
  }

  /** Obtém a opção exibida pela API para concluir a etapa de autorização. */
  private LearningCycleResponse.CommandOption authorizationCommand() {
    return service.list(4L).getFirst().commands().stream()
        .filter(command -> "COMPLETE".equals(command.action()))
        .findFirst()
        .orElseThrow();
  }
}
