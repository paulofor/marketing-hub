package com.marketinghub.financialagentworker;

import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir regressão de contrato ao receber experimentos sucessores na fila. */
class PdeEconomicsBpmRoutingTest {
  /** Exercita a mesma jornada HTTP e Codex simulado usada na imagem empacotada. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "agent-successor",
        "agent-other",
        "agent-stale-envelope",
        "agent-human",
        "agent-legacy",
        "agent-budget",
        "opala",
        "successor",
        "discovery",
        "later-version",
        "legacy",
        "drift",
        "missing",
        "timestamp",
        "opala-timestamp",
        "contribution",
        "budget",
        "stop"
      })
  void processesVersionedEconomicsThroughOfficialCallbacks(String scenario) throws Exception {
    Path resources = Path.of("target/test-classes");
    if (!resources.resolve("bpm/fake-codex.mjs").toFile().setExecutable(true)) {
      throw new IllegalStateException("Modelo simulado não executável");
    }
    PdeEconomicsImageSmoke.runScenario(scenario, resources);
  }
}
