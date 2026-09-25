package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a identidade semântica e estável da entrada inicial de Íris. */
class IrisCommunicationInputFingerprintTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Mantém o mesmo hash para mapas semanticamente iguais com ordens internas diferentes. */
  @Test
  void hashesEquivalentMapsIndependentlyOfIterationOrder() {
    Map<String, Object> first = new LinkedHashMap<>();
    first.put("sourceReference", "experiment:93");
    first.put("product", Map.of("id", 10L, "version", "mira-private-v3", "price", 49));
    first.put("checks", List.of(Map.of("name", "mobile", "approved", true)));
    Map<String, Object> second = new LinkedHashMap<>();
    second.put("checks", List.of(Map.of("approved", true, "name", "mobile")));
    second.put("product", Map.of("version", "mira-private-v3", "id", 10, "price", 49.0));
    second.put("sourceReference", "experiment:93");

    assertThat(IrisCommunicationInputFingerprint.hash(json, first))
        .isEqualTo(IrisCommunicationInputFingerprint.hash(json, second));
    assertThat(IrisCommunicationInputFingerprint.equivalent(json, first, second)).isTrue();
  }

  /** Ignora somente hash e artefatos autorreferentes, preservando toda mudança funcional. */
  @Test
  void ignoresSelfReferencesButDetectsFunctionalChanges() throws Exception {
    var current =
        json.readTree(
            """
            {"mode":"INITIAL_EXPERIMENT_PRIVATE","prototypeVersion":"mira-private-v3",
             "communicationInputHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
             "communicationArtifacts":[{"taskId":502}]}
            """);
    var audited =
        json.readTree(
            """
            {"communicationArtifacts":[],
             "communicationInputHash":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
             "prototypeVersion":"mira-private-v3","mode":"INITIAL_EXPERIMENT_PRIVATE"}
            """);

    assertThat(IrisCommunicationInputFingerprint.equivalent(json, current, audited)).isTrue();

    ((com.fasterxml.jackson.databind.node.ObjectNode) current)
        .put("prototypeVersion", "mira-private-v4");
    assertThat(IrisCommunicationInputFingerprint.equivalent(json, current, audited)).isFalse();
  }
}
