package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmFeedbackSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmFeedbackSnapshot.
 */
public interface OprmFeedbackSnapshotRepository extends JpaRepository<OprmFeedbackSnapshot, Long> {
}
