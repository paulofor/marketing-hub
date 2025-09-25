package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class FacebookCampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignService.class);

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
        List<Experiment> experiments = Collections.emptyList();

        try {
            experiments = backendClient.get()
                .uri(UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/experiments-ready"))
                .exchangeToFlux(response -> {
                    if (response.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }

                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Mono::error);
                    }

                    return response.bodyToFlux(Experiment.class);
                })
                .collectList()
                .block();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch experiments from backend", ex);
        }

        if (experiments == null || experiments.isEmpty()) {
            return;
        }

        experiments.forEach(this::processExperiment);
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
