package com.marketinghub.nichocnaev3.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        assertEquals(expectedStages(), stages);
    }

    /** Confirma que toda etapa v3 cadastrada tem endpoint pending e processor executável pelo scheduler único. */
    @Test
    void shouldRegisterBackendPathAndProcessorForEveryVersionThreeStage() {
        List<NichoCnaeV3StageDefinition> stages = new NichoCnaeV3StageDefinitions().all();

        assertEquals(expectedStages().size(), stages.size());
        for (NichoCnaeV3StageDefinition stage : stages) {
            assertEquals(
                    "/api/internal/oprm/nichocnae/v3/" + stage.stageCode() + "/stage-executions",
                    stage.backendPath());
            assertNotNull(stage.processor());
            assertFalse(stage.stageCode().isBlank());
        }
    }

    /** Lista canônica das etapas v3 executadas pela varredura agendada. */
    private static List<String> expectedStages() {
        return List.of(
                "cnae-intake",
                "persona-candidate-generator",
                "persona-tournament",
                "routine-query-planner",
                "source-searcher",
                "source-fetcher",
                "routine-signal-extractor",
                "daily-tasks-synthesizer",
                "quality-gate",
                "persona-routine-materializer");
    }
}
