package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA responsável pela persistência das sugestões de campanhas Facebook Ads.
 */
public interface FacebookAdsRecommendationRepository extends JpaRepository<FacebookAdsRecommendation, Long> {

    /**
     * Remove as sugestões antigas de uma campanha antes de gravar o novo retrato da Meta.
     */
    @Modifying
    @Query("delete from FacebookAdsRecommendation r where r.campaign.id = :campaignId")
    void deleteByCampaignId(@Param("campaignId") String campaignId);

    /**
     * Lista as sugestões persistidas para uma campanha.
     */
    @Query("select r from FacebookAdsRecommendation r where r.campaign.id = :campaignId order by r.collectedAt desc, r.id asc")
    List<FacebookAdsRecommendation> findByCampaignIdOrderByCollectedAtDescIdAsc(@Param("campaignId") String campaignId);
}
