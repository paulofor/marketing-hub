package com.marketinghub.worker.creative;

import com.marketinghub.worker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client responsible for uploading generated assets to the backend service.
 */
@Component
public class BackendAssetClient {
    private static final Logger log = LoggerFactory.getLogger(BackendAssetClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public BackendAssetClient(WebClient.Builder builder,
                              @Value("${backend.base-url:http://localhost:8080}") String backendBaseUrl,
                              @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /**
     * Sends the provided image bytes to {@code POST /api/assets} in the backend and returns the
     * relative URL exposed by the service.
     */
    public String uploadImage(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Image content must not be empty");
        }
        String effectiveName = StringUtils.hasText(filename) ? filename : "creative.png";
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/assets");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return effectiveName;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, partHeaders);
        body.add("file", filePart);

        logBackendRequest("POST", url);
        return webClient.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(bodyContent -> Mono.error(new BackendAssetUploadException(
                                        errorMessage(status, url, bodyContent))));
                    }
                    return response.bodyToMono(String.class);
                })
                .blockOptional()
                .orElseThrow(() -> new BackendAssetUploadException(
                        "Backend did not return an asset URL"));
    }

    private void logBackendRequest(String method, String url) {
        if (log.isInfoEnabled()) {
            log.info("Calling backend {} {}", method, url);
        }
    }

    private static String errorMessage(HttpStatusCode status, String url, String body) {
        return "Backend asset upload failed: status=" + status.value() + " url=" + url
                + (body.isBlank() ? "" : " body=" + body);
    }

    /** Exception thrown when the backend refuses the upload. */
    public static class BackendAssetUploadException extends RuntimeException {
        public BackendAssetUploadException(String message) {
            super(message);
        }

        public BackendAssetUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

