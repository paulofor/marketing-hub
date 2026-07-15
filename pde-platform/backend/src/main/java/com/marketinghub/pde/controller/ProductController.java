package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.service.ProductCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o catálogo público de produtos experienciais disponíveis. */
@RestController
@RequestMapping("/api/pde/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    /** Recebe o serviço de catálogo de produtos PDE. */
    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /** Retorna a experiência configurada para um produto pelo seu slug. */
    @GetMapping("/{slug}")
    public ProductExperienceResponse getProduct(@PathVariable("slug") String slug) {
        return productCatalogService.getProduct(slug);
    }
}
