package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir origem ausente, cruzada ou tratada como aprovação comercial. */
class KitVisualReferenceContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Outra especialidade deve manter identidade própria, sem alterar o caminho histórico. */
  @Test
  void acceptsSeparateProfilesAndPreservesLegacy() {
    for (String profile : new String[] {"nails-v2", "hair-v2", "nails-v3", "hair-v3"}) {
      assertThat(KitVisualReferenceContract.validate(profile, proof(profile))).isNotNull();
      assertThat(KitSpecialties.profiles("beauty-v2")).hasSize(6);
    }
    assertThat(KitVisualReferenceContract.validate("nails-v1", null)).isNull();
    assertThat(KitSpecialties.profiles("beauty-v1")).allMatch(p -> p.code().endsWith("-v1"));
  }

  /** Os defeitos de origem e diversidade são recusados antes de aceitar o artefato. */
  @Test
  void rejectsMissingCrossProfileDuplicateOrCommercialProof() {
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v2", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("hair-v2", proof("nails-v2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v1", proof("nails-v2")))
        .isInstanceOf(IllegalArgumentException.class);
    var value = proof("nails-v2");
    value.put("commercialApproval", true);
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v2", value))
        .isInstanceOf(IllegalArgumentException.class);
    value.put("commercialApproval", false);
    ((ObjectNode) value.path("assets").get(1)).put("sha256", "0".repeat(64));
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v2", value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Impede promover uma composição antiga ou uma fonte estrangeira à candidata editorial. */
  @Test
  void rejectsMissingEditorialIdentityAndCrossVersionSource() {
    var value = proof("nails-v3");
    value.remove("compositionVersion");
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v3", value))
        .hasMessageContaining("editorial");
    value.put("compositionVersion", "REFERENCE_EDITORIAL_V3");
    ((ObjectNode) value.path("assets").get(0)).put("sourceProfileCode", "hair-v2");
    assertThatThrownBy(() -> KitVisualReferenceContract.validate("nails-v3", value))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(KitSpecialties.profiles("beauty-v3"))
        .hasSize(6)
        .allMatch(p -> p.code().endsWith("-v3"));
    assertThat(KitCompositorCapabilities.parse("nails-v3,hair-v2"))
        .containsExactlyInAnyOrder("nails-v3", "hair-v2");
    assertThat(
            KitSpecialties.declaredProfile(
                json.createObjectNode().put("kitProfileCode", "beauty-v3")))
        .contains("beauty-v3");
  }

  /** Cria recibo sintético sem utilizar nem aprovar uma fotografia comercial. */
  private ObjectNode proof(String profile) {
    var value =
        json.createObjectNode()
            .put("contractVersion", "KIT_LICENSED_REFERENCE_V1")
            .put("profileCode", profile)
            .put("commercialApproval", false)
            .put("librarySha256", "a".repeat(64));
    if (profile.endsWith("-v3")) value.put("compositionVersion", "REFERENCE_EDITORIAL_V3");
    var assets = value.putArray("assets");
    for (int i = 0; i < 5; i++)
      assets
          .addObject()
          .put("profileCode", profile)
          .put("sourceProfileCode", profile.replace("-v3", "-v2"))
          .put("sha256", String.valueOf(i).repeat(64))
          .put("sourceUrl", "https://www.pexels.com/photo/123/")
          .put("licenseUrl", "https://www.pexels.com/license/")
          .put("use", "LICENSED_REFERENCE_DEMO")
          .put("notCustomerWork", true);
    return value;
  }

  /** O contrato HTTP aceita as três versões instaladas e recusa uma versão desconhecida. */
  @Test
  void acceptsInstalledSpecialtyAtHttpBoundary() {
    try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      var type =
          com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Input
              .class;
      assertThat(validator.validateValue(type, "specialtyCode", "nails-v2")).isEmpty();
      assertThat(validator.validateValue(type, "specialtyCode", "hair-v1")).isEmpty();
      assertThat(validator.validateValue(type, "specialtyCode", "nails-v3")).isEmpty();
      assertThat(validator.validateValue(type, "specialtyCode", "nails-v4")).isNotEmpty();
    }
  }
}
