package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetResponse;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Prepara e envia os artefatos auditáveis produzidos por cada job de vídeo. */
@Component
public class VideoAssetUploader {
    private final VideoAssetClient assetClient;
    private final ObjectMapper objectMapper;
    private final VideoManagementProperties properties;

    /** Inicializa o publicador com o ffmpeg configurado para gerar quadros de continuidade. */
    @Autowired
    public VideoAssetUploader(VideoAssetClient assetClient,
                              ObjectMapper objectMapper,
                              VideoManagementProperties properties) {
        this.assetClient = assetClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Inicializa o publicador em testes unitários que não exercitam extração real. */
    VideoAssetUploader(VideoAssetClient assetClient, ObjectMapper objectMapper) {
        this(assetClient, objectMapper, new VideoManagementProperties());
    }

    /** Envia vídeo, quadro final e legenda, criando a ponte visual quando o provider não a entrega. */
    public UploadedAssets uploadAssets(SalesVideoJob job, ProviderArtifacts artifacts) {
        Long videoAssetId = upload(job, artifacts, artifacts.videoFile());
        ProviderFile continuityFrame = isCinematicScene(job)
                ? extractContinuityFrame(job, artifacts.videoFile())
                : artifacts.posterFile() != null
                        ? artifacts.posterFile()
                        : extractContinuityFrame(job, artifacts.videoFile());
        Long posterAssetId = upload(job, artifacts, continuityFrame);
        Long captionAssetId = upload(job, artifacts, artifacts.captionFile());
        return new UploadedAssets(videoAssetId, posterAssetId, captionAssetId);
    }

    /** Identifica cenas que exigem que o poster seja exatamente o último quadro renderizado. */
    private boolean isCinematicScene(SalesVideoJob job) {
        return job.metadataJson() != null
                && job.metadataJson().contains("\"generation_strategy\":\"SCENE_BY_SCENE_MONTAGE\"");
    }

    /** Envia um artefato individual preservando seus metadados de origem. */
    private Long upload(SalesVideoJob job,
                        ProviderArtifacts artifacts,
                        ProviderFile file) {
        if (file == null) {
            return null;
        }
        String metadataJson = serializeMetadata(job, artifacts, file);
        AssetResponse response = assetClient.uploadAsset(file, metadataJson);
        return response != null ? response.id() : null;
    }

    /** Serializa os metadados auditáveis do artefato. */
    private String serializeMetadata(SalesVideoJob job,
                                     ProviderArtifacts artifacts,
                                     ProviderFile file) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("job_id", job.id());
        metadata.put("profile_id", job.profileId());
        metadata.put("script_id", job.scriptId());
        metadata.put("role", file.role().name());
        metadata.put("provider_job_id", artifacts.providerJobId());
        metadata.put("file_name", file.fileName());
        if (!CollectionUtils.isEmpty(artifacts.metadata())) {
            metadata.put("provider_metadata", artifacts.metadata());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new BackendIntegrationException("Não foi possível serializar metadata do asset", ex);
        }
    }

    /** Extrai o último quadro do vídeo para servir de abertura do plano seguinte. */
    private ProviderFile extractContinuityFrame(SalesVideoJob job, ProviderFile videoFile) {
        Path source = null;
        Path frame = null;
        try {
            source = Files.createTempFile("video-continuity-" + job.id(), ".mp4");
            frame = Files.createTempFile("video-continuity-" + job.id(), ".png");
            Files.write(source, videoFile.content());
            String ffmpeg = properties.getProviders().getPostProduction().getFfmpegPath();
            Process process = new ProcessBuilder(
                    ffmpeg, "-y", "-sseof", "-0.12", "-i", source.toString(),
                    "-frames:v", "1", "-vf", "scale=720:1280:force_original_aspect_ratio=increase,crop=720:1280",
                    frame.toString())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0 || Files.size(frame) == 0) {
                throw new VideoProviderException("VIDEO_CONTINUITY_FRAME_FAILED",
                        "ffmpeg não extraiu o quadro final; exitCode=" + exitCode + "; output=" + output);
            }
            return new ProviderFile(
                    "sales-video-" + job.id() + "-continuity-frame.png",
                    org.springframework.http.MediaType.IMAGE_PNG,
                    AssetType.IMAGE,
                    ProviderAssetRole.POSTER,
                    Files.readAllBytes(frame));
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_CONTINUITY_FRAME_FAILED",
                    "Falha ao extrair quadro final do job " + job.id(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("VIDEO_CONTINUITY_FRAME_FAILED",
                    "Extração do quadro final foi interrompida no job " + job.id(), ex);
        } finally {
            deleteIfExists(source);
            deleteIfExists(frame);
        }
    }

    /** Remove arquivo temporário sem esconder o resultado principal do job. */
    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // O diretório temporário do sistema fará a limpeza residual.
        }
    }

    /** Identifica os ativos persistidos pelo backend para concluir o job. */
    public record UploadedAssets(Long videoAssetId,
                                 Long posterAssetId,
                                 Long captionAssetId) {
    }
}
