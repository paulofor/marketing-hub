package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.CampaignStrategyEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA responsavel pela persistencia das avaliacoes de estrategia de campanha.
 */
public interface CampaignStrategyEvaluationRepository extends JpaRepository<CampaignStrategyEvaluation, Long> {
}
