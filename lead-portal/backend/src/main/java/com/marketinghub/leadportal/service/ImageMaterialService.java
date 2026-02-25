package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.ImageMaterialCaseResponse;
import com.marketinghub.leadportal.dto.ImageMaterialDashboardResponse;
import com.marketinghub.leadportal.entity.FlowSubmissionEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import com.marketinghub.leadportal.exception.FlowSubmissionNotFoundException;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.model.SimpleImageBriefing;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageStatusHistoryRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ImageMaterialService {

    private static final ZoneId HISTORY_ZONE = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbcTemplate;
    private final FlowSubmissionRepository submissionRepository;
    private final FlowSubmissionImagePackageRepository packageRepository;
    private final FlowSubmissionImagePackageStatusHistoryRepository historyRepository;
    private final SimpleImageBriefingMapper briefingMapper;

    public ImageMaterialService(
            JdbcTemplate jdbcTemplate,
            FlowSubmissionRepository submissionRepository,
            FlowSubmissionImagePackageRepository packageRepository,
            FlowSubmissionImagePackageStatusHistoryRepository historyRepository,
            SimpleImageBriefingMapper briefingMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.submissionRepository = submissionRepository;
        this.packageRepository = packageRepository;
        this.historyRepository = historyRepository;
        this.briefingMapper = briefingMapper;
    }

    public ImageMaterialDashboardResponse getDashboard(String flowSlug, int recentLimit) {
        String normalizedSlug = normalizeSlug(flowSlug);
        long totalSubmissions = queryForLong(
                "SELECT COUNT(*) FROM flow_submissions WHERE flow_slug = ?",
                normalizedSlug);
        Map<String, Long> statusCount = countPackagesByStatus(normalizedSlug);
        long plannedImages = queryForLong(
                "SELECT COALESCE(SUM(COALESCE(p.planned_outputs, 0)), 0) "
                        + "FROM flow_submission_image_package p "
                        + "JOIN flow_submissions s ON s.id = p.submission_id "
                        + "WHERE s.flow_slug = ?",
                normalizedSlug);
        long imagesGenerated = queryForLong(
                "SELECT COUNT(*) FROM flow_submission_image_item i "
                        + "JOIN flow_submission_image_package p ON p.id = i.package_id "
                        + "JOIN flow_submissions s ON s.id = p.submission_id "
                        + "WHERE s.flow_slug = ?",
                normalizedSlug);
        BigDecimal estimatedCostUsd = queryForBigDecimal(
                "SELECT COALESCE(SUM(p.image_total_price_usd), 0) "
                        + "FROM flow_submission_image_package p "
                        + "JOIN flow_submissions s ON s.id = p.submission_id "
                        + "WHERE s.flow_slug = ?",
                normalizedSlug);

        List<ImageMaterialDashboardResponse.CurrencyTotal> payments = jdbcTemplate.query(
                "SELECT COALESCE(p.payment_currency, 'BRL') AS currency, SUM(p.payment_amount) AS total "
                        + "FROM flow_submission_image_package p "
                        + "JOIN flow_submissions s ON s.id = p.submission_id "
                        + "WHERE s.flow_slug = ? AND p.payment_amount IS NOT NULL "
                        + "GROUP BY COALESCE(p.payment_currency, 'BRL')",
                (rs, rowNum) -> new ImageMaterialDashboardResponse.CurrencyTotal(
                        rs.getString("currency"), rs.getBigDecimal("total")),
                normalizedSlug);

        List<ImageMaterialDashboardResponse.PackageSummary> recentPackages = getRecentPackages(normalizedSlug, recentLimit);

        long queued = statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.RECEIVED.name(), 0L)
                + statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.RECENT.name(), 0L);
        long inProgress = statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.PROCESSING.name(), 0L)
                + statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.WATERMARK_PENDING.name(), 0L)
                + statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.WATERMARKING.name(), 0L);

        return new ImageMaterialDashboardResponse(
                normalizedSlug,
                totalSubmissions,
                queued,
                inProgress,
                statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.COMPLETED.name(), 0L),
                statusCount.getOrDefault(FlowSubmissionImagePackageEntity.Status.FAILED.name(), 0L),
                plannedImages,
                imagesGenerated,
                estimatedCostUsd,
                payments,
                recentPackages);
    }

    public ImageMaterialCaseResponse getCase(UUID submissionId) {
        FlowSubmission submission = submissionRepository
                .findById(submissionId)
                .map(FlowSubmissionEntity::toModel)
                .orElseThrow(() -> new FlowSubmissionNotFoundException(submissionId));

        List<FlowSubmissionImagePackageEntity> packages =
                packageRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId);
        SimpleImageBriefing briefing = briefingMapper
                .map(submission.flowSlug(), submission)
                .orElse(null);

        List<ImageMaterialCaseResponse.PackageDetails> packageDetails = packages.stream()
                .map(this::toPackageDetails)
                .toList();

        List<String> services = briefing == null ? List.of() : briefing.resolvedServices();

        return new ImageMaterialCaseResponse(
                submission.id(),
                submission.flowSlug(),
                briefing == null ? submission.flowSlug() : briefing.activityType(),
                submission.name(),
                submission.email(),
                briefing == null ? submission.email() : briefing.contactSummary(),
                briefing == null ? null : briefing.studioName(),
                briefing == null ? null : briefing.resolvedLocation(),
                services,
                submission.answers(),
                packageDetails);
    }

    private List<ImageMaterialDashboardResponse.PackageSummary> getRecentPackages(String flowSlug, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 20));
        List<FlowSubmissionImagePackageEntity> packages =
                packageRepository.findRecentByFlowSlug(flowSlug, PageRequest.of(0, limit));
        if (packages.isEmpty()) {
            return List.of();
        }

        Map<UUID, FlowSubmission> submissionById = submissionRepository
                .findAllById(packages.stream()
                        .map(FlowSubmissionImagePackageEntity::getSubmissionId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        FlowSubmissionEntity::getId,
                        FlowSubmissionEntity::toModel));

        return packages.stream()
                .map(pack -> {
                    FlowSubmission submission = submissionById.get(pack.getSubmissionId());
                    if (submission == null) {
                        return null;
                    }
                    SimpleImageBriefing briefing = briefingMapper
                            .map(submission.flowSlug(), submission)
                            .orElse(null);
                    List<String> services = briefing == null ? List.of() : briefing.resolvedServices();
                    String contact = briefing == null ? submission.email() : briefing.contactSummary();
                    String location = briefing == null ? null : briefing.resolvedLocation();
                    return new ImageMaterialDashboardResponse.PackageSummary(
                            pack.getId(),
                            submission.id(),
                            pack.getStatus(),
                            submission.name(),
                            contact,
                            briefing == null ? null : briefing.studioName(),
                            location,
                            services,
                            pack.getPlannedOutputs(),
                            pack.getImageTotalPriceUsd(),
                            pack.getImageCurrency(),
                            pack.getCreatedAt(),
                            pack.getUpdatedAt(),
                            pack.getFailureReason());
                })
                .filter(summary -> summary != null)
                .toList();
    }

    private Map<String, Long> countPackagesByStatus(String flowSlug) {
        Map<String, Long> counters = new HashMap<>();
        jdbcTemplate.query(
                "SELECT p.status, COUNT(*) AS total "
                        + "FROM flow_submission_image_package p "
                        + "JOIN flow_submissions s ON s.id = p.submission_id "
                        + "WHERE s.flow_slug = ? GROUP BY p.status",
                new Object[] {flowSlug},
                (ResultSetExtractor<Void>) rs -> {
                    while (rs.next()) {
                        counters.put(rs.getString("status"), rs.getLong("total"));
                    }
                    return null;
                });
        return counters;
    }

    private long queryForLong(String sql, Object... params) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, params);
            return value == null ? 0L : value;
        } catch (EmptyResultDataAccessException ex) {
            return 0L;
        }
    }

    private BigDecimal queryForBigDecimal(String sql, Object... params) {
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, params);
            return value == null ? BigDecimal.ZERO : value;
        } catch (EmptyResultDataAccessException ex) {
            return BigDecimal.ZERO;
        }
    }

    private ImageMaterialCaseResponse.PackageDetails toPackageDetails(FlowSubmissionImagePackageEntity pack) {
        List<ImageMaterialCaseResponse.StatusHistory> history = historyRepository
                .findByPackageIdOrderByCreatedAtAsc(pack.getId())
                .stream()
                .map(this::toHistoryEntry)
                .toList();
        return new ImageMaterialCaseResponse.PackageDetails(
                pack.getId(),
                pack.getStatus(),
                pack.getPlannedOutputs(),
                pack.getFreeImages(),
                pack.getModel(),
                pack.getPrompt(),
                pack.getImageTotalPriceUsd(),
                pack.getImageCurrency(),
                pack.getFailureReason(),
                pack.getCreatedAt(),
                pack.getUpdatedAt(),
                history);
    }

    private ImageMaterialCaseResponse.StatusHistory toHistoryEntry(
            FlowSubmissionImagePackageStatusHistoryEntity entity) {
        Instant created = entity.getCreatedAt() == null
                ? null
                : entity.getCreatedAt().atZone(HISTORY_ZONE).toInstant();
        return new ImageMaterialCaseResponse.StatusHistory(
                entity.getStatus(), created, entity.getFailureReason());
    }

    private String normalizeSlug(String slug) {
        return StringUtils.hasText(slug) ? slug.trim() : "formulario-simples-personal-trainer";
    }
}
