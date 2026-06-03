package com.marketinghub.repository.jpa.oprm.cnae;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório responsável por persistir e consultar candidatos de nicho gerados pelo OPRM.
 */
public interface OprmNicheCandidateRepository extends JpaRepository<OprmNicheCandidate, Long> {
    /**
     * Seleciona com bloqueio pessimista os melhores candidatos ainda pendentes de pesquisa de rotina.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select c from OprmNicheCandidate c "
                    + "where (c.routineResearchStatus is null or c.routineResearchStatus = 'PENDING') "
                    + "and c.opportunityScore is not null "
                    + "order by c.opportunityScore desc, c.createdAt asc")
    List<OprmNicheCandidate> findNextPendingRoutineResearchCandidate(Pageable pageable);

    /**
     * Lista sem bloqueio pessimista os melhores candidatos ainda pendentes de pesquisa de rotina para visualização.
     */
    @Query(
            "select c from OprmNicheCandidate c "
                    + "where (c.routineResearchStatus is null or c.routineResearchStatus = 'PENDING') "
                    + "and c.opportunityScore is not null "
                    + "order by c.opportunityScore desc, c.createdAt asc")
    List<OprmNicheCandidate> findNextPendingRoutineResearchCandidatePreview(Pageable pageable);

    /**
     * Lista candidatos de nicho vinculados a um CNAE específico.
     */
    List<OprmNicheCandidate> findByCnaeCodeOrderByOpportunityScoreDescCreatedAtDesc(String cnaeCode);

    /**
     * Lista candidatos de nicho enriquecidos priorizando maiores scores para acompanhamento no frontend.
     */
    List<OprmNicheCandidate> findAllByOrderByOpportunityScoreDescCreatedAtDesc(Pageable pageable);
}

