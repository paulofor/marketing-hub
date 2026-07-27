package com.marketinghub.repository.jpa.media;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for {@link Asset} entities. */
public interface AssetRepository extends JpaRepository<Asset, Long> {
  List<Asset> findByStatus(AssetStatus status);

  List<Asset> findByStatusAndCampaignId(AssetStatus status, Long campaignId);

  List<Asset> findByUrlIn(List<String> urls);
}
