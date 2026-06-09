package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.targeting.TargetingCandidateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de ExperimentTargetingSelection.
 */
public interface ExperimentTargetingSelectionRepository extends JpaRepository<ExperimentTargetingSelection, Long> {
    List<ExperimentTargetingSelection> findByExperimentIdOrderByCandidateTypeAscTermAsc(Long experimentId);

    /**
     * Busca seleções de público do experimento com o elemento de segmentação materializado.
     */
    @Query("""
            select selection from ExperimentTargetingSelection selection
            left join fetch selection.targetingElement element
            left join fetch element.niche
            left join fetch element.hypothesis
            where selection.experiment.id = :experimentId
            order by selection.candidateType asc, selection.term asc
            """)
    List<ExperimentTargetingSelection> findByExperimentIdWithTargetingElement(@Param("experimentId") Long experimentId);

    long countByExperimentId(Long experimentId);
    long countByExperimentIdAndCandidateType(Long experimentId, TargetingCandidateType candidateType);

    void deleteByExperimentId(Long experimentId);
}
