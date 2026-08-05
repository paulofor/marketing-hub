package com.marketinghub.experimentstrategist.memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: garantir que dados pessoais nao sejam enviados para a memoria S3. */
class ExperimentStrategistAnonymizerTest {
  /** Remove identificadores comuns preservando o aprendizado comercial. */
  @Test
  void shouldRemovePersonalIdentifiers() {
    String result =
        new ExperimentStrategistAnonymizer()
            .anonymize(
                "Contato ana@example.com, CPF 123.456.789-00, telefone (11) 99876-5432, IP 10.0.0.8; hesitou no CTA.");

    assertThat(result)
        .doesNotContain("ana@example.com", "123.456.789-00", "99876-5432", "10.0.0.8")
        .contains("hesitou no CTA");
  }
}
