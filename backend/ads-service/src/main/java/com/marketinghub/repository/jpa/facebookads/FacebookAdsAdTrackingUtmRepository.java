package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsAdTrackingUtm;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsAdTrackingUtm.
 */
public interface FacebookAdsAdTrackingUtmRepository extends JpaRepository<FacebookAdsAdTrackingUtm, String> {
}
