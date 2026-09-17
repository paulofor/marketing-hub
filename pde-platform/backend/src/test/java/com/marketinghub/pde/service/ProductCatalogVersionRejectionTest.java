package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Impede que uma recusa canônica seja substituída por conteúdo comercial local. */
class ProductCatalogVersionRejectionTest {
    /** Preserva o catálogo local existente quando nenhuma versão específica foi solicitada. */
    @Test
    void preservesUnqualifiedLocalCatalogFallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(builder, "http://marketing-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/pausa-de-transicao/pde-experience"))
                .andRespond(withStatus(HttpStatusCode.valueOf(404)));

        org.assertj.core.api.Assertions.assertThat(service.getProduct("pausa-de-transicao").slug())
                .isEqualTo("pausa-de-transicao");
        server.verify();
    }

    /** Preserva recusas do Hub e não tenta outra base ou catálogo após erro funcional. */
    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 422})
    void preservesCanonicalRejection(int status) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(
                builder, "http://marketing-hub,http://another-hub", "");
        server.expect(requestTo("http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v5"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertThatThrownBy(() -> service.getPublicProductForRequest(
                "metodo-musa-7-dias", "v5.clubemusa.com.br", "", ""))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getStatusCode().value()).isEqualTo(status));
        server.verify();
    }

    /** Lê a candidata exata pelo canal interno sem converter a recusa em fallback local. */
    @Test
    void loadsExactCandidateThroughAuthenticatedPreflight() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductCatalogService service = new ProductCatalogService(
                builder, "http://marketing-hub", "", "internal-test-token");
        server.expect(requestTo(
                        "http://marketing-hub/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v8"))
                .andRespond(withStatus(HttpStatusCode.valueOf(409)));
        server.expect(requestTo(
                        "http://marketing-hub/api/internal/pde-validation-contract/v1/products/metodo-musa-7-dias/experience?slotCode=v8"))
                .andExpect(header("X-PDE-Internal-Token", "internal-test-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "slug":"metodo-musa-7-dias",
                          "experienceVersion":"musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                          "layoutKey":"espelho-antes-de-sair",
                          "name":"Método MUSA",
                          "promise":"Primeiro ajuste aplicável",
                          "theme":{"primary":"#000000","accent":"#000000","background":"#ffffff","imageUrl":""},
                          "diagnostic":{"title":"Diagnóstico","intro":"Entrada","questions":[]},
                          "missions":[],
                          "supportMaterials":[],
                          "heroVideos":[]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var product = service.getProductForHost(
                "metodo-musa-7-dias", "v8.clubemusa.com.br");

        org.assertj.core.api.Assertions.assertThat(product.experienceVersion())
                .isEqualTo("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
        server.verify();
    }
}
