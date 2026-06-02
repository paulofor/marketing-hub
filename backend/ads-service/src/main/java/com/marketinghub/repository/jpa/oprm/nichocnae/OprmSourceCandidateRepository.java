package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar fontes candidatas do pipeline OPRM nicho CNAE.
 */
public interface OprmSourceCandidateRepository extends JpaRepository<OprmSourceCandidate, Long> {
    /** Lista fontes candidatas de um ciclo na ordem em que apareceram na busca. */
    List<OprmSourceCandidate> findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(Long researchCycleId);

    /** Verifica se uma URL já foi salva para uma query de pesquisa específica. */
    boolean existsByResearchQueryIdAndSourceUrl(Long researchQueryId, String sourceUrl);

    /** Lista fontes candidatas encontradas ainda não selecionadas para fetch na ordem operacional. */
    List<OprmSourceCandidate> findByStatusAndSelectedForFetchFalseOrderByResearchCycleIdAscResearchQueryIdAscSearchPositionAscIdAsc(
        String status, Pageable pageable);
}
