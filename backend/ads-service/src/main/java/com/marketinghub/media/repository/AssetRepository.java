package com.marketinghub.media.repository;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link Asset} entities.
 */
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByStatus(AssetStatus status);
    List<Asset> findByStatusAndCampaignId(AssetStatus status, Long campaignId);
}
