package com.marketinghub.repository.jpa.niche.description;

import com.marketinghub.niche.description.NicheDetailedDescription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de NicheDetailedDescription. */
public interface NicheDetailedDescriptionRepository
    extends JpaRepository<NicheDetailedDescription, Long> {
  List<NicheDetailedDescription> findByMarketNicheId(Long nicheId);

  List<NicheDetailedDescription> findByMarketNicheIdAndActiveTrue(Long nicheId);

  Optional<NicheDetailedDescription> findByIdAndMarketNicheId(Long id, Long nicheId);

  Optional<NicheDetailedDescription> findByIdAndMarketNicheIdAndActiveTrue(Long id, Long nicheId);
}
