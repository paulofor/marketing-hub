package com.marketinghub.niche.repository;

import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JPA repository for {@link MarketNiche} entities.
 */
public interface MarketNicheRepository extends JpaRepository<MarketNiche, Long> {
    /**
     * Retrieves only niches that still need hypotheses to be generated
     * ({@code hypothesesToGenerate > 0}).
     */
    @Query("select n from MarketNiche n where n.hypothesesToGenerate is not null and n.hypothesesToGenerate > 0")
    List<MarketNiche> findAllToGenerateHypotheses();
}
