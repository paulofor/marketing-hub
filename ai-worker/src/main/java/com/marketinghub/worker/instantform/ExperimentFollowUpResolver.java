package com.marketinghub.worker.instantform;

import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Resolves experiment follow-up action URLs by querying the backend service.
 */
@Component
public class ExperimentFollowUpResolver {
    private static final Logger log = LoggerFactory.getLogger(ExperimentFollowUpResolver.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public ExperimentFollowUpResolver(WebClient.Builder builder,
                                      @Value("${backend.base-url:http://191.252.92.222:8000}") String backendBaseUrl,
                                      @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public Optional<String> resolveFollowUpActionUrl(Long experimentId) {
        if (experimentId == null) {
            return Optional.empty();
        }
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId);
        if (log.isDebugEnabled()) {
            log.debug("Fetching follow-up action URL for experiment {} using {}", experimentId, url);
        }
        try {
            return webClient.get()
                    .uri(url)
                    .exchangeToMono(response -> {
                        HttpStatusCode status = response.statusCode();
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            return Mono.empty();
                        }
                        if (status.isError()) {
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new IllegalStateException(
                                            String.format("GET %s returned %s: %s", url, status, body))));
                        }
                        return response.bodyToMono(ExperimentDto.class);
                    })
                    .flatMap(dto -> {
                        String candidate = dto.getFollowUpActionUrl();
                        if (!StringUtils.hasText(candidate)) {
                            return Mono.empty();
                        }
                        return Mono.just(candidate.trim());
                    })
                    .blockOptional();
        } catch (Exception ex) {
            log.warn("Failed to resolve follow-up action URL from backend for experiment {}", experimentId, ex);
            return Optional.empty();
        }
    }
}
