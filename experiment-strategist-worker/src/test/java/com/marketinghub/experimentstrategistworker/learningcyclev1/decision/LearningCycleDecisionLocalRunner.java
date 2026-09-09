package com.marketinghub.experimentstrategistworker.learningcyclev1.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategistworker.AutomaticExecutionControl;
import com.marketinghub.experimentstrategistworker.WorkerProperties;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Responsabilidade: homologar o worker real contra backend local e modelo explicitamente simulado.
 */
public final class LearningCycleDecisionLocalRunner {
  /** Impede criação de um runner exclusivo de linha de comando. */
  private LearningCycleDecisionLocalRunner() {}

  /** Executa somente a fila de decisão, sem iniciar outras rotinas ou usar credenciais reais. */
  public static void main(String[] args) throws Exception {
    var properties = new WorkerProperties();
    properties.setBackendUrl("http://127.0.0.1:18091");
    properties.setCodexCommand(
        Path.of("experiment-strategist-worker/src/test/resources/learningcycle/codex-fixture.py")
            .toAbsolutePath()
            .toString());
    properties.setModel("fixture-atena-no-external-model");
    properties.setCodexTimeout(Duration.ofSeconds(15));
    var runner = new LearningCycleDecisionRunner(properties, new ObjectMapper());
    var consumer =
        new LearningCycleDecisionConsumer(
            properties, runner, new AutomaticExecutionControl(properties.getBackendUrl()));
    do {
      consumer.processOne();
      if (args.length > 0 && "--once".equals(args[0])) return;
      Thread.sleep(300);
    } while (!Thread.currentThread().isInterrupted());
  }
}
