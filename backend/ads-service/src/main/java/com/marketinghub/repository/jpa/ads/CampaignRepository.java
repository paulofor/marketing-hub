package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de Campaign.
 */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByFacebookAccountIdOrInstagramAccountId(Long facebookAccountId, Long instagramAccountId);
}
