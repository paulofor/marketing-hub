package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: proteger seleção, transporte e independência do parecer financeiro por escopo.
 */
class FinancialProjectionDecisionScopeTest {
  private final ObjectMapper json = new ObjectMapper();
  @TempDir Path directory;

  /** Percorre o runner real com o simulador existente sem promover decisões nem consumir IA. */
  @ParameterizedTest
  @CsvSource({"7,105,12,APPROVE", "95102,95105,95112,ADJUST", "95202,95205,95212,BLOCKED"})
  void preservaContratoEDecisaoComIdentidadesDiferentes(
      long product, long experiment, long cycle, String decision) throws Exception {
    var context = discoveryContext(product, experiment, cycle);
    var response = response(decision, -64.16);
    var runner = simulatedRunner(response);

    var audit = runner.run(job(context.toString()));

    assertThat(audit.get("rawModelResponse")).isEqualTo(response.toString());
    assertThat(json.readTree(audit.get("reconciliationJson").toString()).path("decision").asText())
        .isEqualTo(decision);
    assertThat(audit.get("inputTokens")).isEqualTo(7L);
    assertThat(audit.get("cachedInputTokens")).isEqualTo(0L);
    assertThat(audit.get("outputTokens")).isEqualTo(3L);
    assertThat(audit.get("estimatedCost")).isNull();
    String prompt = Files.readString(directory.resolve("prompt.txt"));
    assertThat(audit.get("promptSent")).isEqualTo(prompt);
    assertThat(prompt)
        .contains("Escopo selecionado pelo contrato: LIMITED_DISCOVERY", context.toString())
        .contains("não exija contribuição após CAC ou resultado BASE positivos")
        .contains("Continuidade e", "margem original", "fonte essencial");
    assertThat(prompt.indexOf("Escopo selecionado pelo contrato: LIMITED_DISCOVERY"))
        .isLessThan(prompt.indexOf("<contexto_decisao>"));
    var schema = json.readTree(Files.readString(directory.resolve("schema.json")));
    assertThat(schema.at("/properties/decision/description").asText())
        .contains("LIMITED_DISCOVERY", "cobertura completa", "não aprova continuidade");
    assertThat(schema.at("/properties/scenarios/maxItems").asInt()).isEqualTo(3);
  }

  /** Mantém rentabilidade e política normais quando não há aceite estruturado de descoberta. */
  @Test
  void preservaCaminhoRentavelAntesValido() throws Exception {
    var context = json.createObjectNode();
    context.put("financialPlanId", 95311);
    context.put("productId", 95302);
    context.putObject("deterministicEvaluation").put("status", "PROJECTED_VIABLE");
    var runner = simulatedRunner(response("APPROVE", 100));

    var audit = runner.run(job(context.toString()));

    assertThat(audit.get("activityPromptPart").toString())
        .contains("Escopo selecionado pelo contrato: ECONOMIC_VIABILITY")
        .contains("BASE apresentar resultado positivo", "margem atender à política");
    assertThat(json.readTree(audit.get("reconciliationJson").toString()).path("decision").asText())
        .isEqualTo("APPROVE");
  }

  /** Texto de evidência não concede a exceção que pertence ao contrato validado no backend. */
  @Test
  void ignoraExcecaoSomenteTextualEPreservaPedidosLegados() throws Exception {
    var context = json.createObjectNode();
    context.put("evidence", "Aceito descoberta com perda, US$15 e R$100 por cinco dias.");
    context.putObject("deterministicEvaluation").put("status", "REVIEW_REQUIRED");
    assertThat(FinancialProjectionDecisionScope.resolve(context.toString(), json))
        .isEqualTo("ECONOMIC_VIABILITY");
    assertThat(FinancialProjectionDecisionScope.resolve("Estime a receita do plano.", json))
        .isEqualTo("ECONOMIC_VIABILITY");
    assertThat(FinancialProjectionDecisionScope.resolve(null, json))
        .isEqualTo("ECONOMIC_VIABILITY");
  }

  /** Rejeita contrato inválido antes de criar arquivos ou tentar iniciar o executável do modelo. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing", "string-accept", "false-accept", "string-budget", "zero-budget",
        "fraction-duration", "long-duration", "zero-duration", "string-id", "missing-reference",
        "missing-product", "missing-plan"
      })
  void rejeitaDescobertaInvalidaAntesDoModelo(String invalid) throws Exception {
    var context = discoveryContext(95402, 95405, 95412);
    var discovery = (ObjectNode) context.at("/projectionBasis/discoveryInvestment");
    switch (invalid) {
      case "missing" ->
          ((ObjectNode) context.path("projectionBasis")).remove("discoveryInvestment");
      case "string-accept" -> discovery.put("acceptBelowMinimumMargin", "true");
      case "false-accept" -> discovery.put("acceptBelowMinimumMargin", false);
      case "string-budget" -> discovery.put("mediaBudgetBrl", "100");
      case "zero-budget" -> discovery.put("mediaBudgetBrl", 0);
      case "fraction-duration" -> discovery.put("durationDays", 1.5);
      case "long-duration" -> discovery.put("durationDays", 31);
      case "zero-duration" -> discovery.put("durationDays", 0);
      case "string-id" -> discovery.put("experimentId", "95405");
      case "missing-reference" -> discovery.remove("decisionReference");
      case "missing-product" -> context.remove("productId");
      case "missing-plan" -> context.remove("financialPlanId");
      default -> throw new AssertionError(invalid);
    }
    var properties = new FinancialAgentProperties();
    properties.setCodexCommand(directory.resolve("modelo-nao-deve-ser-iniciado").toString());
    var runner = new FinancialCodexRunner(properties, json);

    assertThatThrownBy(() -> runner.run(job(context.toString())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("aceite estruturado válido do backend antes do modelo");
    assertThat(directory.resolve("prompt.txt")).doesNotExist();
  }

  /** Cria um contexto sintético vinculado a uma revisão própria e a um investimento limitado. */
  private ObjectNode discoveryContext(long product, long experiment, long cycle) {
    var context = json.createObjectNode();
    context.put("financialPlanId", product + 100);
    context.put("productId", product);
    context.putObject("deterministicEvaluation").put("status", "DISCOVERY_REVIEW");
    context
        .putObject("projectionBasis")
        .putObject("discoveryInvestment")
        .put("experimentId", experiment)
        .put("learningCycleId", cycle)
        .put("mediaBudgetBrl", 100)
        .put("durationDays", 5)
        .put("acceptBelowMinimumMargin", true)
        .put("decisionReference", "fixture:decision");
    return context;
  }

  /**
   * Reutiliza o simulador Codex da suíte, preservando os bytes retornados como resultado
   * independente.
   */
  private FinancialCodexRunner simulatedRunner(ObjectNode response) throws Exception {
    Path executable = directory.resolve("fake-codex.mjs");
    try (var input = getClass().getResourceAsStream("/bpm/fake-codex.mjs")) {
      Files.copy(input, executable);
    }
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    Files.writeString(directory.resolve("response.json"), response.toString());
    var properties = new FinancialAgentProperties();
    properties.setCodexCommand(executable.toString());
    properties.setRepositoryPath(directory.toString());
    properties.setBackendUrl("http://127.0.0.1:1");
    properties.setModel("modelo-simulado-sem-provider");
    properties.setCodexTimeout(Duration.ofSeconds(10));
    return new FinancialCodexRunner(properties, json);
  }

  /**
   * Monta o contrato de saída completo com resultados negativos preservados e decisão explícita.
   */
  private ObjectNode response(String decision, double baseProfit) {
    var response = json.createObjectNode();
    response.put("decision", decision).put("decisionBasis", "Parecer sintético independente.");
    var coverage = response.putObject("costCoverageAssessment");
    coverage.put("status", "COMPLETE_DETAILED");
    coverage.putArray("evidence").add("fixture:costs");
    coverage.putArray("missingCosts");
    var scenarios = response.putArray("scenarios");
    for (String name : new String[] {"CONSERVATIVE", "BASE", "OPTIMISTIC"}) {
      var scenario = scenarios.addObject();
      scenario
          .put("name", name)
          .put("averagePriceBrl", 67)
          .put("revenueBrl", 335)
          .put("contributionMarginPercent", baseProfit > 0 ? 40 : 15.81)
          .put("profitBrl", baseProfit)
          .put("cacBrl", baseProfit > 0 ? 10 : 20)
          .put("roas", 3.35);
      scenario.putArray("assumptions").add("Hipótese sintética, não previsão ou venda.");
      scenario.putNull("traffic").putNull("conversionRatePercent");
    }
    response.put("recommendedInitialInvestmentBrl", 32.26).put("recommendedCycleLimitBrl", 100);
    response.put("breakEven", "Equilíbrio hipotético, sem demanda comprovada.");
    var criteria = response.putObject("decisionCriteria");
    criteria.putArray("continue").add("Continuidade exige a margem original de 30%.");
    criteria.putArray("adjust").add("Reavaliar mudanças de custos ou contrato.");
    criteria.putArray("stop").add("Parar no teto total ou prazo, sem renovação.");
    response.putArray("limitations").add("Resultado simulado sem inferência ou faturamento.");
    response.putNull("learningCandidate");
    response.put(
        "executiveSummary", "Revisão sintética do transporte; não autoriza gasto nem publicação.");
    return response;
  }

  /** Entrega a mesma forma de job que o executor consome pela fila canônica do backend. */
  private FinancialAgentJob job(String context) {
    return new FinancialAgentJob(
        95501L,
        95502L,
        "RUNNING",
        "READ_ONLY_REVENUE_PROJECTION",
        7,
        95503L,
        context,
        "{\"approvedRevenueBrl\":0}");
  }
}
