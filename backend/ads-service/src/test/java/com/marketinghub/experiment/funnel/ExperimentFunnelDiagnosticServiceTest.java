package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Testa os diagnósticos estatísticos das transições do funil de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelDiagnosticServiceTest {

    @Mock
    private ExperimentFunnelService funnelService;

    private ExperimentFunnelDiagnosticService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentFunnelDiagnosticService(funnelService, new ExperimentFunnelDiagnosticConfig());
    }

    @Test
    void marksStageAsStatisticallyFailedWhenRuleOfThreeDropsBelowMinimumRate() {
        when(funnelService.summarize(10L)).thenReturn(stageList(
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 40),
                stage(ExperimentFunnelStage.ENVIO_FORM, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)
        ));

        ExperimentFunnelDiagnosticsResponseDto response = service.diagnose(10L);

        assertThat(response.diagnostics())
                .anySatisfy(item -> {
                    if (item.stageKey() == ExperimentFunnelStage.ENVIO_FORM) {
                        assertThat(item.status()).isEqualTo(FunnelDiagnosticStatus.STATISTICALLY_FAILED);
                        assertThat(item.upper95RateIfZero()).isLessThanOrEqualTo(item.minAcceptableRate());
                        assertThat(item.thresholdChecks())
                                .extracting(check -> check.minAcceptableRate())
                                .containsExactly(0.10, 0.05, 0.03, 0.02);
                    }
                });
    }

    @Test
    void marksTechnicalIssueWhenSuccessesAreHigherThanAttempts() {
        when(funnelService.summarize(20L)).thenReturn(stageList(
                stage(ExperimentFunnelStage.ENVIO_FORM, 5),
                stage(ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA, 8),
                stage(ExperimentFunnelStage.COMPRA, 0)
        ));

        ExperimentFunnelDiagnosticsResponseDto response = service.diagnose(20L);

        assertThat(response.diagnostics())
                .anySatisfy(item -> {
                    if (item.stageKey() == ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA) {
                        assertThat(item.status()).isEqualTo(FunnelDiagnosticStatus.TECHNICAL_ISSUE_SUSPECTED);
                        assertThat(item.technicalIssueSuspected()).isTrue();
                    }
                });
    }

    @Test
    void marksInsufficientDataWhenStillBelowMinimumAttemptsForRuleOfThree() {
        when(funnelService.summarize(30L)).thenReturn(stageList(
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 12),
                stage(ExperimentFunnelStage.ENVIO_FORM, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)
        ));

        ExperimentFunnelDiagnosticsResponseDto response = service.diagnose(30L);

        assertThat(response.diagnostics())
                .anySatisfy(item -> {
                    if (item.stageKey() == ExperimentFunnelStage.ENVIO_FORM) {
                        assertThat(item.status()).isEqualTo(FunnelDiagnosticStatus.INSUFFICIENT_DATA);
                    }
                });
        assertThat(response.contextualAlert()).contains("Alerta contextual");
    }

    /**
     * Garante reprovação estatística quando a taxa de acesso ao formulário fica abaixo do mínimo com confiança.
     */
    @Test
    void marksAdInterestAsStatisticallyFailedWhenAccessRateIsBelowMinimumWithConfidence() {
        when(funnelService.summarize(40L)).thenReturn(stageList(
                stage(ExperimentFunnelStage.VISUALIZACAO_ANUNCIO, 1199),
                stage(ExperimentFunnelStage.ACESSO_FORM_LEAD, 6),
                stage(ExperimentFunnelStage.COMPRA, 0)
        ));

        ExperimentFunnelDiagnosticsResponseDto response = service.diagnose(40L);

        assertThat(response.diagnostics())
                .anySatisfy(item -> {
                    if (item.stageKey() == ExperimentFunnelStage.ACESSO_FORM_LEAD) {
                        assertThat(item.status()).isEqualTo(FunnelDiagnosticStatus.STATISTICALLY_FAILED);
                        assertThat(item.observedRate()).isLessThan(item.minAcceptableRate());
                        assertThat(item.upper95RateIfZero()).isLessThanOrEqualTo(item.minAcceptableRate());
                    }
                });
    }

    private List<ExperimentFunnelStageDto> stageList(ExperimentFunnelStageDto... stages) {
        return Arrays.asList(stages);
    }

    private ExperimentFunnelStageDto stage(ExperimentFunnelStage stage, long total) {
        ExperimentFunnelStageDto dto = new ExperimentFunnelStageDto();
        dto.setStage(stage);
        dto.setLabel(stage.getLabel());
        dto.setOrder(stage.getOrder());
        dto.setTotalCount(total);
        return dto;
    }
}
