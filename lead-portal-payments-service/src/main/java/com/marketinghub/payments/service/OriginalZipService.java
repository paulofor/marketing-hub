package com.marketinghub.payments.service;

import com.marketinghub.payments.config.LeadPortalStorageProperties;
import com.marketinghub.payments.dto.OriginalAsset;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OriginalZipService {

    private static final Logger log = LoggerFactory.getLogger(OriginalZipService.class);

    private final StorageService storageService;
    private final LeadPortalStorageProperties storageProperties;

    public OriginalZipService(StorageService storageService, LeadPortalStorageProperties storageProperties) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    public GeneratedZip buildAndStoreZip(long packageId, List<OriginalAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalStateException("Nenhuma imagem encontrada para o pacote " + packageId);
        }
        byte[] zipBytes = createZipBytes(packageId, assets);
        String key = buildObjectKey(packageId);
        storageService.upload(key, zipBytes, "application/zip");
        log.info("Arquivo ZIP de originais gravado em {} ({} bytes)", key, zipBytes.length);
        return new GeneratedZip(key, zipBytes.length, Instant.now());
    }

    private byte[] createZipBytes(long packageId, List<OriginalAsset> assets) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            int index = 1;
            for (OriginalAsset asset : assets) {
                if (!StringUtils.hasText(asset.objectKey())) {
                    continue;
                }
                byte[] content = storageService.download(asset.objectKey());
                String fileName = buildFileName(asset, index++);
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao gerar arquivo zip de originais", ex);
        }
    }

    private String buildFileName(OriginalAsset asset, int fallbackIndex) {
        String raw = asset.objectKey();
        if (StringUtils.hasText(raw)) {
            int slash = raw.lastIndexOf('/') + 1;
            if (slash > 0 && slash < raw.length()) {
                return raw.substring(slash);
            }
            return raw;
        }
        String extension = "png";
        if (StringUtils.hasText(asset.contentType()) && asset.contentType().contains("jpeg")) {
            extension = "jpg";
        }
        return "imagem-" + fallbackIndex + "." + extension;
    }

    private String buildObjectKey(long packageId) {
        String prefix = storageProperties.getOriginalsPrefix();
        String normalized = (prefix == null ? "" : prefix).replaceAll("^/+", "").replaceAll("/+\\z", "");
        if (!normalized.isBlank()) {
            normalized = normalized + "/";
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        return normalized + "lead-portal/package-" + packageId + "/originals-" + token + ".zip";
    }

    public record GeneratedZip(String objectKey, long sizeBytes, Instant generatedAt) {}
}
