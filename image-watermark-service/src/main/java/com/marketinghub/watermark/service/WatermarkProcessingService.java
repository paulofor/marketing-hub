package com.marketinghub.watermark.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.watermark.config.WatermarkProperties;
import com.marketinghub.watermark.entity.AssetEntity;
import com.marketinghub.watermark.entity.AssetStatus;
import com.marketinghub.watermark.entity.AssetType;
import com.marketinghub.watermark.entity.FlowSubmissionImageItemEntity;
import com.marketinghub.watermark.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.watermark.entity.FlowSubmissionImagePackageEntity.Status;
import com.marketinghub.watermark.entity.FlowSubmissionImageWatermarkEntity;
import com.marketinghub.watermark.entity.MediaProvider;
import com.marketinghub.watermark.repository.AssetRepository;
import com.marketinghub.watermark.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.watermark.repository.FlowSubmissionImageWatermarkRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WatermarkProcessingService {

    private static final Logger log = LoggerFactory.getLogger(WatermarkProcessingService.class);

    private final FlowSubmissionImagePackageRepository packageRepository;
    private final AssetRepository assetRepository;
    private final FlowSubmissionImageWatermarkRepository watermarkRepository;
    private final StorageService storageService;
    private final WatermarkRenderer watermarkRenderer;
    private final WatermarkKeyResolver keyResolver;
    private final WatermarkProperties watermarkProperties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public WatermarkProcessingService(
            FlowSubmissionImagePackageRepository packageRepository,
            AssetRepository assetRepository,
            FlowSubmissionImageWatermarkRepository watermarkRepository,
            StorageService storageService,
            WatermarkRenderer watermarkRenderer,
            WatermarkKeyResolver keyResolver,
            WatermarkProperties watermarkProperties,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.packageRepository = packageRepository;
        this.assetRepository = assetRepository;
        this.watermarkRepository = watermarkRepository;
        this.storageService = storageService;
        this.watermarkRenderer = watermarkRenderer;
        this.keyResolver = keyResolver;
        this.watermarkProperties = watermarkProperties;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void processPendingPackages() {
        int batchSize = Math.max(1, watermarkProperties.getBatchSize());
        List<Long> packageIds = packageRepository
                .findIdsByStatus(Status.WATERMARK_PENDING, PageRequest.of(0, batchSize));
        if (packageIds.isEmpty()) {
            return;
        }

        for (Long packageId : packageIds) {
            Boolean locked = transactionTemplate.execute(status ->
                    packageRepository.updateStatus(packageId, Status.WATERMARK_PENDING, Status.WATERMARKING, null) > 0);
            if (locked == null || !locked) {
                continue;
            }
            try {
                transactionTemplate.executeWithoutResult(status -> applyWatermarkInternal(packageId));
                transactionTemplate.executeWithoutResult(status ->
                        packageRepository.updateStatus(packageId, Status.WATERMARKING, Status.COMPLETED, null));
                log.info("Pacote {} marcado como COMPLETED após geração de marca d'água", packageId);
            } catch (Exception ex) {
                String reason = "Marca d'água: " + ex.getMessage();
                transactionTemplate.executeWithoutResult(status ->
                        packageRepository.updateStatus(packageId, Status.WATERMARKING, Status.FAILED, reason));
                log.error("Falha ao aplicar marca d'água no pacote {}", packageId, ex);
            }
        }
    }

    protected void applyWatermarkInternal(Long packageId) {
        FlowSubmissionImagePackageEntity imagePackage = packageRepository
                .findDetailedById(packageId)
                .orElseThrow(() -> new IllegalStateException("Pacote %d não encontrado".formatted(packageId)));

        List<FlowSubmissionImageItemEntity> items = imagePackage.getItems().stream()
                .sorted(Comparator.comparing((FlowSubmissionImageItemEntity item) ->
                        item.getPositionIndex() == null ? Integer.MAX_VALUE : item.getPositionIndex()))
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            throw new IllegalStateException("Pacote %d não possui imagens geradas para aplicar marca d'água".formatted(packageId));
        }

        for (FlowSubmissionImageItemEntity item : items) {
            if (item.getWatermark() != null) {
                continue;
            }
            AssetEntity sourceAsset = item.getAsset();
            if (sourceAsset == null || sourceAsset.getUrl() == null || sourceAsset.getUrl().isBlank()) {
                throw new IllegalStateException("Item %d não possui asset associado".formatted(item.getId()));
            }

            byte[] originalBytes = storageService.download(sourceAsset.getUrl());
            WatermarkRenderer.WatermarkedImage watermarkedImage = watermarkRenderer.applyWatermark(originalBytes);
            String outputKey = keyResolver.buildKey(packageId, item.getId(), watermarkedImage.extension());
            storageService.upload(outputKey, watermarkedImage.bytes(), watermarkedImage.contentType());

            AssetEntity watermarkAsset = new AssetEntity();
            watermarkAsset.setType(AssetType.IMAGE);
            watermarkAsset.setProvider(MediaProvider.WATERMARKER);
            watermarkAsset.setStatus(AssetStatus.READY);
            watermarkAsset.setUrl(outputKey);
            watermarkAsset.setExternalId(outputKey);
            watermarkAsset.setModel(sourceAsset.getModel());
            watermarkAsset.setPrompt(sourceAsset.getPrompt());
            watermarkAsset.setPayload(buildPayload(sourceAsset));
            assetRepository.save(watermarkAsset);

            FlowSubmissionImageWatermarkEntity watermarkEntity = new FlowSubmissionImageWatermarkEntity();
            watermarkEntity.setItem(item);
            watermarkEntity.setAsset(watermarkAsset);
            watermarkRepository.save(watermarkEntity);

            item.setWatermark(watermarkEntity);
        }
    }

    private String buildPayload(AssetEntity sourceAsset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceAssetId", sourceAsset.getId());
        payload.put("sourceUrl", sourceAsset.getUrl());
        payload.put("watermarkText", watermarkProperties.getText());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Não foi possível serializar metadados de marca d'água", ex);
            return null;
        }
    }
}
