package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger o contrato funcional do gate BPM de Têmis. */
class CommercialBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita gate aprovado somente com justificativa e evidência comercial. */
  @Test
  void acceptsCompleteCommercialReview() throws Exception {
    CommercialBpmTaskConsumer.validate(
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"Jornada coerente\",\"evidence\":[\"Preço consistente\"],\"requiredChanges\":[]}"));
  }

  /** Rejeita autoaprovação vazia que liberaria o processo sem gate real. */
  @Test
  void rejectsCommercialReviewWithoutEvidence() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"\",\"evidence\":[],\"requiredChanges\":[]}");
    assertThatThrownBy(() -> CommercialBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Exige prompt e schema próprios para o gate comercial do criativo. */
  @Test
  void selectsVersionedCreativeContract() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-commercial-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-commercial-review-schema.json");
  }

  /** Seleciona o contrato independente de revisão dos entregáveis do PDE. */
  @Test
  void selectsVersionedPdeDeliverablesContract() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-deliverables-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-deliverables-review-schema.json");
  }

  /** Seleciona a revisão independente de comunicação sem reutilizar prompt de landing. */
  @Test
  void selectsVersionedPdeCommunicationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/pde-communication-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/pde-communication-review-schema.json");

    String prompt = read("prompts/bpm/pde-communication-review.md");
    String schema = read("prompts/bpm/pde-communication-review-schema.json");
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "preço compreensível",
            "tráfego de teste segregado",
            "autorize mídia",
            "compra, acesso, entrega, primeiro uso/aplicação e reembolso",
            "Não crie subagente, worktree ou ambiente auxiliar",
            "Não repita o preflight técnico do processo posterior",
            "80–100",
            "não reduz automaticamente");
    org.assertj.core.api.Assertions.assertThat(schema)
        .contains("priceClarityScore", "commercialRationale", "requiredChanges");
  }

  /** Lê integralmente um contrato versionado do classpath. */
  private String read(String resource) throws Exception {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Lê entrada, cache e saída cumulativos do gate executado pelo Codex. */
  @Test
  void readsTaskTokenUsageFromCodexJsonl() throws Exception {
    Path output = Files.createTempFile("temis-bpm-usage-", ".jsonl");
    Files.writeString(
        output,
        """
        mensagem operacional
        {"type":"turn.completed","usage":{"input_tokens":2400,"cached_input_tokens":1500,"output_tokens":450}}
        """);

    CommercialBpmTaskConsumer.TokenUsage usage =
        CommercialBpmTaskConsumer.readTokenUsage(json, output);

    org.assertj.core.api.Assertions.assertThat(usage.inputTokens()).isEqualTo(2400L);
    org.assertj.core.api.Assertions.assertThat(usage.cachedInputTokens()).isEqualTo(1500L);
    org.assertj.core.api.Assertions.assertThat(usage.outputTokens()).isEqualTo(450L);
    Files.deleteIfExists(output);
  }

  /** Preserva contexto e ausência de efeitos externos inclusive quando Têmis bloqueia. */
  @Test
  void buildsGovernedFailureEvidence() {
    Map<String, Object> evidence =
        CommercialBpmTaskConsumer.evidenceFields(
            "Têmis",
            "gpt-5.6-sol",
            Map.of("sourceReference", "experiment:88", "activityId", "commercial"));

    org.assertj.core.api.Assertions.assertThat(evidence)
        .containsEntry("sourceReference", "experiment:88")
        .containsEntry("activityId", "commercial")
        .containsEntry("accessMode", "READ_ONLY")
        .containsEntry("externalSideEffects", false);
  }
}
