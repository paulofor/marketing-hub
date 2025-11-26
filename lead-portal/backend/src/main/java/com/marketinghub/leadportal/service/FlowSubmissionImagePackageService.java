package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.FlowSubmissionImagePackageResponse;
import com.marketinghub.leadportal.entity.FlowSubmissionEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FlowSubmissionImagePackageService {

    private static final List<String> PENDING_STATUSES = List.of(
            FlowSubmissionImagePackageEntity.Status.RECENT.name(),
            FlowSubmissionImagePackageEntity.Status.RECEIVED.name());

    private final FlowSubmissionImagePackageRepository imagePackageRepository;
    private final FlowSubmissionRepository submissionRepository;

    public FlowSubmissionImagePackageService(
            FlowSubmissionImagePackageRepository imagePackageRepository,
            FlowSubmissionRepository submissionRepository) {
        this.imagePackageRepository = imagePackageRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<FlowSubmissionImagePackageResponse> listPendingPackages() {
        List<FlowSubmissionImagePackageEntity> imagePackages =
                imagePackageRepository.findByStatusIn(PENDING_STATUSES);

        Map<UUID, FlowSubmissionEntity> submissionsById = submissionRepository
                .findAllById(imagePackages.stream()
                        .map(FlowSubmissionImagePackageEntity::getSubmissionId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(FlowSubmissionEntity::getId, Function.identity()));

        Comparator<FlowSubmissionImagePackageEntity> comparator = Comparator
                .comparing(FlowSubmissionImagePackageEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FlowSubmissionImagePackageEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        return imagePackages.stream()
                .sorted(comparator)
                .map(pkg -> FlowSubmissionImagePackageResponse.from(pkg, submissionsById.get(pkg.getSubmissionId())))
                .toList();
    }
}
