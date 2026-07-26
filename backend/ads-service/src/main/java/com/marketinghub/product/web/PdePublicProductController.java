package com.marketinghub.product.web;

import com.marketinghub.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor aliases públicos PDE que consomem o cadastro canônico de produtos. */
@RestController
@RequestMapping("/api/pde/products")
public class PdePublicProductController {
  private final ProductService productService;

  /** Inicializa o controller com o serviço canônico de produtos. */
  public PdePublicProductController(ProductService productService) {
    this.productService = productService;
  }

  /** Retorna o contrato JSON público da experiência PDE pelo slug do produto. */
  @GetMapping(value = "/{productSlug}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicPdeProduct(@PathVariable String productSlug) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(productService.getPublicPdeExperienceJson(productSlug));
  }
}
