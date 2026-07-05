package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sincroniza métricas de campanhas Facebook na Graph API e envia o agregado ao backend.
 */
@Service
public class FacebookCampaignMetricsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignMetricsService.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookAccessTokenManager accessTokenManager;
    private final WebClient backendClient;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final FacebookCampaignStatusSnapshotClient statusSnapshotClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final AtomicBoolean configurationUnavailableWarningLogged = new AtomicBoolean(false);

    /**
     * Inicializa o sincronizador com clientes da Meta, backend e configuração operacional.
     */
    public FacebookCampaignMetricsService(FacebookAdsService facebookAdsService,
                                          FacebookAccessTokenManager accessTokenManager,
                                          FacebookWorkerConfigurationClient configurationClient,
                                          FacebookCampaignStatusSnapshotClient statusSnapshotClient,
                                          WebClient.Builder builder,
                                          @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                          @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.configurationClient = configurationClient;
        this.statusSnapshotClient = statusSnapshotClient;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /**
     * Busca a configuração ativa e sincroniza métricas de todas as campanhas elegíveis.
     */
    public void syncCampaignMetrics() {
        Optional<FacebookWorkerConfiguration> configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            if (configurationUnavailableWarningLogged.compareAndSet(false, true)) {
                LOGGER.warn("Facebook worker configuration is unavailable; skipping campaign metrics sync");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);

        FacebookWorkerConfiguration config = configuration.get();
        if (!StringUtils.hasText(config.accessToken())) {
            LOGGER.warn("Facebook worker configuration does not include an access token; skipping campaign metrics sync");
            return;
        }
        ensureAccessToken(config.accessToken());

        List<CampaignMetricsSyncTarget> targets = fetchSyncTargets();
        if (targets.isEmpty()) {
            return;
        }

        for (CampaignMetricsSyncTarget target : targets) {
            processTarget(target);
        }
    }

    /**
     * Processa uma campanha elegível consultando insights e reportando sucesso ou erro ao backend.
     */
    private void processTarget(CampaignMetricsSyncTarget target) {
        try {
            syncStatusSnapshot(target.campaignId());
            JsonNode insights = facebookAdsService.getCampaignInsights(target.campaignId(), buildInsightsQuery());
            JsonNode data = insights.path("data");
            if (!data.isArray() || data.isEmpty()) {
                sendMetrics(target.campaignId(), buildEmptyMetricsPayload());
                return;
            }
            JsonNode row = data.get(0);
            CampaignMetricsUpdateRequest payload = mapToPayload(row);
            if (payload == null) {
                reportMetricsError(target.campaignId(), "Could not parse insights payload");
                return;
            }
            sendMetrics(target.campaignId(), payload);
        } catch (FacebookAccessTokenExpiredException ex) {
            LOGGER.warn("Facebook access token expired while fetching metrics for campaign {}", target.campaignId(), ex);
            if (tryRenewAccessToken()) {
                processTarget(target);
            }
        } catch (FacebookPermissionException ex) {
            LOGGER.warn("Facebook permission error while fetching metrics for campaign {}: {}", target.campaignId(), ex.getMessage(), ex);
            reportMetricsError(target.campaignId(), "Permission error: " + ex.getMessage());
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Backend connectivity error while syncing metrics for campaign {}", target.campaignId(), ex);
        } catch (Exception ex) {
            LOGGER.warn("Unexpected error while syncing metrics for campaign {}: {}", target.campaignId(), ex.getMessage(), ex);
            reportMetricsError(target.campaignId(), "Unexpected error: " + ex.getMessage());
        }
    }

    /**
     * Consulta status efetivo na Meta e envia o retrato ao backend para manter o painel coerente.
     */
    private void syncStatusSnapshot(String campaignId) {
        try {
            JsonNode snapshot = statusSnapshotClient.fetch(campaignId, facebookAdsService.getCurrentAccessToken());
            if (snapshot == null || snapshot.isNull()) {
                return;
            }
            CampaignStatusSyncRequest payload = mapStatusSnapshot(snapshot);
            sendStatusSync(campaignId, payload);
        } catch (Exception ex) {
            LOGGER.warn("Could not sync Facebook status snapshot for campaign {}: {}", campaignId, ex.getMessage(), ex);
        }
    }

    /**
     * Converte o retrato bruto da Meta em contrato enxuto de status para o backend.
     */
    private CampaignStatusSyncRequest mapStatusSnapshot(JsonNode snapshot) {
        List<CampaignStatusSyncRequest.AdSetStatus> adSets = new java.util.ArrayList<>();
        List<CampaignStatusSyncRequest.AdStatus> ads = new java.util.ArrayList<>();
        JsonNode adSetData = snapshot.path("adsets").path("data");
        if (adSetData.isArray()) {
            for (JsonNode adSetNode : adSetData) {
                String adSetId = adSetNode.path("id").asText(null);
                adSets.add(new CampaignStatusSyncRequest.AdSetStatus(
                    adSetId,
                    adSetNode.path("status").asText(null),
                    adSetNode.path("effective_status").asText(null)
                ));
                JsonNode adData = adSetNode.path("ads").path("data");
                if (adData.isArray()) {
                    for (JsonNode adNode : adData) {
                        ads.add(new CampaignStatusSyncRequest.AdStatus(
                            adNode.path("id").asText(null),
                            adNode.path("status").asText(null),
                            adNode.path("effective_status").asText(null)
                        ));
                    }
                }
            }
        }
        return new CampaignStatusSyncRequest(
            snapshot.path("status").asText(null),
            snapshot.path("effective_status").asText(null),
            adSets,
            ads
        );
    }

    /**
     * Monta os parâmetros canônicos de consulta de insights de campanha na Meta.
     */
    private Map<String, String> buildInsightsQuery() {
        Map<String, String> params = new HashMap<>();
        params.put("fields", "campaign_name,reach,impressions,clicks,spend,actions,date_start,date_stop");
        params.put("date_preset", "maximum");
        params.put("time_increment", "all_days");
        return params;
    }

    /**
     * Converte a linha de insights da Meta no payload aceito pelo backend.
     */
    private CampaignMetricsUpdateRequest mapToPayload(JsonNode row) {
        if (row == null || row.isNull()) {
            return null;
        }
        LocalDate dateStart = parseDate(row.path("date_start").asText(null));
        LocalDate dateStop = parseDate(row.path("date_stop").asText(null));
        Long reach = parseLong(row.path("reach"));
        Long impressions = parseLong(row.path("impressions"));
        Long clicks = parseLong(row.path("clicks"));
        BigDecimal spend = parseBigDecimal(row.path("spend"));
        Long leads = extractLeadCount(row.path("actions"));
        return new CampaignMetricsUpdateRequest(dateStart, dateStop, reach, impressions, clicks, leads, spend);
    }

    /**
     * Cria payload zerado quando a Meta não retorna linha de insights para a campanha.
     */
    private CampaignMetricsUpdateRequest buildEmptyMetricsPayload() {
        return new CampaignMetricsUpdateRequest(null, null, 0L, 0L, 0L, 0L, BigDecimal.ZERO);
    }

    /**
     * Lê um número inteiro longo de um nó JSON, retornando zero quando ausente ou inválido.
     */
    private Long parseLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0L;
        }
        try {
            return node.asLong();
        } catch (RuntimeException ex) {
            LOGGER.debug("Could not parse Long from metrics payload: {}", node, ex);
            return 0L;
        }
    }

    /**
     * Converte uma data textual ISO-8601 da Meta para LocalDate.
     */
    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            LOGGER.debug("Could not parse LocalDate from metrics payload value {}", value, ex);
            return null;
        }
    }

    /**
     * Lê valor decimal de um nó JSON, retornando zero quando ausente ou inválido.
     */
    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            LOGGER.debug("Could not parse BigDecimal from metrics payload value {}", text, ex);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Soma ações de lead retornadas pela Meta no payload de insights.
     */
    private long extractLeadCount(JsonNode actionsNode) {
        if (actionsNode == null || !actionsNode.isArray()) {
            return 0L;
        }
        long total = 0;
        for (JsonNode action : actionsNode) {
            String type = action.path("action_type").asText("");
            if (!StringUtils.hasText(type)) {
                continue;
            }
            if (type.toLowerCase().contains("lead")) {
                total += parseLong(action.path("value"));
            }
        }
        return total;
    }

    /**
     * Envia ao backend as métricas sincronizadas de uma campanha.
     */
    private void sendMetrics(String campaignId, CampaignMetricsUpdateRequest payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/metrics");
        backendClient.post()
            .uri(url)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> response.createException().flatMap(e -> {
                LOGGER.warn("Backend rejected metrics for campaign {}: status={} message={}", campaignId, response.statusCode(), e.getMessage());
                return Mono.error(e);
            }))
            .toBodilessEntity()
            .block();
    }

    /**
     * Envia ao backend o status efetivo sincronizado da campanha e seus filhos.
     */
    private void sendStatusSync(String campaignId, CampaignStatusSyncRequest payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/status-sync");
        backendClient.post()
            .uri(url)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> response.createException().flatMap(e -> {
                LOGGER.warn("Backend rejected status sync for campaign {}: status={} message={}", campaignId, response.statusCode(), e.getMessage());
                return Mono.error(e);
            }))
            .toBodilessEntity()
            .block();
    }

    /**
     * Registra no backend uma falha de sincronização de métricas da campanha.
     */
    private void reportMetricsError(String campaignId, String message) {
        String sanitized = sanitizeMessage(message);
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/metrics-error");
        CampaignMetricsErrorRequest request = new CampaignMetricsErrorRequest(sanitized);
        try {
            backendClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception ex) {
            LOGGER.warn("Could not report metrics error for campaign {}: {}", campaignId, ex.getMessage(), ex);
        }
    }

    /**
     * Limita mensagens de erro para persistência segura no backend.
     */
    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * Busca no backend as campanhas que precisam de sincronização de métricas.
     */
    private List<CampaignMetricsSyncTarget> fetchSyncTargets() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/metrics/sync-targets");
        try {
            List<CampaignMetricsSyncTarget> targets = backendClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CampaignMetricsSyncTarget.class)
                .collectList()
                .block();
            LOGGER.debug("Fetched {} campaign metrics sync targets", targets == null ? 0 : targets.size());
            return targets != null ? targets : Collections.emptyList();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Could not fetch campaign metrics sync targets from backend: url==>{}", url, ex);
            return Collections.emptyList();
        }
    }

    /**
     * Atualiza o token usado nas chamadas da Meta quando a configuração mudou.
     */
    private void ensureAccessToken(String configuredToken) {
        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!StringUtils.hasText(currentToken) || !currentToken.equals(configuredToken)) {
            facebookAdsService.updateAccessToken(configuredToken);
        }
    }

    /**
     * Tenta renovar o token e informa se a sincronização pode ser repetida.
     */
    private boolean tryRenewAccessToken() {
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info("Facebook access token renewed automatically during metrics sync");
            return true;
        }
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED) {
            LOGGER.warn("Automatic token renewal is not configured; metrics sync will remain paused until a valid token is provided");
        } else {
            LOGGER.warn("Automatic token renewal failed: {}", renewalResult.errorMessage());
        }
        return false;
    }

    public record CampaignMetricsSyncTarget(String campaignId, long experimentId, Instant lastSyncedAt) {}

    public record CampaignMetricsUpdateRequest(
            LocalDate dateStart,
            LocalDate dateStop,
            Long reach,
            Long impressions,
            Long clicks,
            Long leads,
            BigDecimal spend) {}

    public record CampaignMetricsErrorRequest(String message) {}

    public record CampaignStatusSyncRequest(
            String status,
            String effectiveStatus,
            List<AdSetStatus> adSets,
            List<AdStatus> ads) {

        public record AdSetStatus(String id, String status, String effectiveStatus) {}

        public record AdStatus(String id, String status, String effectiveStatus) {}
    }
}
