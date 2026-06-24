package com.marketinghub.worker.targeting;

import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationFailureRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationResultRequest;
import com.marketinghub.worker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP responsável por acessar pendências e reportar resultados de targeting pelo backend.
 */
@Component
public class BackendTargetingElementClient {
    private static final Logger log = LoggerFactory.getLogger(BackendTargetingElementClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Inicializa o cliente com base URL e prefixo de API do backend principal. */
    public BackendTargetingElementClient(WebClient.Builder builder,
                                         @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl,
                                         @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Busca pendências de geração de públicos publicadas pelo backend. */
    public List<TargetingElementGenerationPendingDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/targeting-elements/generation/pending");
        String uri = url + "?limit=" + Math.max(limit, 1);
        log.info("Buscando pendências de targeting no backend em {}", uri);
        List<TargetingElementGenerationPendingDto> payload = webClient.get()
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
                    return response.bodyToFlux(TargetingElementGenerationPendingDto.class);
                })
                .collectList()
                .onErrorResume(err -> {
                    log.error("Falha ao buscar pendências de targeting no backend", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    /** Envia os públicos gerados ao backend para persistência canônica. */
    public void sendResults(Long nicheId, TargetingElementType type, List<CreateTargetingElementRequest> items) {
        String url = UrlUtils.joinPath(
                backendBaseUrl,
                apiPrefix,
                "/internal/targeting-elements/generation",
                String.valueOf(nicheId),
                type.name(),
                "results");
        TargetingElementGenerationResultRequest payload = new TargetingElementGenerationResultRequest(items != null ? items : List.of());
        post(url, payload, "resultado de targeting", nicheId, type);
    }

    /** Reporta falha de geração ao backend para liberar a pendência operacional. */
    public void sendFailure(Long nicheId, TargetingElementType type, String error) {
        String url = UrlUtils.joinPath(
                backendBaseUrl,
                apiPrefix,
                "/internal/targeting-elements/generation",
                String.valueOf(nicheId),
                type.name(),
                "failure");
        post(url, new TargetingElementGenerationFailureRequest(error), "falha de targeting", nicheId, type);
    }

    /** Executa POST no backend e registra falhas com contexto operacional. */
    private void post(String url, Object payload, String operation, Long nicheId, TargetingElementType type) {
        webClient.post()
                .uri(url)
                .bodyValue(payload)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        errorMessage("POST", url, status, body))));
                    }
                    return response.bodyToMono(Void.class);
                })
                .onErrorResume(err -> {
                    log.error("Falha ao enviar {} para nicho {} e tipo {}", operation, nicheId, type, err);
                    return Mono.empty();
                })
                .block();
    }

    /** Monta mensagem de erro HTTP com corpo de resposta quando disponível. */
    private static String errorMessage(String method, String url, HttpStatusCode status, String body) {
        if (body != null && !body.isBlank()) {
            return "%s %s falhou com status %s: %s".formatted(method, url, status, body);
        }
        return "%s %s falhou com status %s".formatted(method, url, status);
    }
}
