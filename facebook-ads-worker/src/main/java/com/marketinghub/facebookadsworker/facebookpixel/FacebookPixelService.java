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

/**
 * Orquestra a criação de pixels solicitados no backend e o envio de conversões para a Meta.
 */
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

    // Configura integrações e chaves operacionais sem acessar o banco diretamente.
    public FacebookPixelService(FacebookAdsService facebookAdsService,
                                WebClient.Builder builder,
                                FacebookWorkerConfigurationClient configurationClient,
                                @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
                                @Value("${backend.api-prefix:/api}") String apiPrefix,
                                ObjectMapper objectMapper,
                                @Value("${facebookpixel.enabled:true}") boolean pixelsEnabled) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.configurationClient = configurationClient;
        this.objectMapper = objectMapper;
        this.pixelsEnabled = pixelsEnabled;
    }

    // Sincroniza pixels e as duas fontes autoritativas de conversões em uma única rodada.
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
            LOGGER.error("Invalid Facebook access token in worker configuration", ex);
            return;
        }

        createPixelsForPendingRequests(config);
        sendConversions();
        sendPdeConversions();
    }

    // Cria pixels pendentes usando o token operacional disponível e sem bloquear quando o Business owner não foi configurado.
    private void createPixelsForPendingRequests(FacebookWorkerConfiguration config) {
        if (!StringUtils.hasText(config.adAccountId())) {
            LOGGER.warn("Facebook ad account id is not configured; skipping pixel creation");
            return;
        }
        String pixelAccessToken = resolvePixelAccessToken(config);
        if (!StringUtils.hasText(pixelAccessToken)) {
            LOGGER.warn("Facebook access token is not configured; skipping pixel creation");
            return;
        }
        String pixelOwnerBusinessId = StringUtils.hasText(config.pixelOwnerBusinessId())
            ? config.pixelOwnerBusinessId().trim()
            : null;
        if (!StringUtils.hasText(pixelOwnerBusinessId)) {
            LOGGER.warn(
                "Facebook pixel owner business id is not configured; creating pixel without owner_business to avoid blocking requested niches"
            );
        }
        List<NichePixel> niches = fetchPendingPixelRequests();
        if (niches.isEmpty()) {
            return;
        }
        for (NichePixel niche : niches) {
            try {
                String pixelName = buildPixelName(niche);
                String pixelId = facebookAdsService.createPixel(
                    config.adAccountId(),
                    pixelName,
                    pixelOwnerBusinessId,
                    pixelAccessToken
                );
                String pixelCode = facebookAdsService.fetchPixelCode(pixelId, pixelAccessToken);
                registerPixel(niche.nicheId(), pixelId, pixelCode);
            } catch (Exception ex) {
                LOGGER.error(
                    "Failed to create pixel for niche {} ({}): {}",
                    niche.nicheId(),
                    niche.nicheName(),
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    // Resolve o token usado nas operações de pixel priorizando o system user e usando o token principal como contingência.
    private String resolvePixelAccessToken(FacebookWorkerConfiguration config) {
        if (StringUtils.hasText(config.systemUserAccessToken())) {
            return config.systemUserAccessToken().trim();
        }
        return StringUtils.hasText(config.accessToken()) ? config.accessToken().trim() : null;
    }

    // Envia compras legadas do Lead Portal com os campos mínimos exigidos pelo CAPI.
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
                    conversion.paymentApprovedAt(),
                    conversion.hashedEmail(),
                    conversion.eventSourceUrl()
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

    // Envia compras PDE verificadas e confirma a referência financeira somente após sucesso da Meta.
    private void sendPdeConversions() {
        List<PdePixelConversion> conversions = fetchPdeConversionsReady();
        for (PdePixelConversion conversion : conversions) {
            try {
                facebookAdsService.sendPurchaseEvent(
                    conversion.pixelId(),
                    conversion.eventId(),
                    conversion.amount(),
                    conversion.normalizedCurrency(),
                    conversion.paymentApprovedAt(),
                    conversion.hashedEmail(),
                    conversion.eventSourceUrl()
                );
                acknowledgePdeConversion(conversion.sourceReference(), conversion.pixelId());
            } catch (Exception ex) {
                LOGGER.error(
                    "Failed to send PDE pixel conversion for source {} (experiment {}): {}",
                    conversion.sourceReference(),
                    conversion.experimentId(),
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    // Consulta no backend as solicitações de pixel prontas para criação.
    private List<NichePixel> fetchPendingPixelRequests() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/pending");
        LOGGER.info(
            "Requesting pending pixel creation requests: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(Collections.emptyMap())
        );
        try {
            List<NichePixel> niches = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(NichePixel.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received pending pixel creation requests: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, niches)
            );
            return niches != null ? niches : List.of();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch pending pixel creation requests: url==>{}", url, ex);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend responded with error when fetching pending pixel creation requests: status={}, body={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        }
        return List.of();
    }

    // Devolve ao backend o pixel criado e o snippet recebido da Meta.
    private void registerPixel(long nicheId, String pixelId, String pixelCode) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels");
        PixelCreationRequest request = new PixelCreationRequest(nicheId, pixelId, pixelCode, Instant.now());
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
                "Backend acknowledged Facebook pixel creation: url<=={}, nicheId={}, pixelId={}",
                url,
                nicheId,
                pixelId
            );
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Failed to register Facebook pixel in backend: url==>{}, nicheId={}, pixelId={}",
                url,
                nicheId,
                pixelId,
                ex
            );
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend returned error while registering pixel: status={}, body={}, nicheId={}, pixelId={}",
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                nicheId,
                pixelId,
                ex
            );
        }
    }

    // Consulta compras legadas pendentes preservando compatibilidade com o Lead Portal.
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

    // Consulta pagamentos PDE aprovados ainda não confirmados na trilha idempotente.
    private List<PdePixelConversion> fetchPdeConversionsReady() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/pde-conversions-ready");
        LOGGER.info("Requesting approved PDE payments pending CAPI conversion: url==>{}", url);
        try {
            List<PdePixelConversion> conversions = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(PdePixelConversion.class)
                .collectList()
                .block();
            LOGGER.info(
                "Received PDE CAPI conversion candidates: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(objectMapper, conversions)
            );
            return conversions != null ? conversions : List.of();
        } catch (WebClientRequestException ex) {
            LOGGER.warn("Failed to fetch PDE CAPI conversions: url==>{}", url, ex);
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend responded with error when fetching PDE CAPI conversions: url<=={}, status={}, body={}",
                url,
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                ex
            );
        }
        return List.of();
    }

    // Confirma a compra legada depois que a Graph API aceitou o evento.
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

    // Confirma a entrega PDE com referência e pixel para impedir ACK cruzado entre nichos.
    private void acknowledgePdeConversion(String sourceReference, String pixelId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-pixels/pde-conversions/ack");
        PdeConversionAck request = new PdeConversionAck(sourceReference, pixelId);
        LOGGER.info(
            "Acknowledging PDE CAPI conversion: url==>{}, body={}",
            url,
            JsonLogFormatter.wrap(objectMapper, request)
        );
        try {
            backendClient
                .post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.createException().flatMap(Mono::error))
                .bodyToMono(Void.class)
                .block();
            LOGGER.info(
                "PDE CAPI conversion acknowledged: url<=={}, sourceReference={}",
                url,
                sourceReference
            );
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Failed to acknowledge PDE CAPI conversion: url==>{}, sourceReference={}",
                url,
                sourceReference,
                ex
            );
        } catch (WebClientResponseException ex) {
            LOGGER.error(
                "Backend returned error while acknowledging PDE CAPI conversion: url<=={}, status={}, body={}, sourceReference={}",
                url,
                ex.getRawStatusCode(),
                ex.getResponseBodyAsString(),
                sourceReference,
                ex
            );
        }
    }

    // Constrói um nome operacional estável para o pixel do nicho.
    private String buildPixelName(NichePixel niche) {
        if (StringUtils.hasText(niche.nicheName())) {
            return "Pixel - " + niche.nicheName();
        }
        return "Pixel - Niche " + niche.nicheId();
    }

    /** Representa um nicho que aguarda criação de pixel. */
    public record NichePixel(long nicheId, String nicheName) {}

    /** Representa o callback do pixel criado na Meta. */
    public record PixelCreationRequest(long nicheId, String pixelId, String pixelCode, Instant createdAt) {}

    /** Representa uma compra legada pronta para o CAPI. */
    public record PixelConversion(
        long purchaseId,
        long experimentId,
        String experimentName,
        String pixelId,
        String paymentId,
        BigDecimal amount,
        String currency,
        Instant paymentApprovedAt,
        String hashedEmail,
        String eventSourceUrl
    ) {
        // Normaliza a moeda conforme o contrato da Meta.
        public String normalizedCurrency() {
            return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
        }
    }

    /** Representa uma compra PDE verificada e ainda não confirmada na Meta. */
    public record PdePixelConversion(
        String sourceReference,
        long experimentId,
        String experimentName,
        String pixelId,
        String eventId,
        BigDecimal amount,
        String currency,
        Instant paymentApprovedAt,
        String hashedEmail,
        String eventSourceUrl
    ) {
        // Normaliza a moeda conforme o contrato da Meta.
        public String normalizedCurrency() {
            return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
        }
    }

    /** Confirma ao backend a origem e o pixel aceitos pelo CAPI. */
    public record PdeConversionAck(String sourceReference, String pixelId) {}
}
