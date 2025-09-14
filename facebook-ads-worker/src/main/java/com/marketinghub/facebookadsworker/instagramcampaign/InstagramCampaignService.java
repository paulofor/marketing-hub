package com.marketinghub.facebookadsworker.instagramcampaign;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class InstagramCampaignService {
    private final FacebookAdsService facebookAdsService;
    private final WebClient backendClient;

    public InstagramCampaignService(FacebookAdsService facebookAdsService,
                                    WebClient.Builder builder,
                                    @Value("${backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = builder.baseUrl(backendBaseUrl).build();
    }

    public void createCampaignsFromAuthorizedCreatives() {
        List<Creative> creatives = backendClient.get()
            .uri("/api/instagram-creatives/approved")
            .retrieve()
            .bodyToFlux(Creative.class)
            .collectList()
            .block();

        if (creatives != null) {
            creatives.forEach(this::processCreative);
        }
    }

    private void processCreative(Creative creative) {
        String campaignId = facebookAdsService.createInstagramCampaign(creative.adAccountId(), creative.name());
        backendClient.post()
            .uri("/api/instagram-creatives/" + creative.id() + "/campaign")
            .bodyValue(Map.of("campaignId", campaignId))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public record Creative(long id, String adAccountId, String name) {}
}

