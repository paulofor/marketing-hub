package com.marketinghub.repository.jpa.facebookads.playbook;

import com.marketinghub.facebookads.playbook.ExperimentFacebookApiLogEntry;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de ExperimentFacebookApiLogEntry. */
public interface ExperimentFacebookApiLogEntryRepository
    extends JpaRepository<ExperimentFacebookApiLogEntry, Long> {
  List<ExperimentFacebookApiLogEntry> findByExperimentId(Long experimentId, Pageable pageable);
}
