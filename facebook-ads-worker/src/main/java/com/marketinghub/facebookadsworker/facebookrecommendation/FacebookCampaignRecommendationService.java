package com.marketinghub.facebookadsworker.facebookrecommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Coleta sugestões oficiais da Meta para campanhas ativas e reporta o retrato ao backend.
 */
@Service
public class FacebookCampaignRecommendationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignRecommendationService.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookAccessTokenManager accessTokenManager;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final boolean enabled;
    private final AtomicBoolean configurationUnavailableWarningLogged = new AtomicBoolean(false);

    /**
     * Cria o serviço com clientes necessários para consultar a Meta e persistir o resultado via backend.
     */
    public FacebookCampaignRecommendationService(FacebookAdsService facebookAdsService,
                                                 FacebookAccessTokenManager accessTokenManager,
                                                 FacebookWorkerConfigurationClient configurationClient,
                                                 WebClient.Builder builder,
                                                 @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                                 @Value("${backend.api-prefix:/api}") String apiPrefix,
                                                 @Value("${facebookcampaign.recommendations.enabled:true}") boolean enabled) {
        this.facebookAdsService = facebookAdsService;
        this.accessTokenManager = accessTokenManager;
        this.configurationClient = configurationClient;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.enabled = enabled;
    }

    /**
     * Executa um ciclo completo de coleta de sugestões para campanhas ativas.
     */
    public void syncActiveCampaignRecommendations() {
        if (!enabled) {
            return;
        }
        Optional<FacebookWorkerConfiguration> configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            if (configurationUnavailableWarningLogged.compareAndSet(false, true)) {
                LOGGER.warn("Facebook worker configuration is unavailable; skipping campaign recommendation sync");
            }
            return;
        }
        configurationUnavailableWarningLogged.set(false);
        FacebookWorkerConfiguration config = configuration.get();
        if (!StringUtils.hasText(config.accessToken())) {
            LOGGER.warn("Facebook worker configuration does not include an access token; skipping campaign recommendation sync");
            return;
        }
        ensureAccessToken(config.accessToken());
        for (FacebookCampaignRecommendationSyncTarget target : fetchSyncTargets()) {
            processTarget(target);
        }
    }

    /**
     * Coleta e reporta sugestões de uma campanha individual, renovando token quando necessário.
     */
    private void processTarget(FacebookCampaignRecommendationSyncTarget target) {
        String metaCampaignId = StringUtils.hasText(target.externalCampaignId())
                ? target.externalCampaignId()
                : target.campaignId();
        try {
            JsonNode recommendations = facebookAdsService.getCampaignRecommendations(metaCampaignId);
            sendRecommendations(target.campaignId(), new FacebookCampaignRecommendationIngestionRequest(Instant.now(), recommendations));
        } catch (FacebookAccessTokenExpiredException ex) {
            LOGGER.warn("Facebook access token expired while fetching recommendations for campaign {}", target.campaignId(), ex);
            if (tryRenewAccessToken()) {
                processTarget(target);
            }
        } catch (FacebookPermissionException ex) {
            LOGGER.warn("Facebook permission error while fetching recommendations for campaign {}: {}", target.campaignId(), ex.getMessage(), ex);
            reportRecommendationError(target.campaignId(), "Permission error: " + ex.getMessage());
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Backend connectivity error while syncing recommendations for campaign {}", target.campaignId(), ex);
        } catch (Exception ex) {
            LOGGER.warn("Unexpected error while syncing recommendations for campaign {}: {}", target.campaignId(), ex.getMessage(), ex);
            reportRecommendationError(target.campaignId(), "Unexpected error: " + ex.getMessage());
        }
    }

    /**
     * Busca no backend as campanhas ativas elegíveis para coleta de sugestões.
     */
    private List<FacebookCampaignRecommendationSyncTarget> fetchSyncTargets() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/recommendations/sync-targets");
        try {
            List<FacebookCampaignRecommendationSyncTarget> targets = backendClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToFlux(FacebookCampaignRecommendationSyncTarget.class)
                    .collectList()
                    .block();
            return targets != null ? targets : Collections.emptyList();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Could not fetch campaign recommendation sync targets from backend: url==>{}", url, ex);
            return Collections.emptyList();
        }
    }

    /**
     * Envia ao backend o retrato mais recente de sugestões da campanha.
     */
    private void sendRecommendations(String campaignId, FacebookCampaignRecommendationIngestionRequest payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/recommendations");
        backendClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.createException().flatMap(e -> {
                    LOGGER.warn("Backend rejected recommendations for campaign {}: status={} message={}", campaignId, response.statusCode(), e.getMessage());
                    return Mono.error(e);
                }))
                .toBodilessEntity()
                .block();
    }

    /**
     * Registra no backend uma falha de coleta sem apagar sugestões válidas anteriores.
     */
    private void reportRecommendationError(String campaignId, String message) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-campaigns/" + campaignId + "/recommendations-error");
        try {
            backendClient.post()
                    .uri(url)
                    .bodyValue(new FacebookCampaignRecommendationErrorRequest(sanitizeMessage(message)))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            LOGGER.warn("Could not report recommendation sync error for campaign {}: {}", campaignId, ex.getMessage(), ex);
        }
    }

    /**
     * Aplica no cliente da Graph API o token operacional configurado no backend.
     */
    private void ensureAccessToken(String configuredToken) {
        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!StringUtils.hasText(currentToken) || !currentToken.equals(configuredToken)) {
            facebookAdsService.updateAccessToken(configuredToken);
        }
    }

    /**
     * Tenta renovar o token quando a Meta indica expiração durante a coleta.
     */
    private boolean tryRenewAccessToken() {
        FacebookAccessTokenManager.RenewalAttemptResult renewalResult = accessTokenManager.tryRenewAccessTokenIfPossible();
        if (renewalResult.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS) {
            LOGGER.info("Facebook access token renewed automatically during recommendation sync");
            return true;
        }
        LOGGER.warn("Could not renew Facebook access token during recommendation sync: outcome={} message={}",
                renewalResult.outcome(), renewalResult.errorMessage());
        return false;
    }

    /**
     * Limita a mensagem de erro enviada ao backend.
     */
    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Unknown recommendation sync error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * Contrato da campanha ativa que o backend entrega ao worker para coleta de sugestões.
     */
    public record FacebookCampaignRecommendationSyncTarget(
            String campaignId,
            String externalCampaignId,
            Long experimentId,
            String adAccountId,
            Instant lastSyncedAt) {}

    /**
     * Contrato enviado ao backend com as sugestões retornadas pela Meta.
     */
    public record FacebookCampaignRecommendationIngestionRequest(Instant collectedAt, JsonNode recommendations) {}

    /**
     * Contrato enviado ao backend quando a coleta da Meta falha.
     */
    public record FacebookCampaignRecommendationErrorRequest(String message) {}
}
