package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;

/** Responsabilidade: resolver o destino privado autorizado pelas provas congeladas de Íris. */
final class FrozenCreativeVisualAuthorization {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(FrozenCreativeVisualAuthorization.class);

  /** Impede instâncias de um validador sem estado. */
  private FrozenCreativeVisualAuthorization() {}

  /** Reconhece os contratos existentes de produto e de ciclo sem consultar o cadastro mutável. */
  static String resolve(JsonNode input, String reference) {
    if (!input.isObject() || reference == null) return null;
    if (input.path("visualProofAuthorization").isMissingNode()
        && "LEARNING_CYCLE_PRIVATE".equals(input.path("mode").asText()))
      return privateCycleTarget(input, reference);
    JsonNode authorization = input.path("visualProofAuthorization");
    JsonNode destination = input.path("approvedDestination");
    JsonNode gate = input.path("validationGate");
    String prototypeVersion = authorization.path("prototypeVersion").asText();
    String authorizedUrl = authorization.path("publicUrl").asText();
    long productId = authorization.path("productId").asLong();
    boolean approvedArtifact =
        java.util.stream.StreamSupport.stream(
                input.path("approvedVisualArtifacts").spliterator(), false)
            .map(item -> item.path("result"))
            .anyMatch(
                result ->
                    "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1"
                            .equals(result.path("contractVersion").asText())
                        && "APPROVED".equals(result.path("decision").asText())
                        && productId == result.path("productId").asLong()
                        && prototypeVersion.equals(result.path("prototypeVersion").asText())
                        && authorizedUrl.equals(result.path("publicUrl").asText())
                        && authorization
                            .path("proofSourceReference")
                            .asText()
                            .equals(result.path("sourceReference").asText())
                        && java.util.stream.StreamSupport.stream(
                                result.path("artifacts").spliterator(), false)
                            .anyMatch(
                                artifact ->
                                    authorizedUrl.equals(artifact.path("sourceUrl").asText())
                                        && artifact.path("artifactId").asLong() > 0
                                        && artifact.path("sha256").asText().length() == 64));
    boolean valid =
        "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1"
                .equals(authorization.path("contractVersion").asText())
            && reference.equals(authorization.path("targetSourceReference").asText())
            && productId > 0
            && authorization.path("gateInstanceId").asLong() > 0
            && !prototypeVersion.isBlank()
            && !authorizedUrl.isBlank()
            && "PRIVATE_PDE_DESTINATION_V1".equals(destination.path("contractVersion").asText())
            && prototypeVersion.equals(destination.path("prototypeVersion").asText())
            && authorizedUrl.equals(destination.path("url").asText())
            && productId == gate.path("productId").asLong()
            && prototypeVersion.equals(gate.path("prototypeVersion").asText())
            && authorizedUrl.equals(gate.path("publicUrl").asText())
            && !gate.path("paymentEnabled").asBoolean(true)
            && !gate.path("publicationAuthorized").asBoolean(true)
            && !gate.path("campaignAuthorized").asBoolean(true)
            && approvedArtifact;
    return valid ? authorizedUrl : null;
  }

  /** Confere identidade, gate e homologação do ciclo privado que autorizou os pixels. */
  static String privateCycleTarget(JsonNode input, String reference) {
    String version = input.path("prototypeVersion").asText();
    var product = input.path("product");
    long productId = product.path("id").asLong();
    var destination = input.path("approvedDestination");
    String url = destination.path("url").asText();
    var gate = input.path("validationGate");
    var preparation = input.path("privateCreativePreparation");
    boolean valid =
        reference != null
            && reference.matches("experiment:[1-9][0-9]*")
            && "LEARNING_CYCLE_PRIVATE".equals(input.path("mode").asText())
            && "IRIS_INPUT_V1".equals(input.path("contractVersion").asText())
            && "AVAILABLE".equals(input.path("availability").asText())
            && "READY".equals(input.path("inputReadiness").asText())
            && reference.equals(input.path("sourceReference").asText())
            && input.path("cycleId").asLong() > 0
            && input.path("chainDefinitionId").asLong() > 0
            && input.path("gateInstanceId").asLong() > 0
            && productId > 0
            && !version.isBlank()
            && !url.isBlank()
            && version.equals(product.path("experienceVersion").asText())
            && url.equals(product.path("publicUrl").asText())
            && "PRIVATE_PDE_DESTINATION_V1".equals(destination.path("contractVersion").asText())
            && "APPROVED_PRIVATE_PDE".equals(destination.path("type").asText())
            && version.equals(destination.path("prototypeVersion").asText())
            && !destination.path("requiresLandingGeneration").asBoolean(true)
            && "PDE_AGENT_VALIDATION_GATE_V1".equals(gate.path("evidenceType").asText())
            && reference.equals(gate.path("sourceReference").asText())
            && productId == gate.path("productId").asLong()
            && version.equals(gate.path("prototypeVersion").asText())
            && url.equals(gate.path("publicUrl").asText())
            && "AGENT_VALIDATION".equals(gate.path("trafficClass").asText())
            && !gate.path("humanEvidenceClaimed").asBoolean(true)
            && !gate.path("commercialEvidenceClaimed").asBoolean(true)
            && !gate.path("paymentEnabled").asBoolean(true)
            && !gate.path("publicationAuthorized").asBoolean(true)
            && !gate.path("campaignAuthorized").asBoolean(true)
            && gate.path("mediaSpendAuthorizedBrl").isNumber()
            && gate.path("mediaSpendAuthorizedBrl").asDouble(-1) == 0
            && !input.path("paymentEnabled").asBoolean(true)
            && !input.path("publicationAuthorized").asBoolean(true)
            && !input.path("externalMediaSpendAuthorized").asBoolean(true)
            && "PDE_PRIVATE_CREATIVE_PREPARATION_V1"
                .equals(preparation.path("contractVersion").asText())
            && "PRIVATE_PREPARATION".equals(preparation.path("scope").asText())
            && reference.equals(preparation.path("sourceReference").asText())
            && version.equals(preparation.path("prototypeVersion").asText())
            && preparation.path("nonAudiovisualEvidenceRequired").asBoolean(false)
            && "INDEPENDENT_REVIEW"
                .equals(preparation.path("nonAudiovisualEvidencePurpose").asText())
            && "PROOF_CARD_V1".equals(preparation.path("nonAudiovisualTemplate").asText())
            && !preparation.path("publicationAuthorized").asBoolean(true)
            && !preparation.path("spendAuthorized").asBoolean(true)
            && !preparation.path("commercialEvidenceClaimed").asBoolean(true);
    if (!valid) return null;
    for (var entry : input.path("approvedUpstreamArtifacts")) {
      var proof = entry.path("result");
      long taskId = entry.path("taskId").asLong();
      boolean includedInGate =
          StreamSupport.stream(gate.path("taskEvidence").spliterator(), false)
              .anyMatch(
                  e ->
                      taskId > 0
                          && taskId == e.path("taskId").asLong()
                          && "technicalHomologation".equals(e.path("activityId").asText())
                          && "customer-agent".equals(e.path("agentKey").asText())
                          && e.path("resultSha256").asText().matches("[0-9a-f]{64}")
                          && e.path("resultSha256").asText().equals(proofHash(proof)));
      if (!includedInGate
          || !"customer-agent".equals(entry.path("agentKey").asText())
          || !"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(proof.path("contractVersion").asText())
          || !"APPROVED".equals(proof.path("decision").asText())
          || !reference.equals(proof.path("sourceReference").asText())
          || productId != proof.path("productId").asLong()
          || !version.equals(proof.path("prototypeVersion").asText())
          || !url.equals(proof.path("publicUrl").asText())) continue;
      for (var artifact : proof.path("artifacts"))
        if (artifact.path("artifactId").asLong() > 0
            && artifact.path("sha256").asText().matches("[0-9a-f]{64}")
            && url.equals(artifact.path("sourceUrl").asText())) return url;
    }
    return null;
  }

  /** Confirma o mesmo resultado técnico registrado no gate, sem aceitar uma prova alterada. */
  private static String proofHash(JsonNode proof) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(proof.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error(
          "SHA-256 indisponível ao conferir a prova visual congelada. sourceReference={}",
          proof.path("sourceReference").asText(),
          ex);
      throw new IllegalStateException("SHA-256 indisponível para a prova visual congelada.", ex);
    }
  }
}
