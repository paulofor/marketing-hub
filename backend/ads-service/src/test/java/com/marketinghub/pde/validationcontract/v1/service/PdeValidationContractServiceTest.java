package com.marketinghub.pde.validationcontract.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar autenticação e isolamento do contrato candidato de preflight. */
class PdeValidationContractServiceTest {

  /** Deve liberar somente a candidata exata quando o segredo interno coincide. */
  @Test
  void returnsExactCandidateForAuthorizedPdeBackend() {
    PdeProductionSlotService slots = mock(PdeProductionSlotService.class);
    PublicProductCommercialOfferService offers = mock(PublicProductCommercialOfferService.class);
    PdeValidationContractService service =
        new PdeValidationContractService(slots, offers, "internal-test-token");
    when(slots.findValidationExperienceJson(
            "metodo-musa-7-dias", "v8", "musa-pde-entry-v12-primeiro-ajuste-aplicavel"))
        .thenReturn("{\"slug\":\"metodo-musa-7-dias\"}");

    String response =
        service.experience(
            "internal-test-token",
            "metodo-musa-7-dias",
            "v8",
            "musa-pde-entry-v12-primeiro-ajuste-aplicavel");

    assertThat(response).contains("metodo-musa-7-dias");
    verify(slots)
        .findValidationExperienceJson(
            "metodo-musa-7-dias", "v8", "musa-pde-entry-v12-primeiro-ajuste-aplicavel");
  }

  /** Deve recusar token ausente antes de consultar qualquer contrato candidato. */
  @Test
  void rejectsMissingInternalToken() {
    PdeValidationContractService service =
        new PdeValidationContractService(
            mock(PdeProductionSlotService.class),
            mock(PublicProductCommercialOfferService.class),
            "internal-test-token");

    assertThatThrownBy(() -> service.experience(null, "metodo-musa-7-dias", "v8", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não autorizado");
  }
}
