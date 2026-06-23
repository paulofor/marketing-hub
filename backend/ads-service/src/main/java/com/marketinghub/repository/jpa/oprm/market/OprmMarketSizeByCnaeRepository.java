package com.marketinghub.repository.jpa.oprm.market;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprm.market.OprmMarketSizeByCnae;
import com.marketinghub.oprm.market.OprmMarketSizeByCnaeId;
import com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório responsável por consultar e paginar os agregados de tamanho de mercado por CNAE.
 */
public interface OprmMarketSizeByCnaeRepository extends JpaRepository<OprmMarketSizeByCnae, OprmMarketSizeByCnaeId> {
    /**
     * Retorna o ranking paginado dos CNAEs no snapshot mais recente, ordenado por score OPRM decrescente.
     */
    @Query("""
            select new com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto(
                ms.id.snapshotDate,
                ms.id.cnaeCode,
                c.description,
                ms.totalEstabelecimentos,
                ms.totalEstabelecimentosAtivos,
                ms.totalEmpresas,
                ms.totalEmpresasMei,
                ms.totalEmpresasSimples,
                sc.opportunityScore,
                sc.scoreStatus,
                (select count(distinct c2.neutralNicheName)
                    from OprmRoutineResearchCycle c2
                    where c2.cnaeCode = ms.id.cnaeCode),
                (select count(card.id)
                    from OprmNicheRoutineCard card
                    join OprmRoutineResearchCycle cycle on cycle.id = card.researchCycleId
                    where cycle.cnaeCode = ms.id.cnaeCode
                      and card.readyForHypothesis = true
                      and cycle.currentStageCode = 'enriched-niche-materializer'
                      and card.qualityCheckedAt is not null
                      and not exists (
                        select 1 from MarketNicheEnrichmentProfile profile
                        where profile.sourceRoutineCardId = card.id
                      )),
                (select coalesce(sum(seed.costUsd), 0)
                    from OprmNicheResearchSeed seed
                    where seed.cnaeCode = ms.id.cnaeCode),
                case when (select count(c3.id)
                    from OprmRoutineResearchCycle c3
                    where c3.cnaeCode = ms.id.cnaeCode
                      and c3.status = 'RUNNING') > 0 then true else false end
            )
            from OprmMarketSizeByCnae ms
            left join OprmCnpjCnaeDim c on c.cnaeCode = ms.id.cnaeCode
            left join OprmCnaeOpportunityScore sc on sc.cnaeCode = ms.id.cnaeCode
            where ms.id.snapshotDate = (select max(m2.id.snapshotDate) from OprmMarketSizeByCnae m2)
            order by sc.opportunityScore desc, ms.id.cnaeCode asc
            """)
    List<OprmTopCnaeMarketVolumeDto> findTopByLatestSnapshot(Pageable pageable);

    /**
     * Retorna o volume do CNAE informado no snapshot mais recente com score OPRM quando existir.
     */
    @Query("""
            select new com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto(
                ms.id.snapshotDate,
                ms.id.cnaeCode,
                c.description,
                ms.totalEstabelecimentos,
                ms.totalEstabelecimentosAtivos,
                ms.totalEmpresas,
                ms.totalEmpresasMei,
                ms.totalEmpresasSimples,
                sc.opportunityScore,
                sc.scoreStatus,
                (select count(distinct c2.neutralNicheName)
                    from OprmRoutineResearchCycle c2
                    where c2.cnaeCode = ms.id.cnaeCode),
                (select count(card.id)
                    from OprmNicheRoutineCard card
                    join OprmRoutineResearchCycle cycle on cycle.id = card.researchCycleId
                    where cycle.cnaeCode = ms.id.cnaeCode
                      and card.readyForHypothesis = true
                      and cycle.currentStageCode = 'enriched-niche-materializer'
                      and card.qualityCheckedAt is not null
                      and not exists (
                        select 1 from MarketNicheEnrichmentProfile profile
                        where profile.sourceRoutineCardId = card.id
                      )),
                (select coalesce(sum(seed.costUsd), 0)
                    from OprmNicheResearchSeed seed
                    where seed.cnaeCode = ms.id.cnaeCode),
                case when (select count(c3.id)
                    from OprmRoutineResearchCycle c3
                    where c3.cnaeCode = ms.id.cnaeCode
                      and c3.status = 'RUNNING') > 0 then true else false end
            )
            from OprmMarketSizeByCnae ms
            left join OprmCnpjCnaeDim c on c.cnaeCode = ms.id.cnaeCode
            left join OprmCnaeOpportunityScore sc on sc.cnaeCode = ms.id.cnaeCode
            where ms.id.snapshotDate = (select max(m2.id.snapshotDate) from OprmMarketSizeByCnae m2)
              and ms.id.cnaeCode = :cnaeCode
            """)
    Optional<OprmTopCnaeMarketVolumeDto> findLatestSnapshotByCnaeCode(@Param("cnaeCode") String cnaeCode);
}
