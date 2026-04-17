package com.marketinghub.mds.search;

import java.util.List;

public interface EvidenceSearchClient {
    String source();

    List<SourceSearchHit> search(String query, int limit);
}
