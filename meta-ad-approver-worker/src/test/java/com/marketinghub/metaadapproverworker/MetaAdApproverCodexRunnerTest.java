package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar isolamento, contrato e gates do executor Codex. */
class MetaAdApproverCodexRunnerTest {
  /** Confirma internet, sandbox read-only, MCP próprio, modelo canônico e ausência de bypass. */
  @Test
  void forcesDedicatedReadOnlyCodexSandbox() throws Exception {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setRepositoryPath("/workspace/repository");
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(properties, new ObjectMapper());

    var command =
        runner.buildCommand(
            Path.of("/tmp/output.json"),
            Path.of("/tmp/schema.json"),
            Path.of("/tmp/mcp.mjs"),
            MetaAdReviewJob.from(Map.of("creativeId", 273, "experimentId", 88)));

    assertThat(command).containsSubsequence("codex", "--search", "exec", "-");
    assertThat(command).containsSubsequence("--sandbox", "read-only");
    assertThat(command).contains("approval_policy=\"never\"");
    assertThat(command).contains("--cd", "/workspace/repository", "--model", "gpt-5.6-sol");
    assertThat(command).contains("mcp_servers.meta_ad_approver.command=\"node\"");
    assertThat(command).anyMatch(value -> value.startsWith("mcp_servers.meta_ad_approver.args="));
    assertThat(command)
        .contains(
            "mcp_servers.meta_ad_approver.env={MCP_MARKETING_HUB_URL=\"http://backend:8000\",MCP_CREATIVE_ID=\"273\",MCP_EXPERIMENT_ID=\"88\",PLAYWRIGHT_BROWSERS_PATH=\"/ms-playwright\"}");
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Confirma que o MCP temporário resolve as dependências na imagem ou no módulo local. */
  @Test
  void linksMcpToVersionedBrowserRuntime() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());

    Path server = runner.materializeMcp();
    try {
      assertThat(Files.readString(server)).contains("from 'playwright-core'");
      assertThat(Files.isSymbolicLink(server.getParent().resolve("node_modules"))).isTrue();
      Path dependencies = Files.readSymbolicLink(server.getParent().resolve("node_modules"));
      assertThat(dependencies.resolve("@modelcontextprotocol/sdk")).isDirectory();
    } finally {
      Files.deleteIfExists(server);
      Files.deleteIfExists(server.getParent().resolve("node_modules"));
      Files.deleteIfExists(server.getParent());
    }
  }

  /** Confirma que aprovação abaixo da nota mínima nunca abre o gate. */
  @Test
  void rejectsApprovalBelowMinimumScore() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());
    Method validate =
        MetaAdApproverCodexRunner.class.getDeclaredMethod(
            "validate", com.fasterxml.jackson.databind.JsonNode.class);
    validate.setAccessible(true);
    var value =
        new ObjectMapper()
            .readTree(
                """
        {"decision":"APPROVED","summary":"Parecer completo","attentionScore":79,
        "clarityScore":90,"desireScore":90,"credibilityScore":90,"actionScore":90}
        """);

    assertThatThrownBy(() -> validate.invoke(runner, value))
        .hasRootCauseMessage("Aprovação com nota inferior a 80");
  }

  /** Impede que ajuste ou reprovação chegue ao backend sem tarefas executáveis. */
  @Test
  void rejectsAdjustmentWithoutConvergenceTargets() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());
    Method validate =
        MetaAdApproverCodexRunner.class.getDeclaredMethod(
            "validate", com.fasterxml.jackson.databind.JsonNode.class);
    validate.setAccessible(true);
    var value =
        new ObjectMapper()
            .readTree(
                """
        {"decision":"ADJUST","summary":"Parecer completo",
        "revisedImagePrompt":"Gerar imagem premium do produto",
        "mandatoryVisualRequirements":["Mostrar o produto"],
        "visualAcceptanceCriteria":["Produto legível em mobile"],
        "correctionTargets":[]}
        """);

    assertThatThrownBy(() -> validate.invoke(runner, value))
        .hasRootCauseMessage(
            "Ajuste ou reprovação sem correções verificáveis e responsáveis definidos");
  }

  /** Impede que uma aprovação carregue tarefas pendentes de convergência. */
  @Test
  void rejectsApprovalWithConvergenceTargets() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());
    Method validate =
        MetaAdApproverCodexRunner.class.getDeclaredMethod(
            "validate", com.fasterxml.jackson.databind.JsonNode.class);
    validate.setAccessible(true);
    var value =
        new ObjectMapper()
            .readTree(
                """
        {"decision":"APPROVED","summary":"Parecer completo","attentionScore":90,
        "clarityScore":90,"desireScore":90,"credibilityScore":90,"actionScore":90,
        "correctionTargets":[{"target":"LANDING","issueCode":"CTA_MISMATCH",
        "requirement":"Alinhar o CTA da página ao anúncio",
        "acceptanceCriterion":"Anúncio e página exibem o mesmo CTA"}]}
        """);

    assertThatThrownBy(() -> validate.invoke(runner, value))
        .hasRootCauseMessage("Aprovação não pode solicitar correções");
  }

  /** Mantém o schema aceito pelo Structured Outputs e delega a condição ao gate local. */
  @Test
  void keepsConditionalTargetsOutOfStrictSchema() throws Exception {
    String schema = resource("prompts/meta-ad-approver/v1/review-schema.json");

    assertThat(schema)
        .contains("\"additionalProperties\": false", "\"correctionTargets\":")
        .doesNotContain("\"anyOf\"", "\"allOf\"", "\"oneOf\"");
  }

  /** Confirma que o prompt exige mídia, landing e segregação pelo MCP. */
  @Test
  void requiresAllVisualEvidenceThroughMcp() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v1/review.md");
    String mcp = resource("mcp/meta-ad-approver.mjs");

    assertThat(prompt)
        .contains(
            "consultar_contexto",
            "inspecionar_midia",
            "inspecionar_landing",
            "recuperar_memoria_especializada",
            "recuperar_estrategias_promovidas",
            "registrar_aprendizado_candidato");
    assertThat(mcp)
        .contains(
            "MCP_CREATIVE_ID",
            "MCP_EXPERIMENT_ID",
            "StdioServerTransport",
            "/agent-review/context?experimentId=",
            "waitForCommercialLanding(page)",
            "text.length >= 200",
            "Preparando uma oferta especial para você...",
            "readOnlyHint: true",
            "openWorldHint: true",
            "destructiveHint: false");
    assertThat(mcp)
        .contains("/api/internal/agent-learning/v1/agents/meta-ad-approver/promoted")
        .doesNotContain("/promotion");
  }

  /** Garante que baixa qualidade visual bloqueie e anteceda otimizações secundárias. */
  @Test
  void prioritizesBlockingVisualQualityGate() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v1/review.md");

    assertThat(prompt)
        .contains(
            "Gate visual prioritário",
            "Antes de diagnosticar copy, CTA, público, oferta ou continuidade",
            "Esse gate é bloqueante e tem precedência sobre todas as demais otimizações",
            "CREATIVE_MEDIA",
            "LANDING",
            "não proponha ajustes secundários de copy, CTA ou segmentação",
            "Não confunda imagem tecnicamente carregada com imagem comercialmente aceitável");
  }

  /** Garante que Têmis crie uma alternativa completa e não aprove a própria proposta. */
  @Test
  void requiresCreativeProposalAndIndependentReview() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v1/review.md");

    assertThat(prompt)
        .contains(
            "responsável por criar e aprovar tecnicamente anúncios Meta",
            "proposta completa de anúncio pronta para materialização",
            "outro território criativo, outra cena e outra forma verdadeira de provar o produto",
            "nunca aprove na mesma execução aquilo que você acabou de criar");
  }

  /** Impede que Têmis proponha pós-produção ou referências ausentes do executor visual. */
  @Test
  void declaresExecutableMediaCapabilities() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v1/review.md");

    assertThat(prompt)
        .contains(
            "não recebe automaticamente arquivos reais do produto",
            "nunca peça ao modelo de imagem para renderizar palavras",
            "sem depender de texto dentro da imagem",
            "satisfeitos exclusivamente pela geração descrita no prompt");
  }

  /** Confirma que o job preserva o snapshot e os identificadores do experimento. */
  @Test
  void preservesJobSegregation() {
    Map<String, Object> context = new java.util.LinkedHashMap<>();
    context.put("creativeId", 88);
    context.put("experimentId", 7);
    context.put("cta", "COMPRAR");
    context.put("optionalEvidence", null);
    MetaAdReviewJob job = MetaAdReviewJob.from(context);

    assertThat(job.creativeId()).isEqualTo(88L);
    assertThat(job.experimentId()).isEqualTo(7L);
    assertThat(job.context()).containsEntry("cta", "COMPRAR");
    assertThat(job.context()).containsEntry("optionalEvidence", null);
  }

  /** Lê um recurso versionado usado pelo worker. */
  private String resource(String path) throws Exception {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
