package com.marketinghub.niche.description.repository;

import com.marketinghub.niche.description.NicheDetailedDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NicheDetailedDescriptionRepository extends JpaRepository<NicheDetailedDescription, Long> {
    List<NicheDetailedDescription> findByMarketNicheId(Long nicheId);
}
