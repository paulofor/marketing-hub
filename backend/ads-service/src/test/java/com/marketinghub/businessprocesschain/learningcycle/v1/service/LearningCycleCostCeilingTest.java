package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.ApproveCostCeilingRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.ApproveCostCeilingRequest.Group;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.PreparationBudgetRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.MediaType;

/** Responsabilidade: verificar os tetos individuais, seus gates e a recuperação sem duplicação. */
class LearningCycleCostCeilingTest {
  LearningCycleFinancialReviewTest f;
  String root;

  /** Inicializa contratos reais com dependências externas simuladas e ledger auditável. */
  @BeforeEach
  void setup() {
    f = new LearningCycleFinancialReviewTest();
    f.setup();
    root = "/api/business-process-chains/learning-cycles/v1/products/7/19";
  }

  /** Monta decisão sintética contendo somente o teto e sua autoria explícita. */
  private ObjectNode request(Group group) {
    return (ObjectNode)
        f.json.read(
            f.json.write(
                new ApproveCostCeilingRequest(
                    UUID.randomUUID(),
                    f.cycle.getRevision(),
                    f.cycle.getChainDefinitionId(),
                    f.cycle.getExperimentId(),
                    f.cycle.getProductVersion(),
                    group,
                    new BigDecimal("100.50"),
                    "Operador local",
                    "Conferência sintética sem novo gasto",
                    true)));
  }

  /** Envia a decisão pelo controller e retorna a planilha já persistida e projetada. */
  private JsonNode approve(ObjectNode request) throws Exception {
    return f.json.read(
        f.http
            .perform(
                post(root + "/cost-ceilings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Confere o caso original e outra identidade preservando a reserva e o guard anterior. */
  @ParameterizedTest
  @CsvSource({"7,19,112,27", "99107,99119,99112,99127"})
  void preservesCanonicalPreparationAndReplays(
      long productId, long cycleId, long experimentId, long chainId) throws Exception {
    f.cycle.setProductId(productId);
    f.cycle.setId(cycleId);
    f.cycle.setExperimentId(experimentId);
    f.cycle.setChainDefinitionId(chainId);
    var product = Product.builder().id(productId).name("Contexto sintético").build();
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    when(f.products.findById(productId)).thenReturn(Optional.of(product));
    when(f.products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(f.cycles.findById(cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.cycles.findLocked(productId, cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    f.budget.authorize(
        f.cycle,
        new PreparationBudgetRequest(
            UUID.randomUUID(),
            1L,
            chainId,
            experimentId,
            f.cycle.getProductVersion(),
            new BigDecimal("80"),
            new BigDecimal("20"),
            "Operador anterior",
            "Reserva existente",
            true),
        new BigDecimal("67.37689"),
        3,
        f.now);
    root = "/api/business-process-chains/learning-cycles/v1/products/" + productId + "/" + cycleId;
    var request = request(Group.AGENTS);
    var result = approve(request);
    assertThat(result.at("/rows/0/approved").decimalValue()).isEqualByComparingTo("100.50");
    assertThat(approve(request).path("revision")).isEqualTo(result.path("revision"));
    assertThat(f.budget.history(f.cycle).getFirst().reserveUsd()).isEqualByComparingTo("20");
    assertThat(f.history).hasSize(3);
    assertThat(f.cycle.getStage()).isEqualTo("PLANNING");
    assertThat(f.cycle.getBudgetLimitBrl()).isEqualByComparingTo("89.95");
    assertThat(f.video.current(f.cycle)).isNull();
    verifyNoInteractions(f.plans);
  }

  /** Aprova cada grupo operacional sem exigir custos, previsões ou outros tetos preenchidos. */
  @ParameterizedTest
  @CsvSource({"DELIVERY,3", "SALES_DEDUCTIONS,4", "FIXED_OPERATION,5"})
  void approvesOneOperatingGroup(Group group, int index) throws Exception {
    var request = request(group).put("approved", 0);
    var result = approve(request);
    assertThat(result.at("/rows/" + index + "/approved").decimalValue()).isZero();
    assertThat(result.at("/rows/0/approved").isNull()).isTrue();
    assertThat(result.at("/rows/" + index + "/forecast").isNull()).isTrue();
    assertThat(result.path("saved").isNull()).isTrue();
    assertThat(result.path("ceilingHistory")).hasSize(1);
    approve(request);
    assertThat(f.history).hasSize(1);
    assertThat(f.cycle.getRevision()).isEqualTo(2);
    assertThat(f.cycle.getStage()).isEqualTo("PLANNING");
  }

  /** Registra decisão antecipada sem conceder autoridade operacional ou repetir recibo. */
  @ParameterizedTest
  @CsvSource({
    "PRODUCTION,1,7,19,112,27",
    "MEDIA,2,7,19,112,27",
    "PRODUCTION,1,99107,99119,99112,99127",
    "MEDIA,2,99107,99119,99112,99127"
  })
  void recordsPlanningWithoutBypassingStageGates(
      Group group, int index, long productId, long cycleId, long experimentId, long chainId)
      throws Exception {
    f.cycle.setProductId(productId);
    f.cycle.setId(cycleId);
    f.cycle.setExperimentId(experimentId);
    f.cycle.setChainDefinitionId(chainId);
    var product = Product.builder().id(productId).name("Contexto sintético").build();
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    when(f.products.findById(productId)).thenReturn(Optional.of(product));
    when(f.products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(f.cycles.findById(cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.cycles.findLocked(productId, cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    root = "/api/business-process-chains/learning-cycles/v1/products/" + productId + "/" + cycleId;
    var request = request(group);
    var result = approve(request);
    assertThat(result.at("/rows/" + index + "/approved").decimalValue())
        .isEqualByComparingTo("100.50");
    assertThat(result.at("/rows/" + index + "/approvalScope").asText()).isEqualTo("PLANNING");
    assertThat(result.at("/rows/" + index + "/approvalStatus").asText())
        .contains("ainda não autorizada");
    approve(request);
    assertThat(f.history).hasSize(1);
    assertThat(f.cycle.getStage()).isEqualTo("PLANNING");
    assertThat(f.cycle.getBudgetLimitBrl()).isEqualByComparingTo("89.95");
    assertThat(f.video.current(f.cycle)).isNull();
    verifyNoInteractions(f.plans);
  }

  /** Reutiliza o teto antecipado somente após confirmação na etapa compatível. */
  @Test
  void appliesSavedProductionCeilingOnlyWithNewConfirmation() throws Exception {
    approve(request(Group.PRODUCTION));
    f.cycle.setStage("VIDEO_BRIEF");
    assertThat(f.video.current(f.cycle)).isNull();
    var result = approve(request(Group.PRODUCTION));
    assertThat(result.at("/rows/1/approvalScope").asText()).isEqualTo("EXECUTION_LIMIT");
    assertThat(f.video.current(f.cycle).budgetLimitUsd()).isEqualByComparingTo("100.50");
    assertThat(f.cycle.getStage()).isEqualTo("VIDEO_BRIEF");
  }

  /** Impede reduzir uma autorização operacional por um registro de planejamento posterior. */
  @Test
  void cannotOverrideOperationalBudgetWhileStageIsProtected() throws Exception {
    f.cycle.setStage("VIDEO_BRIEF");
    approve(request(Group.PRODUCTION));
    f.cycle.setStage("VIDEO_PRODUCTION");
    f.http
        .perform(
            post(root + "/cost-ceilings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(Group.PRODUCTION).put("approved", 1).toString()))
        .andExpect(status().isConflict());
    assertThat(f.video.current(f.cycle).budgetLimitUsd()).isEqualByComparingTo("100.50");
    assertThat(f.history).hasSize(2);
  }

  /** Reutiliza o recibo audiovisual oficial quando a etapa permite o teto das duas peças. */
  @Test
  void recordsOfficialVideoBudgetWithoutAdvancingOrApprovingMedia() throws Exception {
    f.cycle.setStage("VIDEO_BRIEF");
    var request = request(Group.PRODUCTION);
    var result = approve(request);
    assertThat(result.at("/rows/1/approved").decimalValue()).isEqualByComparingTo("100.50");
    assertThat(f.video.current(f.cycle).budgetLimitUsd()).isEqualByComparingTo("100.50");
    approve(request);
    assertThat(f.history).hasSize(2);
    assertThat(f.cycle.getStage()).isEqualTo("VIDEO_BRIEF");
    assertThat(f.cycle.getBudgetLimitBrl()).isEqualByComparingTo("89.95");
  }

  /** Recusa dados inválidos antes de qualquer alteração de limite ou recibo. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "negative",
        "precision",
        "confirmation",
        "missing",
        "context",
        "revision",
        "closed"
      })
  void rejectsInvalidDecisions(String failure) throws Exception {
    var request = request(Group.DELIVERY);
    int status = 400;
    switch (failure) {
      case "negative" -> request.put("approved", -1);
      case "precision" -> request.put("approved", new BigDecimal("1.001"));
      case "confirmation" -> request.put("confirmed", false);
      case "missing" -> request.remove("approved");
      case "context" -> {
        request.put("experimentId", 999);
        status = 409;
      }
      case "revision" -> {
        request.put("expectedRevision", 99);
        status = 409;
      }
      case "closed" -> {
        f.cycle.setStatus("CLOSED");
        status = 409;
      }
      default -> throw new IllegalArgumentException(failure);
    }
    f.http
        .perform(
            post(root + "/cost-ceilings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
        .andExpect(status().is(status));
    assertThat(f.history).isEmpty();
  }

  /**
   * Recusa chave reutilizada e mantém revisões antigas somente no histórico após mudança de versão.
   */
  @Test
  void protectsHistoryAndRequestIdentity() throws Exception {
    var request = request(Group.DELIVERY);
    approve(request);
    f.http
        .perform(
            post(root + "/cost-ceilings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.put("approved", 101).toString()))
        .andExpect(status().isConflict());
    f.cycle.setProductVersion("outra-v4");
    var view =
        f.json.read(
            f.http
                .perform(get(root + "/cost-worksheet?chainId=27"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(view.at("/rows/3/approved").isNull()).isTrue();
    assertThat(view.at("/ceilingHistory/0/current").asBoolean()).isFalse();
  }
}
