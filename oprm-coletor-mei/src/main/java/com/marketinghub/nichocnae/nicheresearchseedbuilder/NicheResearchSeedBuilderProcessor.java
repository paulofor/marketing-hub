package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa dois usando IA para gerar o seed do nicho e as queries de pesquisa auditáveis. */
@Component
public class NicheResearchSeedBuilderProcessor implements StageProcessor<NicheResearchSeedBuilderPending, NicheResearchSeedBuilderOutput> {
    private final OpenAiNicheResearchSeedBuilderClient openAiClient;
    private final NicheResearchSeedBuilderBackendClient backendClient;

    /** Inicializa o processor com a integração de IA e a borda backend específica da etapa dois. */
    public NicheResearchSeedBuilderProcessor(
            OpenAiNicheResearchSeedBuilderClient openAiClient,
            NicheResearchSeedBuilderBackendClient backendClient) {
        this.openAiClient = openAiClient;
        this.backendClient = backendClient;
    }

    /** Gera, valida e persiste no backend o seed e as queries da etapa dois do ciclo informado. */
    @Override
    public StageResult<NicheResearchSeedBuilderOutput> process(StageContext<NicheResearchSeedBuilderPending> context) {
        NicheResearchSeedBuilderPending input = context.input();
        OpenAiSeedBuilderResult generated = openAiClient.generate(input);
        NicheResearchSeedBuilderOutput output = backendClient.completeStageExecution(generated);
        Map<String, Object> metrics = Map.of(
                "researchCycleId", output.researchCycleId(),
                "queryCount", output.queries() == null ? 0 : output.queries().size(),
                "model", generated.model());
        return new StageResult<>(output, List.of(), metrics);
    }
}
