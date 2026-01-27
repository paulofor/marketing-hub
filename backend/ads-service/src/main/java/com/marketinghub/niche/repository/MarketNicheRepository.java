package com.marketinghub.niche.repository;

import com.marketinghub.niche.MarketNiche;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            left join fetch n.differentiatedTechnology
            where n.hypothesesToGenerate is not null
              and n.hypothesesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateHypotheses();

    /**
     * Retrieves niches configured to generate interests.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.interestsToGenerate is not null
              and n.interestsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateInterests();

    /**
     * Retrieves niches configured to generate job titles.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.jobTitlesToGenerate is not null
              and n.jobTitlesToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateJobTitles();

    /**
     * Retrieves niches configured to generate behaviors.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.behaviorsToGenerate is not null
              and n.behaviorsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateBehaviors();

    /**
     * Retrieves niches configured to generate detailed descriptions.
     */
    @Query("""
            select distinct n from MarketNiche n
            left join fetch n.differentiatedTechnology
            where n.detailedDescriptionsToGenerate is not null
              and n.detailedDescriptionsToGenerate > 0
            """)
    List<MarketNiche> findAllToGenerateDetailedDescriptions();

    /**
     * Increments the total cost accumulated for a niche.
     */
    @Modifying
    @Query("""
            update MarketNiche n
            set n.totalCost = coalesce(n.totalCost, 0) + :delta
            where n.id = :id
            """)
    void incrementTotalCost(@Param("id") Long id, @Param("delta") BigDecimal delta);

}
