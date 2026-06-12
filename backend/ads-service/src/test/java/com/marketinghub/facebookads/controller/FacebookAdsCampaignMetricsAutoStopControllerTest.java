package com.marketinghub.facebookads.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.ExperimentFunnelAutoStopService;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import com.marketinghub.experiment.service.ExperimentCampaignMetricService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationService;
import com.marketinghub.leadportal.service.LeadPortalMetricsService;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.AdSetRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.EnumType.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa o contrato de métricas que pode acionar parada automática de campanhas Facebook. */
@ExtendWith(MockitoExtension.class)
class FacebookAdsCampaignMetricsAutoStopControllerTest {

    private static final String CAMPAIGN_ID = "120248182742210326";

    @Mock
    private ExperimentService experimentService;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private FacebookAccountRepository accountRepository;
    @Mock
    private FacebookAdsAdSetRepository adSetRepository;
    @Mock
    private FacebookAdsAdCreativeRepository adCreativeRepository;
    @Mock
    private FacebookAdsAdRepository adRepository;
    @Mock
    private CreativeRepository creativeRepository;
    @Mock
    private AdSetRepository experimentAdSetRepository;
    @Mock
    private ExperimentCampaignMetricService campaignMetricService;
    @Mock
    private ExperimentReadinessService experimentReadinessService;
    @Mock
    private LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    @Mock
    private LeadPortalMetricsService leadPortalMetricsService;
    @Mock
    private FacebookCampaignRecommendationService recommendationService;
    @Mock
    private ExperimentFunnelDiagnosticService diagnosticService;
    @Mock
    private ExperimentCampaignMetricRepository campaignMetricRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /** Monta o controller com o serviço real de parada automática e dependências controladas por mock. */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ExperimentFunnelAutoStopService autoStopService = new ExperimentFunnelAutoStopService(
                diagnosticService,
                campaignRepository,
                campaignMetricRepository
        );
        FacebookAdsCampaignController controller = new FacebookAdsCampaignController(
                experimentService,
                campaignRepository,
                accountRepository,
                adSetRepository,
                adCreativeRepository,
                adRepository,
                creativeRepository,
                experimentAdSetRepository,
                objectMapper,
                campaignMetricService,
                autoStopService,
                experimentReadinessService,
                leadPortalPublicUrlResolver,
                leadPortalMetricsService,
                recommendationService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** Garante que todos os motivos atuais cabem no contrato persistido como texto, sem enum físico no banco. */
    @Test
    void stopReasonColumnAcceptsAllCurrentAutomaticStopReasons() throws Exception {
        Column column = FacebookAdsCampaign.class.getDeclaredField("stopReason").getAnnotation(Column.class);
        Enumerated enumerated = FacebookAdsCampaign.class.getDeclaredField("stopReason").getAnnotation(Enumerated.class);

        assertThat(column.name()).isEqualTo("stop_reason");
        assertThat(column.length()).isEqualTo(100);
        assertThat(enumerated.value()).isEqualTo(STRING);
        assertThat(Arrays.stream(FacebookCampaignStopReason.values()).map(Enum::name))
                .allSatisfy(value -> assertThat(value).hasSizeLessThanOrEqualTo(100));
    }

    /**
     * Simula o POST de métricas do worker e confirma que baixo interesse estatístico grava o novo motivo de parada.
     */
    @Test
    void postMetricsPersistsTargetAudienceLowInterestStopReason() throws Exception {
        Experiment experiment = new Experiment();
        experiment.setId(123L);
        experiment.setStatus(ExperimentStatus.RUNNING);

        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId(CAMPAIGN_ID);
        campaign.setExperiment(experiment);
        campaign.setFacebookAccount(new FacebookAccount());
        campaign.setAdAccountId("act_123");
        campaign.setName("Campanha de validação");
        campaign.setObjective("OUTCOME_TRAFFIC");
        campaign.setStatus(FacebookAdStatus.ACTIVE);
        campaign.setCreatedAt(Instant.now());

        ExperimentCampaignMetric metric = ExperimentCampaignMetric.builder()
                .campaign(campaign)
                .experiment(experiment)
                .dateStart(LocalDate.parse("2026-06-12"))
                .dateStop(LocalDate.parse("2026-06-12"))
                .reach(1300L)
                .impressions(1500L)
                .clicks(120L)
                .leads(6L)
                .spend(new BigDecimal("25.00"))
                .cpc(new BigDecimal("0.21"))
                .cpl(new BigDecimal("4.17"))
                .build();

        when(campaignMetricService.upsert(
                eq(CAMPAIGN_ID),
                eq(LocalDate.parse("2026-06-12")),
                eq(LocalDate.parse("2026-06-12")),
                eq(1300L),
                eq(1500L),
                eq(120L),
                eq(6L),
                eq(new BigDecimal("25.00"))
        )).thenReturn(metric);
        when(diagnosticService.diagnose(123L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(lowInterestFailedStage()), null));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric));
        when(campaignRepository.findByExperimentId(123L)).thenReturn(List.of(campaign));

        String payload = """
                {
                  "dateStart": "2026-06-12",
                  "dateStop": "2026-06-12",
                  "reach": 1300,
                  "impressions": 1500,
                  "clicks": 120,
                  "leads": 6,
                  "spend": 25.00
                }
                """;

        mockMvc.perform(post("/api/facebook-campaigns/{campaignId}/metrics", CAMPAIGN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reach").value(1300))
                .andExpect(jsonPath("$.impressions").value(1500))
                .andExpect(jsonPath("$.spend").value(25.0));

        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.TARGET_AUDIENCE_LOW_INTEREST_STATISTICAL);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
        assertThat(campaign.getMetricsLastSyncedAt()).isNotNull();
        assertThat(campaign.getMetricsLastError()).isNull();
        verify(campaignRepository).findByExperimentId(123L);
    }

    /** Cria diagnóstico de interesse baixo com reprovação estatística na etapa de acesso ao formulário. */
    private ExperimentFunnelStageDiagnosticDto lowInterestFailedStage() {
        return new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_FORM_LEAD,
                "Acesso ao formulário",
                1199,
                6,
                0.005,
                0.015,
                0.0108,
                List.of(new FunnelThresholdCheckDto(0.015, 200, 0.0025, false, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
    }
}
