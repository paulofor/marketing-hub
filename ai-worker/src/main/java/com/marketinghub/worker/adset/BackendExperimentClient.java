package com.marketinghub.worker.adset;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.dto.AudienceDto;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.AdSetDto;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * HTTP client responsible for retrieving experiment context and persisting ad sets in the backend.
 */
@Component
public class BackendExperimentClient {
    private static final Logger log = LoggerFactory.getLogger(BackendExperimentClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public BackendExperimentClient(WebClient.Builder builder,
                                   @Value("${backend.base-url:http://191.252.92.222:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /**
     * Retrieves experiments ready for ad set generation along with their audiences.
     */
    public List<ReadyExperiment> listExperimentsReadyForAdSets() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/facebook-adsets/experiments-ready");
        logBackendRequest("GET", url);
        List<ExperimentReadyForAdSetDto> payload = webClient.get()
                .uri(url)
                .exchangeToFlux(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMapMany(body -> Mono.error(new BackendClientException(
                                        errorMessage("GET", url, status, body))));
                    }
                    return response.bodyToFlux(ExperimentReadyForAdSetDto.class);
                })
                .collectList()
                .block();
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        return payload.stream()
                .map(this::toReadyExperiment)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Checks if ad sets were already created for the experiment.
     */
    public boolean hasAdSets(long experimentId) {
        String base = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/adsets");
        String uri = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("experimentId", experimentId)
                .toUriString();
        logBackendRequest("GET", uri);
        Boolean result = webClient.get()
                .uri(uri)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == HttpStatus.NOT_FOUND.value()) {
                        return Mono.just(false);
                    }
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new BackendClientException(
                                        errorMessage("GET", uri, status, body))));
                    }
                    return response.bodyToFlux(AdSetDto.class).hasElements();
                })
                .block();
        return Boolean.TRUE.equals(result);
    }

    /**
     * Persists a new ad set record in the backend service.
     */
    public AdSetDto createAdSet(CreateAdSetRequest request) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/adsets");
        logBackendRequest("POST", url);
        return webClient.post()
                .uri(url)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new BackendClientException(
                                        errorMessage("POST", url, status, body))));
                    }
                    return response.bodyToMono(AdSetDto.class);
                })
                .block();
    }

    private void logBackendRequest(String method, String url) {
        if (log.isInfoEnabled()) {
            log.info("Calling backend {} {}", method, url);
        }
    }

    private ReadyExperiment toReadyExperiment(ExperimentReadyForAdSetDto dto) {
        if (dto.getExperiment() == null) {
            log.warn("Backend returned experiment-ready payload without experiment data");
            return null;
        }
        MarketNiche niche = dto.getNiche() != null ? toMarketNiche(dto.getNiche()) : null;
        Hypothesis hypothesis = dto.getHypothesis() != null ? toHypothesis(dto.getHypothesis()) : null;
        Experiment experiment = toExperiment(dto.getExperiment(), niche, hypothesis);
        List<Audience> audiences = filterAudiences(dto.getAudiences(), niche, hypothesis);
        return new ReadyExperiment(experiment, audiences);
    }

    private static Experiment toExperiment(com.marketinghub.experiment.dto.ExperimentDto dto,
                                           MarketNiche niche,
                                           Hypothesis hypothesis) {
        Experiment experiment = new Experiment();
        experiment.setId(dto.getId());
        experiment.setName(dto.getName());
        experiment.setHypothesis(dto.getHypothesis());
        experiment.setPlatform(dto.getPlatform() != null ? dto.getPlatform() : ExperimentPlatform.FACEBOOK);
        experiment.setStatus(dto.getStatus() != null ? dto.getStatus() : ExperimentStatus.PLANNED);
        experiment.setCreativeApproved(dto.isCreativeApproved());
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        return experiment;
    }

    private static MarketNiche toMarketNiche(MarketNicheDto dto) {
        MarketNiche niche = new MarketNiche();
        niche.setId(dto.getId());
        niche.setName(dto.getName());
        niche.setBaseSegmentation(dto.getBaseSegmentation());
        niche.setInterests(dto.getInterests());
        niche.setDemographicFilters(dto.getDemographicFilters());
        niche.setExtraTips(dto.getExtraTips());
        return niche;
    }

    private static Hypothesis toHypothesis(HypothesisDto dto) {
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(dto.getId());
        hypothesis.setTitle(dto.getTitle());
        hypothesis.setPromise(dto.getPromise());
        hypothesis.setPersona(dto.getPersona());
        hypothesis.setMechanism(dto.getMechanism());
        hypothesis.setUniqueMechanism(dto.getUniqueMechanism());
        return hypothesis;
    }

    private static List<Audience> filterAudiences(List<AudienceDto> dtos,
                                                  MarketNiche niche,
                                                  Hypothesis hypothesis) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        UUID hypothesisId = hypothesis != null ? hypothesis.getId() : null;
        List<Audience> result = new ArrayList<>();
        for (AudienceDto dto : dtos) {
            if (!dto.isApproved()) {
                continue;
            }
            UUID audienceHypothesisId = dto.getHypothesisId();
            if (audienceHypothesisId != null && (hypothesisId == null || !audienceHypothesisId.equals(hypothesisId))) {
                continue;
            }
            Audience audience = new Audience();
            audience.setId(dto.getId());
            audience.setName(dto.getName());
            audience.setDescription(dto.getDescription());
            audience.setPrompt(dto.getPrompt());
            audience.setModel(dto.getModel());
            audience.setApproved(dto.isApproved());
            audience.setNiche(niche);
            if (audienceHypothesisId != null && hypothesis != null && audienceHypothesisId.equals(hypothesisId)) {
                audience.setHypothesis(hypothesis);
            }
            result.add(audience);
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private static String errorMessage(String method, String url, HttpStatusCode status, String body) {
        if (body != null && !body.isBlank()) {
            return "%s %s failed with status %s: %s".formatted(method, url, status, body);
        }
        return "%s %s failed with status %s".formatted(method, url, status);
    }

    /**
     * Exception thrown when the backend rejects a request.
     */
    public static class BackendClientException extends RuntimeException {
        public BackendClientException(String message) {
            super(message);
        }
    }

    /**
     * Aggregated experiment data used by the audience ad set service.
     */
    public record ReadyExperiment(Experiment experiment, List<Audience> audiences) {
    }
}

