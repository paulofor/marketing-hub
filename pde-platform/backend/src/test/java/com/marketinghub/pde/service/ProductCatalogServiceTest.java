package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida o catálogo inicial de produtos PDE. */
class ProductCatalogServiceTest {

    /** Confirma que o produto MUSA nasce como experiência guiada de 7 dias. */
    @Test
    void returnsMusaProductWithSevenMissions() {
        ProductCatalogService service = new ProductCatalogService();

        var product = service.getProduct("metodo-musa-7-dias");

        assertThat(product.promise()).contains("7 dias");
        assertThat(product.missions()).hasSize(7);
        assertThat(product.supportMaterials()).hasSize(4);
    }
}
