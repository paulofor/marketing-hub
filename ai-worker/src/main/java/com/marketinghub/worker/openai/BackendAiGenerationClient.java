package com.marketinghub.worker.openai;

import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.worker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP responsável por registrar auditoria de IA pelo backend principal.
 */
@Component
public class BackendAiGenerationClient {
    private static final Logger log = LoggerFactory.getLogger(BackendAiGenerationClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Inicializa o cliente com a base URL canônica do backend. */
    public BackendAiGenerationClient(WebClient.Builder builder,
                                     @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl,
                                     @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Envia o registro de auditoria de IA para persistência exclusiva no backend. */
    public void record(AiWorkerGenerationRequest request) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/ai/generations/internal");
        webClient.post()
                .uri(url)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(errorMessage(url, status, body))));
                    }
                    return response.bodyToMono(Void.class);
                })
                .onErrorResume(ex -> {
                    log.error("Falha ao registrar auditoria de IA no backend para domínio {} e referência {}",
                            request != null ? request.getDomain() : null,
                            request != null ? request.getReferenceId() : null,
                            ex);
                    return Mono.error(ex);
                })
                .block();
    }

    /** Monta mensagem de erro HTTP com o corpo da resposta quando disponível. */
    private static String errorMessage(String url, HttpStatusCode status, String body) {
        if (body != null && !body.isBlank()) {
            return "POST %s falhou com status %s: %s".formatted(url, status, body);
        }
        return "POST %s falhou com status %s".formatted(url, status);
    }
}
