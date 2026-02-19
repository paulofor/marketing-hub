package com.marketinghub.facebookads;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacebookAdsAdSetRepository extends JpaRepository<FacebookAdsAdSet, String> {

    List<FacebookAdsAdSet> findByCampaignIdIn(Collection<String> campaignIds);

    @Query("""
            select distinct s from FacebookAdsAdSet s
            left join fetch s.experimentAdSet
            left join fetch s.ads
            where s.campaign.id in :campaignIds
            """)
    List<FacebookAdsAdSet> findDetailedByCampaignIds(@Param("campaignIds") Collection<String> campaignIds);
}
