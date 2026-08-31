package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        .isInstanceOf(java.time.format.DateTimeParseException.class);
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
}
