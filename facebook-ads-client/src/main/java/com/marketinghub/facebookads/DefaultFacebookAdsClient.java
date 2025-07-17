package com.marketinghub.facebookads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão utilizando {@link java.net.http.HttpClient}.
 */
@Component
public class DefaultFacebookAdsClient implements FacebookAdsClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultFacebookAdsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String token;
    private final String apiVersion;
    private final String baseUrl;

    public DefaultFacebookAdsClient(
            @Value("${facebook.token:}") String token,
            @Value("${facebook.version:v19.0}") String apiVersion,
            @Value("${facebook.url:https://graph.facebook.com}") String baseUrl) {
        this.token = token;
        this.apiVersion = apiVersion;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public JsonNode getAdAccounts() {
        if (token == null || token.isBlank()) {
            log.warn("Facebook token not configured, returning empty result");
            return MAPPER.createObjectNode();
        }
        try {
            String endpoint = baseUrl + "/" + apiVersion + "/me/adaccounts?access_token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Facebook response: {}", response.body());
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            log.error("Failed to call Facebook API", e);
            return MAPPER.createObjectNode();
        }
    }
}
