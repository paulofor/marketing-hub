package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Consulta na Graph API o status efetivo de campanhas e seus filhos. */
@Component
public class FacebookCampaignStatusSnapshotClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignStatusSnapshotClient.class);
    private final WebClient webClient;
    private final String apiVersion;

    /** Inicializa o client com a base e versão configuradas da Graph API. */
    public FacebookCampaignStatusSnapshotClient(
            WebClient.Builder builder,
            @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
            @Value("${facebook.graph-api.version:v23.0}") String apiVersion) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiVersion = normalizeVersion(apiVersion);
    }

    /** Busca janela, orçamento e status efetivo da campanha, dos ad sets e dos anúncios. */
    public JsonNode fetch(String campaignId, String accessToken) {
        String fields = "start_time,stop_time,status,effective_status,budget_remaining,lifetime_budget,adsets{start_time,end_time,status,effective_status,budget_remaining,daily_budget,lifetime_budget,ads{status,effective_status}}";
        String safeUrl = "/" + apiVersion + "/" + campaignId;
        LOGGER.info(
                "Meta campaign status snapshot request: url==>{}, payload={}",
                safeUrl,
                JsonLogFormatter.wrap(java.util.Map.of("fields", fields)));
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(safeUrl)
                        .queryParam("fields", fields)
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        LOGGER.info(
                "Meta campaign status snapshot response: url<=={}, response={}",
                safeUrl,
                JsonLogFormatter.wrap(response));
        return response;
    }

    /** Normaliza a versão da Graph API para o formato esperado pela URL. */
    private String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "v23.0";
        }
        String trimmed = version.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.startsWith("v") ? trimmed : "v" + trimmed;
    }
}
