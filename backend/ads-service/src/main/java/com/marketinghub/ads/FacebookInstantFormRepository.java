package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FacebookInstantFormRepository extends JpaRepository<FacebookInstantForm, Long> {
    List<FacebookInstantForm> findByHypothesisId(UUID hypothesisId);

    List<FacebookInstantForm> findByApprovedTrueAndPublishedFalse();
}
