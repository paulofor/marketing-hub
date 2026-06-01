package com.marketinghub.repository.jpa.oprm.cnae;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar candidatos de nicho gerados pelo OPRM.
 */
public interface OprmNicheCandidateRepository extends JpaRepository<OprmNicheCandidate, Long> {
    /**
     * Lista candidatos de nicho vinculados a um CNAE específico.
     */
    List<OprmNicheCandidate> findByCnaeCodeOrderByOpportunityScoreDescCreatedAtDesc(String cnaeCode);

    /**
     * Lista candidatos de nicho enriquecidos priorizando maiores scores para acompanhamento no frontend.
     */
    List<OprmNicheCandidate> findAllByOrderByOpportunityScoreDescCreatedAtDesc(Pageable pageable);
}

