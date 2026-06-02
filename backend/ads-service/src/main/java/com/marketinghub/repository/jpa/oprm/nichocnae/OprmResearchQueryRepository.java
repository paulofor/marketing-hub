package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar frases de pesquisa do pipeline OPRM nicho CNAE.
 */
public interface OprmResearchQueryRepository extends JpaRepository<OprmResearchQuery, Long> {
    /** Lista as frases de pesquisa de um ciclo em ordem de prioridade operacional. */
    List<OprmResearchQuery> findByResearchCycleIdOrderByPriorityAscIdAsc(Long researchCycleId);

    /** Lista frases pendentes de busca para a etapa três em ordem operacional. */
    List<OprmResearchQuery> findByStatusOrderByPriorityAscIdAsc(String status, Pageable pageable);
}
