package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlowSubmissionImagePackageRepository
        extends JpaRepository<FlowSubmissionImagePackageEntity, Long> {

    List<FlowSubmissionImagePackageEntity> findByStatusIn(List<String> statuses);
}
