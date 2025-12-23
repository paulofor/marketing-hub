package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageStatusHistoryRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowSubmissionImagePackageStatusHistoryService {

    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

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
        history.setCreatedAt(LocalDateTime.now(SAO_PAULO_ZONE));

        repository.save(history);
    }
}
