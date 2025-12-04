package com.marketinghub.worker.imagegeneration;

import com.marketinghub.worker.util.UrlUtils;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ImageGenerationCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationCatalogClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiPrefix;

    public ImageGenerationCatalogClient(
            WebClient.Builder builder,
            @Value("${lead-portal.backend.base-url:${backend.base-url}}") String baseUrl,
            @Value("${lead-portal.backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.baseUrl = baseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<ImageGenerationModelDto> fetchCatalog() {
        String url = UrlUtils.joinPath(baseUrl, apiPrefix, "/image-generation/models");
        log.debug("Fetching image-generation catalog from {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.createException().map(RuntimeException::new))
                .bodyToFlux(ImageGenerationModelDto.class)
                .collectList()
                .block(REQUEST_TIMEOUT);
    }
}
