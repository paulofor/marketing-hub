package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupCompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupEcosystemType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupPlatform;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupRecommendation;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSearchAttemptCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSearchResultCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSearchTermCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupFinalDossierCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSignalCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSignalType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSourceCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSourceType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSummaryCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupTemperature;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
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
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> PRODUCT_STOPWORDS = Set.of(
            "para",
            "com",
            "sem",
            "dos",
            "das",
            "uma",
            "por",
            "que",
            "curso",
            "produto",
            "oferta",
            "metodo",
            "hotmart",
            "certificacao",
            "avancada");

    private final MarketWarmupQueryBuilder queryBuilder;
    private final MarketWarmupQueryPlanner queryPlanner;
    private final PublicWebSearchClient searchClient;

    /**
     * Processa o job reservado sem acessar banco e devolve o payload estruturado aceito pelo backend.
     */
    public MarketWarmupCompleteRequest process(MarketWarmupClaimedJob job, int searchLimit) throws IOException {
        List<String> baseQueries = queryBuilder.buildQueries(job);
        List<String> queries = queryPlanner.planQueries(job, baseQueries);
        List<PublicSearchResult> rawResults = new ArrayList<>();
        List<MarketWarmupSearchAttemptCompleteItem> searchAttempts = new ArrayList<>();
        List<MarketWarmupSearchResultCompleteItem> searchResults = new ArrayList<>();
        for (String query : queries) {
            List<PublicSearchResult> queryResults = searchClient.search(query, searchLimit);
            rawResults.addAll(queryResults);
            searchResults.addAll(buildSearchResults(job, query, queryResults));
            searchAttempts.add(buildSearchAttempt(job, query, queryResults));
        }
        List<PublicSearchResult> deduplicatedResults = deduplicate(rawResults).stream().limit(searchLimit * 2L).toList();
        List<PublicSearchResult> qualifiedResults = keepOnlyQualifiedProducerSocialResults(job, deduplicatedResults);
        if (qualifiedResults.isEmpty()) {
            throw new MarketWarmupNoQualifiedSourcesException("Busca pública não retornou fontes rastreáveis para montar o dossiê", searchAttempts);
        }
        List<MarketWarmupSourceCompleteItem> sources = buildSources(qualifiedResults);
        List<MarketWarmupSignalCompleteItem> signals = buildSignals(qualifiedResults);
        MarketWarmupSummaryCompleteItem summary = buildSummary(job, sources, signals);
        List<MarketWarmupSearchTermCompleteItem> searchTerms = queries.stream()
                .map(query -> new MarketWarmupSearchTermCompleteItem(query, "Termo usado para medir prestígio público e aquecimento externo do produto.", "OPENAI_OR_FALLBACK"))
                .toList();
        MarketWarmupFinalDossierCompleteItem finalDossier = buildFinalDossier(job, sources, signals, summary);
        log.info("MOIS market-warmup dossier built. jobId={}, pageId={}, queries={}, sources={}, signals={}, score={}",
                job.jobId(), job.pageId(), queries.size(), sources.size(), signals.size(), summary.scoreTotal());
        return new MarketWarmupCompleteRequest(searchAttempts, sources, signals, summary, searchTerms, searchResults, finalDossier, Instant.now());
    }


    /**
     * Converte resultados de busca em evidências persistíveis relacionadas ao termo pesquisado.
     */
    private List<MarketWarmupSearchResultCompleteItem> buildSearchResults(MarketWarmupClaimedJob job, String query, List<PublicSearchResult> results) {
        List<PublicSearchResult> qualified = keepOnlyQualifiedProducerSocialResults(job, results);
        Set<String> qualifiedUrls = qualified.stream().map(PublicSearchResult::url).collect(java.util.stream.Collectors.toSet());
        return results.stream()
                .map(result -> new MarketWarmupSearchResultCompleteItem(query, result.url(), result.title(), result.snippet(), result.rawPayload(), qualifiedUrls.contains(result.url())))
                .toList();
    }

    /**
     * Monta o texto final simples do dossiê para a tela sem depender de recomputação no frontend.
     */
    private MarketWarmupFinalDossierCompleteItem buildFinalDossier(
            MarketWarmupClaimedJob job,
            List<MarketWarmupSourceCompleteItem> sources,
            List<MarketWarmupSignalCompleteItem> signals,
            MarketWarmupSummaryCompleteItem summary
    ) {
        String resources = sources.stream()
                .map(source -> source.platform() + ": " + source.sourceTitle() + " (" + source.sourceUrl() + ")")
                .limit(8)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Nenhum recurso externo qualificado foi identificado.");
        String prestige = "Foram encontradas " + sources.size() + " fontes externas qualificadas e " + signals.size()
                + " sinais de aquecimento relacionados ao produto " + nullSafe(job.title()) + ".";
        String conclusion = summary.opportunityRecommendation() == null || summary.opportunityRecommendation().isBlank()
                ? "Dossiê concluído para apoiar decisão comercial da biblioteca."
                : summary.opportunityRecommendation();
        return new MarketWarmupFinalDossierCompleteItem(prestige, resources, conclusion, null);
    }

    /**
     * Resume uma tentativa de busca para a tela explicar o que foi pesquisado e por que não virou fonte.
     */
    private MarketWarmupSearchAttemptCompleteItem buildSearchAttempt(MarketWarmupClaimedJob job, String query, List<PublicSearchResult> results) {
        List<PublicSearchResult> qualified = keepOnlyQualifiedProducerSocialResults(job, results);
        PublicSearchResult sample = results.isEmpty() ? null : results.getFirst();
        String status = qualified.isEmpty() ? "NO_QUALIFIED_SOURCE" : "QUALIFIED_SOURCE_FOUND";
        String reason = qualified.isEmpty()
                ? "A busca retornou resultados genéricos ou sem ligação comprovável com produto/produtor."
                : "A busca retornou fonte compatível com as âncoras do dossiê.";
        return new MarketWarmupSearchAttemptCompleteItem(query, results.size(), qualified.size(), Math.max(0, results.size() - qualified.size()),
                status, reason, sample == null ? null : sample.title(), sample == null ? null : sample.url());
    }

    /**
     * Mantém fontes sociais do produtor somente quando o mesmo nome aparece junto de conteúdo semelhante ao produto.
     */
    private List<PublicSearchResult> keepOnlyQualifiedProducerSocialResults(MarketWarmupClaimedJob job, List<PublicSearchResult> results) {
        List<String> producerTokens = meaningfulTokens(job.producerName());
        Set<String> productTokens = new LinkedHashSet<>();
        productTokens.addAll(meaningfulTokens(job.title()));
        productTokens.addAll(meaningfulTokens(job.promiseSummary()));
        productTokens.addAll(meaningfulTokens(job.mechanismSummary()));
        productTokens.addAll(meaningfulTokens(job.offerSummary()));
        List<String> requiredProducerTokens = producerTokens.isEmpty() ? List.of() : List.of(producerTokens.getFirst());
        List<PublicSearchResult> qualified = new ArrayList<>();
        for (PublicSearchResult result : results) {
            MarketWarmupPlatform platform = detectPlatform(result.url());
            String text = normalizedComparableText(result.title() + " " + result.snippet() + " " + result.url());
            if (!isProducerSocialPlatform(platform) && containsQualifiedCommercialContext(text, requiredProducerTokens, productTokens)) {
                qualified.add(result);
                continue;
            }
            if (containsAllTokens(text, producerTokens) && containsEnoughProductSimilarity(text, productTokens)) {
                qualified.add(result);
            } else {
                log.info("MOIS market-warmup source ignored because it does not match the product dossier anchors. jobId={}, pageId={}, platform={}, url={}",
                        job.jobId(), job.pageId(), platform, result.url());
            }
        }
        return qualified;
    }

    /**
     * Exige contexto comercial mínimo em fontes web para impedir que palavras genéricas do título virem dossiê falso.
     */
    private boolean containsQualifiedCommercialContext(String text, List<String> producerTokens, Set<String> productTokens) {
        return containsAllTokens(text, producerTokens) || containsEnoughProductSimilarity(text, productTokens);
    }

    /**
     * Identifica plataformas sociais onde homônimos do produtor precisam ser bloqueados.
     */
    private boolean isProducerSocialPlatform(MarketWarmupPlatform platform) {
        return platform == MarketWarmupPlatform.YOUTUBE || platform == MarketWarmupPlatform.INSTAGRAM || platform == MarketWarmupPlatform.TIKTOK;
    }

    /**
     * Verifica se todos os tokens relevantes do nome do produtor aparecem na fonte social.
     */
    private boolean containsAllTokens(String text, List<String> tokens) {
        return !tokens.isEmpty() && tokens.stream().allMatch(text::contains);
    }

    /**
     * Exige semelhança mínima com o produto para evitar perfis corretos tratando de outro assunto.
     */
    private boolean containsEnoughProductSimilarity(String text, Set<String> productTokens) {
        if (productTokens.isEmpty()) {
            return false;
        }
        long matches = productTokens.stream().filter(text::contains).count();
        return matches >= Math.min(2, productTokens.size());
    }

    /**
     * Extrai tokens comparáveis de nomes e temas comerciais removendo ruído comum.
     */
    private List<String> meaningfulTokens(String value) {
        String normalized = normalizedComparableText(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 3)
                .filter(token -> !PRODUCT_STOPWORDS.contains(token))
                .distinct()
                .toList();
    }

    /**
     * Normaliza acentos, caixa e pontuação para comparação robusta entre produtor e conteúdo.
     */
    private String normalizedComparableText(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return NON_ALPHANUMERIC.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Normaliza textos nulos para montar conclusão final segura.
     */
    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "produto sem título" : value;
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
            if (containsAny(text, "youtube", "instagram", "tiktok", "canal", "influencer", "whatsapp", "live")) {
                addSignal(signals, index, MarketWarmupSignalType.CHANNEL_FIT, buildSignalText(result), "A fonte sugere canal público usado para aquecer, capturar ou vender.", 6);
            }
            if (containsAny(text, "fundador", "fundadora", "especialista", "professora", "mentor", "seguidores", "autoridade", "criadora", "criador")) {
                addSignal(signals, index, MarketWarmupSignalType.CREATOR_AUTHORITY, buildSignalText(result), "Há sinal de autoridade pessoal ou marca especialista por trás do produto.", 8);
            }
            if (containsAny(text, "depoimento", "alunas", "alunos", "resultado", "transformação", "antes e depois", "casos de sucesso")) {
                addSignal(signals, index, MarketWarmupSignalType.SOCIAL_PROOF, buildSignalText(result), "Há prova social pública que ajuda a explicar a confiança na oferta.", 7);
            }
            if (containsAny(text, "afiliado", "concorrente", "hotmart", "clickbank", "produto", "oferta", "bônus", "checkout")) {
                addSignal(signals, index, MarketWarmupSignalType.COMPETITOR_OFFER, buildSignalText(result), "Há sinal de maturidade de oferta, distribuição ou venda do produto.", 5);
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
        int authoritySignals = countSignals(signals, MarketWarmupSignalType.CREATOR_AUTHORITY);
        int socialProofSignals = countSignals(signals, MarketWarmupSignalType.SOCIAL_PROOF);
        int channelSignals = countSignals(signals, MarketWarmupSignalType.CHANNEL_FIT);
        int objectionSignals = countSignals(signals, MarketWarmupSignalType.OBJECTION);
        int score = Math.min(100, 20 + Math.min(20, sources.size() * 3) + Math.min(20, painSignals * 5) + Math.min(20, intentSignals * 5) + Math.min(15, authoritySignals * 5) + Math.min(15, socialProofSignals * 5) + Math.min(10, channelSignals * 3) + Math.min(10, competitorSignals * 3) - Math.min(10, objectionSignals * 2));
        MarketWarmupTemperature temperature = classifyTemperature(score, objectionSignals);
        MarketWarmupRecommendation recommendation = classifyRecommendation(temperature);
        return new MarketWarmupSummaryCompleteItem(
                BigDecimal.valueOf(score),
                temperature,
                classifyEcosystem(sources, competitorSignals, objectionSignals),
                recommendation,
                buildMainPains(job, painSignals),
                buildMainObjections(objectionSignals),
                buildMainPromises(job, painSignals),
                collectChannels(sources),
                buildSuccessLevers(authoritySignals, socialProofSignals, competitorSignals, channelSignals),
                buildSaturationRisk(objectionSignals),
                buildOpportunityRecommendation(temperature, authoritySignals, socialProofSignals, channelSignals),
                buildNextInvestigationSuggestion(authoritySignals, socialProofSignals, channelSignals));
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
     * Monta dores somente quando a coleta pública traz sinal compatível ou a análise comercial já trouxe promessa útil.
     */
    private List<String> buildMainPains(MarketWarmupClaimedJob job, int painSignals) {
        String promise = firstUseful(job.promiseSummary(), "");
        if (painSignals == 0 && promise.isBlank()) {
            return List.of();
        }
        if (promise.isBlank()) {
            return List.of("Há sinais públicos de dor, mas o modelo ainda precisa consolidar a dor central.");
        }
        return List.of(promise);
    }

    /**
     * Monta objeções apenas a partir de sinais públicos de objeção.
     */
    private List<String> buildMainObjections(int objectionSignals) {
        if (objectionSignals == 0) {
            return List.of();
        }
        return List.of("A pesquisa pública encontrou sinais de objeção; revisar confiança, prova e risco antes de usar a referência.");
    }

    /**
     * Monta promessas somente quando a análise comercial forneceu promessa útil para cruzar com a pesquisa.
     */
    private List<String> buildMainPromises(MarketWarmupClaimedJob job, int painSignals) {
        String promise = firstUseful(job.promiseSummary(), "");
        if (promise.isBlank() || painSignals == 0) {
            return List.of();
        }
        return List.of(promise);
    }

    /**
     * Resume risco de saturação somente quando há evidência de objeção suficiente.
     */
    private String buildSaturationRisk(int objectionSignals) {
        if (objectionSignals >= 3) {
            return "Risco moderado de desconfiança ou saturação indicado por sinais públicos; exigir ângulo e prova mais específicos.";
        }
        return null;
    }

    /**
     * Monta hipótese executiva somente com base nos sinais públicos disponíveis.
     */
    private String buildOpportunityRecommendation(MarketWarmupTemperature temperature, int authoritySignals, int socialProofSignals, int channelSignals) {
        List<String> observed = new ArrayList<>();
        if (authoritySignals > 0) {
            observed.add("autoridade ou marca pública");
        }
        if (channelSignals > 0) {
            observed.add("canal público de audiência");
        }
        if (socialProofSignals > 0) {
            observed.add("prova social");
        }
        if (observed.isEmpty()) {
            return null;
        }
        String strength = switch (temperature) {
            case HOT -> "forte";
            case PROMISING -> "promissora";
            case WARM -> "parcial";
            case SATURATED -> "com risco de saturação ou desconfiança";
            case COLD -> "fraca";
        };
        return "A pesquisa pública encontrou evidência "
                + strength
                + " de "
                + String.join(", ", observed)
                + ". A conclusão final deve cruzar esses sinais com a análise do modelo e com as fontes listadas abaixo.";
    }

    /**
     * Resume alavancas somente quando os sinais públicos sustentam a leitura.
     */
    private List<String> buildSuccessLevers(int authoritySignals, int socialProofSignals, int competitorSignals, int channelSignals) {
        List<String> levers = new ArrayList<>();
        if (authoritySignals > 0) {
            levers.add("Sinais públicos apontam autoridade pessoal ou marca especialista como hipótese a validar.");
        }
        if (channelSignals > 0) {
            levers.add("Sinais públicos apontam canais de audiência ou aquecimento como hipótese a validar.");
        }
        if (socialProofSignals > 0) {
            levers.add("Sinais públicos apontam depoimentos, resultados ou prova social como hipótese a validar.");
        }
        if (competitorSignals > 0) {
            levers.add("Sinais públicos apontam oferta, afiliados, marketplace ou bônus como hipótese a validar.");
        }
        return levers;
    }

    /**
     * Sugere a próxima solicitação de pesquisa sem transformar lacuna em conclusão.
     */
    private String buildNextInvestigationSuggestion(int authoritySignals, int socialProofSignals, int channelSignals) {
        if (authoritySignals == 0) {
            return "Solicitar ao modelo novas buscas por pessoa pública, fundador, especialista, creator ou marca por trás do produto.";
        }
        if (channelSignals == 0) {
            return "Solicitar ao modelo novas buscas por canais de aquisição: Instagram, YouTube, TikTok, WhatsApp, lives, afiliados ou anúncios.";
        }
        if (socialProofSignals == 0) {
            return "Solicitar ao modelo novas buscas por depoimentos, resultados, reviews e provas usadas para converter a audiência em compra.";
        }
        return "Solicitar ao modelo consolidação final cruzando autoridade, canais, prova social, oferta e fontes públicas rastreáveis.";
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
        if (containsAny(text, "instagram", "tiktok", "post", "perfil")) {
            return MarketWarmupSourceType.SOCIAL_POST;
        }
        if (containsAny(text, "youtube", "canal", "live", "aula gratuita", "webinar")) {
            return MarketWarmupSourceType.CREATOR_CONTENT;
        }
        if (containsAny(text, "fundador", "fundadora", "especialista", "professora", "mentor", "autoridade")) {
            return MarketWarmupSourceType.SPECIALIST_CONTENT;
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
        return containsAny(text, "coment", "pergunta", "review", "depoimento", "preço", "seguidores", "alunas", "whatsapp", "live") ? BigDecimal.valueOf(7) : BigDecimal.valueOf(3);
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
