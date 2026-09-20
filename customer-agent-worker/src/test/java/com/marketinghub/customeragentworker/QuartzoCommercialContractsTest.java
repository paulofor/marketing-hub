package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: comprovar que Psique avalia o kit Quartzo sem depender de manifesto Opala. */
class QuartzoCommercialContractsTest {
  /**
   * Consome a fila própria, mantém captura visual e compõe o prompt sem ler artefatos de outro
   * tipo.
   */
  @Test
  void reviewsQuartzoFromFrozenContextWithoutOpalaWorkspace() throws Exception {
    String code = "quartzo-commercial-preparation-v1";
    var json = new ObjectMapper();
    var worker =
        new CustomerBpmTaskConsumer(
            "http://127.0.0.1:1", "codex", "gpt-5.6-sol", "max", "/missing", "/missing", json);
    var task =
        Map.<String, Object>of(
            "taskId",
            501L,
            "processCode",
            code,
            "activityId",
            "humanExperienceReview",
            "sourceReference",
            "experiment:88",
            "processContextJson",
            "{\"quartzoCommercial\":{\"fingerprint\":\"frozen-quartzo\",\"productId\":7}}",
            "taskTarget",
            Map.of("publicUrl", "https://example.test/kit", "productId", 7));
    assertThat(CustomerBpmTaskConsumer.supportsContract(code, "humanExperienceReview")).isTrue();
    assertThat(CustomerBpmTaskConsumer.requiresVisualAudit(code)).isTrue();
    assertThat(worker.prompt(task, List.of()))
        .contains("frozen-quartzo", "PRODUCT_PROOF", "PERSONALIZATION", "página própria")
        .doesNotContain("{{TASK_CONTEXT}}", "versionedCommercialHomologationEvidence");
    var schema =
        json.readTree(
            new ClassPathResource(CustomerBpmTaskConsumer.schemaResourceFor(code))
                .getInputStream());
    assertThat(schema.path("properties").path("gateChecks").path("minItems").asInt()).isEqualTo(10);
    var evidence =
        CustomerBpmTaskConsumer.class.getDeclaredMethod("evidence", Map.class, List.class);
    evidence.setAccessible(true);
    assertThat(
            json.readTree((String) evidence.invoke(worker, task, List.of()))
                .path("quartzoScope")
                .path("fingerprint")
                .asText())
        .isEqualTo("frozen-quartzo");
  }
}
