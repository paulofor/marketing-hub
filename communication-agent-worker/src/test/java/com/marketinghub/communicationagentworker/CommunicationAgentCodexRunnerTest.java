package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger a fronteira, o contrato e a sandbox do executor de Íris. */
class CommunicationAgentCodexRunnerTest {
  private static final String STRATEGY_HASH = "a".repeat(64);
  private final ObjectMapper json = new ObjectMapper();

  /** Confirma pesquisa, MCP próprio, sandbox somente leitura e política não interativa. */
  @Test
  void shouldBuildReadOnlyCodexCommand() {
    CommunicationAgentProperties properties = properties();
    CommunicationAgentCodexRunner runner =
        new CommunicationAgentCodexRunner(properties, json, mock(CodexTelemetryReporter.class));

    var command =
        runner.command(
            Path.of("/tmp/answer.json"),
            Path.of("/tmp/schema.json"),
            Path.of("/tmp/communication-agent.mjs"));

    assertThat(command)
        .contains(
            "--search",
            "--skip-git-repo-check",
            "approval_policy=\"never\"",
            "model_reasoning_effort=\"high\"",
            "service_tier=\"default\"")
        .containsSubsequence("--sandbox", "read-only")
        .contains("mcp_servers.iris_communication.command=\"node\"")
        .anyMatch(value -> value.startsWith("mcp_servers.iris_communication.args="));
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Bloqueia Íris antes do modelo quando o tipo de raciocínio não está configurado. */
  @Test
  void shouldRejectMissingReasoningEffortBeforeModel() {
    CommunicationAgentProperties properties = properties();
    properties.setReasoningEffort("  ");
    CommunicationAgentCodexRunner runner =
        new CommunicationAgentCodexRunner(properties, json, mock(CodexTelemetryReporter.class));

    assertThatThrownBy(
            () ->
                runner.command(
                    Path.of("/tmp/answer.json"),
                    Path.of("/tmp/schema.json"),
                    Path.of("/tmp/communication-agent.mjs")))
        .hasMessageContaining("obrigatório para auditar Íris");
  }

  /** Mantém quarenta minutos como limite local e não como avanço do backend. */
  @Test
  void shouldKeepFortyMinuteTimeout() {
    CommunicationAgentProperties properties = new CommunicationAgentProperties();

    assertThat(properties.getCodexTimeout()).isEqualTo(Duration.ofMinutes(40));
    assertThat(properties.getServiceTier()).isEqualTo("default");
    assertThat(properties.getServiceTierExceptionReason()).contains("não anuncia Flex");
  }

  /** Impede que o runtime declare Flex quando o catálogo informa que ele será omitido. */
  @Test
  void shouldRejectUnadvertisedFlexTier() {
    CommunicationAgentProperties properties = properties();
    properties.setServiceTier("flex");
    CommunicationAgentCodexRunner runner =
        new CommunicationAgentCodexRunner(properties, json, mock(CodexTelemetryReporter.class));

    assertThatThrownBy(
            () ->
                runner.command(
                    Path.of("/tmp/answer.json"),
                    Path.of("/tmp/schema.json"),
                    Path.of("/tmp/communication-agent.mjs")))
        .hasMessageContaining("não for anunciado");
  }

  /** Reconhece somente os seis contratos publicados pertencentes à comunicação. */
  @Test
  void shouldMapOnlyIrisActivities() {
    assertThat(contract("pde-communication-sales-journey", "communicationContract").outputType())
        .isEqualTo("COMMUNICATION_PACKAGE");
    assertThat(contract("creative-production-approval", "nonAudiovisual").outputType())
        .isEqualTo("NON_AUDIOVISUAL_PACKAGE");
    assertThat(contract("landing-page-generation", "select").outputType())
        .isEqualTo("LANDING_EVIDENCE");
    assertThat(contract("landing-page-generation", "strategy").outputType())
        .isEqualTo("LANDING_STRATEGY");
    assertThat(contract("landing-page-generation", "compose").outputType())
        .isEqualTo("LANDING_COMPOSITION");
    assertThat(contract("landing-page-generation", "html").outputType()).isEqualTo("LANDING_HTML");
    assertThatThrownBy(() -> contract("pde-construction-approval", "journey"))
        .hasMessageContaining("não pertence");
  }

  /** Bloqueia estratégia, economia ou produto ausentes antes de iniciar o processo Codex. */
  @Test
  void shouldFailPreflightWithoutCompletePredecessors() {
    Map<String, Object> missingStrategy =
        task(
            "pde-communication-sales-journey",
            "communicationContract",
            "{\"marketStrategicContract\":{\"availability\":\"MISSING\"}}");
    Map<String, Object> missingProduct =
        task("pde-communication-sales-journey", "communicationContract", context("BLOCKED", true));

    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validateInput(missingStrategy))
        .hasMessageContaining("Contrato Estratégico");
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validateInput(missingProduct))
        .hasMessageContaining("Parecer econômico concluído de Plutus");
  }

  /** Exige prova visual aprovada antes de qualquer atividade da landing. */
  @Test
  void shouldRequireApprovedLandingEvidenceAtZeroModelCost() {
    Map<String, Object> noEvidence =
        task("landing-page-generation", "select", context("READY", false));
    Map<String, Object> withEvidence =
        task("landing-page-generation", "select", context("READY", true));

    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validateInput(noEvidence))
        .hasMessageContaining("prova visual aprovada");
    CommunicationAgentCodexRunner.validateInput(withEvidence);
  }

  /** Aceita pacote completo que preserva identidade, hash e todos os guardrails. */
  @Test
  void shouldAcceptCompleteCommunicationPackage() throws Exception {
    Map<String, Object> task =
        task("pde-communication-sales-journey", "communicationContract", context("READY", true));
    JsonNode result = result("COMMUNICATION_PACKAGE", "communicationContract", "COMPLETED");

    CommunicationAgentCodexRunner.validate(
        result, task, CommunicationAgentCodexRunner.contractFor(task));
  }

  /** Rejeita tentativa de devolver outro contrato estratégico ou concluir sem artefato. */
  @Test
  void shouldRejectChangedStrategyAndEmptyOutput() throws Exception {
    Map<String, Object> task =
        task("pde-communication-sales-journey", "communicationContract", context("READY", true));
    JsonNode changed = result("COMMUNICATION_PACKAGE", "communicationContract", "COMPLETED");
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.path("strategicContractReference"))
        .put("contentHash", "b".repeat(64));
    JsonNode empty = result("COMMUNICATION_PACKAGE", "communicationContract", "COMPLETED");
    ((com.fasterxml.jackson.databind.node.ObjectNode) empty.path("functionalOutput"))
        .put("messageStrategy", "");

    assertThatThrownBy(
            () ->
                CommunicationAgentCodexRunner.validate(
                    changed, task, CommunicationAgentCodexRunner.contractFor(task)))
        .hasMessageContaining("não preservou");
    assertThatThrownBy(
            () ->
                CommunicationAgentCodexRunner.validate(
                    empty, task, CommunicationAgentCodexRunner.contractFor(task)))
        .hasMessageContaining("incompleto");
  }

  /** Soma somente o maior acumulado medido e preserva cache como parte da entrada. */
  @Test
  void shouldReadCumulativeTokenUsage() throws Exception {
    Path log = Files.createTempFile("iris-token-test-", ".jsonl");
    try {
      Files.writeString(
          log,
          "{\"usage\":{\"input_tokens\":100,\"cached_input_tokens\":20,\"output_tokens\":30}}\n"
              + "{\"usage\":{\"inputTokens\":140,\"cachedInputTokens\":40,\"outputTokens\":50}}\n");
      CommunicationAgentCodexRunner runner =
          new CommunicationAgentCodexRunner(properties(), json, mock(CodexTelemetryReporter.class));

      CommunicationAgentCodexRunner.TokenUsage usage = runner.readTokenUsage(log);

      assertThat(usage.inputTokens()).isEqualTo(140);
      assertThat(usage.cachedInputTokens()).isEqualTo(40);
      assertThat(usage.outputTokens()).isEqualTo(50);
      assertThat(usage.informed()).isTrue();
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Confirma que comportamento e schema tornam a especialidade explícita e estrita. */
  @Test
  void shouldVersionBehaviorAndStrictSchema() throws Exception {
    String behavior = read("prompts/iris/v1/behavioral-core.md");
    JsonNode schema = json.readTree(read("prompts/iris/v1/output-schema.json"));

    assertThat(behavior)
        .contains(
            "o que a convence antes",
            "Dédalo",
            "Atena",
            "Plutus",
            "Apolo",
            "Psique",
            "Têmis",
            "Hermes",
            "exatamente três alternativas",
            "sensorial");
    assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
    assertThat(schema.path("required")).hasSize(16);
    assertThat(schema.toString())
        .contains("IRIS_COMMUNICATION_V1", "noFabricatedProof", "landingHtml");
  }

  /** Resolve um contrato curto a partir da identificação BPM informada. */
  private CommunicationAgentCodexRunner.Contract contract(String processCode, String activityId) {
    return CommunicationAgentCodexRunner.contractFor(
        Map.of("processCode", processCode, "activityId", activityId));
  }

  /** Monta a tarefa congelada usada pelos testes do contrato. */
  private Map<String, Object> task(String processCode, String activityId, String context) {
    return Map.of(
        "taskId",
        41L,
        "processCode",
        processCode,
        "processVersion",
        1,
        "activityId",
        activityId,
        "sourceReference",
        "commercial-plan:1@v2:journey",
        "processContextJson",
        context);
  }

  /** Monta contratos predecessores íntegros com ou sem prova aprovada. */
  private String context(String readiness, boolean withLandingEvidence) {
    String assets =
        withLandingEvidence
            ? "[{\"assetId\":7,\"assetUrl\":\"https://example.test/proof.png\",\"version\":2}]"
            : "[]";
    return """
        {
          "marketStrategicContract":{
            "availability":"AVAILABLE",
            "contractVersion":"MARKET_STRATEGY_V2",
            "contentHash":"%s"
          },
          "communicationMaterializationContext":{
            "availability":"AVAILABLE",
            "inputReadiness":"%s",
            "missingRequiredPredecessors":["Parecer econômico concluído de Plutus"],
            "approvedLandingAssets":%s
          }
        }
        """
        .formatted(STRATEGY_HASH, readiness, assets);
  }

  /** Cria uma resposta mínima completa conforme o schema único de Íris. */
  private JsonNode result(String outputType, String activityId, String status) throws Exception {
    return json.readTree(
        """
        {
          "contractVersion":"IRIS_COMMUNICATION_V1",
          "executionStatus":"%s",
          "sourceReference":"commercial-plan:1@v2:journey",
          "activityId":"%s",
          "outputType":"%s",
          "strategicContractReference":{"contractVersion":"MARKET_STRATEGY_V2","contentHash":"%s","preserved":true},
          "alternatives":[
            {"name":"A","benefit":"Clareza","risk":"Baixo","effort":"Médio","adherence":"Alta"},
            {"name":"B","benefit":"Desejo","risk":"Médio","effort":"Médio","adherence":"Alta"},
            {"name":"C","benefit":"Prova","risk":"Baixo","effort":"Baixo","adherence":"Alta"}
          ],
          "chosenAlternative":"A",
          "functionalOutput":{
            "messageStrategy":"Demonstrar o valor cotidiano do PDE.",
            "copy":{"primaryText":"Experiência simples.","headline":"Valor real","description":"Sem complexidade","ctaText":"Conhecer"},
            "channelBriefings":["Landing e Meta"],
            "staticAssets":[{"type":"SVG","format":"1080x1080","content":"Artefato estruturado","proofReference":"asset:7@v2"}],
            "emailSequence":[],
            "evidenceSelection":[{"reference":"asset:7","version":"2","purpose":"Prova do PDE"}],
            "conversionArchitecture":"Dor, mecanismo, prova, oferta e CTA.",
            "visualComposition":"Hierarquia fluida e contraste acessível.",
            "landingHtml":null,
            "audiovisualBrief":null
          },
          "evidenceGaps":[],
          "nextHandoff":"BACKEND",
          "guardrails":{"strategyPreserved":true,"productPreserved":true,"pricePreserved":true,"noFabricatedProof":true,"noPublication":true,"noExternalSpend":true},
          "expectedMetric":"Aprovação na primeira tentativa",
          "continueCriteria":"Aprovação cresce",
          "adjustCriteria":"Handoff vira gargalo",
          "stopCriteria":"Não há ganho mensurável"
        }
        """
            .formatted(status, activityId, outputType, STRATEGY_HASH));
  }

  /** Configura o runtime sem iniciar processo externo. */
  private CommunicationAgentProperties properties() {
    CommunicationAgentProperties properties = new CommunicationAgentProperties();
    properties.setBackendUrl("http://backend:8000");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setCodexCommand("codex");
    properties.setModel("gpt-5.6-sol");
    return properties;
  }

  /** Lê integralmente um recurso versionado do módulo. */
  private String read(String resource) throws Exception {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
