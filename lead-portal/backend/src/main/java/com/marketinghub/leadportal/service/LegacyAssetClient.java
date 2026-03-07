package com.marketinghub.leadportal.service;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class LegacyAssetClient {

    private static final Logger log = LoggerFactory.getLogger(LegacyAssetClient.class);

    private final RestTemplate restTemplate;

    public record DownloadedAsset(byte[] content, String contentType, String fileName) {}

    @Autowired
    public LegacyAssetClient(RestTemplateBuilder restTemplateBuilder) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build());
    }

    LegacyAssetClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DownloadedAsset> fetch(String url) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Legacy asset '{}' returned status {}", url, response.getStatusCode());
                return Optional.empty();
            }

            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : null;
            String filename = extractFileName(url, response);
            return Optional.of(new DownloadedAsset(response.getBody(), contentType, filename));
        } catch (RestClientException ex) {
            log.warn("Failed to download legacy asset '{}'", url, ex);
            return Optional.empty();
        }
    }

    private String extractFileName(String url, ResponseEntity<byte[]> response) {
        if (response.getHeaders().getContentDisposition() != null
                && StringUtils.hasText(response.getHeaders().getContentDisposition().getFilename())) {
            return response.getHeaders().getContentDisposition().getFilename();
        }
        String filename = StringUtils.getFilename(url);
        return filename != null ? filename : "asset";
    }
}
