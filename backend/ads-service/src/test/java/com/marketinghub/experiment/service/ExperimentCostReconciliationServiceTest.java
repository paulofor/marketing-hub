package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.finance.CurrencyConversionProperties;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.repository.jpa.experiment.pipeline.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a reconciliação de custos rastreáveis enviada para a tela de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentCostReconciliationServiceTest {
  @Mock private ExperimentPipelineGenerationJobRepository pipelineJobRepository;

  @Mock private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;

  @Mock
  private GeraSalesPagePublicationStageAuditRepository geraSalesPagePublicationStageAuditRepository;

  @Mock private ExperimentVideoAssetRepository experimentVideoAssetRepository;

  private ExperimentCostReconciliationService service;

  /** Monta o serviço com a conversão canônica de USD para BRL. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentCostReconciliationService(
            new CurrencyConversionService(new CurrencyConversionProperties()),
            pipelineJobRepository,
            geraLandingStageExecutionRepository,
            geraSalesPagePublicationStageAuditRepository,
            experimentVideoAssetRepository);
  }

  /** Garante que o custo de vídeo auditável entra no total exibido para o experimento 69. */
  @Test
  void enrichAddsVideoProviderCostToAuditableTotal() {
    ExperimentCampaignMetric metric =
        ExperimentCampaignMetric.builder().spend(new BigDecimal("5.37")).build();
    Experiment experiment =
        Experiment.builder()
            .id(69L)
            .totalCost(new BigDecimal("12.18"))
            .campaignMetric(metric)
            .build();
    ExperimentDto dto = new ExperimentDto();

    when(pipelineJobRepository.sumCostUsdByExperimentId(69L)).thenReturn(BigDecimal.ZERO);
    when(geraLandingStageExecutionRepository.sumCompletedCostUsdByExperimentId(69L))
        .thenReturn(BigDecimal.ZERO);
    when(geraSalesPagePublicationStageAuditRepository.sumCostUsdByExperimentId(69L))
        .thenReturn(BigDecimal.ZERO);
    when(experimentVideoAssetRepository.sumCostUsdByExperimentId(69L))
        .thenReturn(new BigDecimal("5.90"));

    ExperimentDto result = service.enrich(experiment, dto);

    assertThat(result.getAuditableTotalCost()).isEqualByComparingTo("34.87");
    assertThat(result.getTotalCost()).isEqualByComparingTo("34.87");
    assertThat(result.getLegacyTotalCost()).isEqualByComparingTo("12.18");
    assertThat(result.getUnreconciledLegacyCost()).isEqualByComparingTo("0.00");
  }
}
