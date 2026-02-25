package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowSubmissionImagePackageStatusHistoryRepository
        extends JpaRepository<FlowSubmissionImagePackageStatusHistoryEntity, Long> {

    List<FlowSubmissionImagePackageStatusHistoryEntity> findByPackageIdOrderByCreatedAtAsc(Long packageId);
}
