package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Consulta na Graph API o status efetivo de campanhas e seus filhos. */
@Component
public class FacebookCampaignStatusSnapshotClient {
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

    /** Busca campanha, ad sets e anúncios com status configurado e efetivo. */
    public JsonNode fetch(String campaignId, String accessToken) {
        String fields = "status,effective_status,adsets{status,effective_status,ads{status,effective_status}}";
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/" + apiVersion + "/" + campaignId)
                        .queryParam("fields", fields)
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
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
