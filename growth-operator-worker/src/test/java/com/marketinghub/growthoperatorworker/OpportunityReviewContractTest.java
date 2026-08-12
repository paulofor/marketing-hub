package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato versionado do parecer de Hermes. */
class OpportunityReviewContractTest {
  /** Confirma priorização e saída estruturada obrigatórias. */
  @Test
  void keepsVersionedReviewContract() throws Exception {
    String prompt = resource("prompts/opportunity-review/v1/review.md");
    String schema = resource("prompts/opportunity-review/v1/review-schema.json");
    assertThat(prompt).contains("{{DOSSIER_CONTEXT}}", "três prioridades", "SUPPORT");
    assertThat(schema).contains("decision", "rationale", "risks", "recommendation");
  }

  /** Lê o recurso empacotado exatamente como o worker o receberá. */
  private String resource(String path) throws Exception {
    try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
