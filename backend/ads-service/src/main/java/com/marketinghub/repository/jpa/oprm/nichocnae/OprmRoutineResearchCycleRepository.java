package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório responsável por persistir e consultar ciclos de pesquisa de rotina de nicho CNAE.
 */
public interface OprmRoutineResearchCycleRepository extends JpaRepository<OprmRoutineResearchCycle, Long> {
    /** Lista ciclos vinculados ao nicho CNAE de origem em ordem operacional decrescente. */
    List<OprmRoutineResearchCycle> findBySourceNicheIdOrderByStartedAtDesc(Long sourceNicheId);

    /** Lista ciclos por status para filas internas do pipeline de pesquisa de rotina. */
    List<OprmRoutineResearchCycle> findByStatusOrderByStartedAtAsc(String status, Pageable pageable);

    /** Lista ciclos em execução que ainda não possuem seed de pesquisa de nicho gravado. */
    @Query("""
            select cycle
            from OprmRoutineResearchCycle cycle
            where cycle.status = :status
              and not exists (
                  select 1
                  from OprmNicheResearchSeed seed
                  where seed.researchCycleId = cycle.id
              )
            order by cycle.startedAt asc
            """)
    List<OprmRoutineResearchCycle> findSeedBuilderPendingByStatus(String status, Pageable pageable);
}
