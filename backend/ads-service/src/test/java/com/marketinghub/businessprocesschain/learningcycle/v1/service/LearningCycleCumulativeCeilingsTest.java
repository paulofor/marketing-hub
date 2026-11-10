package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.service.pending.AgentTaskPreparationBudgetGuidance;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunCorrectionBudget;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.ApproveCostCeilingRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.*;
import com.marketinghub.experiment.Experiment;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

/**
 * Responsabilidade: comprovar continuidade financeira real sem renovar saldo ou aprovação no
 * sucessor.
 */
class LearningCycleCumulativeCeilingsTest {
  LearningCycleFinancialReviewTest f;

  /** Reutiliza controller, serviços e ledger do harness financeiro com integrações locais. */
  @BeforeEach
  void setup() {
    f = new LearningCycleFinancialReviewTest();
    f.setup();
    f.cycle.setChainCode("cadeia-local");
  }

  /** Registra teto e reserva explícitos sem executar agentes ou produção. */
  private void authorize(String total, String reserve) {
    f.budget.authorize(
        f.cycle,
        new PreparationBudgetRequest(
            UUID.randomUUID(),
            f.cycle.getRevision(),
            f.cycle.getChainDefinitionId(),
            f.cycle.getExperimentId(),
            f.cycle.getProductVersion(),
            new BigDecimal(total),
            new BigDecimal(reserve),
            "Teste local",
            "Decisão sintética",
            true),
        new BigDecimal("38.89"),
        2,
        f.now);
  }

  /** Prepara um sucessor de outra versão sem copiar eventos nem alterar a decisão original. */
  private LearningSalesCycle successor(LearningSalesCycle previous, long id, long experimentId) {
    var next = new LearningSalesCycle();
    next.setId(id);
    next.setProductId(previous.getProductId());
    next.setChainCode(previous.getChainCode());
    next.setChainDefinitionId(270L);
    next.setProcessDefinitionId(previous.getProcessDefinitionId());
    next.setPreviousCycleId(previous.getId());
    next.setExperimentId(experimentId);
    next.setProductVersion("nova-versao-" + id);
    next.setVersionChangedAt(f.now.plusSeconds(id));
    next.setStage("PLANNING");
    next.setStatus("OPEN");
    next.setRevision(1);
    when(f.cycles.findById(previous.getId())).thenReturn(Optional.of(previous));
    when(f.cycles.findById(id)).thenReturn(Optional.of(next));
    when(f.cycles.findLocked(next.getProductId(), id)).thenReturn(Optional.of(next));
    when(f.cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(next));
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(f.product);
    when(f.experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    return next;
  }

  /** Confirma o caso original e outro produto, incluindo pending, saldo e bloqueio de consumo. */
  @ParameterizedTest
  @CsvSource({"7,19,112,20,113", "9907,9919,99112,9920,99113"})
  void carriesOriginalReceiptAndReserveThroughHttpAndExecutor(
      long productId, long oldId, long oldExperiment, long nextId, long nextExperiment)
      throws Exception {
    f.cycle.setProductId(productId);
    f.cycle.setId(oldId);
    f.cycle.setExperimentId(oldExperiment);
    f.product.setId(productId);
    when(f.products.findById(productId)).thenReturn(Optional.of(f.product));
    when(f.products.findLockedById(productId)).thenReturn(Optional.of(f.product));
    authorize("120", "20");
    var next = successor(f.cycle, nextId, nextExperiment);
    f.cycle.setStatus("ADJUSTED");
    when(f.costs.summarize(productId))
        .thenReturn(
            List.of(new CycleFinancialReview.CostSlice(oldExperiment, new BigDecimal("76.35"), 3)));
    String root =
        "/api/business-process-chains/learning-cycles/v1/products/" + productId + "/" + nextId;
    f.http
        .perform(get(root + "/financial-review?chainId=270"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorization.eventId").value(1))
        .andExpect(jsonPath("$.authorization.sourceCycleId").value(oldId))
        .andExpect(jsonPath("$.authorization.carriedForward").value(true))
        .andExpect(jsonPath("$.authorization.totalLimitUsd").value(120))
        .andExpect(jsonPath("$.authorization.reserveUsd").value(20))
        .andExpect(jsonPath("$.availableUsd").value(23.65))
        .andExpect(jsonPath("$.preparationBalance.requiresBudgetReview").value(false));
    var guidance =
        new AgentTaskPreparationBudgetGuidance(f.cycles, f.budget)
            .read(productId, "experiment:" + nextExperiment);
    assertThat(guidance.cycleId()).isEqualTo(nextId);
    assertThat(guidance.authorization().sourceCycleId()).isEqualTo(oldId);
    assertThat(guidance.authorization().eventId()).isEqualTo(1);
    assertThat(guidance.authorizesMediaOrAudiovisual()).isFalse();
    var run = new ProcessRun();
    run.setProductId(productId);
    run.setLearningCycleId(nextId);
    run.setChainDefinitionId(next.getChainDefinitionId());
    run.setSourceReference("experiment:" + nextExperiment);
    var activity =
        new ObjectMapper()
            .convertValue(
                Map.of("tasks", List.of()), ProductProcessActivityExecutionGroupResponse.class);
    var guard = new ProcessRunCorrectionBudget(f.cycles, f.budget, f.costs);
    assertThat(guard.blockReason(run, activity)).isNull();
    when(f.costs.summarize(productId))
        .thenReturn(
            List.of(new CycleFinancialReview.CostSlice(oldExperiment, new BigDecimal("100"), 3)));
    f.http
        .perform(get(root + "/financial-review?chainId=270"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableUsd").value(0))
        .andExpect(
            jsonPath("$.preparationBalance.title").value("Atenção: teto de agentes atingido"))
        .andExpect(jsonPath("$.preparationBalance.requiresBudgetReview").value(true));
    assertThat(guard.blockReason(run, activity)).contains("Atenção", "não cobre");
    assertThat(f.history).hasSize(1);
    assertThat(next.getRevision()).isEqualTo(1);
    verify(f.tasks, never()).save(any());
  }

  /**
   * Os seis grupos permanecem registrados e substituição atual não se soma aos valores anteriores.
   */
  @Test
  void keepsAllSixCeilingsAndReplacesOnlyExplicitlyApprovedGroup() throws Exception {
    for (var code : ApproveCostCeilingRequest.Group.values()) {
      var request =
          new ApproveCostCeilingRequest(
              UUID.randomUUID(),
              f.cycle.getRevision(),
              27L,
              112L,
              f.cycle.getProductVersion(),
              code,
              new BigDecimal("100"),
              "Teste local",
              "Teto sintético",
              true);
      f.http
          .perform(
              post("/api/business-process-chains/learning-cycles/v1/products/7/19/cost-ceilings")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(f.json.write(request)))
          .andExpect(status().isOk());
    }
    var next = successor(f.cycle, 20, 113);
    var third = successor(next, 21, 114);
    String root = "/api/business-process-chains/learning-cycles/v1/products/7/21";
    var response =
        f.http
            .perform(get(root + "/cost-worksheet?chainId=270"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var view = f.json.read(response);
    for (var row : view.path("rows"))
      assertThat(row.path("approved").decimalValue()).isEqualByComparingTo("100");
    assertThat(view.path("ceilingHistory")).hasSize(6);
    var change =
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            1L,
            270L,
            114L,
            third.getProductVersion(),
            ApproveCostCeilingRequest.Group.DELIVERY,
            new BigDecimal("90"),
            "Teste local",
            "Substituição do teto, sem adicionar verba",
            true);
    for (int i = 0; i < 2; i++)
      f.http
          .perform(
              post(root + "/cost-ceilings")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(f.json.write(change)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.rows[3].approved").value(90));
    var old = f.review.read(f.cycle, f.product, f.now);
    assertThat(old.authorization().carriedForward()).isFalse();
    assertThat(new LearningCycleCostCeilings(f.events, f.json, f.cumulative).history(f.cycle))
        .hasSize(6);
    var stale =
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            1L,
            270L,
            114L,
            third.getProductVersion(),
            ApproveCostCeilingRequest.Group.DELIVERY,
            new BigDecimal("95"),
            "Teste local",
            "Revisão concorrente",
            true);
    f.http
        .perform(
            post(root + "/cost-ceilings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(f.json.write(stale)))
        .andExpect(status().isConflict());
  }

  /** Contextos alheios, predecessores ausentes e ciclos de referências não podem liberar teto. */
  @Test
  void rejectsInvalidLineageWithoutFallingBackToAnotherProduct() {
    authorize("120", "20");
    var next = successor(f.cycle, 20, 113);
    next.setChainCode("outra-cadeia");
    assertThatThrownBy(() -> f.budget.current(next)).hasMessageContaining("linhagem financeira");
    next.setChainCode(f.cycle.getChainCode());
    next.setProductId(999L);
    assertThatThrownBy(() -> f.budget.current(next)).hasMessageContaining("linhagem financeira");
    next.setProductId(7L);
    f.cycle.setPreviousCycleId(next.getId());
    assertThatThrownBy(() -> f.budget.current(next)).hasMessageContaining("linhagem financeira");
    f.cycle.setPreviousCycleId(null);
    next.setPreviousCycleId(999L);
    assertThatThrownBy(() -> f.budget.current(next))
        .hasMessageContaining("predecessor não foi encontrado");
    verify(f.events, never())
        .findByCycleIdInAndActionOrderByCreatedAtDescIdDesc(eq(Set.of(999L)), anyString());
  }

  /** Uma versão invalidada continua consultável e não vira autoridade financeira por fallback. */
  @Test
  void retainsOriginalHistoryWhenItsOwnVersionIsInvalidated() {
    authorize("120", "20");
    f.cycle.setProductVersion("outra-versao-no-mesmo-ciclo");
    f.cycle.setVersionChangedAt(f.now.plusSeconds(1));
    assertThat(f.budget.current(f.cycle)).isNull();
    assertThat(f.budget.history(f.cycle)).hasSize(1);
    assertThat(f.budget.history(f.cycle).getFirst().current()).isFalse();
    assertThat(f.history).hasSize(1);
  }

  /** Um teto planejado herdado não pode ocultar uma decisão operacional posterior e explícita. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"MEDIA", "PRODUCTION"})
  void preservesExplicitOperationalLimitInsteadOfOlderInheritedPlanning(String code) {
    var ceilings = new LearningCycleCostCeilings(f.events, f.json, f.cumulative);
    ceilings.record(
        f.cycle,
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            f.cycle.getRevision(),
            27L,
            112L,
            f.cycle.getProductVersion(),
            ApproveCostCeilingRequest.Group.valueOf(code),
            new BigDecimal("100"),
            "Teste local",
            "Planejamento anterior",
            true),
        f.now);
    var next = successor(f.cycle, 20, 113);
    var observedAt = f.now.plusSeconds(30);
    if ("MEDIA".equals(code)) {
      next.setStage("AUTHORIZATION");
      next.setBudgetLimitBrl(new BigDecimal("80"));
      next.setWindowEnd(observedAt.plusSeconds(3600));
      var event = new LearningSalesCycleEvent();
      event.setId(900L);
      event.setCycleId(next.getId());
      event.setAction("COMPLETE");
      event.setFromStage("AUTHORIZATION");
      event.setCreatedAt(observedAt);
      event.setEvidenceJson(
          f.json.write(
              Map.of(
                  "confirmed",
                  true,
                  "productVersion",
                  next.getProductVersion(),
                  "budgetLimitBrl",
                  80)));
      f.history.add(event);
    } else {
      next.setStage("VIDEO_BRIEF");
      f.video.authorize(
          next,
          new com.marketinghub
              .businessprocesschain
              .learningcycle
              .v1
              .service
              .videoBudget
              .AuthorizeVideoBudgetRequest(
              UUID.randomUUID(),
              next.getRevision(),
              270L,
              113L,
              next.getProductVersion(),
              new BigDecimal("80"),
              "Teste local",
              "Limite operacional posterior",
              true),
          true,
          observedAt);
    }
    var sheet =
        new LearningCycleCostWorksheet(f.events, f.json, new ObjectMapper(), ceilings)
            .read(next, f.review.read(next, f.product, observedAt), null);
    var row =
        sheet.rows().stream().filter(item -> code.equals(item.code())).findFirst().orElseThrow();
    assertThat(row.approved()).isEqualByComparingTo("80");
    assertThat(row.approvalStatus()).contains("operacional").doesNotContain("mantido do ciclo");
    assertThat(ceilings.history(next).getFirst().approved()).isEqualByComparingTo("100");
  }

  /** A invalidação da última decisão nunca ressuscita um teto anterior maior já substituído. */
  @Test
  void neverReactivatesSupersededHigherLimitWhenLatestContextBecomesInvalid() {
    authorize("200", "20");
    var ceilings = new LearningCycleCostCeilings(f.events, f.json, f.cumulative);
    ceilings.record(
        f.cycle,
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            f.cycle.getRevision(),
            27L,
            112L,
            f.cycle.getProductVersion(),
            ApproveCostCeilingRequest.Group.AGENTS,
            new BigDecimal("200"),
            "Teste local",
            "Teto anterior",
            true),
        f.now);
    var next = successor(f.cycle, 20, 113);
    var at = f.now.plusSeconds(30);
    f.budget.authorize(
        next,
        new PreparationBudgetRequest(
            UUID.randomUUID(),
            next.getRevision(),
            270L,
            113L,
            next.getProductVersion(),
            new BigDecimal("100"),
            new BigDecimal("15"),
            "Teste local",
            "Redução explícita do teto",
            true),
        BigDecimal.ONE,
        0,
        at);
    ceilings.record(
        next,
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            next.getRevision(),
            270L,
            113L,
            next.getProductVersion(),
            ApproveCostCeilingRequest.Group.AGENTS,
            new BigDecimal("100"),
            "Teste local",
            "Redução explícita do teto",
            true),
        at);
    assertThat(f.budget.current(next).totalLimitUsd()).isEqualByComparingTo("100");
    next.setProductVersion("contexto-invalidado-no-mesmo-ciclo");
    next.setVersionChangedAt(at.plusSeconds(1));
    assertThat(f.budget.current(next)).isNull();
    assertThat(f.budget.history(next)).hasSize(2).noneMatch(receipt -> receipt.current());
    assertThat(ceilings.history(next)).hasSize(2).noneMatch(receipt -> receipt.current());
  }
}
