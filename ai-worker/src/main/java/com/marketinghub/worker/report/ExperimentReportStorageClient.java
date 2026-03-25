package com.marketinghub.worker.report;

import com.marketinghub.worker.leadportal.image.LeadPortalStorageProperties;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cliente responsável por salvar o arquivo final do relatório no bucket compartilhado do Marketing Hub.
 */
@Component
public class ExperimentReportStorageClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentReportStorageClient.class);

    private final LeadPortalStorageProperties storageProperties;
    private final ExperimentReportProperties reportProperties;
    private final S3Client s3Client;

    public ExperimentReportStorageClient(LeadPortalStorageProperties storageProperties,
                                         ExperimentReportProperties reportProperties,
                                         @Qualifier("leadPortalS3Client") S3Client s3Client) {
        this.storageProperties = storageProperties;
        this.reportProperties = reportProperties;
        this.s3Client = s3Client;
    }

    public StoredReport upload(byte[] content, String filename, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Conteúdo do relatório não pode ser vazio");
        }
        if (!StringUtils.hasText(storageProperties.getBucket())) {
            throw new IllegalStateException("Bucket de storage não configurado (lead-portal.storage.bucket)");
        }
        String key = buildObjectKey(filename);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .contentType(StringUtils.hasText(contentType) ? contentType : "text/html")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        String publicUrl = resolvePublicUrl(key);
        log.info("Relatório armazenado em {} ({} bytes)", key, content.length);
        return new StoredReport(key, publicUrl);
    }

    private String buildObjectKey(String filename) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String prefix = StringUtils.hasText(reportProperties.getStoragePrefix())
                ? reportProperties.getStoragePrefix().trim()
                : "reports";
        String safeFilename = sanitize(filename);
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return String.format(Locale.ROOT,
                "%s/%d/%02d/%02d/%s-%s",
                prefix,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                unique,
                safeFilename);
    }

    private String sanitize(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "experiment-report.html";
        }
        return filename.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\.\\-]", "-");
    }

    private String resolvePublicUrl(String objectKey) {
        if (!StringUtils.hasText(storageProperties.getPublicBaseUrl())) {
            throw new IllegalStateException("lead-portal.storage.public-base-url não configurado para servir relatórios");
        }
        String base = storageProperties.getPublicBaseUrl().endsWith("/")
                ? storageProperties.getPublicBaseUrl().substring(0, storageProperties.getPublicBaseUrl().length() - 1)
                : storageProperties.getPublicBaseUrl();
        return base + "/" + objectKey;
    }

    public record StoredReport(String objectKey, String publicUrl) {}
}
