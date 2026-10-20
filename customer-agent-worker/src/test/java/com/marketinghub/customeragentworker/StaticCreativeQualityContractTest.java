package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: reproduzir aprovação de fixture e preservar peças adequadas em outros
 * contextos.
 */
class StaticCreativeQualityContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Recusa a mesma classe de falha do caso real, incluindo nota insuficiente e prova ausente. */
  @ParameterizedTest
  @ValueSource(longs = {1042, 910042})
  void rejectsPlaceholderAndWeakCraftsmanshipWithoutRejectingValidIllustration(long id) {
    var image =
        json.convertValue(
            Map.of("id", id, "evidenceType", "CREATIVE_RENDER", "sha256", "a".repeat(64)),
            BpmVisualEvidenceBackendClient.UploadedVisualEvidence.class);
    var result = json.createObjectNode().put("decision", "APPROVED");
    var entry =
        result
            .putArray("renderedAssetAudit")
            .addObject()
            .put("artifactId", id)
            .put("sha256", "a".repeat(64));
    assertThatThrownBy(() -> StaticCreativeQualityContract.validate(result, List.of(image)))
        .hasMessageContaining("fundamentada");
    ObjectNode quality =
        entry
            .putObject("staticQuality")
            .put("sourceKind", "TECHNICAL_PLACEHOLDER")
            .put("craftsmanship", 3)
            .put("desireConnection", 3)
            .put("productClarity", 4)
            .put(
                "assessment",
                "Desenho esquemático de roupa, sem textura, apresentado como destaque.");
    assertThatThrownBy(() -> StaticCreativeQualityContract.validate(result, List.of(image)))
        .hasMessageContaining("Fixture técnica");
    quality.put("sourceKind", "EDITORIAL_ILLUSTRATION").put("craftsmanship", 2);
    assertThatThrownBy(() -> StaticCreativeQualityContract.validate(result, List.of(image)))
        .hasMessageContaining("craftsmanship");
    quality.put("craftsmanship", 4).put("desireConnection", 2);
    assertThatThrownBy(() -> StaticCreativeQualityContract.validate(result, List.of(image)))
        .hasMessageContaining("desireConnection");
    quality
        .put("desireConnection", 4)
        .put(
            "assessment",
            "Roupas com proporção e textura legíveis, composição vinculada à ocasião; ilustração identificada.");
    StaticCreativeQualityContract.validate(result, List.of(image));
    quality.put("sourceKind", "FUNCTIONAL_CAPTURE");
    StaticCreativeQualityContract.validate(result, List.of(image));
    quality.put("sourceKind", "TECHNICAL_PLACEHOLDER").put("craftsmanship", 1);
    result.put("decision", "ADJUST");
    StaticCreativeQualityContract.validate(result, List.of(image));
    entry.put("sha256", "b".repeat(64));
    assertThatThrownBy(() -> StaticCreativeQualityContract.validate(result, List.of(image)))
        .hasMessageContaining("identidade");
  }
}
