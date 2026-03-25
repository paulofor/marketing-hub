package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serviço responsável por armazenar arquivos do módulo de vídeo e registrá-los como {@link Asset}.
 */
@Service
public class SalesVideoAssetService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AssetStorageService storageService;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    public SalesVideoAssetService(AssetStorageService storageService,
                                  AssetRepository assetRepository,
                                  ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.assetRepository = assetRepository;
        this.objectMapper = objectMapper;
    }

    public Asset store(MultipartFile file,
                       AssetType assetType,
                       MediaProvider provider,
                       String metadataJson) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Arquivo obrigatório para upload de asset");
        }
        AssetUploadContext context = new AssetUploadContext(AssetUploadCategory.SALES_VIDEO,
                null,
                null,
                null);
        AssetStorageService.StoredObject storedObject = storageService.store(file, context);
        Asset asset = Asset.builder()
                .type(assetType != null ? assetType : AssetType.VIDEO)
                .provider(provider != null ? provider : MediaProvider.VIDEO_MODULE)
                .status(AssetStatus.READY)
                .url(storedObject.publicUrl())
                .externalId(storedObject.storedFileName())
                .payload(buildPayload(storedObject, metadataJson))
                .build();
        return assetRepository.save(asset);
    }

    private String buildPayload(AssetStorageService.StoredObject storedObject,
                                String metadataJson) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("category", AssetUploadCategory.SALES_VIDEO.name());
        payload.put("stored_file_name", storedObject.storedFileName());
        payload.put("public_url", storedObject.publicUrl());
        payload.put("storage_medium", storedObject.storedInBucket() ? "CLOUDFLARE_R2" : "LOCAL_FS");
        payload.put("content_type", storedObject.contentType());
        payload.put("size_bytes", storedObject.sizeBytes());
        if (StringUtils.hasText(metadataJson)) {
            payload.put("metadata", parseMetadata(metadataJson));
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao serializar metadata de asset", ex);
        }
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "metadata inválido, use JSON", ex);
        }
    }
}
