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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: medir estabilidade dentro de tomadas ou planos antes do upload comercial. */
@Component
public class ApolloTechnicalVideoQualityGate {
    private static final Logger log = LoggerFactory.getLogger(ApolloTechnicalVideoQualityGate.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 120;
    private static final double ROTATION_PIXEL_SCALE = 720.0;
    private static final Pattern SCENE_CUT_FRAME = Pattern.compile("^frame:(\\d+)\\s+.*$");
    private final ObjectMapper objectMapper;
    private final VideoManagementProperties properties;

    /** Configura o parser de contrato e o ffmpeg versionado no executor de vídeo. */
    public ApolloTechnicalVideoQualityGate(
            ObjectMapper objectMapper, VideoManagementProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Executa o gate quando o backend exige estabilidade em tomada única ou planos editoriais. */
    public ProviderArtifacts validate(SalesVideoJob job, ProviderArtifacts artifacts) {
        JsonNode gate = technicalGate(job);
        boolean continuousTakeRequired = gate.path("continuousTakeRequired").asBoolean(false);
        boolean intentionalSceneCutsAllowed =
                gate.path("intentionalSceneCutsAllowed").asBoolean(false);
        if (!continuousTakeRequired && !intentionalSceneCutsAllowed) return artifacts;
        if (artifacts.videoFile() == null || artifacts.videoFile().content().length == 0) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Apolo não recebeu vídeo para medir estabilidade.");
        }
        double maximumMean = gate.path("maximumMeanMotionDelta").asDouble(1.25);
        double maximumPeak = gate.path("maximumPeakMotionDelta").asDouble(12.0);
        int maximumSceneCuts = gate.path("maximumSceneCuts").asInt(0);
        StabilityMetrics metrics = measure(job, artifacts.videoFile().content());
        if (continuousTakeRequired && metrics.detectedSceneCuts() > 0) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_CONTINUITY_REJECTED",
                    "Apolo reprovou continuidade: a tomada única contém %d corte(s) de cena."
                            .formatted(metrics.detectedSceneCuts()));
        }
        if (intentionalSceneCutsAllowed && metrics.detectedSceneCuts() > maximumSceneCuts) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_SCENE_CUTS_REJECTED",
                    "Apolo reprovou montagem: foram detectados %d cortes (máximo %d)."
                            .formatted(metrics.detectedSceneCuts(), maximumSceneCuts));
        }
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
        LinkedHashMap<String, Object> technicalQuality = new LinkedHashMap<>();
        technicalQuality.put("stability_status", "APPROVED");
        technicalQuality.put("continuous_take", metrics.detectedSceneCuts() == 0);
        technicalQuality.put("intentional_scene_cuts_allowed", intentionalSceneCutsAllowed);
        technicalQuality.put("detected_scene_cuts", metrics.detectedSceneCuts());
        technicalQuality.put("maximum_scene_cuts", maximumSceneCuts);
        technicalQuality.put("excluded_transition_deltas", metrics.excludedTransitionDeltas());
        technicalQuality.put("measured_frames", metrics.measuredFrames());
        technicalQuality.put("mean_motion_delta", metrics.meanMotionDelta());
        technicalQuality.put("peak_motion_delta", metrics.peakMotionDelta());
        technicalQuality.put("maximum_mean_motion_delta", maximumMean);
        technicalQuality.put("maximum_peak_motion_delta", maximumPeak);
        technicalQuality.put("method", "FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA");
        metadata.put("apollo_technical_quality", technicalQuality);
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
            Path sceneCuts = directory.resolve("scene-cuts.txt");
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
                            "scdet=threshold=10,metadata=print:key=lavfi.scd.time:file=" + sceneCuts,
                            "-an",
                            "-f",
                            "null",
                            "-"),
                    directory,
                    "detecção de cortes editoriais");
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
            List<Integer> sceneCutFrames = Files.isRegularFile(sceneCuts)
                    ? parseSceneCutFrames(Files.readAllLines(sceneCuts, StandardCharsets.UTF_8))
                    : List.of();
            return analyzeTransformLines(
                    Files.readAllLines(globalMotions, StandardCharsets.UTF_8), sceneCutFrames);
        } catch (IOException ex) {
            log.error("Falha ao medir estabilidade do vídeo; jobId={}", job.id(), ex);
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE", "Apolo não conseguiu medir a estabilidade.", ex);
        } finally {
            deleteTemporaryDirectory(directory, job.id());
        }
    }

    /** Converte transformações em média e pico, desconsiderando somente transições de cena. */
    StabilityMetrics analyzeTransformLines(List<String> lines) {
        return analyzeTransformLines(lines, List.of());
    }

    /** Mede tremor dentro dos planos e exclui o corte e o quadro imediato de recomposição. */
    StabilityMetrics analyzeTransformLines(List<String> lines, List<Integer> sceneCutFrames) {
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
        int excluded = 0;
        Set<Integer> excludedDeltas = new HashSet<>();
        sceneCutFrames.forEach(
                frame -> {
                    excludedDeltas.add(frame);
                    excludedDeltas.add(frame + 1);
                });
        for (int index = 1; index < motions.size(); index++) {
            Motion previous = motions.get(index - 1);
            Motion current = motions.get(index);
            double deltaX = current.x() - previous.x();
            double deltaY = current.y() - previous.y();
            double deltaRotation = (current.angle() - previous.angle()) * ROTATION_PIXEL_SCALE;
            double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaRotation * deltaRotation);
            if (excludedDeltas.contains(index)) {
                excluded++;
                continue;
            }
            sum += delta;
            peak = Math.max(peak, delta);
            deltas++;
        }
        if (deltas < 8) {
            throw new VideoProviderException(
                    "APOLLO_VIDEO_STABILITY_UNAVAILABLE",
                    "A amostra útil possui poucos quadros após separar as transições editoriais.");
        }
        return new StabilityMetrics(
                motions.size(), sum / deltas, peak, sceneCutFrames.size(), excluded);
    }

    /** Extrai os números de quadro produzidos pelo filtro scdet sem inferir pela duração. */
    List<Integer> parseSceneCutFrames(List<String> lines) {
        List<Integer> result = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = SCENE_CUT_FRAME.matcher(line == null ? "" : line.trim());
            if (matcher.matches()) result.add(Integer.parseInt(matcher.group(1)));
        }
        return result;
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
    record StabilityMetrics(
            int measuredFrames,
            double meanMotionDelta,
            double peakMotionDelta,
            int detectedSceneCuts,
            int excludedTransitionDeltas) {}
}
