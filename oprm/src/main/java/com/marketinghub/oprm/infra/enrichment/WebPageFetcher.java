package com.marketinghub.oprm.infra.enrichment;

public interface WebPageFetcher {
    FetchedWebPage fetch(String url);
}
