package com.marketinghub.facebookads.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.experiment.repository.AdSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
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
    com.marketinghub.facebookads.FacebookAdsCampaignRepository campaignRepository;
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
    TargetingElementRepository targetingElementRepository;

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
        var exp = Experiment.builder()
                .id(1L)
                .niche(niche)
                .name("Exp")
                .hypothesis("Hipótese")
                .hypothesisRef(hypothesis)
                .kpiTargetCpl(BigDecimal.TEN)
                .stopLossCpl(BigDecimal.valueOf(20))
                .sampleSize(1200)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .creativeApproved(true)
                .journeyTemplate(journeyTemplate)
                .facebookPage(page)
                .instagramAccount(instagramAccount)
                .build();
        when(targetingElementRepository.existsApprovedForExperiment(10L, TargetingElementType.INTEREST, hypothesis.getId()))
                .thenReturn(true);
        when(targetingElementRepository.existsApprovedForExperiment(10L, TargetingElementType.JOB_TITLE, hypothesis.getId()))
                .thenReturn(true);
        when(targetingElementRepository.existsApprovedForExperiment(10L, TargetingElementType.BEHAVIOR, hypothesis.getId()))
                .thenReturn(true);
        when(experimentService.listByStatusAndPlatform(
                com.marketinghub.experiment.ExperimentStatus.PLANNED,
                com.marketinghub.experiment.ExperimentPlatform.FACEBOOK))
                .thenReturn(List.of(exp));
        mockMvc.perform(get("/api/facebook-campaigns/experiments").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Exp"))
                .andExpect(jsonPath("$[0].hypothesis").value("Hipótese"))
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
                .andExpect(jsonPath("$[0].missingConfiguration").isEmpty());
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
              "experimentAdSetId": 101
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
                "creativeId": "creative123"
              }
            }
            """;

        mockMvc.perform(post("/api/facebook-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cmp123"))
                .andExpect(jsonPath("$.adAccountId").value("act_555"))
                .andExpect(jsonPath("$.experiment.id").value(42))
                .andExpect(jsonPath("$.externalId").value("meta-campaign-123"));

        ArgumentCaptor<FacebookAdsCampaign> campaignCaptor = ArgumentCaptor.forClass(FacebookAdsCampaign.class);
        verify(campaignRepository).save(campaignCaptor.capture());
        FacebookAdsCampaign savedCampaign = campaignCaptor.getValue();
        assertThat(savedCampaign.getId()).isEqualTo("cmp123");
        assertThat(savedCampaign.getExternalId()).isEqualTo("meta-campaign-123");

        ArgumentCaptor<FacebookAdsAdSet> adSetCaptor = ArgumentCaptor.forClass(FacebookAdsAdSet.class);
        verify(adSetRepository).save(adSetCaptor.capture());
        FacebookAdsAdSet savedAdSet = adSetCaptor.getValue();
        assertThat(savedAdSet.getId()).isEqualTo("adset123");
        assertThat(savedAdSet.getCampaign().getId()).isEqualTo("cmp123");
        assertThat(savedAdSet.getExternalId()).isEqualTo("meta-adset-123");
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
    }
}
