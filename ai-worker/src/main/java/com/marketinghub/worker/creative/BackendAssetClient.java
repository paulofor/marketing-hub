package com.marketinghub.worker.creative;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.util.UrlUtils;
import java.util.List;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final String assetPath;
    private final String explicitAssetUrl;

    public BackendAssetClient(WebClient.Builder builder,
                              @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                              @Value("${backend.api-prefix:/api}") String apiPrefix,
                              @Value("${backend.asset-path:/assets}") String assetPath,
                              @Value("${backend.asset-url:}") String assetUrl) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.assetPath = normalizeAssetPath(assetPath);
        this.explicitAssetUrl = assetUrl;
    }

    /**
     * Sends the provided image bytes to {@code POST /api/assets} in the backend and returns the
     * relative URL exposed by the service.
     */
    public String uploadImage(byte[] content, String filename, String model, String prompt) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Image content must not be empty");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }
        String effectiveName = StringUtils.hasText(filename) ? filename : "creative.png";
        BackendAssetUploadException lastException = null;
        for (String targetUrl : resolveUploadUrls()) {
            try {
                return performUpload(content, effectiveName, targetUrl, model, prompt);
            } catch (BackendAssetUploadException ex) {
                if (!shouldFallback(ex)) {
                    throw ex;
                }
                lastException = ex;
                log.warn("Asset upload to {} failed with status {}. Trying fallback endpoint...",
                        targetUrl, ex.getStatusCode());
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new BackendAssetUploadException("Backend did not return an asset URL");
    }

    private void logBackendRequest(String method, String url) {
        if (log.isInfoEnabled()) {
            log.info("Calling backend {} {}", method, url);
        }
    }

    private List<String> resolveUploadUrls() {
        if (StringUtils.hasText(explicitAssetUrl)) {
            return List.of(explicitAssetUrl);
        }
        String primary = UrlUtils.joinPath(backendBaseUrl, apiPrefix, assetPath);
        if (!StringUtils.hasText(apiPrefix)) {
            return List.of(primary);
        }
        String fallback = UrlUtils.joinPath(backendBaseUrl, "", assetPath);
        if (primary.equals(fallback)) {
            return List.of(primary);
        }
        return List.of(primary, fallback);
    }

    private String performUpload(byte[] content, String filename, String url, String model, String prompt) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, partHeaders);
        body.add("file", filePart);
        body.add("prompt", prompt);
        if (StringUtils.hasText(model)) {
            body.add("model", model);
        }

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
                                        status, url, bodyContent)));
                    }
                    return response.bodyToMono(String.class)
                            .map(BackendAssetClient::extractAssetUrl)
                            .filter(StringUtils::hasText)
                            .switchIfEmpty(Mono.error(new BackendAssetUploadException(
                                    "Backend did not return an asset URL")));
                })
                .blockOptional()
                .orElseThrow(() -> new BackendAssetUploadException(
                        "Backend did not return an asset URL"));
    }

    static String extractAssetUrl(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isTextual()) {
                return node.asText();
            }
            JsonNode urlNode = node.path("url");
            if (urlNode.isTextual() && StringUtils.hasText(urlNode.asText())) {
                return urlNode.asText();
            }
            JsonNode imageUrlNode = node.path("imageUrl");
            if (imageUrlNode.isTextual() && StringUtils.hasText(imageUrlNode.asText())) {
                return imageUrlNode.asText();
            }
            return null;
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private boolean shouldFallback(BackendAssetUploadException exception) {
        if (StringUtils.hasText(explicitAssetUrl)) {
            return false;
        }
        Integer statusCode = exception.getStatusCode();
        return statusCode != null && statusCode == 404;
    }

    private static String normalizeAssetPath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/assets";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String errorMessage(HttpStatusCode status, String url, String body) {
        return "Backend asset upload failed: status=" + status.value() + " url=" + url
                + (body.isBlank() ? "" : " body=" + body);
    }

    /** Exception thrown when the backend refuses the upload. */
    public static class BackendAssetUploadException extends RuntimeException {
        private final Integer statusCode;
        private final String url;
        private final String responseBody;

        public BackendAssetUploadException(String message) {
            super(message);
            this.statusCode = null;
            this.url = null;
            this.responseBody = null;
        }

        public BackendAssetUploadException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = null;
            this.url = null;
            this.responseBody = null;
        }

        public BackendAssetUploadException(HttpStatusCode status, String url, String body) {
            super(errorMessage(status, url, body));
            this.statusCode = status != null ? status.value() : null;
            this.url = url;
            this.responseBody = body;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getUrl() {
            return url;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
