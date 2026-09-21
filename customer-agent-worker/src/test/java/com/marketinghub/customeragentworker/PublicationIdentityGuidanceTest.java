package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: direcionar impedimentos de publicação sem repetir revisão paga ou geração. */
class PublicationIdentityGuidanceTest {
  /** Orienta a página do experimento correto mesmo se a exceção visual estiver encapsulada. */
  @Test
  void pointsToAuditedRecoveryWithoutSuggestingAnotherGeneration() {
    var consumer = consumer();
    for (long id : new long[] {701, 9843}) {
      var result =
          consumer.failureGuidance(
              Map.of("sourceReference", "experiment:" + id),
              new IllegalStateException(
                  new BpmVisualEvidenceRunner.PublicationIdentityException("origem ausente")));
      assertThat(result.get("category")).isEqualTo("MISSING_EVIDENCE");
      assertThat(result.get("recommendedAction").toString())
          .contains("Reenviar página aprovada", "não refaça", "aguarde a atualização pública");
      assertThat(result.get("helpLinks").toString())
          .contains("/experiments/" + id + "#sales-page-publication");
    }
  }

  /** Mantém a orientação de storage para falhas que não envolvem identidade de publicação. */
  @Test
  void preservesOtherVisualAndTechnicalBlockers() {
    var consumer = consumer();
    assertThat(
            consumer
                .failureGuidance(
                    Map.of(),
                    new BpmVisualEvidenceRunner.VisualEvidenceException("storage ausente"))
                .get("recommendedAction")
                .toString())
        .contains("storage privado")
        .doesNotContain("Reenviar página");
    assertThat(
            consumer.failureGuidance(Map.of(), new IllegalStateException("rede")).get("category"))
        .isEqualTo("TECHNICAL_FAILURE");
    assertThat(
            consumer
                .failureGuidance(
                    Map.of("sourceReference", "product:701"),
                    new BpmVisualEvidenceRunner.PublicationIdentityException("origem ausente"))
                .get("helpLinks")
                .toString())
        .doesNotContain("/experiments/");
  }

  /** Cria o consumidor sem agendar polling nem iniciar modelo externo. */
  private CustomerBpmTaskConsumer consumer() {
    return new CustomerBpmTaskConsumer(
        "http://127.0.0.1:1", "unused-codex", "test-model", "max", "/tmp", "", new ObjectMapper());
  }
}
