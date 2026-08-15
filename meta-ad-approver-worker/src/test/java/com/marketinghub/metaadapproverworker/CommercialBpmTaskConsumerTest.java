package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
