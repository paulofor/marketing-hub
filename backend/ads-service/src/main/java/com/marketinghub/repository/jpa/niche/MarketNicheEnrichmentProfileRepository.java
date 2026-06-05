package com.marketinghub.repository.jpa.niche;

import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir e consultar perfis enriquecidos de nichos. */
public interface MarketNicheEnrichmentProfileRepository extends JpaRepository<MarketNicheEnrichmentProfile, Long> {
  /** Verifica se um cartão de rotina já foi materializado como nicho enriquecido. */
  boolean existsBySourceRoutineCardId(Long sourceRoutineCardId);

  /** Busca o perfil enriquecido materializado para um ciclo de pesquisa de rotina. */
  Optional<MarketNicheEnrichmentProfile> findFirstByResearchCycleIdOrderByIdDesc(Long researchCycleId);
}
