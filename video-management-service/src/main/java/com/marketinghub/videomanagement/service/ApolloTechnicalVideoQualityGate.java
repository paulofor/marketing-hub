package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: reprovar tremor abrupto em tomadas contínuas antes do upload comercial. */
@Component
public class ApolloTechnicalVideoQualityGate {
    private static final Logger log = LoggerFactory.getLogger(ApolloTechnicalVideoQualityGate.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 120;
    private static final double ROTATION_PIXEL_SCALE = 720.0;
    private final ObjectMapper objectMapper;
    private final VideoManagementProperties properties;

    /** Configura o parser de contrato e o ffmpeg versionado no executor de vídeo. */
    public ApolloTechnicalVideoQualityGate(
            ObjectMapper objectMapper, VideoManagementProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Executa o gate apenas quando o backend exige tomada contínua estável. */
    public ProviderArtifacts validate(SalesVideoJob job, ProviderArtifacts artifacts) {
        JsonNode gate = technicalGate(job);
        if (!gate.path("continuousTakeRequired").asBoolean(false)) return artifacts;
        if (artifacts.videoFile() == null || artifacts.videoFile().content().length == 0) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Apolo não recebeu vídeo para medir estabilidade.");
        }
        double maximumMean = gate.path("maximumMeanMotionDelta").asDouble(1.25);
        double maximumPeak = gate.path("maximumPeakMotionDelta").asDouble(12.0);
        StabilityMetrics metrics = measure(job, artifacts.videoFile().content());
        boolean approved = metrics.meanMotionDelta() <= maximumMean
                && metrics.peakMotionDelta() <= maximumPeak;
        if (!approved) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_REJECTED",
                    "Apolo reprovou tremor: variação média %.3f (máximo %.3f) e pico %.3f (máximo %.3f)."
                            .formatted(
                                    metrics.meanMotionDelta(),
                                    maximumMean,
                                    metrics.peakMotionDelta(),
                                    maximumPeak));
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (artifacts.metadata() != null) metadata.putAll(artifacts.metadata());
        metadata.put(
                "apollo_technical_quality",
                Map.of(
                        "stability_status", "APPROVED",
                        "continuous_take", true,
                        "measured_frames", metrics.measuredFrames(),
                        "mean_motion_delta", metrics.meanMotionDelta(),
                        "peak_motion_delta", metrics.peakMotionDelta(),
                        "maximum_mean_motion_delta", maximumMean,
                        "maximum_peak_motion_delta", maximumPeak,
                        "method", "FFMPEG_VIDSTAB_GLOBAL_MOTION_DELTA"));
        return new ProviderArtifacts(
                artifacts.providerJobId(),
                artifacts.videoFile(),
                artifacts.posterFile(),
                artifacts.captionFile(),
                metadata,
                artifacts.auditFiles());
    }

    /** Mede variações bruscas entre movimentos globais consecutivos estimados pelo vid.stab. */
    private StabilityMetrics measure(SalesVideoJob job, byte[] video) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("apollo-stability-" + job.id() + "-");
            Path source = directory.resolve("source.mp4");
            Path transforms = directory.resolve("transforms.trf");
            Files.write(source, video);
            String ffmpeg = properties.getProviders().getPostProduction().getFfmpegPath();
            runProcess(
                    List.of(
                            ffmpeg,
                            "-hide_banner",
                            "-loglevel",
                            "error",
                            "-i",
                            source.toString(),
                            "-vf",
                            "vidstabdetect=result=" + transforms + ":shakiness=10:accuracy=15",
                            "-f",
                            "null",
                            "-"),
                    directory,
                    "detecção de movimento");
            runProcess(
                    List.of(
                            ffmpeg,
                            "-hide_banner",
                            "-loglevel",
                            "error",
                            "-i",
                            source.toString(),
                            "-vf",
                            "vidstabtransform=input=" + transforms + ":debug=1",
                            "-f",
                            "null",
                            "-"),
                    directory,
                    "consolidação de movimento");
            Path globalMotions = directory.resolve("global_motions.trf");
            if (!Files.isRegularFile(globalMotions)) {
                throw new VideoProviderException(
                        "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                        "ffmpeg não produziu as medições globais de estabilidade.");
            }
            return analyzeTransformLines(Files.readAllLines(globalMotions, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.error("Falha ao medir estabilidade do vídeo; jobId={}", job.id(), ex);
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Apolo não conseguiu medir a estabilidade.", ex);
        } finally {
            deleteTemporaryDirectory(directory, job.id());
        }
    }

    /** Converte as transformações globais em média e pico de mudança entre quadros. */
    StabilityMetrics analyzeTransformLines(List<String> lines) {
        List<Motion> motions = new ArrayList<>();
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.isEmpty() || normalized.startsWith("#")) continue;
            String[] fields = normalized.split("\\s+");
            if (fields.length < 4) continue;
            try {
                motions.add(
                        new Motion(
                                Double.parseDouble(fields[1]),
                                Double.parseDouble(fields[2]),
                                Double.parseDouble(fields[3])));
            } catch (NumberFormatException ex) {
                log.debug("Linha de movimento ignorada pelo gate de estabilidade; line={}", normalized, ex);
            }
        }
        if (motions.size() < 10) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                    "A amostra possui poucos quadros para medir tremor com segurança.");
        }
        double sum = 0;
        double peak = 0;
        int deltas = 0;
        for (int index = 1; index < motions.size(); index++) {
            Motion previous = motions.get(index - 1);
            Motion current = motions.get(index);
            double deltaX = current.x() - previous.x();
            double deltaY = current.y() - previous.y();
            double deltaRotation = (current.angle() - previous.angle()) * ROTATION_PIXEL_SCALE;
            double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaRotation * deltaRotation);
            sum += delta;
            peak = Math.max(peak, delta);
            deltas++;
        }
        return new StabilityMetrics(motions.size(), sum / deltas, peak);
    }

    /** Lê o contrato técnico do job e falha fechado quando o JSON obrigatório está corrompido. */
    private JsonNode technicalGate(SalesVideoJob job) {
        if (!StringUtils.hasText(job.metadataJson())) return objectMapper.missingNode();
        try {
            return objectMapper.readTree(job.metadataJson()).path("technicalQualityGate");
        } catch (JsonProcessingException ex) {
            log.error("Metadata inválido no gate de estabilidade; jobId={}", job.id(), ex);
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Contrato técnico de Apolo está inválido.", ex);
        }
    }

    /** Executa o ffmpeg com prazo finito e preserva sua saída quando houver falha. */
    private void runProcess(List<String> command, Path directory, String operation) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new VideoProviderException(
                        "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                        "Timeout na " + operation + " do gate de estabilidade.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new VideoProviderException(
                        "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                        "ffmpeg falhou na " + operation + ": " + limit(output));
            }
        } catch (IOException ex) {
            log.error("Falha ao iniciar ffmpeg no gate de estabilidade; operation={}", operation, ex);
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                    "Não foi possível iniciar o ffmpeg para " + operation + ".",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Gate de estabilidade interrompido; operation={}", operation, ex);
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Gate de estabilidade interrompido.", ex);
        }
    }

    /** Remove somente o diretório temporário criado pelo próprio gate. */
    private void deleteTemporaryDirectory(Path directory, Long jobId) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deleteTemporaryPath);
        } catch (IOException ex) {
            log.warn("Falha ao limpar temporários do gate de estabilidade; jobId={}", jobId, ex);
        }
    }

    /** Remove um arquivo temporário individual sem esconder o resultado do gate. */
    private void deleteTemporaryPath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Falha ao remover temporário do gate de estabilidade; path={}", path, ex);
        }
    }

    /** Limita a saída técnica incorporada à falha operacional. */
    private String limit(String value) {
        if (!StringUtils.hasText(value)) return "sem saída";
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
    }

    /** Representa deslocamento e rotação globais estimados em um quadro. */
    private record Motion(double x, double y, double angle) {}

    /** Resume a amostra usada pelo gate para persistência e testes. */
    record StabilityMetrics(int measuredFrames, double meanMotionDelta, double peakMotionDelta) {}
}
