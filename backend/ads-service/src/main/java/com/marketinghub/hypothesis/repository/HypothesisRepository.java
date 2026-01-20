package com.marketinghub.hypothesis.repository;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

import java.util.UUID;

public interface HypothesisRepository extends JpaRepository<Hypothesis, UUID> {
    List<Hypothesis> findByMarketNicheId(Long marketNicheId);
    List<Hypothesis> findByMarketNicheIdAndStatus(Long marketNicheId, HypothesisStatus status);
    List<Hypothesis> findByStatus(HypothesisStatus status);

    @Modifying
    @Query("""
            update Hypothesis h
            set h.totalCost = coalesce(h.totalCost, 0) + :delta
            where h.id = :id
            """)
    void incrementTotalCost(@Param("id") UUID id, @Param("delta") BigDecimal delta);
}
