package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.Campaign;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de Campaign. */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
  List<Campaign> findByFacebookAccountIdOrInstagramAccountId(
      Long facebookAccountId, Long instagramAccountId);
}
