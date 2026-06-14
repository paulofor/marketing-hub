package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório responsável por persistir e consultar fontes candidatas do pipeline OPRM nicho CNAE.
 */
public interface OprmSourceCandidateRepository extends JpaRepository<OprmSourceCandidate, Long> {
    /** Lista fontes candidatas de um ciclo na ordem em que apareceram na busca. */
    List<OprmSourceCandidate> findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(Long researchCycleId);

    /** Verifica se uma URL já foi salva para uma query de pesquisa específica. */
    boolean existsByResearchQueryIdAndSourceUrl(Long researchQueryId, String sourceUrl);

    /** Lista apenas fontes de rotina, sem risco de solução/comercial, de ciclos ativos para coleta curta. */
    @Query("""
            select candidate
            from OprmSourceCandidate candidate, OprmRoutineResearchCycle cycle
            where cycle.id = candidate.researchCycleId
              and cycle.status = :cycleStatus
              and cycle.finishedAt is null
              and candidate.status = :candidateStatus
              and candidate.selectedForFetch = false
              and candidate.commercialPageRisk = false
              and candidate.solutionLanguageRisk = false
            order by candidate.researchCycleId asc,
              candidate.routineEvidenceScore desc,
              candidate.autonomousProfessionalEvidenceScore desc,
              candidate.brazilRelevanceScore desc,
              candidate.sourceFreshnessScore desc,
              candidate.researchQueryId asc,
              candidate.searchPosition asc,
              candidate.id asc
            """)
    List<OprmSourceCandidate> findPendingForFetchFromActiveCycles(
            @Param("candidateStatus") String candidateStatus,
            @Param("cycleStatus") String cycleStatus,
            Pageable pageable);
}
