package com.marketinghub.nichocnae.sourcesearcher;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa três executando queries em provedor público e persistindo fontes candidatas no backend. */
@Component
public class SourceSearcherProcessor implements StageProcessor<SourceSearcherPending, SourceSearcherOutput> {
    private static final int MAX_RESULTS_PER_QUERY = 20;

    private final PublicSourceSearchProvider searchProvider;
    private final SourceSearcherBackendClient backendClient;

    /** Inicializa o processor com o provedor de busca pública e a borda backend da etapa três. */
    public SourceSearcherProcessor(PublicSourceSearchProvider searchProvider, SourceSearcherBackendClient backendClient) {
        this.searchProvider = searchProvider;
        this.backendClient = backendClient;
    }

    /** Executa uma query pendente, normaliza os resultados e conclui a etapa três no backend. */
    @Override
    public StageResult<SourceSearcherOutput> process(StageContext<SourceSearcherPending> context) {
        SourceSearcherPending input = context.input();
        List<SourceSearchResult> searchResults = searchProvider.search(input.queryText(), MAX_RESULTS_PER_QUERY);
        SourceSearcherOutput output = backendClient.completeStageExecution(input, searchProvider.providerCode(), searchResults);
        Map<String, Object> metrics = Map.of(
                "researchQueryId", output.researchQueryId(),
                "researchCycleId", output.researchCycleId(),
                "resultCount", output.resultCount() == null ? 0 : output.resultCount(),
                "searchProvider", searchProvider.providerCode());
        return new StageResult<>(output, List.of(), metrics);
    }
}
