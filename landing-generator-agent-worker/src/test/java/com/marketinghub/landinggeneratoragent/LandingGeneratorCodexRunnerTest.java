package com.marketinghub.landinggeneratoragent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida os limites estruturais do processo Codex do Agente Gerador de Landing. */
class LandingGeneratorCodexRunnerTest {
  /** Deve fixar modelo, sandbox, pesquisa e MCP exclusivo em toda execução. */
  @Test
  void shouldBuildPremiumCodexCommand() {
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            properties, new ObjectMapper(), mock(CodexTelemetryReporter.class));

    List<String> command =
        runner.command(
            Path.of("/tmp/out"),
            Path.of("/tmp/schema"),
            Path.of("/tmp/mcp"),
            new LandingAgentJob("job-88", 88L, Map.of()));

    assertTrue(command.contains("--search"));
    assertTrue(command.contains("read-only"));
    assertTrue(command.contains("gpt-5.6-sol"));
    assertTrue(command.stream().anyMatch(value -> value.contains("mcp_servers.landing_generator")));
  }

  /** Deve versionar modelagem sem cópia e aprendizado por recompensa independente. */
  @Test
  void shouldDeclareReferenceModelingAndReinforcementContract() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/landing-generator/v1/remediation.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/landing-generator/v1/remediation-schema.json"));

    assertTrue(prompt.contains("aprendizado por reforço governado"));
    assertTrue(prompt.contains("Nunca copie"));
    assertTrue(schema.contains("referencePatternModels"));
    assertTrue(schema.contains("learningHypotheses"));
    assertTrue(prompt.contains("Agenda Cheia"));
    assertTrue(prompt.contains("três estratégias"));
    assertTrue(schema.contains("autonomousBacklog"));
    assertTrue(schema.contains("stopConditions"));
  }
}
