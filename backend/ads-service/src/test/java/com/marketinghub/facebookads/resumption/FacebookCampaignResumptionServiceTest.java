package com.marketinghub.facebookads.resumption;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.funnel.ExperimentFinancialGuardrailPolicy;
import com.marketinghub.experiment.service.ExperimentCampaignMetricService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.facebookads.*;
import com.marketinghub.facebookads.resumption.service.*;
import com.marketinghub.facebookads.resumption.service.request.ResumeCampaignRequest;
import com.marketinghub.facebookads.resumption.service.result.CampaignReplacementResult;
import com.marketinghub.facebookads.resumption.service.result.ResumeCampaignResult;
import com.marketinghub.facebookads.service.CampaignStrategyService;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.facebookads.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** Verifica autorização individual, preservação de coorte e confirmação antes de marcar RUNNING. */
class FacebookCampaignResumptionServiceTest {
  private final FacebookCampaignResumptionRepository requests =
      mock(FacebookCampaignResumptionRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final FacebookAdsCampaignRepository campaigns = mock(FacebookAdsCampaignRepository.class);
  private final FacebookAdsAdSetRepository adSets = mock(FacebookAdsAdSetRepository.class);
  private final FacebookAdsAdRepository ads = mock(FacebookAdsAdRepository.class);
  private final FacebookAdsAdTrackingUtmRepository trackingUtms =
      mock(FacebookAdsAdTrackingUtmRepository.class);
  private final ExperimentCampaignMetricRepository metrics =
      mock(ExperimentCampaignMetricRepository.class);
  private final ExperimentCampaignMetricService campaignMetrics =
      mock(ExperimentCampaignMetricService.class);
  private final CampaignStrategyService campaignStrategies = mock(CampaignStrategyService.class);
  private final ExperimentStatusChangeRepository history =
      mock(ExperimentStatusChangeRepository.class);
  private final ExperimentReadinessService readiness = mock(ExperimentReadinessService.class);
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final FacebookCampaignResumptionService service =
      new FacebookCampaignResumptionService(
          requests,
          experiments,
          campaigns,
          adSets,
          ads,
          trackingUtms,
          metrics,
          campaignMetrics,
          campaignStrategies,
          history,
          readiness,
          json);
  private Experiment e;
  private FacebookAdsCampaign c;
  private FacebookCampaignResumption saved;
  private LocalDate end;

  /** Prepara campanha pausada com gasto anterior realista e dependências sem serviços externos. */
  @BeforeEach
  void setup() {
    e = new Experiment();
    e.setId(91L);
    e.setPlatform(ExperimentPlatform.FACEBOOK);
    e.setStatus(ExperimentStatus.USER_STOPPED);
    e.setDailyBudget(new BigDecimal("20"));
    e.setMediaSpendLimit(new BigDecimal("100"));
    e.setFollowUpActionUrl("https://vega.invalid");
    e.setFunnelResetAt(Instant.parse("2026-09-06T22:18:58Z"));
    e.setStartDate(LocalDate.of(2026, 9, 7));
    e.setEndDate(LocalDate.of(2026, 9, 11));
    c = new FacebookAdsCampaign();
    c.setId("campaign");
    c.setStatus(FacebookAdStatus.PAUSED);
    c.setExperiment(e);
    FacebookAdsAdSet set = new FacebookAdsAdSet();
    set.setId("adset");
    c.setAdSets(List.of(set));
    ExperimentCampaignMetric m = new ExperimentCampaignMetric();
    m.setSpend(new BigDecimal("27.45"));
    when(experiments.findById(91L)).thenReturn(Optional.of(e));
    when(experiments.findForFacebookRelease(91L)).thenReturn(Optional.of(e));
    when(campaigns.findById("campaign")).thenReturn(Optional.of(c));
    when(campaigns.findDetailedByExperimentId(91L)).thenReturn(List.of(c));
    when(metrics.findByExperiment(e)).thenReturn(Optional.of(m));
    when(readiness.isReadyForCampaign(e)).thenReturn(true);
    when(requests.save(any()))
        .thenAnswer(
            inv -> {
              saved = inv.getArgument(0);
              saved.setId(1L);
              when(requests.findLocked(1L)).thenReturn(Optional.of(saved));
              when(requests.findById(1L)).thenReturn(Optional.of(saved));
              return saved;
            });
    end = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(6);
  }

  /** Percorre autorização, reserva e callback sem resetar gasto, coorte ou identidade. */
  @Test
  void completesOnlyAfterNativeEvidenceAndPreservesHistory() throws Exception {
    var response = service.request(91L, input());
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.leaseToken()).isNull();
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
    assertThat(saved.getPreviousLimit()).isEqualByComparingTo("100");
    assertThat(saved.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 9, 11));
    assertThat(saved.getDailyBudget()).isEqualByComparingTo("20");
    assertThat(saved.getZeroPurchaseSpendLimit()).isEqualByComparingTo("50");
    assertThat(saved.getPurchaseStopCount()).isEqualTo(5);
    assertThat(e.getFunnelResetAt()).isEqualTo(Instant.parse("2026-09-06T22:18:58Z"));
    assertThat(service.summary(91L).synchronizedSpend()).isEqualByComparingTo("27.45");
    var claim = service.claim(1L);
    assertThat(claim.leaseToken()).isNotBlank();
    assertThatThrownBy(() -> service.claim(1L)).hasMessageContaining("reservada");
    var evidence =
        json.readTree(
            "{\"campaignId\":\"campaign\",\"campaignStatus\":\"ACTIVE\",\"adSetId\":\"adset\",\"budgetMode\":\"DAILY_WITH_CAMPAIGN_CAP\",\"campaignSpendCapMinor\":15000,\"dailyBudgetMinor\":2000,\"spend\":27.45,\"startDate\":\""
                + LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                + "\",\"endDate\":\""
                + end
                + "\"}");
    var result = new ResumeCampaignResult(claim.leaseToken(), true, null, evidence);
    assertThat(service.result(1L, result).status()).isEqualTo("COMPLETED");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    assertThat(c.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
    assertThat(service.result(1L, result).status()).isEqualTo("COMPLETED");
    verify(history, times(2)).save(any());
    verify(metrics, never()).save(any());
  }

  /** Aceita vitalício na campanha quando o mínimo do teto diário da conta é maior. */
  @Test
  void completesWithCampaignLifetimeBudgetBelowMinimum() {
    service.request(91L, input());
    var claim = service.claim(1L);
    var evidence =
        json.createObjectNode()
            .put("campaignId", "campaign")
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", "adset")
            .put("budgetMode", "CAMPAIGN_LIFETIME_BELOW_MINIMUM")
            .put("campaignLifetimeBudgetMinor", 15000)
            .put("accountMinimumCampaignSpendCapMinor", 30000)
            .put("dailyBudgetMinor", 2000)
            .put("authorizedDailyBudgetMinor", 2000)
            .put("campaignDailyBudgetMinor", 0)
            .put("adSetDailyBudgetMinor", 0)
            .put("lifetimeBudgetMinor", 0)
            .put("adSetLifetimeSpendCapMinor", 0)
            .put("campaignSpendCapMinor", 0)
            .put("historicalSpendMinor", 0)
            .put("sourceCampaignSpend", new BigDecimal("27.45"))
            .put("remainingDays", 7)
            .put("effectiveRemainingAverageMinor", 1751)
            .put("spend", new BigDecimal("27.45"))
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString());

    assertThat(
            service
                .result(1L, new ResumeCampaignResult(claim.leaseToken(), true, null, evidence))
                .status())
        .isEqualTo("COMPLETED");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
  }

  /**
   * Materializa uma nova hierarquia vitalícia quando a Meta proíbe mudar o tipo de orçamento da
   * campanha diária.
   */
  @Test
  void completesWithAuditableReplacementBelowCampaignMinimum() {
    c.setId("120000000000000001");
    c.setExternalId(c.getId());
    c.setAdAccountId("123456");
    c.setName("Campanha Capella");
    c.setObjective("OUTCOME_SALES");
    c.setBudgetMode(BudgetMode.ADSET);
    c.setApiVersion("v23.0");
    FacebookAdsAdSet sourceSet = c.getAdSets().get(0);
    sourceSet.setId("120000000000000002");
    sourceSet.setExternalId(sourceSet.getId());
    sourceSet.setCampaign(c);
    sourceSet.setName("Conjunto Capella");
    sourceSet.setBillingEvent("IMPRESSIONS");
    sourceSet.setOptimizationGoal("OFFSITE_CONVERSIONS");
    sourceSet.setBidStrategy("LOWEST_COST_WITHOUT_CAP");
    sourceSet.setTargetingJson("{\"geo_locations\":{\"countries\":[\"BR\"]}}");
    sourceSet.setPromotedObjectJson(
        "{\"pixel_id\":\"1272936690700110\",\"custom_event_type\":\"PURCHASE\"}");
    FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
    creative.setId("1399338238757778");
    FacebookAdsAd sourceAd = new FacebookAdsAd();
    sourceAd.setId("120000000000000003");
    sourceAd.setAdSet(sourceSet);
    sourceAd.setName("Criativo Capella");
    sourceAd.setCreative(creative);
    sourceAd.setStatus(FacebookAdStatus.ACTIVE);
    sourceSet.setAds(List.of(sourceAd));
    c.setAdSets(List.of(sourceSet));
    when(campaigns.findById(c.getId())).thenReturn(Optional.of(c));
    when(campaigns.findDetailedByExperimentId(91L)).thenReturn(List.of(c));
    when(adSets.findDetailedByCampaignIds(List.of(c.getId()))).thenReturn(List.of(sourceSet));
    when(campaigns.save(any(FacebookAdsCampaign.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(adSets.save(any(FacebookAdsAdSet.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(ads.save(any(FacebookAdsAd.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.request(91L, input());
    var claim = service.claim(1L);
    String targetCampaignId = "120000000000000011";
    String targetAdSetId = "120000000000000012";
    String targetAdId = "120000000000000013";
    var evidence =
        json.createObjectNode()
            .put("campaignId", targetCampaignId)
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", targetAdSetId)
            .put("budgetMode", "REPLACEMENT_ADSET_LIFETIME_BELOW_MINIMUM")
            .put("accountMinimumCampaignSpendCapMinor", 30000)
            .put("dailyBudgetMinor", 2000)
            .put("authorizedDailyBudgetMinor", 2000)
            .put("campaignDailyBudgetMinor", 0)
            .put("campaignLifetimeBudgetMinor", 0)
            .put("campaignSpendCapMinor", 0)
            .put("adSetDailyBudgetMinor", 0)
            .put("lifetimeBudgetMinor", 12255)
            .put("adSetLifetimeSpendCapMinor", 0)
            .put("confirmedPriorSpendMinor", 2745)
            .put("historicalSpendMinor", 0)
            .put("remainingDays", 7)
            .put("effectiveRemainingAverageMinor", 1751)
            .put("spend", new BigDecimal("27.45"))
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString());
    var replacement =
        new CampaignReplacementResult(
            c.getId(),
            sourceSet.getId(),
            targetCampaignId,
            targetAdSetId,
            12255L,
            new BigDecimal("27.45"),
            List.of(
                new CampaignReplacementResult.ReplacementAdResult(sourceAd.getId(), targetAdId)));

    assertThat(
            service
                .result(
                    1L,
                    new ResumeCampaignResult(claim.leaseToken(), true, null, evidence, replacement))
                .status())
        .isEqualTo("COMPLETED");

    var campaignCaptor = org.mockito.ArgumentCaptor.forClass(FacebookAdsCampaign.class);
    verify(campaigns).save(campaignCaptor.capture());
    FacebookAdsCampaign target = campaignCaptor.getValue();
    assertThat(target.getId()).isEqualTo(targetCampaignId);
    assertThat(target.getReplacesCampaignId()).isEqualTo(c.getId());
    assertThat(target.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
    assertThat(c.getSupersededByCampaignId()).isEqualTo(targetCampaignId);
    assertThat(c.getStatus()).isEqualTo(FacebookAdStatus.PAUSED);
    verify(campaignMetrics).activateReplacement(target, new BigDecimal("27.45"));
    verify(campaignStrategies).ensureDefaultStrategy(target);
    verify(ads).save(argThat(ad -> targetAdId.equals(ad.getId())));
  }

  /** Recusa o vitalício quando o mínimo informado não excede o teto autorizado. */
  @Test
  void rejectsUnjustifiedCampaignLifetimeFallback() {
    service.request(91L, input());
    var claim = service.claim(1L);
    var evidence =
        json.createObjectNode()
            .put("campaignId", "campaign")
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", "adset")
            .put("budgetMode", "CAMPAIGN_LIFETIME_BELOW_MINIMUM")
            .put("campaignLifetimeBudgetMinor", 15000)
            .put("accountMinimumCampaignSpendCapMinor", 10000)
            .put("dailyBudgetMinor", 2000)
            .put("authorizedDailyBudgetMinor", 2000)
            .put("campaignDailyBudgetMinor", 0)
            .put("adSetDailyBudgetMinor", 0)
            .put("lifetimeBudgetMinor", 0)
            .put("adSetLifetimeSpendCapMinor", 0)
            .put("campaignSpendCapMinor", 0)
            .put("historicalSpendMinor", 0)
            .put("sourceCampaignSpend", new BigDecimal("27.45"))
            .put("remainingDays", 7)
            .put("effectiveRemainingAverageMinor", 1751)
            .put("spend", new BigDecimal("27.45"))
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString());

    assertThatThrownBy(
            () ->
                service.result(
                    1L, new ResumeCampaignResult(claim.leaseToken(), true, null, evidence)))
        .hasMessageContaining("não confirmou");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
  }

  /** Recusa orçamento vitalício acima do teto ou da capacidade diária restante aprovada. */
  @Test
  void rejectsCampaignLifetimeAboveAuthorizedLimits() {
    service.request(91L, input());
    var claim = service.claim(1L);
    var evidence =
        json.createObjectNode()
            .put("campaignId", "campaign")
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", "adset")
            .put("budgetMode", "CAMPAIGN_LIFETIME_BELOW_MINIMUM")
            .put("campaignLifetimeBudgetMinor", 17000)
            .put("accountMinimumCampaignSpendCapMinor", 30000)
            .put("dailyBudgetMinor", 2000)
            .put("authorizedDailyBudgetMinor", 2000)
            .put("campaignDailyBudgetMinor", 0)
            .put("adSetDailyBudgetMinor", 0)
            .put("lifetimeBudgetMinor", 0)
            .put("adSetLifetimeSpendCapMinor", 0)
            .put("campaignSpendCapMinor", 0)
            .put("historicalSpendMinor", 0)
            .put("sourceCampaignSpend", new BigDecimal("27.45"))
            .put("remainingDays", 7)
            .put("effectiveRemainingAverageMinor", 2037)
            .put("spend", new BigDecimal("27.45"))
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString());

    assertThatThrownBy(
            () ->
                service.result(
                    1L, new ResumeCampaignResult(claim.leaseToken(), true, null, evidence)))
        .hasMessageContaining("não confirmou");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
  }

  /** Recusa orçamento e teto vitalício simultâneos no conjunto, incompatíveis na Meta. */
  @Test
  void rejectsDailyBudgetMixedWithAdSetLifetimeCap() {
    service.request(91L, input());
    var claim = service.claim(1L);
    var evidence =
        json.createObjectNode()
            .put("campaignId", "campaign")
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", "adset")
            .put("budgetMode", "DAILY_WITH_ADSET_LIFETIME_CAP")
            .put("adSetLifetimeSpendCapMinor", 15000)
            .put("accountMinimumCampaignSpendCapMinor", 30000)
            .put("dailyBudgetMinor", 2000)
            .put("spend", new BigDecimal("27.45"))
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString());

    assertThatThrownBy(
            () ->
                service.result(
                    1L, new ResumeCampaignResult(claim.leaseToken(), true, null, evidence)))
        .hasMessageContaining("não confirmou");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
  }

  /** Recusa confirmação sem prova do teto e mantém a campanha pausada. */
  @Test
  void rejectsFalseSuccessAndStaleCallback() {
    service.request(91L, input());
    var claim = service.claim(1L);
    assertThatThrownBy(
            () ->
                service.result(
                    1L, new ResumeCampaignResult("other", true, null, json.createObjectNode())))
        .hasMessageContaining("Reserva inválida");
    assertThatThrownBy(
            () ->
                service.result(
                    1L,
                    new ResumeCampaignResult(
                        claim.leaseToken(), true, null, json.createObjectNode())))
        .hasMessageContaining("não confirmou");
    assertThat(c.getStatus()).isEqualTo(FacebookAdStatus.PAUSED);
    assertThat(
            service
                .result(
                    1L,
                    new ResumeCampaignResult(
                        claim.leaseToken(), false, "Meta rejeitou prazo", json.createObjectNode()))
                .status())
        .isEqualTo("FAILED");
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
  }

  /** Repetição idêntica não cria nova autorização nem libera uma reserva concorrente. */
  @Test
  void deduplicatesAndRecoversExpiredLease() {
    service.request(91L, input());
    when(requests.findFirstByExperimentIdOrderByIdDesc(91L)).thenReturn(Optional.of(saved));
    assertThat(service.request(91L, input()).id()).isEqualTo(1L);
    verify(requests, times(1)).save(any());
    String oldToken = service.claim(1L).leaseToken();
    saved.setLeaseUntil(Instant.now().minusSeconds(1));
    assertThat(service.claim(1L).leaseToken()).isNotEqualTo(oldToken);
  }

  /** Impede gasto sem autorização, teto esgotado, prazo vencido e campanha fora do contrato. */
  @Test
  void rejectsInvalidFinancialRequests() {
    assertThatThrownBy(
            () ->
                service.request(
                    91L,
                    new ResumeCampaignRequest(
                        new BigDecimal("150"),
                        new BigDecimal("20"),
                        LocalDate.now(ZoneId.of("America/Sao_Paulo")),
                        end,
                        new BigDecimal("50"),
                        new BigDecimal("50"),
                        5,
                        "Coletar mais dados",
                        false,
                        false)))
        .hasMessageContaining("Confirme");
    assertThatThrownBy(
            () ->
                service.request(
                    91L,
                    new ResumeCampaignRequest(
                        new BigDecimal("27.45"),
                        new BigDecimal("20"),
                        LocalDate.now(ZoneId.of("America/Sao_Paulo")),
                        end,
                        new BigDecimal("27.45"),
                        new BigDecimal("27.45"),
                        5,
                        "Coletar mais dados",
                        true,
                        false)))
        .hasMessageContaining("superar");
    assertThatThrownBy(
            () ->
                service.request(
                    91L,
                    new ResumeCampaignRequest(
                        new BigDecimal("150"),
                        new BigDecimal("20"),
                        LocalDate.now().minusDays(2),
                        end,
                        new BigDecimal("50"),
                        new BigDecimal("50"),
                        5,
                        "Coletar mais dados",
                        true,
                        false)))
        .hasMessageContaining("Início");
    c.setStatus(FacebookAdStatus.ACTIVE);
    assertThatThrownBy(() -> service.request(91L, input())).hasMessageContaining("pausada");
    verify(requests, never()).save(any());
  }

  /** Mantém a regra padrão nos demais experimentos e limita cada parada ao teto absoluto. */
  @Test
  void isolatesFinancialException() {
    assertThat(ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(e))
        .isEqualByComparingTo("25");
    service.request(91L, input());
    assertThat(ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(e))
        .isEqualByComparingTo("50");
    assertThat(e.getZeroPurchaseSpendLimit()).isEqualByComparingTo("50");
    assertThat(ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(new Experiment()))
        .isEqualByComparingTo("25");
    e.setMediaSpendLimit(new BigDecimal("125"));
    assertThat(ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(e))
        .isEqualByComparingTo("50");
  }

  /** Fornece a autorização explícita do cenário de retomada controlada. */
  private ResumeCampaignRequest input() {
    return new ResumeCampaignRequest(
        new BigDecimal("150"),
        new BigDecimal("20"),
        LocalDate.now(ZoneId.of("America/Sao_Paulo")),
        end,
        new BigDecimal("50"),
        new BigDecimal("50"),
        5,
        "Coletar mais dados preservando a campanha",
        true,
        false);
  }

  /** Percorre as rotas HTTP reais com serviço canônico e persistência simulada segregada. */
  @Test
  void httpContractQueuesClaimsAndConfirms() throws Exception {
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.marketinghub.facebookads.resumption.controller
                    .FacebookCampaignResumptionController(service))
            .setMessageConverters(
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                    json))
            .build();
    var request =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/facebook-campaign-resumptions/experiments/91")
            .contentType("application/json")
            .content(json.writeValueAsString(input()));
    mvc.perform(request)
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value("PENDING"));
    when(requests.findPending(any(), any(), any())).thenReturn(List.of(saved));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/facebook-campaign-resumptions/pending"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$[0].campaignId")
                .value("campaign"));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/facebook-campaign-resumptions/1"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value("PENDING"));
    String claim =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/facebook-campaign-resumptions/1/claim"))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var evidence =
        json.createObjectNode()
            .put("campaignId", "campaign")
            .put("campaignStatus", "ACTIVE")
            .put("adSetId", "adset")
            .put("budgetMode", "LIFETIME")
            .put("lifetimeBudgetMinor", 15000)
            .put("startDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
            .put("endDate", end.toString())
            .put("spend", new BigDecimal("27.45"));
    var result =
        new ResumeCampaignResult(
            json.readTree(claim).path("leaseToken").asText(), true, null, evidence);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/facebook-campaign-resumptions/1/result")
                .contentType("application/json")
                .content(json.writeValueAsString(result)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value("COMPLETED"));
    assertThat(e.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
  }
}
