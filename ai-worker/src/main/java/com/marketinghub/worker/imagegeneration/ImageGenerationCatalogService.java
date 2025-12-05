package com.marketinghub.worker.imagegeneration;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service("workerImageGenerationCatalogService")
public class ImageGenerationCatalogService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ImageGenerationCatalogClient client;
    private volatile CatalogSnapshot snapshot;

    public ImageGenerationCatalogService(ImageGenerationCatalogClient client) {
        this.client = client;
    }

    public List<ImageGenerationModelDto> getCatalog() {
        CatalogSnapshot current = snapshot;
        if (current == null || current.isExpired()) {
            synchronized (this) {
                current = snapshot;
                if (current == null || current.isExpired()) {
                    List<ImageGenerationModelDto> models = Optional.ofNullable(client.fetchCatalog())
                            .filter(list -> !list.isEmpty())
                            .orElse(Collections.emptyList());
                    snapshot = new CatalogSnapshot(models, Instant.now());
                    current = snapshot;
                }
            }
        }
        return current.models();
    }

    public Optional<ImageGenerationModelDto> findModel(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return getCatalog().stream()
                .filter(model -> id.equals(model.id()))
                .findFirst();
    }

    public Optional<ImageGenerationQualityDto> findQuality(Long qualityId) {
        if (qualityId == null) {
            return Optional.empty();
        }
        return getCatalog().stream()
                .flatMap(model -> model.qualities().stream())
                .filter(quality -> qualityId.equals(quality.id()))
                .findFirst();
    }

    public Optional<ImageGenerationQualityDto> findQuality(Long modelId, Long qualityId) {
        if (modelId == null || qualityId == null) {
            return Optional.empty();
        }
        return findModel(modelId).flatMap(model -> model.qualities().stream()
                .filter(q -> qualityId.equals(q.id()))
                .findFirst());
    }

    public Optional<ImageGenerationQualityDto> findDefaultQuality(Long modelId) {
        return findModel(modelId).flatMap(model -> model.qualities().stream()
                .filter(ImageGenerationQualityDto::defaultQuality)
                .findFirst()
                .or(() -> model.qualities().stream().findFirst()));
    }

    private record CatalogSnapshot(List<ImageGenerationModelDto> models, Instant fetchedAt) {
        boolean isExpired() {
            return fetchedAt.plus(CACHE_TTL).isBefore(Instant.now());
        }
    }
}
