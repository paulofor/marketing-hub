package com.marketinghub.worker.experimentpromise;

import com.marketinghub.worker.util.UrlUtils;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Responsável por consumir e atualizar solicitações de promessa no backend principal. */
@Component
public class ExperimentPromiseBackendClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Inicializa o cliente HTTP apontando para o backend principal. */
    public ExperimentPromiseBackendClient(WebClient.Builder builder,
                                          @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                          @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Lista solicitações pendentes para processamento pelo worker. */
    public List<ExperimentPromiseOptionsResponse> listPending(int limit) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/experiments/promise-contract-options/stage-executions/pending") + "?limit=" + Math.max(1, limit);
        return webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> response.statusCode().isError()
                        ? errorFlux("GET", uri, response.statusCode(), response)
                        : response.bodyToFlux(ExperimentPromiseOptionsResponse.class))
                .collectList()
                .onErrorResume(ex -> {
                    log.error("Falha ao buscar promessas pendentes; operation=experiment-promise-pending endpoint={}", uri, ex);
                    return Mono.just(Collections.emptyList());
                })
                .blockOptional()
                .orElse(List.of());
    }

    /** Marca uma solicitação como assumida pelo worker antes de chamar a OpenAI. */
    public ExperimentPromiseOptionsResponse claim(Long requestId, String workerId) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/experiments/promise-contract-options/stage-executions/", requestId.toString(), "/claim")
                + "?workerId=" + workerId;
        return post(uri, null, ExperimentPromiseOptionsResponse.class);
    }

    /** Envia as três opções geradas para concluir a solicitação no backend. */
    public void complete(Long requestId, ExperimentPromiseOptionsResponse response) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/experiments/promise-contract-options/stage-executions/", requestId.toString(), "/complete");
        post(uri, response, ExperimentPromiseOptionsResponse.class);
    }

    /** Registra falha funcional para a tela sair do estado de espera. */
    public void fail(Long requestId, String errorMessage) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/experiments/promise-contract-options/stage-executions/", requestId.toString(), "/fail");
        post(uri, errorMessage != null ? errorMessage : "Falha desconhecida no AI Worker", Void.class);
    }

    /** Executa POST validando erro HTTP com contexto operacional. */
    private <T> T post(String uri, Object body, Class<T> responseType) {
        WebClient.RequestBodySpec request = webClient.post().uri(uri);
        WebClient.RequestHeadersSpec<?> spec = body != null ? request.bodyValue(body) : request;
        return spec.exchangeToMono(response -> response.statusCode().isError()
                        ? errorMono("POST", uri, response.statusCode(), response)
                        : response.bodyToMono(responseType))
                .block();
    }

    /** Cria erro reativo para respostas HTTP inválidas em consultas de lista. */
    private static <T> Flux<T> errorFlux(String method, String uri, HttpStatusCode status,
                                         org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMapMany(body -> Flux.error(new IllegalStateException(errorMessage(method, uri, status, body))));
    }

    /** Cria erro reativo para respostas HTTP inválidas em comandos. */
    private static <T> Mono<T> errorMono(String method, String uri, HttpStatusCode status,
                                        org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new IllegalStateException(errorMessage(method, uri, status, body))));
    }

    /** Monta mensagem de erro com método, endpoint e corpo retornado. */
    private static String errorMessage(String method, String uri, HttpStatusCode status, String body) {
        return "%s %s falhou com status %s%s".formatted(method, uri, status,
                body != null && !body.isBlank() ? ": " + body : "");
    }
}
