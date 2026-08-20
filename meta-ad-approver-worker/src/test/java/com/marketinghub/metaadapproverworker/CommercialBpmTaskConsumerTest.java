package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

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
}
