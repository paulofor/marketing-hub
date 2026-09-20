package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: comprovar a revisão de integridade Quartzo sem pacote ou slots de Opala. */
class QuartzoCommercialContractsTest {
  /** O worker usa o contrato de kit e devolve o mesmo snapshot recebido pela fila canônica. */
  @Test
  void reviewsQuartzoUsingOnlyItsFrozenCommercialSources() throws Exception {
    String code = "quartzo-commercial-preparation-v1";
    var json = new ObjectMapper();
    var worker =
        new CommercialBpmTaskConsumer(
            new MetaAdApproverProperties(), "codex", "gpt-5.6-sol", "/missing", "/missing", json);
    var task =
        Map.<String, Object>of(
            "taskId",
            502L,
            "processCode",
            code,
            "activityId",
            "commercialIntegrityReview",
            "sourceReference",
            "experiment:88",
            "processContextJson",
            "{\"quartzoCommercial\":{\"fingerprint\":\"frozen-quartzo\",\"productId\":7}}",
            "taskTarget",
            Map.of("publicUrl", "https://example.test/kit", "productId", 7));
    assertThat(CommercialBpmTaskConsumer.supportsContract(code, "commercialIntegrityReview"))
        .isTrue();
    assertThat(worker.prompt(task))
        .contains("frozen-quartzo", "PRODUCT_PROOF", "PERSONALIZATION", "página própria")
        .doesNotContain("{{TASK_CONTEXT}}", "versionedCommercialHomologationEvidence");
    var schema =
        json.readTree(
            new ClassPathResource(CommercialBpmTaskConsumer.schemaResourceFor(code))
                .getInputStream());
    assertThat(schema.path("properties").path("gateChecks").path("minItems").asInt()).isEqualTo(10);
    var evidence = CommercialBpmTaskConsumer.class.getDeclaredMethod("evidence", Map.class);
    evidence.setAccessible(true);
    assertThat(
            json.readTree((String) evidence.invoke(worker, task))
                .path("quartzoScope")
                .path("fingerprint")
                .asText())
        .isEqualTo("frozen-quartzo");
  }
}
