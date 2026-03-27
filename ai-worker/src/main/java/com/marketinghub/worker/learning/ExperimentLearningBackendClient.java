package com.marketinghub.worker.learning;

import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDetailDto;
import com.marketinghub.experiment.learning.dto.UpdateExperimentLearningRequest;
import com.marketinghub.worker.learning.exception.BackendClientException;
import com.marketinghub.worker.util.UrlUtils;
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
 * Cliente HTTP para interagir com o backend em relação às solicitações de aprendizado.
 */
@Component
public class ExperimentLearningBackendClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentLearningBackendClient.class);

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiPrefix;

    public ExperimentLearningBackendClient(WebClient.Builder builder,
                                           @Value("${backend.base-url:http://localhost:8080}") String baseUrl,
                                           @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.baseUrl = baseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<ExperimentLearningRequestDetailDto> fetchPendingRequests() {
        String url = UriComponentsBuilder.fromHttpUrl(UrlUtils.joinPath(baseUrl, apiPrefix, "/experiment-learning-requests"))
                .queryParam("status", ExperimentLearningStatus.PENDING.name())
                .toUriString();
        log.debug("Buscando solicitações de aprendizado pendentes em {}", url);
        List<ExperimentLearningRequestDetailDto> payload = webClient.get()
                .uri(url)
                .exchangeToFlux(response -> mapToFlux(response, url))
                .collectList()
                .block();
        return payload == null ? List.of() : payload;
    }

    public ExperimentLearningRequestDetailDto updateStatus(Long requestId,
                                                           ExperimentLearningStatus status,
                                                           ExperimentLearningPayloadDto payload,
                                                           String failureReason) {
        Objects.requireNonNull(requestId, "requestId é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
        String url = UrlUtils.joinPath(baseUrl, apiPrefix, "/experiment-learning-requests/" + requestId);
        log.debug("Atualizando solicitação {} para status {}", requestId, status);
        UpdateExperimentLearningRequest body = new UpdateExperimentLearningRequest(status, payload, failureReason);
        return webClient.patch()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> mapToMono(response, url))
                .block();
    }

    private Flux<ExperimentLearningRequestDetailDto> mapToFlux(ClientResponse response, String url) {
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new BackendClientException(errorMessage("GET", url, status, body))));
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return Flux.empty();
        }
        return response.bodyToFlux(ExperimentLearningRequestDetailDto.class);
    }

    private Mono<ExperimentLearningRequestDetailDto> mapToMono(ClientResponse response, String url) {
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new BackendClientException(errorMessage("PATCH", url, status, body))));
        }
        return response.bodyToMono(ExperimentLearningRequestDetailDto.class);
    }

    private String errorMessage(String method, String url, HttpStatusCode status, String body) {
        String reason = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : status.toString();
        return String.format("Backend %s %s respondeu %d (%s). Body: %s", method, url, status.value(), reason, body);
    }
}
