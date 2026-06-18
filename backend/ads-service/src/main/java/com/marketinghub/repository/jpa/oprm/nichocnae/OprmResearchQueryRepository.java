package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório responsável por persistir e consultar frases de pesquisa do pipeline OPRM nicho CNAE.
 */
public interface OprmResearchQueryRepository extends JpaRepository<OprmResearchQuery, Long> {
    /** Lista as frases de pesquisa de um ciclo em ordem de prioridade operacional. */
    List<OprmResearchQuery> findByResearchCycleIdOrderByPriorityAscIdAsc(Long researchCycleId);

    /** Conta frases de pesquisa de um ciclo que ainda estão em determinado status operacional. */
    long countByResearchCycleIdAndStatus(Long researchCycleId, String status);

    /** Lista frases pendentes de busca para a etapa três em ordem operacional. */
    List<OprmResearchQuery> findByStatusOrderByPriorityAscIdAsc(String status, Pageable pageable);

    /** Lista frases pendentes somente de ciclos posicionados na etapa atual de busca pública. */
    @Query("""
            select query
            from OprmResearchQuery query
            join OprmRoutineResearchCycle cycle on cycle.id = query.researchCycleId
            where query.status = :status
              and cycle.currentStageCode = :currentStageCode
            order by query.priority asc, query.id asc
            """)
    List<OprmResearchQuery> findPendingByStatusAndCycleStage(
            @Param("status") String status, @Param("currentStageCode") String currentStageCode, Pageable pageable);

    /** Remove queries de um ciclo antes de reexecutar etapas do mesmo job. */
    void deleteByResearchCycleId(Long researchCycleId);
}
