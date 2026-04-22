package com.marketinghub.mois.service;

import com.marketinghub.mois.MoisDiscoveryRequest;
import java.util.List;

public interface MoisResearchGateway {

    MoisResearchResult discoverSources(MoisDiscoveryRequest request, List<String> seedUrls, List<String> seedQueries);

    record MoisResearchResult(List<MoisDiscoveredSource> sources, List<String> operationalErrors) {
    }

    record MoisDiscoveredSource(
            String sourceUrl,
            String sourceTitle,
            String sourceKind,
            Integer httpStatus,
            String normalizedText,
            String captureNotes,
            boolean success
    ) {
    }
}
