package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: impedir redução silenciosa de raciocínio nos três executores de Dédalo. */
class DedaloReasoningContractTest {
  /** Preserva o máximo no construtor de tarefas PDE e no valor enviado para auditoria. */
  @Test
  void usesMaximumInPdeTasksAndAudit() {
    var properties = new LandingGeneratorAgentProperties();
    var consumer =
        new PdeConstructionBpmTaskConsumer(
            properties, new ObjectMapper(), mock(AutomaticExecutionControl.class));

    List<String> command =
        ReflectionTestUtils.invokeMethod(
            consumer, "command", Path.of("out"), Path.of("log"), Path.of("schema"));

    assertThat(command).containsSubsequence("--config", "model_reasoning_effort=\"max\"");
    assertThat(properties.requiredReasoningEffort()).isEqualTo("max");
  }

  /** Bloqueia tarefa PDE, estratégia e HTML antes de executar um modelo sob esforço inferior. */
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "none", "low", "medium", "high", "xhigh"})
  void blocksEveryExecutorBelowMaximum(String effort) {
    var properties = new LandingGeneratorAgentProperties();
    properties.setReasoningEffort(effort);
    var json = new ObjectMapper();
    var telemetry = mock(CodexTelemetryReporter.class);
    var html = new LandingHtmlCodexGenerator(properties, json, telemetry);
    var strategy = new LandingGeneratorCodexRunner(properties, json, telemetry, html);
    var pde =
        new PdeConstructionBpmTaskConsumer(properties, json, mock(AutomaticExecutionControl.class));

    assertThatThrownBy(() -> html.command(Path.of("out"), Path.of("schema")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
    assertThatThrownBy(
            () ->
                strategy.command(
                    Path.of("out"),
                    Path.of("schema"),
                    Path.of("mcp"),
                    new LandingAgentJob("local-test", 1L, Map.of())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    pde, "command", Path.of("out"), Path.of("log"), Path.of("schema")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
  }
}
