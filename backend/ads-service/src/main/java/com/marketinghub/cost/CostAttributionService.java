package com.marketinghub.cost;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Centralizes how generation and campaign costs cascade across experiment, hypothesis and niche.
 */
@Service
public class CostAttributionService {
    private final ExperimentRepository experimentRepository;
    private final HypothesisRepository hypothesisRepository;
    private final MarketNicheRepository marketNicheRepository;
    private final CurrencyConversionService currencyConversionService;

    public CostAttributionService(ExperimentRepository experimentRepository,
                                  HypothesisRepository hypothesisRepository,
                                  MarketNicheRepository marketNicheRepository,
                                  CurrencyConversionService currencyConversionService) {
        this.experimentRepository = experimentRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.marketNicheRepository = marketNicheRepository;
        this.currencyConversionService = currencyConversionService;
    }

    public void addUsdCostToExperimentHierarchy(Experiment experiment, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToExperimentHierarchy(experiment, delta);
    }

    public void addCostToExperimentHierarchy(Experiment experiment, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || experiment == null) {
            return;
        }
        experiment.setTotalCost(add(experiment.getTotalCost(), delta));
        if (experiment.getId() != null) {
            experimentRepository.incrementTotalCost(experiment.getId(), delta);
        }
        addCostToHypothesisHierarchy(experiment.getHypothesisRef(), delta);
    }

    public void addUsdCostToHypothesisHierarchy(Hypothesis hypothesis, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToHypothesisHierarchy(hypothesis, delta);
    }

    public void addCostToHypothesisHierarchy(Hypothesis hypothesis, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || hypothesis == null) {
            return;
        }
        hypothesis.setTotalCost(add(hypothesis.getTotalCost(), delta));
        if (hypothesis.getId() != null) {
            hypothesisRepository.incrementTotalCost(hypothesis.getId(), delta);
        }
        addCostToNiche(hypothesis.getMarketNiche(), delta);
    }

    public void addUsdCostToNiche(MarketNiche niche, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToNiche(niche, delta);
    }

    public void addCostToNiche(MarketNiche niche, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || niche == null) {
            return;
        }
        niche.setTotalCost(add(niche.getTotalCost(), delta));
        if (niche.getId() != null) {
            marketNicheRepository.incrementTotalCost(niche.getId(), delta);
        }
    }

    private boolean hasDelta(BigDecimal delta) {
        return delta != null && delta.compareTo(BigDecimal.ZERO) != 0;
    }

    private BigDecimal add(BigDecimal current, BigDecimal delta) {
        if (current == null) {
            return delta;
        }
        return current.add(delta);
    }
}
