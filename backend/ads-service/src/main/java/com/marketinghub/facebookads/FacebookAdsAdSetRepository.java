package com.marketinghub.facebookads;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FacebookAdsAdSetRepository extends JpaRepository<FacebookAdsAdSet, String> {

    List<FacebookAdsAdSet> findByCampaignIdIn(Collection<String> campaignIds);
}
