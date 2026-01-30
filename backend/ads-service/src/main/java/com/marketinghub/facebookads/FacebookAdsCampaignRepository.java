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

    List<FacebookAdsCampaign> findByExperimentId(Long experimentId);
}
