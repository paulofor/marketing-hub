package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
import com.marketinghub.communication.v1.PrivateCommunicationCreativeProof;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: contratar e conferir provas da interface comercial com copy privada rastreável.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialUiProof {
  public static final String CONTRACT = "OPALA_COMMERCIAL_UI_PROOF_V1";
  private final BusinessProcessActivityInstanceRepository instances;
  private final PrivateCommunicationCreativeProof privateProofs;
  private final AgentTaskRepository tasks;
  private final AgentTaskVisualEvidenceRepository artifacts;
  private final AgentTaskVisualEvidenceService visuals;
  private final ObjectMapper json;

  /** Oferece captura somente ao adaptador de primeiro valor local, sem imagens externas. */
  public JsonNode input(
      OpalaCommercialContext.Scope scope, OpalaCommercialContext.Candidate candidate) {
    var contract = candidate.productContract();
    var questions = contract.path("publicDiagnosticQuestions");
    var fields = new java.util.HashSet<String>();
    questions.forEach(q -> fields.add(q.path("key").asText()));
    if (!"DAYS_2_TO_7_AND_SUPPORT_MATERIALS"
            .equals(contract.path("commercialAccess").path("scope").asText())
        || !fields.equals(java.util.Set.of("occasion", "existingSelection"))
        || questions.size() != 2
        || contract.path("heroVideos").size() != 0) return json.nullNode();
    try {
      byte[] adapterBytes =
          new ClassPathResource(
                  "agent-behavior-files/landing-generator-agent-worker/src/main/resources/proofs/opala/v1/local-first-value-adapter.json")
              .getContentAsByteArray();
      var adapterContract = json.readTree(adapterBytes);
      if (!"MUSA_LOCAL_FIRST_VALUE_CAPTURE_V1"
              .equals(adapterContract.path("contractVersion").asText())
          || adapterContract.path("providerCallsAuthorized").asInt(-1) != 0
          || java.util.stream.StreamSupport.stream(
                  adapterContract.path("supportedExperienceVersions").spliterator(), false)
              .noneMatch(v -> scope.cycle().getProductVersion().equals(v.asText())))
        return json.nullNode();
      String reference = "experiment:" + scope.experiment().getId();
      var call =
          instances
              .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                  "pde-communication-sales-journey", reference)
              .stream()
              .filter(i -> "creatives".equals(i.getActivityDefinition().getActivityId()))
              .findFirst()
              .orElseThrow();
      require(
          "COMPLETED".equals(call.getStatus()) && call.isObjectiveAchieved(),
          "Falta o aceite da comunicação privada.");
      var proof = privateProofs.resolve(call, reference, scope.cycle().getProductVersion());
      require(
          proof.path("renderedAssets").size() == 1,
          "A comunicação privada precisa identificar uma peça exata.");
      var producer = tasks.findById(proof.path("producerTaskId").asLong()).orElseThrow();
      require(reference.equals(producer.getSourceReference()), "Copy de outra ocorrência.");
      var copy = json.readTree(producer.getResultJson()).path("functionalOutput").path("copy");
      require(
          !copy.path("headline").asText().isBlank() && !copy.path("primaryText").asText().isBlank(),
          "Falta a copy de Íris.");
      var result =
          json.createObjectNode()
              .put("contractVersion", CONTRACT)
              .put("adapter", "MUSA_LOCAL_FIRST_VALUE_V1")
              .put("productId", scope.cycle().getProductId())
              .put("cycleId", scope.cycle().getId())
              .put("experimentId", scope.experiment().getId())
              .put("productVersion", scope.cycle().getProductVersion())
              .put("destinationUrl", candidate.destinationUrl())
              .put("copySourceTaskId", producer.getId())
              .put(
                  "copySourceArtifactId",
                  proof.path("renderedAssets").get(0).path("artifactId").asLong());
      result.put("adapterContractSha256", sha(adapterBytes));
      result.put("adapterExperienceVersion", scope.cycle().getProductVersion());
      result.set("copy", copy);
      return result;
    } catch (Exception ex) {
      log.warn(
          "Prova comercial ainda sem entrada íntegra. productId={} experimentId={}",
          scope.cycle().getProductId(),
          scope.experiment().getId(),
          ex);
      return json.nullNode();
    }
  }

  /** Confere a captura persistida, a origem, o executor e a ausência de inferência externa. */
  public Content validate(
      OpalaCommercialContext.Scope scope,
      OpalaCommercialContext.Candidate candidate,
      AgentTask task,
      JsonNode receipt,
      JsonNode instruction) {
    try {
      JsonNode input = input(scope, candidate);
      require(
          input.isObject() && receipt.isObject(),
          "A captura comercial não possui entrada canônica.");
      for (String field :
          java.util.List.of(
              "contractVersion",
              "adapter",
              "productId",
              "cycleId",
              "experimentId",
              "productVersion",
              "destinationUrl",
              "copySourceTaskId",
              "copySourceArtifactId",
              "adapterContractSha256",
              "adapterExperienceVersion"))
        require(
            sameIdentityValue(input.path(field), receipt.path(field)),
            "A prova comercial diverge da entrada: " + field);
      for (String field : java.util.List.of("headline", "primaryText", "description"))
        require(
            input.path("copy").path(field).asText("").equals(instruction.path(field).asText("")),
            "A captura não pode reescrever a copy de Íris.");
      require(
          task != null
              && task.getAssignedAgent() != null
              && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
              && task.getProcessDefinition() != null
              && OpalaCommercialContext.CODE.equals(task.getProcessDefinition().getProcessCode())
              && "creative".equals(task.getProcessActivityId())
              && ("experiment:" + scope.experiment().getId()).equals(task.getSourceReference())
              && "DETERMINISTIC".equals(task.getExecutionMode())
              && CONTRACT.equals(task.getExecutionModelCode()),
          "Executor da prova comercial inválido.");
      require(
          receipt.path("testData").asBoolean()
              && !receipt.path("commercialEvidenceEligible").asBoolean(true)
              && "MUSA_LOCAL_RULES_V1".equals(receipt.path("model").asText())
              && "LOCAL".equals(receipt.path("serviceTier").asText())
              && receipt.path("inputTokens").isIntegralNumber()
              && receipt.path("inputTokens").asLong() == 0
              && receipt.path("outputTokens").isIntegralNumber()
              && receipt.path("outputTokens").asLong() == 0
              && receipt.path("costUsd").isNumber()
              && receipt.path("costUsd").decimalValue().signum() == 0
              && "OWN_PRODUCT_UI".equals(receipt.path("sourceType").asText())
              && !receipt.path("externalImagesIncluded").asBoolean(true)
              && receipt.path("responseSha256").asText().matches("[a-f0-9]{64}"),
          "A prova precisa ser identificada como teste local sem geração externa.");
      byte[] renderer =
          new ClassPathResource(
                  "agent-behavior-files/landing-generator-agent-worker/src/main/resources/proofs/opala/v1/capture-ui-proof.mjs")
              .getContentAsByteArray();
      require(
          sha(renderer).equals(receipt.path("rendererSha256").asText()),
          "A versão do capturador comercial diverge do código publicado.");
      var inspected = java.net.URI.create(receipt.path("inspectedUrl").asText());
      var expected = java.net.URI.create(candidate.destinationUrl());
      require(
          Objects.equals(inspected.getScheme(), expected.getScheme())
              && Objects.equals(inspected.getRawAuthority(), expected.getRawAuthority())
              && Objects.equals(
                  inspected.getPath().isEmpty() ? "/" : inspected.getPath(),
                  expected.getPath().isEmpty() ? "/" : expected.getPath())
              && "mh_preview=qa&pde_analytics=off".equals(inspected.getRawQuery())
              && inspected.getFragment() == null,
          "A página inspecionada saiu do destino candidato autorizado.");
      var artifact =
          artifacts
              .findByIdAndTaskId(receipt.path("artifactId").asLong(), task.getId())
              .orElseThrow();
      require(
          "COMMERCIAL-FIRST-VALUE".equals(artifact.getEvidenceKey())
              && "FOLD".equals(artifact.getEvidenceType())
              && candidate.destinationUrl().equals(artifact.getSourceUrl())
              && candidate.destinationUrl().equals(artifact.getFinalUrl())
              && artifact.getSha256().equals(receipt.path("sha256").asText()),
          "A captura persistida não corresponde à candidata e aos pixels declarados.");
      var content = visuals.read(task.getId(), artifact.getId());
      require(
          "image/png".equals(content.contentType())
              && sha(content.bytes()).equals(artifact.getSha256()),
          "Os pixels comerciais foram alterados.");
      var pixels = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(content.bytes()));
      require(
          pixels != null
              && pixels.getWidth() >= 600
              && pixels.getHeight() >= 600
              && receipt.path("pixelWidth").isIntegralNumber()
              && receipt.path("pixelHeight").isIntegralNumber()
              && pixels.getWidth() == receipt.path("pixelWidth").asInt()
              && pixels.getHeight() == receipt.path("pixelHeight").asInt(),
          "As dimensões da prova comercial não correspondem aos pixels.");
      return new Content(
          content.bytes(), artifact.getId(), artifact.getSha256(), (ObjectNode) input);
    } catch (Exception ex) {
      log.error(
          "Prova comercial inválida. productId={} experimentId={} taskId={}",
          scope.cycle().getProductId(),
          scope.experiment().getId(),
          task == null ? null : task.getId(),
          ex);
      throw new IllegalStateException("Não foi possível comprovar a captura comercial do PDE.", ex);
    }
  }

  /**
   * Compara IDs inteiros pelo valor transportado em JSON sem aceitar frações, overflow ou coerção
   * de texto.
   */
  private boolean sameIdentityValue(JsonNode expected, JsonNode received) {
    if (expected.isIntegralNumber())
      return received.isIntegralNumber()
          && received.canConvertToLong()
          && expected.longValue() == received.longValue();
    return expected.isTextual()
        && received.isTextual()
        && expected.textValue().equals(received.textValue());
  }

  /** Calcula o identificador imutável de um arquivo auditado. */
  private String sha(byte[] bytes) throws java.security.NoSuchAlgorithmException {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  /** Responsabilidade: transportar os pixels conferidos e sua origem canônica. */
  public record Content(byte[] bytes, Long artifactId, String sha256, ObjectNode input) {}
}
