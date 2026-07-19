package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import java.util.ArrayList;
import java.util.List;
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
        assertThat(product.scientificEvidencePack().version()).isEqualTo("musa-evidence-pack-v1");
        assertThat(product.scientificEvidencePack().forbiddenClaims()).contains("garante elegância");
    }

    /** Garante que o catálogo visível da MUSA usa português brasileiro com acentuação. */
    @Test
    void returnsMusaCustomerFacingTextWithPortugueseAccents() {
        ProductCatalogService service = new ProductCatalogService();

        var product = service.getProduct("metodo-musa-7-dias");

        assertThat(customerFacingTexts(product))
                .allSatisfy(text -> assertThat(text)
                        .doesNotContain(
                                "voce",
                                "presenca",
                                "combinacao",
                                "acessorios",
                                "sensacao",
                                "nao",
                                "esta",
                                "Inventario"));
    }

    /** Coleta os textos do produto que são renderizados para a cliente. */
    private static List<String> customerFacingTexts(ProductExperienceResponse product) {
        List<String> texts = new ArrayList<>();
        texts.add(product.name());
        texts.add(product.promise());
        texts.add(product.audience());
        texts.add(product.diagnostic().title());
        texts.add(product.diagnostic().intro());
        texts.addAll(product.diagnostic().questions());
        product.missions().forEach(mission -> {
            texts.add(mission.title());
            texts.add(mission.principle());
            texts.add(mission.action());
            texts.add(mission.evidence());
            texts.add(mission.visualCue());
        });
        product.supportMaterials().forEach(material -> {
            texts.add(material.title());
            texts.add(material.type());
            texts.add(material.description());
        });
        texts.add(product.completionOffer());
        return texts;
    }
}
