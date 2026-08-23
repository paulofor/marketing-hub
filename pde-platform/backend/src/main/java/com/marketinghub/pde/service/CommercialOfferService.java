package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.CommercialOfferResponse;
import java.util.Arrays;
import java.util.List;
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

    /** Inicializa a integração com as bases oficiais e seus fallbacks operacionais. */
    public CommercialOfferService(
            RestClient.Builder restClientBuilder,
            @Value("${pde.catalog.marketing-hub-base-url:}") String marketingHubBaseUrl) {
        this.restClientBuilder = restClientBuilder;
        this.marketingHubBaseUrls = parseBaseUrls(marketingHubBaseUrl);
    }

    /** Obtém uma oferta completa ou falha fechado para não renderizar venda sem contrato. */
    public CommercialOfferResponse getOffer(String productSlug) {
        for (String baseUrl : marketingHubBaseUrls) {
            try {
                CommercialOfferResponse offer = restClientBuilder.clone()
                        .baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri("/api/products/public/{slug}/commercial-offer", productSlug)
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
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Oferta comercial indisponível no Marketing Hub.");
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
