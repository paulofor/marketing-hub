package com.marketinghub.pde.validationcontract.v1.controller;

import com.marketinghub.pde.validationcontract.v1.service.PdeValidationContractService;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor ao backend PDE os contratos exatos da candidata em homologação. */
@RestController
@RequestMapping("/api/internal/pde-validation-contract/v1/products")
public class PdeValidationContractController {
  private final PdeValidationContractService service;

  /** Recebe o serviço que autentica e resolve os contratos sem antecipar publicação. */
  public PdeValidationContractController(PdeValidationContractService service) {
    this.service = service;
  }

  /** Entrega o contrato da experiência pedido por slot e versão explícitos. */
  @Operation(summary = "Consulta contrato PDE candidato para preflight autenticado")
  @GetMapping(value = "/{productSlug}/experience", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> experience(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
      @PathVariable String productSlug,
      @RequestParam(required = false) String slotCode,
      @RequestParam(required = false) String experienceVersion) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(service.experience(internalToken, productSlug, slotCode, experienceVersion));
  }

  /** Entrega a oferta da candidata pedida sem criar preferência, cobrança ou acesso. */
  @Operation(summary = "Consulta oferta PDE candidata para preflight autenticado")
  @GetMapping("/{productSlug}/commercial-offer")
  public PublicProductCommercialOfferResponse offer(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
      @PathVariable String productSlug,
      @RequestParam(required = false) String slotCode,
      @RequestParam(required = false) String experienceVersion) {
    return service.offer(internalToken, productSlug, slotCode, experienceVersion);
  }
}
