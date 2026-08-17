package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato funcional da revisão BPM de Psique. */
class CustomerBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita parecer aprovado somente quando há perspectiva e evidência verificável. */
  @Test
  void acceptsCompleteCustomerReview() throws Exception {
    CustomerBpmTaskConsumer.validate(
        json.readTree(
            "{\"decision\":\"APPROVED\",\"customerPerspective\":\"Oferta clara\",\"evidence\":[\"CTA visível\"],\"requiredChanges\":[]}"));
  }

  /** Rejeita aprovação vazia que liberaria Têmis sem avaliação real da cliente. */
  @Test
  void rejectsReviewWithoutEvidence() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"customerPerspective\":\"\",\"evidence\":[],\"requiredChanges\":[]}");
    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Exige prompt e schema próprios para a percepção do criativo. */
  @Test
  void selectsVersionedCreativeContract() {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-customer-review-schema.json");
  }
}
