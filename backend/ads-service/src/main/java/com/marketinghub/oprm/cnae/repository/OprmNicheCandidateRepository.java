package com.marketinghub.oprm.cnae.repository;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar candidatos de nicho gerados pelo OPRM.
 */
public interface OprmNicheCandidateRepository extends JpaRepository<OprmNicheCandidate, Long> {
    /**
     * Lista candidatos de nicho vinculados a um CNAE específico.
     */
    List<OprmNicheCandidate> findByCnaeCodeOrderByOpportunityScoreDescCreatedAtDesc(String cnaeCode);
}
