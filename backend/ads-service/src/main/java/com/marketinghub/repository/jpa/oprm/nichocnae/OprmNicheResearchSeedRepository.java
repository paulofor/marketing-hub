package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir e consultar seeds de pesquisa de nicho CNAE.
 */
public interface OprmNicheResearchSeedRepository extends JpaRepository<OprmNicheResearchSeed, Long> {
    /** Verifica se um ciclo já possui seed de pesquisa operacional gerado. */
    boolean existsByResearchCycleId(Long researchCycleId);

    /** Busca o seed operacional gerado para um ciclo de pesquisa. */
    Optional<OprmNicheResearchSeed> findByResearchCycleId(Long researchCycleId);
}
