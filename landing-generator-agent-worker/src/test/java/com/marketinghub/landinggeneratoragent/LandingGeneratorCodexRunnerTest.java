package com.marketinghub.landinggeneratoragent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    String mcp = Files.readString(Path.of("src/main/resources/mcp/landing-generator.mjs"));

    assertTrue(prompt.contains("aprendizado por reforço governado"));
    assertTrue(prompt.contains("Nunca copie"));
    assertTrue(schema.contains("referencePatternModels"));
    assertTrue(schema.contains("learningHypotheses"));
    assertTrue(prompt.contains("Agenda Cheia"));
    assertTrue(prompt.contains("três estratégias"));
    assertTrue(schema.contains("autonomousBacklog"));
    assertTrue(schema.contains("stopConditions"));
    assertTrue(prompt.contains("qual abordagem de geração de landing é a melhor"));
    assertTrue(schema.contains("generationApproachOptions"));
    assertTrue(schema.contains("selectedGenerationApproach"));
    assertTrue(prompt.contains("recuperar_estrategias_promovidas"));
    assertTrue(mcp.contains("/api/internal/agent-learning/v1/agents/landing-generator/promoted"));
    assertFalse(mcp.contains("/promotion"));
  }

  /** Deve impedir palavras-chave rejeitadas pelo Structured Outputs da OpenAI. */
  @Test
  void shouldKeepStrictOutputSchemaCompatibleWithOpenAi() throws Exception {
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/landing-generator/v1/remediation-schema.json"));

    assertFalse(schema.contains("\"uniqueItems\""));
    assertFalse(schema.contains("\"anyOf\""));
    assertFalse(schema.contains("\"oneOf\""));
    assertFalse(schema.contains("\"allOf\""));
  }

  /** Deve impedir que o modelo selecione uma abordagem sem executor no catálogo congelado. */
  @Test
  void shouldRejectUnavailableGenerationApproach() throws Exception {
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            new LandingGeneratorAgentProperties(),
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class));
    String decision =
        """
        {"approvalRecommendation":"REGENERATE_BEFORE_PUBLICATION","recommendedRegeneration":["LANDING_PAGE_HTML"],"acceptanceCriteria":["Quality Review independente"],"score":70,"strategyOptions":[{},{},{}],"selectedStrategy":{"name":"premium"},"autonomousBacklog":[{}],"generationApproachOptions":[{"approachCode":"GERALANDING_PIPELINE","available":true},{"approachCode":"COMPONENT_TEMPLATE_COMPOSER","available":true},{"approachCode":"CODEX_CODE_IMPLEMENTATION","available":true}],"selectedGenerationApproach":{"approachCode":"CODEX_CODE_IMPLEMENTATION"},"expectedMetrics":[{}],"stopConditions":{"continueWhen":["evolução"],"adjustWhen":["estagnação"],"stopWhen":["risco"]}}
        """;
    LandingAgentJob job =
        new LandingAgentJob(
            "job-88",
            88L,
            Map.of(
                "generationApproachCatalog",
                List.of(
                    Map.of("approachCode", "GERALANDING_PIPELINE", "available", true),
                    Map.of("approachCode", "CODEX_CODE_IMPLEMENTATION", "available", false))));

    assertThrows(
        IllegalArgumentException.class,
        () -> runner.validate(new ObjectMapper().readTree(decision), job));
  }
}
