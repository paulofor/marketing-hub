package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignPublicationJobStep;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookCampaignPublicationJobStepRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Valida o diagnóstico de publicação de experimentos no Facebook Ads.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentDiagnosticsServiceTest {

    @Mock
    private ExperimentService experimentService;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private FacebookAdsAdSetRepository adSetRepository;
    @Mock
    private FacebookAdsAdRepository adRepository;
    @Mock
    private ExperimentFacebookApiLogService facebookApiLogService;
    @Mock
    private FacebookCampaignPublicationJobStepRepository publicationJobStepRepository;

    @InjectMocks
    private ExperimentDiagnosticsService service;

    /**
     * Garante que campanhas legadas com ID Meta no campo interno não sejam tratadas como pendentes.
     */
    @Test
    void shouldNotFlagPendingWhenLegacyMetaIdIsStoredInIdField() {
        Long experimentId = 10L;

        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setStatus(ExperimentStatus.FAILED);

        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("1202334455667788");
        campaign.setExternalId(null);
        campaign.setName("Campanha legado");

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(campaignRepository.findByExperimentId(experimentId)).thenReturn(List.of(campaign));
        when(adSetRepository.findByCampaignIdIn(List.of(campaign.getId()))).thenReturn(List.of());
        when(publicationJobStepRepository.findTopByExperimentIdAndErrorMessageIsNotNullOrderByOccurredAtDescIdDesc(experimentId))
                .thenReturn(Optional.empty());
        when(facebookApiLogService.findLogs(experimentId, 200)).thenReturn(List.of());

        ExperimentDiagnosticsDto diagnostics = service.diagnose(experimentId);

        assertThat(diagnostics.artifacts()).isEmpty();
        assertThat(diagnostics.headline()).isEqualTo("Experimento está marcado como FAILED");
    }

    /**
     * Impede que uma falha antiga continue aparecendo quando tentativas mais recentes já tiveram sucesso.
     */
    @Test
    void shouldHideHistoricalFailureWhenMostRecentMetaCallSucceeded() {
        Long experimentId = 38L;

        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setStatus(ExperimentStatus.FAILED);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(campaignRepository.findByExperimentId(experimentId)).thenReturn(List.of());
        when(publicationJobStepRepository.findTopByExperimentIdAndErrorMessageIsNotNullOrderByOccurredAtDescIdDesc(experimentId))
                .thenReturn(Optional.empty());
        when(facebookApiLogService.findLogs(experimentId, 200)).thenReturn(List.of(
                apiLog(1L, 400, "400 Bad Request", Instant.parse("2026-06-08T20:31:25Z")),
                apiLog(2L, 200, null, Instant.parse("2026-06-09T03:24:53Z"))
        ));

        ExperimentDiagnosticsDto diagnostics = service.diagnose(experimentId);

        assertThat(diagnostics.failureDetails()).isNull();
        assertThat(diagnostics.headline()).isEqualTo("Experimento está marcado como FAILED");
    }

    /**
     * Mantém o detalhe da falha quando a chamada mais recente da Meta é realmente uma falha.
     */
    @Test
    void shouldShowFailureDetailsWhenMostRecentMetaCallFailed() {
        Long experimentId = 39L;

        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setStatus(ExperimentStatus.FAILED);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(campaignRepository.findByExperimentId(experimentId)).thenReturn(List.of());
        when(publicationJobStepRepository.findTopByExperimentIdAndErrorMessageIsNotNullOrderByOccurredAtDescIdDesc(experimentId))
                .thenReturn(Optional.empty());
        when(facebookApiLogService.findLogs(experimentId, 200)).thenReturn(List.of(
                apiLog(1L, 200, null, Instant.parse("2026-06-09T03:20:00Z")),
                apiLog(2L, 400, "400 Bad Request", Instant.parse("2026-06-09T03:24:53Z"))
        ));

        ExperimentDiagnosticsDto diagnostics = service.diagnose(experimentId);

        assertThat(diagnostics.failureDetails()).isNotNull();
        assertThat(diagnostics.failureDetails().message()).isEqualTo("400 Bad Request");
        assertThat(diagnostics.failureDetails().occurredAt()).isEqualTo(Instant.parse("2026-06-09T03:24:53Z"));
    }


    /**
     * Mostra a falha operacional do job quando a Meta respondeu 200, mas o worker bloqueou por regra de alcance.
     */
    @Test
    void shouldShowPublicationJobFailureWhenReachValidationBlockedAfterSuccessfulMetaCall() {
        Long experimentId = 39L;

        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setStatus(ExperimentStatus.FAILED);

        FacebookCampaignPublicationJobStep step = new FacebookCampaignPublicationJobStep();
        step.setErrorMessage("Público pequeno demais para publicar: a Meta estimou 1.000 a 1.000 pessoas.");
        step.setProvider("FACEBOOK_WORKER");
        step.setStepName("CAMPAIGN_REACH_VALIDATION_BLOCKED");
        step.setOccurredAt(Instant.parse("2026-06-17T00:54:48Z"));

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(campaignRepository.findByExperimentId(experimentId)).thenReturn(List.of());
        when(publicationJobStepRepository.findTopByExperimentIdAndErrorMessageIsNotNullOrderByOccurredAtDescIdDesc(experimentId))
                .thenReturn(Optional.of(step));

        ExperimentDiagnosticsDto diagnostics = service.diagnose(experimentId);

        assertThat(diagnostics.failureDetails()).isNotNull();
        assertThat(diagnostics.failureDetails().message()).contains("Público pequeno demais");
        assertThat(diagnostics.failureDetails().source()).isEqualTo("FACEBOOK_WORKER");
        assertThat(diagnostics.failureDetails().occurredAt()).isEqualTo(Instant.parse("2026-06-17T00:54:48Z"));
    }

    /**
     * Cria um DTO mínimo de log da API Meta para exercitar a ordenação do diagnóstico.
     */
    private static ExperimentFacebookApiLogDto apiLog(Long id, Integer statusCode, String errorMessage, Instant respondedAt) {
        return new ExperimentFacebookApiLogDto(
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                "CAMPAIGN_AD_SET",
                "FACEBOOK",
                "/v23.0/act_939323521124952/adimages",
                "POST",
                statusCode,
                errorMessage,
                respondedAt.minusSeconds(1),
                respondedAt,
                1000L,
                null,
                null,
                respondedAt.minusSeconds(2)
        );
    }
}
