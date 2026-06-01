package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookAdsAd;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de FacebookAdsAd.
 */
public interface FacebookAdsAdRepository extends JpaRepository<FacebookAdsAd, String> {

    List<FacebookAdsAd> findByAdSetIdIn(Collection<String> adSetIds);
}
