package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import javax.imageio.ImageIO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o gerador genérico de movimento editorial com imagens aprovadas. */
class EditorialMotionVideoProviderTest {
    private MockWebServer server;

    /** Inicializa o servidor que representa o storage de imagens aprovadas. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o storage simulado após cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve criar um MP4 vertical com cortes, fontes e custo local auditados. */
    @Test
    void shouldRenderEditorialMotionFromApprovedImages() throws Exception {
        server.enqueue(imageResponse(Color.DARK_GRAY));
        server.enqueue(imageResponse(new Color(122, 36, 68)));
        VideoManagementProperties properties = properties();
        EditorialMotionVideoProvider provider = new EditorialMotionVideoProvider(
                properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(jobWithSources(), profile(), (percent, status, message) -> { });

        assertThat(provider.supports(jobWithSources())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("editorial-motion-77");
        assertThat(artifacts.videoFile().content()).hasSizeGreaterThan(10_000);
        assertThat(new String(artifacts.videoFile().content(), 4, 4)).isEqualTo("ftyp");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "EDITORIAL_MOTION")
                .containsEntry("generation_mode", "DETERMINISTIC_KINETIC_STILL")
                .containsEntry("duration_seconds", 6)
                .containsEntry("cut_count", 2)
                .containsEntry("cost_usd", 0)
                .containsEntry("human_review_required", true);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Deve bloquear antes do ffmpeg quando não houver imagem aprovada. */
    @Test
    void shouldRejectMissingApprovedSource() {
        EditorialMotionVideoProvider provider = new EditorialMotionVideoProvider(
                properties(), new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job("{}"), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("imagem aprovada");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Configura a renderização editorial local usada no teste. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getEditorialMotion().setEnabled(true);
        properties.getProviders().getEditorialMotion().setWidth(360);
        properties.getProviders().getEditorialMotion().setHeight(640);
        properties.getProviders().getEditorialMotion().setFramesPerSecond(12);
        return properties;
    }

    /** Cria uma resposta PNG vertical pequena e válida. */
    private MockResponse imageResponse(Color color) throws Exception {
        BufferedImage image = new BufferedImage(180, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody(new Buffer().write(output.toByteArray()));
    }

    /** Cria um job com duas imagens versionadas e dois cortes comerciais. */
    private SalesVideoJob jobWithSources() {
        return job("""
                {
                  "editorial_source_images": [
                    {"assetId": 1924, "url": "%s/a.png"},
                    {"assetId": 1953, "url": "%s/b.png"}
                  ],
                  "cut_plan": [
                    {"duration_seconds": 3, "role": "HOOK_DOR"},
                    {"duration_seconds": 3, "role": "CTA"}
                  ]
                }
                """.formatted(baseUrl(), baseUrl()));
    }

    /** Retorna a base HTTP do storage simulado sem barra final. */
    private String baseUrl() {
        return server.url("/").toString().replaceAll("/$", "");
    }

    /** Cria um job editorial com o metadata informado. */
    private SalesVideoJob job(String metadataJson) {
        return new SalesVideoJob(
                77L, 57L, 558L, "default", SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "EDITORIAL_MOTION", null, SalesVideoJobType.RENDER, SalesVideoStatus.VIDEO_REQUESTED,
                1, null, null, null, 0, null, null, "teste@marketinghub.local", Instant.now(),
                null, null, null, null, null, null, metadataJson, Instant.now(), Instant.now());
    }

    /** Cria o perfil comercial mínimo necessário para o fallback editorial. */
    private SalesVideoProfile profile() {
        return new SalesVideoProfile(
                57L, 4L, null, "HERO", "Vega 91", "Mulheres 35 a 60", "Editorial",
                "Acolhedora", "pt-BR", 24, SalesVideoStatus.SCRIPT_READY,
                Instant.now(), Instant.now(), null, null);
    }
}
