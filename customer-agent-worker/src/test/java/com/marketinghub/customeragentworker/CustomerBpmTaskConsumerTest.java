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
}
