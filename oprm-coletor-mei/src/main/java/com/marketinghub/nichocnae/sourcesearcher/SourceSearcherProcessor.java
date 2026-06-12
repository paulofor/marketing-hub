package com.marketinghub.nichocnae.sourcesearcher;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa três executando queries em provedor público e persistindo fontes candidatas no backend. */
@Component
public class SourceSearcherProcessor implements StageProcessor<SourceSearcherPending, SourceSearcherOutput> {
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
        List<SourceSearchResult> searchResults = searchProvider.search(input.queryText(), MAX_RESULTS_PER_QUERY).stream()
                .map(sourceIntentClassifier::classify)
                .sorted(Comparator.comparing(SourceSearchResult::commercialPageRisk)
                        .thenComparing(SourceSearchResult::structuredBusinessDriftRisk)
                        .thenComparing(Comparator.comparing(SourceSearchResult::routineEvidenceScore).reversed())
                        .thenComparing(Comparator.comparing(SourceSearchResult::autonomousProfessionalEvidenceScore).reversed())
                        .thenComparing(Comparator.comparing(SourceSearchResult::brazilRelevanceScore).reversed())
                        .thenComparing(SourceSearchResult::outdatedSourceRisk)
                        .thenComparing(Comparator.comparing(SourceSearchResult::sourceFreshnessScore).reversed())
                        .thenComparing(SourceSearchResult::searchPosition))
                .toList();
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
}
