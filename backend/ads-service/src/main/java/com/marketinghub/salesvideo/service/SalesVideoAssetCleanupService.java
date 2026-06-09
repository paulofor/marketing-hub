package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.storage.AssetStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executa a limpeza de assets órfãos gerados pelo módulo de vídeo.
 */
@Component
public class SalesVideoAssetCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesVideoAssetCleanupService.class);
    private static final String SELECT_ORPHANS = """
            SELECT id, payload
            FROM asset
            WHERE provider = :provider
              AND created_at < :cutoff
            ORDER BY created_at ASC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AssetRepository assetRepository;
    private final SalesVideoJobRepository jobRepository;
    private final LandingVideoSlotRepository slotRepository;
    private final AssetStorageService storageService;
    private final ObjectMapper objectMapper;

    public SalesVideoAssetCleanupService(NamedParameterJdbcTemplate jdbcTemplate,
                                         AssetRepository assetRepository,
                                         SalesVideoJobRepository jobRepository,
                                         LandingVideoSlotRepository slotRepository,
                                         AssetStorageService storageService,
                                         ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetRepository = assetRepository;
        this.jobRepository = jobRepository;
        this.slotRepository = slotRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public int cleanup(Duration retention, int batchSize) {
        Instant cutoff = Instant.now().minus(retention);
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(SELECT_ORPHANS,
                new MapSqlParameterSource()
                        .addValue("provider", "VIDEO_MODULE")
                        .addValue("cutoff", cutoff)
                        .addValue("limit", batchSize));
        if (CollectionUtils.isEmpty(candidates)) {
            return 0;
        }
        int removed = 0;
        for (Map<String, Object> row : candidates) {
            Long assetId = ((Number) row.get("id")).longValue();
            if (jobRepository.existsByAnyAssetReference(assetId) || slotRepository.existsByAnyAssetReference(assetId)) {
                continue;
            }
            Optional<Asset> assetOptional = assetRepository.findById(assetId);
            if (assetOptional.isEmpty()) {
                continue;
            }
            Asset asset = assetOptional.get();
            deleteStoredObject(asset);
            assetRepository.delete(asset);
            removed++;
        }
        return removed;
    }

    private void deleteStoredObject(Asset asset) {
        try {
            JsonNode payload = asset.getPayload() != null
                    ? objectMapper.readTree(asset.getPayload())
                    : null;
            if (payload == null) {
                return;
            }
            String storedFile = payload.path("stored_file_name").asText(null);
            boolean storedInBucket = "CLOUDFLARE_R2".equalsIgnoreCase(payload.path("storage_medium").asText());
            if (storedFile != null) {
                storageService.deleteStoredObject(storedFile, storedInBucket);
            }
        } catch (Exception ex) {
            LOGGER.warn("Falha ao remover objeto físico do asset {}", asset.getId(), ex);
        }
    }
}
