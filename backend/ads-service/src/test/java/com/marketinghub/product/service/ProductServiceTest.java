package com.marketinghub.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Deve persistir alterações comerciais em um produto já existente. */
    @Test
    void updateProduct() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);
        Product product = Product.builder().id(1L).name("Nome antigo").build();
        MarketNiche niche = new MarketNiche();
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Método MUSA - Presença Elegante em 7 Dias");
        request.setSlug("metodo-musa-7-dias");
        request.setLogoUrl("https://clubemusa.com.br/assets/logo-musa.svg");
        request.setMarketNicheId(10L);
        request.setCurrentPriceBrl(new BigDecimal("47.00"));
        request.setTargetAudience("Mulheres urbanas");
        request.setScientificEvidencePack("Evidence Pack MUSA v1");
        request.setPdeExperienceJson("{\"slug\":\"metodo-musa-7-dias\",\"missions\":[]}");
        request.setSevenDayJourney("Dia 1: diagnóstico; Dia 2: limpeza visual.");
        request.setSupportMaterialPositioning("Material de apoio como reforço secundário.");
        request.setPrimaryCta("Ver meu plano MUSA de 7 dias");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
        when(productRepository.save(product)).thenReturn(product);

        Product updated = service.updateProduct(1L, request);

        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getSlug()).isEqualTo(request.getSlug());
        assertThat(updated.getLogoUrl()).isEqualTo("https://clubemusa.com.br/assets/logo-musa.svg");
        assertThat(updated.getCurrentPriceBrl()).isEqualByComparingTo("47.00");
        assertThat(updated.getTargetAudience()).isEqualTo(request.getTargetAudience());
        assertThat(updated.getScientificEvidencePack()).isEqualTo("Evidence Pack MUSA v1");
        assertThat(updated.getPdeExperienceJson()).contains("\"metodo-musa-7-dias\"");
        assertThat(updated.getSevenDayJourney()).isEqualTo("Dia 1: diagnóstico; Dia 2: limpeza visual.");
        assertThat(updated.getSupportMaterialPositioning()).isEqualTo("Material de apoio como reforço secundário.");
        assertThat(updated.getPrimaryCta()).isEqualTo("Ver meu plano MUSA de 7 dias");
        assertThat(updated.getMarketNiche()).isSameAs(niche);
    }

    /** Deve montar uma definição pública em Markdown com foco comercial e sem detalhes técnicos. */
    @Test
    void buildPublicMarketingDefinitionMarkdown() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);
        MarketNiche niche = MarketNiche.builder().name("Elegância feminina prática").build();
        Product product = Product.builder()
                .id(1L)
                .slug("metodo-musa-7-dias")
                .name("Método MUSA - Presença Elegante em 7 Dias")
                .logoUrl("https://clubemusa.com.br/assets/logo-musa.svg")
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
                .sevenDayJourney("- **Dia 1 — Diagnóstico de presença:** identificar ruído visual.\n"
                        + "- **Dia 2 — Limpeza de ruído visual:** remover excessos sem comprar nada novo.")
                .supportMaterialPositioning("Material de apoio aparece como reforço secundário da jornada.")
                .primaryCta("Ver meu plano MUSA de 7 dias")
                .socialProof("Prova científica, prova visual e experimento 66.")
                .scientificEvidencePack("Evidence Pack MUSA v1: uso de IA associado aos artigos científicos citados, princípios permitidos, linguagem permitida, afirmações proibidas e referências científicas.")
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
        assertThat(markdown).contains("Logo");
        assertThat(markdown).contains("https://clubemusa.com.br/assets/logo-musa.svg");
        assertThat(markdown).contains("Parecer mais elegante em 7 dias.");
        assertThat(markdown).contains("Adam e Galinsky (2012)");
        assertThat(markdown).contains("Paleta visual completa");
        assertThat(markdown).contains("7. Champanhe #F7E4C6");
        assertThat(markdown).contains("Experiência guiada de 7 dias");
        assertThat(markdown).contains("Material de apoio aparece como reforço secundário da jornada.");
        assertThat(markdown).contains("CTA principal recomendado");
        assertThat(markdown).contains("Ver meu plano MUSA de 7 dias");
        assertThat(markdown).contains("## 7. Jornada de 7 dias");
        assertThat(markdown).contains("Dia 1 — Diagnóstico de presença");
        assertThat(markdown).contains("Dia 2 — Limpeza de ruído visual");
        assertThat(markdown).contains("## 8. Funil de aquisição e venda");
        assertThat(markdown).contains("Prova científica");
        assertThat(markdown).contains("Base científica operacional");
        assertThat(markdown).contains("uso de IA associado aos artigos científicos citados");
        assertThat(markdown).contains("afirmações proibidas");
        assertThat(markdown).doesNotContain("pde-platform");
        assertThat(markdown).doesNotContain("1.20");
    }

    /** Deve aceitar o identificador interno como fallback quando o código for numérico. */
    @Test
    void buildPublicMarketingDefinitionMarkdownByNumericCode() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);
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
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);

        when(productRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buildPublicMarketingDefinitionMarkdown("inexistente"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Produto não encontrado");
    }

    /** Deve expor o contrato JSON de experiência PDE salvo no cadastro do produto. */
    @Test
    void getPublicPdeExperienceJson() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);
        Product product = Product.builder()
                .slug("metodo-musa-7-dias")
                .pdeExperienceJson("{\"slug\":\"metodo-musa-7-dias\"}")
                .build();

        when(productRepository.findBySlug("metodo-musa-7-dias")).thenReturn(Optional.of(product));

        String json = service.getPublicPdeExperienceJson("metodo-musa-7-dias");

        assertThat(json).isEqualTo("{\"slug\":\"metodo-musa-7-dias\"}");
    }

    /** Deve rejeitar contrato PDE que não seja JSON válido antes de salvar o produto. */
    @Test
    void updateProductRejectsInvalidPdeExperienceJson() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository, objectMapper);
        Product product = Product.builder().id(1L).build();
        MarketNiche niche = new MarketNiche();
        CreateProductRequest request = new CreateProductRequest();
        request.setMarketNicheId(10L);
        request.setPdeExperienceJson("{json inválido");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));

        assertThatThrownBy(() -> service.updateProduct(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Contrato JSON da experiência PDE inválido");
    }
}
