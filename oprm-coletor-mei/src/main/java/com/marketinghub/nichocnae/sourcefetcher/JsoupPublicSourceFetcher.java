package com.marketinghub.nichocnae.sourcefetcher;

import java.net.URI;
import java.time.Duration;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Coleta fontes públicas via Jsoup, mantendo apenas metadados e trecho curto para o backend OPRM. */
@Component
public class JsoupPublicSourceFetcher implements PublicSourceFetcher {
    private static final Logger log = LoggerFactory.getLogger(JsoupPublicSourceFetcher.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; MarketingHubOPRM/1.0; +https://oportunidadebrasil.shop)";
    private static final String DEFAULT_SOURCE_TYPE = "PUBLIC_CONTENT";
    private static final String FETCH_STATUS_COMPLETED = "COMPLETED";
    private static final String STORAGE_POLICY = "SHORT_EXCERPT_ONLY";
    private static final String LICENSE_STATE = "PUBLIC_SNIPPET";
    private static final int TIMEOUT_MILLIS = (int) Duration.ofSeconds(20).toMillis();
    private static final int MAX_SHORT_EXCERPT_LENGTH = 1200;
    private static final int MAX_SNIPPET_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_RAW_LOG_PREVIEW_LENGTH = 2000;

    /** Coleta uma fonte candidata e transforma o HTML recebido em snapshot curto contratual. */
    @Override
    public FetchedSourceSnapshot fetch(SourceFetcherPending pending) {
        validatePending(pending);
        try {
            Connection.Response response = Jsoup.connect(pending.sourceUrl())
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
            String rawHtml = response.body();
            log.info(
                    "Payload bruto recebido na etapa quatro OPRM nichocnae (sourceCandidateId={}, researchCycleId={}, sourceUrl={}, httpStatus={}, rawLength={}, rawPreview={})",
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    pending.sourceUrl(),
                    response.statusCode(),
                    rawHtml == null ? 0 : rawHtml.length(),
                    truncate(rawHtml, MAX_RAW_LOG_PREVIEW_LENGTH));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Fonte retornou HTTP " + response.statusCode());
            }
            return toSnapshot(pending, response, rawHtml);
        } catch (RuntimeException ex) {
            log.error(
                    "Erro runtime ao coletar fonte da etapa quatro OPRM nichocnae (sourceCandidateId={}, researchCycleId={}, sourceUrl={})",
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    pending.sourceUrl(),
                    ex);
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Erro de integração ao coletar fonte da etapa quatro OPRM nichocnae (sourceCandidateId={}, researchCycleId={}, sourceUrl={})",
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    pending.sourceUrl(),
                    ex);
            throw new IllegalStateException("Falha ao coletar fonte pública: " + ex.getMessage(), ex);
        }
    }

    /** Valida os campos mínimos necessários para acessar a fonte candidata. */
    private void validatePending(SourceFetcherPending pending) {
        if (pending == null) {
            throw new IllegalArgumentException("pending source is required");
        }
        if (!StringUtils.hasText(pending.sourceUrl())) {
            throw new IllegalArgumentException("sourceUrl is required");
        }
        if (!StringUtils.hasText(pending.sourceDomain())) {
            throw new IllegalArgumentException("sourceDomain is required");
        }
    }

    /** Converte a resposta HTTP bruta em snapshot curto sem carregar o HTML completo no contrato final. */
    private FetchedSourceSnapshot toSnapshot(SourceFetcherPending pending, Connection.Response response, String rawHtml) {
        Document document = Jsoup.parse(rawHtml == null ? "" : rawHtml, pending.sourceUrl());
        String visibleText = normalizeWhitespace(document.body() == null ? null : document.body().text());
        String fallbackSnippet = normalizeWhitespace(pending.sourceSnippet());
        String shortExcerpt = firstUsefulText(visibleText, fallbackSnippet);
        if (!StringUtils.hasText(shortExcerpt)) {
            throw new IllegalStateException("Fonte sem texto útil para shortExcerpt");
        }
        String title = firstUsefulText(normalizeWhitespace(document.title()), normalizeWhitespace(pending.sourceTitle()));
        if (!StringUtils.hasText(title)) {
            title = pending.sourceDomain();
        }
        return new FetchedSourceSnapshot(
                pending.sourceUrl(),
                pending.sourceDomain(),
                truncate(title, MAX_TITLE_LENGTH),
                DEFAULT_SOURCE_TYPE,
                sourceIntent(pending),
                routineEvidenceScore(pending, shortExcerpt),
                Boolean.TRUE.equals(pending.commercialPageRisk()),
                Boolean.TRUE.equals(pending.solutionLanguageRisk()),
                pending.sourceClassificationType(),
                pending.sourceFreshnessScore(),
                Boolean.TRUE.equals(pending.outdatedSourceRisk()),
                pending.brazilRelevanceScore(),
                pending.autonomousProfessionalEvidenceScore(),
                Boolean.TRUE.equals(pending.structuredBusinessDriftRisk()),
                pending.publishedAt(),
                truncate(fallbackSnippet, MAX_SNIPPET_LENGTH),
                truncate(shortExcerpt, MAX_SHORT_EXCERPT_LENGTH),
                FETCH_STATUS_COMPLETED,
                response.statusCode(),
                STORAGE_POLICY,
                LICENSE_STATE,
                routineEvidenceScore(pending, shortExcerpt));
    }

    /** Define a intenção propagada da etapa três, usando o grupo legado como compatibilidade. */
    private String sourceIntent(SourceFetcherPending pending) {
        return StringUtils.hasText(pending.sourceIntent()) ? pending.sourceIntent().trim() : pending.sourceGroup();
    }

    /** Define o escore de evidência de rotina propagado da etapa três ou recalculado por fallback simples. */
    private Integer routineEvidenceScore(SourceFetcherPending pending, String shortExcerpt) {
        return pending.routineEvidenceScore() == null
                ? calculateRelevanceScore(shortExcerpt, pending)
                : pending.routineEvidenceScore();
    }

    /** Calcula um score simples de relevância com base em texto útil e aderência ao domínio esperado. */
    private Integer calculateRelevanceScore(String shortExcerpt, SourceFetcherPending pending) {
        int score = StringUtils.hasText(shortExcerpt) && shortExcerpt.length() >= 300 ? 90 : 70;
        String fetchedDomain = extractDomain(pending.sourceUrl());
        if (StringUtils.hasText(fetchedDomain) && fetchedDomain.equalsIgnoreCase(pending.sourceDomain())) {
            score += 5;
        }
        return Math.min(score, 100);
    }

    /** Extrai o domínio de uma URL para validar aderência operacional da fonte. */
    private String extractDomain(String sourceUrl) {
        try {
            String host = URI.create(sourceUrl).getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (RuntimeException ex) {
            log.warn("Domínio da fonte inválido na etapa quatro OPRM nichocnae (sourceUrl={})", sourceUrl, ex);
            return null;
        }
    }

    /** Escolhe o primeiro texto útil dentre as opções normalizadas. */
    private String firstUsefulText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    /** Normaliza espaços de texto extraído de HTML ou snippets públicos. */
    private String normalizeWhitespace(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    /** Limita texto ao tamanho contratual máximo preservando campos curtos. */
    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalizeWhitespace(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
