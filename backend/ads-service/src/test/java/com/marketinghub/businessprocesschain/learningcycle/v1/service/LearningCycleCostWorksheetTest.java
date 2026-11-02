package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.CostWorksheetRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.MediaType;

/**
 * Responsabilidade: homologar cálculos, fontes, segregação e recuperação da planilha pelo contrato
 * HTTP.
 */
class LearningCycleCostWorksheetTest {
  LearningCycleFinancialReviewTest f;
  String root;

  /** Reutiliza o harness financeiro com controller e serviços reais, sem integração paga. */
  @BeforeEach
  void setup() {
    f = new LearningCycleFinancialReviewTest();
    f.setup();
    root = "/api/business-process-chains/learning-cycles/v1/products/7/19";
  }

  /** Constrói totais sintéticos com investimento inicial e despesas sem sobreposição. */
  private ObjectNode request() {
    var lines = new ArrayList<CostWorksheetRequest.Line>();
    for (String code :
        List.of("AGENTS", "PRODUCTION", "MEDIA", "DELIVERY", "SALES_DEDUCTIONS", "FIXED_OPERATION"))
      lines.add(
          new CostWorksheetRequest.Line(
              code,
              new BigDecimal("100.00"),
              new BigDecimal("50.00"),
              new BigDecimal("80.00"),
              "Fonte local sintética; total do grupo no período"));
    return (ObjectNode)
        f.json.read(
            f.json.write(
                new CostWorksheetRequest(
                    UUID.randomUUID(),
                    f.cycle.getRevision(),
                    f.cycle.getChainDefinitionId(),
                    f.cycle.getExperimentId(),
                    f.cycle.getProductVersion(),
                    LocalDate.of(2026, 11, 1),
                    LocalDate.of(2026, 11, 30),
                    new BigDecimal("2000.00"),
                    new BigDecimal("5.00"),
                    "Câmbio fictício para teste",
                    LocalDate.of(2026, 10, 30),
                    "Teste local",
                    "Dez vendas fictícias; todos os custos classificados uma vez",
                    false,
                    lines)));
  }

  /** Envia o comando real e recupera os valores já calculados no backend. */
  private JsonNode save(ObjectNode body) throws Exception {
    return f.json.read(
        f.http
            .perform(
                post(root + "/cost-worksheet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Comprova os seis grupos com valores desconhecidos, sem gravar nem chamar Plutus. */
  @Test
  void readsSixGroupsWithoutInventingActualCosts() throws Exception {
    var response =
        f.http
            .perform(get(root + "/cost-worksheet?chainId=27"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    var json = f.json.read(response);
    assertThat(json.path("rows")).hasSize(6);
    assertThat(json.at("/rows/0/estimatedCost").decimalValue()).isEqualByComparingTo("38.89");
    assertThat(json.at("/rows/0/reportedCost").isNull()).isTrue();
    assertThat(json.at("/totals/0/forecast").isNull()).isTrue();
    assertThat(json.at("/projectedResultBrl").isNull()).isTrue();
    assertThat(f.history).isEmpty();
    verifyNoInteractions(f.plans);
  }

  /** Confirma o caso original e outra identidade, sem somar estimativa ao custo informado. */
  @ParameterizedTest
  @CsvSource({"7,19,112,27", "99107,99119,99112,99127"})
  void savesCalculatesAndReplaysForDifferentProducts(
      long productId, long cycleId, long experimentId, long chainId) throws Exception {
    f.cycle.setId(cycleId);
    f.cycle.setProductId(productId);
    f.cycle.setExperimentId(experimentId);
    f.cycle.setChainDefinitionId(chainId);
    Product product = Product.builder().id(productId).name("Produto sintético").build();
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    when(f.cycles.findById(cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.cycles.findLocked(productId, cycleId)).thenReturn(Optional.of(f.cycle));
    when(f.products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(f.products.findById(productId)).thenReturn(Optional.of(product));
    when(f.experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(f.costs.summarize(productId))
        .thenReturn(
            List.of(
                new CycleFinancialReview.CostSlice(experimentId, new BigDecimal("67.37689"), 3)));
    root = "/api/business-process-chains/learning-cycles/v1/products/" + productId + "/" + cycleId;
    var body = request();
    for (int i = 0; i < 2; i++) {
      var result = save(body);
      assertThat(result.path("revision").asLong()).isEqualTo(2);
      assertThat(result.at("/totals/0/reportedCost").decimalValue()).isEqualByComparingTo("100");
      assertThat(result.at("/totals/0/forecast").decimalValue()).isEqualByComparingTo("160");
      assertThat(result.at("/totals/1/forecast").decimalValue()).isEqualByComparingTo("320");
      assertThat(result.path("projectedResultBrl").decimalValue()).isEqualByComparingTo("880.00");
      assertThat(result.at("/rows/0/approved").isNull()).isTrue();
      assertThat(result.at("/rows/3/approved").isNull()).isTrue();
    }
    assertThat(f.history).hasSize(1);
    assertThat(f.cycle.getStage()).isEqualTo("PLANNING");
    assertThat(f.cycle.getBudgetLimitBrl()).isEqualByComparingTo("89.95");
    f.http
        .perform(get(root + "/cost-worksheet?chainId=" + chainId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.history.length()").value(1));
  }

  /** Aprovação administrativa não muda recibos operacionais e nova edição remove sua vigência. */
  @Test
  void preservesOperationalApprovalsAndInvalidatesPlanningApprovalOnEdit() throws Exception {
    var body = request();
    body.put("approveOperatingBudgets", true);
    var result = save(body);
    assertThat(result.at("/rows/3/approved").decimalValue()).isEqualByComparingTo("100");
    assertThat(result.at("/rows/3/approvalStatus").asText())
        .contains("sem autorização de execução");
    assertThat(f.budget.current(f.cycle)).isNull();
    assertThat(f.video.current(f.cycle)).isNull();
    result = save(request());
    assertThat(result.at("/rows/3/approved").isNull()).isTrue();
    assertThat(result.path("history")).hasSize(2);
    f.cycle.setProductVersion("outra-versao");
    var read =
        f.http
            .perform(get(root + "/cost-worksheet?chainId=27"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(f.json.read(read).path("saved").isNull()).isTrue();
    assertThat(f.json.read(read).path("history")).hasSize(2);
  }

  /** Custo faltante, receita ausente ou câmbio ausente impedem resultado completo. */
  @ParameterizedTest
  @ValueSource(strings = {"expectedRevenueBrl", "usdBrl", "forecast"})
  void partialForecastDoesNotBecomeProfit(String field) throws Exception {
    var body = request();
    if ("forecast".equals(field)) ((ObjectNode) body.path("lines").get(1)).putNull(field);
    else body.putNull(field);
    var result = save(body);
    assertThat(result.path("projectedResultBrl").isNull()).isTrue();
    assertThat(result.path("gaps")).isNotEmpty();
  }

  /**
   * Zero declarado com fonte é preservado e dispensa conversão quando não existe previsão em US$.
   */
  @Test
  void explicitZeroIsDifferentFromUnknown() throws Exception {
    var body = request();
    body.putNull("usdBrl");
    for (var line : body.path("lines")) {
      ((ObjectNode) line).put("reportedCost", 0);
      ((ObjectNode) line).put("forecast", 0);
    }
    var result = save(body);
    assertThat(result.path("projectedResultBrl").decimalValue()).isEqualByComparingTo("2000");
    assertThat(result.at("/totals/0/missingForecast").asInt()).isZero();
  }

  /** Recusa dados inválidos ou contexto obsoleto antes de persistir qualquer efeito. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "negative",
        "precision",
        "missingSource",
        "duplicate",
        "period",
        "identity",
        "revision",
        "closed",
        "fxSource",
        "approveMissing"
      })
  void rejectsInvalidOrObsoleteInputs(String issue) throws Exception {
    var body = request();
    var row = (ObjectNode) body.path("lines").get(0);
    switch (issue) {
      case "negative" -> row.put("planned", -1);
      case "precision" -> row.put("reportedCost", new BigDecimal("1.001"));
      case "missingSource" -> row.put("source", " ");
      case "duplicate" -> row.put("code", "MEDIA");
      case "period" -> body.put("periodEnd", "2026-10-01");
      case "identity" -> body.put("experimentId", 99999);
      case "revision" -> body.put("expectedRevision", 999);
      case "closed" -> f.cycle.setStatus("CLOSED");
      case "fxSource" -> body.putNull("exchangeSource");
      case "approveMissing" -> {
        body.put("approveOperatingBudgets", true);
        ((ObjectNode) body.path("lines").get(5)).putNull("planned");
      }
      default -> throw new IllegalStateException("Caso de teste não previsto");
    }
    f.http
        .perform(
            post(root + "/cost-worksheet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().is4xxClientError());
    assertThat(f.history).isEmpty();
  }

  /** Teto cadastrado não vira aprovação e o recibo perde vigência ao vencer a janela. */
  @Test
  void requiresActualMediaReceiptAndValidWindow() throws Exception {
    var first = save(request());
    assertThat(first.at("/rows/2/approved").isNull()).isTrue();
    var event =
        new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent();
    event.setId(99L);
    event.setCycleId(19L);
    event.setRevision(2);
    event.setAction("COMPLETE");
    event.setFromStage("AUTHORIZATION");
    event.setCreatedAt(java.time.Instant.now());
    event.setEvidenceJson(
        "{\"confirmed\":true,\"productVersion\":\"teste-v3\",\"budgetLimitBrl\":89.95}");
    f.history.add(event);
    f.cycle.setWindowEnd(java.time.Instant.now().plusSeconds(60));
    f.http
        .perform(get(root + "/cost-worksheet?chainId=27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[2].approved").value(89.95));
    f.cycle.setWindowEnd(java.time.Instant.now().minusSeconds(60));
    f.http
        .perform(get(root + "/cost-worksheet?chainId=27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[2].approved").isEmpty());
  }

  /** Uma chave já persistida não aceita conteúdo novo e o acesso por outro produto é recusado. */
  @Test
  void rejectsKeyReuseAndCrossProductRead() throws Exception {
    var body = request();
    save(body);
    body.put("expectedRevenueBrl", 3000);
    f.http
        .perform(
            post(root + "/cost-worksheet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().is4xxClientError());
    f.http
        .perform(
            get(
                "/api/business-process-chains/learning-cycles/v1/products/4/19/cost-worksheet?chainId=27"))
        .andExpect(status().isNotFound());
    assertThat(f.history).hasSize(1);
  }
}
