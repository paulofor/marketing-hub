package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationIngestionRequest;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationService;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsRecommendationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Valida a seleção e a persistência das sugestões oficiais da Meta no backend.
 */
@ExtendWith(MockitoExtension.class)
class FacebookCampaignRecommendationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FacebookAdsCampaignRepository campaignRepository;

    @Mock
    private FacebookAdsRecommendationRepository recommendationRepository;

    private FacebookCampaignRecommendationService service;

    @org.junit.jupiter.api.BeforeEach
    // Prepara o serviço com repositórios simulados para cada teste.
    void setUp() {
        service = new FacebookCampaignRecommendationService(campaignRepository, recommendationRepository, objectMapper);
    }

    @Test
    // Garante que somente campanhas ativas em experimentos em execução viram alvo de coleta.
    void shouldListOnlyActiveCampaignTargets() {
        FacebookAdsCampaign campaign = campaign();
        campaign.setRecommendationsLastSyncedAt(Instant.parse("2026-06-10T10:00:00Z"));
        when(campaignRepository.findAllByExperimentStatusAndStatus(ExperimentStatus.RUNNING, FacebookAdStatus.ACTIVE))
                .thenReturn(List.of(campaign));

        var targets = service.listSyncTargets();

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).campaignId()).isEqualTo("camp-1");
        assertThat(targets.get(0).externalCampaignId()).isEqualTo("meta-camp-1");
        assertThat(targets.get(0).experimentId()).isEqualTo(42L);
        assertThat(targets.get(0).adAccountId()).isEqualTo("12345");
    }

    @Test
    // Garante que a ingestão substitui o retrato anterior e limpa erro antigo.
    void shouldReplaceRecommendationSnapshotAndClearPreviousError() throws Exception {
        FacebookAdsCampaign campaign = campaign();
        campaign.setRecommendationsLastError("erro antigo");
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign));
        var payload = objectMapper.readTree("{\"data\":[{\"code\":\"1\",\"title\":\"Ajuste orçamento\",\"message\":\"Teste\",\"importance\":\"HIGH\",\"confidence\":\"MEDIUM\",\"blame_field\":\"daily_budget\",\"recommendation_data\":{\"value\":10}}]}");
        Instant collectedAt = Instant.parse("2026-06-10T11:00:00Z");

        service.ingest("camp-1", new FacebookCampaignRecommendationIngestionRequest(collectedAt, payload));

        verify(recommendationRepository).deleteByCampaignId("camp-1");
        ArgumentCaptor<FacebookAdsRecommendation> captor = ArgumentCaptor.forClass(FacebookAdsRecommendation.class);
        verify(recommendationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecommendationCode()).isEqualTo("1");
        assertThat(captor.getValue().getTitle()).isEqualTo("Ajuste orçamento");
        assertThat(captor.getValue().getRecommendationDataJson()).contains("value");
        assertThat(campaign.getRecommendationsLastSyncedAt()).isEqualTo(collectedAt);
        assertThat(campaign.getRecommendationsLastError()).isNull();
    }

    // Monta uma campanha válida para os cenários de sugestão.
    private FacebookAdsCampaign campaign() {
        Experiment experiment = new Experiment();
        experiment.setId(42L);
        FacebookAccount account = FacebookAccount.builder().id(7L).name("Conta").build();
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-1");
        campaign.setExternalId("meta-camp-1");
        campaign.setAdAccountId("12345");
        campaign.setName("Campanha");
        campaign.setObjective("OUTCOME_TRAFFIC");
        campaign.setStatus(FacebookAdStatus.ACTIVE);
        campaign.setBudgetMode(BudgetMode.CAMPAIGN);
        campaign.setExperiment(experiment);
        campaign.setFacebookAccount(account);
        return campaign;
    }
}
