package com.marketinghub.watermark.service;

import com.marketinghub.watermark.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.watermark.repository.FlowSubmissionImagePackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowSubmissionImagePackageStatusService {

    private final FlowSubmissionImagePackageRepository packageRepository;
    private final FlowSubmissionImagePackageStatusHistoryService historyService;

    public FlowSubmissionImagePackageStatusService(
            FlowSubmissionImagePackageRepository packageRepository,
            FlowSubmissionImagePackageStatusHistoryService historyService) {
        this.packageRepository = packageRepository;
        this.historyService = historyService;
    }

    public boolean updateStatus(
            Long id,
            FlowSubmissionImagePackageEntity.Status expectedStatus,
            FlowSubmissionImagePackageEntity.Status newStatus,
            String failureReason) {
        String normalizedReason = StringUtils.hasText(failureReason) ? failureReason.trim() : null;
        int updated = packageRepository.updateStatus(id, expectedStatus, newStatus, normalizedReason);
        if (updated > 0) {
            historyService.recordStatusChange(id, newStatus, normalizedReason);
            return true;
        }
        return false;
    }
}
