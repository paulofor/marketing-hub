package com.marketinghub.hypothesis.repository;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HypothesisRepository extends JpaRepository<Hypothesis, Long> {
    List<Hypothesis> findByExperimentId(Long experimentId);
    List<Hypothesis> findByExperimentIdAndStatus(Long experimentId, HypothesisStatus status);
}
