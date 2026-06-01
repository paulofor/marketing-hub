package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsMediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsMediaAsset.
 */
public interface FacebookAdsMediaAssetRepository extends JpaRepository<FacebookAdsMediaAsset, String> {
}
