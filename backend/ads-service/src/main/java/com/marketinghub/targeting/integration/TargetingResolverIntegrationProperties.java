package com.marketinghub.targeting.integration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configurações para acionar o Facebook Ads Worker e resolver candidatos com a Graph API.
 */
@Component
@ConfigurationProperties(prefix = "targeting.resolution")
@Getter
@Setter
@ToString
public class TargetingResolverIntegrationProperties {
    /**
     * Controla se o backend deve enviar candidatos automaticamente para o worker.
     */
    private boolean enabled = true;

    /**
     * Base URL do Facebook Ads Worker (ex.: http://facebook-ads-worker:8080).
     */
    private String baseUrl = "http://facebook-ads-worker:8080";

    /**
     * Prefixo do endpoint interno exposto pelo worker.
     */
    private String apiPrefix = "/internal/targeting";

    /**
     * Limite máximo de itens enviados para o targetingsearch.
     */
    private Integer searchLimit = 25;

    /**
     * ID da conta de anúncios usado como fallback quando o request não informa.
     */
    private String adAccountId;

    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(30);

    /** Tempo de espera antes do primeiro ciclo de reprocessamento automático. */
    private Duration retryInitialDelay = Duration.ofMinutes(1);

    /** Intervalo entre as tentativas de reprocessar candidatos pendentes. */
    private Duration retryInterval = Duration.ofMinutes(5);

    /** Quantidade máxima de solicitações reprocessadas por ciclo. */
    private int retryLimit = 25;
}
