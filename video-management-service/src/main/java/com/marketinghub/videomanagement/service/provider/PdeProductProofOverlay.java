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
        if (hasSequence(metadata)) {
            for (JsonNode shot : sequence(metadata)) verify(withShot(metadata, shot), jobId);
            return;
        }
        if (!metadata.at("/post_production/product_proof").isMissingNode()) {
            PdeProofCaptionTiming.cueIndex(metadata);
            String label = metadata.at("/post_production/product_proof/label").asText("").trim();
            if (label.length() > 48 || label.codePoints().anyMatch(Character::isISOControl))
                throw invalid("Identificação editorial inválida.");
            load(metadata, jobId);
        }
    }

    /** Preserva a composição legada sem locução sincronizada. */
    ProductUgcReferenceOverlay.OverlayResult apply(Path source, JsonNode metadata, Long jobId) {
        return apply(source, metadata, jobId, List.of());
    }

    /** Compõe a captura no trecho narrado com enquadramento explícito ou legado, preservando identidade. */
    ProductUgcReferenceOverlay.OverlayResult apply(Path source, JsonNode metadata, Long jobId,
                                                  List<PdeProofCaptionTiming.Window> captions) {
        if (hasSequence(metadata)) return applySequence(source, metadata, jobId, captions);
        if (metadata.at("/post_production/product_proof").isMissingNode()) {
            return new ProductUgcReferenceOverlay.OverlayResult(source, Map.of());
        }
        Proof proof = load(metadata, jobId);
        double outputDuration = Math.max(proof.duration(), metadata.path("postProductionDurationSeconds").asDouble());
        var timing = PdeProofCaptionTiming.resolve(metadata, captions);
        boolean contextual = timing != null
                && metadata.at("/post_production/product_proof/sequenceShot").asBoolean(false)
                && ((timing.cueIndex() == 0 && timing.start() < .1)
                    || timing.cueIndex() == captions.size() - 1);
        String requestedLayout = metadata.at("/post_production/product_proof/layout").asText("");
        if (!requestedLayout.isEmpty()) contextual = "MOTION_WITH_PRODUCT_PROOF_V1".equals(requestedLayout);
        boolean hybrid = DeliveryExamplesMontage.requested(metadata);
        String intervals = timing == null ? proof.intervals()
                : "between(t," + number(timing.start()) + "," + number(timing.end()) + ")";
        Path image = null;
        Path output = null;
        Path processLog = null;
        Path labelFile = null;
        String label = metadata.at("/post_production/product_proof/label").asText("").trim();
        if (label.length() > 48 || label.codePoints().anyMatch(Character::isISOControl))
            throw invalid("Identificação editorial inválida.");
        try {
            image = Files.createTempFile("pde-proof-" + jobId, ".png");
            output = Files.createTempFile("pde-proof-" + jobId, ".mp4");
            processLog = Files.createTempFile("pde-proof-" + jobId, ".log");
            Files.write(image, proof.bytes());
            String labelFilter = "";
            if (!label.isEmpty()) {
                labelFile = Files.createTempFile("pde-proof-label-", ".txt");
                Files.writeString(labelFile, wrapLabel(label));
                labelFilter = "drawtext=textfile=" + labelFile + ":expansion=none:fontsize=38:fontcolor=0x173f35:"
                        + "x=(w-text_w)/2:y=" + (hybrid ? "610" : contextual ? "750" : "110")
                        + ":enable='" + intervals + "',";
            }
            String crop = "crop=%d:%d:%d:%d,".formatted(proof.width(), proof.height(), proof.x(), proof.y());
            int proofHeight = metadata.at("/post_production/product_proof/sequenceShot").asBoolean(false) ? 1100 : 1360;
            String proofFrame = hybrid
                    ? (label.isEmpty() ? "scale=972:800:force_original_aspect_ratio=decrease,pad=1080:900:(ow-iw)/2:80:color=0xf8f3ee," : "scale=972:800:force_original_aspect_ratio=decrease,pad=1080:1020:(ow-iw)/2:200:color=0xf8f3ee,")
                    : contextual
                    ? (label.isEmpty() ? "scale=972:710:force_original_aspect_ratio=decrease,pad=1080:770:(ow-iw)/2:60:color=0xf8f3ee," : "scale=972:710:force_original_aspect_ratio=decrease,pad=1080:890:(ow-iw)/2:180:color=0xf8f3ee,")
                    : "scale=972:" + proofHeight + ":force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:" + (label.isEmpty() ? "180" : "230") + ":color=0xf8f3ee,";
            String filter = "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2[base];"
                    + "[1:v]" + crop + proofFrame
                    + "format=yuva420p,loop=loop=-1:size=1:start=0,setpts=N/(25*TB)[proof];"
                    + "[base][proof]overlay=0:" + (hybrid ? "540" : contextual ? "690" : "0") + ":enable='" + intervals + "',"
                    + labelFilter
                    + "drawtext=text='Exemplo com dados de teste':fontsize=38:fontcolor=0x333333:"
                    + "x=(w-text_w)/2:y=" + (hybrid ? "560" : contextual ? "700" : "50") + ":enable='" + intervals + "'[out]";
            List<String> command = List.of(properties.getProviders().getPostProduction().getFfmpegPath(),
                    "-hide_banner", "-loglevel", "error", "-y", "-threads", "1", "-i", source.toString(),
                    "-threads", "1", "-i", image.toString(), "-filter_complex_threads", "1", "-filter_complex", filter,
                    "-map", "[out]", "-map", "0:a?", "-c:v", "libx264", "-threads", "2", "-preset", "ultrafast",
                    "-crf", "20", "-pix_fmt", "yuv420p", "-c:a", "copy", "-t", number(outputDuration),
                    "-movflags", "+faststart", output.toString());
            long started = System.nanoTime();
            log.info("Compondo captura estática do PDE; jobId={} pixels={}x{} duration={} mode=PREPARED_FRAME_LOOP_V1",
                    jobId, proof.width(), proof.height(), proof.duration());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(processLog.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw invalid("A composição da prova excedeu o tempo limite.");
            }
            if (process.exitValue() != 0) throw invalid("A composição da prova falhou: " + Files.readString(processLog));
            Map<String, Object> audit = new java.util.LinkedHashMap<>(Map.of(
                    "contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1", "status", "APPLIED",
                    "sha256", proof.sha256(), "source", proof.contentPath(),
                    "crop", List.of(proof.x(), proof.y(), proof.width(), proof.height()),
                    "intervals", intervals, "syntheticScenario", true,
                    "commercialEvidenceClaimed", false,
                    "compositionMode", "PREPARED_FRAME_LOOP_V1"));
            if (!label.isEmpty()) audit.put("label", label);
            if (timing != null) {
                audit.put("timingSource", "NARRATION_CUE_V1");
                audit.put("reviewMoments", List.of((timing.start() + timing.end()) / 2));
                audit.put("captionCueIndex", timing.cueIndex());
                audit.put("layout", hybrid ? "DELIVERY_WITH_REAL_ACTION_V1" : contextual ? "MOTION_WITH_PRODUCT_PROOF_V1" : "FULL_PRODUCT_PROOF_V1");
            }
            log.info("Captura do PDE composta; jobId={} elapsedMs={} sha256={} mode=PREPARED_FRAME_LOOP_V1",
                    jobId, Duration.ofNanos(System.nanoTime() - started).toMillis(), proof.sha256());
            return new ProductUgcReferenceOverlay.OverlayResult(output, audit);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            delete(output, jobId);
            log.error("Falha ao compor prova do PDE; jobId={}", jobId, ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("PDE_PRODUCT_PROOF_INVALID", "Não foi possível compor a prova privada do PDE.", ex);
        } finally {
            delete(labelFile, jobId);
            delete(image, jobId);
            delete(processLog, jobId);
        }
    }

    /** Limita a identificação a duas linhas fora da captura, sem interpretar comandos de texto. */
    private String wrapLabel(String label) {
        if (label.length() <= 24) return label;
        int split = label.lastIndexOf(' ', 24);
        if (split < label.length() - 24) split = 24;
        return label.substring(0, split).stripTrailing() + "\n" + label.substring(split).stripLeading();
    }

    /** Reconhece a sequência explícita sem alterar o comportamento do contrato legado. */
    private boolean hasSequence(JsonNode metadata) {
        return !metadata.at("/post_production/proofShots").isMissingNode();
    }

    /** Exige trechos ordenados, limitados e com enquadramento válido antes de consumir voz ou baixar capturas. */
    private JsonNode sequence(JsonNode metadata) {
        JsonNode shots = metadata.at("/post_production/proofShots");
        if (!shots.isArray() || shots.isEmpty() || shots.size() > 5
                || !metadata.at("/technicalQualityGate/captionMustMatchNarration").asBoolean())
            throw invalid("Sequência da demonstração inválida.");
        boolean containsOriginal = false;
        int previous = -1;
        for (JsonNode shot : shots) {
            containsOriginal |= !shot.path("sha256").asText().isBlank()
                    && shot.path("sha256").asText().equals(metadata.at("/post_production/product_proof/sha256").asText());
            int cue = shot.path("captionCueIndex").asInt(-1);
            double seconds = shot.path("minimumSeconds").asDouble(Double.NaN);
            if (cue <= previous || !Double.isFinite(seconds) || seconds < 2 || seconds > 12)
                throw invalid("Trechos da demonstração fora de ordem ou sem duração válida.");
            String layout = shot.path("layout").asText("");
            if (!layout.isEmpty() && !java.util.Set.of("FULL_PRODUCT_PROOF_V1", "MOTION_WITH_PRODUCT_PROOF_V1").contains(layout))
                throw invalid("Enquadramento da demonstração inválido.");
            previous = cue;
        }
        if (!containsOriginal) throw invalid("A sequência precisa preservar a prova homologada original.");
        return shots;
    }

    /** Isola o contrato de um trecho mantendo a identidade do projeto e o plano narrativo. */
    private JsonNode withShot(JsonNode metadata, JsonNode shot) {
        var copy = (com.fasterxml.jackson.databind.node.ObjectNode) metadata.deepCopy();
        var post = (com.fasterxml.jackson.databind.node.ObjectNode) copy.path("post_production");
        post.remove("proofShots");
        var proof = (com.fasterxml.jackson.databind.node.ObjectNode) shot.deepCopy();
        proof.put("sequenceShot", true);
        post.set("product_proof", proof);
        return copy;
    }

    /** Monta capturas reais no relógio da fala e audita cada hash, enquadramento e intervalo. */
    private ProductUgcReferenceOverlay.OverlayResult applySequence(Path source, JsonNode metadata, Long jobId,
            List<PdeProofCaptionTiming.Window> captions) {
        var shots = sequence(metadata);
        double previousEnd = -1;
        for (var shot : shots) {
            var window = PdeProofCaptionTiming.resolve(withShot(metadata, shot), captions);
            if (window == null || window.start() < previousEnd - .01)
                throw invalid("As capturas se sobrepõem; distribua a fala e o tempo de leitura antes de finalizar.");
            previousEnd = window.end();
        }
        Path current = source;
        var audits = new ArrayList<Map<String, Object>>();
        var moments = new ArrayList<Double>();
        try {
            for (var shot : shots) {
                var output = apply(current, withShot(metadata, shot), jobId, captions);
                if (!current.equals(source)) delete(current, jobId);
                current = output.videoFile();
                audits.add(output.audit());
                var window = PdeProofCaptionTiming.resolve(withShot(metadata, shot), captions);
                moments.add((window.start() + window.end()) / 2);
            }
            var original = metadata.at("/post_production/product_proof");
            var audit = new java.util.LinkedHashMap<String, Object>();
            audit.put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1");
            audit.put("status", "APPLIED");
            audit.put("sha256", original.path("sha256").asText());
            audit.put("source", original.path("contentPath").asText());
            audit.put("syntheticScenario", true);
            audit.put("commercialEvidenceClaimed", false);
            audit.put("compositionMode", "NARRATED_PROOF_SEQUENCE_V1");
            audit.put("shots", audits);
            audit.put("reviewMoments", moments);
            return new ProductUgcReferenceOverlay.OverlayResult(current, audit);
        } catch (RuntimeException ex) {
            if (!current.equals(source)) delete(current, jobId);
            log.error("Falha na sequência da demonstração; jobId={}", jobId, ex);
            throw ex;
        }
    }

    /** Baixa somente a rota de vídeo esperada e recusa captura ou contexto divergentes. */
    private Proof load(JsonNode metadata, Long jobId) {
        JsonNode proof = metadata.at("/post_production/product_proof");
        long projectId = metadata.path("videoProjectId").asLong();
        String path = "/api/sales-videos/projects/" + projectId + "/product-proof";
        if (proof.path("sequenceShot").asBoolean(false)) {
            if (proof.path("taskId").asLong() <= 0 || proof.path("evidenceId").asLong() <= 0)
                throw invalid("Identidade da captura da sequência ausente.");
            path += "/" + proof.path("taskId").asLong() + "/" + proof.path("evidenceId").asLong();
        }
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
            // Uma página inteira reduzida não é prova legível no celular.
            if (height > width * 2L) {
                throw invalid("A captura longa exige um recorte legível da prova; não reduza a página inteira.");
            }
            double elapsed = 0;
            List<String> intervals = new ArrayList<>();
            for (JsonNode cut : metadata.path("cut_plan")) {
                double seconds = cut.path("duration_seconds").asDouble();
                if (!Double.isFinite(seconds) || seconds <= 0) throw invalid("Duração de corte inválida.");
                if ("PROVA".equals(cut.path("role").asText())) {
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
