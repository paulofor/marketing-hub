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

        assertThat(product.experienceVersion()).isEqualTo("musa-pde-entry-v5-video-explicativo");
        assertThat(product.layoutKey()).isEqualTo("video-explicativo");
        assertThat(product.promise()).contains("7 dias");
        assertThat(product.priceLabel()).isEqualTo("R$67");
        assertThat(product.missions()).hasSize(7);
        assertThat(product.supportMaterials()).hasSize(4);
        assertThat(product.heroVideos()).singleElement().satisfies(heroVideo -> {
            assertThat(heroVideo.experienceVersion()).isEqualTo("musa-pde-entry-v6-video-motivacional");
            assertThat(heroVideo.posterUrl()).isNull();
            assertThat(heroVideo.autoplay()).isFalse();
            assertThat(heroVideo.muted()).isFalse();
            assertThat(heroVideo.controls()).isTrue();
        });
        assertThat(product.scientificEvidencePack().version()).isEqualTo("musa-evidence-pack-v1");
        assertThat(product.scientificEvidencePack().forbiddenClaims()).contains("garante elegância");
    }

    /** Confirma que o catálogo público não entrega missões, materiais ou evidência interna pagos. */
    @Test
    void hidesPaidMusaContentFromPublicCatalog() {
        ProductCatalogService service = new ProductCatalogService();

        var publicProduct = service.getPublicProductForRequest(
                "metodo-musa-7-dias",
                "v7.clubemusa.com.br",
                "v7",
                "musa-pde-entry-v7-espelho-antes-de-sair");

        assertThat(publicProduct.priceLabel()).isEqualTo("R$67");
        assertThat(publicProduct.missions()).isEmpty();
        assertThat(publicProduct.supportMaterials()).isEmpty();
        assertThat(publicProduct.scientificEvidencePack()).isNull();
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
                          "priceLabel": "R$67",
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

    /** Preserva escopo, provas, processo e binding comercial ao reduzir o contrato para uso público. */
    @Test
    void preservesAssistedCommercialV2FieldsInPublicContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/kit-whatsapp-pronto/pde-experience"))
                .andRespond(withSuccess("""
                        {
                          "slug":"kit-whatsapp-pronto",
                          "experienceVersion":"kit-whatsapp-pronto-pde-v2",
                          "layoutKey":"assisted-service-v2",
                          "promise":"Promessa canônica",
                          "serviceScope":{"includedItems":["10 a 20 respostas"],"excludedItems":["bot"],"deadlineStartsWhen":"Após briefing completo"},
                          "publicProofs":[{"id":"sample-response","type":"RESPONSE","title":"Resposta","content":"Prova fiel","items":[],"evidenceLabel":"Interface real","source":"tasting-v1"}],
                          "commercialProcess":[{"order":1,"title":"Briefing","description":"Entrada guiada","timing":"Após pagamento"}],
                          "commercialBinding":{"experimentId":89,"primaryCta":"Quero meu atendimento sob medida","priceBrl":349,"billingModel":"ONE_TIME"},
                          "missions":[],"supportMaterials":[]
                        }
                        """, MediaType.APPLICATION_JSON));

        var product = service.getPublicProductForRequest("kit-whatsapp-pronto", "", "", "");

        assertThat(product.serviceScope().includedItems()).containsExactly("10 a 20 respostas");
        assertThat(product.publicProofs()).singleElement().satisfies(proof ->
                assertThat(proof.source()).isEqualTo("tasting-v1"));
        assertThat(product.commercialProcess()).singleElement().satisfies(step ->
                assertThat(step.title()).isEqualTo("Briefing"));
        assertThat(product.commercialBinding().experimentId()).isEqualTo(89L);
        assertThat(product.missions()).isEmpty();
        server.verify();
    }

    /** Confirma que host versionado pede ao Marketing Hub o contrato publicado do slot. */
    @Test
    void requestsMarketingHubContractForVersionedSlotHost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v6"))
                .andRespond(withSuccess("""
                        {
                          "slug": "metodo-musa-7-dias",
                          "experienceVersion": "musa-v6-teste-publicado",
                          "layoutKey": "layout-custom-v6",
                          "name": "Método MUSA v6 editável",
                          "promise": "Promessa independente da v6",
                          "audience": "Mulheres urbanas",
                          "priceLabel": "R$67",
                          "theme": {
                            "primary": "#7a2444",
                            "accent": "#d6a75c",
                            "background": "#fff8f3",
                            "imageUrl": "/assets/musa-cover.png"
                          },
                          "diagnostic": {
                            "title": "Mapa v6",
                            "intro": "Entrada v6 publicada no Hub",
                            "questions": ["Pergunta v6"]
                          },
                          "missions": [],
                          "supportMaterials": [],
                          "heroVideos": [],
                          "publicFirstFold": {
                            "headline": "Você se arruma, mas ainda sente que sua presença não acompanha a mulher que você quer ser?",
                            "supportingText": "Quatro escolhas rápidas mostram o sinal que deixa seu look comum.",
                            "videoCtaLabel": "Ver meu Mapa de Presença"
                          },
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

        var product = service.getProductForHost("metodo-musa-7-dias", "v6.clubemusa.com.br");

        assertThat(product.name()).isEqualTo("Método MUSA v6 editável");
        assertThat(product.experienceVersion()).isEqualTo("musa-v6-teste-publicado");
        assertThat(product.layoutKey()).isEqualTo("layout-custom-v6");
        assertThat(product.publicFirstFold().headline())
                .isEqualTo("Você se arruma, mas ainda sente que sua presença não acompanha a mulher que você quer ser?");
        assertThat(product.publicFirstFold().videoCtaLabel()).isEqualTo("Ver meu Mapa de Presença");
        server.verify();
    }

    /** Confirma que a v7 publicada pelo Hub não é mascarada pelo fallback local. */
    @Test
    void returnsPublishedV7ContractWithoutReplacingItWithLocalFallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v7"))
                .andRespond(withSuccess("""
                        {
                          "slug": "metodo-musa-7-dias",
                          "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair",
                          "layoutKey": "espelho-antes-de-sair",
                          "name": "Contrato publicado pelo Marketing Hub",
                          "promise": "Promessa publicada pela fonte canônica",
                          "audience": "Mulheres urbanas",
                          "priceLabel": "",
                          "theme": {"primary":"#000000","accent":"#000000","background":"#ffffff","imageUrl":""},
                          "diagnostic": {"title":"Antigo","intro":"Antigo","questions":[]},
                          "missions": [],
                          "supportMaterials": [],
                          "heroVideos": [{"playbackUrl":"https://example.com/antigo.mp4"}],
                          "completionOffer": "Assinatura antiga"
                        }
                        """, MediaType.APPLICATION_JSON));

        var product = service.getProductForHost("metodo-musa-7-dias", "v7.clubemusa.com.br");

        assertThat(product.name()).isEqualTo("Contrato publicado pelo Marketing Hub");
        assertThat(product.promise()).isEqualTo("Promessa publicada pela fonte canônica");
        assertThat(product.priceLabel()).isEmpty();
        assertThat(product.missions()).isEmpty();
        assertThat(product.heroVideos()).singleElement();
        assertThat(product.completionOffer()).isEqualTo("Assinatura antiga");
        server.verify();
    }

    /** Confirma que o slot enviado pelo frontend público sobrevive mesmo quando o proxy altera o Host. */
    @Test
    void requestsMarketingHubContractForExplicitSlotCodeWhenHostIsGeneric() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v6"))
                .andRespond(withSuccess("""
                        {
                          "slug": "metodo-musa-7-dias",
                          "experienceVersion": "musa-pde-entry-v6-video-motivacional",
                          "layoutKey": "video-motivacional",
                          "name": "Método MUSA v6",
                          "promise": "Promessa v6",
                          "audience": "Mulheres urbanas",
                          "priceLabel": "R$67",
                          "theme": {
                            "primary": "#7a2444",
                            "accent": "#d6a75c",
                            "background": "#fff8f3",
                            "imageUrl": "/assets/musa-cover.png"
                          },
                          "diagnostic": {
                            "title": "Mapa v6",
                            "intro": "Entrada v6 publicada no Hub",
                            "questions": ["Pergunta v6"]
                          },
                          "missions": [],
                          "supportMaterials": [],
                          "heroVideos": [],
                          "publicFirstFold": {
                            "headline": "Headline comercial publicada",
                            "videoCtaLabel": "Ver meu Mapa de Presença"
                          },
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

        var product = service.getProductForRequest(
                "metodo-musa-7-dias",
                "pde-platform-backend:8096",
                "v6",
                "musa-pde-entry-v6-video-motivacional");

        assertThat(product.layoutKey()).isEqualTo("video-motivacional");
        assertThat(product.publicFirstFold().headline()).isEqualTo("Headline comercial publicada");
        server.verify();
    }

    /** Confirma que o deploy pode publicar uma versão de experiência sem trocar o contrato base. */
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

    /** Confirma que o hostname versionado tem prioridade sobre override global de deploy. */
    @Test
    void appliesVersionedHostBeforeGlobalOverride() {
        ProductCatalogService service = new ProductCatalogService(
                RestClient.builder(),
                "",
                "musa-pde-entry-v5-video-explicativo");

        var product = service.getProductForHost("metodo-musa-7-dias", "v6.clubemusa.com.br:5176");

        assertThat(product.experienceVersion()).isEqualTo("musa-pde-entry-v6-video-motivacional");
        assertThat(product.funnelVersion()).isEqualTo("musa-membership-funnel-v1");
    }

    /** Confirma que slots futuros reservados usam a experiência estável até terem contrato próprio. */
    @Test
    void appliesStableExperienceForReservedFutureHosts() {
        ProductCatalogService service = new ProductCatalogService(
                RestClient.builder(),
                "",
                "musa-pde-entry-v5-video-explicativo");

        assertThat(service.getProductForHost("metodo-musa-7-dias", "v8.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
        assertThat(service.getProductForHost("metodo-musa-7-dias", "v9.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
        assertThat(service.getProductForHost("metodo-musa-7-dias", "v10.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
    }

    /** Confirma que a v7 local preserva a identidade comercial canônica sem vídeo, IA ou assinatura. */
    @Test
    void appliesCanonicalCommercialIdentityForV7Host() {
        ProductCatalogService service = new ProductCatalogService();

        var product = service.getProductForHost("metodo-musa-7-dias", "v7.clubemusa.com.br");

        assertThat(product.experienceVersion()).isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
        assertThat(product.layoutKey()).isEqualTo("espelho-antes-de-sair");
        assertThat(product.name()).isEqualTo("Método MUSA - Presença Elegante em 7 Dias");
        assertThat(product.promise())
                .isEqualTo("Organizar em sete dias escolhas práticas de presença elegante prioritariamente com o que a cliente já possui, sem promessa de transformação garantida.");
        assertThat(product.publicFirstFold().headline()).contains("Sua roupa fala antes de você");
        assertThat(product.publicFirstFold().videoKicker()).isEqualTo("Método MUSA em 7 dias");
        assertThat(product.publicFirstFold().videoKicker()).doesNotContain("v7");
        assertThat(product.heroVideos()).isEmpty();
        assertThat(product.scientificEvidencePack()).isNull();
        assertThat(product.completionOffer()).contains("90 dias", "sem assinatura").doesNotContain("desafios mensais");
        assertThat(product.missions()).hasSize(7);
        assertThat(product.missions()).extracting(ProductExperienceResponse.MissionDto::id)
                .containsExactly(
                        "dia-1-ruido-visual",
                        "dia-2-assinatura",
                        "dia-3-base-acessivel",
                        "dia-4-checklist-12-minutos",
                        "dia-5-compra-inteligente",
                        "dia-6-situacao-chave",
                        "dia-7-plano-pessoal");
        assertThat(product.missions()).allSatisfy(mission -> {
            assertThat(mission.interaction()).as(mission.id()).isNotNull();
            assertThat(mission.interaction().fields()).as(mission.id()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(mission.interaction().fields()).allSatisfy(field -> {
                assertThat(field.key()).isNotBlank();
                assertThat(field.options()).hasSizeGreaterThanOrEqualTo(4);
            });
        });
        assertThat(product.missions().get(1).interaction().title()).contains("peça-sinal");
        assertThat(product.missions().get(2).interaction().title()).contains("sinal de estrutura");
        assertThat(product.missions().get(4).interaction().title()).contains("duas cores");
        assertThat(product.missions().get(5).interaction().title()).contains("três sinais repetíveis");
        assertThat(product.missions().get(6).interaction().title()).contains("fórmula MUSA");
    }

    /** Confirma que slots legados continuam funcionais na experiência estável de entrada. */
    @Test
    void appliesStableEntryExperienceForLegacyHosts() {
        ProductCatalogService service = new ProductCatalogService(
                RestClient.builder(),
                "",
                "musa-pde-entry-v6-video-motivacional");

        assertThat(service.getProductForHost("metodo-musa-7-dias", "v1.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v5-video-explicativo");
        assertThat(service.getProductForHost("metodo-musa-7-dias", "v2.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v5-video-explicativo");
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
                          "priceLabel": "R$67",
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
