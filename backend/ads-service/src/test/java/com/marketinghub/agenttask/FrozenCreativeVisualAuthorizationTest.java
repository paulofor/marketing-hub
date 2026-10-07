package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: preservar identidade e autoridade dos pixels aprovados no próprio ciclo. */
class FrozenCreativeVisualAuthorizationTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz a entrada real que não contém o formato antigo de reautorização de produto. */
  @Test
  void acceptsArchivedCycleProofWithoutInventingLegacyAuthorization() throws Exception {
    var input = fixture(json);
    assertThat(input.has("visualProofAuthorization")).isFalse();
    assertThat(FrozenCreativeVisualAuthorization.resolve(input, "experiment:102"))
        .isEqualTo(input.path("approvedDestination").path("url").asText());
  }

  /** Prova que a regra acompanha outra identidade, sem exceção por nome ou identificador. */
  @Test
  void acceptsDifferentProductCycleAndExperimentWithMatchingGate() throws Exception {
    var input = fixture(json);
    input
        .put("sourceReference", "experiment:91099")
        .put("cycleId", 91009)
        .put("chainDefinitionId", 91026);
    ((ObjectNode) input.path("product")).put("id", 91010);
    ((ObjectNode) input.path("privateCreativePreparation"))
        .put("sourceReference", "experiment:91099");
    var gate = (ObjectNode) input.path("validationGate");
    gate.put("sourceReference", "experiment:91099").put("productId", 91010);
    var proof = (ObjectNode) input.path("approvedUpstreamArtifacts").get(0).path("result");
    proof.put("sourceReference", "experiment:91099").put("productId", 91010);
    ((ObjectNode) gate.path("taskEvidence").get(0)).put("resultSha256", hash(proof));
    assertThat(FrozenCreativeVisualAuthorization.resolve(input, "experiment:91099"))
        .isEqualTo(input.path("approvedDestination").path("url").asText());
  }

  /**
   * Recusa divergências reais, ausência de prova e qualquer autorização comercial no regime
   * privado.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "product",
        "reference",
        "version",
        "url",
        "gate",
        "technical",
        "technicalHash",
        "sourceHash",
        "publication",
        "payment",
        "campaign",
        "spend",
        "missingFlag",
        "mode",
        "preparation"
      })
  void rejectsInvalidAuthority(String change) throws Exception {
    var input = fixture(json);
    var gate = (ObjectNode) input.path("validationGate");
    var proof = (ObjectNode) input.path("approvedUpstreamArtifacts").get(0).path("result");
    switch (change) {
      case "product" -> ((ObjectNode) input.path("product")).put("id", 91010);
      case "reference" -> input.put("sourceReference", "experiment:91099");
      case "version" -> input.put("prototypeVersion", "different-private-version");
      case "url" ->
          ((ObjectNode) input.path("approvedDestination"))
              .put("url", "https://example.com/foreign");
      case "gate" -> input.remove("gateInstanceId");
      case "technical" -> proof.put("decision", "REJECTED");
      case "technicalHash" ->
          ((ObjectNode) gate.path("taskEvidence").get(0)).put("resultSha256", "a".repeat(64));
      case "sourceHash" -> ((ObjectNode) proof.path("artifacts").get(0)).put("sha256", "invalid");
      case "publication" -> input.put("publicationAuthorized", true);
      case "payment" -> gate.put("paymentEnabled", true);
      case "campaign" -> gate.put("campaignAuthorized", true);
      case "spend" -> gate.put("mediaSpendAuthorizedBrl", 1);
      case "missingFlag" -> gate.remove("publicationAuthorized");
      case "mode" -> input.put("mode", "UNKNOWN");
      case "preparation" ->
          ((ObjectNode) input.path("privateCreativePreparation")).put("spendAuthorized", true);
      default -> throw new IllegalArgumentException(change);
    }
    assertThat(FrozenCreativeVisualAuthorization.resolve(input, "experiment:102")).isNull();
  }

  /** Carrega o contrato congelado real dentro da matriz compartilhada existente. */
  static ObjectNode fixture(ObjectMapper json) throws Exception {
    for (var directory = Path.of("").toAbsolutePath();
        directory != null;
        directory = directory.getParent()) {
      var path =
          directory.resolve("infra/testing/mira-creative-recovery/frozen-cycle-visual-input.json");
      if (Files.exists(path)) return (ObjectNode) json.readTree(path.toFile());
    }
    throw new IllegalStateException("Fixture da prova visual do ciclo ausente.");
  }

  /** Gera o hash do resultado alterado somente para a identidade sintética do teste. */
  private String hash(JsonNode value) throws Exception {
    return java.util.HexFormat.of()
        .formatHex(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
