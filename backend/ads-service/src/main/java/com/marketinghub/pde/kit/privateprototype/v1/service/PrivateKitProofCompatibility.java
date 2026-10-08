package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Responsabilidade: impedir que uma prova do pacote antigo libere a revisão do contrato corrigido.
 */
public final class PrivateKitProofCompatibility {
  /** Conserva outros runtimes e exige formato, quantidade e hash reais nos cenários de kits. */
  public static boolean current(JsonNode result) {
    if (!"PDE_PRIVATE_KIT_FIXTURES_V1".equals(result.path("fixtureContract").asText())) return true;
    if (!KitArtifactContract.PACKAGE_CONTRACT_VERSION.equals(
            result.path("packageContractVersion").asText())
        || !result.path("scenarios").isArray()
        || result.path("scenarios").isEmpty()) return false;
    for (JsonNode scenario : result.path("scenarios")) {
      if ("SAFETY".equals(scenario.path("scenarioCode").asText())) {
        if (scenario.path("packageFileCount").asInt(-1) != 0
            || !scenario.path("zipSha256").isNull()) return false;
      } else if (scenario.path("packageFileCount").asInt(-1) != 36
          || !scenario.path("zipSha256").asText().matches("[a-f0-9]{64}")) return false;
    }
    return true;
  }
}
