package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar ciclos de pesquisa de rotina de nicho CNAE.
 */
public interface OprmRoutineResearchCycleRepository extends JpaRepository<OprmRoutineResearchCycle, Long> {
    /** Lista ciclos vinculados ao nicho CNAE de origem em ordem operacional decrescente. */
    List<OprmRoutineResearchCycle> findBySourceNicheIdOrderByStartedAtDesc(Long sourceNicheId);

    /** Lista ciclos por status para filas internas do pipeline de pesquisa de rotina. */
    List<OprmRoutineResearchCycle> findByStatusOrderByStartedAtAsc(String status, Pageable pageable);
}
