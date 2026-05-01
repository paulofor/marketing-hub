package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.PipelineOperationalMetricsDto;
import com.marketinghub.experiment.pipeline.dto.PipelineSectionMetricDto;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PipelineOperationalMetricsService {
    private static final String LHM_REGISTRY_FEATURE_FLAG = "lhm.registry.enabled";
    private static final String LHM_AUDIT_FEATURE_FLAG = "lhm.audit.gate.enabled";

    private final ExperimentPipelineGenerationJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public PipelineOperationalMetricsService(ExperimentPipelineGenerationJobRepository jobRepository,
                                             ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    public PipelineOperationalMetricsDto collect(int limit) {
        int safeLimit = Math.max(50, Math.min(limit, 1000));
        List<ExperimentPipelineGenerationJob> jobs = jobRepository.findAll(
                        PageRequest.of(0, safeLimit))
                .getContent();

        Map<ExperimentPipelineSection, List<ExperimentPipelineGenerationJob>> bySection = new EnumMap<>(ExperimentPipelineSection.class);
        jobs.forEach(job -> bySection.computeIfAbsent(job.getSection(), ignored -> new ArrayList<>()).add(job));

        List<PipelineSectionMetricDto> sectionMetrics = bySection.entrySet().stream()
                .map(entry -> toSectionMetrics(entry.getKey(), entry.getValue()))
                .toList();

        double avgDuration = sectionMetrics.stream().mapToDouble(PipelineSectionMetricDto::averageDurationSeconds).average().orElse(0D);
        double failRate = sectionMetrics.stream().mapToDouble(PipelineSectionMetricDto::failureRate).average().orElse(0D);
        double reworkRate = sectionMetrics.stream().mapToDouble(PipelineSectionMetricDto::reworkRate).average().orElse(0D);
        double placeholderRate = sectionMetrics.stream().mapToDouble(PipelineSectionMetricDto::placeholderRate).average().orElse(0D);
        double qualityScore = sectionMetrics.stream().mapToDouble(PipelineSectionMetricDto::averageQualityScore).average().orElse(0D);

        return new PipelineOperationalMetricsDto(
                Instant.now().toString(),
                Boolean.parseBoolean(System.getProperty(LHM_REGISTRY_FEATURE_FLAG, "false")),
                Boolean.parseBoolean(System.getProperty(LHM_AUDIT_FEATURE_FLAG, "false")),
                jobs.size(),
                avgDuration,
                failRate,
                reworkRate,
                placeholderRate,
                qualityScore,
                sectionMetrics);
    }

    private PipelineSectionMetricDto toSectionMetrics(ExperimentPipelineSection section,
                                                      List<ExperimentPipelineGenerationJob> jobs) {
        long total = jobs.size();
        long failed = jobs.stream().filter(j -> j.getStatus() == ExperimentPipelineGenerationJobStatus.FAILED).count();
        long rework = Math.max(0, total - 1);
        long placeholders = jobs.stream().filter(this::containsPlaceholder).count();
        double avgDuration = jobs.stream()
                .filter(j -> j.getStartedAt() != null && j.getFinishedAt() != null)
                .mapToLong(j -> Duration.between(j.getStartedAt(), j.getFinishedAt()).toSeconds())
                .average().orElse(0D);
        double avgQuality = jobs.stream()
                .mapToDouble(this::extractQualityScore)
                .filter(score -> score >= 0)
                .average()
                .orElse(0D);

        return new PipelineSectionMetricDto(
                section != null ? section.path() : "unknown",
                total,
                failed,
                pct(failed, total),
                rework,
                pct(rework, total),
                placeholders,
                pct(placeholders, total),
                avgDuration,
                avgQuality);
    }

    private double pct(long part, long total) {
        return total == 0 ? 0D : (part * 100.0D) / total;
    }

    private boolean containsPlaceholder(ExperimentPipelineGenerationJob job) {
        String source = job.getResponseContent();
        if (!StringUtils.hasText(source)) {
            source = job.getRawResponse();
        }
        return StringUtils.hasText(source) && source.toLowerCase(Locale.ROOT).contains("placeholder");
    }

    private double extractQualityScore(ExperimentPipelineGenerationJob job) {
        if (!StringUtils.hasText(job.getRequestBodyJson())) {
            return -1;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(job.getRequestBodyJson(), new TypeReference<>() {});
            Object qualityAudit = payload.get("qualityAudit");
            if (!(qualityAudit instanceof Map<?, ?> qa)) {
                return -1;
            }
            Object score = qa.get("score");
            return score instanceof Number number ? number.doubleValue() : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }
}

