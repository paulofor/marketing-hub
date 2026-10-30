package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Responsabilidade: validar o pedido de correção na origem antes de devolver a arquitetura. */
final class PdeUpstreamCorrectionContract {
  /** Impede instanciação do validador sem estado. */
  private PdeUpstreamCorrectionContract() {}

  /** Preserva respostas históricas e exige destino e aceite completos em novos retornos. */
  static void validate(JsonNode result) {
    var request = result.path("upstreamCorrection");
    if (request.isMissingNode() || request.isNull()) return;
    if (!"ADJUST".equals(result.path("decision").asText())
        || !"PROCESS_UPSTREAM_CORRECTION_V1".equals(request.path("contractVersion").asText())
        || !Set.of("marketStrategy", "economics")
            .contains(request.path("targetActivityId").asText())
        || request.path("reason").asText().trim().length() < 10
        || request.path("acceptanceCriterion").asText().trim().length() < 10) {
      throw new IllegalArgumentException(
          "Retorno à origem exige ADJUST, atividade predecessora, motivo e critério de aceite.");
    }
  }
}
