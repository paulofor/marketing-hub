package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetResponse;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

/** Empacota o MP4 final em HLS e registra segmentos e manifesto pela API canônica de assets. */
public class HlsVideoDelivery {
    private static final Logger log = LoggerFactory.getLogger(HlsVideoDelivery.class);
    private final VideoAssetClient client;
    private final VideoManagementProperties properties;
    private final ObjectMapper mapper;

    /** Mantém uploads no backend e processamento de mídia no executor. */
    public HlsVideoDelivery(VideoAssetClient client, VideoManagementProperties properties, ObjectMapper mapper) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** Gera HLS sem IA nem reencodificação e só retorna sucesso após registrar todos os arquivos. */
    public Delivery deliver(SalesVideoJob job, ProviderFile video) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("video-hls-" + job.id() + "-");
            Path source = directory.resolve("source.mp4");
            Path playlist = directory.resolve("index.m3u8");
            Path output = directory.resolve("ffmpeg.log");
            Files.write(source, video.content());
            Process process = new ProcessBuilder(properties.getProviders().getPostProduction().getFfmpegPath(),
                    "-v", "error", "-y", "-i", source.toString(), "-map", "0:v:0", "-map", "0:a:0",
                    "-c", "copy", "-hls_time", "4", "-hls_playlist_type", "vod", "-hls_flags", "independent_segments",
                    "-hls_segment_filename", directory.resolve("segment-%03d.ts").toString(), playlist.toString())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Tempo limite do empacotamento HLS excedido.");
            }
            if (process.exitValue() != 0 || !Files.exists(playlist)) {
                throw new IllegalStateException("FFmpeg recusou HLS: " + Files.readString(output));
            }
            List<String> lines = Files.readAllLines(playlist);
            if (!lines.contains("#EXTM3U") || !lines.contains("#EXT-X-ENDLIST")) {
                throw new IllegalStateException("Manifesto HLS incompleto.");
            }
            List<Long> segments = new ArrayList<>();
            List<String> published = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    published.add(line);
                    continue;
                }
                if (!line.matches("segment-[0-9]{3,}\\.ts") || segments.size() >= 150) {
                    throw new IllegalStateException("Referência HLS inválida: " + line);
                }
                byte[] bytes = Files.readAllBytes(directory.resolve(line));
                if (bytes.length < 188 || bytes[0] != 0x47) {
                    throw new IllegalStateException("Segmento MPEG-TS inválido.");
                }
                AssetResponse stored = upload(job, "sales-video-" + job.id() + "-" + line,
                        "video/mp2t", "HLS_SEGMENT", bytes);
                segments.add(stored.id());
                published.add(stored.url());
            }
            if (segments.isEmpty()) throw new IllegalStateException("HLS sem segmentos.");
            byte[] manifest = (String.join("\n", published) + "\n").getBytes(StandardCharsets.UTF_8);
            AssetResponse stored = upload(job, "sales-video-" + job.id() + "-index.m3u8",
                    "application/vnd.apple.mpegurl", "HLS_PLAYLIST", manifest);
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("contractVersion", "VIDEO_HLS_DELIVERY_V1");
            audit.put("status", "READY");
            audit.put("sourceSha256", sha256(video.content()));
            audit.put("playlistAssetId", stored.id());
            audit.put("playlistSha256", sha256(manifest));
            audit.put("segmentAssetIds", segments);
            audit.put("encoding", "COPY_EXISTING_AUDIO_VIDEO");
            log.info("Entrega HLS registrada; jobId={} playlistAssetId={} segmentCount={}",
                    job.id(), stored.id(), segments.size());
            return new Delivery(stored.url(), audit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Entrega HLS interrompida; jobId={}", job.id(), ex);
            throw new VideoProviderException("VIDEO_HLS_DELIVERY_FAILED", "Entrega HLS interrompida", ex);
        } catch (Exception ex) {
            log.error("Entrega HLS falhou; jobId={} profileId={}", job.id(), job.profileId(), ex);
            throw new VideoProviderException("VIDEO_HLS_DELIVERY_FAILED", "Falha ao preparar HLS", ex);
        } finally {
            cleanup(directory);
        }
    }

    /** Registra conteúdo, hash e correlação sem acessar diretamente o bucket ou banco. */
    private AssetResponse upload(SalesVideoJob job, String name, String type, String role, byte[] bytes) throws Exception {
        String metadata = mapper.writeValueAsString(Map.of("job_id", job.id(), "profile_id", job.profileId(),
                "tenant_id", job.tenantId(), "role", role, "file_name", name, "size_bytes", bytes.length,
                "sha256", sha256(bytes)));
        AssetResponse response = client.uploadAsset(new ProviderFile(name, MediaType.valueOf(type),
                AssetType.VIDEO, ProviderAssetRole.VIDEO, bytes), metadata);
        if (response == null || response.id() == null || response.url() == null
                || !response.url().matches("https?://[^\\s]+")) {
            throw new IllegalStateException("Upload HLS não retornou asset e URL válidos.");
        }
        return response;
    }

    /** Calcula identidade verificável dos bytes entregues. */
    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    /** Remove somente os temporários desta execução, preservando a falha original. */
    private void cleanup(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (Exception ex) {
            log.warn("Não foi possível limpar temporários HLS; directory={}", directory, ex);
        }
    }

    /** Transporta o manifesto persistido e a auditoria de entrega ao callback do job. */
    public record Delivery(String playbackUrl, Map<String, Object> audit) {}
}
