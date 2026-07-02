package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Testa os diagnósticos estatísticos das transições do funil de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelDiagnosticServiceTest {

    @Mock
    private ExperimentFunnelService funnelService;
    @Mock
    private ExperimentRepository experimentRepository;

    private ExperimentFunnelDiagnosticService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentFunnelDiagnosticService(funnelService, new ExperimentFunnelDiagnosticConfig(), experimentRepository);
        lenient().when(experimentRepository.findById(10L)).thenReturn(Optional.of(Experiment.builder().id(10L).build()));
        lenient().when(experimentRepository.findById(20L)).thenReturn(Optional.of(Experiment.builder().id(20L).build()));
        lenient().when(experimentRepository.findById(30L)).thenReturn(Optional.of(Experiment.builder().id(30L).build()));
        lenient().when(experimentRepository.findById(40L)).thenReturn(Optional.of(Experiment.builder().id(40L).build()));
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

    /** Garante que venda low-ticket diagnostica página de venda e checkout, sem formulário. */
    @Test
    void usesDirectSalesRulesForLowTicketProduct() {
        when(experimentRepository.findById(50L)).thenReturn(Optional.of(Experiment.builder()
                .id(50L)
                .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
                .build()));
        when(funnelService.summarize(50L)).thenReturn(stageList(
                stage(ExperimentFunnelStage.VISUALIZACAO_ANUNCIO, 1000),
                stage(ExperimentFunnelStage.ACESSO_FORM_LEAD, 50),
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 40),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)
        ));

        ExperimentFunnelDiagnosticsResponseDto response = service.diagnose(50L);

        assertThat(response.diagnostics())
                .extracting(item -> item.stageKey())
                .containsExactly(
                        ExperimentFunnelStage.ACESSO_FORM_LEAD,
                        ExperimentFunnelStage.ACESSO_CHECKOUT,
                        ExperimentFunnelStage.COMPRA)
                .doesNotContain(ExperimentFunnelStage.ENVIO_FORM);
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
