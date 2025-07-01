package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByFacebookAccountId(Long accountId);
    List<Campaign> findByInstagramAccountId(Long accountId);
}
