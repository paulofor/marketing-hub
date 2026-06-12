package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Mantém as métricas agregadas de campanha e propaga o custo de mídia para o experimento.
 */
@Service
public class ExperimentCampaignMetricService {
    private final ExperimentCampaignMetricRepository repository;
    private final FacebookAdsCampaignRepository campaignRepository;
    private final CostAttributionService costAttributionService;

    /**
     * Inicializa o serviço com repositórios de campanha, métricas e atribuição de custo.
     */
    public ExperimentCampaignMetricService(ExperimentCampaignMetricRepository repository,
                                           FacebookAdsCampaignRepository campaignRepository,
                                           CostAttributionService costAttributionService) {
        this.repository = repository;
        this.campaignRepository = campaignRepository;
        this.costAttributionService = costAttributionService;
    }

    /**
     * Cria ou atualiza as métricas sincronizadas da campanha Facebook de um experimento.
     */
    @Transactional
    public ExperimentCampaignMetric upsert(String campaignId,
                                           LocalDate dateStart,
                                           LocalDate dateStop,
                                           Long reach,
                                           Long impressions,
                                           Long clicks,
                                           Long leads,
                                           BigDecimal spend) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Facebook campaign not found: " + campaignId));
        Experiment experiment = campaign.getExperiment();
        BigDecimal normalizedSpend = spend != null ? spend.setScale(2, RoundingMode.HALF_UP) : null;
        ExperimentCampaignMetric metric = repository.findByExperiment(experiment)
                .orElseGet(() -> ExperimentCampaignMetric.builder()
                        .experiment(experiment)
                        .build());
        BigDecimal previousSpend = metric.getSpend();
        metric.setCampaign(campaign);
        metric.setDateStart(dateStart);
        metric.setDateStop(dateStop);
        metric.setReach(reach);
        metric.setImpressions(impressions);
        metric.setClicks(clicks);
        metric.setLeads(leads);
        metric.setSpend(normalizedSpend);
        metric.setCpc(calculateCpc(normalizedSpend, clicks));
        metric.setCpl(calculateCpl(normalizedSpend, leads));
        ExperimentCampaignMetric saved = repository.save(metric);
        applySpendDelta(experiment, normalizedSpend, previousSpend);
        return saved;
    }

    /**
     * Calcula o custo por clique a partir do gasto e dos cliques sincronizados.
     */
    private BigDecimal calculateCpc(BigDecimal spend, Long clicks) {
        if (spend == null || clicks == null || clicks == 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o custo por lead a partir do gasto e dos leads sincronizados.
     */
    private BigDecimal calculateCpl(BigDecimal spend, Long leads) {
        if (spend == null || leads == null || leads == 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica ao experimento apenas a diferença entre o gasto novo e o gasto anterior.
     */
    private void applySpendDelta(Experiment experiment, BigDecimal newSpend, BigDecimal previousSpend) {
        if (experiment == null) {
            return;
        }
        BigDecimal current = newSpend == null ? BigDecimal.ZERO : newSpend;
        BigDecimal previous = previousSpend == null ? BigDecimal.ZERO : previousSpend;
        BigDecimal delta = current.subtract(previous);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        costAttributionService.addCostToExperimentHierarchy(experiment, delta);
    }
}
