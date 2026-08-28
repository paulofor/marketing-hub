package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Responsabilidade: preservar o contrato histórico dos antigos pareceres de oportunidade. */
class OpportunityReviewContractTest {
  /** Confirma que o contrato histórico permanece auditável e explicitamente retirado. */
  @Test
  void keepsVersionedReviewContract() throws Exception {
    String prompt = resource("prompts/opportunity-review/v2/review.md");
    String schema = resource("prompts/opportunity-review/v2/review-schema.json");
    assertThat(prompt)
        .contains(
            "{{DOSSIER_CONTEXT}}",
            "{{PSIQUE_BEHAVIORAL_CORE_V3}}",
            "retirado da execução",
            "objeções",
            "SUPPORT",
            "amada",
            "sensoryExperience");
    assertThat(schema)
        .contains(
            "decision",
            "rationale",
            "risks",
            "recommendation",
            "behavioralResponse",
            "belongingAdmirationLove",
            "sensoryExperience");
  }

  /** Lê o recurso empacotado exatamente como o worker o receberá. */
  private String resource(String path) throws Exception {
    try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
