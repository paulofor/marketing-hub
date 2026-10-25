package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;

/** Responsabilidade: exigir prova da separação entre interface e auditoria nos kits privados. */
final class KitCustomerSurfaceProof {
  /** Impede instanciação de uma validação sem estado. */
  private KitCustomerSurfaceProof() {}

  /** Reconhece somente o runtime de kit declarado no contrato, sem exceção por produto. */
  static boolean applies(JsonNode target) {
    return "DETERMINISTIC_PRIVATE_KIT_V1"
        .equals(
            target
                .path("pdeContext")
                .path("privatePrototypeAcceptance")
                .path("runtimeKind")
                .asText());
  }

  /** Recusa prova ausente ou reprovada antes de aceitar o relatório ou chamar o modelo. */
  static void validate(JsonNode target, JsonNode report) {
    if (!applies(target)) return;
    JsonNode scenarios = report.path("scenarios");
    if (!scenarios.isArray() || scenarios.isEmpty())
      throw new IllegalArgumentException("O kit não informou os cenários da revisão da interface.");
    for (JsonNode scenario : scenarios) {
      JsonNode proof = scenario.path("customerSurface");
      if (!"PDE_CUSTOMER_SURFACE_V1".equals(proof.path("contractVersion").asText())
          || !proof.path("internalInformationAbsent").isBoolean()
          || !proof.path("internalInformationAbsent").booleanValue()
          || !proof.path("collapsedContentChecked").isBoolean()
          || !proof.path("collapsedContentChecked").booleanValue())
        throw new IllegalArgumentException(
            "O kit não comprovou ausência de dados internos na interface e nos detalhes recolhidos.");
    }
  }
}
