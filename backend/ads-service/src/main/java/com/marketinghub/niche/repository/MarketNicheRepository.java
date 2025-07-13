package com.marketinghub.niche.repository;

import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link MarketNiche} entities.
 */
public interface MarketNicheRepository extends JpaRepository<MarketNiche, Long> {}
