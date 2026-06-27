package com.marketinghub.pipelines.nichocnae.v3.personatournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a seleção da persona vencedora no torneio do pipeline NichoCNAE v3. */
class PersonaTournamentProcessorTest {
    /** Garante que a saída contenha explicitamente o candidato vencedor para cumprir o objetivo da etapa. */
    @Test
    void shouldSelectWinnerPersonaFromCandidates() {
        PersonaTournamentProcessor processor = new PersonaTournamentProcessor();

        StageResult result = processor.process(new StageContext("job-4781400", "73", Map.of("candidatePersonas", List.of(
                Map.of("name", "Persona fraca", "operationalPains", List.of("dor"), "dailyTasks", List.of("tarefa"), "buyingSignals", List.of()),
                Map.of("name", "Dono operador de loja", "operationalPains", List.of("caixa", "estoque"), "dailyTasks", List.of("atender", "comprar", "conferir"), "buyingSignals", List.of("busca modelo pronto", "paga por facilidade"))))));

        assertThat(result.status()).isEqualTo("PERSONA_PRIORIZADA");
        assertThat(result.output()).containsEntry("nextStageCode", "routine-query-planner");
        assertThat(result.output()).containsEntry("winningPersonaName", "Dono operador de loja");
        assertThat(result.output()).containsKeys("winnerPersona", "selectionRationale", "personaRanking");
        Map<?, ?> winner = (Map<?, ?>) result.output().get("winnerPersona");
        assertThat(winner.get("tournamentScore")).isEqualTo(14);
    }

    /** Bloqueia avanço quando a etapa anterior não forneceu candidatos para o torneio. */
    @Test
    void shouldFailWithoutCandidatePersonas() {
        PersonaTournamentProcessor processor = new PersonaTournamentProcessor();

        assertThatThrownBy(() -> processor.process(new StageContext("job-1", "73", Map.of("cnaeCode", "4781400"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("candidatePersonas");
    }
}
