package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositório responsável por persistir e consultar cartões de rotina do pipeline OPRM NichoCNAE. */
public interface OprmNicheRoutineCardRepository extends JpaRepository<OprmNicheRoutineCard, Long> {
  /** Verifica se o ciclo já possui cartão de rotina sintetizado. */
  boolean existsByResearchCycleId(Long researchCycleId);

  /** Busca o cartão de rotina mais recente de um ciclo. */
  Optional<OprmNicheRoutineCard> findFirstByResearchCycleIdOrderByIdDesc(Long researchCycleId);

  /** Lista cartões sintetizados ainda não avaliados pelo gate de qualidade. */
  List<OprmNicheRoutineCard> findByQualityCheckedAtIsNullOrderByCreatedAtAscIdAsc(Pageable pageable);

  /** Lista cartões do ciclo ativo sintetizado mais recente que ainda não possuem segmentação MEI/autônomo. */
  @Query("""
      select c from OprmNicheRoutineCard c
      join OprmRoutineResearchCycle cycle on cycle.id = c.researchCycleId
      where cycle.status = 'ROUTINE_SYNTHESIZED'
        and not exists (
          select 1 from OprmMeiAudienceProfile p
          where p.researchCycleId = c.researchCycleId
        )
        and not exists (
          select 1 from MarketNicheEnrichmentProfile enrichment
          where enrichment.sourceRoutineCardId = c.id
             or enrichment.researchCycleId = c.researchCycleId
        )
        and not exists (
          select 1 from OprmRoutineResearchCycle newerCycle
          where newerCycle.sourceNicheId = cycle.sourceNicheId
            and (
              newerCycle.startedAt > cycle.startedAt
              or (newerCycle.startedAt = cycle.startedAt and newerCycle.id > cycle.id)
            )
        )
      order by cycle.startedAt desc, c.createdAt asc, c.id asc
      """)
  List<OprmNicheRoutineCard> findPendingMeiAudienceSegmentation(Pageable pageable);

  /** Lista cartões aprovados no gate de qualidade que ainda não alimentaram nicho e nicho enriquecido. */
  @Query("""
      select c from OprmNicheRoutineCard c
      where c.readyForHypothesis = true
        and c.qualityCheckedAt is not null
        and not exists (
          select 1 from MarketNicheEnrichmentProfile p
          where p.sourceRoutineCardId = c.id
        )
      order by c.qualityCheckedAt asc, c.id asc
      """)
  List<OprmNicheRoutineCard> findPendingEnrichedNicheMaterialization(Pageable pageable);
}
