package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Responsabilidade: validar a declaração backend de provas da preparação criativa privada. */
final class PrivateCreativePreparationInput {
  private static final String VERSION = "PDE_PRIVATE_CREATIVE_PREPARATION_V1";
  private static final Set<String> MODES =
      Set.of("LEARNING_CYCLE_PRIVATE", "PRODUCT_PRIVATE", "INITIAL_EXPERIMENT_PRIVATE");

  /** Impede instanciar um verificador determinístico sem estado. */
  private PrivateCreativePreparationInput() {}

  /** Recusa contratos divergentes antes do modelo; ausência preserva o transporte legado. */
  static void validate(JsonNode communication, String reference) {
    JsonNode contract = communication.path("privateCreativePreparation");
    if (contract.isMissingNode()) return;
    String intent = contract.path("audiovisualProductionIntent").asText();
    if (!VERSION.equals(contract.path("contractVersion").asText())
        || !MODES.contains(communication.path("mode").asText())
        || !"PRIVATE_PREPARATION".equals(contract.path("scope").asText())
        || !reference.equals(contract.path("sourceReference").asText())
        || !reference.equals(communication.path("sourceReference").asText())
        || communication.path("prototypeVersion").asText().isBlank()
        || !communication
            .path("prototypeVersion")
            .asText()
            .equals(contract.path("prototypeVersion").asText())
        || !contract.path("nonAudiovisualEvidenceRequired").asBoolean(false)
        || !"INDEPENDENT_REVIEW".equals(contract.path("nonAudiovisualEvidencePurpose").asText())
        || !"PROOF_CARD_V1".equals(contract.path("nonAudiovisualTemplate").asText())
        || !contract.path("commercialFormatDecisionPreserved").asBoolean(false)
        || !Set.of("BRIEF_ONLY", "GOVERNED_PRODUCTION_REQUESTED").contains(intent)
        || !contract.path("videoProductionRequestId").isIntegralNumber()
        || ("BRIEF_ONLY".equals(intent) && contract.path("videoProductionRequestId").asLong() != 0)
        || ("GOVERNED_PRODUCTION_REQUESTED".equals(intent)
            && contract.path("videoProductionRequestId").asLong() <= 0)
        || communication.path("publicationAuthorized").asBoolean(true)
        || communication.path("paymentEnabled").asBoolean(true)
        || communication.path("externalMediaSpendAuthorized").asBoolean(true)
        || contract.path("publicationAuthorized").asBoolean(true)
        || contract.path("spendAuthorized").asBoolean(true)
        || contract.path("commercialEvidenceClaimed").asBoolean(true)) {
      throw new IllegalArgumentException(
          "Contrato de prova criativa privada inválido; confira identidade, versão, escopo e produção governada antes da execução.");
    }
  }
}
