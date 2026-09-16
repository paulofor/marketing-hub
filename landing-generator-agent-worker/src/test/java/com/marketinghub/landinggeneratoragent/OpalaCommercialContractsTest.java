package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: impedir atividades Opala sem consumidor ou contrato empacotado. */
class OpalaCommercialContractsTest {
  /** Cada tarefa técnica possui fila, prompt versionado e schema acessíveis ao executor. */
  @ParameterizedTest
  @ValueSource(strings = {"entry", "creative", "checkout", "targeting"})
  void resolvesExecutableContract(String activity) throws Exception {
    String code = "opala-commercial-preparation-v1";
    assertThat(PdeConstructionBpmTaskConsumer.supportsContract(code, activity)).isTrue();
    var prompt =
        new ClassPathResource(PdeConstructionBpmTaskConsumer.promptResourceFor(code, activity));
    assertThat(prompt.exists()).isTrue();
    assertThat(new String(prompt.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8))
        .contains("{{TASK_CONTEXT}}");
    assertThat(
            new ClassPathResource(PdeConstructionBpmTaskConsumer.schemaResourceFor(code, activity))
                .exists())
        .isTrue();
  }
}
