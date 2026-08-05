package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Monta múltiplos clipes prontos em um único vídeo vertical para teste comercial. */
@Component
@ConditionalOnProperty(prefix = "video.providers.post-production", name = "enabled", havingValue = "true")
public class VideoMontageProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 150 * 1024 * 1024;
    private static final double MAX_MONTAGE_DURATION_SECONDS = 600.0;
    private static final String PROVIDER_NAME = "MUSA_VIDEO_MONTAGE";
    private static final double TRANSITION_SECONDS = 0.18;
    private static final Logger log = LoggerFactory.getLogger(VideoMontageProvider.class);

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient downloadWebClient;

    /** Inicializa o provider de montagem com configuração e cliente de download. */
    public VideoMontageProvider(VideoManagementProperties properties,
                                ObjectMapper objectMapper,
                                WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.downloadWebClient = webClientBuilder.clone()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_VIDEO_DOWNLOAD_BYTES))
                        .build())
                .build();
    }

    /** Verifica se o job representa montagem local de múltiplos vídeos. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.POST_PRODUCTION) {
            return false;
        }
        return PROVIDER_NAME.equals(normalize(job.providerName()));
    }

    /** Baixa os clipes, normaliza cada fonte e concatena tudo em um MP4 único. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        JsonNode metadata = readMetadata(job);
        List<SourceVideo> sources = readSources(metadata);
        double targetShotSeconds = metadata.path("targetShotSeconds").asDouble(0);
        List<Path> temporaryFiles = new ArrayList<>();
        try {
            progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING,
                    "Baixando clipes para montagem");
            List<Path> normalizedClips = new ArrayList<>();
            for (int index = 0; index < sources.size(); index++) {
                SourceVideo sourceVideo = sources.get(index);
                Path source = downloadSourceVideo(job, sourceVideo.url(), index + 1);
                Path normalized = Files.createTempFile("sales-video-" + job.id() + "-clip-" + (index + 1), ".mp4");
                temporaryFiles.add(source);
                temporaryFiles.add(normalized);
                int progress = 20 + Math.min(40, (index + 1) * 40 / sources.size());
                progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                        "Normalizando clipe " + (index + 1) + " de " + sources.size());
                normalizeClip(source, normalized, targetShotSeconds);
                normalizedClips.add(normalized);
            }
            Path output = Files.createTempFile("sales-video-" + job.id() + "-montage", ".mp4");
            temporaryFiles.add(output);
            progressCallback.onProgress(75, SalesVideoStatus.VIDEO_PROCESSING,
                    "Aplicando cortes e pontes visuais entre os planos aprovados");
            List<Double> clipDurations = normalizedClips.stream().map(this::probeDurationSeconds).toList();
            composeCinematicSequence(normalizedClips, clipDurations, output);
            double durationSeconds = probeDurationSeconds(output);
            validateMontageQuality(sources.size(), durationSeconds);
            ProviderFile video = new ProviderFile(
                    "sales-video-" + job.id() + "-musa-montage.mp4",
                    VIDEO_MP4,
                    AssetType.VIDEO,
                    ProviderAssetRole.VIDEO,
                    Files.readAllBytes(output));
            progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING,
                    "Montagem finalizada para revisão");
            return new ProviderArtifacts("montage-" + job.id(), video, null, null,
                    resultMetadata(job, sources, durationSeconds));
        } catch (IOException ex) {
            log.error("Falha de arquivo na montagem de vídeo do job {}", job.id(), ex);
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED", "Falha de arquivo na montagem de vídeo", ex);
        } finally {
            temporaryFiles.forEach(this::deleteIfExists);
        }
    }

    /** Baixa um clipe fonte preservando URLs absolutas e relativas ao backend. */
    private Path downloadSourceVideo(SalesVideoJob job, String sourceVideoUrl, int index) throws IOException {
        URI sourceUri = resolveSourceUri(sourceVideoUrl);
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(sourceUri)
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                    "Download vazio do clipe fonte " + index);
        }
        Path source = Files.createTempFile("sales-video-" + job.id() + "-source-" + index, ".mp4");
        Files.write(source, content);
        return source;
    }

    /** Normaliza e corta o clipe no ritmo alvo antes da composição cinematográfica. */
    private void normalizeClip(Path source, Path output, double targetShotSeconds) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        List<String> command = new ArrayList<>(List.of(
                config.getFfmpegPath(), "-y", "-i", source.toAbsolutePath().toString()));
        if (targetShotSeconds > 0) {
            command.addAll(List.of("-t", String.format(Locale.ROOT, "%.3f", targetShotSeconds)));
        }
        command.addAll(List.of(
                "-vf", "scale=720:1280:force_original_aspect_ratio=increase,crop=720:1280,fps=30,setsar=1",
                "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "30",
                output.toAbsolutePath().toString()));
        runProcess(command,
                "ffmpeg falhou ao normalizar clipe para montagem");
    }

    /** Compõe os clipes com microtransições para suavizar cortes sem esconder mudanças de plano. */
    private void composeCinematicSequence(List<Path> clips, List<Double> durations, Path output) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        List<String> command = new ArrayList<>();
        command.add(config.getFfmpegPath());
        command.add("-y");
        clips.forEach(clip -> {
            command.add("-i");
            command.add(clip.toAbsolutePath().toString());
        });
        StringBuilder filter = new StringBuilder();
        double accumulatedDuration = durations.getFirst();
        for (int index = 1; index < clips.size(); index++) {
            String previous = index == 1 ? "[0:v]" : "[v" + (index - 1) + "]";
            double offset = Math.max(0.01, accumulatedDuration - TRANSITION_SECONDS);
            filter.append(previous).append("[").append(index).append(":v]")
                    .append("xfade=transition=fade:duration=")
                    .append(TRANSITION_SECONDS)
                    .append(":offset=").append(String.format(Locale.ROOT, "%.3f", offset))
                    .append("[v").append(index).append("];");
            accumulatedDuration += durations.get(index) - TRANSITION_SECONDS;
        }
        filter.setLength(filter.length() - 1);
        command.addAll(List.of(
                "-filter_complex", filter.toString(),
                "-map", "[v" + (clips.size() - 1) + "]",
                "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "30",
                "-movflags", "+faststart", output.toAbsolutePath().toString()));
        runProcess(command, "ffmpeg falhou ao compor a montagem cinematográfica");
    }

    /** Mede a duração real do MP4 final usando ffprobe antes de liberar o job. */
    private double probeDurationSeconds(Path output) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        String result = runProcessOutput(List.of(
                config.getFfprobePath(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                output.toAbsolutePath().toString()),
                "ffprobe falhou ao auditar duração da montagem");
        try {
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException ex) {
            log.error("ffprobe retornou duração inválida para montagem: {}", result, ex);
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                    "ffprobe retornou duração inválida para montagem", ex);
        }
    }

    /** Bloqueia montagens sem ritmo mínimo ou fora do limite operacional. */
    private void validateMontageQuality(int sceneCount, double durationSeconds) {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                    "Duração final da montagem não pôde ser auditada");
        }
        if (durationSeconds > MAX_MONTAGE_DURATION_SECONDS) {
            throw new VideoProviderException("VIDEO_MONTAGE_DURATION_EXCEEDED",
                    "Montagem excede o limite atual de 10 minutos: %.2fs".formatted(durationSeconds));
        }
        double averageShotSeconds = durationSeconds / sceneCount;
        if (sceneCount >= 6 && averageShotSeconds > 4.0) {
            throw new VideoProviderException("VIDEO_MONTAGE_RHYTHM_REJECTED",
                    "Ritmo reprovado: média de %.2fs por plano; máximo premium de 4s"
                            .formatted(averageShotSeconds));
        }
    }

    /** Executa um processo externo e converte falhas em erro operacional claro. */
    private void runProcess(List<String> command, String failureMessage) {
        runProcessOutput(command, failureMessage);
    }

    /** Executa um processo externo e retorna a saída de diagnóstico. */
    private String runProcessOutput(List<String> command, String failureMessage) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                        failureMessage + "; exitCode=" + exitCode + "; output=" + output);
            }
            return output;
        } catch (IOException ex) {
            log.error("Falha ao executar processo externo da montagem: {}", failureMessage, ex);
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED", failureMessage, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Processo externo da montagem interrompido: {}", failureMessage, ex);
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED", failureMessage, ex);
        }
    }

    /** Lê os metadados do job como JSON. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (!StringUtils.hasText(job.metadataJson())) {
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED", "Metadata de montagem ausente");
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            log.error("Metadata de montagem inválida no job {}", job.id(), ex);
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED", "Metadata de montagem inválida", ex);
        }
    }

    /** Extrai os clipes fonte do metadata persistido pelo backend. */
    private List<SourceVideo> readSources(JsonNode metadata) {
        JsonNode sources = metadata.path("sourceVideos");
        if (!sources.isArray() || sources.size() < 2) {
            throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                    "Montagem exige pelo menos dois clipes fonte");
        }
        List<SourceVideo> result = new ArrayList<>();
        for (JsonNode source : sources) {
            long sourceJobId = source.path("sourceJobId").asLong();
            String sourceVideoUrl = source.path("sourceVideoUrl").asText(null);
            if (!StringUtils.hasText(sourceVideoUrl)) {
                throw new VideoProviderException("VIDEO_MONTAGE_FAILED",
                        "Clipe fonte sem URL de vídeo");
            }
            result.add(new SourceVideo(sourceJobId, sourceVideoUrl.trim()));
        }
        return result;
    }

    /** Resolve uma URL fonte absoluta ou relativa ao backend. */
    private URI resolveSourceUri(String sourceVideoUrl) {
        URI uri = URI.create(sourceVideoUrl);
        if (uri.isAbsolute()) {
            return uri;
        }
        return properties.getBackendBaseUrl().resolve(sourceVideoUrl);
    }

    /** Consolida metadados de saída da montagem. */
    private Map<String, Object> resultMetadata(
            SalesVideoJob job, List<SourceVideo> sources, double durationSeconds) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", PROVIDER_NAME);
        metadata.put("provider_job_id", "montage-" + job.id());
        metadata.put("source_job_ids", sources.stream().map(SourceVideo::jobId).toList());
        metadata.put("source_count", sources.size());
        metadata.put("resolution", "720x1280");
        metadata.put("duration_seconds", Math.round(durationSeconds));
        metadata.put("average_shot_seconds", Math.round(durationSeconds * 100.0 / sources.size()) / 100.0);
        metadata.put("transition_style", "MOTION_MATCH_CROSSFADE");
        metadata.put("transition_seconds", TRANSITION_SECONDS);
        metadata.put("quality_gate", Map.of(
                "rhythm", sources.size() < 6 || durationSeconds / sources.size() <= 4.0 ? "APPROVED" : "REJECTED",
                "scene_count", sources.size(),
                "mobile_resolution", "APPROVED"));
        metadata.put("max_duration_seconds", (int) MAX_MONTAGE_DURATION_SECONDS);
        metadata.put("audio", Map.of("preserved", false, "reason", "montagem preparada para voz off final"));
        metadata.put("finished_at", Instant.now().toString());
        return metadata;
    }

    /** Normaliza nome de provider para comparação estável. */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Remove arquivo temporário se ele tiver sido criado. */
    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            log.debug("Arquivo temporário de montagem não pôde ser removido: {}", path, ignored);
            // Arquivo temporário residual não deve mascarar o resultado do job.
        }
    }

    /** Representa um clipe fonte selecionado para montagem. */
    private record SourceVideo(long jobId, String url) {
    }
}
