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
}
