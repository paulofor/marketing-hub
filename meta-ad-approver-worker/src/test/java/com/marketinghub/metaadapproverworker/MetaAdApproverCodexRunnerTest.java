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

  /** Confirma que o MCP temporário consegue resolver o Playwright instalado no container. */
  @Test
  void linksMcpToVersionedBrowserRuntime() throws Exception {
    MetaAdApproverCodexRunner runner =
        new MetaAdApproverCodexRunner(new MetaAdApproverProperties(), new ObjectMapper());

    Path server = runner.materializeMcp();
    try {
      assertThat(Files.readString(server)).contains("from 'playwright-core'");
      assertThat(Files.isSymbolicLink(server.getParent().resolve("node_modules"))).isTrue();
      assertThat(Files.readSymbolicLink(server.getParent().resolve("node_modules")))
          .isEqualTo(Path.of("/app/node_modules"));
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
            "registrar_aprendizado_candidato");
    assertThat(mcp)
        .contains(
            "MCP_CREATIVE_ID",
            "MCP_EXPERIMENT_ID",
            "StdioServerTransport",
            "/agent-review/context?experimentId=",
            "readOnlyHint: true",
            "openWorldHint: true",
            "destructiveHint: false");
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
