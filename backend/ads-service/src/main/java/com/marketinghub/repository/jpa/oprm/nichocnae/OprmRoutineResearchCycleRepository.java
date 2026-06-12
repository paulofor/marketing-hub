package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório responsável por persistir e consultar ciclos de pesquisa de rotina de nicho CNAE.
 */
public interface OprmRoutineResearchCycleRepository extends JpaRepository<OprmRoutineResearchCycle, Long> {
    /** Lista ciclos vinculados ao nicho CNAE de origem em ordem operacional decrescente. */
    List<OprmRoutineResearchCycle> findBySourceNicheIdOrderByStartedAtDesc(Long sourceNicheId);

    /** Lista ciclos vinculados ao CNAE informado em ordem operacional decrescente. */
    List<OprmRoutineResearchCycle> findByCnaeCodeOrderByStartedAtDesc(String cnaeCode);

    /** Lista ciclos por status para filas internas do pipeline de pesquisa de rotina. */
    List<OprmRoutineResearchCycle> findByStatusOrderByStartedAtAsc(String status, Pageable pageable);

    /** Lista os ciclos de pesquisa de rotina mais recentes para acompanhamento operacional. */
    List<OprmRoutineResearchCycle> findAllByOrderByStartedAtDesc(Pageable pageable);

    /** Lista ciclos aptos à etapa de seed, incluindo falhas retryáveis sem artefatos persistidos. */
    @Query("""
            select cycle
            from OprmRoutineResearchCycle cycle
            where not exists (
                  select 1
                  from OprmNicheResearchSeed seed
                  where seed.researchCycleId = cycle.id
              )
              and not exists (
                  select 1
                  from OprmResearchQuery researchQuery
                  where researchQuery.researchCycleId = cycle.id
              )
              and (
                  cycle.status = :runningStatus
                  or (
                      cycle.status = :failedStatus
                      and cycle.errorMessage is not null
                      and (
                          lower(cycle.errorMessage) like lower(concat('%', :legacyContractErrorFragment, '%'))
                          or lower(cycle.errorMessage) like lower(concat('%', :queryGoalLengthErrorFragment, '%'))
                      )
                      and lower(cycle.errorMessage) like lower(concat('%', :completePathFragment, '%'))
                  )
              )
            order by case when cycle.status = :failedStatus then 0 else 1 end, cycle.startedAt asc
            """)
    List<OprmRoutineResearchCycle> findSeedBuilderPendingOrRetryable(
            @Param("runningStatus") String runningStatus,
            @Param("failedStatus") String failedStatus,
            @Param("legacyContractErrorFragment") String legacyContractErrorFragment,
            @Param("queryGoalLengthErrorFragment") String queryGoalLengthErrorFragment,
            @Param("completePathFragment") String completePathFragment,
            Pageable pageable);

    /** Localiza o identificador do nicho materializado mais recente vinculado ao ciclo informado. */
    @Query("""
            select profile.marketNiche.id
            from MarketNicheEnrichmentProfile profile
            where profile.researchCycleId = :researchCycleId
              and profile.marketNiche is not null
            order by profile.id desc
            """)
    List<Long> findLatestMaterializedMarketNicheIdByResearchCycleId(
            @Param("researchCycleId") Long researchCycleId, Pageable pageable);

    /** Lista ciclos recentes cujo nome ou conteúdo principal ainda contém linguagem de solução. */
    @Query("""
            select cycle
            from OprmRoutineResearchCycle cycle
            where lower(coalesce(cycle.originalNicheName, '')) like lower(concat('%', :term, '%'))
               or lower(coalesce(cycle.nicheName, '')) like lower(concat('%', :term, '%'))
               or lower(coalesce(cycle.errorMessage, '')) like lower(concat('%', :term, '%'))
            order by cycle.startedAt desc
            """)
    List<OprmRoutineResearchCycle> findPotentiallyContaminatedByTerm(@Param("term") String term, Pageable pageable);
}
