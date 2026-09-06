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
    assertThat(command)
        .doesNotContain(
            "--dangerously-bypass-approvals-and-sandbox", "PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
  }

  /** Confirma que o MCP temporário resolve as dependências na imagem ou no módulo local. */
  @Test
  void linksMcpToVersionedBrowserRuntime() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());

    Path server = runner.materializeMcp();
    try {
      assertThat(Files.readString(server))
          .contains("from 'playwright-core'", "from './video-frame-extractor.mjs'");
      assertThat(Files.readString(server.getParent().resolve("video-frame-extractor.mjs")))
          .contains("/usr/local/bin/ffmpeg", "/usr/local/bin/ffprobe");
      assertThat(Files.isSymbolicLink(server.getParent().resolve("node_modules"))).isTrue();
      Path dependencies = Files.readSymbolicLink(server.getParent().resolve("node_modules"));
      assertThat(dependencies.resolve("@modelcontextprotocol/sdk")).isDirectory();
    } finally {
      Files.deleteIfExists(server);
      Files.deleteIfExists(server.getParent().resolve("video-frame-extractor.mjs"));
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
    String schema = resource("prompts/meta-ad-approver/v2/review-schema.json");

    assertThat(schema)
        .contains("\"additionalProperties\": false", "\"correctionTargets\":")
        .doesNotContain("\"anyOf\"", "\"allOf\"", "\"oneOf\"");
  }

  /** Confirma que o prompt exige mídia, landing e segregação pelo MCP. */
  @Test
  void requiresAllVisualEvidenceThroughMcp() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v2/review.md");
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
            "extractRemoteVideoFrames(url, {",
            "decoder: 'FFMPEG_7_1_1'",
            "readOnlyHint: true",
            "openWorldHint: true",
            "destructiveHint: false");
    assertThat(mcp)
        .contains("/api/internal/agent-learning/v1/agents/meta-ad-approver/promoted")
        .doesNotContain("/promotion", "executablePath", "PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
  }

  /** Garante que baixa qualidade visual bloqueie e anteceda otimizações secundárias. */
  @Test
  void prioritizesBlockingVisualQualityGate() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v2/review.md");

    assertThat(prompt)
        .contains(
            "Gate de integridade",
            "Compare alegação, prova, produto real, landing, checkout e direitos",
            "CREATIVE_MEDIA",
            "LANDING",
            "responsável correto",
            "critério de aceite observável");
  }

  /** Exige que direitos de vídeo estejam ligados ao arquivo final, e não apenas declarados. */
  @Test
  void requiresVerifiedMediaGovernanceForVideo() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v2/review.md");

    assertThat(prompt)
        .contains(
            "mediaGovernanceEvidence",
            "status precisa ser `VERIFIED`",
            "URL e o SHA-256",
            "referência sintética",
            "licença comercial do provedor",
            "ligada a outra mídia mantém o gate fechado");
  }

  /** Garante que Têmis devolva critérios sem criar a alternativa que será revisada. */
  @Test
  void requiresReviewOnlyAndIndependentMaterialization() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v2/review.md");

    assertThat(prompt)
        .contains(
            "revisora independente de integridade comercial",
            "Você não cria copy, CTA, conceito, imagem, vídeo, landing ou produto",
            "Não escreva a solução substituta",
            "devem ser sempre strings vazias");
  }

  /** Garante que Têmis delegue requisitos visuais sem assumir a produção de Íris ou Apolo. */
  @Test
  void declaresExecutableMediaCapabilities() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v2/review.md");

    assertThat(prompt)
        .contains(
            "Íris materializa mídia estática ou Apolo materializa audiovisual",
            "Requisitos visuais obrigatórios",
            "descrever uma peça pronta",
            "escolher livremente a solução");
  }

  /** Mantém a correção de landing e mídia estática com Íris também no contrato legado. */
  @Test
  void assignsLegacyCommunicationCorrectionsToIris() throws Exception {
    String prompt = resource("prompts/meta-ad-approver/v1/review.md");

    assertThat(prompt)
        .contains("devolverá a causa para Íris")
        .contains("para que Íris ou")
        .contains("Apolo materialize uma solução nova")
        .doesNotContain("para que Dédalo ou");
  }

  /** Bloqueia resposta que tente devolver copy pronta sob o disfarce de parecer. */
  @Test
  void rejectsReplacementContentFromReviewer() throws Exception {
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
        {"decision":"ADJUST","summary":"Parecer completo","revisedHeadline":"Compre agora",
        "correctionTargets":[{"target":"CREATIVE_COPY","issueCode":"PROOF_MISMATCH",
        "requirement":"Alinhar a alegação à prova real disponível",
        "acceptanceCriterion":"Cada alegação aponta para evidência rastreável"}]}
        """);

    assertThatThrownBy(() -> validate.invoke(runner, value))
        .hasRootCauseMessage("Têmis não pode criar conteúdo substituto");
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
