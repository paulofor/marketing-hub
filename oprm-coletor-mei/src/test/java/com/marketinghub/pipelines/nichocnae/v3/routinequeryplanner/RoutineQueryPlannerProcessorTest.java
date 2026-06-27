package com.marketinghub.pipelines.nichocnae.v3.routinequeryplanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o planejamento útil de buscas da rotina no pipeline NichoCNAE v3. */
class RoutineQueryPlannerProcessorTest {
    /** Garante que a saída da etapa quatro contenha buscas acionáveis para validar rotina, dor e esforço real. */
    @Test
    void shouldCreateUsefulSearchPlanFromWinnerPersona() {
        RoutineQueryPlannerProcessor processor = new RoutineQueryPlannerProcessor();

        StageResult result = processor.process(new StageContext("job-4781400", "137", Map.of(
                "winningPersonaName", "Gerente de loja em rede pequena",
                "winnerPersona", Map.of(
                        "name", "Gerente de loja em rede pequena",
                        "dailyTasks", List.of("conferir estoque", "acompanhar metas"),
                        "operationalPains", List.of("ruptura de estoque", "retrabalho em planilhas"),
                        "buyingSignals", List.of("procura sistema simples")))));

        assertThat(result.status()).isEqualTo("QUERIES_PLANEJADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-searcher");
        assertThat(result.output()).containsEntry("personaFocus", "Gerente de loja em rede pequena");
        assertThat(result.output()).containsKeys("searchObjective", "plannedQueries", "validationQuestions", "sourceAcceptanceCriteria", "discardCriteria");
        List<?> plannedQueries = (List<?>) result.output().get("plannedQueries");
        assertThat(plannedQueries).hasSize(5);
        assertThat(plannedQueries.getFirst()).asString().contains("conferir estoque");
        @SuppressWarnings("unchecked")
        Map<String, Object> firstQuery = (Map<String, Object>) plannedQueries.getFirst();
        assertThat(firstQuery).containsEntry("intent", "TAREFA_DIARIA");
        assertThat(firstQuery).containsKeys("query", "objective", "expectedEvidence", "priority");
    }

    /** Bloqueia uma saída genérica quando a etapa anterior não trouxe a persona vencedora. */
    @Test
    void shouldFailWithoutWinnerPersona() {
        RoutineQueryPlannerProcessor processor = new RoutineQueryPlannerProcessor();

        assertThatThrownBy(() -> processor.process(new StageContext("job-1", "137", Map.of("winningPersonaName", "persona"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("winnerPersona");
    }
}
