package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

    /** Confirma que o catálogo prioriza o contrato PDE publicado pelo Marketing Hub. */
    @Test
    void returnsMarketingHubProductWhenConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience"))
                .andRespond(withSuccess("""
                        {
                          "slug": "metodo-musa-7-dias",
                          "name": "Método MUSA pelo Hub",
                          "promise": "Promessa publicada pelo Marketing Hub",
                          "audience": "Mulheres urbanas",
                          "priceLabel": "",
                          "theme": {
                            "primary": "#7a2444",
                            "accent": "#d6a75c",
                            "background": "#fff8f3",
                            "imageUrl": "/assets/musa-cover.png"
                          },
                          "diagnostic": {
                            "title": "Mapa de Presença",
                            "intro": "Entrada publicada no Hub",
                            "questions": ["Pergunta 1"]
                          },
                          "missions": [],
                          "supportMaterials": [],
                          "scientificEvidencePack": {
                            "version": "musa-evidence-pack-v1",
                            "principles": [],
                            "practicalApplications": [],
                            "allowedLanguage": [],
                            "forbiddenClaims": [],
                            "references": []
                          },
                          "completionOffer": "Continuidade"
                        }
                        """, MediaType.APPLICATION_JSON));

        var product = service.getProduct("metodo-musa-7-dias");

        assertThat(product.name()).isEqualTo("Método MUSA pelo Hub");
        assertThat(product.promise()).isEqualTo("Promessa publicada pelo Marketing Hub");
        server.verify();
    }

    /** Confirma que homologação pode publicar uma versão de experiência sem trocar o contrato base. */
    @Test
    void appliesExperienceVersionOverrideWhenConfigured() {
        ProductCatalogService service = new ProductCatalogService(
                RestClient.builder(),
                "",
                "musa-pde-entry-v4-video-hero");

        var product = service.getProduct("metodo-musa-7-dias");

        assertThat(product.experienceVersion()).isEqualTo("musa-pde-entry-v4-video-hero");
        assertThat(product.funnelVersion()).isEqualTo("musa-membership-funnel-v1");
    }

    /** Confirma que o catálogo tenta a próxima base quando a primeira URL do Hub falha. */
    @Test
    void returnsMarketingHubProductFromFallbackBaseUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(
                builder,
                "http://marketing-hub-unavailable, http://marketing-hub",
                "");
        server.expect(requestTo("http://marketing-hub-unavailable/api/products/public/metodo-musa-7-dias/pde-experience"))
                .andRespond(withServerError());
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience"))
                .andRespond(withSuccess("""
                        {
                          "slug": "metodo-musa-7-dias",
                          "name": "Método MUSA pela segunda base",
                          "promise": "Promessa publicada pelo fallback",
                          "audience": "Mulheres urbanas",
                          "priceLabel": "",
                          "theme": {
                            "primary": "#7a2444",
                            "accent": "#d6a75c",
                            "background": "#fff8f3",
                            "imageUrl": "/assets/musa-cover.png"
                          },
                          "diagnostic": {
                            "title": "Mapa de Presença",
                            "intro": "Entrada publicada no Hub",
                            "questions": ["Pergunta 1"]
                          },
                          "missions": [],
                          "supportMaterials": [],
                          "scientificEvidencePack": {
                            "version": "musa-evidence-pack-v1",
                            "principles": [],
                            "practicalApplications": [],
                            "allowedLanguage": [],
                            "forbiddenClaims": [],
                            "references": []
                          },
                          "completionOffer": "Continuidade"
                        }
                        """, MediaType.APPLICATION_JSON));

        var product = service.getProduct("metodo-musa-7-dias");

        assertThat(product.name()).isEqualTo("Método MUSA pela segunda base");
        server.verify();
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
