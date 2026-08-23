package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.CommercialOfferResponse;
import com.marketinghub.pde.service.CommercialOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe ao frontend PDE a oferta canônica obtida exclusivamente pelo backend principal. */
@RestController
@RequestMapping("/api/pde/products")
public class CommercialOfferController {
    private final CommercialOfferService service;

    /** Inicializa o controller com o cliente oficial do Marketing Hub. */
    public CommercialOfferController(CommercialOfferService service) {
        this.service = service;
    }

    /** Retorna a oferta que a superfície pré-compra deve renderizar. */
    @GetMapping("/{productSlug}/commercial-offer")
    public CommercialOfferResponse getOffer(@PathVariable String productSlug) {
        return service.getOffer(productSlug);
    }
}
