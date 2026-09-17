package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.CommercialOfferResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Consulta a oferta comercial canônica sem permitir que o PDE acesse o banco principal. */
@Service
public class CommercialOfferService {
    private static final Logger log = LoggerFactory.getLogger(CommercialOfferService.class);

    private final RestClient.Builder restClientBuilder;
    private final List<String> marketingHubBaseUrls;
    private final String internalToken;

    /** Inicializa a integração com as bases oficiais e seus fallbacks operacionais. */
    @Autowired
    public CommercialOfferService(
            RestClient.Builder restClientBuilder,
            @Value("${pde.catalog.marketing-hub-base-url:}") String marketingHubBaseUrl,
            @Value("${pde.internal-api.token:}") String internalToken) {
        this.restClientBuilder = restClientBuilder;
        this.marketingHubBaseUrls = parseBaseUrls(marketingHubBaseUrl);
        this.internalToken = internalToken == null ? "" : internalToken;
    }

    /** Mantém os testes legados sem segredo quando exercitam somente contratos já publicados. */
    CommercialOfferService(RestClient.Builder restClientBuilder, String marketingHubBaseUrl) {
        this(restClientBuilder, marketingHubBaseUrl, "");
    }

    /** Obtém uma oferta completa ou falha fechado para não renderizar venda sem contrato. */
    public CommercialOfferResponse getOffer(String productSlug) {
        return getOffer(productSlug, "");
    }

    /** Encaminha o slot derivado do host para manter a oferta atribuída à versão exibida. */
    public CommercialOfferResponse getOffer(String productSlug, String host) {
        String slotCode = resolveSlotCode(host);
        for (String baseUrl : marketingHubBaseUrls) {
            try {
                CommercialOfferResponse offer = restClientBuilder.clone()
                        .baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri(uriBuilder -> {
                            var uri = uriBuilder.path("/api/products/public/{slug}/commercial-offer");
                            if (StringUtils.hasText(slotCode)) {
                                uri.queryParam("slotCode", slotCode);
                            }
                            return uri.build(productSlug);
                        })
                        .retrieve()
                        .body(CommercialOfferResponse.class);
                if (offer != null) {
                    return offer;
                }
            } catch (RuntimeException ex) {
                log.warn(
                        "Falha ao carregar oferta comercial do Marketing Hub: productSlug={}, baseUrl={}",
                        productSlug,
                        baseUrl,
                        ex);
                Optional<CommercialOfferResponse> candidate =
                        loadValidationOffer(baseUrl, productSlug, slotCode);
                if (candidate.isPresent()) {
                    return candidate.get();
                }
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Oferta comercial indisponível no Marketing Hub.");
    }

    /**
     * Consulta a candidata exata pelo canal autenticado somente quando a rota pública ainda não foi
     * promovida.
     */
    private Optional<CommercialOfferResponse> loadValidationOffer(
            String baseUrl, String productSlug, String slotCode) {
        if (!StringUtils.hasText(slotCode) || !StringUtils.hasText(internalToken)) {
            return Optional.empty();
        }
        try {
            CommercialOfferResponse offer = restClientBuilder.clone()
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/pde-validation-contract/v1/products/{slug}/commercial-offer")
                            .queryParam("slotCode", slotCode)
                            .build(productSlug))
                    .header("X-PDE-Internal-Token", internalToken)
                    .retrieve()
                    .body(CommercialOfferResponse.class);
            return Optional.ofNullable(offer);
        } catch (RuntimeException ex) {
            log.warn(
                    "Falha ao carregar oferta candidata pelo preflight autenticado: productSlug={}, slotCode={}, baseUrl={}",
                    productSlug,
                    slotCode,
                    baseUrl,
                    ex);
            return Optional.empty();
        }
    }

    /** Extrai apenas subdomínios versionados válidos, ignorando portas e hosts não versionados. */
    private String resolveSlotCode(String host) {
        if (!StringUtils.hasText(host)) {
            return "";
        }
        String normalizedHost = host.split(":", 2)[0].trim().toLowerCase();
        String candidate = normalizedHost.split("\\.", 2)[0];
        return candidate.matches("v\\d+") ? candidate : "";
    }

    /** Converte a configuração de URLs em lista limpa e ordenada de tentativas. */
    private List<String> parseBaseUrls(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
