package com.marketinghub.facebookads.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookads.dto.ExperimentDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Service that periodically fetches campaign insights. */
@Service
public class InsightsService {
    private static final Logger log = LoggerFactory.getLogger(InsightsService.class);
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String token;
    private final String version;
    private final String baseUrl;
    private final String hubUrl;

    public InsightsService(
            @Value("${facebook.token:}") String token,
            @Value("${facebook.version:v19.0}") String version,
            @Value("${facebook.url:https://graph.facebook.com}") String baseUrl,
            @Value("${hub.url:http://localhost:8000/api}") String hubUrl) {
        this.token = token;
        this.version = version;
        this.baseUrl = baseUrl;
        this.hubUrl = hubUrl;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .build();
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void syncInsights() {
        String ids = System.getenv().getOrDefault("FB_CAMPAIGN_IDS", "");
        for (String id : ids.split(",")) {
            if (!id.isBlank()) {
                fetchAndPost(id.trim());
            }
        }
    }

    void fetchAndPost(String campaignId) {
        String url = baseUrl + "/" + version + "/" + campaignId
                + "/insights?fields=impressions,clicks,spend,actions,website_purchase_roas&access_token=" + token;
        Request request = new Request.Builder().url(url).build();
        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                log.warn("Insights call failed: {}", resp.code());
                return;
            }
            JsonNode data = mapper.readTree(resp.body().string())
                    .path("data").get(0);
            List<MetricSnapshotDTO> dtos = new ArrayList<>();
            MetricSnapshotDTO dto = new MetricSnapshotDTO();
            dto.setImpressions(data.path("impressions").asInt());
            dto.setClicks(data.path("clicks").asInt());
            dto.setCost(new BigDecimal(data.path("spend").asText("0")));
            dto.setRoas(new BigDecimal(data.path("website_purchase_roas").asText("0")));
            dtos.add(dto);
            Request backend = new Request.Builder()
                    .url(hubUrl + "/metrics/bulk")
                    .post(RequestBody.create(mapper.writeValueAsBytes(dtos), MediaType.parse("application/json")))
                    .build();
            client.newCall(backend).execute().close();
        } catch (Exception e) {
            log.error("Failed to sync insights", e);
        }
    }

    /** Minimal DTO for sending metrics to backend. */
    public static class MetricSnapshotDTO {
        private Integer impressions;
        private Integer clicks;
        private BigDecimal cost;
        private BigDecimal roas;
        // getters and setters omitted for brevity
        public Integer getImpressions() { return impressions; }
        public void setImpressions(Integer impressions) { this.impressions = impressions; }
        public Integer getClicks() { return clicks; }
        public void setClicks(Integer clicks) { this.clicks = clicks; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
        public BigDecimal getRoas() { return roas; }
        public void setRoas(BigDecimal roas) { this.roas = roas; }
    }
}
