package com.marketinghub.repository;

import com.marketinghub.model.Lead;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Lead entity.
 */
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    @EntityGraph(attributePaths = {"experiment"})
    @Query("select l from Lead l where l.cpl <= :cpl and l.capturedAt between :start and :end")
    List<Lead> findByCplAndPeriod(@Param("cpl") BigDecimal cpl,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);
}
