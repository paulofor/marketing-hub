package com.marketinghub.facebookads;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FacebookAdsAdRepository extends JpaRepository<FacebookAdsAd, String> {

    List<FacebookAdsAd> findByAdSetIdIn(Collection<String> adSetIds);
}
