package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.FacebookAdStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsCampaign.
 */
public interface FacebookAdsCampaignRepository extends JpaRepository<FacebookAdsCampaign, String> {

    /**
     * Lista campanhas cujo experimento proprietário está no status informado.
     */
    @Query("""
            select c from FacebookAdsCampaign c
            join fetch c.experiment e
            where e.status = :status
            """)
    List<FacebookAdsCampaign> findAllByExperimentStatus(@Param("status") ExperimentStatus status);

    /**
     * Lista campanhas que devem ter métricas sincronizadas, incluindo janela final de liquidação Meta.
     */
    @Query("""
            select c from FacebookAdsCampaign c
            join fetch c.experiment e
            where e.status = :runningStatus
               or (
                   e.status in :settlementStatuses
                   and (
                       c.metricsFinalSyncedAt is null
                       or c.updatedAt >= :settlementCutoff
                       or e.updatedAt >= :settlementCutoff
                   )
               )
            """)
    List<FacebookAdsCampaign> findMetricsSyncTargets(
            @Param("runningStatus") ExperimentStatus runningStatus,
            @Param("settlementStatuses") Collection<ExperimentStatus> settlementStatuses,
            @Param("settlementCutoff") Instant settlementCutoff);

    /**
     * Lista campanhas ativas cujo experimento proprietário está no status informado.
     */
    @Query("""
            select c from FacebookAdsCampaign c
            join fetch c.experiment e
            where e.status = :experimentStatus
              and c.status = :campaignStatus
            """)
    List<FacebookAdsCampaign> findAllByExperimentStatusAndStatus(
            @Param("experimentStatus") ExperimentStatus experimentStatus,
            @Param("campaignStatus") FacebookAdStatus campaignStatus);

    /**
     * Lista campanhas com seus conjuntos de anúncios para um experimento em ordem de criação.
     */
    @Query("""
            select distinct c from FacebookAdsCampaign c
            left join fetch c.adSets s
            left join fetch s.experimentAdSet eas
            where c.experiment.id = :experimentId
            order by c.createdAt asc
            """)
    List<FacebookAdsCampaign> findDetailedByExperimentId(@Param("experimentId") Long experimentId);

    /**
     * Verifica se um experimento já possui campanha persistida no backend.
     */
    boolean existsByExperimentId(Long experimentId);

    /**
     * Lista campanhas persistidas para um experimento sem forçar joins de leitura.
     */
    List<FacebookAdsCampaign> findByExperimentId(Long experimentId);

    /**
     * Remove campanhas persistidas para um experimento antes de reprocessar a publicação.
     */
    @org.springframework.data.jpa.repository.Modifying
    void deleteByExperimentId(Long experimentId);

    /**
     * Lista campanhas com solicitações de parada pendentes para o worker Facebook.
     */
    @Query("""
            select distinct c from FacebookAdsCampaign c
            left join fetch c.experiment e
            where c.stopRequestedAt is not null
              and c.stopCompletedAt is null
            """)
    List<FacebookAdsCampaign> findPendingStopRequests();
}
