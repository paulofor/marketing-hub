package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class FacebookCampaignService {
    private final FacebookAdsService facebookAdsService;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final String adAccountId;

    public FacebookCampaignService(FacebookAdsService facebookAdsService,
                                   WebClient.Builder builder,
                                   @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix,
                                   @Value("${facebook.ad-account-id}") String adAccountId) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.adAccountId = adAccountId;
    }

    public void createCampaignsFromExperiments() {
        List<Experiment> experiments = backendClient.get()
            .uri(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments-ready"))
            .retrieve()
            .bodyToFlux(Experiment.class)
            .collectList()
            .block();

        if (experiments != null) {
            experiments.forEach(this::processExperiment);
        }
    }

    private void processExperiment(Experiment exp) {
        String campaignId = facebookAdsService.createCampaign(adAccountId, exp.name());
        CreateCampaignRequest req = new CreateCampaignRequest(campaignId, adAccountId, exp.name(), "OUTCOME_TRAFFIC", "CAMPAIGN");
        backendClient.post()
            .uri(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns"))
            .bodyValue(req)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public record Experiment(long id, String name) {}
    public record CreateCampaignRequest(String id, String adAccountId, String name, String objective, String budgetMode) {}
}
