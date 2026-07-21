package com.marketinghub.experiment.monitoring.pde;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Consulta os backends PDE publicados para confirmar deploy real por ambiente. */
@Component
public class PdeDeployStatusHttpClient implements PdeDeployStatusClient {

    private static final Logger log = LoggerFactory.getLogger(PdeDeployStatusHttpClient.class);
    private final List<EnvironmentEndpoint> endpoints;

    /** Inicializa o cliente com endpoints públicos de homologação e produção. */
    public PdeDeployStatusHttpClient(
            @Value("${integrations.pde-platform.deploy-status.homolog-url:http://191.252.102.54:8097}") String homologUrl,
            @Value("${integrations.pde-platform.deploy-status.production-url:http://191.252.102.54:8096}") String productionUrl) {
        this.endpoints = List.of(
                new EnvironmentEndpoint("homolog", trimTrailingSlash(homologUrl)),
                new EnvironmentEndpoint("production", trimTrailingSlash(productionUrl)));
    }

    /** Consulta cada ambiente e retorna indisponível quando o backend não responde. */
    @Override
    public List<PdeDeployStatus> fetchStatuses() {
        List<PdeDeployStatus> statuses = new ArrayList<>();
        for (EnvironmentEndpoint endpoint : endpoints) {
            if (!StringUtils.hasText(endpoint.baseUrl())) {
                continue;
            }
            statuses.add(fetchStatus(endpoint));
        }
        return statuses;
    }

    /** Consulta um ambiente específico com timeout curto para proteger o painel. */
    private PdeDeployStatus fetchStatus(EnvironmentEndpoint endpoint) {
        try {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
            requestFactory.setReadTimeout(Duration.ofSeconds(3));
            RestClient restClient = RestClient.builder()
                    .baseUrl(endpoint.baseUrl())
                    .requestFactory(requestFactory)
                    .build();
            PdeDeployStatus status = restClient.get()
                    .uri("/api/pde/deploy/status")
                    .retrieve()
                    .body(PdeDeployStatus.class);
            return normalize(endpoint, status);
        } catch (Exception ex) {
            log.error("Falha ao consultar status de deploy PDE; environment={}, baseUrl={}",
                    endpoint.environment(), endpoint.baseUrl(), ex);
            return unavailable(endpoint, ex.getMessage());
        }
    }

    /** Garante campos de disponibilidade mesmo quando o backend retorna manifesto antigo. */
    private PdeDeployStatus normalize(EnvironmentEndpoint endpoint, PdeDeployStatus status) {
        if (status == null) {
            return unavailable(endpoint, "Resposta vazia do backend PDE");
        }
        boolean frontendReachable = isReachable(status.frontendUrl());
        return new PdeDeployStatus(
                StringUtils.hasText(status.environment()) ? status.environment() : endpoint.environment(),
                true,
                "AVAILABLE",
                null,
                status.composeFile(),
                status.commitSha(),
                status.imageTag(),
                status.experienceVersion(),
                status.frontendUrl(),
                status.backendUrl(),
                frontendReachable,
                true,
                status.deployedAt(),
                status.services() != null ? status.services() : List.of());
    }

    /** Verifica se a URL pública do frontend responde sem bloquear a tela administrativa. */
    private boolean isReachable(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
            requestFactory.setReadTimeout(Duration.ofSeconds(3));
            RestClient.builder()
                    .baseUrl(trimTrailingSlash(url))
                    .requestFactory(requestFactory)
                    .build()
                    .get()
                    .uri("/")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("Frontend PDE não respondeu ao health público; url={}", url, ex);
            return false;
        }
    }

    /** Retorna status indisponível para ambientes sem resposta real. */
    private PdeDeployStatus unavailable(EnvironmentEndpoint endpoint, String errorMessage) {
        return new PdeDeployStatus(
                endpoint.environment(),
                false,
                "UNAVAILABLE",
                errorMessage,
                null,
                null,
                null,
                null,
                null,
                endpoint.baseUrl(),
                false,
                false,
                null,
                List.of());
    }

    /** Remove barra final para compor caminhos HTTP previsíveis. */
    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    /** Guarda a URL base configurada para um ambiente PDE. */
    private record EnvironmentEndpoint(String environment, String baseUrl) {}
}
