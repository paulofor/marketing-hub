package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Armazena arquivos finais de vídeo no R2 e devolve somente a URL pública.
 */
@Service
public class VideoR2StorageService {
    private final VideoManagementProperties properties;
    private final S3Client s3Client;

    /** Inicializa o serviço com as propriedades externas e o client S3 compatível com R2. */
    public VideoR2StorageService(VideoManagementProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    /** Envia um arquivo do provider para o R2 e retorna sua URL pública. */
    public StoredVideoAsset store(Long jobId, ProviderFile file) {
        if (file == null) {
            return null;
        }
        if (StringUtils.hasText(file.externalUrl())) {
            return new StoredVideoAsset(file.externalUrl().trim(), null, 0L, mediaType(file));
        }
        if (file.content() == null || file.content().length == 0) {
            throw new IllegalArgumentException("Arquivo de vídeo sem conteúdo e sem URL externa.");
        }
        VideoManagementProperties.Storage storage = properties.getStorage();
        ensureConfigured(storage);
        String objectKey = buildObjectKey(storage, jobId, file);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storage.getBucket())
                .key(objectKey)
                .contentType(mediaType(file))
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.content()));
        } catch (SdkException ex) {
            throw new IllegalStateException("Falha ao gravar vídeo no R2.", ex);
        }
        return new StoredVideoAsset(buildPublicUrl(storage, objectKey), objectKey, file.content().length, mediaType(file));
    }

    /** Garante que o storage foi configurado antes de processar vídeo real. */
    private void ensureConfigured(VideoManagementProperties.Storage storage) {
        if (!StringUtils.hasText(storage.getBucket())
                || storage.getEndpoint() == null
                || !StringUtils.hasText(storage.getPublicBaseUrl())
                || !StringUtils.hasText(storage.getAccessKeyId())
                || !StringUtils.hasText(storage.getSecretAccessKey())) {
            throw new IllegalStateException("Storage R2 do video-management-service não está configurado.");
        }
    }

    /** Monta uma chave estável por data e job para armazenar o arquivo final. */
    private String buildObjectKey(VideoManagementProperties.Storage storage, Long jobId, ProviderFile file) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String root = StringUtils.hasText(storage.getRootFolder()) ? storage.getRootFolder() : "sales-videos";
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "%s/%d/%02d/%02d/job-%s/%s-%s".formatted(
                root,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                jobId,
                unique,
                sanitize(file.fileName()));
    }

    /** Monta a URL pública final a partir da chave salva no bucket. */
    private String buildPublicUrl(VideoManagementProperties.Storage storage, String objectKey) {
        String base = storage.getPublicBaseUrl().endsWith("/")
                ? storage.getPublicBaseUrl().substring(0, storage.getPublicBaseUrl().length() - 1)
                : storage.getPublicBaseUrl();
        return base + "/" + objectKey;
    }

    /** Normaliza o nome do arquivo para evitar caracteres problemáticos no storage. */
    private String sanitize(String fileName) {
        String candidate = StringUtils.hasText(fileName) ? fileName : "asset.bin";
        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "asset.bin" : normalized;
    }

    /** Extrai o media type textual do arquivo. */
    private String mediaType(ProviderFile file) {
        return file.mediaType() != null ? file.mediaType().toString() : "application/octet-stream";
    }

    /**
     * Referência pública e metadados técnicos do arquivo gravado no R2.
     */
    public record StoredVideoAsset(String publicUrl, String objectKey, long sizeBytes, String contentType) {
    }
}
