package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsCampaign.
 */
public interface FacebookAdsCampaignRepository extends JpaRepository<FacebookAdsCampaign, String> {

    /**
     * Lists campaigns whose owning experiment currently has the requested status.
     */
    @Query("""
            select c from FacebookAdsCampaign c
            join fetch c.experiment e
            where e.status = :status
            """)
    List<FacebookAdsCampaign> findAllByExperimentStatus(@Param("status") ExperimentStatus status);

    /**
     * Lists campaigns with their ad sets for one experiment in creation order.
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
     * Checks whether an experiment already has a campaign persisted in the backend.
     */
    boolean existsByExperimentId(Long experimentId);

    /**
     * Lists campaigns persisted for one experiment without forcing fetch joins.
     */
    List<FacebookAdsCampaign> findByExperimentId(Long experimentId);

    /**
     * Lists campaigns with pending stop requests for the Facebook worker.
     */
    @Query("""
            select distinct c from FacebookAdsCampaign c
            left join fetch c.experiment e
            where c.stopRequestedAt is not null
              and c.stopCompletedAt is null
            """)
    List<FacebookAdsCampaign> findPendingStopRequests();
}
