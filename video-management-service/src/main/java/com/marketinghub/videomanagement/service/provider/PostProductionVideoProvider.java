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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Finaliza vídeos brutos com voz off, legenda queimada e trilha leve para venda. */
@Component
@ConditionalOnProperty(prefix = "video.providers.post-production", name = "enabled", havingValue = "true")
public class PostProductionVideoProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final MediaType TEXT_VTT = MediaType.valueOf("text/vtt");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 150 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient downloadWebClient;

    /** Inicializa o provider de pós-produção com configuração e cliente de download. */
    public PostProductionVideoProvider(VideoManagementProperties properties,
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

    /** Verifica se o job é uma etapa local de pós-produção. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.POST_PRODUCTION) {
            return false;
        }
        String providerName = normalize(job.providerName());
        return properties.getProviders().getPostProduction().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Baixa o vídeo fonte, adiciona voz, legenda e trilha, e devolve o MP4 final. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        JsonNode metadata = readMetadata(job);
        String sourceVideoUrl = requiredText(metadata, "sourceVideoUrl");
        String captionText = firstText(metadata, "captionText", "voiceOverScript");
        String voiceOverScript = firstText(metadata, "voiceOverScript", "captionText");
        Path source = null;
        Path voice = null;
        Path caption = null;
        Path output = null;
        try {
            progressCallback.onProgress(15, SalesVideoStatus.VIDEO_PROCESSING, "Baixando vídeo bruto para pós-produção");
            source = downloadSourceVideo(job, sourceVideoUrl);
            voice = Files.createTempFile("sales-video-" + job.id() + "-voiceover", ".wav");
            caption = Files.createTempFile("sales-video-" + job.id() + "-caption", ".txt");
            output = Files.createTempFile("sales-video-" + job.id() + "-final", ".mp4");
            Files.writeString(caption, wrapCaption(captionText), StandardCharsets.UTF_8);
            progressCallback.onProgress(35, SalesVideoStatus.VIDEO_PROCESSING, "Gerando voz off em português");
            runEspeak(voiceOverScript, voice);
            progressCallback.onProgress(65, SalesVideoStatus.VIDEO_PROCESSING, "Aplicando legenda e trilha leve");
            runFfmpeg(source, voice, caption, output);
            Map<String, Object> audioReview = reviewAudio(output, voiceOverScript);
            ProviderFile video = new ProviderFile(
                    "sales-video-" + job.id() + "-musa-final.mp4",
                    VIDEO_MP4,
                    AssetType.VIDEO,
                    ProviderAssetRole.VIDEO,
                    Files.readAllBytes(output));
            ProviderFile captions = new ProviderFile(
                    "sales-video-" + job.id() + "-musa-final.vtt",
                    TEXT_VTT,
                    AssetType.CAPTION,
                    ProviderAssetRole.CAPTION,
                    buildVtt(captionText));
            Map<String, Object> resultMetadata = resultMetadata(job, metadata, captionText, voiceOverScript, audioReview);
            progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Vídeo finalizado para venda");
            return new ProviderArtifacts("post-production-" + job.id(), video, null, captions, resultMetadata);
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Falha de arquivo na pós-produção", ex);
        } finally {
            deleteIfExists(source);
            deleteIfExists(voice);
            deleteIfExists(caption);
            deleteIfExists(output);
        }
    }

    /** Baixa o MP4 fonte preservando URLs absolutas e relativas ao backend. */
    private Path downloadSourceVideo(SalesVideoJob job, String sourceVideoUrl) throws IOException {
        URI sourceUri = resolveSourceUri(sourceVideoUrl);
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(sourceUri)
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Download vazio do vídeo fonte");
        }
        Path source = Files.createTempFile("sales-video-" + job.id() + "-source", ".mp4");
        Files.write(source, content);
        return source;
    }

    /** Gera o áudio de voz off usando binário versionado na imagem do worker. */
    private void runEspeak(String voiceOverScript, Path voiceFile) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        runProcess(List.of(
                config.getEspeakPath(),
                "-v", config.getEspeakVoice(),
                "-s", "145",
                "-w", voiceFile.toAbsolutePath().toString(),
                voiceOverScript),
                "espeak-ng falhou ao gerar voz off");
    }

    /** Compoe o MP4 final com legenda queimada, voz e trilha discreta. */
    private void runFfmpeg(Path source, Path voice, Path caption, Path output) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        String drawText = "drawtext=fontfile='%s':textfile='%s':fontcolor=white:fontsize=42:"
                + "line_spacing=10:borderw=4:bordercolor=black@0.75:box=1:boxcolor=black@0.35:"
                + "boxborderw=18:x=(w-text_w)/2:y=h-(text_h+140)";
        String videoFilter = drawText.formatted(
                escapeFilterPath(config.getFontFile()),
                escapeFilterPath(caption.toAbsolutePath().toString()));
        String filter = "[2:a]volume=0.035,apad[music];[1:a]volume=1.0[voice];"
                + "[voice][music]amix=inputs=2:duration=first:dropout_transition=0[aout];"
                + "[0:v]" + videoFilter + "[vout]";
        runProcess(List.of(
                config.getFfmpegPath(),
                "-y",
                "-i", source.toAbsolutePath().toString(),
                "-i", voice.toAbsolutePath().toString(),
                "-f", "lavfi",
                "-i", "sine=frequency=392:sample_rate=44100",
                "-filter_complex", filter,
                "-map", "[vout]",
                "-map", "[aout]",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-shortest",
                output.toAbsolutePath().toString()),
                "ffmpeg falhou ao finalizar vídeo para venda");
    }

    /** Executa um processo externo e converte falhas em erro operacional claro. */
    private String runProcess(List<String> command, String failureMessage) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED",
                        failureMessage + "; exitCode=" + exitCode + "; output=" + output);
            }
            return output;
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", failureMessage, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", failureMessage, ex);
        }
    }

    /** Lê os metadados do job como JSON. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (!StringUtils.hasText(job.metadataJson())) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Metadata de pós-produção ausente");
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Metadata de pós-produção inválida", ex);
        }
    }

    /** Resolve uma URL fonte absoluta ou relativa ao backend. */
    private URI resolveSourceUri(String sourceVideoUrl) {
        URI uri = URI.create(sourceVideoUrl);
        if (uri.isAbsolute()) {
            return uri;
        }
        return properties.getBackendBaseUrl().resolve(sourceVideoUrl);
    }

    /** Busca um texto obrigatório nos metadados. */
    private String requiredText(JsonNode metadata, String field) {
        String value = metadata.path(field).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Campo obrigatório ausente: " + field);
        }
        return value.trim();
    }

    /** Retorna o primeiro texto preenchido entre dois campos de metadados. */
    private String firstText(JsonNode metadata, String preferredField, String fallbackField) {
        String preferred = metadata.path(preferredField).asText(null);
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        return requiredText(metadata, fallbackField);
    }

    /** Quebra a legenda em linhas curtas para leitura em vídeo vertical. */
    private String wrapCaption(String text) {
        String[] words = text.replaceAll("\\s+", " ").trim().split(" ");
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 26) {
                result.append(line).append('\n');
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            result.append(line);
        }
        return result.toString();
    }

    /** Gera legenda VTT simples para auditoria e players que suportarem caption externa. */
    private byte[] buildVtt(String captionText) {
        String vtt = "WEBVTT\n\n00:00:00.000 --> 00:00:30.000\n" + wrapCaption(captionText) + "\n";
        return vtt.getBytes(StandardCharsets.UTF_8);
    }

    /** Revisa o áudio final com métricas objetivas e decisão comercial para mobile. */
    private Map<String, Object> reviewAudio(Path output, String voiceOverScript) {
        try {
            VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
            String outputText = runProcess(List.of(
                    config.getFfmpegPath(),
                    "-hide_banner",
                    "-nostats",
                    "-i", output.toAbsolutePath().toString(),
                    "-filter_complex", "ebur128=peak=true",
                    "-f", "null",
                    "-"),
                    "ffmpeg falhou ao revisar áudio final");
            return buildAudioReview(outputText, voiceOverScript);
        } catch (RuntimeException ex) {
            Map<String, Object> review = new LinkedHashMap<>();
            review.put("status", "REVIEW_UNAVAILABLE");
            review.put("label", "Revisão de áudio indisponível");
            review.put("recommendation",
                    "Ouvir manualmente antes de campanha; a análise automática não conseguiu ler o áudio final.");
            review.put("error", ex.getMessage());
            return review;
        }
    }

    /** Monta o parecer de áudio a partir da saída do filtro ebur128 do ffmpeg. */
    private Map<String, Object> buildAudioReview(String ffmpegOutput, String voiceOverScript) {
        Double integratedLufs = parseLastMetric(ffmpegOutput, "I:\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+LUFS");
        Double truePeakDbfs = parseLastMetric(ffmpegOutput, "Peak:\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+dBFS");
        boolean syntheticVoice = isSyntheticLocalVoice();
        String status = resolveAudioReviewStatus(integratedLufs, truePeakDbfs, syntheticVoice);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("integrated_lufs", integratedLufs);
        metrics.put("true_peak_dbfs", truePeakDbfs);
        metrics.put("target_lufs_min", -18);
        metrics.put("target_lufs_max", -16);
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("status", status);
        review.put("label", audioReviewLabel(status));
        review.put("provider", "ESPEAK_NG");
        review.put("voice_quality", syntheticVoice ? "synthetic_local" : "unknown");
        review.put("metrics", metrics);
        review.put("script_character_count", voiceOverScript == null ? 0 : voiceOverScript.length());
        review.put("issues", audioReviewIssues(integratedLufs, truePeakDbfs, syntheticVoice));
        review.put("recommendation", audioReviewRecommendation(status, syntheticVoice));
        return review;
    }

    /** Decide o status comercial do áudio para anúncio mobile. */
    private String resolveAudioReviewStatus(Double integratedLufs, Double truePeakDbfs, boolean syntheticVoice) {
        if (syntheticVoice) {
            return "BLOCKED_FOR_CAMPAIGN";
        }
        if (integratedLufs == null || truePeakDbfs == null) {
            return "NEEDS_HUMAN_REVIEW";
        }
        if (integratedLufs < -20 || integratedLufs > -12 || truePeakDbfs > -1) {
            return "NEEDS_ADJUSTMENT";
        }
        return "APPROVED_FOR_TEST";
    }

    /** Nomeia o status para leitura direta na tela. */
    private String audioReviewLabel(String status) {
        return switch (status) {
            case "BLOCKED_FOR_CAMPAIGN" -> "Bloqueado: voz robótica";
            case "NEEDS_ADJUSTMENT" -> "Ajustar volume";
            case "APPROVED_FOR_TEST" -> "Apto para teste";
            case "REVIEW_UNAVAILABLE" -> "Revisão indisponível";
            default -> "Revisão humana necessária";
        };
    }

    /** Lista os problemas encontrados na avaliação automática de áudio. */
    private List<String> audioReviewIssues(Double integratedLufs, Double truePeakDbfs, boolean syntheticVoice) {
        java.util.ArrayList<String> issues = new java.util.ArrayList<>();
        if (syntheticVoice) {
            issues.add("Voz gerada por síntese local simples; tende a soar robótica em anúncio mobile.");
        }
        if (integratedLufs != null && integratedLufs < -20) {
            issues.add("Volume médio abaixo do recomendado para feed e reels.");
        }
        if (integratedLufs != null && integratedLufs > -12) {
            issues.add("Volume médio alto demais; pode soar agressivo ou distorcido.");
        }
        if (truePeakDbfs != null && truePeakDbfs > -1) {
            issues.add("Pico muito próximo de clipping; precisa normalização com margem de segurança.");
        }
        if (issues.isEmpty()) {
            issues.add("Métricas técnicas dentro de faixa aceitável; ainda exige escuta humana do tom da voz.");
        }
        return issues;
    }

    /** Define a recomendação comercial do áudio final. */
    private String audioReviewRecommendation(String status, boolean syntheticVoice) {
        if (syntheticVoice) {
            return "Não usar como versão final de campanha. Trocar para provedor de voz natural e manter este render apenas como protótipo.";
        }
        if ("NEEDS_ADJUSTMENT".equals(status)) {
            return "Normalizar o áudio antes de publicar, mirando -16 a -18 LUFS e pico seguro próximo de -1 dBFS.";
        }
        if ("APPROVED_FOR_TEST".equals(status)) {
            return "Pode entrar em teste controlado, com revisão humana final de emoção, ritmo e CTA.";
        }
        return "Ouvir manualmente e, se a narração parecer artificial, refazer a voz antes do experimento.";
    }

    /** Identifica quando a voz foi criada pelo sintetizador local do worker. */
    private boolean isSyntheticLocalVoice() {
        String path = properties.getProviders().getPostProduction().getEspeakPath();
        return path != null && path.toLowerCase(Locale.ROOT).contains("espeak");
    }

    /** Extrai a última ocorrência numérica de uma métrica no log do ffmpeg. */
    private Double parseLastMetric(String output, String regex) {
        if (output == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(output);
        Double value = null;
        while (matcher.find()) {
            value = Double.valueOf(matcher.group(1));
        }
        return value;
    }

    /** Consolida metadados de saída da pós-produção. */
    private Map<String, Object> resultMetadata(SalesVideoJob job,
                                               JsonNode sourceMetadata,
                                               String captionText,
                                               String voiceOverScript,
                                               Map<String, Object> audioReview) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "MUSA_POST_PRODUCTION");
        metadata.put("provider_job_id", "post-production-" + job.id());
        metadata.put("duration_seconds", 30);
        metadata.put("audio", Map.of(
                "voice_over", true,
                "language", "pt-BR",
                "music", "synthetic_light_bed",
                "review", audioReview));
        metadata.put("captions", Map.of("burned_in", true, "vtt_asset", true, "text", captionText));
        metadata.put("voice_over_script", voiceOverScript);
        metadata.put("source_experiment_video_asset_id", sourceMetadata.path("experimentVideoAssetId").asLong());
        metadata.put("finished_at", Instant.now().toString());
        return metadata;
    }

    /** Normaliza nome de provider para comparação estável. */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Escapa caminhos usados dentro de filtros ffmpeg. */
    private String escapeFilterPath(String value) {
        return value.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'");
    }

    /** Remove arquivo temporário se ele tiver sido criado. */
    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Arquivo temporário residual não deve mascarar o resultado do job.
        }
    }
}
