package com.marketinghub.videomanagement.client;

import com.marketinghub.videomanagement.client.dto.AssetResponse;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client HTTP para upload de assets no backend.
 */
@Component
public class VideoAssetClient {
    private static final String DEFAULT_PROVIDER = "VIDEO_MODULE";

    private final Logger log = LoggerFactory.getLogger(VideoAssetClient.class);
    private final WebClient webClient;
    private final VideoManagementProperties properties;

    public VideoAssetClient(WebClient.Builder builder,
                            VideoManagementProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getBackendBaseUrl().toString())
                .build();
        this.properties = properties;
    }

    public AssetResponse uploadAsset(ProviderFile file, String metadataJson) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", asResource(file))
                .filename(file.fileName())
                .contentType(file.mediaType());
        bodyBuilder.part("assetType", file.assetType().name());
        bodyBuilder.part("provider", DEFAULT_PROVIDER);
        if (StringUtils.hasText(metadataJson)) {
            bodyBuilder.part("metadata", metadataJson);
        }
        try {
            return authorized(webClient.post()
                            .uri("/internal/video/assets")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(bodyBuilder.build())))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new BackendIntegrationException(
                                            "Erro ao enviar asset: " + body)))
                    .bodyToMono(AssetResponse.class)
                    .block();
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao enviar asset para o backend", ex);
            throw new BackendIntegrationException("Falha ao enviar asset", ex);
        }
    }

    private ByteArrayResource asResource(ProviderFile file) {
        return new ByteArrayResource(file.content()) {
            @Override
            public String getFilename() {
                return file.fileName();
            }
        };
    }

    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> spec) {
        if (StringUtils.hasText(properties.getAuthToken())) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthToken());
        }
        return spec;
    }
}
