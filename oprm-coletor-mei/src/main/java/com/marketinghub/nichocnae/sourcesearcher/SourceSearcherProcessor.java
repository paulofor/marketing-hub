package com.marketinghub.nichocnae.sourcesearcher;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Processa a etapa três executando queries em provedor público e persistindo fontes candidatas no backend. */
@Component
public class SourceSearcherProcessor implements StageProcessor<SourceSearcherPending, SourceSearcherOutput> {
    private static final Logger log = LoggerFactory.getLogger(SourceSearcherProcessor.class);
    private static final int MAX_RESULTS_PER_QUERY = 20;

    private final PublicSourceSearchProvider searchProvider;
    private final SourceSearcherBackendClient backendClient;
    private final SourceIntentClassifier sourceIntentClassifier;

    /** Inicializa o processor com busca pública, classificador de intenção e borda backend da etapa três. */
    public SourceSearcherProcessor(
            PublicSourceSearchProvider searchProvider,
            SourceSearcherBackendClient backendClient,
            SourceIntentClassifier sourceIntentClassifier) {
        this.searchProvider = searchProvider;
        this.backendClient = backendClient;
        this.sourceIntentClassifier = sourceIntentClassifier;
    }

    /** Executa uma query pendente, classifica a intenção das fontes e conclui a etapa três no backend. */
    @Override
    public StageResult<SourceSearcherOutput> process(StageContext<SourceSearcherPending> context) {
        SourceSearcherPending input = context.input();
        long startedAt = System.nanoTime();
        log.info(
                "Chamando provedor de busca da etapa três OPRM nichocnae (researchQueryId={}, researchCycleId={}, provider={}, maxResults={}, queryText={})",
                input.researchQueryId(),
                input.researchCycleId(),
                searchProvider.providerCode(),
                MAX_RESULTS_PER_QUERY,
                input.queryText());
        List<SourceSearchResult> searchResults = searchProvider.search(input.queryText(), MAX_RESULTS_PER_QUERY).stream()
                .map(sourceIntentClassifier::classify)
                .sorted(Comparator.comparing(SourceSearchResult::commercialPageRisk)
                        .thenComparing(SourceSearchResult::solutionLanguageRisk)
                        .thenComparing(SourceSearchResult::structuredBusinessDriftRisk)
                        .thenComparing(Comparator.comparing(this::realWorkEvidenceSelectionScore).reversed())
                        .thenComparing(Comparator.comparing(SourceSearchResult::routineEvidenceScore).reversed())
                        .thenComparing(Comparator.comparing(SourceSearchResult::autonomousProfessionalEvidenceScore).reversed())
                        .thenComparing(Comparator.comparing(SourceSearchResult::brazilRelevanceScore).reversed())
                        .thenComparing(SourceSearchResult::outdatedSourceRisk)
                        .thenComparing(Comparator.comparing(SourceSearchResult::sourceFreshnessScore).reversed())
                        .thenComparing(SourceSearchResult::searchPosition))
                .toList();
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info(
                "Provedor de busca da etapa três OPRM nichocnae retornou resultados (researchQueryId={}, researchCycleId={}, provider={}, resultCount={}, durationMs={}, commercialRiskCount={}, outdatedRiskCount={}, structuredBusinessDriftRiskCount={})",
                input.researchQueryId(),
                input.researchCycleId(),
                searchProvider.providerCode(),
                searchResults.size(),
                durationMs,
                searchResults.stream().filter(SourceSearchResult::commercialPageRisk).count(),
                searchResults.stream().filter(SourceSearchResult::outdatedSourceRisk).count(),
                searchResults.stream().filter(SourceSearchResult::structuredBusinessDriftRisk).count());
        SourceSearcherOutput output =
                backendClient.completeStageExecution(input, searchProvider.providerCode(), searchResults);
        Map<String, Object> metrics = Map.of(
                "researchQueryId", output.researchQueryId(),
                "researchCycleId", output.researchCycleId(),
                "resultCount", output.resultCount() == null ? 0 : output.resultCount(),
                "searchProvider", searchProvider.providerCode(),
                "commercialRiskCount", searchResults.stream().filter(SourceSearchResult::commercialPageRisk).count(),
                "outdatedRiskCount", searchResults.stream().filter(SourceSearchResult::outdatedSourceRisk).count(),
                "structuredBusinessDriftRiskCount", searchResults.stream().filter(SourceSearchResult::structuredBusinessDriftRisk).count());
        return new StageResult<>(output, List.of(), metrics);
    }

    /** Calcula prioridade de seleção para fontes com rotina manual, atendimento real e linguagem do executor. */
    private int realWorkEvidenceSelectionScore(SourceSearchResult result) {
        String text = (safe(result.sourceTitle()) + " " + safe(result.sourceSnippet())).toLowerCase();
        int score = 0;
        score += contains(text, "rotina manual") ? 20 : 0;
        score += contains(text, "atendimento real") || contains(text, "atendimento cliente") ? 18 : 0;
        score += contains(text, "fidelização") || contains(text, "recorrência") || contains(text, "indicação") ? 16 : 0;
        score += contains(text, "dor") || contains(text, "medo") || contains(text, "insegurança") || contains(text, "frustração") ? 14 : 0;
        score += contains(text, "relato") || contains(text, "minha rotina") || contains(text, "profissional relata") ? 14 : 0;
        return score;
    }

    /** Verifica presença literal de um termo em texto já normalizado. */
    private boolean contains(String text, String term) {
        return text.contains(term);
    }

    /** Normaliza nulos para comparação local de priorização. */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
