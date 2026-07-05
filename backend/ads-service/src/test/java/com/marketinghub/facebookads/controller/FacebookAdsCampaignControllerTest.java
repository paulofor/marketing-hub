package com.marketinghub.facebookads.controller;

import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.ExperimentFunnelAutoStopService;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.service.publicationstep.FacebookCampaignPublicationJobStepService;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import com.marketinghub.leadportal.dto.LeadPortalExperimentUserDto;
import com.marketinghub.leadportal.service.LeadPortalMetricsService;
import com.marketinghub.repository.jpa.experiment.AdSetRepository;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Valida os contratos HTTP de campanhas Facebook Ads expostos ao worker e à UI.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class FacebookAdsCampaignControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    ExperimentService experimentService;
    @MockBean
    com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository campaignRepository;
    @MockBean
    FacebookAccountRepository facebookAccountRepository;
    @MockBean
    FacebookAdsAdSetRepository adSetRepository;
    @MockBean
    FacebookAdsAdCreativeRepository adCreativeRepository;
    @MockBean
    FacebookAdsAdRepository adRepository;
    @MockBean
    AdSetRepository experimentAdSetRepository;
    @MockBean
    ExperimentTargetingSelectionRepository targetingSelectionRepository;
    @MockBean
    CreativeRepository creativeRepository;
    @MockBean
    GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
    @MockBean
    GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;
    @MockBean
    FacebookCampaignPublicationJobStepService publicationJobStepService;

    @MockBean
    LeadPortalMetricsService leadPortalMetricsService;
    @MockBean
    ExperimentFunnelAutoStopService funnelAutoStopService;


    @Test
    // Verifica que o módulo Facebook expõe somente criativos aprovados para consumo do worker.
    void listsReadyCreativesForFacebookConsumption() throws Exception {
        var experiment = Experiment.builder()
                .id(1L)
                .name("Exp")
                .build();
        var readyCreative = Creative.builder()
                .id(101L)
                .experiment(experiment)
                .format("IMAGE")
                .headline("Headline")
                .primaryText("Texto principal")
                .imageUrl("https://cdn.example/creative.png")
                .description("Descrição")
                .cta("SIGN_UP")
                .destinationUrl("https://example.com/landing")
                .leadGenFormId("321")
                .instagramUserId("987")
                .status(CreativeStatus.READY)
                .build();
        var draftCreative = Creative.builder()
                .id(102L)
                .experiment(experiment)
                .headline("Rascunho")
                .status(CreativeStatus.DRAFT)
                .build();
        when(creativeRepository.findByExperimentId(1L)).thenReturn(List.of(draftCreative, readyCreative));

        mockMvc.perform(get("/api/facebook-campaigns/experiments/1/creatives-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].experimentId").value(1))
                .andExpect(jsonPath("$[0].format").value("IMAGE"))
                .andExpect(jsonPath("$[0].headline").value("Headline"))
                .andExpect(jsonPath("$[0].primaryText").value("Texto principal"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://cdn.example/creative.png"))
                .andExpect(jsonPath("$[0].description").value("Descrição"))
                .andExpect(jsonPath("$[0].cta").value("SIGN_UP"))
                .andExpect(jsonPath("$[0].destinationUrl").value("https://example.com/landing"))
                .andExpect(jsonPath("$[0].leadGenFormId").value("321"))
                .andExpect(jsonPath("$[0].instagramUserId").value("987"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void listExperimentsByStatus() throws Exception {
        var niche = MarketNiche.builder()
                .id(10L)
                .name("Test Nicho")
                .build();
        var hypothesis = Hypothesis.builder()
                .id(java.util.UUID.randomUUID())
                .title("Hipótese do Nicho")
                .build();
        var journeyTemplate = JourneyTemplate.builder()
                .id(20L)
                .name("Lifecycle Pós-Clique")
                .build();
        var account = FacebookAccount.builder()
                .id(55L)
                .name("Conta Worker")
                .build();
        var instagramAccount = InstagramAccount.builder()
                .id(91L)
                .handle("@estudio")
                .code("IG-ESTUDIO")
                .name("Estúdio")
                .build();
        var page = FacebookPage.builder()
                .id(9L)
                .account(account)
                .pageId("84")
                .name("Estúdio")
                .build();
        var leadPortalFlow = LeadPortalFlow.builder()
                .id(33L)
                .name("Fluxo principal")
                .slug("fluxo-principal")
                .approved(true)
                .build();
        var exp = Experiment.builder()
                .id(1L)
                .niche(niche)
                .name("Exp")
                .hypothesis("Hipótese")
                .hypothesisRef(hypothesis)
                .followUpActionUrl("https://landing.example.com/exp")
                .kpiTargetCpl(BigDecimal.TEN)
                .stopLossCpl(BigDecimal.valueOf(20))
                .sampleSize(1200)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .creativeApproved(true)
                .journeyTemplate(journeyTemplate)
                .facebookPage(page)
                .instagramAccount(instagramAccount)
                .leadPortalFlow(leadPortalFlow)
                .build();
        when(creativeRepository.existsByExperimentIdAndStatus(1L, CreativeStatus.READY)).thenReturn(true);
        when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(1L)).thenReturn(List.of(
                ExperimentTargetingSelection.builder()
                        .candidateType(TargetingCandidateType.INTEREST)
                        .term("Loja de roupas")
                        .targetingElement(TargetingElement.builder()
                                .type(TargetingElementType.INTEREST)
                                .term("Loja de roupas")
                                .status(TargetingElementStatus.APPROVED)
                                .metaId("meta-interest-1")
                                .build())
                        .build()));
        when(experimentService.listByStatusAndPlatform(
                com.marketinghub.experiment.ExperimentStatus.PLANNED,
                com.marketinghub.experiment.ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(exp));
        when(leadPortalMetricsService.listExperimentMetrics()).thenReturn(List.of(
                new LeadPortalExperimentMetricsDto(
                        exp.getId(),
                        exp.getName(),
                        25L,
                        0L,
                        5L,
                        List.of(new LeadPortalExperimentUserDto("Lead 1", "lead@example.com", null, false)),
                        0L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0L,
                        0L,
                        null)));

        mockMvc.perform(get("/api/facebook-campaigns/experiments").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Exp"))
                .andExpect(jsonPath("$[0].hypothesis").value("Hipótese"))
                .andExpect(jsonPath("$[0].followUpActionUrl").value("https://landing.example.com/exp"))
                .andExpect(jsonPath("$[0].kpiTargetCpl").value(10))
                .andExpect(jsonPath("$[0].pageId").value("84"))
                .andExpect(jsonPath("$[0].facebookPage.pageId").value("84"))
                .andExpect(jsonPath("$[0].facebookPage.name").value("Estúdio"))
                .andExpect(jsonPath("$[0].instagramAccount.handle").value("@estudio"))
                .andExpect(jsonPath("$[0].instagramAccount.code").value("IG-ESTUDIO"))
                .andExpect(jsonPath("$[0].startDate").value("2024-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("2024-01-31"))
                .andExpect(jsonPath("$[0].nicheName").value("Test Nicho"))
                .andExpect(jsonPath("$[0].hypothesisTitle").value("Hipótese do Nicho"))
                .andExpect(jsonPath("$[0].missingConfiguration").isArray())
                .andExpect(jsonPath("$[0].missingConfiguration").isEmpty())
                .andExpect(jsonPath("$[0].leadPortalFunnel.formAccesses").value(25))
                .andExpect(jsonPath("$[0].leadPortalFunnel.formSubmissions").value(1));
    }

    @Test
    void experimentsReadyExcludesExperimentsThatAlreadyHaveCampaign() throws Exception {
        var experiment = Experiment.builder()
                .id(37L)
                .name("Experimento 37")
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .facebookReleaseRequestedAt(Instant.parse("2026-06-05T23:56:48Z"))
                .followUpActionUrl("https://landing.example.com/37")
                .creativeApproved(true)
                .build();
        when(experimentService.listByStatusAndPlatform(ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(experiment));
        when(campaignRepository.existsByExperimentId(37L)).thenReturn(true);
        when(leadPortalMetricsService.listExperimentMetrics()).thenReturn(List.of());

        mockMvc.perform(get("/api/facebook-campaigns/experiments-ready"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    // Garante que o contrato do worker recebe tipo, objetivo e pixel de low-ticket.
    void experimentsReadyIncludesExperimentTypeForLowTicketSalesCampaigns() throws Exception {
        var experiment = Experiment.builder()
                .id(53L)
                .name("Experimento 53")
                .niche(MarketNiche.builder()
                        .id(23L)
                        .name("Vestuário")
                        .facebookPixelId("pixel-exp53")
                        .build())
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
                .campaignObjective(ExperimentCampaignObjective.SALES)
                .singlePain("Cliente some depois da primeira compra")
                .freeReward("preview da ferramenta")
                .funnelPromise("Visualizar riscos e oportunidades antes de comprar")
                .primaryCta("Comprar a ferramenta")
                .unitPrice(new BigDecimal("29.90"))
                .facebookReleaseRequestedAt(Instant.parse("2026-07-01T20:00:00Z"))
                .followUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html")
                .creativeApproved(true)
                .build();
        when(experimentService.listByStatusAndPlatform(ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(experiment));
        when(campaignRepository.existsByExperimentId(53L)).thenReturn(false);
        when(creativeRepository.existsByExperimentIdAndStatus(53L, CreativeStatus.READY)).thenReturn(true);
        when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(53L)).thenReturn(List.of(
                ExperimentTargetingSelection.builder()
                        .candidateType(TargetingCandidateType.INTEREST)
                        .term("Loja de roupas")
                        .targetingElement(TargetingElement.builder()
                                .type(TargetingElementType.INTEREST)
                                .term("Loja de roupas")
                                .status(TargetingElementStatus.APPROVED)
                                .metaId("meta-interest-vestuario")
                                .build())
                        .build()));
        when(geraSalesPageStageExecutionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                53L, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code()))
                .thenReturn(Optional.of(GeraSalesPageStageExecution.builder()
                        .idJob("job-exp53-publication")
                        .experimentId(53L)
                        .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                        .status("CONCLUIDO")
                        .executionRequestedAt(Instant.parse("2026-07-01T20:05:00Z"))
                        .build()));
        when(geraSalesPagePublicationAuditRepository.findTopByExperimentIdOrderByPublishedAtDesc(53L))
                .thenReturn(Optional.of(GeraSalesPagePublicationAudit.builder()
                        .experimentId(53L)
                        .publicationJobId("job-exp53-publication")
                        .salesPageUrl("https://pagamentopalf.site/sales-page-exp53.html")
                        .checkoutUrl("https://mpago.la/checkout-exp53")
                        .html("data-mh-sales-page-analytics page_view page_load_metric section_view_time checkout_click")
                        .publishedAt(Instant.parse("2026-07-01T20:06:00Z"))
                        .createdAt(Instant.parse("2026-07-01T20:06:00Z"))
                        .build()));
        when(leadPortalMetricsService.listExperimentMetrics()).thenReturn(List.of());

        mockMvc.perform(get("/api/facebook-campaigns/experiments-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(53))
                .andExpect(jsonPath("$[0].experimentType").value("LOW_TICKET_PRODUCT"))
                .andExpect(jsonPath("$[0].campaignObjective").value("SALES"))
                .andExpect(jsonPath("$[0].facebookPixelId").value("pixel-exp53"))
                .andExpect(jsonPath("$[0].freeReward").value("preview da ferramenta"));
    }

    @Test
    void createCampaignPersistsHierarchy() throws Exception {
        var experiment = Experiment.builder()
                .id(42L)
                .name("Exp")
                .creativeApproved(true)
                .journeyTemplate(JourneyTemplate.builder().id(99L).name("Lifecycle").build())
                .instagramAccount(InstagramAccount.builder()
                        .id(5L)
                        .handle("@acc")
                        .code("IG-ACC")
                        .name("Account")
                        .build())
                .build();
        when(experimentService.get(42L)).thenReturn(experiment);

        var experimentAdSet = AdSet.builder()
                .id(101L)
                .experiment(experiment)
                .targetingJson("{}")
                .build();
        when(experimentAdSetRepository.findById(101L)).thenReturn(Optional.of(experimentAdSet));

        FacebookAccount account = new FacebookAccount();
        account.setId(77L);
        when(facebookAccountRepository.findById(77L)).thenReturn(Optional.of(account));

        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adCreativeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = """
            {
              "id": "cmp123",
              "externalId": "meta-campaign-123",
              "adAccountId": "act_555",
              "name": "Exp",
              "objective": "OUTCOME_TRAFFIC",
              "status": "ACTIVE",
              "budgetMode": "CAMPAIGN",
              "experimentId": 42,
              "facebookAccountId": 77,
              "adSet": {
                "id": "adset123",
                "externalId": "meta-adset-123",
                "name": "Exp - Ad Set",
                "billingEvent": "IMPRESSIONS",
                "optimizationGoal": "LINK_CLICKS",
                "bidStrategy": "LOWEST_COST_WITHOUT_CAP",
              "bidAmount": "1000",
              "dailyBudget": "5000",
              "lifetimeBudget": null,
              "targetCountry": "BR",
              "destinationType": "WEBSITE",
              "pageId": "12345",
              "targetingJson": "{\\"geo_locations\\":{\\"countries\\":[\\"BR\\"]}}",
              "savedAudienceId": "AUD-1",
              "savedAudienceName": "Audience",
              "experimentAdSetId": 101,
              "status": "ACTIVE"
            },
              "adCreative": {
                "id": "creative123",
                "pageId": "12345",
                "instagramActorId": "6789",
                "websiteUrl": "https://example.com",
                "message": "Mensagem",
                "callToActionType": "LEARN_MORE",
                "headline": "Headline",
                "description": "Description"
              },
              "ad": {
                "id": "ad987",
                "externalId": "meta-ad-123",
                "name": "Exp - Ad",
                "adSetId": "adset123",
                "creativeId": "creative123",
                "status": "ACTIVE"
              }
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<FacebookAdsCampaign> campaignCaptor = ArgumentCaptor.forClass(FacebookAdsCampaign.class);
        verify(campaignRepository).save(campaignCaptor.capture());
        FacebookAdsCampaign savedCampaign = campaignCaptor.getValue();
        assertThat(savedCampaign.getId()).isEqualTo("cmp123");
        assertThat(savedCampaign.getExternalId()).isEqualTo("meta-campaign-123");
        assertThat(savedCampaign.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);

        ArgumentCaptor<FacebookAdsAdSet> adSetCaptor = ArgumentCaptor.forClass(FacebookAdsAdSet.class);
        verify(adSetRepository).save(adSetCaptor.capture());
        FacebookAdsAdSet savedAdSet = adSetCaptor.getValue();
        assertThat(savedAdSet.getId()).isEqualTo("adset123");
        assertThat(savedAdSet.getCampaign().getId()).isEqualTo("cmp123");
        assertThat(savedAdSet.getExternalId()).isEqualTo("meta-adset-123");
        assertThat(savedAdSet.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(savedAdSet.getExperimentAdSet()).isNotNull();
        assertThat(savedAdSet.getExperimentAdSet().getId()).isEqualTo(101L);
        JsonNode targetingJson = objectMapper.readTree(savedAdSet.getTargetingJson());
        assertThat(targetingJson.path("saved_audience_id").asText()).isEqualTo("AUD-1");
        assertThat(targetingJson.path("geo_locations").path("countries").get(0).asText()).isEqualTo("BR");
        JsonNode promotedObject = savedAdSet.getPromotedObjectJson() != null
                ? objectMapper.readTree(savedAdSet.getPromotedObjectJson())
                : objectMapper.nullNode();
        assertThat(promotedObject.path("page_id").asText()).isEqualTo("12345");

        ArgumentCaptor<FacebookAdsAdCreative> creativeCaptor = ArgumentCaptor.forClass(FacebookAdsAdCreative.class);
        verify(adCreativeRepository).save(creativeCaptor.capture());
        FacebookAdsAdCreative savedCreative = creativeCaptor.getValue();
        assertThat(savedCreative.getId()).isEqualTo("creative123");
        assertThat(savedCreative.getPageId()).isEqualTo("12345");
        JsonNode linkData = objectMapper.readTree(savedCreative.getLinkDataJson());
        assertThat(linkData.path("link").asText()).isEqualTo("https://example.com");
        assertThat(linkData.path("call_to_action").path("type").asText()).isEqualTo("LEARN_MORE");

        ArgumentCaptor<FacebookAdsAd> adCaptor = ArgumentCaptor.forClass(FacebookAdsAd.class);
        verify(adRepository).save(adCaptor.capture());
        FacebookAdsAd savedAd = adCaptor.getValue();
        assertThat(savedAd.getId()).isEqualTo("ad987");
        assertThat(savedAd.getExternalId()).isEqualTo("meta-ad-123");
        assertThat(savedAd.getAdSet().getId()).isEqualTo("adset123");
        assertThat(savedAd.getCreative().getId()).isEqualTo("creative123");
        assertThat(savedAd.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
    }

    @Test
    void createCampaignAcceptsRetryForSameCampaignIdWithoutDuplicatingHierarchy() throws Exception {
        var experiment = Experiment.builder()
                .id(37L)
                .name("Experimento 37")
                .build();
        when(experimentService.get(37L)).thenReturn(experiment);
        var existingCampaign = new FacebookAdsCampaign();
        existingCampaign.setId("cmp-existente");
        existingCampaign.setExperiment(experiment);
        existingCampaign.setStatus(FacebookAdStatus.PAUSED);
        var existingAdSet = new FacebookAdsAdSet();
        existingAdSet.setId("adset-existente");
        existingAdSet.setStatus(FacebookAdStatus.PAUSED);
        var existingAd = new FacebookAdsAd();
        existingAd.setId("ad-existente");
        existingAd.setStatus(FacebookAdStatus.PAUSED);
        when(campaignRepository.findById("cmp-existente")).thenReturn(Optional.of(existingCampaign));
        when(adSetRepository.findById("adset-existente")).thenReturn(Optional.of(existingAdSet));
        when(adRepository.findById("ad-existente")).thenReturn(Optional.of(existingAd));

        String payload = """
            {
              "id": "cmp-existente",
              "adAccountId": "act_888",
              "name": "Experimento 37",
              "objective": "OUTCOME_TRAFFIC",
              "status": "ACTIVE",
              "budgetMode": "CAMPAIGN",
              "experimentId": 37,
              "facebookAccountId": 88,
              "adSet": {
                "id": "adset-existente",
                "status": "ACTIVE"
              },
              "ads": [
                {
                  "id": "ad-existente",
                  "status": "ACTIVE"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        assertThat(existingCampaign.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(existingAdSet.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(existingAd.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void syncStatusUpdatesCampaignAdSetAndAdsFromMetaSnapshot() throws Exception {
        var campaign = new FacebookAdsCampaign();
        campaign.setId("cmp-sync");
        campaign.setStatus(FacebookAdStatus.PAUSED);
        var adSet = new FacebookAdsAdSet();
        adSet.setId("adset-sync");
        adSet.setStatus(FacebookAdStatus.PAUSED);
        var ad = new FacebookAdsAd();
        ad.setId("ad-sync");
        ad.setStatus(FacebookAdStatus.PAUSED);
        when(campaignRepository.findById("cmp-sync")).thenReturn(Optional.of(campaign));
        when(adSetRepository.findById("adset-sync")).thenReturn(Optional.of(adSet));
        when(adRepository.findById("ad-sync")).thenReturn(Optional.of(ad));

        String payload = """
            {
              "status": "ACTIVE",
              "effectiveStatus": "ACTIVE",
              "adSets": [
                {
                  "id": "adset-sync",
                  "status": "ACTIVE",
                  "effectiveStatus": "ACTIVE"
                }
              ],
              "ads": [
                {
                  "id": "ad-sync",
                  "status": "ACTIVE",
                  "effectiveStatus": "ACTIVE"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns/cmp-sync/status-sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        assertThat(campaign.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(adSet.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
        assertThat(ad.getStatus()).isEqualTo(FacebookAdStatus.ACTIVE);
    }

    @Test
    void createCampaignRejectsAnotherCampaignForSameExperiment() throws Exception {
        var experiment = Experiment.builder()
                .id(37L)
                .name("Experimento 37")
                .build();
        when(experimentService.get(37L)).thenReturn(experiment);
        when(campaignRepository.existsByExperimentId(37L)).thenReturn(true);

        String payload = """
            {
              "id": "cmp-duplicada",
              "adAccountId": "act_888",
              "name": "Experimento 37",
              "objective": "OUTCOME_TRAFFIC",
              "budgetMode": "CAMPAIGN",
              "experimentId": 37,
              "facebookAccountId": 88
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void createCampaignPersistsMultipleAds() throws Exception {
        var experiment = Experiment.builder()
                .id(55L)
                .name("Exp Multi")
                .build();
        when(experimentService.get(55L)).thenReturn(experiment);

        var experimentAdSet = AdSet.builder()
                .id(202L)
                .experiment(experiment)
                .targetingJson("{}")
                .build();
        when(experimentAdSetRepository.findById(202L)).thenReturn(Optional.of(experimentAdSet));

        FacebookAccount account = new FacebookAccount();
        account.setId(88L);
        when(facebookAccountRepository.findById(88L)).thenReturn(Optional.of(account));

        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adCreativeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = """
            {
              "id": "cmp-multi",
              "adAccountId": "act_888",
              "name": "Exp Multi",
              "objective": "OUTCOME_TRAFFIC",
              "budgetMode": "CAMPAIGN",
              "experimentId": 55,
              "facebookAccountId": 88,
              "adSet": {
                "id": "adset-multi",
                "name": "Exp Multi - Ad Set",
                "billingEvent": "IMPRESSIONS",
                "optimizationGoal": "LINK_CLICKS",
                "bidStrategy": "LOWEST_COST_WITHOUT_CAP",
                "bidAmount": "1500",
                "dailyBudget": "6000",
                "targetCountry": "BR",
                "destinationType": "WEBSITE",
                "pageId": "33333",
                "targetingJson": "{}",
                "experimentAdSetId": 202
              },
              "adCreatives": [
                {
                  "id": "creativeA",
                  "pageId": "33333",
                  "instagramActorId": "IG-222",
                  "websiteUrl": "https://example.com/a",
                  "message": "Mensagem A",
                  "callToActionType": "LEARN_MORE",
                  "headline": "Headline A",
                  "description": "Desc A"
                },
                {
                  "id": "creativeB",
                  "pageId": "33333",
                  "instagramActorId": "IG-222",
                  "websiteUrl": "https://example.com/b",
                  "message": "Mensagem B",
                  "callToActionType": "SIGN_UP",
                  "headline": "Headline B",
                  "description": "Desc B"
                }
              ],
              "ads": [
                {
                  "id": "adA",
                  "name": "Exp Multi - Ad 1",
                  "adSetId": "adset-multi",
                  "creativeId": "creativeA"
                },
                {
                  "id": "adB",
                  "name": "Exp Multi - Ad 2",
                  "adSetId": "adset-multi",
                  "creativeId": "creativeB"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        ArgumentCaptor<FacebookAdsAdCreative> creativeCaptor = ArgumentCaptor.forClass(FacebookAdsAdCreative.class);
        verify(adCreativeRepository, times(2)).save(creativeCaptor.capture());
        assertThat(creativeCaptor.getAllValues())
                .extracting(FacebookAdsAdCreative::getId)
                .containsExactly("creativeA", "creativeB");

        ArgumentCaptor<FacebookAdsAd> adCaptor = ArgumentCaptor.forClass(FacebookAdsAd.class);
        verify(adRepository, times(2)).save(adCaptor.capture());
        assertThat(adCaptor.getAllValues())
                .extracting(ad -> ad.getCreative().getId())
                .containsExactly("creativeA", "creativeB");
    }


}
