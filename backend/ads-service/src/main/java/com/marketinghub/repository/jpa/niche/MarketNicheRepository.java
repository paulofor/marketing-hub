package com.marketinghub.repository.jpa.niche;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheListItemProjection;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Responsabilidade: persistir e consultar nichos de mercado no banco de dados.
 */
public interface MarketNicheRepository extends JpaRepository<MarketNiche, Long> {
    /**
     * Lista os nichos para a tela administrativa com agregados de hipóteses do pipeline e experimentos.
     */
    @Query("""
            select n.id as id,
                   n.name as name,
                   n.createdAt as createdAt,
                   coalesce(n.totalCost, 0) as totalCost,
                   (select count(hp.idJob)
                    from HypothesisPainStageExecution hp
                    where hp.marketNicheId = n.id
                      and hp.stageCode = 'hypothesis-result'
                      and hp.status = 'CONCLUIDO') as pipelineHypothesesCount,
                   (select count(e.id)
                    from Experiment e
                    where e.niche = n) as experimentsCount
            from MarketNiche n
            order by n.createdAt desc, n.id desc
            """)
    Page<MarketNicheListItemProjection> findListItems(Pageable pageable);

    /**
     * Busca nichos configurados para geração de hipóteses.
     *
     * <p>Os filtros ficam na consulta para carregar apenas os registros necessários.</p>
     */
    @Query("""
            select n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.hypothesesToGenerate is not null
              and n.hypothesesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateHypotheses();

    /**
     * Busca nichos configurados para geração de interesses.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.interestsToGenerate is not null
              and n.interestsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateInterests();

    /**
     * Busca nichos configurados para geração de cargos.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.jobTitlesToGenerate is not null
              and n.jobTitlesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateJobTitles();

    /**
     * Busca nichos configurados para geração de comportamentos.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.behaviorsToGenerate is not null
              and n.behaviorsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateBehaviors();

    /**
     * Busca nichos configurados para geração de descrições detalhadas.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.detailedDescriptionsToGenerate is not null
              and n.detailedDescriptionsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateDetailedDescriptions();

    /**
     * Lista nichos que possuem solicitação pendente e ao menos um experimento comercialmente pronto para criação de pixel.
     */
    @Query("""
            select distinct n from MarketNiche n
            where n.facebookPixelId is null
              and n.facebookPixelRequestStatus = 'PENDING'
              and n.facebookPixelRequestedAt is not null
              and exists (
                    select 1 from Experiment e
                    where e.niche = n
                      and e.status in :statuses
                      and e.platform = :platform
                      and e.creativeApproved = true
                      and e.followUpActionUrl is not null
              )
            """)
    List<MarketNiche> findPendingPixelRequests(@Param("statuses") List<ExperimentStatus> statuses,
                                               @Param("platform") ExperimentPlatform platform);

    /**
     * Incrementa o custo total acumulado de um nicho.
     */
    @Modifying
    @Query("""
            update MarketNiche n
            set n.totalCost = coalesce(n.totalCost, 0) + :delta
            where n.id = :id
            """)
    void incrementTotalCost(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /**
     * Verifica se já existe nicho com o mesmo nome, ignorando caixa, para impedir duplicidade comercial.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Verifica se outro nicho já usa o mesmo nome, preservando atualização idempotente do próprio registro.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Busca o nicho mais recente associado ao CNAE de origem para reprocessamento idempotente.
     */
    java.util.Optional<MarketNiche> findFirstBySourceCnaeCodeOrderByIdDesc(String sourceCnaeCode);

}
