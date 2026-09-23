package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: comprovar que Psique avalia Safira sem promover a validação privada. */
class SafiraCommercialContractsTest {
  /** Usa contexto, imagens, checkout e callback da mesma candidata comercial imutável. */
  @Test
  void reviewsSafiraFromFrozenPublicCommercialContext() throws Exception {
    String code = "safira-commercial-preparation-v1";
    var json = new ObjectMapper();
    var worker =
        new CustomerBpmTaskConsumer(
            "http://127.0.0.1:1", "codex", "gpt-5.6-sol", "max", "/missing", "/missing", json);
    var task =
        Map.<String, Object>of(
            "taskId",
            601L,
            "processCode",
            code,
            "activityId",
            "humanExperienceReview",
            "sourceReference",
            "experiment:301",
            "processContextJson",
            "{\"safiraCommercial\":{\"fingerprint\":\"frozen-safira\",\"productId\":10,"
                + "\"checkoutUrl\":\"https://example.test/checkout\"}}",
            "taskTarget",
            Map.of(
                "publicUrl",
                "https://example.test/mira",
                "commercialCheckoutUrl",
                "https://example.test/checkout",
                "productId",
                10));

    assertThat(CustomerBpmTaskConsumer.supportsContract(code, "humanExperienceReview")).isTrue();
    assertThat(CustomerBpmTaskConsumer.requiresVisualAudit(code)).isTrue();
    assertThat(worker.additionalVisualUrls(task)).containsExactly("https://example.test/checkout");
    assertThat(worker.prompt(task, List.of()))
        .contains(
            "frozen-safira",
            "AI_PRODUCT",
            "cinco critérios comerciais",
            "não representa cliente",
            "custos de IA",
            "preflight")
        .doesNotContain("{{TASK_CONTEXT}}", "Mira", "pele madura");
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
                .path("safiraScope")
                .path("fingerprint")
                .asText())
        .isEqualTo("frozen-safira");
  }
}
