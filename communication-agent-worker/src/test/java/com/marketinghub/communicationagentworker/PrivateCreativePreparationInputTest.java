package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir gasto ou mudança de identidade por declaração privada adulterada. */
class PrivateCreativePreparationInputTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita somente a prova interna sem vídeo pago para duas origens de teste diferentes. */
  @ParameterizedTest
  @ValueSource(strings = {"experiment:92010", "product:91077@agent-validation-v1"})
  void acceptsPrivateProofAndPreservesCommercialChoice(String reference) {
    var input = input(reference);
    PrivateCreativePreparationInput.validate(input, reference);
    assertThat(input.path("privateCreativePreparation").path("commercialFormatDecisionPreserved").asBoolean()).isTrue();
  }

  /** Recusa permissões implícitas, identidade trocada, versão divergente ou pedido não declarado. */
  @ParameterizedTest
  @ValueSource(strings = {"PUBLICATION", "PAYMENT", "SPEND", "CONTRACT_PUBLICATION", "CONTRACT_SPEND", "COMMERCIAL_EVIDENCE", "REFERENCE", "VERSION", "TEMPLATE", "MARKET_FORMAT", "VIDEO_ID", "VIDEO_INTENT", "MODE", "NULL"})
  void refusesInvalidContract(String defect) {
    String reference = "experiment:92010";
    var input = input(reference);
    var contract = input.withObject("/privateCreativePreparation");
    switch (defect) {
      case "PUBLICATION" -> input.put("publicationAuthorized", true);
      case "PAYMENT" -> input.put("paymentEnabled", true);
      case "SPEND" -> input.put("externalMediaSpendAuthorized", true);
      case "CONTRACT_PUBLICATION" -> contract.put("publicationAuthorized", true);
      case "CONTRACT_SPEND" -> contract.put("spendAuthorized", true);
      case "COMMERCIAL_EVIDENCE" -> contract.put("commercialEvidenceClaimed", true);
      case "REFERENCE" -> contract.put("sourceReference", "experiment:99999");
      case "VERSION" -> contract.put("prototypeVersion", "sandbox-v0");
      case "TEMPLATE" -> contract.put("nonAudiovisualTemplate", "UNKNOWN");
      case "MARKET_FORMAT" -> contract.put("commercialFormatDecisionPreserved", false);
      case "VIDEO_ID" -> contract.put("videoProductionRequestId", 12L);
      case "VIDEO_INTENT" -> contract.put("audiovisualProductionIntent", "FREE_PRODUCTION_ASSUMED");
      case "MODE" -> input.put("mode", "COMMERCIAL");
      case "NULL" -> input.putNull("privateCreativePreparation");
      default -> throw new AssertionError("Caso desconhecido.");
    }
    assertThatThrownBy(() -> PrivateCreativePreparationInput.validate(input, reference))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("prova criativa privada inválido");
  }

  /** Pedido explícito pode seguir governado sem ganhar autorização de gasto neste contrato. */
  @Test
  void acceptsGovernedVideoRequestWithoutAuthorizingSpend() {
    var input = input("experiment:92010");
    input.withObject("/privateCreativePreparation")
        .put("audiovisualProductionIntent", "GOVERNED_PRODUCTION_REQUESTED")
        .put("videoProductionRequestId", 94010L);
    PrivateCreativePreparationInput.validate(input, "experiment:92010");
    assertThat(input.path("privateCreativePreparation").path("spendAuthorized").asBoolean(true)).isFalse();
  }

  /** Mantém entradas legadas sem produzir dispensas ou permissões artificiais. */
  @Test
  void acceptsLegacyAbsenceWithoutAddingContract() {
    var input = json.createObjectNode().put("mode", "COMMERCIAL");
    PrivateCreativePreparationInput.validate(input, "experiment:92010");
    assertThat(input.has("privateCreativePreparation")).isFalse();
  }

  /** Monta o contrato restrito, com IDs reservados e nenhum endereço ou credencial de produção. */
  private ObjectNode input(String reference) {
    var input = json.createObjectNode()
        .put("mode", reference.startsWith("product:") ? "PRODUCT_PRIVATE" : "LEARNING_CYCLE_PRIVATE")
        .put("sourceReference", reference).put("prototypeVersion", "sandbox-v1")
        .put("publicationAuthorized", false).put("paymentEnabled", false).put("externalMediaSpendAuthorized", false);
    input.putObject("privateCreativePreparation")
        .put("contractVersion", "PDE_PRIVATE_CREATIVE_PREPARATION_V1")
        .put("scope", "PRIVATE_PREPARATION").put("sourceReference", reference)
        .put("prototypeVersion", "sandbox-v1").put("nonAudiovisualEvidenceRequired", true)
        .put("nonAudiovisualEvidencePurpose", "INDEPENDENT_REVIEW")
        .put("nonAudiovisualTemplate", "PROOF_CARD_V1").put("commercialFormatDecisionPreserved", true)
        .put("audiovisualProductionIntent", "BRIEF_ONLY").put("videoProductionRequestId", 0L)
        .put("publicationAuthorized", false).put("spendAuthorized", false).put("commercialEvidenceClaimed", false);
    return input;
  }
}
