package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: comprovar a auditoria independente da oferta pública Safira. */
class SafiraCommercialContractsTest {
  /** Preserva o snapshot recebido e não converte validação privada em integridade comercial. */
  @Test
  void reviewsSafiraUsingOnlyFrozenCommercialSources() throws Exception {
    String code = "safira-commercial-preparation-v1";
    var json = new ObjectMapper();
    var worker =
        new CommercialBpmTaskConsumer(
            new MetaAdApproverProperties(), "codex", "gpt-5.6-sol", "/missing", "/missing", json);
    var task =
        Map.<String, Object>of(
            "taskId",
            602L,
            "processCode",
            code,
            "activityId",
            "commercialIntegrityReview",
            "sourceReference",
            "experiment:301",
            "researchIntelligence",
            Map.of(
                "routes",
                java.util.List.of(
                    Map.of(
                        "agentKey",
                        "meta-ad-approver",
                        "cards",
                        java.util.List.of(
                            Map.of(
                                "cardId", "RI1-AAAAAAAAAAAA", "collection", "neuromarketing"))))),
            "processContextJson",
            "{\"safiraCommercial\":{\"fingerprint\":\"frozen-safira\",\"productId\":10}}",
            "taskTarget",
            Map.of("publicUrl", "https://example.test/mira", "productId", 10));

    assertThat(CommercialBpmTaskConsumer.supportsContract(code, "commercialIntegrityReview"))
        .isTrue();
    assertThat(worker.prompt(task).replaceAll("\\s+", " "))
        .contains(
            "frozen-safira",
            "AI_PRODUCT",
            "cinco critérios comerciais",
            "não pode ser promovida",
            "custo de IA",
            "preflight",
            "Uso auditável da inteligência de pesquisa de Têmis v1",
            "cada coleção entregue",
            "RI1-AAAAAAAAAAAA")
        .doesNotContain(
            "{{TASK_CONTEXT}}", "versionedCommercialHomologationEvidence", "Mira", "pele madura");
    var schema =
        json.readTree(
            new ClassPathResource(CommercialBpmTaskConsumer.schemaResourceFor(code))
                .getInputStream());
    assertThat(schema.path("properties").path("gateChecks").path("minItems").asInt()).isEqualTo(10);
    var evidence = CommercialBpmTaskConsumer.class.getDeclaredMethod("evidence", Map.class);
    evidence.setAccessible(true);
    assertThat(
            json.readTree((String) evidence.invoke(worker, task))
                .path("safiraScope")
                .path("fingerprint")
                .asText())
        .isEqualTo("frozen-safira");
  }
}
