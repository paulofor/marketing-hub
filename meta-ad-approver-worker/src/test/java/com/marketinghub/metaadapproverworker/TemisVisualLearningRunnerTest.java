package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger o replay visual contra efeitos externos e alteração da amostra. */
class TemisVisualLearningRunnerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Executa a consolidação em sandbox somente leitura, sem busca ou MCP operacional. */
  @Test
  void usesReadOnlyCodexWithoutExternalTools() throws Exception {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setRepositoryPath("/workspace/repository");
    TemisVisualLearningRunner runner = new TemisVisualLearningRunner(properties, objectMapper);
    Method command =
        TemisVisualLearningRunner.class.getDeclaredMethod("command", Path.class, Path.class);
    command.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<String> value =
        (List<String>)
            command.invoke(runner, Path.of("/tmp/result.json"), Path.of("/tmp/schema.json"));

    assertThat(value).containsSubsequence("--sandbox", "read-only");
    assertThat(value).contains("approval_policy=\"never\"");
    assertThat(value).doesNotContain("--search");
    assertThat(value).noneMatch(item -> item.contains("mcp_servers"));
  }

  /** Aceita exatamente os quinze IDs congelados e nenhuma autoridade externa. */
  @Test
  void acceptsCompleteFrozenSampleWithoutExternalEffects() throws Exception {
    TemisVisualLearningRunner runner =
        new TemisVisualLearningRunner(new MetaAdApproverProperties(), objectMapper);
    Method validate =
        TemisVisualLearningRunner.class.getDeclaredMethod(
            "validate",
            com.fasterxml.jackson.databind.JsonNode.class,
            TemisVisualLearningJob.class);
    validate.setAccessible(true);

    validate.invoke(runner, response(false, ids()), job());
  }

  /** Rejeita provider e IDs repetidos antes de enviar o callback ao backend. */
  @Test
  void rejectsExternalEffectAndChangedSample() throws Exception {
    TemisVisualLearningRunner runner =
        new TemisVisualLearningRunner(new MetaAdApproverProperties(), objectMapper);
    Method validate =
        TemisVisualLearningRunner.class.getDeclaredMethod(
            "validate",
            com.fasterxml.jackson.databind.JsonNode.class,
            TemisVisualLearningJob.class);
    validate.setAccessible(true);
    List<Integer> duplicated = new ArrayList<>(ids());
    duplicated.set(14, 14);

    assertThatThrownBy(() -> validate.invoke(runner, response(true, ids()), job()))
        .hasRootCauseMessage("Replay visual declarou efeito externo proibido");
    assertThatThrownBy(() -> validate.invoke(runner, response(false, duplicated), job()))
        .hasRootCauseMessage("IDs do replay visual divergentes");
  }

  /** Mantém prompt e schema versionados com holdout, regressão e limites de autoridade. */
  @Test
  void keepsGovernedPromptAndStrictSchemaVersioned() throws Exception {
    String prompt = resource("prompts/visual-learning/v1/consolidate.md");
    String schema = resource("prompts/visual-learning/v1/consolidate-schema.json");

    assertThat(prompt)
        .contains("10 primeiros casos como replay", "5 últimos como holdout")
        .contains("não chame OpenAI", "não pode promover o próprio resultado");
    assertThat(schema)
        .contains("\"additionalProperties\": false", "\"caseAssessments\"")
        .doesNotContain("\"uniqueItems\"");
  }

  /** Cria o job com quinze casos históricos congelados. */
  private TemisVisualLearningJob job() {
    List<Map<String, Object>> cases =
        ids().stream()
            .map(id -> Map.<String, Object>of("caseId", id, "approved", id >= 14))
            .toList();
    return new TemisVisualLearningJob(
        9L,
        "agenda-cheia-feed",
        "temis-visual-playbook-v1",
        "temis-visual-1-15",
        Map.of("cases", cases),
        "reviewer-9");
  }

  /** Cria um resultado estrutural suficiente para exercitar os gates locais do runner. */
  private com.fasterxml.jackson.databind.JsonNode response(
      boolean externalProviderCalled, List<Integer> caseIds) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("externalProviderCalled", externalProviderCalled);
    value.put("spendingAuthorized", false);
    value.put("publicationPerformed", false);
    value.put(
        "caseAssessments",
        caseIds.stream().map(id -> Map.<String, Object>of("caseId", id)).toList());
    return objectMapper.valueToTree(value);
  }

  /** Produz os quinze IDs canônicos. */
  private List<Integer> ids() {
    return java.util.stream.IntStream.rangeClosed(1, 15).boxed().toList();
  }

  /** Lê um recurso integral do contrato do executor. */
  private String resource(String path) throws Exception {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
