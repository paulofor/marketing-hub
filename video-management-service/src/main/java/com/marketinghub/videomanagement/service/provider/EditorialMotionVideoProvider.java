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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

/** Gera criativos verticais com movimento editorial a partir de imagens comerciais aprovadas. */
@Component
@ConditionalOnProperty(prefix = "video.providers.editorial-motion", name = "enabled", havingValue = "true")
public class EditorialMotionVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(EditorialMotionVideoProvider.class);
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient downloadWebClient;

    /** Inicializa o gerador com configuração, parser JSON e cliente de download limitado. */
    public EditorialMotionVideoProvider(VideoManagementProperties properties,
                                        ObjectMapper objectMapper,
                                        WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        int maxBytes = properties.getProviders().getEditorialMotion().getMaxImageBytes();
        this.downloadWebClient = webClientBuilder.clone()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxBytes))
                        .build())
                .build();
    }

    /** Verifica se o job pede o gerador editorial local. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        return properties.getProviders().getEditorialMotion().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Baixa imagens aprovadas, anima cada corte e entrega um MP4 vertical auditável. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        JsonNode metadata = readMetadata(job);
        List<SourceImage> sources = resolveSources(metadata);
        List<Cut> cuts = resolveCuts(metadata, profile, sources);
        Map<String, DownloadedImage> downloaded = new LinkedHashMap<>();
        List<Path> temporaryFiles = new ArrayList<>();
        try {
            progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING,
                    "Carregando imagens aprovadas para o criativo editorial");
            for (SourceImage source : sources) {
                downloaded.computeIfAbsent(source.url(), url -> downloadImage(job, source, temporaryFiles));
            }
            List<Path> clips = new ArrayList<>();
            for (int index = 0; index < cuts.size(); index++) {
                Cut cut = cuts.get(index);
                SourceImage source = sources.get(index % sources.size());
                DownloadedImage image = downloaded.get(source.url());
                int progress = 20 + ((index + 1) * 55 / cuts.size());
                progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                        "Animando corte editorial %d/%d".formatted(index + 1, cuts.size()));
                clips.add(renderCut(job, cut, image.file(), index, temporaryFiles));
            }
            progressCallback.onProgress(82, SalesVideoStatus.VIDEO_PROCESSING,
                    "Montando narrativa editorial vertical");
            Path output = assemble(job, clips, temporaryFiles);
            byte[] videoBytes = Files.readAllBytes(output);
            ensureMp4(videoBytes);
            ProviderFile video = new ProviderFile(
                    "sales-video-" + job.id() + "-editorial-motion.mp4",
                    VIDEO_MP4,
                    AssetType.VIDEO,
                    ProviderAssetRole.VIDEO,
                    videoBytes);
            Map<String, Object> resultMetadata = resultMetadata(job, sources, downloaded, cuts);
            progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING,
                    "Criativo editorial pronto para pós-produção");
            return new ProviderArtifacts("editorial-motion-" + job.id(), video, null, null, resultMetadata);
        } catch (IOException ex) {
            log.error("Falha de arquivo no criativo editorial; jobId={} profileId={}",
                    job.id(), profile.id(), ex);
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_FAILED", "Falha de arquivo ao gerar criativo editorial", ex);
        } finally {
            temporaryFiles.forEach(this::deleteIfExists);
        }
    }

    /** Lê o contrato persistido do Estúdio. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (!StringUtils.hasText(job.metadataJson())) {
            throw new VideoProviderException("EDITORIAL_MOTION_INVALID_REQUEST", "Metadata editorial ausente");
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            log.warn("Metadata editorial inválida; jobId={}", job.id(), ex);
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_INVALID_REQUEST", "Metadata editorial inválida", ex);
        }
    }

    /** Resolve a sequência explícita de imagens ou usa a imagem-base aprovada do render. */
    private List<SourceImage> resolveSources(JsonNode metadata) {
        JsonNode sourceArray = metadata.path("editorial_source_images");
        if (!sourceArray.isArray()) {
            sourceArray = metadata.path("editorialSourceImages");
        }
        List<SourceImage> result = new ArrayList<>();
        if (sourceArray.isArray()) {
            for (JsonNode source : sourceArray) {
                String url = firstText(source, "url", "source_image_url");
                if (StringUtils.hasText(url)) {
                    result.add(new SourceImage(source.path("assetId").asLong(0), validateSourceUri(url).toString()));
                }
            }
        }
        if (result.isEmpty()) {
            JsonNode imageToVideo = metadata.path("image_to_video");
            String url = firstText(imageToVideo, "source_image_url", "reference_image_url");
            if (StringUtils.hasText(url)) {
                result.add(new SourceImage(
                        imageToVideo.path("source_asset_id").asLong(0),
                        validateSourceUri(url).toString()));
            }
        }
        if (result.isEmpty()) {
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_INVALID_REQUEST", "Informe ao menos uma imagem aprovada no metadata editorial");
        }
        return result;
    }

    /** Valida que a origem remota usa apenas HTTP ou HTTPS. */
    private URI validateSourceUri(String value) {
        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_INVALID_REQUEST", "Imagem editorial deve usar URL HTTP(S) absoluta");
        }
        return uri;
    }

    /** Converte o plano de cortes persistido em duração e papel comercial executáveis. */
    private List<Cut> resolveCuts(JsonNode metadata, SalesVideoProfile profile, List<SourceImage> sources) {
        VideoManagementProperties.EditorialMotion config = properties.getProviders().getEditorialMotion();
        JsonNode cutPlan = metadata.path("cut_plan");
        List<Cut> cuts = new ArrayList<>();
        if (cutPlan.isArray()) {
            for (int index = 0; index < Math.min(cutPlan.size(), config.getMaxCuts()); index++) {
                JsonNode cut = cutPlan.get(index);
                int seconds = Math.max(1, Math.min(10,
                        cut.path("duration_seconds").asInt(cut.path("durationSeconds").asInt(3))));
                cuts.add(new Cut(seconds, cut.path("role").asText("MECANISMO")));
            }
        }
        if (cuts.isEmpty()) {
            int target = metadata.path("targetDurationSeconds").asInt(
                    profile.targetDurationSeconds() == null ? 24 : profile.targetDurationSeconds());
            target = Math.max(6, Math.min(target, config.getMaxDurationSeconds()));
            int count = Math.min(config.getMaxCuts(), Math.max(2, sources.size()));
            int baseSeconds = Math.max(1, target / count);
            for (int index = 0; index < count; index++) {
                cuts.add(new Cut(baseSeconds + (index < target % count ? 1 : 0), "MECANISMO"));
            }
        }
        int totalSeconds = cuts.stream().mapToInt(Cut::durationSeconds).sum();
        if (totalSeconds > config.getMaxDurationSeconds()) {
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_INVALID_REQUEST",
                    "Plano editorial excede %d segundos".formatted(config.getMaxDurationSeconds()));
        }
        return cuts;
    }

    /** Baixa e registra a identidade técnica da imagem sem persistir seus bytes em log. */
    private DownloadedImage downloadImage(SalesVideoJob job,
                                          SourceImage source,
                                          List<Path> temporaryFiles) {
        try {
            ResponseEntity<byte[]> response = downloadWebClient.get()
                    .uri(URI.create(source.url()))
                    .retrieve()
                    .toEntity(byte[].class)
                    .block();
            byte[] content = response == null ? null : response.getBody();
            MediaType mediaType = response == null ? null : response.getHeaders().getContentType();
            if (content == null || content.length == 0 || content.length > properties.getProviders()
                    .getEditorialMotion().getMaxImageBytes() || mediaType == null
                    || !"image".equalsIgnoreCase(mediaType.getType())) {
                throw new VideoProviderException(
                        "EDITORIAL_MOTION_INVALID_REQUEST", "Origem editorial não retornou imagem válida");
            }
            String extension = mediaType.getSubtype().toLowerCase(Locale.ROOT).contains("png") ? ".png" : ".jpg";
            Path file = Files.createTempFile("editorial-motion-" + job.id(), extension);
            Files.write(file, content);
            temporaryFiles.add(file);
            String sha256 = sha256(content);
            log.info("Imagem editorial carregada; jobId={} assetId={} url={} status={} mediaType={} bytes={} sha256={}",
                    job.id(), source.assetId(), source.url(), response.getStatusCode().value(), mediaType,
                    content.length, sha256);
            return new DownloadedImage(file, sha256, content.length);
        } catch (IOException ex) {
            log.error("Falha ao guardar imagem editorial; jobId={} assetId={} url={}",
                    job.id(), source.assetId(), source.url(), ex);
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_FAILED", "Falha ao preparar imagem editorial", ex);
        }
    }

    /** Renderiza um corte com movimento de câmera e gradação coerente com seu papel comercial. */
    private Path renderCut(SalesVideoJob job,
                           Cut cut,
                           Path image,
                           int index,
                           List<Path> temporaryFiles) throws IOException {
        VideoManagementProperties.EditorialMotion config = properties.getProviders().getEditorialMotion();
        Path output = Files.createTempFile("editorial-motion-" + job.id() + "-cut-" + index, ".mp4");
        temporaryFiles.add(output);
        int frames = cut.durationSeconds() * config.getFramesPerSecond();
        String filter = buildVisualFilter(index, frames, cut.role(), config);
        runProcess(List.of(
                config.getFfmpegPath(), "-y", "-hide_banner", "-loglevel", "error",
                "-i", image.toAbsolutePath().toString(),
                "-vf", filter,
                "-frames:v", String.valueOf(frames),
                "-r", String.valueOf(config.getFramesPerSecond()),
                "-an", "-c:v", "libx264", "-preset", "veryfast", "-crf", "18",
                "-pix_fmt", "yuv420p", "-movflags", "+faststart", output.toString()),
                "ffmpeg falhou ao animar corte editorial " + (index + 1));
        return output;
    }

    /** Alterna movimentos sutis e realça gradualmente resultado e CTA sem inventar cena. */
    private String buildVisualFilter(int index,
                                     int frames,
                                     String role,
                                     VideoManagementProperties.EditorialMotion config) {
        double initialZoom = switch (index % 4) {
            case 1 -> 1.08;
            case 2 -> 1.04;
            case 3 -> 1.06;
            default -> 1.01;
        };
        String zoom = index % 2 == 0
                ? "min(zoom+0.00065,1.12)"
                : "max(zoom-0.00050,1.01)";
        String x = switch (index % 4) {
            case 1 -> "min(max((iw-iw/zoom)*on/%d,0),iw-iw/zoom)".formatted(Math.max(1, frames - 1));
            case 3 -> "max((iw-iw/zoom)*(1-on/%d),0)".formatted(Math.max(1, frames - 1));
            default -> "iw/2-(iw/zoom/2)";
        };
        String y = index % 3 == 2
                ? "min(max((ih-ih/zoom)*0.30,0),ih-ih/zoom)"
                : "ih/2-(ih/zoom/2)";
        double saturation = "HOOK_DOR".equalsIgnoreCase(role) ? 0.90
                : ("RESULTADO".equalsIgnoreCase(role) || "CTA".equalsIgnoreCase(role) ? 1.08 : 1.01);
        double brightness = "RESULTADO".equalsIgnoreCase(role) || "CTA".equalsIgnoreCase(role) ? 0.012 : 0.0;
        return "scale=800:1422:force_original_aspect_ratio=increase,crop=800:1422,"
                + "zoompan=z='if(eq(on,0),%.3f,%s)':x='%s':y='%s':d=%d:s=%dx%d:fps=%d,"
                        .formatted(initialZoom, zoom, x, y, frames, config.getWidth(), config.getHeight(),
                                config.getFramesPerSecond())
                + "eq=contrast=1.025:saturation=%.3f:brightness=%.3f,format=yuv420p"
                        .formatted(saturation, brightness);
    }

    /** Concatena cortes homogêneos sem nova perda visual. */
    private Path assemble(SalesVideoJob job, List<Path> clips, List<Path> temporaryFiles) throws IOException {
        Path manifest = Files.createTempFile("editorial-motion-" + job.id(), ".txt");
        temporaryFiles.add(manifest);
        StringBuilder entries = new StringBuilder();
        for (Path clip : clips) {
            entries.append("file '").append(clip.toAbsolutePath()).append("'\n");
        }
        Files.writeString(manifest, entries, StandardCharsets.UTF_8);
        Path output = Files.createTempFile("editorial-motion-" + job.id() + "-final", ".mp4");
        temporaryFiles.add(output);
        runProcess(List.of(
                properties.getProviders().getEditorialMotion().getFfmpegPath(),
                "-y", "-hide_banner", "-loglevel", "error",
                "-f", "concat", "-safe", "0", "-i", manifest.toString(),
                "-c", "copy", "-movflags", "+faststart", output.toString()),
                "ffmpeg falhou ao montar criativo editorial");
        return output;
    }

    /** Executa ffmpeg preservando sua saída para diagnóstico causal. */
    private void runProcess(List<String> command, String failureMessage) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoProviderException(
                        "EDITORIAL_MOTION_FAILED",
                        failureMessage + "; exitCode=" + exitCode + "; output=" + output);
            }
        } catch (IOException ex) {
            log.error("Falha ao iniciar processo editorial; operation={}", failureMessage, ex);
            throw new VideoProviderException("EDITORIAL_MOTION_FAILED", failureMessage, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Processo editorial interrompido; operation={}", failureMessage, ex);
            throw new VideoProviderException("EDITORIAL_MOTION_FAILED", failureMessage, ex);
        }
    }

    /** Confirma a assinatura MP4 antes de publicar o asset no backend. */
    private void ensureMp4(byte[] content) {
        if (content.length < 12 || content[4] != 'f' || content[5] != 't'
                || content[6] != 'y' || content[7] != 'p') {
            throw new VideoProviderException(
                    "EDITORIAL_MOTION_FAILED", "Criativo editorial não produziu um MP4 íntegro");
        }
    }

    /** Monta auditoria de fontes, cortes, custo e limite criativo do fallback. */
    private Map<String, Object> resultMetadata(SalesVideoJob job,
                                               List<SourceImage> sources,
                                               Map<String, DownloadedImage> downloaded,
                                               List<Cut> cuts) {
        List<Map<String, Object>> sourceAudit = sources.stream().map(source -> {
            DownloadedImage image = downloaded.get(source.url());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("asset_id", source.assetId() == 0 ? null : source.assetId());
            entry.put("url", source.url());
            entry.put("sha256", image.sha256());
            entry.put("bytes", image.bytes());
            return entry;
        }).toList();
        Set<Long> assetIds = new LinkedHashSet<>();
        sources.stream().map(SourceImage::assetId).filter(id -> id > 0).forEach(assetIds::add);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "EDITORIAL_MOTION");
        metadata.put("provider_job_id", "editorial-motion-" + job.id());
        metadata.put("generation_mode", "DETERMINISTIC_KINETIC_STILL");
        metadata.put("duration_seconds", cuts.stream().mapToInt(Cut::durationSeconds).sum());
        metadata.put("cut_count", cuts.size());
        metadata.put("source_asset_ids", assetIds);
        metadata.put("sources", sourceAudit);
        metadata.put("cost_usd", 0);
        metadata.put("cost_scope", "LOCAL_RENDER_ONLY");
        metadata.put("generated_at", Instant.now().toString());
        metadata.put("human_review_required", true);
        metadata.put("commercial_limitation",
                "Movimento editorial de câmera sobre imagens aprovadas; não representa movimento corporal sintetizado.");
        return metadata;
    }

    /** Calcula o hash auditável dos bytes recebidos. */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    /** Retorna o primeiro campo textual preenchido. */
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /** Normaliza nomes de provider para comparação estável. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Remove somente arquivos temporários criados pelo job atual. */
    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Falha ao remover arquivo temporário editorial; path={}", path, ex);
        }
    }

    /** Identifica uma imagem aprovada usada como fonte do movimento editorial. */
    private record SourceImage(long assetId, String url) { }

    /** Preserva o arquivo temporário e sua identidade auditável durante o render. */
    private record DownloadedImage(Path file, String sha256, int bytes) { }

    /** Descreve a duração e a função comercial de um corte editorial. */
    private record Cut(int durationSeconds, String role) { }
}
