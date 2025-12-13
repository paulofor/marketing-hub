package com.marketinghub.emailservice.service.client;

import com.marketinghub.emailservice.config.CloudflareClientProperties;
import com.marketinghub.emailservice.dto.EmailAttachmentRequest;
import com.marketinghub.emailservice.exception.RemoteServiceException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CloudflareImageClient {

    private static final Logger log = LoggerFactory.getLogger(CloudflareImageClient.class);

    private final RestClient cloudflareRestClient;
    private final CloudflareClientProperties properties;

    public CloudflareImageClient(@Qualifier("cloudflareRestClient") RestClient cloudflareRestClient,
                                 CloudflareClientProperties properties) {
        this.cloudflareRestClient = cloudflareRestClient;
        this.properties = properties;
    }

    public RemoteAsset fetchAsset(EmailAttachmentRequest request) {
        String resourceUri = resolveResourceUri(request);
        try {
            ResponseEntity<byte[]> response = cloudflareRestClient
                    .get()
                    .uri(URI.create(resourceUri))
                    .retrieve()
                    .toEntity(byte[].class);

            MediaType mediaType = response.getHeaders().getContentType();
            if (mediaType == null && StringUtils.hasText(request.contentType())) {
                mediaType = MediaType.parseMediaType(request.contentType());
            }

            return new RemoteAsset(request.fileName(), mediaType, response.getBody());
        } catch (RestClientException ex) {
            log.error("Erro ao fazer download da mídia {} no Cloudflare", request.id(), ex);
            throw new RemoteServiceException("Não foi possível baixar o ativo no Cloudflare", ex);
        }
    }

    private String resolveResourceUri(EmailAttachmentRequest request) {
        if (StringUtils.hasText(request.resourceUrl())) {
            return request.resourceUrl();
        }

        if (StringUtils.hasText(properties.deliveryBaseUrl()) && StringUtils.hasText(properties.deliveryHash())) {
            String base = trimTrailingSlash(properties.deliveryBaseUrl());
            String variant = StringUtils.hasText(request.variant()) ? request.variant() : defaultVariant();
            return base + "/" + properties.deliveryHash() + "/" + request.id() + "/" + variant;
        }

        if (StringUtils.hasText(properties.baseUrl())) {
            String base = trimTrailingSlash(properties.baseUrl());
            return base + "/images/" + request.id();
        }

        throw new IllegalStateException("Configuração do Cloudflare incompleta. Informe resourceUrl ou variáveis de ambiente");
    }

    private String defaultVariant() {
        return StringUtils.hasText(properties.defaultVariant()) ? properties.defaultVariant() : "public";
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
