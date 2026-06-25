package com.marketinghub.nichocnaev3.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida o catálogo completo das etapas NichoCNAE v3. */
class NichoCnaeV3StageDefinitionsTest {
    /** Confirma que a v3 nasce com o fluxo completo até materialização da rotina da persona. */
    @Test
    void shouldRegisterAllVersionThreeStagesInOrder() {
        List<String> stages = new NichoCnaeV3StageDefinitions().all().stream()
                .map(NichoCnaeV3StageDefinition::stageCode)
                .toList();

        assertEquals(List.of(
                "cnae-intake",
                "persona-candidate-generator",
                "persona-tournament",
                "routine-query-planner",
                "source-searcher",
                "source-fetcher",
                "routine-signal-extractor",
                "daily-tasks-synthesizer",
                "quality-gate",
                "persona-routine-materializer"), stages);
    }
}
