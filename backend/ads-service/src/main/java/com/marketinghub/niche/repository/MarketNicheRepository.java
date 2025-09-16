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
     * Retrieves niches configured to generate hypotheses.
     *
     * <p>Filters are handled in the query so we only fetch the records we
     * actually need.</p>
     */
    @Query("""
            select n from MarketNiche n
            where n.hypothesesToGenerate is not null
              and n.hypothesesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateHypotheses();

    /**
     * Retrieves niches configured to generate audiences.
     */
    @Query("""
            select n from MarketNiche n
            where n.audiencesToGenerate is not null
              and n.audiencesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateAudiences();
}
