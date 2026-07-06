package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.CampaignStrategy;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA responsavel pela persistencia das estrategias de campanha.
 */
public interface CampaignStrategyRepository extends JpaRepository<CampaignStrategy, Long> {
    /**
     * Busca a estrategia vinculada a uma campanha.
     */
    Optional<CampaignStrategy> findByCampaign(FacebookAdsCampaign campaign);

    /**
     * Busca a estrategia mais recente da campanha de um experimento.
     */
    Optional<CampaignStrategy> findTopByCampaign_Experiment_IdOrderByCreatedAtDesc(Long experimentId);
}
