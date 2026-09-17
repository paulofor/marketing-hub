package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger reconciliação, prazo e contribuição do gate econômico de Plutus. */
class PdeEconomicsBpmTaskConsumerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Aceita três cenários com contribuição positiva e reconciliada. */
  @Test
  void acceptsReconciledEconomics() throws Exception {
    assertThatCode(
            () ->
                PdeEconomicsBpmTaskConsumer.validate(
                    objectMapper.readTree(result("APPROVE", 97, 12, 85, "2026-10-31"))))
        .doesNotThrowAnyException();
  }

  /** Rejeita contribuição que não corresponde ao preço menos o custo variável. */
  @Test
  void rejectsUnreconciledContribution() throws Exception {
    var result = objectMapper.readTree(result("APPROVE", 97, 12, 70, "2026-10-31"));

    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não reconcilia");
  }

  /** Rejeita prazo ambíguo para manter a hipótese financeira auditável. */
  @Test
  void rejectsNonIsoDeadline() throws Exception {
    var result = objectMapper.readTree(result("APPROVE", 97, 12, 85, "fim de outubro"));

    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("YYYY-MM-DD");
  }

  /** Aceita preço hipotético privado sem liberar orçamento, tráfego, venda ou receita. */
  @Test
  void acceptsPrivateValidationHypothesisWithoutCommercialEffects() throws Exception {
    var result = objectMapper.readTree(privateResult(false, 2, 0));

    assertThatCode(() -> PdeEconomicsBpmTaskConsumer.validatePrivateValidation(result))
        .doesNotThrowAnyException();
  }

  /** Rejeita a economia privada quando o parecer tenta autorizar gasto comercial. */
  @Test
  void rejectsPrivateValidationWithCommercialSpend() throws Exception {
    var result = objectMapper.readTree(privateResult(true, 2, 100));

    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validatePrivateValidation(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("antecipou operação comercial");
  }

  /** Rejeita o contrato v2 de Atena antes que Plutus consuma uma nova chamada de modelo. */
  @Test
  void rejectsLegacyAtenaContractBeforeEconomics() throws Exception {
    var context =
        objectMapper.readTree(
            """
            {
              "marketStrategicContract": {
                "contractVersion": "MARKET_STRATEGY_V2",
                "status": "READY_FOR_OPERATION"
              }
            }
            """);

    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validatePrivateStrategyContract(context))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MARKET_STRATEGY_V3");
  }

  /** Aceita o contrato v3 com exatamente as duas leituras e os cinco sinais canônicos. */
  @Test
  void acceptsCurrentAtenaContractBeforeEconomics() throws Exception {
    var context =
        objectMapper.readTree(
            """
            {
              "marketStrategicContract": {
                "contractVersion": "MARKET_STRATEGY_V3",
                "status": "READY_FOR_PRIVATE_VALIDATION",
                "privateValidationPlan": {
                  "minimumIndependentReadings": 2,
                  "requiredSignals": [
                    "EXPERIENCE_STARTED",
                    "VALUE_MOMENT",
                    "READY_RESULT_USED",
                    "PREFERRED_OVER_FREE",
                    "CHECKOUT_STARTED"
                  ]
                }
              }
            }
            """);

    assertThatCode(() -> PdeEconomicsBpmTaskConsumer.validatePrivateStrategyContract(context))
        .doesNotThrowAnyException();
  }

  /** Aceita o contrato v3 dentro do envelope real de atividades predecessoras do backend. */
  @Test
  void acceptsCurrentAtenaContractFromStructuredProcessContext() throws Exception {
    var context =
        objectMapper.readTree(
            """
            {
              "completedActivities": [
                {
                  "taskId": 321,
                  "activityId": "marketStrategy",
                  "result": {
                    "marketStrategicContract": {
                      "contractVersion": "MARKET_STRATEGY_V2",
                      "status": "READY_FOR_OPERATION"
                    }
                  }
                },
                {
                  "taskId": 327,
                  "activityId": "marketStrategy",
                  "result": {
                    "marketStrategicContract": {
                      "contractVersion": "MARKET_STRATEGY_V3",
                      "status": "READY_FOR_PRIVATE_VALIDATION",
                      "privateValidationPlan": {
                        "minimumIndependentReadings": 2,
                        "requiredSignals": [
                          "EXPERIENCE_STARTED",
                          "VALUE_MOMENT",
                          "READY_RESULT_USED",
                          "PREFERRED_OVER_FREE",
                          "CHECKOUT_STARTED"
                        ]
                      }
                    }
                  }
                }
              ]
            }
            """);

    assertThatCode(() -> PdeEconomicsBpmTaskConsumer.validatePrivateStrategyContract(context))
        .doesNotThrowAnyException();
  }

  /** Mantém prompt, núcleo financeiro e schema coerentes sobre hipóteses sem gasto. */
  @Test
  void keepsPrivateValidationPromptAndSchemaAligned() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/pde-commercial-plan/v5/economics.md"));
    String agentCore =
        Files.readString(Path.of("src/main/resources/prompts/financial-agent/v1/agent-core.md"));
    var schema =
        objectMapper.readTree(
            Files.readString(
                Path.of(
                    "src/main/resources/prompts/pde-commercial-plan/v5/economics-schema.json")));

    assertThatCode(() -> objectMapper.readTree(schema.toString())).doesNotThrowAnyException();
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains("PDE_PRIVATE_ECONOMICS_V1", "checkout **simulado**", "commercialSpendAuthorized");
    org.assertj.core.api.Assertions.assertThat(agentCore)
        .contains("hipóteses numéricas explícitas", "isoladas de métricas realizadas");
    JsonNode properties = schema.path("properties").path("economics").path("properties");
    org.assertj.core.api.Assertions.assertThat(
            properties.path("maxBudgetBrl").path("const").asInt())
        .isZero();
    org.assertj.core.api.Assertions.assertThat(
            properties.path("commercialSpendAuthorized").path("const").asBoolean())
        .isFalse();
    org.assertj.core.api.Assertions.assertThat(
            properties.path("privateReadingsTarget").path("const").asInt())
        .isEqualTo(2);
  }

  /** Aceita antes da inferência um plano LIVE completo, vigente e da mesma versão Opala. */
  @Test
  void acceptsVersionedOpalaFinancialPlanBeforePaidCall() throws Exception {
    var context = objectMapper.readTree(opalaContext("READY", "fixture-v12", "2099-10-31"));

    assertThatCode(() -> PdeEconomicsBpmTaskConsumer.validateOpalaPlanContract(context))
        .doesNotThrowAnyException();
  }

  /** Bloqueia antes da inferência plano ausente, versão divergente ou janela vencida. */
  @Test
  void rejectsUnreadyOpalaFinancialPlanBeforePaidCall() throws Exception {
    var missing = objectMapper.readTree(opalaContext("MISSING", "fixture-v12", "2099-10-31"));
    var wrongVersion = objectMapper.readTree(opalaContext("READY", "fixture-v11", "2099-10-31"));
    var expired = objectMapper.readTree(opalaContext("READY", "fixture-v12", "2026-09-16"));

    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validateOpalaPlanContract(missing))
        .hasMessageContaining("plano financeiro LIVE");
    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validateOpalaPlanContract(wrongVersion))
        .hasMessageContaining("plano financeiro LIVE");
    assertThatThrownBy(() -> PdeEconomicsBpmTaskConsumer.validateOpalaPlanContract(expired))
        .hasMessageContaining("plano financeiro LIVE");
  }

  /** Impede que o modelo altere custos, contribuição, CAC ou reembolso calculados pelo backend. */
  @Test
  void rejectsOpalaApprovalDivergentFromDeterministicPlan() throws Exception {
    var context = objectMapper.readTree(opalaContext("READY", "fixture-v12", "2099-10-31"));
    var approved = objectMapper.readTree(result("APPROVE", 67, 42, 25, "2099-10-31"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) approved.path("economics"))
        .put("contributionMarginPercent", 37.31)
        .put("maxCacBrl", 15)
        .put("expectedRefundPercent", 12);
    var divergent = approved.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) divergent.path("economics"))
        .put("contributionPerSaleBrl", 24);

    assertThatCode(
            () -> PdeEconomicsBpmTaskConsumer.validateOpalaResultAgainstPlan(approved, context))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> PdeEconomicsBpmTaskConsumer.validateOpalaResultAgainstPlan(divergent, context))
        .hasMessageContaining("diverge");
  }

  /** Aceita o arredondamento comercial real da tarefa 437 sem incorporar CAC ao custo variável. */
  @Test
  void acceptsRoundedOpalaContributionBeforeCacFromTask437() throws Exception {
    var context = objectMapper.readTree(realisticOpalaContext());
    var approved = objectMapper.readTree(result("APPROVE", 67, 42, 24, "2099-10-31"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) approved.path("economics"))
        .put("variableCostPerSaleBrl", 42.82)
        .put("contributionPerSaleBrl", 24.18)
        .put("contributionMarginPercent", 36.08)
        .put("maxCacBrl", 15)
        .put("expectedRefundPercent", 12);

    assertThatCode(
            () -> PdeEconomicsBpmTaskConsumer.validateOpalaResultAgainstPlan(approved, context))
        .doesNotThrowAnyException();
  }

  /** Rejeita a dupla contagem que incorpora CAC ao custo e ainda o informa como limite separado. */
  @Test
  void rejectsOpalaContributionAfterCacAsUnitContribution() throws Exception {
    var context = objectMapper.readTree(realisticOpalaContext());
    var afterCac = objectMapper.readTree(result("APPROVE", 67, 57, 9, "2099-10-31"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) afterCac.path("economics"))
        .put("variableCostPerSaleBrl", 57.82)
        .put("contributionPerSaleBrl", 9.18)
        .put("contributionMarginPercent", 13.70)
        .put("maxCacBrl", 15)
        .put("expectedRefundPercent", 12);

    assertThatThrownBy(
            () -> PdeEconomicsBpmTaskConsumer.validateOpalaResultAgainstPlan(afterCac, context))
        .hasMessageContaining("diverge");
  }

  /** Monta o parecer financeiro mínimo usado nos testes de contrato. */
  private String result(
      String decision, int price, int variableCost, int contribution, String deadline) {
    return """
        {
          "decision":"%s",
          "scenarios":[{},{},{}],
          "economics":{
            "offerPriceBrl":%d,
            "variableCostPerSaleBrl":%d,
            "contributionPerSaleBrl":%d,
            "deadline":"%s"
          },
          "metrics":{},
          "rationale":"Números tratados como hipótese, sem autorizar gasto."
        }
        """
        .formatted(decision, price, variableCost, contribution, deadline);
  }

  /** Monta o contrato privado completo com controles comerciais parametrizáveis. */
  private String privateResult(boolean commercialSpendAuthorized, int readings, int maxBudget) {
    return """
        {
          "contractVersion":"PDE_PRIVATE_ECONOMICS_V1",
          "activity":"economics",
          "mode":"PRIVATE_VALIDATION_HYPOTHESIS",
          "decision":"APPROVE",
          "scenarios":[
            {"name":"Conservador","priceBrl":47,"variableCostBrl":12,"maxCacBrl":0,"targetSales":0,"benefit":"limite inferior","risk":"hipótese","recommended":false},
            {"name":"Base","priceBrl":67,"variableCostBrl":12,"maxCacBrl":0,"targetSales":0,"benefit":"teste equilibrado","risk":"hipótese","recommended":true},
            {"name":"Valor percebido","priceBrl":97,"variableCostBrl":15,"maxCacBrl":0,"targetSales":0,"benefit":"limite superior","risk":"hipótese","recommended":false}
          ],
          "economics":{
            "offerPriceBrl":67,
            "variableCostPerSaleBrl":12,
            "contributionPerSaleBrl":55,
            "contributionMarginPercent":82.09,
            "maxCacBrl":0,
            "fixedInitialCostBrl":0,
            "maxBudgetBrl":%d,
            "expectedTraffic":0,
            "expectedConversionPercent":0,
            "expectedRefundPercent":10,
            "targetSales":0,
            "targetRevenueBrl":0,
            "deadline":"2026-09-23",
            "commercialSpendAuthorized":%s,
            "privateReadingsTarget":%d
          },
          "metrics":{},
          "assumptions":["Preço é hipótese de checkout simulado."],
          "requiredChanges":[],
          "rationale":"Aprova somente duas leituras privadas sem venda ou gasto.",
          "sources":[]
        }
        """
        .formatted(maxBudget, commercialSpendAuthorized, readings);
  }

  /** Monta o contrato financeiro Opala usado para validar identidade e vigência. */
  private String opalaContext(String status, String planVersion, String validUntil) {
    return """
        {
          "opalaCommercial": {
            "productVersion":"fixture-v12",
            "priceBrl":67,
            "windowEnd":"2099-10-31T23:59:59Z",
            "financialPlan":{
              "status":"%s",
              "assumptions":{
                "productVersion":"%s",
                "validUntil":"%s",
                "evidence":"Taxas, tributos, entrega e uso local comprovados.",
                "priceBrl":67,
                "maximumCacBrl":15,
                "costs":{"refundPercent":12}
              },
              "deterministicEvaluation":{
                "status":"PROJECTED_VIABLE",
                "scenarios":[{"code":"BASE","viable":true,"contributionBeforeCacBrl":25,"contributionAfterCacBrl":10}]
              }
            }
          }
        }
        """
        .formatted(status, planVersion, validUntil);
  }

  /** Reproduz a precisão determinística do plano financeiro recebido pela tarefa 437. */
  private String realisticOpalaContext() {
    return """
        {
          "opalaCommercial": {
            "productVersion":"fixture-v12",
            "priceBrl":67,
            "windowEnd":"2099-10-31T23:59:59Z",
            "financialPlan":{
              "status":"READY",
              "assumptions":{
                "productVersion":"fixture-v12",
                "validUntil":"2099-10-31",
                "evidence":"Fontes auditadas.",
                "priceBrl":67,
                "maximumCacBrl":15,
                "costs":{"refundPercent":12}
              },
              "deterministicEvaluation":{
                "status":"PROJECTED_VIABLE",
                "scenarios":[{
                  "code":"BASE",
                  "viable":true,
                  "contributionBeforeCacBrl":24.1767,
                  "contributionAfterCacBrl":9.1767
                }]
              }
            }
          }
        }
        """;
  }
}
