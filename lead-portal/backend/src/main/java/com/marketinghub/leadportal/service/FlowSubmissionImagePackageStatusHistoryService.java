package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowSubmissionImagePackageStatusHistoryService {

    private final FlowSubmissionImagePackageStatusHistoryRepository repository;

    public FlowSubmissionImagePackageStatusHistoryService(
            FlowSubmissionImagePackageStatusHistoryRepository repository) {
        this.repository = repository;
    }

    public void recordStatusChange(Long packageId, FlowSubmissionImagePackageEntity.Status status, String reason) {
        if (packageId == null || status == null) {
            return;
        }
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;

        FlowSubmissionImagePackageStatusHistoryEntity history =
                new FlowSubmissionImagePackageStatusHistoryEntity();
        history.setPackageId(packageId);
        history.setStatus(status.name());
        history.setFailureReason(normalizedReason);

        repository.save(history);
    }
}
