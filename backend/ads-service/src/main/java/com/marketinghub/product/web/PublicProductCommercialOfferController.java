package com.marketinghub.product.web;

import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a oferta comercial pública que a experiência PDE usa antes da compra. */
@RestController
@RequestMapping("/api/products/public")
public class PublicProductCommercialOfferController {
  private final PublicProductCommercialOfferService service;

  /** Inicializa o controller com a fonte canônica da oferta comercial. */
  public PublicProductCommercialOfferController(PublicProductCommercialOfferService service) {
    this.service = service;
  }

  /** Retorna o contrato de venda do produto sem expor configuração administrativa. */
  @GetMapping("/{productSlug}/commercial-offer")
  public PublicProductCommercialOfferResponse getOffer(@PathVariable String productSlug) {
    return service.getOffer(productSlug);
  }
}
