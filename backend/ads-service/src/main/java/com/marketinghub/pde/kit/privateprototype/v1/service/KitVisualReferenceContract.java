package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;

/** Responsabilidade: conferir a origem visual privada antes de persistir o recibo do compositor. */
public final class KitVisualReferenceContract {
  /** Impede instanciação de um contrato sem estado. */
  private KitVisualReferenceContract() {}

  /**
   * Exige cinco fontes íntegras da versão selecionada e preserva callbacks históricos sem essa
   * origem.
   */
  public static JsonNode validate(String profile, JsonNode value) {
    boolean reference = KitSpecialties.isReference(profile) && !KitSpecialties.isFamily(profile);
    if (!reference) {
      if (value != null && !value.isNull() && !value.isMissingNode())
        throw new IllegalArgumentException("Referências não pertencem ao perfil histórico.");
      return null;
    }
    if (value == null
        || !value.isObject()
        || value.toString().length() > 16000
        || !"KIT_LICENSED_REFERENCE_V1".equals(value.path("contractVersion").asText())
        || !profile.equals(value.path("profileCode").asText())
        || !value.path("commercialApproval").isBoolean()
        || value.path("commercialApproval").asBoolean()
        || !value.path("librarySha256").asText().matches("[a-f0-9]{64}")
        || !value.path("assets").isArray()
        || value.path("assets").size() != 5)
      throw new IllegalArgumentException("Origem visual privada incompleta ou incompatível.");
    boolean editorial = profile.endsWith("-v3");
    if (editorial && !"REFERENCE_EDITORIAL_V3".equals(value.path("compositionVersion").asText()))
      throw new IllegalArgumentException("Versão editorial não comprovada.");
    var hashes = new HashSet<String>();
    for (var asset : value.path("assets")) {
      if ((editorial
              && !profile.replace("-v3", "-v2").equals(asset.path("sourceProfileCode").asText()))
          || !profile.equals(asset.path("profileCode").asText())
          || !asset.path("sha256").asText().matches("[a-f0-9]{64}")
          || !hashes.add(asset.path("sha256").asText())
          || !asset.path("sourceUrl").asText().matches("https://www[.]pexels[.]com/photo/[0-9]+/")
          || !"https://www.pexels.com/license/".equals(asset.path("licenseUrl").asText())
          || !"LICENSED_REFERENCE_DEMO".equals(asset.path("use").asText())
          || !asset.path("notCustomerWork").asBoolean())
        throw new IllegalArgumentException(
            "Fonte visual sem licença, identidade ou diversidade comprovada.");
    }
    return value.deepCopy();
  }
}
