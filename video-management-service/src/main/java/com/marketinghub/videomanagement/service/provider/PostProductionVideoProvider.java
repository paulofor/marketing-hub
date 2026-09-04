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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Finaliza vídeos brutos com legenda queimada e, quando houver roteiro, voz off e trilha leve. */
@Component
@ConditionalOnProperty(prefix = "video.providers.post-production", name = "enabled", havingValue = "true")
public class PostProductionVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(PostProductionVideoProvider.class);
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final MediaType TEXT_VTT = MediaType.valueOf("text/vtt");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 150 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient downloadWebClient;
    private final WebClient openAiWebClient;

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
        this.openAiWebClient = webClientBuilder.clone()
                .baseUrl(properties.getProviders().getPostProduction().getOpenAiBaseUrl().toString())
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

    /** Baixa o vídeo fonte, adiciona legenda e aplica voz/trilha quando houver roteiro. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        JsonNode metadata = readMetadata(job);
        String sourceVideoUrl = requiredText(metadata, "sourceVideoUrl");
        String captionText = requiredText(metadata, "captionText");
        String voiceOverScript = optionalText(metadata, "voiceOverScript");
        Map<String, Object> textSyncReview =
                validateCaptionNarrationSync(metadata, captionText, voiceOverScript);
        Path source = null;
        Path voice = null;
        Path caption = null;
        Path output = null;
        List<Map<String, Object>> ttsInteractions = List.of();
        List<ProviderFile> ttsAuditFiles = List.of();
        try {
            progressCallback.onProgress(15, SalesVideoStatus.VIDEO_PROCESSING, "Baixando vídeo bruto para pós-produção");
            source = downloadSourceVideo(job, sourceVideoUrl);
            double durationSeconds = probeDurationSeconds(source, metadata, job.id());
            CaptionTimeline captionTimeline;
            VoiceOverAudio voiceOverAudio = null;
            Map<String, Object> audioReview = Map.of("mode", "CAPTION_ONLY", "status", "NOT_REQUESTED");
            if (StringUtils.hasText(voiceOverScript)) {
                if (captionNarrationTimingRequired(metadata)) {
                    SynchronizedNarration synchronizedNarration =
                            generateSynchronizedNarration(captionText, durationSeconds, job.id());
                    voiceOverAudio = synchronizedNarration.audio();
                    captionTimeline = synchronizedNarration.timeline();
                    textSyncReview = mergeTimingReview(textSyncReview, synchronizedNarration);
                    ttsInteractions = synchronizedNarration.interactions();
                    ttsAuditFiles = synchronizedNarration.rawResponseFiles();
                } else {
                    voiceOverAudio = generateVoiceOver(voiceOverScript, job.id(), 1);
                    captionTimeline = buildCaptionTimeline(captionText, durationSeconds);
                    ttsInteractions = List.of(voiceOverAudio.interaction());
                    ttsAuditFiles = voiceOverAudio.rawResponseFile() == null
                            ? List.of()
                            : List.of(voiceOverAudio.rawResponseFile());
                }
                voice = voiceOverAudio.file();
                progressCallback.onProgress(35, SalesVideoStatus.VIDEO_PROCESSING, "Voz off em português gerada por "
                        + voiceOverAudio.providerLabel());
            } else {
                captionTimeline = buildCaptionTimeline(captionText, durationSeconds);
            }
            caption = Files.createTempFile("sales-video-" + job.id() + "-caption", ".ass");
            output = Files.createTempFile("sales-video-" + job.id() + "-final", ".mp4");
            Files.writeString(caption, buildAss(captionTimeline, voiceOverAudio != null), StandardCharsets.UTF_8);
            if (voiceOverAudio != null) {
                progressCallback.onProgress(65, SalesVideoStatus.VIDEO_PROCESSING, "Aplicando legenda e voz premium sincronizadas");
                runFfmpegWithVoice(
                        source,
                        voice,
                        caption,
                        output,
                        durationSeconds,
                        !captionNarrationTimingRequired(metadata));
                audioReview = reviewAudio(output, voiceOverScript, voiceOverAudio);
            } else {
                progressCallback.onProgress(65, SalesVideoStatus.VIDEO_PROCESSING, "Aplicando legenda grande sem voz off");
                runFfmpegCaptionOnly(source, caption, output);
            }
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
                    buildVtt(captionTimeline));
            Map<String, Object> resultMetadata = resultMetadata(
                    job,
                    metadata,
                    captionText,
                    voiceOverScript,
                    audioReview,
                    textSyncReview,
                    captionTimeline,
                    ttsInteractions);
            progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Vídeo finalizado para venda");
            return new ProviderArtifacts(
                    "post-production-" + job.id(), video, null, captions, resultMetadata, ttsAuditFiles);
        } catch (IOException ex) {
            log.error("Falha de arquivo na pós-produção; jobId={} profileId={}", job.id(), profile.id(), ex);
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "Falha de arquivo na pós-produção", ex);
        } catch (VideoProviderException ex) {
            log.error("Falha operacional na pós-produção; jobId={} profileId={} code={}",
                    job.id(), profile.id(), ex.getCode(), ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Falha inesperada na pós-produção; jobId={} profileId={}", job.id(), profile.id(), ex);
            throw new VideoProviderException(
                    "VIDEO_POST_PRODUCTION_FAILED", "Falha inesperada na pós-produção", ex);
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

    /** Gera a voz off usando OpenAI TTS quando configurado, com fallback local explícito. */
    private VoiceOverAudio generateVoiceOver(String voiceOverScript, Long jobId, int segmentIndex) throws IOException {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        if (config.isOpenAiTtsEnabled() && StringUtils.hasText(resolveOpenAiApiKey())) {
            Path voice = Files.createTempFile("sales-video-" + jobId + "-voiceover-openai", "."
                    + config.getOpenAiTtsResponseFormat());
            TtsResponse response = runOpenAiTts(voiceOverScript, voice, jobId, segmentIndex);
            return new VoiceOverAudio(
                    voice,
                    "OPENAI_TTS",
                    config.getOpenAiTtsModel(),
                    config.getOpenAiTtsVoice(),
                    response.interaction(),
                    response.rawResponseFile());
        }
        Path voice = Files.createTempFile("sales-video-" + jobId + "-voiceover-espeak", ".wav");
        runEspeak(voiceOverScript, voice);
        return new VoiceOverAudio(
                voice,
                "ESPEAK_NG",
                "espeak-ng",
                config.getEspeakVoice(),
                localVoiceInteraction(voiceOverScript, jobId, segmentIndex),
                null);
    }

    /** Gera cada trecho exatamente como exibido e usa a duração física do áudio como relógio da legenda. */
    private SynchronizedNarration generateSynchronizedNarration(
            String captionText, double videoDurationSeconds, Long jobId) throws IOException {
        List<String> segments = captionSegments(captionText);
        List<VoiceOverAudio> segmentAudios = new ArrayList<>();
        Path combined = null;
        try {
            List<Double> durations = new ArrayList<>();
            for (int index = 0; index < segments.size(); index++) {
                VoiceOverAudio audio = generateVoiceOver(segments.get(index), jobId, index + 1);
                double duration = probeNarrationDurationSeconds(audio.file(), jobId, index + 1);
                audio = audio.withMeasuredDuration(duration);
                segmentAudios.add(audio);
                durations.add(duration);
            }
            double narrationDuration = durations.stream().mapToDouble(Double::doubleValue).sum();
            if (narrationDuration > videoDurationSeconds + 0.05) {
                throw new VideoProviderException(
                        "APOLLO_NARRATION_DURATION_EXCEEDED",
                        "A narração segmentada dura %.3fs e ultrapassa o vídeo de %.3fs; encurte a copy antes de renderizar."
                                .formatted(narrationDuration, videoDurationSeconds));
            }
            VoiceOverAudio first = segmentAudios.getFirst();
            VoiceOverAudio result;
            if (segmentAudios.size() == 1) {
                result = first;
            } else {
                combined = Files.createTempFile("sales-video-" + jobId + "-voiceover-synchronized", ".wav");
                concatenateNarrationSegments(segmentAudios, combined);
                result = new VoiceOverAudio(
                        combined,
                        first.provider(),
                        first.model(),
                        first.voice(),
                        first.interaction(),
                        null);
            }
            CaptionTimeline timeline = synchronizedCaptionTimeline(
                    segments, durations, videoDurationSeconds);
            if (segmentAudios.size() > 1) {
                segmentAudios.forEach(value -> deleteIfExists(value.file()));
            }
            return new SynchronizedNarration(
                    result,
                    timeline,
                    List.copyOf(durations),
                    narrationDuration,
                    segmentAudios.stream().map(VoiceOverAudio::interaction).toList(),
                    segmentAudios.stream()
                            .map(VoiceOverAudio::rawResponseFile)
                            .filter(java.util.Objects::nonNull)
                            .toList());
        } catch (IOException | RuntimeException ex) {
            segmentAudios.forEach(value -> deleteIfExists(value.file()));
            deleteIfExists(combined);
            if (ex instanceof VideoProviderException providerException) {
                log.error(
                        "Gate de narração segmentada bloqueou a pós-produção; jobId={} code={}",
                        jobId,
                        providerException.getCode(),
                        providerException);
                throw providerException;
            }
            log.error("Falha ao sincronizar narração segmentada; jobId={}", jobId, ex);
            throw ex;
        }
    }

    /** Concatena os trechos de voz sem sobreposição para preservar seus limites medidos. */
    private void concatenateNarrationSegments(List<VoiceOverAudio> segments, Path output) {
        List<String> command = new ArrayList<>();
        command.add(properties.getProviders().getPostProduction().getFfmpegPath());
        command.add("-y");
        for (VoiceOverAudio segment : segments) {
            command.add("-i");
            command.add(segment.file().toAbsolutePath().toString());
        }
        StringBuilder filter = new StringBuilder();
        for (int index = 0; index < segments.size(); index++) {
            filter.append('[').append(index).append(":a]");
        }
        filter.append("concat=n=").append(segments.size()).append(":v=0:a=1[aout]");
        command.addAll(List.of(
                "-filter_complex", filter.toString(),
                "-map", "[aout]",
                "-ac", "1",
                "-ar", "44100",
                "-c:a", "pcm_s16le",
                output.toAbsolutePath().toString()));
        runProcess(command, "ffmpeg falhou ao concatenar a narração segmentada");
    }

    /** Gera o áudio de voz off usando a API de Speech da OpenAI. */
    private TtsResponse runOpenAiTts(
            String voiceOverScript, Path voiceFile, Long jobId, int segmentIndex) throws IOException {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getOpenAiTtsModel());
        payload.put("voice", config.getOpenAiTtsVoice());
        payload.put("input", voiceOverScript);
        payload.put("response_format", config.getOpenAiTtsResponseFormat());
        payload.put("instructions", openAiTtsInstructions(config));
        String endpoint = config.getOpenAiBaseUrl().toString().replaceFirst("/+$", "") + "/audio/speech";
        String rawRequest = objectMapper.writeValueAsString(payload);
        log.info(
                "Enviando request OpenAI TTS; jobId={} segment={} url={} request={}",
                jobId,
                segmentIndex,
                endpoint,
                rawRequest);
        ResponseEntity<byte[]> response;
        try {
            response = openAiWebClient.post()
                    .uri("/audio/speech")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveOpenAiApiKey())
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(byte[].class))
                    .block();
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao chamar OpenAI TTS; jobId={} segment={} url={}",
                    jobId,
                    segmentIndex,
                    endpoint,
                    ex);
            throw new VideoProviderException(
                    "OPENAI_TTS_FAILED",
                    "OpenAI TTS indisponível; jobId=%s; segment=%d; url=%s; rawRequest=%s"
                            .formatted(jobId, segmentIndex, endpoint, rawRequest),
                    ex);
        }
        byte[] content = response == null ? null : response.getBody();
        int statusCode = response == null ? 0 : response.getStatusCode().value();
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String rawResponse = readableResponse(content);
            log.error(
                    "OpenAI TTS recusou request; jobId={} segment={} url={} status={} response={}",
                    jobId,
                    segmentIndex,
                    endpoint,
                    statusCode,
                    rawResponse);
            throw new VideoProviderException(
                    "OPENAI_TTS_FAILED",
                    "OpenAI TTS recusou a locução; jobId=%s; segment=%d; url=%s; status=%d; rawRequest=%s; rawResponse=%s"
                            .formatted(jobId, segmentIndex, endpoint, statusCode, rawRequest, rawResponse));
        }
        if (content == null || content.length == 0) {
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", "OpenAI TTS retornou áudio vazio");
        }
        Files.write(voiceFile, content);
        String responseSha256 = sha256(content);
        MediaType responseType = response.getHeaders().getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : response.getHeaders().getContentType();
        String auditFileName = "sales-video-%d-tts-segment-%02d.%s".formatted(
                jobId, segmentIndex, config.getOpenAiTtsResponseFormat());
        ProviderFile rawResponseFile = new ProviderFile(
                auditFileName,
                responseType,
                AssetType.AUDIO,
                ProviderAssetRole.AUDIO_AUDIT,
                content);
        Map<String, Object> interaction = openAiVoiceInteraction(
                jobId,
                segmentIndex,
                endpoint,
                payload,
                response,
                auditFileName,
                responseSha256,
                content.length);
        log.info(
                "Resposta OpenAI TTS recebida; jobId={} segment={} url={} status={} bytes={} sha256={}",
                jobId,
                segmentIndex,
                endpoint,
                statusCode,
                content.length,
                responseSha256);
        return new TtsResponse(interaction, rawResponseFile);
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

    /** Registra a interação externa sem confundir tarifa publicada com custo efetivamente reconciliado. */
    private Map<String, Object> openAiVoiceInteraction(
            Long jobId,
            int segmentIndex,
            String endpoint,
            Map<String, Object> rawRequest,
            ResponseEntity<byte[]> response,
            String responseAssetFileName,
            String responseSha256,
            int responseBytes) {
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        rawResponse.put("representation", "BINARY_AUDIT_ASSET");
        rawResponse.put("asset_file_name", responseAssetFileName);
        rawResponse.put("content_type", String.valueOf(response.getHeaders().getContentType()));
        rawResponse.put("bytes", responseBytes);
        rawResponse.put("sha256", responseSha256);
        rawResponse.put("request_id", response.getHeaders().getFirst("x-request-id"));

        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("status", "PENDING_PROVIDER_RECONCILIATION");
        pricing.put("cost_usd", null);
        pricing.put("input_usd_per_million_text_tokens", 0.60);
        pricing.put("output_usd_per_million_audio_tokens", 12.00);
        pricing.put("catalog_source", "OPENAI_GPT_4O_MINI_TTS_2026_09_04");
        pricing.put("reason", "O endpoint de Speech devolve áudio binário sem usage por request.");

        Map<String, Object> interaction = new LinkedHashMap<>();
        interaction.put("interaction_type", "TEXT_TO_SPEECH");
        interaction.put("provider", "OPENAI");
        interaction.put("job_id", jobId);
        interaction.put("segment_index", segmentIndex);
        interaction.put("endpoint", endpoint);
        interaction.put("status", "COMPLETED");
        interaction.put("model", rawRequest.get("model"));
        interaction.put("service_tier", "NOT_SUPPORTED_BY_AUDIO_SPEECH_ENDPOINT");
        interaction.put("raw_request", new LinkedHashMap<>(rawRequest));
        interaction.put("raw_response", rawResponse);
        interaction.put("input_character_count", String.valueOf(rawRequest.get("input")).length());
        interaction.put("usage_status", "PENDING_PROVIDER_RECONCILIATION");
        interaction.put("pricing", pricing);
        interaction.put("completed_at", Instant.now().toString());
        return interaction;
    }

    /** Registra que o fallback local não realizou chamada externa nem gerou custo de provedor. */
    private Map<String, Object> localVoiceInteraction(String text, Long jobId, int segmentIndex) {
        Map<String, Object> interaction = new LinkedHashMap<>();
        interaction.put("interaction_type", "LOCAL_TEXT_TO_SPEECH");
        interaction.put("provider", "ESPEAK_NG");
        interaction.put("job_id", jobId);
        interaction.put("segment_index", segmentIndex);
        interaction.put("status", "COMPLETED_LOCAL_ONLY");
        interaction.put("input_character_count", text.length());
        interaction.put("cost_status", "NOT_APPLICABLE");
        interaction.put("cost_usd", 0);
        return interaction;
    }

    /** Converte resposta textual de erro em evidência limitada para o callback persistido. */
    private String readableResponse(byte[] content) {
        if (content == null || content.length == 0) {
            return "<empty>";
        }
        String value = new String(content, StandardCharsets.UTF_8);
        return value.length() <= 16_384 ? value : value.substring(0, 16_384) + "...[truncated]";
    }

    /** Calcula a identidade SHA-256 do binário bruto recebido do provedor de voz. */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível durante a auditoria da resposta OpenAI TTS", ex);
            throw new IllegalStateException("SHA-256 indisponível para auditoria de TTS", ex);
        }
    }

    /** Carrega a direção vocal versionada, preservando override operacional explícito quando houver. */
    private String openAiTtsInstructions(VideoManagementProperties.PostProduction config) {
        if (StringUtils.hasText(config.getOpenAiTtsInstructions())) {
            return config.getOpenAiTtsInstructions().trim();
        }
        String path = config.getOpenAiTtsInstructionsPath();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            log.error("Falha ao carregar direção vocal versionada; path={}", path, ex);
            throw new VideoProviderException(
                    "OPENAI_TTS_CONTRACT_INVALID", "Direção vocal de Apolo não foi empacotada.", ex);
        }
    }

    /** Compõe o MP4 final apenas com legenda queimada, preservando o vídeo fonte. */
    private void runFfmpegCaptionOnly(Path source, Path caption, Path output) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        String videoFilter = captionSubtitles(caption);
        runProcess(List.of(
                config.getFfmpegPath(),
                "-y",
                "-i", source.toAbsolutePath().toString(),
                "-vf", videoFilter,
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "20",
                "-pix_fmt", "yuv420p",
                "-an",
                "-movflags", "+faststart",
                output.toAbsolutePath().toString()),
                "ffmpeg falhou ao aplicar legenda no vídeo");
    }

    /** Compõe o MP4 final com legenda queimada, voz e trilha discreta. */
    private void runFfmpegWithVoice(Path source,
                                    Path voice,
                                    Path caption,
                                    Path output,
                                    double durationSeconds,
                                    boolean includeSyntheticBed) {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        String videoFilter = captionSubtitles(caption);
        String audioFilter;
        List<String> command = new ArrayList<>(List.of(
                config.getFfmpegPath(),
                "-y",
                "-i", source.toAbsolutePath().toString(),
                "-i", voice.toAbsolutePath().toString()));
        if (includeSyntheticBed) {
            command.addAll(List.of(
                    "-f", "lavfi",
                    "-i", "sine=frequency=220:sample_rate=44100:duration=" + durationSeconds));
            audioFilter = "[2:a]volume=0.018,afade=t=in:st=0:d=1,afade=t=out:st="
                    + Math.max(1, durationSeconds - 1) + ":d=1[music];[1:a]volume=1.0[voice];"
                    + "[voice][music]amix=inputs=2:duration=longest:dropout_transition=0[mixed];"
                    + "[mixed]loudnorm=I=-17:TP=-2:LRA=7[aout];";
        } else {
            audioFilter = "[1:a]volume=1.0,loudnorm=I=-17:TP=-2:LRA=7[aout];";
        }
        String filter = audioFilter + "[0:v]" + videoFilter + "[vout]";
        command.addAll(List.of(
                "-filter_complex", filter,
                "-map", "[vout]",
                "-map", "[aout]",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "20",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-t", formatFfmpegDuration(durationSeconds),
                "-movflags", "+faststart",
                output.toAbsolutePath().toString()));
        runProcess(command, "ffmpeg falhou ao finalizar vídeo para venda");
    }

    /** Formata a duração limite usada para impedir que filtros de áudio mantenham o render aberto. */
    private String formatFfmpegDuration(double durationSeconds) {
        return java.math.BigDecimal.valueOf(durationSeconds).stripTrailingZeros().toPlainString();
    }

    /** Monta o filtro de legendas temporizadas para leitura em telas mobile. */
    private String captionSubtitles(Path caption) {
        return "subtitles=filename='%s':charenc=UTF-8".formatted(
                escapeFilterPath(caption.toAbsolutePath().toString()));
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
            log.error("Falha ao iniciar processo de pós-produção; operation={}", failureMessage, ex);
            throw new VideoProviderException("VIDEO_POST_PRODUCTION_FAILED", failureMessage, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Processo de pós-produção interrompido; operation={}", failureMessage, ex);
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
            log.warn("Metadata de pós-produção inválida; jobId={}", job.id(), ex);
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

    /** Retorna texto opcional normalizado dos metadados. */
    private String optionalText(JsonNode metadata, String field) {
        String value = metadata.path(field).asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
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

    /** Mede a duração real do vídeo para sincronizar legenda, áudio e auditoria. */
    private double probeDurationSeconds(Path source, JsonNode metadata, Long jobId) {
        try {
            String value = runProcess(List.of(
                    properties.getProviders().getPostProduction().getFfprobePath(),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    source.toAbsolutePath().toString()),
                    "ffprobe falhou ao medir vídeo fonte").trim();
            double duration = Double.parseDouble(value);
            if (duration > 0 && Double.isFinite(duration)) {
                return duration;
            }
        } catch (RuntimeException ex) {
            log.warn("Duração do vídeo não pôde ser medida; jobId={}", jobId, ex);
        }
        double fallback = metadata.path("targetDurationSeconds").asDouble(30);
        return fallback > 0 && Double.isFinite(fallback) ? fallback : 30;
    }

    /** Mede a duração de um trecho narrado e falha fechado para não inventar timestamps. */
    private double probeNarrationDurationSeconds(Path source, Long jobId, int segment) {
        try {
            String value = runProcess(List.of(
                    properties.getProviders().getPostProduction().getFfprobePath(),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    source.toAbsolutePath().toString()),
                    "ffprobe falhou ao medir trecho narrado").trim();
            double duration = Double.parseDouble(value);
            if (duration > 0 && Double.isFinite(duration)) {
                return duration;
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao medir trecho narrado; jobId={} segment={}", jobId, segment, ex);
            throw new VideoProviderException(
                    "APOLLO_NARRATION_TIMING_UNAVAILABLE",
                    "Apolo não conseguiu medir o trecho narrado " + segment + ".",
                    ex);
        }
        throw new VideoProviderException(
                "APOLLO_NARRATION_TIMING_UNAVAILABLE",
                "O trecho narrado " + segment + " não possui duração válida.");
    }

    /** Divide uma legenda por barras verticais e distribui os trechos pelo vídeo. */
    private CaptionTimeline buildCaptionTimeline(String captionText, double durationSeconds) {
        List<String> segments = captionSegments(captionText);
        double segmentDuration = durationSeconds / segments.size();
        List<CaptionCue> cues = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            double start = index * segmentDuration;
            double end = index == segments.size() - 1 ? durationSeconds : (index + 1) * segmentDuration;
            cues.add(new CaptionCue(start, end, segments.get(index)));
        }
        return new CaptionTimeline(durationSeconds, cues);
    }

    /** Separa os trechos editoriais preservando exatamente a ordem da copy aprovada. */
    private List<String> captionSegments(String captionText) {
        List<String> segments = java.util.Arrays.stream(captionText.split("\\s*\\|\\s*"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .limit(12)
                .toList();
        return segments.isEmpty() ? List.of(captionText.trim()) : segments;
    }

    /** Constrói cues nos limites físicos de cada áudio e mantém o CTA visível até o fim. */
    private CaptionTimeline synchronizedCaptionTimeline(
            List<String> segments, List<Double> durations, double videoDurationSeconds) {
        List<CaptionCue> cues = new ArrayList<>();
        double cursor = 0;
        for (int index = 0; index < segments.size(); index++) {
            double start = cursor;
            cursor += durations.get(index);
            double end = index == segments.size() - 1 ? videoDurationSeconds : cursor;
            cues.add(new CaptionCue(start, end, segments.get(index)));
        }
        return new CaptionTimeline(videoDurationSeconds, cues);
    }

    /** Identifica o contrato premium que exige relógio derivado do áudio de cada trecho. */
    private boolean captionNarrationTimingRequired(JsonNode metadata) {
        return metadata.at("/technicalQualityGate/captionMustMatchNarration").asBoolean(false);
    }

    /** Acrescenta ao gate textual a evidência temporal medida de cada trecho narrado. */
    private Map<String, Object> mergeTimingReview(
            Map<String, Object> textReview, SynchronizedNarration narration) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(textReview);
        result.put("timing_status", "APPROVED");
        result.put("timing_method", "SEGMENT_AUDIO_DURATION");
        result.put("segment_count", narration.segmentDurationsSeconds().size());
        result.put("segment_durations_seconds", narration.segmentDurationsSeconds());
        result.put("narration_duration_seconds", narration.narrationDurationSeconds());
        result.put("synthetic_music_bed", false);
        return result;
    }

    /** Gera ASS com caixa legível e margens seguras para Reels e Stories. */
    private String buildAss(CaptionTimeline timeline, boolean aiVoiceDisclosureRequired) {
        StringBuilder ass = new StringBuilder("""
                [Script Info]
                ScriptType: v4.00+
                PlayResX: 720
                PlayResY: 1280
                WrapStyle: 0

                [V4+ Styles]
                Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
                Style: Default,DejaVu Sans,48,&H00FFFFFF,&H00FFFFFF,&H90000000,&H78000000,-1,0,0,0,100,100,0,0,3,2,0,2,54,54,170,1
                Style: Disclosure,DejaVu Sans,22,&H00FFFFFF,&H00FFFFFF,&H90000000,&H78000000,0,0,0,0,100,100,0,0,3,1,0,8,54,54,70,1

                [Events]
                Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
                """);
        if (aiVoiceDisclosureRequired) {
            ass.append("Dialogue: 1,")
                    .append(formatAssTime(0)).append(',')
                    .append(formatAssTime(timeline.durationSeconds()))
                    .append(",Disclosure,,0,0,0,,Voz gerada por IA\n");
        }
        for (CaptionCue cue : timeline.cues()) {
            ass.append("Dialogue: 0,")
                    .append(formatAssTime(cue.startSeconds())).append(',')
                    .append(formatAssTime(cue.endSeconds())).append(",Default,,0,0,0,,")
                    .append(escapeAssText(wrapCaption(cue.text()))).append('\n');
        }
        return ass.toString();
    }

    /** Gera VTT temporizado para auditoria e players com legenda externa. */
    private byte[] buildVtt(CaptionTimeline timeline) {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        for (int index = 0; index < timeline.cues().size(); index++) {
            CaptionCue cue = timeline.cues().get(index);
            vtt.append(index + 1).append('\n')
                    .append(formatVttTime(cue.startSeconds())).append(" --> ")
                    .append(formatVttTime(cue.endSeconds())).append('\n')
                    .append(wrapCaption(cue.text())).append("\n\n");
        }
        return vtt.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Formata segundos no relógio centesimal do formato ASS. */
    private String formatAssTime(double seconds) {
        long centiseconds = Math.max(0, Math.round(seconds * 100));
        return "%d:%02d:%02d.%02d".formatted(
                centiseconds / 360000,
                (centiseconds / 6000) % 60,
                (centiseconds / 100) % 60,
                centiseconds % 100);
    }

    /** Formata segundos no relógio milissegundo do formato VTT. */
    private String formatVttTime(double seconds) {
        long milliseconds = Math.max(0, Math.round(seconds * 1000));
        return "%02d:%02d:%02d.%03d".formatted(
                milliseconds / 3600000,
                (milliseconds / 60000) % 60,
                (milliseconds / 1000) % 60,
                milliseconds % 1000);
    }

    /** Escapa texto de legenda para não permitir comandos ASS vindos do formulário. */
    private String escapeAssText(String text) {
        return text.replace("\\", "\\\\")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("\n", "\\N");
    }

    /** Revisa o áudio final com métricas objetivas e decisão comercial para mobile. */
    private Map<String, Object> reviewAudio(Path output, String voiceOverScript, VoiceOverAudio voiceOverAudio) {
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
            return buildAudioReview(outputText, voiceOverScript, voiceOverAudio);
        } catch (RuntimeException ex) {
            log.warn("Revisão automática de áudio indisponível; output={}", output, ex);
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
    private Map<String, Object> buildAudioReview(String ffmpegOutput,
                                                 String voiceOverScript,
                                                 VoiceOverAudio voiceOverAudio) {
        Double integratedLufs = parseLastMetric(ffmpegOutput, "I:\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+LUFS");
        Double truePeakDbfs = parseLastMetric(ffmpegOutput, "Peak:\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+dBFS");
        boolean syntheticVoice = voiceOverAudio.isSyntheticLocal();
        String status = resolveAudioReviewStatus(integratedLufs, truePeakDbfs, syntheticVoice);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("integrated_lufs", integratedLufs);
        metrics.put("true_peak_dbfs", truePeakDbfs);
        metrics.put("target_lufs_min", -18);
        metrics.put("target_lufs_max", -16);
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("status", status);
        review.put("label", audioReviewLabel(status));
        review.put("provider", voiceOverAudio.provider());
        review.put("model", voiceOverAudio.model());
        review.put("voice", voiceOverAudio.voice());
        review.put("voice_quality", syntheticVoice ? "synthetic_local" : "natural_tts_candidate");
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
                                               Map<String, Object> audioReview,
                                               Map<String, Object> textSyncReview,
                                               CaptionTimeline captionTimeline,
                                               List<Map<String, Object>> ttsInteractions) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "MUSA_POST_PRODUCTION");
        metadata.put("provider_job_id", "post-production-" + job.id());
        metadata.put("post_production_mode", StringUtils.hasText(voiceOverScript) ? "VOICE_AND_CAPTION" : "CAPTION_ONLY");
        metadata.put("duration_seconds", captionTimeline.durationSeconds());
        boolean hasAudio = StringUtils.hasText(voiceOverScript);
        metadata.put("has_audio", hasAudio);
        metadata.put("audio_streams", hasAudio ? 1 : 0);
        metadata.put("audio", Map.of(
                "voice_over", StringUtils.hasText(voiceOverScript),
                "language", "pt-BR",
                "ai_generated_disclosure", StringUtils.hasText(voiceOverScript),
                "ai_generated_disclosure_text", StringUtils.hasText(voiceOverScript)
                        ? "Voz gerada por IA"
                        : "",
                "music", "SEGMENT_AUDIO_DURATION".equals(textSyncReview.get("timing_method"))
                        ? "none"
                        : StringUtils.hasText(voiceOverScript) ? "synthetic_light_bed" : "none",
                "review", audioReview));
        metadata.put("captions", Map.of(
                "burned_in", true,
                "vtt_asset", true,
                "text", captionText,
                "cue_count", captionTimeline.cues().size(),
                "timed", captionTimeline.cues().size() > 1));
        metadata.put("caption_narration_sync", textSyncReview);
        metadata.put("tts_interactions", ttsInteractions);
        metadata.put(
                "tts_cost_reconciliation_status",
                ttsInteractions.stream().anyMatch(value -> "OPENAI".equals(value.get("provider")))
                        ? "PENDING_PROVIDER_RECONCILIATION"
                        : "NOT_APPLICABLE");
        metadata.put("voice_over_script", StringUtils.hasText(voiceOverScript) ? voiceOverScript : null);
        metadata.put("cta_text", sourceMetadata.path("post_production").path("cta_text").asText(null));
        metadata.put("source_experiment_video_asset_id", sourceMetadata.path("experimentVideoAssetId").asLong());
        metadata.put("finished_at", Instant.now().toString());
        return metadata;
    }

    /** Exige que a narração repita a mesma sequência normalizada exibida nas legendas. */
    private Map<String, Object> validateCaptionNarrationSync(
            JsonNode metadata, String captionText, String voiceOverScript) {
        boolean required =
                metadata.at("/technicalQualityGate/captionMustMatchNarration").asBoolean(false);
        if (!required) {
            return Map.of("status", "NOT_REQUIRED");
        }
        if (!StringUtils.hasText(voiceOverScript)) {
            throw new VideoProviderException(
                    "APOLLO_CAPTION_NARRATION_MISMATCH",
                    "O gate de Apolo exige narração para o mesmo texto exibido.");
        }
        String normalizedCaption = normalizeSpokenText(captionText);
        String normalizedNarration = normalizeSpokenText(voiceOverScript);
        if (!normalizedCaption.equals(normalizedNarration)) {
            throw new VideoProviderException(
                    "APOLLO_CAPTION_NARRATION_MISMATCH",
                    "O texto exibido diverge da sequência narrada; gere ambos a partir da mesma fonte.");
        }
        return Map.of(
                "status", "APPROVED",
                "method", "NORMALIZED_EXACT_SEQUENCE",
                "word_count", normalizedCaption.isBlank() ? 0 : normalizedCaption.split(" ").length);
    }

    /** Normaliza pausas, pontuação e acentos sem permitir troca ou omissão de palavras. */
    private String normalizeSpokenText(String value) {
        String decomposed =
                java.text.Normalizer.normalize(
                    value == null ? "" : value.replace('|', ' '), java.text.Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
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
        } catch (IOException ex) {
            log.warn("Falha ao remover arquivo temporário da pós-produção; path={}", path, ex);
        }
    }

    /** Resolve a credencial de TTS sem expor seu conteúdo em log ou metadata. */
    private String resolveOpenAiApiKey() {
        VideoManagementProperties.PostProduction config = properties.getProviders().getPostProduction();
        if (StringUtils.hasText(config.getOpenAiApiKey())) {
            return config.getOpenAiApiKey().trim();
        }
        if (!StringUtils.hasText(config.getOpenAiApiKeyFile())) {
            return "";
        }
        try {
            Path path = Path.of(config.getOpenAiApiKeyFile().trim());
            if (!Files.isReadable(path)) {
                return "";
            }
            return Files.readString(path).trim();
        } catch (IOException ex) {
            log.error("Falha ao ler secret OpenAI para TTS; file={}", config.getOpenAiApiKeyFile(), ex);
            throw new UncheckedIOException("Não foi possível ler OPENAI_API_KEY_FILE para TTS", ex);
        }
    }

    /** Representa um trecho de legenda e sua janela exata de exibição. */
    private record CaptionCue(double startSeconds, double endSeconds, String text) { }

    /** Agrupa a duração física do vídeo e todas as legendas temporizadas. */
    private record CaptionTimeline(double durationSeconds, List<CaptionCue> cues) { }

    /** Preserva áudio contínuo, cues e medições produzidos pelo sincronismo de Apolo. */
    private record SynchronizedNarration(
            VoiceOverAudio audio,
            CaptionTimeline timeline,
            List<Double> segmentDurationsSeconds,
            double narrationDurationSeconds,
            List<Map<String, Object>> interactions,
            List<ProviderFile> rawResponseFiles) { }

    /** Agrupa o registro da chamada TTS e o binário bruto que será persistido como ativo. */
    private record TtsResponse(
            Map<String, Object> interaction,
            ProviderFile rawResponseFile) { }

    /** Descreve o arquivo de voz off gerado para auditoria da pós-produção. */
    private record VoiceOverAudio(
            Path file,
            String provider,
            String model,
            String voice,
            Map<String, Object> interaction,
            ProviderFile rawResponseFile) {
        /** Acrescenta a duração medida sem alterar a evidência original do provedor. */
        VoiceOverAudio withMeasuredDuration(double durationSeconds) {
            Map<String, Object> measured = new LinkedHashMap<>(interaction);
            measured.put("output_duration_seconds", durationSeconds);
            return new VoiceOverAudio(file, provider, model, voice, measured, rawResponseFile);
        }

        /** Retorna o rótulo operacional do provedor de voz. */
        String providerLabel() {
            return provider + "/" + voice;
        }

        /** Indica se a voz veio do fallback local robótico. */
        boolean isSyntheticLocal() {
            return "ESPEAK_NG".equals(provider);
        }
    }
}
