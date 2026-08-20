package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Valida a leitura auditável do consumo real informado pelo processo Codex. */
class CodexTelemetryReporterTest {
  /** Deve extrair a última medição cumulativa dos eventos oficiais em JSONL. */
  @Test
  void shouldReadLatestOfficialTokenUsage() throws Exception {
    Path output = Files.createTempFile("codex-events-", ".jsonl");
    Files.writeString(
        output,
        """
        WARN mensagem operacional emitida no stderr
        {"type":"item.completed","item":{"type":"agent_message"}}
        {"type":"turn.completed","usage":{"input_tokens":1234,"input_tokens_details":{"cached_tokens":700},"output_tokens":321}}
        """);
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    CodexTelemetryReporter.TokenUsage usage = reporter.readTokenUsage(output);

    assertThat(usage.inputTokens()).isEqualTo(1234);
    assertThat(usage.cachedInputTokens()).isEqualTo(700);
    assertThat(usage.outputTokens()).isEqualTo(321);
    Files.deleteIfExists(output);
  }

  /** Deve manter nulos quando nenhum evento informou usage, sem criar estimativa. */
  @Test
  void shouldKeepTokensUnknownWithoutUsageEvent() throws Exception {
    Path output = Files.createTempFile("codex-events-", ".jsonl");
    Files.writeString(output, "{\"type\":\"thread.started\"}\n");
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    CodexTelemetryReporter.TokenUsage usage = reporter.readTokenUsage(output);

    assertThat(usage.inputTokens()).isNull();
    assertThat(usage.outputTokens()).isNull();
    Files.deleteIfExists(output);
  }

  /** Deve separar tentativas distintas e preservar a identidade em uma retomada da mesma lease. */
  @Test
  void shouldUseStableTelemetryIdentityPerTechnicalExecution() {
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    assertThat(reporter.executionTelemetryId("execution-a"))
        .isEqualTo(reporter.executionTelemetryId("execution-a"));
    assertThat(reporter.executionTelemetryId("execution-a"))
        .isNotEqualTo(reporter.executionTelemetryId("execution-b"));
  }
}
