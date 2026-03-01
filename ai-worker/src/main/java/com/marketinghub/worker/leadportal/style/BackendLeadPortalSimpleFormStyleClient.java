package com.marketinghub.worker.leadportal.style;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.worker.util.UrlUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BackendLeadPortalSimpleFormStyleClient {
    private static final Logger log = LoggerFactory.getLogger(BackendLeadPortalSimpleFormStyleClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public BackendLeadPortalSimpleFormStyleClient(WebClient.Builder builder,
                                                  @Value("${backend.base-url:http://191.252.92.222:8000}") String backendBaseUrl,
                                                  @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<PendingStyleDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/lead-portal/simple-form-styles/pending");
        String uri = url + "?limit=" + Math.max(limit, 1);
        log.info("Buscando estilos pendentes para geração em {}", uri);
        List<PendingStyleDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMapMany(body -> Mono.error(new IllegalStateException(
                                        errorMessage("GET", uri, status, body))));
                    }
                    return response.bodyToFlux(PendingStyleDto.class);
                })
                .collectList()
                .onErrorResume(err -> {
                    log.error("Falha ao buscar estilos pendentes", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    public void saveGeneration(Long styleId, GenerationResultPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/lead-portal/simple-form-styles/", styleId.toString(), "/generation");
        webClient.patch()
                .uri(url)
                .bodyValue(payload)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        errorMessage("PATCH", url, status, body))));
                    }
                    return response.bodyToMono(Void.class);
                })
                .onErrorResume(err -> {
                    log.error("Falha ao enviar geração do estilo {}", styleId, err);
                    return Mono.empty();
                })
                .block();
    }

    private static String errorMessage(String method, String url, HttpStatusCode status, String body) {
        if (body != null && !body.isBlank()) {
            return "%s %s failed with status %s: %s".formatted(method, url, status, body);
        }
        return "%s %s failed with status %s".formatted(method, url, status);
    }

    public record PendingStyleDto(Long id,
                                  String name,
                                  String slug,
                                  String description,
                                  String textModel,
                                  String textPrompt,
                                  String generationStatus) {
    }

    public record GenerationResultPayload(String status,
                                          String generationError,
                                          String textParameters,
                                          BigDecimal generationCostUsd,
                                          LeadPortalSimpleFormStyleDefinition definition) {
    }
}
