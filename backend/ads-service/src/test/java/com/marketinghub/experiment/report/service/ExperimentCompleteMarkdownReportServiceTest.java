package com.marketinghub.experiment.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCaptureDestinationType;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a montagem do relatório completo em Markdown do experimento. */
@ExtendWith(MockitoExtension.class)
class ExperimentCompleteMarkdownReportServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;

    @Mock
    private FacebookAdsCampaignRepository facebookAdsCampaignRepository;

    @Mock
    private ExperimentReportMaterialService materialService;

    private ExperimentCompleteMarkdownReportService service;

    /** Garante que Instant Form não seja relatado como analytics comportamental de landing. */
    @Test
    void shouldDescribeLandingAnalyticsAsNotApplicableForMetaInstantForm() {
        service = new ExperimentCompleteMarkdownReportService(
                experimentRepository,
                geraLandingStageExecutionRepository,
                facebookAdsCampaignRepository,
                materialService,
                new ObjectMapper());
        Experiment experiment = Experiment.builder()
                .id(77L)
                .name("Experimento encerrado com formulário Meta")
                .status(ExperimentStatus.VALIDATED)
                .captureDestinationType(ExperimentCaptureDestinationType.META_INSTANT_FORM)
                .build();

        when(experimentRepository.findById(77L)).thenReturn(Optional.of(experiment));
        when(materialService.build(77L)).thenReturn(ExperimentReportMaterialDto.builder().build());
        when(geraLandingStageExecutionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(77L))
                .thenReturn(List.of());
        when(facebookAdsCampaignRepository.findDetailedByExperimentId(77L)).thenReturn(List.of());

        String markdown = service.buildMarkdown(77L);

        assertThat(markdown).contains("Destino de captura: META_INSTANT_FORM");
        assertThat(markdown).contains("Analytics comportamental da landing não se aplica");
    }
}
