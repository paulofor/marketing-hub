package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger os contratos BPM de construção do PDE executados por Dédalo. */
class PdeConstructionBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Resolve prompts e schemas específicos sem misturar contratos entre atividades. */
  @Test
  void resolvesVersionedResourcesByActivity() {
    assertThat(PdeConstructionBpmTaskConsumer.promptResourceFor("journey"))
        .isEqualTo("prompts/pde-construction/v1/journey.md");
    assertThat(PdeConstructionBpmTaskConsumer.schemaResourceFor("access"))
        .isEqualTo("prompts/pde-construction/v1/access-schema.json");
  }

  /** Aceita somente jornada com decisão comparada, cinco etapas e critérios verificáveis. */
  @Test
  void validatesCompleteJourney() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Contrato completo e coerente.",
             "selectedApproach":"Formulário guiado com entrega assistida completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "experienceContract":{"stages":[{},{},{},{},{}]}}
            """);

    PdeConstructionBpmTaskConsumer.validate(result, "journey");
  }

  /** Bloqueia pacote que não contém todos os grupos mínimos de entregáveis. */
  @Test
  void rejectsIncompleteDeliveryPackage() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Pacote parcial.",
             "selectedApproach":"Materiais editáveis com orientação guiada completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "deliveryPackage":{"assets":[{},{},{}]}}
            """);

    assertThatThrownBy(() -> PdeConstructionBpmTaskConsumer.validate(result, "deliverables"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Pacote");
  }

  /** Impede decisão de abordagem truncada pelo contrato estruturado. */
  @Test
  void rejectsTruncatedSelectedApproach() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"A jornada atende aos gates.",
             "selectedApproach":"sem","alternatives":[{},{},{}],
             "acceptanceCriteria":["critério"],
             "experienceContract":{"stages":[{},{},{},{},{}]}}
            """);

    assertThatThrownBy(() -> PdeConstructionBpmTaskConsumer.validate(result, "journey"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisão comparada");
  }
}
