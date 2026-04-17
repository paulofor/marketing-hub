package com.marketinghub.mds.search;

import com.marketinghub.mds.config.MdsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchExecutionService {
    private static final Logger log = LoggerFactory.getLogger(SearchExecutionService.class);
    private final Map<String, EvidenceSearchClient> clientsBySource;
    private final int retryMaxAttempts;
    private final int retryBackoffMs;

    public SearchExecutionService(List<EvidenceSearchClient> clients,
                                  MdsProperties properties) {
        this.clientsBySource = clients.stream().collect(Collectors.toMap(c -> c.source().toLowerCase(), Function.identity()));
        this.retryMaxAttempts = Math.max(1, properties.getSearch().getRetryMaxAttempts());
        this.retryBackoffMs = Math.max(0, properties.getSearch().getRetryBackoffMs());
    }

    public List<SourceSearchHit> execute(List<SearchQueryPlan> plans) {
        List<SourceSearchHit> hits = new ArrayList<>();
        int sourcesWithFailure = 0;
        for (SearchQueryPlan plan : plans) {
            EvidenceSearchClient client = clientsBySource.get(plan.source().toLowerCase());
            if (client == null) {
                continue;
            }
            try {
                hits.addAll(searchWithRetry(client, plan));
            } catch (RecoverableSourceException ex) {
                sourcesWithFailure++;
                log.warn("mds-source-recoverable-failure source={} query={} attempts={} reason={}",
                        plan.source(), plan.query(), retryMaxAttempts, ex.getMessage());
            }
        }

        if (!plans.isEmpty() && sourcesWithFailure == plans.size()) {
            throw new RecoverableSourceException("all configured sources failed during evidence search");
        }
        return hits;
    }

    private List<SourceSearchHit> searchWithRetry(EvidenceSearchClient client, SearchQueryPlan plan) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                return client.search(plan.query(), plan.limit());
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < retryMaxAttempts) {
                    sleepBackoff();
                }
            }
        }
        throw new RecoverableSourceException("source " + plan.source() + " exhausted retry attempts", lastException);
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NonRecoverablePipelineException("search retry backoff interrupted", ex);
        }
    }
}
