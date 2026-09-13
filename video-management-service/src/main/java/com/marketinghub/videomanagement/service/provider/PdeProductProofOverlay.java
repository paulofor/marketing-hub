package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Compõe a prova privada e íntegra do PDE nos cortes funcionais, sem gerar ou reescrever a tela. */
final class PdeProductProofOverlay {
    private static final Logger log = LoggerFactory.getLogger(PdeProductProofOverlay.class);
    private final VideoManagementProperties properties;
    private final WebClient backend;

    /** Configura acesso somente ao backend oficial do módulo de vídeo, sem redirecionamentos. */
    PdeProductProofOverlay(VideoManagementProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.backend = builder.clone().baseUrl(properties.getBackendBaseUrl().toString())
                .codecs(value -> value.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)).build();
    }

    /** Confere bytes, hash, enquadramento e cortes antes de consumir o gerador pago. */
    void verify(JsonNode metadata, Long jobId) {
        if (!metadata.at("/post_production/product_proof").isMissingNode()) load(metadata, jobId);
    }

    /** Insere a captura nos cortes de mecanismo, resultado e prova, reservando área para legenda. */
    ProductUgcReferenceOverlay.OverlayResult apply(Path source, JsonNode metadata, Long jobId) {
        if (metadata.at("/post_production/product_proof").isMissingNode()) {
            return new ProductUgcReferenceOverlay.OverlayResult(source, Map.of());
        }
        Proof proof = load(metadata, jobId);
        Path image = null;
        Path output = null;
        Path processLog = null;
        try {
            image = Files.createTempFile("pde-proof-" + jobId, ".png");
            output = Files.createTempFile("pde-proof-" + jobId, ".mp4");
            processLog = Files.createTempFile("pde-proof-" + jobId, ".log");
            Files.write(image, proof.bytes());
            String crop = "crop=%d:%d:%d:%d,".formatted(proof.width(), proof.height(), proof.x(), proof.y());
            String filter = "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2[base];"
                    + "[1:v]" + crop + "scale=972:1360:force_original_aspect_ratio=decrease,"
                    + "pad=1080:1920:(ow-iw)/2:180:color=0xf8f3ee[proof];"
                    + "[base][proof]overlay=0:0:enable='" + proof.intervals() + "'[out]";
            List<String> command = List.of(properties.getProviders().getPostProduction().getFfmpegPath(),
                    "-hide_banner", "-loglevel", "error", "-y", "-i", source.toString(),
                    "-loop", "1", "-i", image.toString(), "-filter_complex", filter,
                    "-map", "[out]", "-map", "0:a?", "-c:v", "libx264", "-preset", "veryfast",
                    "-crf", "20", "-pix_fmt", "yuv420p", "-c:a", "copy", "-t", number(proof.duration()),
                    "-movflags", "+faststart", output.toString());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(processLog.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw invalid("A composição da prova excedeu o tempo limite.");
            }
            if (process.exitValue() != 0) throw invalid("A composição da prova falhou: " + Files.readString(processLog));
            Map<String, Object> audit = Map.of(
                    "contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1", "status", "APPLIED",
                    "sha256", proof.sha256(), "source", proof.contentPath(),
                    "crop", List.of(proof.x(), proof.y(), proof.width(), proof.height()),
                    "intervals", proof.intervals(), "syntheticScenario", true,
                    "commercialEvidenceClaimed", false);
            return new ProductUgcReferenceOverlay.OverlayResult(output, audit);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            delete(output, jobId);
            log.error("Falha ao compor prova do PDE; jobId={}", jobId, ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("PDE_PRODUCT_PROOF_INVALID", "Não foi possível compor a prova privada do PDE.", ex);
        } finally {
            delete(image, jobId);
            delete(processLog, jobId);
        }
    }

    /** Baixa somente a rota de vídeo esperada e recusa captura ou contexto divergentes. */
    private Proof load(JsonNode metadata, Long jobId) {
        JsonNode proof = metadata.at("/post_production/product_proof");
        long projectId = metadata.path("videoProjectId").asLong();
        String path = "/api/sales-videos/projects/" + projectId + "/product-proof";
        String tenantId = metadata.path("tenantId").asText();
        if (tenantId.isBlank() || !tenantId.equals(proof.path("tenantId").asText())
                || projectId <= 0 || projectId != proof.path("projectId").asLong()
                || !path.equals(proof.path("contentPath").asText())
                || !"PDE_PRIVATE_VIDEO_PROOF_V1".equals(proof.path("contractVersion").asText())
                || metadata.path("productId").asLong() != proof.path("productId").asLong()
                || metadata.path("experimentId").asLong() != proof.path("experimentId").asLong()
                || proof.path("commercialEvidenceClaimed").asBoolean(true)) {
            throw invalid("A prova visual não corresponde ao projeto, produto ou experimento.");
        }
        try {
            log.info("Solicitando prova privada do PDE; jobId={} endpoint={}", jobId, path);
            var request = backend.get().uri(path).header("X-Tenant-ID", tenantId);
            if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank()) {
                request.header("Authorization", "Bearer " + properties.getAuthToken());
            }
            byte[] bytes = request.retrieve().bodyToMono(byte[].class).block(Duration.ofSeconds(60));
            if (bytes == null || bytes.length == 0) throw invalid("Captura vazia.");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(proof.path("sha256").asText())) throw invalid("A captura mudou depois da preparação do vídeo.");
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw invalid("A prova não é uma imagem válida.");
            int x = 0, y = 0, width = image.getWidth(), height = image.getHeight();
            JsonNode crop = proof.path("crop");
            if (!crop.isMissingNode()) {
                if (!crop.isArray() || crop.size() != 4) throw invalid("Enquadramento inválido.");
                x = crop.get(0).asInt(-1); y = crop.get(1).asInt(-1);
                width = crop.get(2).asInt(0); height = crop.get(3).asInt(0);
                if (x < 0 || y < 0 || width <= 0 || height <= 0
                        || (long) x + width > image.getWidth() || (long) y + height > image.getHeight()) {
                    throw invalid("O enquadramento ultrapassa a captura homologada.");
                }
            }
            double elapsed = 0;
            List<String> intervals = new ArrayList<>();
            for (JsonNode cut : metadata.path("cut_plan")) {
                double seconds = cut.path("duration_seconds").asDouble();
                if (!Double.isFinite(seconds) || seconds <= 0) throw invalid("Duração de corte inválida.");
                if (List.of("MECANISMO", "RESULTADO", "PROVA").contains(cut.path("role").asText())) {
                    intervals.add("between(t," + number(elapsed) + "," + number(elapsed + seconds) + ")");
                }
                elapsed += seconds;
            }
            if (intervals.isEmpty() || Math.abs(elapsed - metadata.path("targetDurationSeconds").asDouble()) > .1) {
                throw invalid("A prova exige plano de cortes com duração integral e papel funcional.");
            }
            log.info("Prova privada recebida e íntegra; jobId={} endpoint={} bytes={} sha256={}", jobId, path, bytes.length, hash);
            return new Proof(bytes, hash, path, x, y, width, height, String.join("+", intervals), elapsed);
        } catch (IOException | NoSuchAlgorithmException | RuntimeException ex) {
            log.error("Falha ao validar prova privada; jobId={} endpoint={}", jobId, path, ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("PDE_PRODUCT_PROOF_INVALID", "A prova privada não pôde ser validada.", ex);
        }
    }

    /** Formata segundos com ponto decimal para o contrato do ffmpeg. */
    private String number(double value) { return String.format(Locale.ROOT, "%.3f", value); }

    /** Representa o bloqueio funcional sem tentar outro arquivo ou gerar prova sintética. */
    private VideoProviderException invalid(String message) {
        return new VideoProviderException("PDE_PRODUCT_PROOF_INVALID", message);
    }

    /** Limpa somente o temporário criado por esta composição. */
    private void delete(Path path, Long jobId) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException ex) { log.warn("Falha ao limpar prova temporária; jobId={} path={}", jobId, path, ex); }
    }

    /** Mantém pixels e enquadramento auditáveis durante a composição local. */
    private record Proof(byte[] bytes, String sha256, String contentPath, int x, int y,
                         int width, int height, String intervals, double duration) {}
}
