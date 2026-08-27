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
    assertTrue(command.contains("model_reasoning_effort=\"high\""));
    assertTrue(command.stream().anyMatch(value -> value.contains("mcp_servers.landing_generator")));
  }

  /** Deve bloquear uma configuração sem esforço antes de iniciar qualquer chamada do Codex. */
  @Test
  void shouldRejectMissingReasoningEffortBeforeCodexExecution() {
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setReasoningEffort("  ");
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            properties,
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            mock(LandingHtmlCodexGenerator.class));

    assertThrows(
        IllegalStateException.class,
        () ->
            runner.command(
                Path.of("/tmp/out"),
                Path.of("/tmp/schema"),
                Path.of("/tmp/mcp"),
                new LandingAgentJob("job-88", 88L, Map.of())));
  }

  /** Deve executar o MCP no caminho instalado sem copiá-lo para fora do node_modules da imagem. */
  @Test
  void shouldUseInstalledMcpScript() throws Exception {
    Path installedMcp = Path.of("src/main/resources/mcp/landing-generator.mjs").toAbsolutePath();
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setMcpScriptPath(installedMcp.toString());
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            properties,
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            mock(LandingHtmlCodexGenerator.class));

    assertTrue(runner.requiredMcpScript().equals(installedMcp.normalize()));
  }

  /** Deve falhar cedo quando a imagem não contém o MCP no caminho configurado. */
  @Test
  void shouldRejectMissingInstalledMcpScript() {
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setMcpScriptPath("/tmp/landing-generator-mcp-inexistente.mjs");
    LandingGeneratorCodexRunner runner =
        new LandingGeneratorCodexRunner(
            properties,
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class),
            mock(LandingHtmlCodexGenerator.class));

    assertThrows(IllegalStateException.class, runner::requiredMcpScript);
  }

  /** Deve versionar modelagem sem cópia e retirar instruções globais abaixo do padrão aprovado. */
  @Test
  void shouldDeclareReferenceModelingWithoutLowValueGlobalSections() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/landing-generator/v1/remediation.md"));
    String normalizedPrompt = prompt.replaceAll("\\s+", " ");
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/landing-generator/v1/remediation-schema.json"));
    String mcp = Files.readString(Path.of("src/main/resources/mcp/landing-generator.mjs"));

    assertTrue(prompt.contains("Nunca copie"));
    assertTrue(schema.contains("referencePatternModels"));
    assertFalse(prompt.contains("aprendizado por reforço governado"));
    assertFalse(schema.contains("learningHypotheses"));
    assertFalse(prompt.contains("Quando o produto for **Agenda Cheia**"));
    assertTrue(prompt.contains("três estratégias"));
    assertTrue(schema.contains("autonomousBacklog"));
    assertTrue(schema.contains("stopConditions"));
    assertTrue(prompt.contains("qual abordagem de geração de landing é a melhor"));
    assertTrue(schema.contains("generationApproachOptions"));
    assertTrue(schema.contains("selectedGenerationApproach"));
    assertTrue(prompt.contains("recuperar_estrategias_promovidas"));
    assertTrue(prompt.contains("segunda interação dedicada"));
    assertTrue(prompt.contains("começando em `<!doctype html>` e terminando em `</html>`"));
    assertTrue(schema.contains("\"generatedHtml\":{\"type\":\"null\"}"));
    assertTrue(
        normalizedPrompt.contains("nomeie explicitamente quem entrega, quem revisa e quem aplica"));
    assertTrue(normalizedPrompt.contains("Todo item de `previousAttemptBlocks`"));
    assertTrue(normalizedPrompt.contains("approvedCreativeEvidence.status"));
    assertTrue(normalizedPrompt.contains("adCopy` ou `adImageBriefing` vazios"));
    assertTrue(mcp.contains("/api/internal/agent-learning/v1/agents/landing-generator/promoted"));
    assertFalse(mcp.contains("/promotion"));
  }

  /** Deve excluir HTML e auditoria bruta da decisão sem perder causas e score independente. */
  @Test
  void shouldBuildCompactPlanningPrompt() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    LandingAgentJob job =
        new LandingAgentJob(
            "job-88",
            88L,
            Map.of(
                "productName",
                "Rigel",
                "landingHtml",
                "<!doctype html><html><body>HTML_SENTINELA</body></html>",
                "qualityReview",
                Map.of(
                    "score",
                    90,
                    "blockingIssues",
                    List.of("CAUSA_SENTINELA"),
                    "qualityReviewAudit",
                    "AUDITORIA_BRUTA_SENTINELA")));

    String prompt = runner.buildPrompt(job);

    assertTrue(prompt.contains("baselineQualityReviewScore\":90"));
    assertTrue(prompt.contains("CAUSA_SENTINELA"));
    assertFalse(prompt.contains("HTML_SENTINELA"));
    assertFalse(prompt.contains("AUDITORIA_BRUTA_SENTINELA"));
  }

  /** Deve limitar HTML atual à materialização e continuar removendo auditoria bruta. */
  @Test
  void shouldKeepCurrentHtmlOnlyInMaterializationPrompt() throws Exception {
    LandingHtmlCodexGenerator generator =
        new LandingHtmlCodexGenerator(
            new LandingGeneratorAgentProperties(),
            new ObjectMapper(),
            mock(CodexTelemetryReporter.class));
    LandingAgentJob job =
        new LandingAgentJob(
            "job-88",
            88L,
            Map.of(
                "landingHtml",
                "<!doctype html><html><body>HTML_SENTINELA</body></html>",
                "qualityReview",
                Map.of(
                    "score",
                    90,
                    "blockingIssues",
                    List.of("CAUSA_SENTINELA"),
                    "qualityReviewAudit",
                    "AUDITORIA_BRUTA_SENTINELA")));

    String prompt =
        generator.buildPrompt(job, new ObjectMapper().readTree("{\"generatedHtml\":null}"));

    assertTrue(prompt.contains("HTML_SENTINELA"));
    assertTrue(prompt.contains("CAUSA_SENTINELA"));
    assertFalse(prompt.contains("AUDITORIA_BRUTA_SENTINELA"));
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
    List<String> command = generator.command(Path.of("/tmp/html-out"), Path.of("/tmp/html-schema"));
    assertTrue(command.contains("read-only"));
    assertTrue(command.contains("model_reasoning_effort=\"high\""));
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

  /** Deve rejeitar a landing quando ela troca os arquivos aprovados por uma representacao. */
  @Test
  void shouldRejectLandingWithoutMinimumApprovedProductAssets() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    String canonical = "https://checkout.example/agenda-cheia?ref=88";
    LandingAgentJob job = checkoutJobWithAssets(canonical);
    String html =
        "<a id=\"checkout-cta-primary\" href=\""
            + canonical
            + "\">Comprar</a><img src=\"https://assets.example/1.png\">"
            + " conteudo".repeat(80);

    assertThrows(IllegalArgumentException.class, () -> runner.validate(decision(html), job));
  }

  /** Deve rejeitar URLs aprovadas escondidas em metadado quando não existem imagens reais. */
  @Test
  void shouldRejectApprovedUrlsOutsideImageSources() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    String canonical = "https://checkout.example/agenda-cheia?ref=88";
    LandingAgentJob job = checkoutJobWithAssets(canonical);
    String html =
        "<a id=\"checkout-cta-primary\" href=\""
            + canonical
            + "\">Comprar</a><div data-assets=\"https://assets.example/1.png "
            + "https://assets.example/2.png https://assets.example/3.png "
            + "https://assets.example/4.png\">Sem imagens reais</div>"
            + " conteudo".repeat(80);

    assertThrows(IllegalArgumentException.class, () -> runner.validate(decision(html), job));
  }

  /** Deve aceitar a composicao que preserva quatro arquivos reais e o checkout congelado. */
  @Test
  void shouldAcceptLandingWithMinimumApprovedProductAssets() throws Exception {
    LandingGeneratorCodexRunner runner = runner();
    String canonical = "https://checkout.example/agenda-cheia?ref=88";
    LandingAgentJob job = checkoutJobWithAssets(canonical);
    String html =
        "<a id=\"checkout-cta-primary\" href=\""
            + canonical
            + "\">Comprar</a>"
            + "<img src=\"https://assets.example/1.png\">"
            + "<img src=\"https://assets.example/2.png\">"
            + "<img src=\"https://assets.example/3.png\">"
            + "<img src=\"https://assets.example/4.png\">"
            + " conteudo".repeat(80);

    runner.validate(decision(html), job);
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

  /** Cria um job que obriga o uso literal de quatro referencias aprovadas do produto. */
  private LandingAgentJob checkoutJobWithAssets(String canonical) {
    return new LandingAgentJob(
        "job-88",
        88L,
        Map.of(
            "generationApproachCatalog",
            List.of(Map.of("approachCode", "CODEX_CODE_IMPLEMENTATION", "available", true)),
            "checkoutContract",
            Map.of("canonicalUrl", canonical),
            "minimumApprovedLandingVisualAssets",
            4,
            "approvedLandingVisualAssets",
            List.of(
                Map.of("assetUrl", "https://assets.example/1.png"),
                Map.of("assetUrl", "https://assets.example/2.png"),
                Map.of("assetUrl", "https://assets.example/3.png"),
                Map.of("assetUrl", "https://assets.example/4.png"))));
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
