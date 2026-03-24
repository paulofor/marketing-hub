package com.marketinghub.experiment.report.mapper;

import com.marketinghub.experiment.report.ExperimentReportRequest;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDto;
import org.springframework.stereotype.Component;

/**
 * Conversões entre entidades e DTOs de solicitações de relatório.
 */
@Component
public class ExperimentReportRequestMapper {

    public ExperimentReportRequestDto toDto(ExperimentReportRequest entity) {
        if (entity == null) {
            return null;
        }
        ExperimentReportRequestDto dto = new ExperimentReportRequestDto();
        dto.setId(entity.getId());
        dto.setExperimentId(entity.getExperiment() != null ? entity.getExperiment().getId() : null);
        dto.setStatus(entity.getStatus());
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setDownloadUrl(entity.getDownloadUrl());
        dto.setFailureReason(entity.getFailureReason());
        return dto;
    }

    public ExperimentReportRequestDetailDto toDetailDto(ExperimentReportRequest entity) {
        ExperimentReportRequestDetailDto dto = new ExperimentReportRequestDetailDto();
        ExperimentReportRequestDto base = toDto(entity);
        if (base != null) {
            dto.setId(base.getId());
            dto.setExperimentId(base.getExperimentId());
            dto.setStatus(base.getStatus());
            dto.setRequestedAt(base.getRequestedAt());
            dto.setCompletedAt(base.getCompletedAt());
            dto.setRequestedBy(base.getRequestedBy());
            dto.setDownloadUrl(base.getDownloadUrl());
            dto.setFailureReason(base.getFailureReason());
        }
        dto.setPayloadSnapshot(entity != null ? entity.getPayloadSnapshot() : null);
        return dto;
    }
}
