package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupCompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupEcosystemType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupPlatform;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupRecommendation;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSignalCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSignalType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSourceCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSourceType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSummaryCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupTemperature;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orquestra a pesquisa pública V1 e monta o dossiê inicial de aquecimento de mercado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketWarmupProcessor {
    private final MarketWarmupQueryBuilder queryBuilder;
    private final PublicWebSearchClient searchClient;

    /**
     * Processa o job reservado sem acessar banco e devolve o payload estruturado aceito pelo backend.
     */
    public MarketWarmupCompleteRequest process(MarketWarmupClaimedJob job, int searchLimit) throws IOException {
        List<String> queries = queryBuilder.buildQueries(job);
        List<PublicSearchResult> rawResults = new ArrayList<>();
        for (String query : queries) {
            rawResults.addAll(searchClient.search(query, searchLimit));
        }
        List<PublicSearchResult> deduplicatedResults = deduplicate(rawResults).stream().limit(searchLimit * 2L).toList();
        if (deduplicatedResults.isEmpty()) {
            throw new IllegalStateException("Busca pública não retornou fontes rastreáveis para montar o dossiê");
        }
        List<MarketWarmupSourceCompleteItem> sources = buildSources(deduplicatedResults);
        List<MarketWarmupSignalCompleteItem> signals = buildSignals(deduplicatedResults);
        MarketWarmupSummaryCompleteItem summary = buildSummary(job, sources, signals);
        log.info("MOIS market-warmup dossier built. jobId={}, pageId={}, queries={}, sources={}, signals={}, score={}",
                job.jobId(), job.pageId(), queries.size(), sources.size(), signals.size(), summary.scoreTotal());
        return new MarketWarmupCompleteRequest(sources, signals, summary, Instant.now());
    }

    /**
     * Remove duplicidades por URL mantendo a primeira evidência encontrada.
     */
    private List<PublicSearchResult> deduplicate(List<PublicSearchResult> rawResults) {
        Set<String> seen = new LinkedHashSet<>();
        List<PublicSearchResult> unique = new ArrayList<>();
        for (PublicSearchResult result : rawResults) {
            String key = result.url() == null ? "" : result.url().trim().toLowerCase(Locale.ROOT);
            if (!key.isBlank() && seen.add(key)) {
                unique.add(result);
            }
        }
        return unique;
    }

    /**
     * Converte resultados públicos em fontes contratuais rastreáveis para revisão humana.
     */
    private List<MarketWarmupSourceCompleteItem> buildSources(List<PublicSearchResult> results) {
        List<MarketWarmupSourceCompleteItem> sources = new ArrayList<>();
        for (PublicSearchResult result : results) {
            sources.add(new MarketWarmupSourceCompleteItem(
                    detectPlatform(result.url()),
                    detectSourceType(result),
                    result.url(),
                    result.title(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    scoreRecency(result),
                    scoreEngagement(result),
                    buildEvidenceSummary(result)));
        }
        return sources;
    }

    /**
     * Extrai sinais comerciais básicos a partir de título, URL e resumo público.
     */
    private List<MarketWarmupSignalCompleteItem> buildSignals(List<PublicSearchResult> results) {
        List<MarketWarmupSignalCompleteItem> signals = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            PublicSearchResult result = results.get(index);
            String text = normalizeText(result.title() + " " + result.snippet() + " " + result.url());
            addSignal(signals, index, MarketWarmupSignalType.CONTENT_RECENCY, "Fonte pública encontrada em busca web recente", "Há conversa pública rastreável sobre o tema.", 4);
            if (containsAny(text, "dor", "sofr", "problema", "dificuldade", "ansiedade", "emagrecer", "tratamento")) {
                addSignal(signals, index, MarketWarmupSignalType.PAIN_EXPLICIT, buildSignalText(result), "A comunicação pública expõe dor ou esforço do público.", 6);
            }
            if (containsAny(text, "comprar", "preço", "vale a pena", "review", "funciona", "depoimento")) {
                addSignal(signals, index, MarketWarmupSignalType.BUYING_INTENT, buildSignalText(result), "Há indício de comparação, decisão de compra ou validação pré-compra.", 7);
            }
            if (containsAny(text, "reclama", "golpe", "não funciona", "risco", "contraindica", "procon")) {
                addSignal(signals, index, MarketWarmupSignalType.OBJECTION, buildSignalText(result), "Existe objeção ou risco de confiança que precisa ser tratado na oferta.", 7);
            }
            if (containsAny(text, "youtube", "instagram", "tiktok", "canal", "influencer")) {
                addSignal(signals, index, MarketWarmupSignalType.CHANNEL_FIT, buildSignalText(result), "A fonte sugere canal público adequado para aquecer ou vender.", 5);
            }
            if (containsAny(text, "afiliado", "concorrente", "hotmart", "clickbank", "produto")) {
                addSignal(signals, index, MarketWarmupSignalType.COMPETITOR_OFFER, buildSignalText(result), "Há sinal de maturidade de oferta ou concorrência no mercado.", 5);
            }
        }
        return signals;
    }

    /**
     * Adiciona um sinal ao dossiê preservando força numérica de 0 a 10.
     */
    private void addSignal(List<MarketWarmupSignalCompleteItem> signals, int sourceIndex, MarketWarmupSignalType type, String text, String interpretation, int strength) {
        signals.add(new MarketWarmupSignalCompleteItem(sourceIndex, type, BigDecimal.valueOf(strength), text, interpretation));
    }

    /**
     * Calcula um resumo comercial inicial e simples até a implantação do motor completo de score.
     */
    private MarketWarmupSummaryCompleteItem buildSummary(MarketWarmupClaimedJob job, List<MarketWarmupSourceCompleteItem> sources, List<MarketWarmupSignalCompleteItem> signals) {
        int painSignals = countSignals(signals, MarketWarmupSignalType.PAIN_EXPLICIT);
        int intentSignals = countSignals(signals, MarketWarmupSignalType.BUYING_INTENT);
        int competitorSignals = countSignals(signals, MarketWarmupSignalType.COMPETITOR_OFFER);
        int objectionSignals = countSignals(signals, MarketWarmupSignalType.OBJECTION);
        int score = Math.min(100, 20 + Math.min(20, sources.size() * 3) + Math.min(20, painSignals * 5) + Math.min(20, intentSignals * 5) + Math.min(10, competitorSignals * 3) - Math.min(10, objectionSignals * 2));
        MarketWarmupTemperature temperature = classifyTemperature(score, objectionSignals);
        MarketWarmupRecommendation recommendation = classifyRecommendation(temperature);
        return new MarketWarmupSummaryCompleteItem(
                BigDecimal.valueOf(score),
                temperature,
                classifyEcosystem(sources, competitorSignals, objectionSignals),
                recommendation,
                List.of(firstUseful(job.promiseSummary(), "Dor principal inferida das fontes públicas e da promessa analisada.")),
                objectionSignals > 0 ? List.of("Confiança no método e prova de resultado precisam ser reforçadas.") : List.of("Objeções explícitas ainda não apareceram com força na busca V1."),
                List.of(firstUseful(job.promiseSummary(), "Promessa principal ainda precisa de refinamento pela análise comercial.")),
                collectChannels(sources),
                competitorSignals > 0 ? List.of("Há sinais de produtos, afiliados ou ofertas concorrentes nas fontes públicas.") : List.of("Concorrentes diretos não foram fortes no dossiê V1."),
                objectionSignals >= 3 ? "Risco moderado de desconfiança ou saturação; exigir ângulo e prova mais específicos." : "Risco de saturação baixo ou não conclusivo na coleta V1.",
                buildOpportunityRecommendation(temperature),
                "Validar experimento usando a dor e a promessa com maior presença nas fontes rastreáveis.");
    }

    /**
     * Conta sinais por tipo para manter o score explicável.
     */
    private int countSignals(List<MarketWarmupSignalCompleteItem> signals, MarketWarmupSignalType type) {
        return (int) signals.stream().filter(signal -> signal.signalType() == type).count();
    }

    /**
     * Classifica a temperatura comercial usando pontuação e risco de objeção.
     */
    private MarketWarmupTemperature classifyTemperature(int score, int objectionSignals) {
        if (objectionSignals >= 5) {
            return MarketWarmupTemperature.SATURATED;
        }
        if (score >= 80) {
            return MarketWarmupTemperature.HOT;
        }
        if (score >= 60) {
            return MarketWarmupTemperature.PROMISING;
        }
        if (score >= 40) {
            return MarketWarmupTemperature.WARM;
        }
        return MarketWarmupTemperature.COLD;
    }

    /**
     * Classifica a recomendação objetiva a partir da temperatura inicial.
     */
    private MarketWarmupRecommendation classifyRecommendation(MarketWarmupTemperature temperature) {
        return switch (temperature) {
            case HOT, PROMISING -> MarketWarmupRecommendation.PRIORITIZE;
            case WARM -> MarketWarmupRecommendation.RESEARCH_MORE;
            case SATURATED -> MarketWarmupRecommendation.SATURATED_REQUIRES_ANGLE;
            case COLD -> MarketWarmupRecommendation.DISCARD;
        };
    }

    /**
     * Classifica o tipo de ecossistema dominante com heurística compatível com a V1.
     */
    private MarketWarmupEcosystemType classifyEcosystem(List<MarketWarmupSourceCompleteItem> sources, int competitorSignals, int objectionSignals) {
        if (objectionSignals >= 5) {
            return MarketWarmupEcosystemType.SATURATED;
        }
        if (competitorSignals >= 2) {
            return MarketWarmupEcosystemType.COMPETITORS_HEATED;
        }
        boolean hasCreator = sources.stream().anyMatch(source -> source.platform() == MarketWarmupPlatform.YOUTUBE || source.platform() == MarketWarmupPlatform.INSTAGRAM || source.platform() == MarketWarmupPlatform.TIKTOK);
        return hasCreator ? MarketWarmupEcosystemType.CREATORS_HEATED : MarketWarmupEcosystemType.RECURRING_PAIN_HEATED;
    }

    /**
     * Monta recomendação de negócio direta para orientar o próximo experimento.
     */
    private String buildOpportunityRecommendation(MarketWarmupTemperature temperature) {
        return switch (temperature) {
            case HOT -> "Priorizar experimento: há evidência pública suficiente para testar oferta rapidamente.";
            case PROMISING -> "Priorizar com refinamento de ângulo: há sinais de demanda, mas a promessa deve ser diferenciada.";
            case WARM -> "Pesquisar mais antes de criar oferta: o mercado tem sinais, mas ainda não sustenta prioridade máxima.";
            case SATURATED -> "Avançar somente com ângulo diferenciado e prova forte para reduzir desconfiança.";
            case COLD -> "Baixa prioridade comercial no momento; buscar outro mercado com dor mais explícita.";
        };
    }

    /**
     * Detecta a plataforma da fonte a partir do domínio público.
     */
    private MarketWarmupPlatform detectPlatform(String url) {
        String normalized = normalizeText(url);
        if (normalized.contains("youtube.com") || normalized.contains("youtu.be")) {
            return MarketWarmupPlatform.YOUTUBE;
        }
        if (normalized.contains("instagram.com")) {
            return MarketWarmupPlatform.INSTAGRAM;
        }
        if (normalized.contains("tiktok.com")) {
            return MarketWarmupPlatform.TIKTOK;
        }
        if (normalized.contains("reclameaqui") || normalized.contains("trustpilot")) {
            return MarketWarmupPlatform.REVIEW_SITE;
        }
        if (normalized.contains("hotmart") || normalized.contains("clickbank")) {
            return MarketWarmupPlatform.MARKETPLACE;
        }
        return MarketWarmupPlatform.WEB;
    }

    /**
     * Detecta o tipo funcional da fonte pública para explicar o dossiê.
     */
    private MarketWarmupSourceType detectSourceType(PublicSearchResult result) {
        String text = normalizeText(result.title() + " " + result.snippet() + " " + result.url());
        if (containsAny(text, "review", "vale a pena", "funciona")) {
            return MarketWarmupSourceType.REVIEW;
        }
        if (containsAny(text, "reclama", "golpe", "procon")) {
            return MarketWarmupSourceType.COMPLAINT;
        }
        if (containsAny(text, "youtube", "instagram", "tiktok", "canal")) {
            return MarketWarmupSourceType.CREATOR_CONTENT;
        }
        if (containsAny(text, "afiliado", "hotmart", "clickbank")) {
            return MarketWarmupSourceType.AFFILIATE_PROMOTION;
        }
        return MarketWarmupSourceType.SEARCH_RESULT;
    }

    /**
     * Atribui pontuação simples de recência quando a fonte foi encontrada em busca pública atual.
     */
    private BigDecimal scoreRecency(PublicSearchResult result) {
        return BigDecimal.valueOf(result.snippet() == null || result.snippet().isBlank() ? 3 : 5);
    }

    /**
     * Atribui pontuação inicial de engajamento por sinais textuais de interação comercial.
     */
    private BigDecimal scoreEngagement(PublicSearchResult result) {
        String text = normalizeText(result.title() + " " + result.snippet());
        return containsAny(text, "coment", "pergunta", "review", "depoimento", "preço") ? BigDecimal.valueOf(6) : BigDecimal.valueOf(3);
    }

    /**
     * Monta resumo de evidência sem inserir metadado técnico no artefato final.
     */
    private String buildEvidenceSummary(PublicSearchResult result) {
        String snippet = result.snippet() == null || result.snippet().isBlank() ? "Fonte pública encontrada na busca inicial." : result.snippet();
        return limit(snippet, 480);
    }

    /**
     * Monta texto curto do sinal baseado na evidência pública rastreável.
     */
    private String buildSignalText(PublicSearchResult result) {
        return limit((result.title() == null ? "" : result.title()) + " — " + (result.snippet() == null ? "" : result.snippet()), 480);
    }

    /**
     * Coleta canais principais encontrados no conjunto de fontes.
     */
    private List<String> collectChannels(List<MarketWarmupSourceCompleteItem> sources) {
        return sources.stream()
                .map(source -> source.platform().name())
                .distinct()
                .limit(5)
                .toList();
    }

    /**
     * Escolhe o primeiro texto útil ou devolve uma alternativa de negócio.
     */
    private String firstUseful(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : limit(value, 240);
    }

    /**
     * Verifica se um texto contém algum termo de interesse comercial.
     */
    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normaliza texto para comparação simples de palavras-chave.
     */
    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Limita texto funcional para não poluir o resumo apresentado ao usuário.
     */
    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength).trim() : normalized;
    }
}
