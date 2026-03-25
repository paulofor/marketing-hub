package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.service.provider.VideoProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProviderRegistry {
    private final List<VideoProvider> providers;

    public ProviderRegistry(List<VideoProvider> providers) {
        this.providers = providers;
    }

    public Optional<VideoProvider> resolve(SalesVideoJob job) {
        return providers.stream()
                .filter(provider -> provider.supports(job))
                .findFirst();
    }
}
