package com.marketinghub.facebookadsworker.facebookpixel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class FacebookPixelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookPixelService.class);

    private final FacebookAdsService facebookAdsService;
    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final ObjectMapper objectMapper;
    private final boolean pixelsEnabled;

    public FacebookPixelService(FacebookAdsService facebookAdsService,
                                WebClient.Builder builder,
                                FacebookWorkerConfigurationClient configurationClient,
                                @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                @Value("${backend.api-prefix:/api}") String apiPrefix,
                                ObjectMapper objectMapper,
                                @Value("${facebookpixel.enabled:false}") boolean pixelsEnabled) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.configurationClient = configurationClient;
        this.objectMapper = objectMapper;
        this.pixelsEnabled = pixelsEnabled;
    }

    public void syncPixelsAndConversions() {
        if (!pixelsEnabled) {
            LOGGER.debug("Facebook pixel sync disabled via configuration; skipping execution");
            return;
        }
        var configOptional = configurationClient.fetchConfiguration();
        if (configOptional.isEmpty()) {
            return;
        }
        FacebookWorkerConfiguration config = configOptional.get();
        if (!StringUtils.hasText(config.accessToken())) {
            LOGGER.warn("Facebook worker configuration is missing access token; skipping pixel sync");
            return;
        }
        try {
            facebookAdsService.updateAccessToken(config.accessToken());
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Invalid Facebook access token in worker configuration: {}", ex.getMessage());
            return;
        }

        createPixelsForReadyExperiments(config);
        sendConversions();
    }

    private void createPixelsForReadyExperiments(FacebookWorkerConfiguration config) {
        if (!StringUtils.hasText(config.adAccountId())) {
            LOGGER.warn("Facebook ad account id is not configured; skipping pixel creation");
            return;
        }
        if (!StringUtils.hasText(config.systemUserAccessToken())) {
            LOGGER.warn("Facebook system user access token is not configured; skipping pixel creation");
            return;
        }
        String systemUserToken = config.systemUserAccessToken().trim();
        if (!StringUtils.hasText(config.pixelOwnerBusinessId())) {
            LOGGER.warn("Facebook pixel owner business id is not configured; skipping pixel creation");
            return;
        }
        String pixelOwnerBusinessId = config.pixelOwnerBusinessId().trim();
        List<ExperimentPixel> experiments = fetchExperimentsReadyForPixel();
        if (experiments.isEmpty()) {
            return;
        }
        for (ExperimentPixel exp : experiments) {
            try {
                String pixelName = buildPixelName(exp);
                String pixelId = facebookAdsService.createPixel(
                    config.adAccountId(),
                    pixelName,
                    pixelOwnerBusinessId,
                    systemUserToken
                );
                String pixelCode = facebookAdsService.fetchPixelCode(pixelId, systemUserToken);
                registerPixel(exp.experimentId(), pixelId, pixelCode);
            } catch (Exception ex) {
                LOGGER.error(
                    "Failed to create pixel for experiment {} ({}): {}",
                    exp.experimentId(),
                    exp.experimentName(),
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    private void sendConversions() {
        List<PixelConversion> conversions = fetchConversionsReady();
        if (conversions.isEmpty()) {
            return;
        }
        for (PixelConversion conversion : conversions) {
            try {
                String eventId = StringUtils.hasText(conversion.paymentId())
                        ? conversion.paymentId()
                        : String.valueOf(conversion.purchaseId());
                facebookAdsService.sendPurchaseEvent(
                    conversion.pixelId(),
                    eventId,
                    conversion.amount(),
                    conversion.normalizedCurrency(),
                    conversion.paymentApprovedAt()
                );
                acknowledgeConversion(conversion.purchaseId());
            } catch (Exception ex) {
                LOGGER.error(
                    "Failed to send pixel conversion for purchase {} (experiment {}): {}",
                    conversion.purchaseId(),
                    conversion.experimentId(),
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    private List<ExperimentPixel> fetchExperimentsReadyForPixel() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/experiments-ready");
        LOGGER.info(
            "Requesting experiments ready for pixel creation: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(Collections.emptyMap())
        );
        try {
            List<ExperimentPixel> experiments = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(ExperimentPixel.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received experiments for pixel creation: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, experiments)
            );
            return experiments != null ? experiments : List.of();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch experiments ready for pixel creation: url==>{}", url, ex);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend responded with error when fetching experiments ready for pixel creation: status={}, body={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        }
        return List.of();
    }

    private void registerPixel(long experimentId, String pixelId, String pixelCode) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels");
        PixelCreationRequest request = new PixelCreationRequest(experimentId, pixelId, pixelCode, Instant.now());
        LOGGER.info(
            "Registering Facebook pixel in backend: url==>{}, body={}",
            url,
            JsonLogFormatter.wrap(objectMapper, request)
        );
        try {
            backendClient
                .post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(Void.class)
                .block();
            LOGGER.info(
                "Backend acknowledged Facebook pixel creation: url<=={}, experimentId={}, pixelId={}",
                url,
                experimentId,
                pixelId
            );
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Failed to register Facebook pixel in backend: url==>{}, experimentId={}, pixelId={}",
                url,
                experimentId,
                pixelId,
                ex
            );
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend returned error while registering pixel: status={}, body={}, experimentId={}, pixelId={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                experimentId,
                pixelId,
                ex
            );
        }
    }

    private List<PixelConversion> fetchConversionsReady() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/conversions-ready");
        LOGGER.info("Requesting approved purchases pending pixel conversion: url==>{}", url);
        try {
            List<PixelConversion> conversions = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(PixelConversion.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received pixel conversion candidates: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, conversions)
            );
            return conversions != null ? conversions : List.of();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch pixel conversions from backend: url==>{}", url, ex);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend responded with error when fetching pixel conversions: status={}, body={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        }
        return List.of();
    }

    private void acknowledgeConversion(long purchaseId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/conversions/" + purchaseId + "/ack");
        LOGGER.info("Acknowledging pixel conversion in backend: url==>{}, purchaseId={}", url, purchaseId);
        try {
            backendClient
                .post()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(Void.class)
                .block();
            LOGGER.info("Conversion acknowledged: url<=={}, purchaseId={}", url, purchaseId);
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to acknowledge pixel conversion: url==>{}, purchaseId={}", url, purchaseId, ex);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend returned error while acknowledging pixel conversion: status={}, body={}, purchaseId={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                purchaseId,
                ex
            );
        }
    }

    private String buildPixelName(ExperimentPixel exp) {
        if (StringUtils.hasText(exp.experimentName())) {
            return "Pixel - " + exp.experimentName();
        }
        return "Pixel - Experiment " + exp.experimentId();
    }

    public record ExperimentPixel(long experimentId, String experimentName) {}

    public record PixelCreationRequest(long experimentId, String pixelId, String pixelCode, Instant createdAt) {}

    public record PixelConversion(
        long purchaseId,
        long experimentId,
        String experimentName,
        String pixelId,
        String paymentId,
        BigDecimal amount,
        String currency,
        Instant paymentApprovedAt
    ) {
        public String normalizedCurrency() {
            return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
        }
    }
}
