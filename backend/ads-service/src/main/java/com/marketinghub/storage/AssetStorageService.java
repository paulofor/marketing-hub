package com.marketinghub.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Serviço responsável por enviar arquivos para o bucket do Cloudflare R2 utilizado pelo Marketing Hub.
 */
@Service
public class AssetStorageService {

    private static final Logger log = LoggerFactory.getLogger(AssetStorageService.class);

    private final StorageProperties properties;
    private final S3Client s3Client;

    public AssetStorageService(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public StoredObject store(MultipartFile file, AssetUploadContext context) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File must not be empty");
        }
        AssetUploadContext safeContext = context != null ? context : new AssetUploadContext(null, null, null, null);
        String objectKey = buildObjectKey(file, safeContext);
        String contentType = resolveContentType(file);
        long sizeBytes = file.getSize();

        if (isCloudStorageReady()) {
            uploadToBucket(file, objectKey, contentType, sizeBytes);
            String publicUrl = buildPublicUrl(objectKey);
            return new StoredObject(objectKey, publicUrl, sizeBytes, contentType, true);
        }

        Path localPath = storeLocally(file, objectKey);
        String publicUrl = "/" + localPath.toString().replace('\\', '/');
        log.warn("Cloud storage is not fully configured; stored asset '{}' locally at {}", objectKey, localPath);
        return new StoredObject(objectKey, publicUrl, sizeBytes, contentType, false);
    }

    private boolean isCloudStorageReady() {
        return !isBlank(properties.getBucket())
                && !isBlank(properties.getEndpoint())
                && !isBlank(properties.getAccessKeyId())
                && !isBlank(properties.getSecretAccessKey());
    }

    private void uploadToBucket(MultipartFile file, String objectKey, String contentType, long sizeBytes) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        try (InputStream input = file.getInputStream()) {
            long length = sizeBytes >= 0 ? sizeBytes : file.getBytes().length;
            s3Client.putObject(request, RequestBody.fromInputStream(input, length));
            log.info("Uploaded asset '{}' ({} bytes) to bucket '{}'", objectKey, length, properties.getBucket());
        } catch (SdkException ex) {
            throw new StorageException("Failed to upload file to bucket", ex);
        }
    }

    private Path storeLocally(MultipartFile file, String objectKey) throws IOException {
        Path baseDir = Path.of("uploads");
        Path destination = baseDir.resolve(objectKey);
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private String buildObjectKey(MultipartFile file, AssetUploadContext context) {
        AssetUploadCategory category = context.category();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String baseName = sanitizeBaseName(file.getOriginalFilename());
        String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
        String identifier = buildIdentifier(category, context);
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return String.format(Locale.ROOT,
                "%s/%d/%02d/%02d/%s/%s-%s.%s",
                category.getRootFolder(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                identifier,
                unique,
                baseName,
                extension);
    }

    private String buildIdentifier(AssetUploadCategory category, AssetUploadContext context) {
        return switch (category) {
            case EXPERIMENT_CREATIVE -> context.experimentId() != null
                    ? "exp-" + context.experimentId()
                    : "exp-generic";
            case LEAD_PORTAL_FORM -> {
                if (context.flowId() != null) {
                    yield "flow-" + context.flowId();
                }
                if (StringUtils.hasText(context.flowSlug())) {
                    yield "flow-" + slugify(context.flowSlug());
                }
                yield "flow-generic";
            }
            default -> "misc";
        };
    }

    private String sanitizeBaseName(String originalFilename) {
        String candidate = StringUtils.hasText(originalFilename)
                ? StringUtils.getFilename(originalFilename)
                : "asset";
        String base = StringUtils.stripFilenameExtension(candidate != null ? candidate : "asset");
        if (!StringUtils.hasText(base)) {
            base = "asset";
        }
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (normalized.isBlank()) {
            normalized = "asset";
        }
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private String resolveExtension(String contentType, String originalFilename) {
        String ext = StringUtils.getFilenameExtension(originalFilename);
        if (StringUtils.hasText(ext)) {
            return ext.toLowerCase(Locale.ROOT);
        }
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("png")) {
                return "png";
            }
            if (lower.contains("jpeg") || lower.contains("jpg")) {
                return "jpg";
            }
            if (lower.contains("webp")) {
                return "webp";
            }
        }
        return "bin";
    }

    private String resolveContentType(MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private String buildPublicUrl(String objectKey) {
        String base = properties.getPublicBaseUrl();
        if (isBlank(base)) {
            throw new StorageException("lead-portal.storage.public-base-url must be configured to expose assets");
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + "/" + objectKey;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record StoredObject(String storedFileName,
                               String publicUrl,
                               long sizeBytes,
                               String contentType,
                               boolean storedInBucket) {}

    public void deleteStoredObject(String storedFileName, boolean storedInBucket) {
        if (!StringUtils.hasText(storedFileName)) {
            return;
        }
        if (storedInBucket && isCloudStorageReady()) {
            try {
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest request =
                        software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                                .bucket(properties.getBucket())
                                .key(storedFileName)
                                .build();
                s3Client.deleteObject(request);
            } catch (SdkException ex) {
                throw new StorageException("Falha ao remover objeto do bucket", ex);
            }
        } else {
            Path target = Path.of("uploads").resolve(storedFileName);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                throw new StorageException("Falha ao remover arquivo local", ex);
            }
        }
    }

}
