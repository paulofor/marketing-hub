package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.ProductJourneyIntegrationContractResponse;
import com.marketinghub.pde.service.ProductJourneyIntegrationContractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o contrato público preparado para homologar a jornada comercial. */
@RestController
@RequestMapping("/api/pde/products")
public class ProductJourneyIntegrationController {
    private final ProductJourneyIntegrationContractService service;

    /** Recebe o serviço que deriva o contrato do catálogo e dos eventos realmente suportados. */
    public ProductJourneyIntegrationController(ProductJourneyIntegrationContractService service) {
        this.service = service;
    }

    /** Retorna rotas, correlações e eventos sem executar qualquer ação comercial. */
    @GetMapping("/{productSlug}/integration-contract")
    public ProductJourneyIntegrationContractResponse get(
            @PathVariable("productSlug") String productSlug) {
        return service.get(productSlug);
    }
}
