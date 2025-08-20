package com.marketinghub.worker;

import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerMarketNicheRepository extends JpaRepository<MarketNiche, Long> {
    List<MarketNiche> findByHypothesesToGenerateGreaterThan(int quantity);
}

