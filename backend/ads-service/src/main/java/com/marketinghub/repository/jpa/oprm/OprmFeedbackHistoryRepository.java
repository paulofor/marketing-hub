package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmFeedbackHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmFeedbackHistory.
 */
public interface OprmFeedbackHistoryRepository extends JpaRepository<OprmFeedbackHistory, Long> {
    List<OprmFeedbackHistory> findByOccupationNameIgnoreCaseAndPersonaLabelIgnoreCaseOrderByGeneratedAtAsc(String occupationName,
                                                                                                             String personaLabel);
}
