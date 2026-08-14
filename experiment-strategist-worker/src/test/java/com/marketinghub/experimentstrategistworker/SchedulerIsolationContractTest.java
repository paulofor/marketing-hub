package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o isolamento entre as filas agendadas do worker de Atena. */
class SchedulerIsolationContractTest {

  /** Garante uma thread independente para cada rotina bloqueante e para a reconexão. */
  @Test
  void configuresConcurrentSchedulerForIndependentQueues() throws Exception {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(application).contains("task:\n    scheduling:\n      pool:\n        size: 3");
  }
}
