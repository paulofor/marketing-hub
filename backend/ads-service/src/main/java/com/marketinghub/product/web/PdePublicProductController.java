package com.marketinghub.product.web;

import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor aliases públicos PDE que consomem o cadastro canônico de produtos. */
@RestController
@RequestMapping("/api/pde/products")
public class PdePublicProductController {
  private final ProductService productService;
  private final PdeProductionSlotService pdeProductionSlotService;

  /** Inicializa o controller com o serviço canônico de produtos. */
  public PdePublicProductController(
      ProductService productService, PdeProductionSlotService pdeProductionSlotService) {
    this.productService = productService;
    this.pdeProductionSlotService = pdeProductionSlotService;
  }

  /** Retorna o contrato JSON público da experiência PDE pelo slug, slot ou versão do produto. */
  @GetMapping(value = "/{productSlug}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicPdeProduct(
      @PathVariable String productSlug,
      @RequestParam(required = false) String slotCode,
      @RequestParam(required = false) String experienceVersion) {
    String body =
        pdeProductionSlotService
            .findPublishedExperienceJson(productSlug, slotCode, experienceVersion)
            .orElseGet(() -> productService.getPublicPdeExperienceJson(productSlug));
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
