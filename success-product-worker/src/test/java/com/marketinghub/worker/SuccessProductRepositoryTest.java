package com.marketinghub.worker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.marketinghub.worker.test.MySqlContainerBase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration
@EntityScan("com.marketinghub.ads")
@ContextConfiguration(classes = SuccessProductWorkerApplication.class)
class SuccessProductRepositoryTest extends MySqlContainerBase {

    @Autowired
    SuccessProductRepository repository;

    @Test
    void testSaveSuccessProduct() {
        SuccessProduct product = SuccessProduct.builder()
                .description("Great product")
                .name("Produto")
                .salesPageUrl("https://example.com")
                .instagramUrl("https://instagram.com/example")
                .facebookUrl("https://facebook.com/example")
                .youtubeUrl("https://youtube.com/example")
                .build();
        repository.save(product);
        SuccessProduct saved = repository.findById(product.getId()).orElseThrow();
        assertThat(saved.isNovo()).isTrue();
        assertThat(saved.getName()).isEqualTo("Produto");
        assertThat(saved.getSalesPageUrl()).contains("example.com");
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
                .name("Produto")
                .salesPageUrl("https://www.taichichenonline.com/PVDTAICHI")
                .build();
        repository.save(product);
        SuccessProduct saved = repository.findById(product.getId()).orElseThrow();
        assertThat(saved.getDescription()).isEqualTo(description);
        assertThat(saved.getName()).isEqualTo("Produto");
        assertThat(saved.getSalesPageUrl()).contains("PVDTAICHI");
    }
}
