package com.marketinghub.emailservice.service.client;

import com.marketinghub.emailservice.config.MarketingHubClientProperties;
import com.marketinghub.emailservice.exception.RemoteServiceException;
import com.marketinghub.emailservice.exception.TemplateNotFoundException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MarketingHubClient {

    private static final Logger log = LoggerFactory.getLogger(MarketingHubClient.class);

    private final RestClient marketingHubRestClient;
    private final MarketingHubClientProperties properties;

    public MarketingHubClient(@Qualifier("marketingHubRestClient") RestClient marketingHubRestClient,
                              MarketingHubClientProperties properties) {
        this.marketingHubRestClient = marketingHubRestClient;
        this.properties = properties;
    }

    public MarketingHubTemplateResponse fetchTemplate(String templateId) {
        String uri = buildTemplateUri(templateId);
        try {
            ResponseEntity<MarketingHubTemplateResponse> responseEntity = marketingHubRestClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(MarketingHubTemplateResponse.class);

            if (responseEntity.getStatusCode() == HttpStatus.NOT_FOUND || responseEntity.getBody() == null) {
                throw new TemplateNotFoundException(templateId);
            }

            return responseEntity.getBody();
        } catch (RestClientException ex) {
            log.error("Erro ao buscar template {} no Marketing Hub", templateId, ex);
            throw new RemoteServiceException("Falha ao consultar template no Marketing Hub", ex);
        }
    }

    public Map<String, Object> fetchDynamicVariables(String templateId) {
        String uri = buildTemplateUri(templateId) + "/variables";
        try {
            ResponseEntity<Map<String, Object>> responseEntity = marketingHubRestClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});
            Map<String, Object> body = responseEntity.getBody();
            return body != null ? Map.copyOf(body) : Map.of();
        } catch (RestClientException ex) {
            log.warn("Falha ao buscar variáveis dinâmicas do template {}. Prosseguindo com apenas as variáveis informadas no payload.", templateId);
            return Map.of();
        }
    }

    private String buildTemplateUri(String templateId) {
        String path = properties.templatesPath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path + templateId;
    }
}
