package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowSubmissionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlowSubmissionRepository extends JpaRepository<FlowSubmissionEntity, UUID> {}
