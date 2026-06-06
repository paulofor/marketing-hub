package com.marketinghub.repository.jpa.niche;

import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório responsável por persistir e consultar perfis enriquecidos de nichos. */
public interface MarketNicheEnrichmentProfileRepository extends JpaRepository<MarketNicheEnrichmentProfile, Long> {
  /** Verifica se um cartão de rotina já foi materializado como nicho enriquecido. */
  boolean existsBySourceRoutineCardId(Long sourceRoutineCardId);

  /** Busca o perfil enriquecido materializado para um ciclo de pesquisa de rotina. */
  Optional<MarketNicheEnrichmentProfile> findFirstByResearchCycleIdOrderByIdDesc(Long researchCycleId);

  /** Lista o perfil enriquecido mais recente de cada nicho informado. */
  @Query("""
      select p from MarketNicheEnrichmentProfile p
      join fetch p.marketNiche n
      where n.id in :marketNicheIds
        and not exists (
          select 1 from MarketNicheEnrichmentProfile newer
          where newer.marketNiche = p.marketNiche
            and newer.id > p.id
        )
      """)
  List<MarketNicheEnrichmentProfile> findLatestByMarketNicheIds(@Param("marketNicheIds") List<Long> marketNicheIds);

  /** Lista perfis recentes cujo nome ou conteúdo principal ainda contém linguagem de solução. */
  @Query("""
      select p from MarketNicheEnrichmentProfile p
      join fetch p.marketNiche n
      where lower(coalesce(n.name, '')) like lower(concat('%', :term, '%'))
         or lower(coalesce(p.routineSummary, '')) like lower(concat('%', :term, '%'))
         or lower(coalesce(p.painsSummary, '')) like lower(concat('%', :term, '%'))
         or lower(coalesce(p.resultsSummary, '')) like lower(concat('%', :term, '%'))
         or lower(coalesce(p.mechanismOpportunitiesSummary, '')) like lower(concat('%', :term, '%'))
      order by p.createdAt desc, p.id desc
      """)
  List<MarketNicheEnrichmentProfile> findPotentiallyContaminatedByTerm(@Param("term") String term, org.springframework.data.domain.Pageable pageable);

}
