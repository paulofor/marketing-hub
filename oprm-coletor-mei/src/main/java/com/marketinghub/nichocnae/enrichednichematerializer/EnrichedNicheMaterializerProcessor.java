package com.marketinghub.nichocnae.enrichednichematerializer;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa final montando o perfil enriquecido e persistindo pelo backend. */
@Component
public class EnrichedNicheMaterializerProcessor implements StageProcessor<EnrichedNicheMaterializerPending, EnrichedNicheMaterializerOutput> {
    private final EnrichedNicheMaterializerEngine engine;
    private final EnrichedNicheMaterializerBackendClient backendClient;

    /** Inicializa o processor com o montador determinístico e a borda backend da etapa final. */
    public EnrichedNicheMaterializerProcessor(EnrichedNicheMaterializerEngine engine, EnrichedNicheMaterializerBackendClient backendClient) {
        this.engine = engine;
        this.backendClient = backendClient;
    }

    /** Materializa um nicho aprovado pelo gate alimentando nicho e nicho enriquecido no backend. */
    @Override
    public StageResult<EnrichedNicheMaterializerOutput> process(StageContext<EnrichedNicheMaterializerPending> context) {
        EnrichedNicheMaterializerPending input = context.input();
        EnrichedNicheProfileDraft draft = engine.buildDraft(input);
        EnrichedNicheMaterializerOutput output = backendClient.completeStageExecution(input, draft);
        Map<String, Object> metrics = Map.of(
                "routineCardId", output.routineCardId(),
                "researchCycleId", output.researchCycleId(),
                "marketNicheId", output.marketNicheId(),
                "enrichedNicheProfileId", output.enrichedNicheProfileId());
        return new StageResult<>(output, List.of(), metrics);
    }
}
