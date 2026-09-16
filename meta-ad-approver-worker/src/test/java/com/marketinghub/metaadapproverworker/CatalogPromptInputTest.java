package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Responsabilidade: impedir fallback de texto e execução de contrato incompatível no piloto Opala.
 */
class CatalogPromptInputTest {
  private static final String SCHEMA =
      "prompts/bpm/pde-commercial-homologation-independent-review-schema.json";

  /** Monta a resposta do backend com texto próprio de teste e schema real do executor. */
  static Map<String, Object> fixture(String activity, String agent, String schema)
      throws Exception {
    String text = "Fixture: preparação comercial Opala v1. Contexto: {{TASK_CONTEXT}}";
    var result = new HashMap<String, Object>();
    result.put("origin", "DATABASE");
    result.put("bindingId", 900001);
    result.put("versionId", 900007);
    result.put("versionNumber", 7);
    result.put("processVersion", 1);
    result.put("agentKey", agent);
    result.put("activityId", activity);
    result.put("executorModule", "meta-ad-approver-worker");
    result.put("text", text);
    result.put("sha256", CatalogPromptInput.sha256(text));
    result.put("schemaId", schema);
    result.put(
        "schemaSha256",
        CatalogPromptInput.sha256(
            new ClassPathResource(schema)
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)));
    return result;
  }

  /** Produz uma tarefa isolada com a identidade oficial recebida pela fila. */
  private Map<String, Object> task() throws Exception {
    var result = new HashMap<String, Object>();
    result.put("processCode", "opala-commercial-preparation-v1");
    result.put("processVersion", 1);
    result.put("agentKey", "meta-ad-approver");
    result.put("activityId", "commercialIntegrityReview");
    result.put("catalogPrompt", fixture("commercialIntegrityReview", "meta-ad-approver", SCHEMA));
    return result;
  }

  /** Usa a versão fixada e retira o texto do contexto, preservando referência para auditoria. */
  @Test
  void consumesPinnedTextWithoutDuplicatingInstructions() throws Exception {
    var task = task();
    assertThat(CatalogPromptInput.text(task, SCHEMA)).contains("preparação comercial Opala v1");
    assertThat(CatalogPromptInput.context(task))
        .doesNotContainKey("catalogPrompt")
        .containsKey("catalogPromptReference");
    assertThat(task).containsKey("catalogPrompt");
  }

  /**
   * Falha antes de usar o modelo quando o backend não entrega a versão, mesmo havendo arquivos
   * locais.
   */
  @Test
  void rejectsMissingCatalogWithoutFallback() throws Exception {
    var task = task();
    task.remove("catalogPrompt");
    assertThatThrownBy(() -> CatalogPromptInput.text(task, SCHEMA))
        .hasMessageContaining("sem prompt fixado");
  }

  /** Recusa corrupção, outro agente, outro schema e outra versão de processo separadamente. */
  @Test
  void rejectsContractDrift() throws Exception {
    for (String key :
        List.of(
            "text",
            "sha256",
            "agentKey",
            "activityId",
            "schemaSha256",
            "schemaId",
            "processVersion",
            "executorModule")) {
      var task = task();
      var prompt = new HashMap<>(fixture("commercialIntegrityReview", "meta-ad-approver", SCHEMA));
      prompt.put(key, "divergente");
      task.put("catalogPrompt", prompt);
      assertThatThrownBy(() -> CatalogPromptInput.text(task, SCHEMA))
          .as(key)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  /** Não interpreta campos de outros fluxos como migração autorizada ao banco. */
  @Test
  void preservesNonMigratedContexts() throws Exception {
    var task = task();
    task.put("processCode", "legacy");
    assertThat(CatalogPromptInput.migrated(task)).isFalse();
    assertThat(CatalogPromptInput.context(task)).isSameAs(task);
  }

  /**
   * Confirma que o consumidor real injeta o texto do banco, mantendo a referência e sem
   * autorreferência.
   */
  @Test
  void composesActualConsumerFromPinnedCatalogText() throws Exception {
    var properties = new MetaAdApproverProperties();
    properties.setReasoningEffort("max");
    var consumer =
        new CommercialBpmTaskConsumer(
            properties,
            "unused",
            "fixture-model",
            "/tmp",
            "",
            new com.fasterxml.jackson.databind.ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(
        consumer, "pdeArtifactLoader", org.mockito.Mockito.mock(PdeReviewArtifactLoader.class));
    String prompt = consumer.prompt(task());
    assertThat(prompt).contains("Fixture: preparação comercial Opala v1", "catalogPromptReference");
    assertThat(prompt).doesNotContain("{{TASK_CONTEXT}}", "\"catalogPrompt\":");
  }
}
