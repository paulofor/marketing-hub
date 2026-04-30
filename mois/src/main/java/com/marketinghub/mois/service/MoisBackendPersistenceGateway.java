package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MoisBackendPersistenceGateway {

    private static final Logger log = LoggerFactory.getLogger(MoisBackendPersistenceGateway.class);

    private final MoisBackendPersistenceProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public MoisBackendPersistenceGateway(
            MoisBackendPersistenceProperties properties,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(properties.getBaseUrl());
    }

    public void upsertCollectionJobState(String jobId, MoisCollectionPersistenceDtos.CollectionJobStateResponse payload) {
        if (!isEnabled()) {
            return;
        }
        String path = "/api/v1/mois/persistence/collection-jobs/" + jobId;
        log.info("MOIS backend persistence request (method={}, path={}, payload={})",
                HttpMethod.PUT,
                path,
                payload);
        exchange(path, HttpMethod.PUT, payload, MoisCollectionPersistenceDtos.CollectionJobStateResponse.class);
    }

    public Optional<MoisCollectionPersistenceDtos.CollectionJobStateResponse> getCollectionJobState(String jobId) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(exchange("/api/v1/mois/persistence/collection-jobs/" + jobId,
                    HttpMethod.GET,
                    null,
                    MoisCollectionPersistenceDtos.CollectionJobStateResponse.class).getBody());
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }

    public MoisCollectionPersistenceDtos.CollectionJobStateListResponse listCollectionJobStates(String workspaceId, String status) {
        if (!isEnabled()) {
            return new MoisCollectionPersistenceDtos.CollectionJobStateListResponse(java.util.List.of());
        }
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/mois/persistence/collection-jobs");
        if (StringUtils.hasText(workspaceId)) {
            uri.queryParam("workspaceId", workspaceId);
        }
        if (StringUtils.hasText(status)) {
            uri.queryParam("status", status);
        }
        return exchange(uri.toUriString(), HttpMethod.GET, null, MoisCollectionPersistenceDtos.CollectionJobStateListResponse.class)
                .getBody();
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        try {
            return buildRestTemplate().exchange(path, method, new HttpEntity<>(body), responseType);
        } catch (RestClientException ex) {
            throw ex;
        }
    }

    private RestTemplate buildRestTemplate() {
        return restTemplateBuilder
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }
}
