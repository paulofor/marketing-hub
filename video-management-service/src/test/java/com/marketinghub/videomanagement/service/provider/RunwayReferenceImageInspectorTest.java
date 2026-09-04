package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: comprovar a inspeção raster e a imutabilidade das referências Product UGC. */
class RunwayReferenceImageInspectorTest {
    private MockWebServer server;
    private RunwayReferenceImageInspector inspector;

    /** Inicializa um servidor local e permite HTTP somente dentro deste teste. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        inspector = new RunwayReferenceImageInspector(WebClient.builder(), true);
    }

    /** Encerra o servidor de imagens simulado. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Aceita PNGs decodificáveis e registra dimensões, origem e hash sem guardar a URL completa. */
    @Test
    void shouldInspectBothProductUgcRasterReferences() throws Exception {
        byte[] character = png(108, 192, Color.PINK);
        byte[] product = png(120, 200, Color.BLUE);
        server.enqueue(image(character));
        server.enqueue(image(product));

        List<RunwayReferenceImageInspector.Evidence> result =
                inspector.inspectProductUgc(
                        server.url("/presenter.png").toString(),
                        server.url("/product.png").toString());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("CHARACTER_IMAGE");
        assertThat(result.get(0).contentType()).isEqualTo("image/png");
        assertThat(result.get(0).width()).isEqualTo(108);
        assertThat(result.get(0).height()).isEqualTo(192);
        assertThat(result.get(0).sha256()).matches("[0-9a-f]{64}");
        assertThat(result.get(1).role()).isEqualTo("PRODUCT_IMAGE");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Recusa o fallback HTML de uma SPA antes de qualquer chamada faturável à Runway. */
    @Test
    void shouldRejectHtmlReturnedByAnImageUrl() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "text/html")
                        .setBody("<html>aplicação</html>"));

        assertThatThrownBy(
                        () ->
                                inspector.inspectProductUgc(
                                        server.url("/missing.png").toString(),
                                        server.url("/unused.png").toString()))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "PROVIDER_REFERENCE_INVALID")
                .hasMessageContaining("não retornou PNG/JPEG");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Detecta qualquer mudança de bytes entre o preflight e a geração paga. */
    @Test
    void shouldRejectReferenceDriftAgainstFrozenEvidence() {
        List<RunwayReferenceImageInspector.Evidence> frozen =
                List.of(
                        evidence("CHARACTER_IMAGE", "a".repeat(64)),
                        evidence("PRODUCT_IMAGE", "b".repeat(64)));
        JsonNode expected =
                new ObjectMapper()
                        .valueToTree(frozen.stream().map(inspector::audit).toList());
        List<RunwayReferenceImageInspector.Evidence> changed =
                List.of(
                        evidence("CHARACTER_IMAGE", "a".repeat(64)),
                        evidence("PRODUCT_IMAGE", "c".repeat(64)));

        assertThatThrownBy(() -> inspector.requireMatches(expected, changed))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "PROVIDER_REFERENCE_DRIFT")
                .hasMessageContaining("mudou depois do preflight");
    }

    /** Produz um PNG real para validar o decodificador usado em produção. */
    private byte[] png(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    /** Serve uma imagem raster com cabeçalho compatível com a API pública. */
    private MockResponse image(byte[] content) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody(new Buffer().write(content));
    }

    /** Cria uma evidência congelada para testar a proteção contra alteração. */
    private RunwayReferenceImageInspector.Evidence evidence(String role, String sha256) {
        return new RunwayReferenceImageInspector.Evidence(
                role, "assets.example", "image/png", 1000, 1080, 1920, sha256);
    }
}
