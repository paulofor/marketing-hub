package com.marketinghub.scientificresearch.productevidence.v1.evidencesynthesis;

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
 * Usa IA para transformar fontes científicas em síntese comercial responsável.
 */
@Component
public class EvidenceSynthesisProcessor implements StageProcessor {

    private final ScientificEvidenceOpenAiClient openAiClient;

    /**
     * Recebe o client de IA da etapa.
     */
    public EvidenceSynthesisProcessor(ScientificEvidenceOpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    /**
     * Retorna o código canônico da etapa de síntese científica.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.EVIDENCE_SYNTHESIS;
    }

    /**
     * Gera a síntese científica e bloqueia alegações sem sustentação suficiente.
     */
    @Override
    public StageResult process(StageContext context) {
        EvidenceSynthesisOutput synthesis = openAiClient.synthesize(context);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("synthesis", synthesis);

        List<StageArtifact> artifacts = List.of(
                new StageArtifact("scientific-evidence-synthesis.json", "application/json", synthesis),
                new StageArtifact("scientific-evidence-request.json", "application/json", openAiClient.lastRequest()),
                new StageArtifact("scientific-evidence-response.json", "application/json", openAiClient.lastResponse()));

        if (!synthesis.approvedForProductClaim()) {
            return StageResult.blocked(
                    output,
                    artifacts,
                    "A evidência encontrada não sustenta a promessa do produto com segurança.",
                    "O experimento pode vender uma explicação falsa, reduzindo confiança e aumentando risco regulatório.",
                    "Reformular a promessa para uma orientação educacional limitada ao que os artigos realmente indicam.");
        }
        return StageResult.completed(output, artifacts, StageCode.DELIVERABLE_COMPOSER);
    }
}
