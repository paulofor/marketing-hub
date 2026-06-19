package com.marketinghub.nichocnaev2.pipeline.sourcesafetyfilter;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Executa o filtro plugável de segurança e canonicalização de URLs candidatas do NichoCNAE versão 2. */
public final class SourceSafetyFilterProcessor implements StageProcessor {
    private static final Set<String> HARD_BLOCKED_HOST_FRAGMENTS = Set.of(
            "adult", "porn", "casino", "bet", "torrent", "warez", "malware", "phishing", "xxx");
    private static final Set<String> TRACKING_QUERY_PREFIXES = Set.of("utm_", "fbclid", "gclid", "msclkid");

    /** Filtra URLs inseguras, deduplica canônicos e devolve somente metadados das rejeições. */
    @Override
    public StageResult process(StageContext context) {
        List<String> rawUrls = extractCandidateUrls(context.input());
        List<Map<String, Object>> allowedUrls = new ArrayList<>();
        List<Map<String, Object>> rejectedUrls = new ArrayList<>();
        Set<String> seenCanonicalUrls = new LinkedHashSet<>();
        for (String rawUrl : rawUrls) {
            SafetyDecision decision = classify(rawUrl);
            if (!decision.allowed()) {
                rejectedUrls.add(rejection(rawUrl, decision.category(), decision.reason()));
                continue;
            }
            if (!seenCanonicalUrls.add(decision.canonicalUrl())) {
                rejectedUrls.add(rejection(rawUrl, "DUPLICATE", "URL canônica já havia sido aceita nesta execução."));
                continue;
            }
            allowedUrls.add(allowed(decision.canonicalUrl(), decision.host()));
        }
        String safetyDecision = rejectedUrls.size() == rawUrls.size() && !rawUrls.isEmpty() ? "HARD_REJECT" : "ALLOW";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-safety-filter");
        output.put("safetyDecision", safetyDecision);
        output.put("allowedUrlCount", allowedUrls.size());
        output.put("rejectedUrlCount", rejectedUrls.size());
        output.put("allowedUrls", allowedUrls);
        output.put("rejectedSources", rejectedUrls);
        return new StageResult(safetyDecision, output, List.of(new StageArtifact(
                "SAFETY_SUMMARY", "inline://source-safety-filter/summary", "Resumo auditável sem persistir conteúdo bloqueado.")));
    }

    /** Extrai URLs candidatas aceitando contratos simples de entrada sem acoplar a etapa anterior. */
    private List<String> extractCandidateUrls(Map<String, Object> input) {
        Object candidateUrls = input.get("candidateUrls");
        if (!(candidateUrls instanceof List<?> values)) {
            candidateUrls = input.get("urls");
        }
        if (!(candidateUrls instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    /** Classifica uma URL bruta usando hard blocklist local e regras determinísticas de canonicalização. */
    private SafetyDecision classify(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return SafetyDecision.rejected("INVALID_URL", "URL vazia não pode entrar no pipeline.");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            return SafetyDecision.rejected("INVALID_URL", "URL inválida não pode entrar no pipeline.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return SafetyDecision.rejected("UNSUPPORTED_SCHEME", "Somente HTTP e HTTPS são permitidos para coleta pública.");
        }
        if (host.isBlank()) {
            return SafetyDecision.rejected("INVALID_DOMAIN", "Domínio ausente não pode ser auditado.");
        }
        if (HARD_BLOCKED_HOST_FRAGMENTS.stream().anyMatch(host::contains)) {
            return SafetyDecision.rejected("HARD_BLOCKED_DOMAIN", "Domínio bloqueado por categoria proibida.");
        }
        return SafetyDecision.allowed(canonicalize(uri, scheme, host), host);
    }

    /** Remove fragmentos e parâmetros de rastreamento para deduplicar a página real. */
    private String canonicalize(URI uri, String scheme, String host) {
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        String query = canonicalQuery(uri.getRawQuery());
        return scheme + "://" + host + path + (query.isBlank() ? "" : "?" + query);
    }

    /** Mantém apenas parâmetros funcionais, removendo tracking conhecido. */
    private String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        List<String> kept = new ArrayList<>();
        for (String parameter : rawQuery.split("&")) {
            String key = parameter.contains("=") ? parameter.substring(0, parameter.indexOf('=')) : parameter;
            String normalizedKey = key.toLowerCase(Locale.ROOT);
            boolean tracking = TRACKING_QUERY_PREFIXES.stream().anyMatch(normalizedKey::startsWith);
            if (!tracking) {
                kept.add(parameter);
            }
        }
        return String.join("&", kept);
    }

    /** Monta o item aceito sem conteúdo textual da página. */
    private Map<String, Object> allowed(String canonicalUrl, String host) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("canonicalUrl", canonicalUrl);
        item.put("domain", host);
        item.put("safetyDecision", "ALLOW");
        item.put("persistContent", true);
        return item;
    }

    /** Monta rejeição persistindo só categoria e motivo, sem snippet de conteúdo bloqueado. */
    private Map<String, Object> rejection(String rawUrl, String category, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("urlFingerprint", Integer.toHexString(String.valueOf(rawUrl).hashCode()));
        item.put("safetyCategory", category);
        item.put("decision", "HARD_REJECT");
        item.put("persistContent", false);
        item.put("reason", reason);
        return item;
    }

    /** Representa a classificação determinística de segurança de uma URL candidata. */
    private record SafetyDecision(boolean allowed, String canonicalUrl, String host, String category, String reason) {
        /** Cria decisão de aceitação com URL canônica. */
        private static SafetyDecision allowed(String canonicalUrl, String host) {
            return new SafetyDecision(true, canonicalUrl, host, "ALLOW", "Fonte permitida.");
        }

        /** Cria decisão de rejeição sem preservar conteúdo sensível. */
        private static SafetyDecision rejected(String category, String reason) {
            return new SafetyDecision(false, null, null, category, reason);
        }
    }
}
