package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImagePackageDto;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest.GeneratedImageRequest;
import com.marketinghub.media.Asset;
import com.marketinghub.imagegeneration.ImageGenerationPrice;
import com.marketinghub.imagegeneration.ImageOrientation;
import com.marketinghub.imagegeneration.service.ImageGenerationPricingService;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.repository.AssetRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Regras de negócio voltadas à comunicação com o Worker AI para pacotes de imagem do Lead Portal.
 */
@Service
public class LeadPortalImagePackageWorkerService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageWorkerService.class);

    private static final RowMapper<LeadPortalWorkerImagePackageDto> RECENT_PACKAGE_MAPPER = (rs, rowNum) ->
            new LeadPortalWorkerImagePackageDto(
                    rs.getLong("id"),
                    parseUuid(rs.getString("submission_id")),
                    rs.getString("stored_file_name"),
                    getInteger(rs, "planned_outputs"),
                    getInteger(rs, "free_images"),
                    rs.getString("model"),
                    rs.getString("prompt"),
                    rs.getString("treatment"),
                    getLong(rs, "resolved_image_model_id"),
                    getLong(rs, "resolved_image_model_quality_id"),
                    rs.getString("image_orientation"),
                    getInteger(rs, "image_width"),
                    getInteger(rs, "image_height"),
                    rs.getBigDecimal("image_unit_price_usd"),
                    rs.getBigDecimal("image_total_price_usd"),
                    rs.getString("image_currency"));

    private final JdbcTemplate jdbcTemplate;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;
    private final ImageGenerationPricingService pricingService;
    private final LeadPortalImagePackageStatusHistoryService statusHistoryService;

    public LeadPortalImagePackageWorkerService(
            JdbcTemplate jdbcTemplate,
            AssetRepository assetRepository,
            ObjectMapper objectMapper,
            ImageGenerationPricingService pricingService,
            LeadPortalImagePackageStatusHistoryService statusHistoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetRepository = assetRepository;
        this.objectMapper = objectMapper;
        this.pricingService = pricingService;
        this.statusHistoryService = statusHistoryService;
    }

    /**
     * Lista pacotes aguardando processamento pelo worker.
     */
    public List<LeadPortalWorkerImagePackageDto> listRecentPackages() {
        String sql = """
                SELECT
                    pack.id,
                    pack.submission_id,
                    sub.stored_file_name,
                    COALESCE(pack.planned_outputs, exp.images_per_package, 20) AS planned_outputs,
                    pack.free_images,
                    pack.model,
                    pack.prompt,
                    NULL AS treatment,
                    COALESCE(pack.image_model_id, exp.image_model_id) AS resolved_image_model_id,
                    COALESCE(pack.image_model_quality_id, exp.image_model_quality_id) AS resolved_image_model_quality_id,
                    pack.image_orientation,
                    pack.image_width,
                    pack.image_height,
                    pack.image_unit_price_usd,
                    pack.image_total_price_usd,
                    exp.unit_price_brl AS experiment_unit_price_brl,
                    pack.image_currency
                FROM flow_submission_image_package pack
                LEFT JOIN flow_submissions sub ON sub.id = pack.submission_id
                LEFT JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                WHERE pack.status IN ('RECENT', 'RECEIVED')
                ORDER BY pack.created_at DESC
                """;
        return jdbcTemplate.query(sql, RECENT_PACKAGE_MAPPER);
    }

    /**
     * Marca um pacote como em processamento.
     */
    @Transactional
    public void markProcessing(long packageId) {
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, failure_reason = NULL, updated_at = ? "
                        + "WHERE id = ? AND status IN (?, ?)",
                FlowSubmissionImagePackageStatus.PROCESSING.name(),
                Timestamp.from(now),
                packageId,
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                FlowSubmissionImagePackageStatus.RECENT.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> notFound(packageId));
            throw conflict("Pacote %d não está apto a entrar em processamento (status atual: %s)"
                    .formatted(packageId, current));
        } else {
            statusHistoryService.recordStatusChange(
                    packageId, FlowSubmissionImagePackageStatus.PROCESSING, null, now);
        }
    }

    /**
     * Marca um pacote como falho, registrando o motivo informado pelo worker.
     */
    @Transactional
    public void markFailed(long packageId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason não pode ser vazio");
        }
        String trimmedReason = reason.trim();
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, failure_reason = ?, updated_at = ? "
                        + "WHERE id = ? AND status IN (?, ?, ?)",
                FlowSubmissionImagePackageStatus.FAILED.name(),
                trimmedReason,
                Timestamp.from(now),
                packageId,
                FlowSubmissionImagePackageStatus.PROCESSING.name(),
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                FlowSubmissionImagePackageStatus.RECENT.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> notFound(packageId));
            throw conflict("Pacote %d não pode ser marcado como FAILED a partir do status %s"
                    .formatted(packageId, current));
        } else {
            statusHistoryService.recordStatusChange(
                    packageId, FlowSubmissionImagePackageStatus.FAILED, trimmedReason, now);
        }
    }

    /**
     * Reabre um pacote para reprocessamento quando o erro é potencialmente temporário.
     */
    @Transactional
    public void retry(long packageId, String reason) {
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, failure_reason = ?, updated_at = ? "
                        + "WHERE id = ? AND status IN (?, ?)",
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                normalizedReason,
                Timestamp.from(now),
                packageId,
                FlowSubmissionImagePackageStatus.PROCESSING.name(),
                FlowSubmissionImagePackageStatus.FAILED.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> notFound(packageId));
            throw conflict("Pacote %d não pode ser reprocessado a partir do status %s"
                    .formatted(packageId, current));
        } else {
            statusHistoryService.recordStatusChange(
                    packageId, FlowSubmissionImagePackageStatus.RECEIVED, normalizedReason, now);
        }
    }

    /**
     * Conclui o processamento de um pacote, persistindo as imagens geradas e metadados.
     */
    @Transactional
    public void submitResults(long packageId, LeadPortalWorkerImageResultRequest request) {
        PackageSnapshot snapshot = findPackage(packageId)
                .orElseThrow(() -> notFound(packageId));

        boolean recoveredAfterTimeout = shouldAcceptLateResults(packageId, snapshot.status());
        FlowSubmissionImagePackageStatus expectedStatus = FlowSubmissionImagePackageStatus.PROCESSING;
        if (snapshot.status() != FlowSubmissionImagePackageStatus.PROCESSING) {
            if (recoveredAfterTimeout) {
                expectedStatus = FlowSubmissionImagePackageStatus.RECEIVED;
                log.warn(
                        "Accepting late results for lead-portal image package {} after automatic recovery",
                        packageId);
            } else {
                throw conflict("Pacote %d precisa estar em PROCESSING para receber resultados (status atual: %s)"
                        .formatted(packageId, snapshot.status()));
            }
        }
        if (request.images().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário informar ao menos uma imagem gerada");
        }

        // Limpa resultados anteriores caso existam
        jdbcTemplate.update("DELETE FROM flow_submission_image_item WHERE package_id = ?", packageId);

        List<Asset> assets = saveAssets(request, snapshot);
        insertImageItems(packageId, assets, snapshot.freeImages());

        String finalModel = resolveModelFromAssets(request.model(), snapshot.model(), assets);
        String finalPrompt = resolvePrompt(request.prompt(), snapshot.prompt());

        GenerationMetadata generationMetadata = resolveGenerationMetadata(request);
        ImageOrientation orientation = generationMetadata.orientation();
        Integer imageWidth = generationMetadata.width();
        Integer imageHeight = generationMetadata.height();
        Long resolvedModelId = snapshot.imageModelId();
        Long resolvedQualityId = snapshot.imageModelQualityId();

        ImageGenerationPrice resolvedPrice = null;
        if (resolvedQualityId != null) {
            resolvedPrice = pricingService.resolvePrice(resolvedQualityId, orientation).orElse(null);
        }
        if (resolvedPrice != null) {
            resolvedQualityId = resolvedPrice.getQuality() != null ? resolvedPrice.getQuality().getId() : resolvedQualityId;
            if (resolvedModelId == null && resolvedPrice.getQuality() != null
                    && resolvedPrice.getQuality().getModel() != null) {
                resolvedModelId = resolvedPrice.getQuality().getModel().getId();
            }
        }

        java.math.BigDecimal unitPrice;
        if (snapshot.experimentUnitPriceBrl() != null) {
            unitPrice = snapshot.experimentUnitPriceBrl().setScale(2, java.math.RoundingMode.HALF_UP);
        } else if (resolvedPrice != null) {
            unitPrice = resolvedPrice.getUnitPriceUsd();
        } else {
            unitPrice = snapshot.imageUnitPriceUsd();
        }
        java.math.BigDecimal totalPrice = unitPrice != null
                ? unitPrice.setScale(unitPrice.scale(), java.math.RoundingMode.HALF_UP)
                : snapshot.imageTotalPriceUsd();
        String currency;
        if (snapshot.experimentUnitPriceBrl() != null) {
            currency = "BRL";
        } else if (StringUtils.hasText(snapshot.imageCurrency())) {
            currency = snapshot.imageCurrency().trim().toUpperCase(java.util.Locale.ROOT);
        } else {
            currency = "USD";
        }

        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, model = ?, prompt = ?, failure_reason = NULL, "
                        + "image_model_id = ?, image_model_quality_id = ?, image_orientation = ?, image_width = ?, image_height = ?, "
                        + "image_unit_price_usd = ?, image_total_price_usd = ?, image_currency = ?, updated_at = ? "
                        + "WHERE id = ? AND status = ?",
                FlowSubmissionImagePackageStatus.WATERMARK_PENDING.name(),
                finalModel,
                finalPrompt,
                resolvedModelId,
                resolvedQualityId,
                orientation != null ? orientation.name() : snapshot.imageOrientation(),
                imageWidth != null ? imageWidth : snapshot.imageWidth(),
                imageHeight != null ? imageHeight : snapshot.imageHeight(),
                unitPrice,
                totalPrice,
                currency,
                Timestamp.from(now),
                packageId,
                expectedStatus.name());
        if (updated == 0) {
            throw conflict("Não foi possível concluir o pacote %d porque seu status mudou durante o processamento".formatted(packageId));
        } else {
            statusHistoryService.recordStatusChange(
                    packageId, FlowSubmissionImagePackageStatus.WATERMARK_PENDING, null, now);
        }
    }

    private boolean shouldAcceptLateResults(long packageId, FlowSubmissionImagePackageStatus currentStatus) {
        if (currentStatus != FlowSubmissionImagePackageStatus.RECEIVED) {
            return false;
        }
        return statusHistoryService.hasProcessingAttempt(packageId);
    }

    private List<Asset> saveAssets(LeadPortalWorkerImageResultRequest request, PackageSnapshot snapshot) {
        List<Asset> assets = new ArrayList<>(request.images().size());
        for (GeneratedImageRequest image : request.images()) {
            String storedName = normalizeStoredFileName(image.storedFileName());
            String publicUrl = normalizePublicUrl(image.publicUrl());
            String prompt = resolvePrompt(
                    StringUtils.hasText(image.prompt()) ? image.prompt() : request.prompt(),
                    snapshot.prompt());
            String model = resolveModelValue(image.model(), request.model(), null);
            Asset asset = Asset.builder()
                    .type(AssetType.IMAGE)
                    .provider(resolveProvider(image.source()))
                    .status(AssetStatus.READY)
                    .url(storedName)
                    .externalId(storedName)
                    .model(model)
                    .prompt(prompt)
                    .payload(buildPayload(image, storedName, publicUrl))
                    .build();
            assets.add(asset);
        }
        return assetRepository.saveAll(assets);
    }

    private void insertImageItems(long packageId, List<Asset> assets, Integer freeImages) {
        int freeSlots = freeImages != null && freeImages > 0 ? freeImages : 0;
        String sql = "INSERT INTO flow_submission_image_item (package_id, asset_id, access_type, position_index, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        Timestamp now = Timestamp.from(Instant.now());
        for (int index = 0; index < assets.size(); index++) {
            Asset asset = assets.get(index);
            String accessType = index < freeSlots ? "FREE" : "PREMIUM";
            jdbcTemplate.update(sql, packageId, asset.getId(), accessType, index, now);
        }
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private static UUID parseUuid(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Valor de submission_id '{}' não pôde ser convertido para UUID", raw);
            return null;
        }
    }

    private Optional<FlowSubmissionImagePackageStatus> findStatus(long packageId) {
        List<FlowSubmissionImagePackageStatus> statuses = jdbcTemplate.query(
                "SELECT status FROM flow_submission_image_package WHERE id = ?",
                (rs, rowNum) -> parseStatus(rs.getString("status")),
                packageId);
        return statuses.stream().findFirst();
    }

    private Optional<PackageSnapshot> findPackage(long packageId) {
        String sql = """
                SELECT
                    pack.id,
                    pack.status,
                    pack.free_images,
                    pack.model,
                    pack.prompt,
                    COALESCE(pack.image_model_id, exp.image_model_id) AS resolved_image_model_id,
                    COALESCE(pack.image_model_quality_id, exp.image_model_quality_id) AS resolved_image_model_quality_id,
                    pack.image_orientation,
                    pack.image_width,
                    pack.image_height,
                    pack.image_unit_price_usd,
                    pack.image_total_price_usd,
                    exp.unit_price_brl AS experiment_unit_price_brl,
                    pack.image_currency
                FROM flow_submission_image_package pack
                LEFT JOIN flow_submissions sub ON sub.id = pack.submission_id
                LEFT JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                WHERE pack.id = ?
                """;
        List<PackageSnapshot> items = jdbcTemplate.query(sql, (rs, rowNum) -> new PackageSnapshot(
                rs.getLong("id"),
                parseStatus(rs.getString("status")),
                getInteger(rs, "free_images"),
                rs.getString("model"),
                rs.getString("prompt"),
                getLong(rs, "resolved_image_model_id"),
                getLong(rs, "resolved_image_model_quality_id"),
                rs.getString("image_orientation"),
                getInteger(rs, "image_width"),
                getInteger(rs, "image_height"),
                rs.getBigDecimal("image_unit_price_usd"),
                rs.getBigDecimal("image_total_price_usd"),
                rs.getBigDecimal("experiment_unit_price_brl"),
                rs.getString("image_currency")), packageId);
        return items.stream().findFirst();
    }

    private static FlowSubmissionImagePackageStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            log.warn("Status '{}' desconhecido para flow_submission_image_package", status);
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private MediaProvider resolveProvider(String source) {
        if (!StringUtils.hasText(source)) {
            return MediaProvider.OPENAI;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        try {
            return MediaProvider.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Provider '{}' desconhecido, usando OPENAI como padrão", source);
            }
            return MediaProvider.OPENAI;
        }
    }

    private String resolvePrompt(String providedPrompt, String fallbackPrompt) {
        if (StringUtils.hasText(providedPrompt)) {
            return providedPrompt.trim();
        }
        if (StringUtils.hasText(fallbackPrompt)) {
            return fallbackPrompt.trim();
        }
        return null;
    }

    private String resolveModelFromAssets(String requestModel, String snapshotModel, List<Asset> assets) {
        if (StringUtils.hasText(requestModel)) {
            return requestModel.trim();
        }
        if (assets != null && !assets.isEmpty()) {
            List<String> models = new ArrayList<>();
            for (Asset asset : assets) {
                if (asset == null) {
                    continue;
                }
                String model = asset.getModel();
                if (!StringUtils.hasText(model)) {
                    continue;
                }
                String normalized = model.trim();
                if (!models.contains(normalized)) {
                    models.add(normalized);
                }
            }
            if (models.size() == 1) {
                return models.get(0);
            }
            if (models.size() > 1) {
                log.warn("Imagens geradas retornaram múltiplos modelos: {}", models);
                return models.get(0);
            }
        }
        if (StringUtils.hasText(snapshotModel)) {
            return snapshotModel.trim();
        }
        return null;
    }

    private String resolveModelValue(String primary, String secondary, String tertiary) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(secondary)) {
            return secondary.trim();
        }
        if (StringUtils.hasText(tertiary)) {
            return tertiary.trim();
        }
        return null;
    }

    private String buildPayload(GeneratedImageRequest image, String storedFileName, String publicUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stored_file_name", storedFileName);
        if (StringUtils.hasText(publicUrl)) {
            payload.put("public_url", publicUrl);
        }
        String model = resolveModelValue(image.model(), null, null);
        if (StringUtils.hasText(model)) {
            payload.put("model", model);
        }
        if (StringUtils.hasText(image.prompt())) {
            payload.put("prompt", image.prompt().trim());
        }
        Map<String, Object> metadata = buildGenerationMetadata(image);
        if (!metadata.isEmpty()) {
            payload.put("metadata", metadata);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar o payload da imagem gerada", ex);
        }
    }

    private Map<String, Object> buildGenerationMetadata(GeneratedImageRequest image) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (StringUtils.hasText(image.source())) {
            metadata.put("source", image.source().trim());
        }
        if (image.width() != null) {
            metadata.put("width", image.width());
        }
        if (image.height() != null) {
            metadata.put("height", image.height());
        }
        if (StringUtils.hasText(image.orientation())) {
            metadata.put("orientation", image.orientation().trim());
        }
        return metadata;
    }

    private String normalizeStoredFileName(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stored_file_name não pode ser vazio");
        }
        return storedFileName.trim();
    }

    private String normalizePublicUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }
        return publicUrl.trim();
    }

    private GenerationMetadata resolveGenerationMetadata(LeadPortalWorkerImageResultRequest request) {
        if (request == null || request.images() == null || request.images().isEmpty()) {
            return new GenerationMetadata(null, null, null);
        }
        for (GeneratedImageRequest image : request.images()) {
            if (image == null) {
                continue;
            }
            ImageOrientation orientation = parseOrientation(image.orientation());
            Integer width = image.width();
            Integer height = image.height();
            if (orientation != null || width != null || height != null) {
                return new GenerationMetadata(orientation, width, height);
            }
        }
        return new GenerationMetadata(null, null, null);
    }

    private ImageOrientation parseOrientation(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return ImageOrientation.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Unknown image orientation '{}' received from worker", raw);
            }
            return null;
        }
    }

    private record GenerationMetadata(ImageOrientation orientation, Integer width, Integer height) { }

    private ResponseStatusException notFound(long packageId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Pacote %d não encontrado".formatted(packageId));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record PackageSnapshot(
            long id,
            FlowSubmissionImagePackageStatus status,
            Integer freeImages,
            String model,
            String prompt,
            Long imageModelId,
            Long imageModelQualityId,
            String imageOrientation,
            Integer imageWidth,
            Integer imageHeight,
            BigDecimal imageUnitPriceUsd,
            BigDecimal imageTotalPriceUsd,
            BigDecimal experimentUnitPriceBrl,
            String imageCurrency) {}
}
