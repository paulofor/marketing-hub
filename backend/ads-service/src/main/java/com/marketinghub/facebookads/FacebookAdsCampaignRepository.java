package com.marketinghub.facebookads;

import com.marketinghub.experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacebookAdsCampaignRepository extends JpaRepository<FacebookAdsCampaign, String> {

    @EntityGraph(attributePaths = {"experiment"})
    @Query("""
        select campaign from FacebookAdsCampaign campaign
        where campaign.experiment.status = :status
        """)
    List<FacebookAdsCampaign> findByExperimentStatus(@Param("status") ExperimentStatus status);
}
