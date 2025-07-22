package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {
    java.util.List<LandingPage> findByExperimentId(Long experimentId);
}
