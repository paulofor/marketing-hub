package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.CommercialOfferResponse;
import com.marketinghub.pde.service.CommercialOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** Retorna a oferta do slot ou da versão explícita preservando o contrato legado sem seletor. */
    @GetMapping("/{productSlug}/commercial-offer")
    public CommercialOfferResponse getOffer(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader(value = "Host", required = false) String host,
            @RequestParam(value = "slotCode", required = false) String slotCode,
            @RequestParam(value = "experienceVersion", required = false) String experienceVersion) {
        return experienceVersion == null ? service.getOffer(productSlug, host, slotCode)
                : service.getOffer(productSlug, host, slotCode, experienceVersion);
    }
}
