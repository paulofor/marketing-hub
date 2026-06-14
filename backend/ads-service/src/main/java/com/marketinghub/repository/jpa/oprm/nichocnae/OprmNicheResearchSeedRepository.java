package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório responsável por persistir e consultar seeds de pesquisa de nicho CNAE. */
public interface OprmNicheResearchSeedRepository extends JpaRepository<OprmNicheResearchSeed, Long> {
    /** Verifica se um ciclo já possui seed de pesquisa operacional gerado. */
    boolean existsByResearchCycleId(Long researchCycleId);

    /** Busca o seed operacional gerado para um ciclo de pesquisa. */
    Optional<OprmNicheResearchSeed> findByResearchCycleId(Long researchCycleId);

    /** Soma o custo de IA registrado para um ciclo de pesquisa. */
    @Query(
      "select coalesce(sum(s.costUsd), 0) from OprmNicheResearchSeed s where s.researchCycleId = :researchCycleId")
    BigDecimal sumCostUsdByResearchCycleId(@Param("researchCycleId") Long researchCycleId);
}
