package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

/** Reproduz composição real e rejeita fontes inadequadas antes de qualquer inferência paga. */
class DeliveryExamplesMontageTest {
    private final ObjectMapper mapper = new ObjectMapper();

    /** Recusa cruzamentos, recibos adulterados e falsa variedade sem gerar voz ou vídeo. */
    @ParameterizedTest
    @ValueSource(strings = {"tenant", "product", "experiment", "project", "version", "rights", "repeated", "hash", "long"})
    void rejectsInvalidSourceBeforeVoice(String scenario) throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            byte[] a = pixels(900, scenario.equals("long") ? 5000 : 900, Color.PINK);
            byte[] b = pixels(900, 900, Color.CYAN);
            server.enqueue(response(a)); server.enqueue(response(b));
            ObjectNode metadata = metadata(server, a, b, 9007);
            var examples = (ObjectNode) metadata.at("/post_production/deliveryExamples");
            var shot = (ObjectNode) examples.path("shots").get(0);
            switch (scenario) {
                case "tenant" -> examples.put("tenantId", "other");
                case "product" -> examples.put("productId", 9008);
                case "experiment" -> examples.put("experimentId", 99008);
                case "project" -> examples.put("projectId", 9008);
                case "version" -> examples.put("productVersion", "other-v2");
                case "rights" -> shot.put("rightsStatement", "");
                case "repeated" -> ((ObjectNode) examples.path("shots").get(1)).put("sha256", hash(a));
                case "hash" -> shot.put("sha256", "c".repeat(64));
            }
            assertThatThrownBy(() -> new DeliveryExamplesMontage(new VideoManagementProperties(), WebClient.builder()).prepare(metadata, 90099L))
                    .isInstanceOf(VideoProviderException.class).hasFieldOrPropertyWithValue("code", "DELIVERY_EXAMPLES_INVALID");
            assertThat(server.getRequestCount()).isLessThanOrEqualTo(1);
        }
    }

    /** Confere dois produtos com FFmpeg real: variedade, movimento, duração e manifesto final. */
    @ParameterizedTest @ValueSource(longs = {9007, 9010})
    @EnabledIfSystemProperty(named = "video.delivery.real-ffmpeg", matches = "true")
    void composesRealFramesForIndependentProducts(long product) throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            byte[] a = pixels(900, 900, Color.PINK), b = pixels(900, 900, Color.CYAN);
            server.enqueue(response(a)); server.enqueue(response(b));
            ObjectNode metadata = metadata(server, a, b, product);
            var montage = new DeliveryExamplesMontage(new VideoManagementProperties(), WebClient.builder());
            try (var sources = montage.prepare(metadata, 99001L)) {
                var result = montage.compose(sources, metadata,
                        List.of(new PdeProofCaptionTiming.Window(0, 3, 0), new PdeProofCaptionTiming.Window(3, 6, 1)), 6);
                try {
                    assertThat(result.audit()).containsEntry("commercialEvidenceClaimed", false)
                            .containsEntry("runtimeCaptureClaimed", false).containsEntry("status", "APPLIED");
                    byte[] early = frame(result.videoFile(), .1), later = frame(result.videoFile(), 1.2);
                    assertThat(early).isNotEqualTo(later);
                    assertThat(frame(result.videoFile(), 4)).isNotEqualTo(later);
                    assertThat(duration(result.videoFile())).isBetween(5.95, 6.05);
                } finally { Files.deleteIfExists(result.videoFile()); }
            }
        }
    }

    /** Executa o finalizador completo com fontes locais e áudio preservado, sem acessar APIs externas. */
    @Test @EnabledIfSystemProperty(named = "video.delivery.preview", matches = ".+")
    void previewsExistingNarrationWithLocalSources() throws Exception {
        Path directory = Path.of(System.getProperty("video.delivery.preview"));
        for (String name : List.of("ad", "demo")) {
            ObjectNode metadata = (ObjectNode) mapper.readTree(Files.readString(directory.resolve(name + ".json")));
            try (var server = new MockWebServer()) {
                server.setDispatcher(new Dispatcher() {
                    /** Entrega somente fixtures locais e contabiliza qualquer tentativa externa inesperada. */
                    @Override public MockResponse dispatch(RecordedRequest request) {
                        try {
                            String file = request.getPath().substring(1);
                            if (file.contains("..") || file.contains("/")) return new MockResponse().setResponseCode(400);
                            return response(Files.readAllBytes(directory.resolve(file)));
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(DeliveryExamplesMontageTest.class)
                                    .error("Fixture audiovisual indisponível; path={}", request.getPath(), ex);
                            throw new AssertionError("Requisição fora da fixture local: " + request.getPath(), ex);
                        }
                    }
                });
                server.start();
                metadata.put("sourceVideoUrl", server.url("/" + name + "-source.mp4").toString());
                for (var shot : metadata.at("/post_production/deliveryExamples/shots"))
                    ((ObjectNode) shot).put("url", server.url("/image-" + shot.path("visualAssetId").asLong() + ".png").toString());
                for (var segment : metadata.at("/preservedNarration/segments"))
                    ((ObjectNode) segment).put("url", server.url("/" + name + "-voice-" + segment.path("segmentIndex").asInt() + ".mp3").toString());
                var job = mapper.treeToValue(mapper.createObjectNode().put("id", 99099).put("profileId", 99098)
                        .put("tenantId", metadata.path("tenantId").asText()).put("metadataJson", metadata.toString()), SalesVideoJob.class);
                var profile = mapper.treeToValue(mapper.createObjectNode().put("id", 99098)
                        .put("targetDurationSeconds", metadata.path("postProductionDurationSeconds").asInt()), SalesVideoProfile.class);
                var properties = new VideoManagementProperties();
                properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
                properties.getProviders().getPostProduction().setOpenAiApiKey("local-fixture-no-external-access");
                properties.getProviders().getPostProduction().setOpenAiBaseUrl(server.url("/").uri());
                var result = new PostProductionVideoProvider(properties, mapper, WebClient.builder()).render(job, profile, (p, s, m) -> {});
                Files.write(directory.resolve(name + "-preview.mp4"), result.videoFile().content());
                Files.writeString(directory.resolve(name + "-result.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.metadata()));
                assertThat(result.metadata().get("tts_interactions").toString()).contains("TEXT_TO_SPEECH_REUSE", "incremental_cost_usd=0");
                assertThat(result.auditFiles()).isEmpty();
            }
        }
    }

    /** Cria contrato isolado do mercado com fontes entregues pelo servidor local. */
    private ObjectNode metadata(MockWebServer server, byte[] a, byte[] b, long product) throws Exception {
        var m = mapper.createObjectNode().put("tenantId", "fixture").put("productId", product)
                .put("experimentId", product + 1).put("videoProjectId", product + 2).put("campaign_key", "fixture-v3")
                .put("captionText", "Veja uma amostra do kit. | Confira uma segunda arte.");
        m.putObject("technicalQualityGate").put("captionMustMatchNarration", true);
        var c = m.putObject("post_production").putObject("deliveryExamples");
        c.put("contractVersion", "APPROVED_DELIVERY_EXAMPLES_V1").put("tenantId", "fixture")
                .put("productId", product).put("experimentId", product + 1).put("projectId", product + 2)
                .put("productVersion", "fixture-v3").put("commercialEvidenceClaimed", false).put("runtimeCaptureClaimed", false);
        var shots = c.putArray("shots");
        for (int i = 0; i < 2; i++) shots.addObject().put("visualAssetId", 91001 + i).put("captionCueIndex", i)
                .put("sha256", hash(i == 0 ? a : b)).put("url", server.url("/" + i + ".png").toString())
                .put("origin", "Fixture").put("rightsStatement", "Somente teste local");
        return m;
    }

    /** Produz pixels determinísticos sem gerar arte comercial ou consumir serviços externos. */
    private byte[] pixels(int width, int height, Color color) throws Exception {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(color); g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK); g.fillOval(100, 100, 300, 300); g.dispose();
        var out = new ByteArrayOutputStream(); ImageIO.write(image, "png", out); return out.toByteArray();
    }
    /** Calcula identidade exata dos bytes usados pelo contrato da fixture. */
    private String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    /** Entrega conteúdo binário local sem dependência de CDN. */
    private MockResponse response(byte[] bytes) { return new MockResponse().setBody(new Buffer().write(bytes)); }
    /** Extrai frame para detectar ausência de movimento e troca de conteúdo. */
    private byte[] frame(Path video, double time) throws Exception {
        var process = new ProcessBuilder("ffmpeg", "-v", "error", "-ss", "" + time, "-i", video.toString(), "-frames:v", "1", "-vf", "scale=108:192", "-f", "rawvideo", "-pix_fmt", "rgb24", "-").start();
        byte[] bytes = process.getInputStream().readAllBytes(); assertThat(process.waitFor()).isZero(); return bytes;
    }
    /** Mede duração real, sem confiar na resposta do compositor. */
    private double duration(Path video) throws Exception {
        var process = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", video.toString()).start();
        String seconds = new String(process.getInputStream().readAllBytes()); assertThat(process.waitFor()).isZero(); return Double.parseDouble(seconds.strip());
    }
}
