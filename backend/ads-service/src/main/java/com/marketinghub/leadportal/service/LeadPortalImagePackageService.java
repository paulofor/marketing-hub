package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageDetailDto;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageSummaryDto;
import com.marketinghub.leadportal.integration.LeadPortalIntegrationProperties;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides read access to Lead Portal image packages so that the operations team can
 * monitor the pipeline inside Marketing Hub.
 */
@Service
public class LeadPortalImagePackageService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageService.class);

    private static final String IMAGE_TYPE_ORIGINAL = "ORIGINAL";
    private static final String IMAGE_TYPE_GENERATED = "GENERATED";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LeadPortalIntegrationProperties integrationProperties;

    public LeadPortalImagePackageService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            LeadPortalIntegrationProperties integrationProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.integrationProperties = integrationProperties;
    }

    public List<LeadPortalImagePackageSummaryDto> listImagePackages(Collection<FlowSubmissionImagePackageStatus> statuses) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    pack.id,
                    pack.submission_id,
                    pack.status,
                    pack.prompt,
                    pack.model,
                    pack.planned_outputs,
                    pack.free_images,
                    pack.failure_reason,
                    pack.created_at,
                    pack.updated_at,
                    sub.flow_slug,
                    sub.name,
                    sub.email,
                    sub.answers,
                    pack.image_model_id,
                    pack.image_model_quality_id,
                    pack.image_orientation,
                    pack.image_width,
                    pack.image_height,
                    pack.image_unit_price_usd,
                    pack.image_total_price_usd,
                    pack.image_currency,
                    igm.display_name AS image_model_name,
                    igq.display_name AS image_model_quality_name,
                    (SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id) AS generated_count,
                    (SELECT COUNT(*) FROM flow_submission_image_watermark wm JOIN flow_submission_image_item items2 ON items2.id = wm.item_id WHERE items2.package_id = pack.id) AS watermarked_count
                FROM flow_submission_image_package pack
                LEFT JOIN flow_submissions sub ON sub.id = pack.submission_id
                LEFT JOIN image_generation_model igm ON igm.id = pack.image_model_id
                LEFT JOIN image_generation_quality igq ON igq.id = pack.image_model_quality_id
                """);

        List<Object> params = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" WHERE pack.status IN (")
                    .append(statuses.stream().map(s -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            statuses.forEach(status -> params.add(status.name()));
        }
        sql.append(" ORDER BY pack.created_at DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> mapSummary(rs));
    }

    public LeadPortalImagePackageDetailDto getImagePackage(long id) {
        String sql = """
                SELECT
                    pack.id,
                    pack.submission_id,
                    pack.status,
                    pack.prompt,
                    pack.model,
                    pack.planned_outputs,
                    pack.free_images,
                    pack.failure_reason,
                    pack.created_at,
                    pack.updated_at,
                    sub.flow_slug,
                    sub.name,
                    sub.email,
                    sub.answers,
                    pack.image_model_id,
                    pack.image_model_quality_id,
                    pack.image_orientation,
                    pack.image_width,
                    pack.image_height,
                    pack.image_unit_price_usd,
                    pack.image_total_price_usd,
                    pack.image_currency,
                    igm.display_name AS image_model_name,
                    igq.display_name AS image_model_quality_name,
                    (SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id) AS generated_count,
                    (SELECT COUNT(*) FROM flow_submission_image_watermark wm JOIN flow_submission_image_item items2 ON items2.id = wm.item_id WHERE items2.package_id = pack.id) AS watermarked_count,
                    sub.image_question_key,
                    sub.stored_file_name,
                    sub.created_at AS submission_created_at
                FROM flow_submission_image_package pack
                LEFT JOIN flow_submissions sub ON sub.id = pack.submission_id
                LEFT JOIN image_generation_model igm ON igm.id = pack.image_model_id
                LEFT JOIN image_generation_quality igq ON igq.id = pack.image_model_quality_id
                WHERE pack.id = ?
                """;

        DetailProjection projection;
        try {
            projection = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapDetailProjection(rs), id);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacote de imagem não encontrado");
        }

        LeadPortalImagePackageDetailDto.ImageReference originalImage = buildOriginalImage(projection);
        List<LeadPortalImagePackageDetailDto.ImageReference> generatedImages = fetchGeneratedImages(id);

        LeadPortalImagePackageSummaryDto summary = projection.summary();
        LeadPortalImagePackageDetailDto.SubmissionInfo submissionInfo = new LeadPortalImagePackageDetailDto.SubmissionInfo(
                projection.flowSlug(),
                projection.name(),
                projection.email(),
                summary.phone(),
                projection.imageQuestionKey());

        return new LeadPortalImagePackageDetailDto(
                summary.id(),
                summary.submissionId(),
                summary.status(),
                summary.prompt(),
                summary.model(),
                summary.plannedOutputs(),
                summary.freeImages(),
                summary.watermarkedImageCount(),
                summary.failureReason(),
                summary.createdAt(),
                summary.updatedAt(),
                submissionInfo,
                originalImage,
                generatedImages,
                summary.imageModelId(),
                summary.imageModelName(),
                summary.imageModelQualityId(),
                summary.imageModelQualityName(),
                summary.imageOrientation(),
                summary.imageWidth(),
                summary.imageHeight(),
                summary.imageUnitPriceUsd(),
                summary.imageTotalPriceUsd(),
                summary.imageCurrency());
    }

    @Transactional
    public void retry(long packageId) {
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, updated_at = ? WHERE id = ? AND status = ?",
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                Timestamp.from(Instant.now()),
                packageId,
                FlowSubmissionImagePackageStatus.FAILED.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacote de imagem não encontrado"));
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pacote %d não pode ser reprocessado a partir do status %s".formatted(packageId, current));
        }
    }

    private LeadPortalImagePackageSummaryDto mapSummary(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        UUID submissionId = mapSubmissionId(rs);
        FlowSubmissionImagePackageStatus status = parseStatus(rs.getString("status"));
        Instant createdAt = toInstant(rs.getTimestamp("created_at"));
        Instant updatedAt = toInstant(rs.getTimestamp("updated_at"));
        Integer plannedOutputs = getInteger(rs, "planned_outputs");
        Integer freeImages = getInteger(rs, "free_images");
        Integer generatedImageCount = getInteger(rs, "generated_count");
        if (generatedImageCount == null) {
            generatedImageCount = 0;
        }
        Integer watermarkedImageCount = getInteger(rs, "watermarked_count");
        if (watermarkedImageCount == null) {
            watermarkedImageCount = 0;
        }

        String answers = rs.getString("answers");
        String phone = extractPhone(answers);
        Long imageModelId = getLong(rs, "image_model_id");
        Long imageModelQualityId = getLong(rs, "image_model_quality_id");
        String imageModelName = rs.getString("image_model_name");
        String imageModelQualityName = rs.getString("image_model_quality_name");
        String imageOrientation = rs.getString("image_orientation");
        Integer imageWidth = getInteger(rs, "image_width");
        Integer imageHeight = getInteger(rs, "image_height");
        java.math.BigDecimal imageUnitPriceUsd = rs.getBigDecimal("image_unit_price_usd");
        java.math.BigDecimal imageTotalPriceUsd = rs.getBigDecimal("image_total_price_usd");
        String imageCurrency = rs.getString("image_currency");

        return new LeadPortalImagePackageSummaryDto(
                id,
                submissionId,
                rs.getString("flow_slug"),
                rs.getString("name"),
                rs.getString("email"),
                phone,
                status,
                rs.getString("prompt"),
                rs.getString("model"),
                plannedOutputs,
                freeImages,
                generatedImageCount,
                watermarkedImageCount,
                createdAt,
                updatedAt,
                rs.getString("failure_reason"),
                imageModelId,
                imageModelName,
                imageModelQualityId,
                imageModelQualityName,
                imageOrientation,
                imageWidth,
                imageHeight,
                imageUnitPriceUsd,
                imageTotalPriceUsd,
                imageCurrency);
    }

    private DetailProjection mapDetailProjection(ResultSet rs) throws SQLException {
        LeadPortalImagePackageSummaryDto summary = mapSummary(rs);
        return new DetailProjection(
                summary,
                rs.getString("flow_slug"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("image_question_key"),
                rs.getString("stored_file_name"),
                toInstant(rs.getTimestamp("submission_created_at")));
    }

    private LeadPortalImagePackageDetailDto.ImageReference buildOriginalImage(DetailProjection projection) {
        if (!StringUtils.hasText(projection.storedFileName())) {
            return null;
        }

        Optional<String> url = buildSubmissionImageUrl(projection.summary().submissionId());
        if (url.isEmpty()) {
            return null;
        }

        return new LeadPortalImagePackageDetailDto.ImageReference(
                IMAGE_TYPE_ORIGINAL,
                url.get(),
                url.get(),
                IMAGE_TYPE_ORIGINAL,
                null,
                0,
                null,
                null,
                projection.submissionCreatedAt(),
                null,
                projection.storedFileName(),
                null);
    }

    private Optional<String> buildSubmissionImageUrl(UUID submissionId) {
        if (submissionId == null) {
            return Optional.empty();
        }
        String baseUrl = integrationProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return Optional.of(normalized + "/api/flows/submissions/" + submissionId + "/image");
    }

    private List<LeadPortalImagePackageDetailDto.ImageReference> fetchGeneratedImages(long packageId) {
        String sql = """
                SELECT
                    item.id,
                    item.asset_id,
                    item.access_type,
                    item.position_index,
                    item.created_at,
                    asset.url,
                    asset.prompt,
                    asset.model,
                    asset.external_id,
                    wm.asset_id AS watermark_asset_id,
                    wm_asset.url AS watermark_url,
                    wm_asset.external_id AS watermark_external_id,
                    wm_asset.created_at AS watermark_created_at
                FROM flow_submission_image_item item
                LEFT JOIN asset ON asset.id = item.asset_id
                LEFT JOIN flow_submission_image_watermark wm ON wm.item_id = item.id
                LEFT JOIN asset wm_asset ON wm_asset.id = wm.asset_id
                WHERE item.package_id = ?
                ORDER BY item.position_index ASC, item.id ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            LeadPortalImagePackageDetailDto.WatermarkReference watermark = null;
            Long watermarkAssetId = getLong(rs, "watermark_asset_id");
            if (watermarkAssetId != null) {
                watermark = new LeadPortalImagePackageDetailDto.WatermarkReference(
                        watermarkAssetId,
                        rs.getString("watermark_url"),
                        rs.getString("watermark_url"),
                        toInstant(rs.getTimestamp("watermark_created_at")),
                        rs.getString("watermark_external_id"));
            }
            return new LeadPortalImagePackageDetailDto.ImageReference(
                    IMAGE_TYPE_GENERATED,
                    rs.getString("url"),
                    rs.getString("url"),
                    rs.getString("access_type"),
                    getLong(rs, "asset_id"),
                    getInteger(rs, "position_index"),
                    rs.getString("prompt"),
                    rs.getString("model"),
                    toInstant(rs.getTimestamp("created_at")),
                    getLong(rs, "id"),
                    rs.getString("external_id"),
                    watermark);
        }, packageId);
    }

    private Optional<FlowSubmissionImagePackageStatus> findStatus(long packageId) {
        return jdbcTemplate.query(
                        "SELECT status FROM flow_submission_image_package WHERE id = ?",
                        (rs, rowNum) -> parseStatus(rs.getString("status")),
                        packageId)
                .stream()
                .findFirst();
    }

    private FlowSubmissionImagePackageStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Status '{}' desconhecido para flow_submission_image_package", raw);
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private UUID mapSubmissionId(ResultSet rs) throws SQLException {
        Object rawValue = rs.getObject("submission_id");
        if (rawValue instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        String submission = rs.getString("submission_id");
        if (!StringUtils.hasText(submission)) {
            return null;
        }
        try {
            return UUID.fromString(submission);
        } catch (IllegalArgumentException ex) {
            log.warn("Valor de submission_id '{}' não pôde ser convertido para UUID", submission);
            return null;
        }
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String extractPhone(String answersJson) {
        if (!StringUtils.hasText(answersJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(answersJson);
            for (String key : List.of("phone", "telefone", "whatsapp")) {
                JsonNode node = root.get(key);
                if (node != null && node.isValueNode()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse flow submission answers while extracting phone", e);
        }
        return null;
    }

    private record DetailProjection(
            LeadPortalImagePackageSummaryDto summary,
            String flowSlug,
            String name,
            String email,
            String imageQuestionKey,
            String storedFileName,
            Instant submissionCreatedAt
    ) {}
}
