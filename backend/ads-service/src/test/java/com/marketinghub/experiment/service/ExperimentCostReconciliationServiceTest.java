package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsReader;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsSnapshot;
import com.marketinghub.finance.CurrencyConversionProperties;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.repository.jpa.experiment.pipeline.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
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

  @Mock private CommercialPlanImageStudioJobRepository imageStudioJobRepository;
  @Mock private ExperimentAcquisitionMetricsReader acquisition;

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
            experimentVideoAssetRepository,
            imageStudioJobRepository,
            acquisition);
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
    when(imageStudioJobRepository.sumCostUsdByExperimentId(69L)).thenReturn(BigDecimal.ZERO);

    ExperimentDto result = service.enrich(experiment, dto);

    assertThat(result.getAuditableTotalCost()).isEqualByComparingTo("34.87");
    assertThat(result.getTotalCost()).isEqualByComparingTo("34.87");
    assertThat(result.getLegacyTotalCost()).isEqualByComparingTo("12.18");
    assertThat(result.getUnreconciledLegacyCost()).isEqualByComparingTo("0.00");
  }

  /** Inclui até as artes reprovadas de Íris porque seu custo já foi efetivamente consumido. */
  @Test
  void enrichAddsImageStudioCostToAuditableTotal() {
    Experiment experiment = Experiment.builder().id(94L).build();
    ExperimentDto dto = new ExperimentDto();

    when(pipelineJobRepository.sumCostUsdByExperimentId(94L)).thenReturn(BigDecimal.ZERO);
    when(geraLandingStageExecutionRepository.sumCompletedCostUsdByExperimentId(94L))
        .thenReturn(BigDecimal.ZERO);
    when(geraSalesPagePublicationStageAuditRepository.sumCostUsdByExperimentId(94L))
        .thenReturn(BigDecimal.ZERO);
    when(experimentVideoAssetRepository.sumCostUsdByExperimentId(94L)).thenReturn(BigDecimal.ZERO);
    when(imageStudioJobRepository.sumCostUsdByExperimentId(94L))
        .thenReturn(new BigDecimal("13.5787"));

    ExperimentDto result = service.enrich(experiment, dto);

    assertThat(result.getAuditableTotalCost()).isEqualByComparingTo("67.89");
    assertThat(result.getTotalCost()).isEqualByComparingTo("67.89");
  }

  /** Usa gasto do canal exato uma vez e não apresenta contribuição quando falta conciliação. */
  @Test
  void reconcilesChatgptSpendAndKeepsUnknownCostsUnknown() {
    var experiment =
        Experiment.builder()
            .id(194L)
            .platform(ExperimentPlatform.CHATGPT_ADS)
            .cost(new BigDecimal("2.00"))
            .expense(new BigDecimal("1.00"))
            .campaignMetric(ExperimentCampaignMetric.builder().spend(new BigDecimal("999")).build())
            .build();
    when(acquisition.read(experiment))
        .thenReturn(
            new ExperimentAcquisitionMetricsSnapshot(
                ExperimentPlatform.CHATGPT_ADS,
                new BigDecimal("4.50"),
                java.time.Instant.now(),
                null,
                null,
                100L,
                3L,
                true));
    assertThat(service.enrich(experiment, new ExperimentDto()).getAuditableTotalCost())
        .isEqualByComparingTo("7.50");
    when(acquisition.read(experiment))
        .thenReturn(
            new ExperimentAcquisitionMetricsSnapshot(
                ExperimentPlatform.CHATGPT_ADS,
                null,
                java.time.Instant.now(),
                null,
                "Câmbio pendente",
                100L,
                3L,
                true));
    var result = service.enrich(experiment, new ExperimentDto());
    assertThat(result.getAuditableTotalCost()).isNull();
    assertThat(result.getTotalCost()).isNull();
  }
}
