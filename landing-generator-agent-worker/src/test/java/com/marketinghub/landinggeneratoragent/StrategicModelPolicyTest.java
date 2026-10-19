package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Protege a precedência da atividade estratégica e os contratos operacionais existentes. */
class StrategicModelPolicyTest {
  /** Compara atividades do mesmo agente e impede roteamento pelo nome isolado da etapa. */
  @ParameterizedTest
  @CsvSource({
    "pde-commercial-plan-offer,productArchitecture,gpt-6-astra",
    "pde-construction-approval,journey,operational-test",
    "pde-construction-approval,deliverables,operational-test",
    "pde-construction-approval,prototypeCorrection,operational-test",
    "venda-entrega-satisfacao-cliente,materialization,operational-test",
    "other,productArchitecture,operational-test"
  })
  void routesOnlyExplicitContracts(String process, String activity, String expected) {
    var properties = new LandingGeneratorAgentProperties();
    properties.setModel("operational-test");
    assertThat(properties.modelForActivity(process, activity)).isEqualTo(expected);
  }

  /** Falha com configuração estratégica vazia sem impedir os trabalhos operacionais válidos. */
  @Test
  void rejectsMissingStrategicConfigurationWithoutSilentFallback() {
    var properties = new LandingGeneratorAgentProperties();
    properties.setModel("operational-test");
    properties.setStrategicModel(" ");
    assertThatThrownBy(properties::getStrategicModel).hasMessageContaining("CODEX_STRATEGIC_MODEL");
    assertThat(properties.modelForActivity("operational", "other")).isEqualTo("operational-test");
  }
}
