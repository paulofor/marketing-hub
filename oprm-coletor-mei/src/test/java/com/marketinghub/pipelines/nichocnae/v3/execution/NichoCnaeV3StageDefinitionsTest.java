package com.marketinghub.pipelines.nichocnae.v3.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o catálogo completo das etapas NichoCNAE v3. */
class NichoCnaeV3StageDefinitionsTest {
    /** Confirma que a v3 nasce com o fluxo completo até materialização da rotina da persona. */
    @Test
    void shouldRegisterAllVersionThreeStagesInOrder() {
        List<String> stages = new NichoCnaeV3StageDefinitions(request -> personaPayload(), (query, limit) -> List.of()).all().stream()
                .map(NichoCnaeV3StageDefinition::stageCode)
                .toList();

        assertEquals(expectedStages(), stages);
    }

    /** Confirma que toda etapa v3 cadastrada tem endpoint pending e processor executável pelo scheduler único. */
    @Test
    void shouldRegisterBackendPathAndProcessorForEveryVersionThreeStage() {
        List<NichoCnaeV3StageDefinition> stages = new NichoCnaeV3StageDefinitions(request -> personaPayload(), (query, limit) -> List.of()).all();

        assertEquals(expectedStages().size(), stages.size());
        for (NichoCnaeV3StageDefinition stage : stages) {
            assertEquals(
                    "/api/internal/oprmcoletormei/nichocnae/v3/" + stage.stageCode() + "/stage-executions",
                    stage.backendPath());
            assertNotNull(stage.processor());
            assertFalse(stage.stageCode().isBlank());
        }
    }

    /** Cria payload mínimo de personas para o processor plugado no catálogo. */
    private static Map<String, Object> personaPayload() {
        return Map.of("candidatePersonas", List.of(
                Map.of("name", "persona"),
                Map.of("name", "persona 2"),
                Map.of("name", "persona 3")));
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
