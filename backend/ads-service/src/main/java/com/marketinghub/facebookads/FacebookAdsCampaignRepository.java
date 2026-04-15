package com.marketinghub.facebookads;

import com.marketinghub.experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacebookAdsCampaignRepository extends JpaRepository<FacebookAdsCampaign, String> {

    @Query("""
            select c from FacebookAdsCampaign c
            join fetch c.experiment e
            where e.status = :status
            """)
    List<FacebookAdsCampaign> findAllByExperimentStatus(@Param("status") ExperimentStatus status);

    @Query("""
            select distinct c from FacebookAdsCampaign c
            left join fetch c.adSets s
            left join fetch s.experimentAdSet eas
            where c.experiment.id = :experimentId
            order by c.createdAt asc
            """)
    List<FacebookAdsCampaign> findDetailedByExperimentId(@Param("experimentId") Long experimentId);

    List<FacebookAdsCampaign> findByExperimentId(Long experimentId);

    @Query("""
            select distinct c from FacebookAdsCampaign c
            left join fetch c.experiment e
            where c.stopRequestedAt is not null
              and c.stopCompletedAt is null
            """)
    List<FacebookAdsCampaign> findPendingStopRequests();
}
