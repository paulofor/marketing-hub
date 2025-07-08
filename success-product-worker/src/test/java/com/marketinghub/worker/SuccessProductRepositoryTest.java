package com.marketinghub.worker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = SuccessProductWorkerApplication.class)
class SuccessProductRepositoryTest {

    @Autowired
    SuccessProductRepository repository;

    @Test
    void testSaveSuccessProduct() {
        SuccessProduct product = SuccessProduct.builder()
                .description("Great product")
                .build();
        repository.save(product);
        SuccessProduct saved = repository.findById(product.getId()).orElseThrow();
        assertThat(saved.isNovo()).isTrue();
    }

    @Test
    void testSaveSuccessProductWithMultilineDescription() {
        String description = """
                OFERTA ESCALADA 03/07/25 -  🚀
                🌏 Idioma: Português
                ✅ Criativos Ativos: 75 anúncios
                📆 Data Ativação: 28/05/2025
                🎯Nicho: Saúde e Bem Estar
                🎯SubNicho: Arte Milenar
                📱Funil: Anúncio + Página + Checkout R$97 + 2 Order's

                🔗Link das Ofertas:
                📣Página de Vendas: https://www.taichichenonline.com/PVDTAICHI
                📣Página de Checkout:
                📚Biblioteca de Anúncios: https://www.facebook.com/ads/library/?active_status=active&ad_type=all&country=ALL&is_targeted_country=false&media_type=all&search_type=page&view_all_page_id=354475828085920
                """;

        SuccessProduct product = SuccessProduct.builder()
                .description(description)
                .build();
        repository.save(product);
        SuccessProduct saved = repository.findById(product.getId()).orElseThrow();
        assertThat(saved.getDescription()).isEqualTo(description);
    }
}
