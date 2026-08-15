package com.marketinghub.landinggeneratoragent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            properties,
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            mock(LandingHtmlCodexGenerator.class));

    List<String> command =
        runner.command(
            Path.of("/tmp/out"),
            Path.of("/tmp/schema"),
            Path.of("/tmp/mcp"),
            new LandingAgentJob("job-88", 88L, Map.of()));

    assertTrue(command.contains("--search"));
    assertTrue(command.contains("--json"));
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
    assertTrue(
        prompt.contains("não selecione essa abordagem se não puder entregar o HTML integral"));
    assertTrue(prompt.contains("começando em `<!doctype html>` e terminando em `</html>`"));
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

  /** Deve separar a decisão estratégica da materialização obrigatória do HTML integral. */
  @Test
  void shouldKeepDedicatedIntegralHtmlContract() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/landing-generator/v1/html.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/landing-generator/v1/html-schema.json"));
    LandingHtmlCodexGenerator generator =
        new LandingHtmlCodexGenerator(
            new LandingGeneratorAgentProperties(),
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class));

    assertTrue(prompt.contains("documento integral"));
    assertTrue(prompt.contains("checkoutContract.canonicalUrl"));
    assertTrue(schema.contains("\"required\":[\"generatedHtml\"]"));
    assertFalse(schema.contains("null"));
    assertTrue(
        generator
            .command(Path.of("/tmp/html-out"), Path.of("/tmp/html-schema"))
            .contains("read-only"));
  }

  /** Deve materializar automaticamente o HTML quando a decisão por código vier sem artefato. */
  @Test
  void shouldMaterializeMissingHtmlAfterCodeDecision() throws Exception {
    LandingHtmlCodexGenerator generator = mock(LandingHtmlCodexGenerator.class);
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            new LandingGeneratorAgentProperties(),
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            generator);
    var decision = decision("conteúdo ".repeat(80));
    ((com.fasterxml.jackson.databind.node.ObjectNode) decision).putNull("generatedHtml");
    LandingAgentJob job = checkoutJob("https://checkout.example/agenda-cheia?ref=88");
    String generatedHtml =
        "<!doctype html><html><body><a id=\"checkout-cta-primary\" href=\"https://checkout.example/agenda-cheia?ref=88\">Comprar</a>"
            + " conteúdo".repeat(80)
            + "</body></html>";
    when(generator.generate(job, decision))
        .thenReturn(new LandingHtmlCodexGenerator.GeneratedHtml(generatedHtml, null));

    runner.materializeHtmlIfNeeded(decision, job);

    assertTrue(decision.path("generatedHtml").asText().startsWith("<!doctype html>"));
    verify(generator).generate(job, decision);
  }

  /** Deve impedir que o modelo selecione uma abordagem sem executor no catálogo congelado. */
  @Test
  void shouldRejectUnavailableGenerationApproach() throws Exception {
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            new LandingGeneratorAgentProperties(),
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            mock(LandingHtmlCodexGenerator.class));
    String decisionTemplate =
        """
        {"approvalRecommendation":"REGENERATE_BEFORE_PUBLICATION","recommendedRegeneration":["LANDING_PAGE_HTML"],"acceptanceCriteria":["Quality Review independente"],"score":70,"strategyOptions":[{},{},{}],"selectedStrategy":{"name":"premium"},"autonomousBacklog":[{}],"generationApproachOptions":[{"approachCode":"GERALANDING_PIPELINE","available":true},{"approachCode":"COMPONENT_TEMPLATE_COMPOSER","available":true},{"approachCode":"CODEX_CODE_IMPLEMENTATION","available":true}],"selectedGenerationApproach":{"approachCode":"CODEX_CODE_IMPLEMENTATION"},"generatedHtml":"<!doctype html><html><body>%s</body></html>","expectedMetrics":[{}],"stopConditions":{"continueWhen":["evolução"],"adjustWhen":["estagnação"],"stopWhen":["risco"]}}
        """;
    String decision = decisionTemplate.formatted("conteúdo ".repeat(80));
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

  /** Deve aceitar somente a URL canônica literal em todos os CTAs marcados. */
  @Test
  void shouldRejectGeneratedHtmlWithDivergentCheckoutBeforeCallback() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    String canonical = "https://checkout.example/agenda-cheia?ref=88";
    LandingAgentJob job = checkoutJob(canonical);
    String invalidHtml =
        "<a id=\"checkout-cta-primary\" href=\"#checkout\">Comprar</a>" + " conteúdo".repeat(80);

    assertThrows(IllegalArgumentException.class, () -> runner.validate(decision(invalidHtml), job));
  }

  /** Deve permitir múltiplos CTAs quando todos copiam literalmente o contrato congelado. */
  @Test
  void shouldAcceptGeneratedHtmlWithCanonicalCheckoutOnEveryMarkedCta() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    String canonical = "https://checkout.example/agenda-cheia?ref=88";
    String html =
        "<a id=\"checkout-cta-primary\" href=\""
            + canonical
            + "\">Comprar</a><a href=\""
            + canonical
            + "\" data-analytics-role=\"primary-checkout\">Quero agora</a>"
            + " conteúdo".repeat(80);

    runner.validate(decision(html), checkoutJob(canonical));
  }

  /** Cria o runner isolado para os contratos de saída do Codex. */
  private LandingGeneratorCodexRunner runner() {
    return new LandingGeneratorCodexRunner(
        new LandingGeneratorAgentProperties(),
        new ObjectMapper(),
        mock(CodexTelemetryReporter.class),
        mock(LandingHtmlCodexGenerator.class));
  }

  /** Cria um job com catálogo disponível e checkout canônico congelado. */
  private LandingAgentJob checkoutJob(String canonical) {
    return new LandingAgentJob(
        "job-88",
        88L,
        Map.of(
            "generationApproachCatalog",
            List.of(Map.of("approachCode", "CODEX_CODE_IMPLEMENTATION", "available", true)),
            "checkoutContract",
            Map.of("canonicalUrl", canonical)));
  }

  /** Cria uma decisão mínima válida para exercitar o contrato de checkout. */
  private com.fasterxml.jackson.databind.JsonNode decision(String html) throws Exception {
    String value =
        """
        {"approvalRecommendation":"REGENERATE_BEFORE_PUBLICATION","recommendedRegeneration":["LANDING_PAGE_HTML"],"acceptanceCriteria":["Quality Review independente"],"score":70,"strategyOptions":[{},{},{}],"selectedStrategy":{"name":"premium"},"autonomousBacklog":[{}],"generationApproachOptions":[{"approachCode":"CODEX_CODE_IMPLEMENTATION","available":true},{"approachCode":"GERALANDING_PIPELINE","available":true},{"approachCode":"COMPONENT_TEMPLATE_COMPOSER","available":false}],"selectedGenerationApproach":{"approachCode":"CODEX_CODE_IMPLEMENTATION"},"generatedHtml":%s,"expectedMetrics":[{}],"stopConditions":{"continueWhen":["evolução comprovada"],"adjustWhen":["estagnação comprovada"],"stopWhen":["risco comercial"]}}
        """
            .formatted(
                new ObjectMapper()
                    .writeValueAsString("<!doctype html><html><body>" + html + "</body></html>"));
    return new ObjectMapper().readTree(value);
  }
}
