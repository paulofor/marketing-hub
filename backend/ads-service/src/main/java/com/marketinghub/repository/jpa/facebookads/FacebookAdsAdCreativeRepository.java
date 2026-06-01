package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsAdCreative;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsAdCreative.
 */
public interface FacebookAdsAdCreativeRepository extends JpaRepository<FacebookAdsAdCreative, String> {
}
