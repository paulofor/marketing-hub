package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: impedir atividades Opala sem consumidor ou contrato empacotado. */
class OpalaCommercialContractsTest {
  /** Cada tarefa técnica exige o catálogo do backend e mantém o schema empacotado no executor. */
  @ParameterizedTest
  @ValueSource(strings = {"entry", "creative", "checkout", "targeting"})
  void resolvesExecutableContract(String activity) throws Exception {
    String code = "opala-commercial-preparation-v1";
    assertThat(PdeConstructionBpmTaskConsumer.supportsContract(code, activity)).isTrue();
    assertThat(PdeConstructionBpmTaskConsumer.promptResourceFor(code, activity))
        .isEqualTo("catalogo-vivo:opala/" + activity);
    assertThat(new ClassPathResource("prompts/opala-commercial/v1/" + activity + ".md").exists())
        .isFalse();
    assertThat(
            new ClassPathResource(PdeConstructionBpmTaskConsumer.schemaResourceFor(code, activity))
                .exists())
        .isTrue();
  }
}
