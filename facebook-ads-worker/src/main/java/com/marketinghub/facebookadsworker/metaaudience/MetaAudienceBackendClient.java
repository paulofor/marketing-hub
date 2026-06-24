package com.marketinghub.facebookadsworker.metaaudience;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Cliente HTTP responsável por consumir e atualizar a fila de audiências Meta no backend. */
@Component
public class MetaAudienceBackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaAudienceBackendClient.class);
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Inicializa o cliente com URL base e prefixo de API do backend. */
    public MetaAudienceBackendClient(WebClient.Builder builder,
                                     @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                     @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Busca audiências pendentes de criação na Meta. */
    public List<PendingAudience> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/meta-audiences/pending") + "?limit=" + limit;
        LOGGER.info("Requesting pending Meta audiences: url==>{}", url);
        try {
            List<PendingAudience> response = backendClient.get().uri(url).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<PendingAudience>>() {}).block();
            List<PendingAudience> payload = response == null ? Collections.emptyList() : response;
            LOGGER.info("Received pending Meta audiences: url<=={}, response={}", url, JsonLogFormatter.wrap(payload));
            return payload;
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error("Failed to fetch pending Meta audiences: url<=={}, message={}", url, ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao buscar audiências pendentes no backend", ex);
        }
    }

    /** Reporta sucesso ou falha da sincronização de audiência ao backend. */
    public void reportSync(Long id, SyncResult payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/meta-audiences/" + id + "/sync");
        LOGGER.info("Reporting Meta audience sync: url==>{}, payload={}", url, JsonLogFormatter.wrap(payload));
        try {
            backendClient.patch().uri(url).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).retrieve().toBodilessEntity().block();
            LOGGER.info("Backend acknowledged Meta audience sync: url<=={}", url);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error("Failed to report Meta audience sync: url<=={}, payload={}, message={}", url, JsonLogFormatter.wrap(payload), ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao reportar audiência ao backend", ex);
        }
    }

    /** Payload de audiência pendente recebido do backend. */
    public record PendingAudience(Long id, Long marketNicheId, String sourceCnaeCode, String audienceName,
                                  String facebookAdAccountId, List<String> emails) {}

    /** Payload de conclusão enviado ao backend. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SyncResult(@JsonProperty("facebookAudienceId") String facebookAudienceId,
                             @JsonProperty("syncedContacts") long syncedContacts,
                             @JsonProperty("status") String status,
                             @JsonProperty("errorMessage") String errorMessage) {}
}
