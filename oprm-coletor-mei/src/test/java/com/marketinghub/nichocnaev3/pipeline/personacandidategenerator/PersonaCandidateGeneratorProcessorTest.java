package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a geração funcional de personas candidatas no pipeline NichoCNAE v3. */
class PersonaCandidateGeneratorProcessorTest {
    private final PersonaCandidateGeneratorProcessor processor = new PersonaCandidateGeneratorProcessor();

    /** Garante que a etapa retorna personas reais, dores, tarefas e avanço para o torneio. */
    @Test
    void shouldGenerateStructuredCandidatePersonasForCnae() {
        StageResult result = processor.process(new StageContext(
                "job-4781400",
                "72",
                Map.of("cnaeCode", "4781400", "cnaeDescription", "Comércio varejista de artigos do vestuário")));

        assertThat(result.status()).isEqualTo("PERSONAS_CANDIDATAS");
        assertThat(result.output()).containsEntry("nextStageCode", "persona-tournament");
        assertThat(result.output()).containsEntry("cnaeDescription", "Comércio varejista de artigos do vestuário");
        assertThat(result.output()).containsEntry("personaCount", 4);
        assertThat(result.output().get("personaSummary").toString()).contains("Dono(a) operador(a)");
        List<?> personas = (List<?>) result.output().get("candidatePersonas");
        assertThat(personas).hasSize(4);
        Map<?, ?> firstPersona = (Map<?, ?>) personas.getFirst();
        assertThat(firstPersona.get("name")).isEqualTo("Dono(a) operador(a) de loja de vestuário");
        assertThat((List<String>) firstPersona.get("operationalPains")).contains("falta de tempo");
        assertThat((List<String>) firstPersona.get("dailyTasks")).contains("atender clientes");
        assertThat((List<String>) firstPersona.get("buyingSignals")).contains("procura atalhos práticos");
    }

    /** Garante fallback útil quando a descrição do CNAE ainda não veio no payload de entrada. */
    @Test
    void shouldResolveKnownCnaeDescriptionWhenInputOnlyHasCode() {
        StageResult result = processor.process(new StageContext("job-1", "72", Map.of("cnaeCode", "4781400")));

        assertThat(result.output()).containsEntry("cnaeDescription", "Comércio varejista de artigos do vestuário");
        assertThat(result.output().get("routineSummary").toString()).contains("loja de vestuário");
    }
}
