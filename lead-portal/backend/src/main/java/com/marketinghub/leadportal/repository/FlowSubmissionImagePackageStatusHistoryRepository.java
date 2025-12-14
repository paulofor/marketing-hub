package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowSubmissionImagePackageStatusHistoryRepository
        extends JpaRepository<FlowSubmissionImagePackageStatusHistoryEntity, Long> {}
