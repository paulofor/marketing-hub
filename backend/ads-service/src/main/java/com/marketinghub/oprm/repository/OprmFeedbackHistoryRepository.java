package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmFeedbackHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmFeedbackHistoryRepository extends JpaRepository<OprmFeedbackHistory, Long> {
    List<OprmFeedbackHistory> findByOccupationNameIgnoreCaseAndPersonaLabelIgnoreCaseOrderByGeneratedAtAsc(String occupationName,
                                                                                                             String personaLabel);
}
