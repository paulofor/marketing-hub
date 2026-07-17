package com.marketinghub.product.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Responsabilidade: validar as regras de cadastro comercial de produtos. */
class ProductServiceTest {

    /** Deve persistir alterações comerciais em um produto já existente. */
    @Test
    void updateProduct() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository);
        Product product = Product.builder().id(1L).name("Nome antigo").build();
        MarketNiche niche = new MarketNiche();
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Método MUSA - Presença Elegante em 7 Dias");
        request.setSlug("metodo-musa-7-dias");
        request.setMarketNicheId(10L);
        request.setCurrentPriceBrl(new BigDecimal("47.00"));
        request.setTargetAudience("Mulheres urbanas");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
        when(productRepository.save(product)).thenReturn(product);

        Product updated = service.updateProduct(1L, request);

        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getSlug()).isEqualTo(request.getSlug());
        assertThat(updated.getCurrentPriceBrl()).isEqualByComparingTo("47.00");
        assertThat(updated.getTargetAudience()).isEqualTo(request.getTargetAudience());
        assertThat(updated.getMarketNiche()).isSameAs(niche);
    }

    /** Deve montar uma definição pública em Markdown com foco comercial e sem detalhes técnicos. */
    @Test
    void buildPublicMarketingDefinitionMarkdown() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository);
        MarketNiche niche = MarketNiche.builder().name("Elegância feminina prática").build();
        Product product = Product.builder()
                .id(1L)
                .slug("metodo-musa-7-dias")
                .name("Método MUSA - Presença Elegante em 7 Dias")
                .productType("PDE")
                .commercialStatus("validação comercial")
                .currentPriceBrl(new BigDecimal("47.00"))
                .marketNiche(niche)
                .targetAudience("Mulheres que querem parecer elegantes sem trocar o guarda-roupa inteiro")
                .primaryHypothesis("Mulheres desejam presença elegante com baixo esforço e baixo gasto.")
                .explicitPain("Sente que a aparência não comunica o valor pessoal.")
                .promise("Parecer mais elegante em 7 dias.")
                .uniqueMechanism("Curadoria guiada de presença visual com base em Adam e Galinsky (2012).")
                .languageStyle("Sofisticada, prática e acolhedora.")
                .colorPalette("1. Vinho MUSA #7A2444; 2. Dourado #D6A75C; 3. Creme #FFF8F3; 4. Grafite #2F2A2C; 5. Blush #F3C9C1; 6. Oliva #6F7A52; 7. Champanhe #F7E4C6.")
                .tripwire("Experiência guiada de 7 dias com diagnóstico, missões, checklists e templates.")
                .socialProof("Prova científica, prova visual e experimento 66.")
                .funnel("Anúncio → login → experiência gratuita → paywall → compra.")
                .codeModules("pde-platform, backend")
                .aiCost(new BigDecimal("1.20"))
                .build();

        when(productRepository.findBySlug("metodo-musa-7-dias")).thenReturn(Optional.of(product));

        String markdown = service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias");

        assertThat(markdown).contains("# Definição de Produto para Mercado — Método MUSA - Presença Elegante em 7 Dias");
        assertThat(markdown).contains("## 2. Mercado e nicho");
        assertThat(markdown).contains("Elegância feminina prática");
        assertThat(markdown).contains("## 4. Dor, resultado e mecanismo");
        assertThat(markdown).contains("Resultado prometido");
        assertThat(markdown).contains("Parecer mais elegante em 7 dias.");
        assertThat(markdown).contains("Adam e Galinsky (2012)");
        assertThat(markdown).contains("Paleta visual completa");
        assertThat(markdown).contains("7. Champanhe #F7E4C6");
        assertThat(markdown).contains("Experiência guiada de 7 dias");
        assertThat(markdown).contains("Prova científica");
        assertThat(markdown).doesNotContain("pde-platform");
        assertThat(markdown).doesNotContain("1.20");
    }

    /** Deve aceitar o identificador interno como fallback quando o código for numérico. */
    @Test
    void buildPublicMarketingDefinitionMarkdownByNumericCode() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository);
        Product product = Product.builder().id(7L).name("Produto 7").build();

        when(productRepository.findBySlug("7")).thenReturn(Optional.empty());
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        String markdown = service.buildPublicMarketingDefinitionMarkdown("7");

        assertThat(markdown).contains("Produto 7");
    }

    /** Deve retornar erro controlado quando o produto não existir. */
    @Test
    void buildPublicMarketingDefinitionMarkdownNotFound() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository);

        when(productRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buildPublicMarketingDefinitionMarkdown("inexistente"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Produto não encontrado");
    }
}
