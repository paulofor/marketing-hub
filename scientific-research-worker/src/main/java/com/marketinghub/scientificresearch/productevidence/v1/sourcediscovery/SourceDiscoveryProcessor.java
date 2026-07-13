package com.marketinghub.scientificresearch.productevidence.v1.sourcediscovery;

import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageArtifact;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageCode;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageContext;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageProcessor;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Executa a descoberta de artigos científicos em fontes públicas qualificadas.
 */
@Component
public class SourceDiscoveryProcessor implements StageProcessor {

    private final ScientificSourceSearchClient searchClient;

    /**
     * Recebe o client responsável por buscar fontes científicas.
     */
    public SourceDiscoveryProcessor(ScientificSourceSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /**
     * Retorna o código canônico da etapa de descoberta de fontes.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.SOURCE_DISCOVERY;
    }

    /**
     * Busca fontes e bloqueia o fluxo quando não há base científica mínima.
     */
    @Override
    public StageResult process(StageContext context) {
        String query = buildQuery(context);
        SourceDiscoveryOutput output = searchClient.search(query);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", output.query());
        payload.put("sources", output.sources());
        payload.put("rejectedReasons", output.rejectedReasons());

        List<StageArtifact> artifacts = List.of(new StageArtifact(
                "scientific-source-candidates.json",
                "application/json",
                output));

        if (output.sources().size() < 3) {
            return StageResult.blocked(
                    payload,
                    artifacts,
                    "Base científica insuficiente para sustentar a proposta do produto.",
                    "Risco de promessa falsa ou fraca para venda do experimento " + context.experimentCode() + ".",
                    "Refinar a pergunta científica ou trocar a promessa central antes de gerar entregáveis.");
        }
        return StageResult.completed(payload, artifacts, StageCode.EVIDENCE_SYNTHESIS);
    }

    /**
     * Monta a consulta científica a partir da ideia e pergunta do produto.
     */
    private String buildQuery(StageContext context) {
        if (context.scientificQuestion() != null && !context.scientificQuestion().isBlank()) {
            return context.scientificQuestion();
        }
        return context.productIdea() + " scientific evidence mechanism";
    }
}
