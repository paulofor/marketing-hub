package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.repository.jpa.experiment.pipeline.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Consolida custos rastreáveis do experimento para a interface administrativa. */
@Service
public class ExperimentCostReconciliationService {
  private final CurrencyConversionService currencyConversionService;
  private final ExperimentPipelineGenerationJobRepository pipelineJobRepository;
  private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
  private final GeraSalesPagePublicationStageAuditRepository
      geraSalesPagePublicationStageAuditRepository;
  private final ExperimentVideoAssetRepository experimentVideoAssetRepository;

  /** Inicializa o serviço com fontes auditáveis de custo técnico e financeiro. */
  public ExperimentCostReconciliationService(
      CurrencyConversionService currencyConversionService,
      ExperimentPipelineGenerationJobRepository pipelineJobRepository,
      GeraLandingStageExecutionRepository geraLandingStageExecutionRepository,
      GeraSalesPagePublicationStageAuditRepository geraSalesPagePublicationStageAuditRepository,
      ExperimentVideoAssetRepository experimentVideoAssetRepository) {
    this.currencyConversionService = currencyConversionService;
    this.pipelineJobRepository = pipelineJobRepository;
    this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
    this.geraSalesPagePublicationStageAuditRepository =
        geraSalesPagePublicationStageAuditRepository;
    this.experimentVideoAssetRepository = experimentVideoAssetRepository;
  }

  /** Preenche no DTO o total rastreável e a diferença frente ao legado persistido. */
  public ExperimentDto enrich(Experiment experiment, ExperimentDto dto) {
    if (experiment == null || dto == null || experiment.getId() == null) {
      return dto;
    }
    BigDecimal baseBrl =
        money(experiment.getCost())
            .add(money(experiment.getExpense()))
            .add(
                money(
                    experiment.getCampaignMetric() != null
                        ? experiment.getCampaignMetric().getSpend()
                        : null));
    BigDecimal technicalCostBrl =
        currencyConversionService.usdToBrl(totalTechnicalCostUsd(experiment.getId()));
    BigDecimal auditableTotal = round(baseBrl.add(money(technicalCostBrl)));
    BigDecimal legacyTotal = round(money(experiment.getTotalCost()));
    BigDecimal visibleTotal =
        auditableTotal.compareTo(BigDecimal.ZERO) > 0 ? auditableTotal : legacyTotal;
    BigDecimal unreconciled = legacyTotal.subtract(auditableTotal);

    dto.setAuditableTotalCost(auditableTotal);
    dto.setLegacyTotalCost(legacyTotal);
    dto.setUnreconciledLegacyCost(
        unreconciled.compareTo(BigDecimal.ZERO) > 0 ? unreconciled : BigDecimal.ZERO);
    dto.setTotalCost(visibleTotal);
    return dto;
  }

  /** Soma custos técnicos em USD persistidos nas etapas e ativos do experimento. */
  private BigDecimal totalTechnicalCostUsd(Long experimentId) {
    return money(pipelineJobRepository.sumCostUsdByExperimentId(experimentId))
        .add(
            money(
                geraLandingStageExecutionRepository.sumCompletedCostUsdByExperimentId(
                    experimentId)))
        .add(
            money(
                geraSalesPagePublicationStageAuditRepository.sumCostUsdByExperimentId(
                    experimentId)))
        .add(money(experimentVideoAssetRepository.sumCostUsdByExperimentId(experimentId)));
  }

  /** Converte valores nulos para zero para permitir soma segura. */
  private BigDecimal money(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  /** Normaliza valores monetários para duas casas decimais. */
  private BigDecimal round(BigDecimal value) {
    return money(value).setScale(2, RoundingMode.HALF_UP);
  }
}
