package com.marketinghub.facebookads.playbook.repository;

import com.marketinghub.facebookads.playbook.ExperimentFacebookApiLogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentFacebookApiLogEntryRepository extends JpaRepository<ExperimentFacebookApiLogEntry, Long> {
    List<ExperimentFacebookApiLogEntry> findByExperimentId(Long experimentId, Pageable pageable);
}
