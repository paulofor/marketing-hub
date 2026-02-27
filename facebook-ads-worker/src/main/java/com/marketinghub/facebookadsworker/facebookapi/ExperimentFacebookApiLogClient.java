package com.marketinghub.facebookadsworker.facebookapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

/**
 * Cliente responsável por registrar chamadas da Graph API associadas a um experimento.
 */
@Component
public class ExperimentFacebookApiLogClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentFacebookApiLogClient.class);

    private final WebClient backendClient;
    private final ObjectMapper objectMapper;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public ExperimentFacebookApiLogClient(WebClient.Builder builder,
                                          ObjectMapper objectMapper,
                                          @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                          @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.backendClient = builder.build();
        this.objectMapper = objectMapper;
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public void logCall(Long experimentId,
                        ExperimentFacebookApiLogContext context,
                        FacebookAdsService.FacebookApiCallDebugInfo debugInfo) {
        if (experimentId == null || context == null || debugInfo == null) {
            return;
        }
        ExperimentFacebookApiLogPayload payload = new ExperimentFacebookApiLogPayload(
            "FACEBOOK",
            debugInfo.endpoint(),
            debugInfo.httpMethod(),
            debugInfo.statusCode(),
            toJsonNode(debugInfo.requestBody()),
            toJsonNode(debugInfo.responseBody()),
            debugInfo.errorMessage(),
            debugInfo.requestedAt(),
            debugInfo.respondedAt()
        );
        ExperimentFacebookApiLogIngestionRequest body = new ExperimentFacebookApiLogIngestionRequest(
            context,
            List.of(payload)
        );
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId + "/facebook-api-logs");
        try {
            backendClient
                .post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception ex) {
            LOGGER.debug("Failed to register Facebook API log for experiment {}: {}", experimentId, ex.getMessage());
        }
    }

    private JsonNode toJsonNode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(raw);
        }
    }

    public record ExperimentFacebookApiLogIngestionRequest(
        ExperimentFacebookApiLogContext context,
        List<ExperimentFacebookApiLogPayload> logs
    ) {
        public ExperimentFacebookApiLogIngestionRequest {
            if (logs == null) {
                logs = Collections.emptyList();
            }
        }
    }

    public record ExperimentFacebookApiLogPayload(
        String provider,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        JsonNode requestPayload,
        JsonNode responsePayload,
        String errorMessage,
        java.time.Instant requestedAt,
        java.time.Instant respondedAt
    ) {}
}
