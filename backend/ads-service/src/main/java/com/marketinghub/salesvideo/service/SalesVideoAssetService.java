package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Serviço responsável por armazenar arquivos do módulo de vídeo e registrá-los como {@link Asset}.
 */
@Component
public class SalesVideoAssetService {
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final AssetStorageService storageService;
  private final AssetRepository assetRepository;
  private final ObjectMapper objectMapper;

  public SalesVideoAssetService(
      AssetStorageService storageService,
      AssetRepository assetRepository,
      ObjectMapper objectMapper) {
    this.storageService = storageService;
    this.assetRepository = assetRepository;
    this.objectMapper = objectMapper;
  }

  /** Armazena asset de vídeo usando R2 obrigatório e registra o metadado comercial do asset. */
  public Asset store(
      MultipartFile file, AssetType assetType, MediaProvider provider, String metadataJson)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Arquivo obrigatório para upload de asset");
    }
    String sha256 = sha256(file);
    AssetUploadContext context =
        new AssetUploadContext(AssetUploadCategory.SALES_VIDEO, null, null, null);
    AssetStorageService.StoredObject storedObject = storageService.storeInBucketOnly(file, context);
    Asset asset =
        Asset.builder()
            .type(assetType != null ? assetType : AssetType.VIDEO)
            .provider(provider != null ? provider : MediaProvider.VIDEO_MODULE)
            .status(AssetStatus.READY)
            .url(storedObject.publicUrl())
            .externalId(storedObject.storedFileName())
            .payload(buildPayload(storedObject, metadataJson, sha256))
            .build();
    return assetRepository.save(asset);
  }

  /** Monta o payload auditável do asset gravado no R2. */
  private String buildPayload(
      AssetStorageService.StoredObject storedObject, String metadataJson, String sha256) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("category", AssetUploadCategory.SALES_VIDEO.name());
    payload.put("stored_file_name", storedObject.storedFileName());
    payload.put("public_url", storedObject.publicUrl());
    payload.put("storage_medium", storedObject.storedInBucket() ? "CLOUDFLARE_R2" : "LOCAL_FS");
    payload.put("content_type", storedObject.contentType());
    payload.put("size_bytes", storedObject.sizeBytes());
    Map<String, Object> metadata =
        StringUtils.hasText(metadataJson)
            ? new LinkedHashMap<>(parseMetadata(metadataJson))
            : new LinkedHashMap<>();
    metadata.put("sha256", sha256);
    payload.put("metadata", metadata);
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new VideoModuleException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
          VideoModuleErrorCode.INTERNAL_ERROR,
          "Falha ao serializar metadata de asset",
          ex);
    }
  }

  /** Calcula a identidade imutável do arquivo antes de enviá-lo ao storage. */
  private String sha256(MultipartFile file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = file.getInputStream()) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
          if (read > 0) {
            digest.update(buffer, 0, read);
          }
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
  }

  /** Converte metadata JSON recebido do módulo de vídeo em mapa estruturado. */
  private Map<String, Object> parseMetadata(String metadataJson) {
    try {
      return objectMapper.readValue(metadataJson, MAP_TYPE);
    } catch (JsonProcessingException ex) {
      throw new VideoModuleException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          VideoModuleErrorCode.BAD_REQUEST,
          "metadata inválido, use JSON",
          ex);
    }
  }
}
