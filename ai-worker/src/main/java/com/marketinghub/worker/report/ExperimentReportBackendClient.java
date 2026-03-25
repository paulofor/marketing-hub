package com.marketinghub.worker.report;

import com.marketinghub.experiment.report.ExperimentReportStatus;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP responsável por interagir com os endpoints do backend relacionados aos relatórios de experimento.
 */
@Component
public class ExperimentReportBackendClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentReportBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public ExperimentReportBackendClient(WebClient.Builder builder,
                                         @Value("${backend.base-url:http://localhost:8080}") String backendBaseUrl,
                                         @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /**
     * Busca todas as solicitações com status PENDING ordenadas por data de solicitação.
     */
    public List<ExperimentReportRequestDetailDto> fetchPendingRequests() {
        String base = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiment-report-requests");
        String uri = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("status", ExperimentReportStatus.PENDING.name())
                .toUriString();
        logBackendRequest("GET", uri);
        List<ExperimentReportRequestDetailDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> mapToFlux(response, uri))
                .collectList()
                .block();
        return payload == null ? List.of() : payload;
    }

    /**
     * Atualiza o status de uma solicitação específica.
     */
    public ExperimentReportRequestDetailDto updateStatus(Long requestId,
                                                         ExperimentReportStatus status,
                                                         String downloadUrl,
                                                         String failureReason) {
        Objects.requireNonNull(requestId, "requestId é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");

        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiment-report-requests/" + requestId);
        logBackendRequest("PATCH", url);
        HashMap<String, Object> body = new HashMap<>();
        body.put("status", status.name());
        if (downloadUrl != null) {
            body.put("downloadUrl", downloadUrl);
        }
        if (failureReason != null) {
            body.put("failureReason", failureReason);
        }
        return webClient.patch()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> mapToMono(response, url))
                .block();
    }

    private Flux<ExperimentReportRequestDetailDto> mapToFlux(ClientResponse response, String url) {
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new BackendClientException(errorMessage("GET", url, status, body))));
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return Flux.empty();
        }
        return response.bodyToFlux(ExperimentReportRequestDetailDto.class);
    }

    private Mono<ExperimentReportRequestDetailDto> mapToMono(ClientResponse response, String url) {
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new BackendClientException(errorMessage("PATCH", url, status, body))));
        }
        return response.bodyToMono(ExperimentReportRequestDetailDto.class);
    }

    private String errorMessage(String method, String url, HttpStatusCode status, String body) {
        String reason = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : status.toString();
        return String.format(
                "Backend %s %s respondeu %d (%s). Body: %s",
                method,
                url,
                status.value(),
                reason,
                body);
    }

    private void logBackendRequest(String method, String url) {
        log.debug("Calling backend {} {}", method, url);
    }

    public static class BackendClientException extends RuntimeException {
        public BackendClientException(String message) {
            super(message);
        }
    }
}
