package com.marketinghub.cost;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 * Centraliza como custos de geração e campanha sobem para experimento, hipótese e nicho.
 */
@Service
public class CostAttributionService {
    private final ExperimentRepository experimentRepository;
    private final HypothesisRepository hypothesisRepository;
    private final MarketNicheRepository marketNicheRepository;
    private final CurrencyConversionService currencyConversionService;

    @PersistenceContext
    private EntityManager entityManager;

    /** Cria o serviço com repositórios de custo e conversão monetária. */
    public CostAttributionService(ExperimentRepository experimentRepository,
                                  HypothesisRepository hypothesisRepository,
                                  MarketNicheRepository marketNicheRepository,
                                  CurrencyConversionService currencyConversionService) {
        this.experimentRepository = experimentRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.marketNicheRepository = marketNicheRepository;
        this.currencyConversionService = currencyConversionService;
    }

    /** Converte custo em dólar para real e propaga na hierarquia do experimento. */
    public void addUsdCostToExperimentHierarchy(Experiment experiment, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToExperimentHierarchy(experiment, delta);
    }

    /** Adiciona custo em real ao experimento e propaga para hipótese e nicho sem duplicar persistência. */
    public void addCostToExperimentHierarchy(Experiment experiment, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || experiment == null) {
            return;
        }
        experiment.setTotalCost(add(experiment.getTotalCost(), delta));
        if (experiment.getId() != null && !isManaged(experiment)) {
            experimentRepository.incrementTotalCost(experiment.getId(), delta);
        }
        addCostToHypothesisHierarchy(experiment.getHypothesisRef(), delta);
    }

    /** Converte custo em dólar para real e propaga na hierarquia da hipótese. */
    public void addUsdCostToHypothesisHierarchy(Hypothesis hypothesis, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToHypothesisHierarchy(hypothesis, delta);
    }

    /** Adiciona custo em real à hipótese e ao nicho sem duplicar persistência. */
    public void addCostToHypothesisHierarchy(Hypothesis hypothesis, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || hypothesis == null) {
            return;
        }
        hypothesis.setTotalCost(add(hypothesis.getTotalCost(), delta));
        if (hypothesis.getId() != null && !isManaged(hypothesis)) {
            hypothesisRepository.incrementTotalCost(hypothesis.getId(), delta);
        }
        addCostToNiche(hypothesis.getMarketNiche(), delta);
    }

    /** Converte custo em dólar para real e adiciona ao nicho. */
    public void addUsdCostToNiche(MarketNiche niche, BigDecimal costUsd) {
        BigDecimal delta = currencyConversionService.usdToBrl(costUsd);
        addCostToNiche(niche, delta);
    }

    /** Adiciona custo em real ao nicho sem duplicar persistência. */
    public void addCostToNiche(MarketNiche niche, BigDecimal costBrl) {
        BigDecimal delta = currencyConversionService.normalizeBrl(costBrl);
        if (!hasDelta(delta) || niche == null) {
            return;
        }
        niche.setTotalCost(add(niche.getTotalCost(), delta));
        if (niche.getId() != null && !isManaged(niche)) {
            marketNicheRepository.incrementTotalCost(niche.getId(), delta);
        }
    }

    /** Verifica se há variação financeira relevante para persistir. */
    private boolean hasDelta(BigDecimal delta) {
        return delta != null && delta.compareTo(BigDecimal.ZERO) != 0;
    }

    /** Soma valores aceitando acumulado nulo. */
    private BigDecimal add(BigDecimal current, BigDecimal delta) {
        if (current == null) {
            return delta;
        }
        return current.add(delta);
    }

    /** Identifica entidades gerenciadas para evitar update SQL além do flush do JPA. */
    private boolean isManaged(Object entity) {
        return entityManager != null && entityManager.contains(entity);
    }
}
