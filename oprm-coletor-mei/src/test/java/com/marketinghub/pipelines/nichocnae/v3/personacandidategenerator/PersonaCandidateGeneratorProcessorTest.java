package com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a geração funcional de personas candidatas no pipeline NichoCNAE v3. */
class PersonaCandidateGeneratorProcessorTest {
    /** Garante que a etapa usa o cliente gerador e completa campos técnicos canônicos. */
    @Test
    void shouldGenerateStructuredCandidatePersonasWithOpenAiClient() {
        CapturingClient client = new CapturingClient(personaPayload());
        PersonaCandidateGeneratorProcessor processor = new PersonaCandidateGeneratorProcessor(client);

        StageResult result = processor.process(new StageContext(
                "job-4781400",
                "72",
                Map.of("cnaeCode", "4781400", "cnaeDescription", "Comércio varejista de artigos do vestuário")));

        assertThat(client.request.cnaeCode()).isEqualTo("4781400");
        assertThat(client.request.cnaeDescription()).isEqualTo("Comércio varejista de artigos do vestuário");
        assertThat(result.status()).isEqualTo("PERSONAS_CANDIDATAS");
        assertThat(result.output()).containsEntry("nextStageCode", "persona-tournament");
        assertThat(result.output()).containsEntry("personaCount", 3);
        assertThat(result.output().get("personaSummary").toString()).contains("Dono operador");
        List<?> personas = (List<?>) result.output().get("candidatePersonas");
        assertThat(personas).hasSize(3);
        Map<?, ?> firstPersona = (Map<?, ?>) personas.getFirst();
        assertThat(firstPersona.get("name")).isEqualTo("Dono operador de loja de vestuário");
    }

    /** Garante fallback útil quando a descrição do CNAE ainda não veio no payload de entrada. */
    @Test
    void shouldResolveKnownCnaeDescriptionBeforeCallingOpenAi() {
        CapturingClient client = new CapturingClient(personaPayload());
        PersonaCandidateGeneratorProcessor processor = new PersonaCandidateGeneratorProcessor(client);

        processor.process(new StageContext("job-1", "72", Map.of("cnaeCode", "4781400")));

        assertThat(client.request.cnaeDescription()).isEqualTo("Comércio varejista de artigos do vestuário");
    }

    /** Bloqueia conclusão quando a OpenAI não retorna personas suficientes. */
    @Test
    void shouldFailWhenOpenAiDoesNotReturnEnoughPersonas() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidatePersonas", List.of(Map.of("name", "única")));
        PersonaCandidateGeneratorProcessor processor = new PersonaCandidateGeneratorProcessor(new CapturingClient(payload));

        assertThatThrownBy(() -> processor.process(new StageContext("job-1", "72", Map.of("cnaeCode", "4781400"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("personas candidatas suficientes");
    }

    /** Monta payload funcional semelhante ao retorno estruturado da OpenAI. */
    private Map<String, Object> personaPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("personaSummary", "Dono operador; vendedor digital; operação familiar");
        payload.put("candidatePersonas", List.of(
                Map.of("name", "Dono operador de loja de vestuário", "operationalPains", List.of("falta de tempo"), "dailyTasks", List.of("atender clientes"), "buyingSignals", List.of("procura atalhos")),
                Map.of("name", "Vendedor digital de vestuário", "operationalPains", List.of("mensagens repetidas"), "dailyTasks", List.of("responder dúvidas"), "buyingSignals", List.of("busca modelo pronto")),
                Map.of("name", "Operação familiar de vestuário", "operationalPains", List.of("delegação informal"), "dailyTasks", List.of("conferir estoque"), "buyingSignals", List.of("quer previsibilidade"))));
        return payload;
    }

    /** Cliente fake que captura a requisição enviada pelo processor. */
    private static final class CapturingClient implements PersonaCandidateGenerationClient {
        private final Map<String, Object> payload;
        private PersonaCandidateGenerationRequest request;

        /** Inicializa o fake com o payload que simula retorno da OpenAI. */
        private CapturingClient(Map<String, Object> payload) {
            this.payload = payload;
        }

        /** Captura a requisição e retorna payload controlado pelo teste. */
        @Override
        public Map<String, Object> generate(PersonaCandidateGenerationRequest request) {
            this.request = request;
            return payload;
        }
    }
}
