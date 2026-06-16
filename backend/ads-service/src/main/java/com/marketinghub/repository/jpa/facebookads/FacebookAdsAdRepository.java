package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsAd;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsAd.
 */
public interface FacebookAdsAdRepository extends JpaRepository<FacebookAdsAd, String> {

    List<FacebookAdsAd> findByAdSetIdIn(Collection<String> adSetIds);

    /**
     * Remove anúncios vinculados às campanhas informadas antes de reprocessar a publicação.
     */
    @Modifying
    void deleteByAdSetCampaignIdIn(Collection<String> campaignIds);
}
