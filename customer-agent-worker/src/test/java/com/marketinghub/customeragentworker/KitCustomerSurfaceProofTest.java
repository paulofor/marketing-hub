package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir parecer de kit sem prova da ausência de informação interna na tela. */
class KitCustomerSurfaceProofTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Mantém a prova aplicável a outras identidades e não exige migração de contratos históricos. */
  @Test
  void validatesOnlyDeclaredKitRuntimeAcrossProducts() {
    for (long productId : new long[] {7L, 9918L}) {
      var target = target(productId);
      assertThatCode(() -> KitCustomerSurfaceProof.validate(target, report()))
          .doesNotThrowAnyException();
      ((ObjectNode) target.path("pdeContext").path("privatePrototypeAcceptance"))
          .put("runtimeKind", "OTHER_PRIVATE_RUNTIME");
      assertThatCode(() -> KitCustomerSurfaceProof.validate(target, json.createObjectNode()))
          .doesNotThrowAnyException();
    }
  }

  /** Recusa ausência, reprovação e valores textuais em lugar de verificação executada. */
  @Test
  void rejectsMissingOrInvalidSurfaceProof() {
    var target = target(7L);
    assertThatThrownBy(() -> KitCustomerSurfaceProof.validate(target, json.createObjectNode()))
        .hasMessageContaining("cenários");
    for (String field :
        new String[] {"contractVersion", "internalInformationAbsent", "collapsedContentChecked"}) {
      var report = report();
      ((ObjectNode) report.path("scenarios").get(0).path("customerSurface")).remove(field);
      assertThatThrownBy(() -> KitCustomerSurfaceProof.validate(target, report))
          .hasMessageContaining("dados internos");
    }
    var report = report();
    ((ObjectNode) report.path("scenarios").get(0).path("customerSurface"))
        .put("internalInformationAbsent", "true");
    assertThatThrownBy(() -> KitCustomerSurfaceProof.validate(target, report))
        .hasMessageContaining("dados internos");
  }

  /** Monta somente o contrato de runtime, sem nome de produto ou preferência presumida. */
  private ObjectNode target(long productId) {
    var target = json.createObjectNode().put("productId", productId);
    target
        .putObject("pdeContext")
        .putObject("privatePrototypeAcceptance")
        .put("runtimeKind", "DETERMINISTIC_PRIVATE_KIT_V1");
    return target;
  }

  /**
   * Representa uma verificação determinística concluída, sem alegar parecer ou efeito comercial.
   */
  private ObjectNode report() {
    var report = json.createObjectNode();
    report
        .putArray("scenarios")
        .addObject()
        .putObject("customerSurface")
        .put("contractVersion", "PDE_CUSTOMER_SURFACE_V1")
        .put("internalInformationAbsent", true)
        .put("collapsedContentChecked", true);
    return report;
  }
}
