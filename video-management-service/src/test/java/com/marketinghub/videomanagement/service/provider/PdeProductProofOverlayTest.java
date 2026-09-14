package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

/** Protege a prova privada contra troca de pixels, identidade e origem de download. */
class PdeProductProofOverlayTest {
    /** Verifica captura íntegra pelo backend do módulo, sem chamar provedor pago. */
    @Test
    void verifiesPrivatePixelsAndExactModulePath() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            byte[] pixels = pixels();
            server.enqueue(response(pixels));
            var properties = new VideoManagementProperties();
            properties.setBackendBaseUrl(server.url("/").uri());
            new PdeProductProofOverlay(properties, WebClient.builder()).verify(metadata(pixels), 91009L);
            var request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/api/sales-videos/projects/91001/product-proof");
            assertThat(request.getHeader("X-Tenant-ID")).isEqualTo("fixture-tenant");
            assertThat(server.getRequestCount()).isEqualTo(1);
        }
    }

    /** Bloqueia alterações de contrato antes da rede ou dos pixels antes de compor e consumir voz. */
    @ParameterizedTest
    @ValueSource(strings = {"tenant", "project", "product", "experiment", "url", "hash", "crop", "duration"})
    void rejectsInvalidProof(String scenario) throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            byte[] pixels = pixels();
            server.enqueue(response(pixels));
            ObjectNode metadata = metadata(pixels);
            var proof = (ObjectNode) metadata.at("/post_production/product_proof");
            switch (scenario) {
                case "tenant" -> proof.put("tenantId", "other-tenant");
                case "project" -> proof.put("projectId", 4);
                case "product" -> proof.put("productId", 4);
                case "experiment" -> proof.put("experimentId", 92);
                case "url" -> proof.put("contentPath", "https://outside.invalid/private");
                case "hash" -> proof.put("sha256", "changed");
                case "crop" -> proof.putArray("crop").add(0).add(0).add(2000).add(2000);
                case "duration" -> metadata.put("targetDurationSeconds", 99);
            }
            var properties = new VideoManagementProperties();
            properties.setBackendBaseUrl(server.url("/").uri());
            var overlay = new PdeProductProofOverlay(properties, WebClient.builder());
            assertThatThrownBy(() -> overlay.verify(metadata, 91009L))
                    .isInstanceOf(VideoProviderException.class)
                    .hasFieldOrPropertyWithValue("code", "PDE_PRODUCT_PROOF_INVALID");
            assertThat(server.getRequestCount()).isLessThanOrEqualTo(1);
            if (java.util.List.of("tenant", "project", "product", "experiment", "url").contains(scenario)) {
                assertThat(server.getRequestCount()).isZero();
            }
        }
    }

    /** Executa composição real quando a homologação local habilita explicitamente FFmpeg. */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "video.proof.real-ffmpeg", matches = "true")
    void composesPixelsWithRealFfmpeg() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            byte[] pixels = pixels();
            server.enqueue(response(pixels));
            var properties = new VideoManagementProperties();
            properties.setBackendBaseUrl(server.url("/").uri());
            Path source = Files.createTempFile("pde-proof-local-source", ".mp4");
            Path result = null;
            try {
                var process = new ProcessBuilder("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i",
                        "color=c=blue:s=360x640:r=15:d=15", "-c:v", "libx264", "-pix_fmt", "yuv420p", source.toString())
                        .inheritIO().start();
                assertThat(process.waitFor()).isZero();
                var output = new PdeProductProofOverlay(properties, WebClient.builder()).apply(source, metadata(pixels), 91009L);
                result = output.videoFile();
                assertThat(output.audit()).containsEntry("status", "APPLIED").containsEntry("commercialEvidenceClaimed", false);
                Path target = Path.of("target/pde-proof-real.mp4");
                Files.copy(result, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                var probe = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration:stream=width,height",
                        "-of", "json", target.toString()).start();
                var measured = new ObjectMapper().readTree(probe.getInputStream());
                assertThat(probe.waitFor()).isZero();
                assertThat(measured.at("/streams/0/width").asInt()).isEqualTo(1080);
                assertThat(measured.at("/streams/0/height").asInt()).isEqualTo(1920);
                assertThat(measured.at("/format/duration").asDouble()).isBetween(14.9, 15.1);
                assertThat(samplePixel(target, 1)[2] & 255).isGreaterThan(200);
                byte[] proofPixel = samplePixel(target, 6);
                assertThat(proofPixel[0] & 255).isGreaterThan(230);
                assertThat(proofPixel[1] & 255).isBetween(180, 220);
                assertThat(samplePixel(target, 14)[2] & 255).isGreaterThan(200);
            } finally {
                Files.deleteIfExists(source);
                if (result != null) Files.deleteIfExists(result);
            }
        }
    }

    /** Mede um pixel central para comprovar a presença e a retirada da prova nos instantes certos. */
    private byte[] samplePixel(Path video, int second) throws Exception {
        var process = new ProcessBuilder("ffmpeg", "-v", "error", "-ss", Integer.toString(second), "-i", video.toString(),
                "-frames:v", "1", "-vf", "crop=2:2:540:600,scale=1:1", "-f", "rawvideo", "-pix_fmt", "rgb24", "-").start();
        byte[] pixel = process.getInputStream().readAllBytes();
        assertThat(process.waitFor()).isZero();
        assertThat(pixel).hasSize(3);
        return pixel;
    }

    /** Produz apenas pixels artificiais de fixture, segregados de qualquer evidência real. */
    private byte[] pixels() throws Exception {
        var image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 300; x++) for (int y = 0; y < 400; y++) image.setRGB(x,y,0xffcc33);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    /** Devolve a captura local em resposta HTTP sem armazenamento externo. */
    private MockResponse response(byte[] pixels) {
        return new MockResponse().setHeader("Content-Type", "image/png").setBody(new Buffer().write(pixels));
    }

    /** Monta contrato isolado com cinco cortes e hash da fixture que será lida. */
    private ObjectNode metadata(byte[] pixels) throws Exception {
        var data = new ObjectMapper().createObjectNode();
        data.put("tenantId", "fixture-tenant").put("videoProjectId", 91001).put("productId", 91002).put("experimentId", 91003).put("targetDurationSeconds", 15);
        var proof = data.putObject("post_production").putObject("product_proof");
        proof.put("tenantId", "fixture-tenant").put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1").put("projectId", 91001)
                .put("productId", 91002).put("experimentId", 91003).put("commercialEvidenceClaimed", false)
                .put("contentPath", "/api/sales-videos/projects/91001/product-proof")
                .put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels)));
        var cuts = data.putArray("cut_plan");
        for (String role : java.util.List.of("HOOK_DOR", "MECANISMO", "RESULTADO", "PROVA", "CTA")) {
            cuts.addObject().put("role", role).put("duration_seconds", 3);
        }
        return data;
    }
}
