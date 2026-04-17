package com.marketinghub.mds.search;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SourceDedupService {

    public List<SourceSearchHit> deduplicate(List<SourceSearchHit> hits) {
        Map<String, SourceSearchHit> byKey = new LinkedHashMap<>();
        for (SourceSearchHit hit : hits) {
            String key = dedupKey(hit);
            SourceSearchHit existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, hit);
            } else {
                byKey.put(key, chooseBest(existing, hit));
            }
        }
        return List.copyOf(byKey.values());
    }

    private SourceSearchHit chooseBest(SourceSearchHit left, SourceSearchHit right) {
        return score(right) > score(left) ? right : left;
    }

    private int score(SourceSearchHit hit) {
        int score = 0;
        if (notBlank(hit.doi())) score += 4;
        if (notBlank(hit.abstractText())) score += 3;
        if (notBlank(hit.url())) score += 2;
        if (notBlank(hit.title())) score += 2;
        if (hit.openAccess()) score += 1;
        return score;
    }

    private String dedupKey(SourceSearchHit hit) {
        String doi = normalizeDoi(hit.doi());
        if (notBlank(doi)) return "doi:" + doi;

        String pmid = extractPmid(hit);
        if (notBlank(pmid)) return "pmid:" + pmid;

        String pmcid = extractPmcid(hit);
        if (notBlank(pmcid)) return "pmcid:" + pmcid;

        String normalizedTitle = normalizeTitle(hit.title());
        if (notBlank(normalizedTitle)) {
            return "title:" + normalizedTitle + ":" + normalizeBasic(hit.publicationYear());
        }

        String url = canonicalizeUrl(hit.url());
        if (notBlank(url)) return "url:" + url;

        return "fallback:" + normalizeBasic(hit.source()) + ":" + normalizeBasic(hit.sourceDocumentId());
    }

    private String extractPmid(SourceSearchHit hit) {
        String id = normalizeBasic(hit.sourceDocumentId());
        if (id.startsWith("pubmed:")) {
            return id.substring("pubmed:".length());
        }
        String url = normalizeBasic(hit.url());
        if (url.contains("pubmed.ncbi.nlm.nih.gov/")) {
            String tail = url.substring(url.indexOf("pubmed.ncbi.nlm.nih.gov/") + "pubmed.ncbi.nlm.nih.gov/".length());
            StringBuilder pmid = new StringBuilder();
            for (char c : tail.toCharArray()) {
                if (Character.isDigit(c)) pmid.append(c);
                else break;
            }
            return pmid.toString();
        }
        return "";
    }

    private String extractPmcid(SourceSearchHit hit) {
        String id = normalizeBasic(hit.sourceDocumentId());
        if (id.contains("pmc")) {
            int idx = id.indexOf("pmc");
            return id.substring(idx).replaceAll("[^a-z0-9]", "");
        }
        String url = normalizeBasic(hit.url());
        if (url.contains("/pmc/articles/")) {
            String tail = url.substring(url.indexOf("/pmc/articles/") + "/pmc/articles/".length());
            int stop = tail.indexOf('/');
            return (stop >= 0 ? tail.substring(0, stop) : tail).replaceAll("[^a-z0-9]", "");
        }
        return "";
    }

    private String normalizeDoi(String doi) {
        String value = normalizeBasic(doi);
        if (value.startsWith("https://doi.org/")) {
            value = value.substring("https://doi.org/".length());
        }
        if (value.startsWith("http://doi.org/")) {
            value = value.substring("http://doi.org/".length());
        }
        if (value.startsWith("doi:")) {
            value = value.substring("doi:".length());
        }
        return value;
    }

    private String normalizeTitle(String title) {
        String value = normalizeBasic(title);
        value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        value = value.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
        return value;
    }

    private String canonicalizeUrl(String url) {
        String value = normalizeBasic(url);
        if (value.startsWith("https://")) value = value.substring(8);
        if (value.startsWith("http://")) value = value.substring(7);
        if (value.startsWith("www.")) value = value.substring(4);
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);
        int fragment = value.indexOf('#');
        if (fragment >= 0) value = value.substring(0, fragment);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private String normalizeBasic(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
