package com.marketinghub.hypothesis.repository;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;

public interface HypothesisRepository extends JpaRepository<Hypothesis, UUID> {
    List<Hypothesis> findByExperimentId(Long experimentId);
    List<Hypothesis> findByExperimentIdAndStatus(Long experimentId, HypothesisStatus status);
    List<Hypothesis> findByStatus(HypothesisStatus status);
}
