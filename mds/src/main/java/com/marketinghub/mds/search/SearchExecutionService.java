package com.marketinghub.mds.search;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchExecutionService {
    private final Map<String, EvidenceSearchClient> clientsBySource;

    public SearchExecutionService(List<EvidenceSearchClient> clients) {
        this.clientsBySource = clients.stream().collect(Collectors.toMap(c -> c.source().toLowerCase(), Function.identity()));
    }

    public List<SourceSearchHit> execute(List<SearchQueryPlan> plans) {
        List<SourceSearchHit> hits = new ArrayList<>();
        for (SearchQueryPlan plan : plans) {
            EvidenceSearchClient client = clientsBySource.get(plan.source().toLowerCase());
            if (client == null) {
                continue;
            }
            hits.addAll(client.search(plan.query(), plan.limit()));
        }
        return hits;
    }
}
