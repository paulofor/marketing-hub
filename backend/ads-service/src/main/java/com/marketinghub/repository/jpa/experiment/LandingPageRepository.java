package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de LandingPage.
 */
public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {
    java.util.List<LandingPage> findByExperimentId(Long experimentId);
}
