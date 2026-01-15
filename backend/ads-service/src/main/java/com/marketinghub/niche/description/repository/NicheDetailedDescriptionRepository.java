package com.marketinghub.niche.description.repository;

import com.marketinghub.niche.description.NicheDetailedDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NicheDetailedDescriptionRepository extends JpaRepository<NicheDetailedDescription, Long> {
    List<NicheDetailedDescription> findByMarketNicheId(Long nicheId);

    List<NicheDetailedDescription> findByMarketNicheIdAndActiveTrue(Long nicheId);

    Optional<NicheDetailedDescription> findByIdAndMarketNicheId(Long id, Long nicheId);

    Optional<NicheDetailedDescription> findByIdAndMarketNicheIdAndActiveTrue(Long id, Long nicheId);
}
