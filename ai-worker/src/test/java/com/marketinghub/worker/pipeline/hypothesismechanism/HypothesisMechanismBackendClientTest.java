package com.marketinghub.worker.pipeline.hypothesismechanism;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a montagem do contexto de prompt da etapa Mecanismo da hipótese. */
class HypothesisMechanismBackendClientTest {

    /** Garante que o contexto da etapa Mecanismo carregue Dor e Resultado concluídos. */
    @Test
    void shouldIncludePainAndResultResponsesInPromptData() {
        HypothesisMechanismBackendClient client = newClient();
        Map<String, Object> niche = new LinkedHashMap<>();
        niche.put("name", "Cabeleireiros, manicure e pedicure");
        niche.put("description", "Rotina com agenda, atendimento e retrabalho.");
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("marketNicheId", 18L);
        pending.put("niche", niche);
        pending.put("painModelResponse", "Dor: perda de agenda por falhas operacionais.");
        pending.put("resultModelResponse", "Resultado: agenda previsível com menos retrabalho.");

        Map<String, Object> promptData = client.buildPromptDataFromPending(pending);

        assertThat(promptData).containsEntry("painModelResponse", "Dor: perda de agenda por falhas operacionais.");
        assertThat(promptData).containsEntry("resultModelResponse", "Resultado: agenda previsível com menos retrabalho.");
        assertThat(promptData.get("CASE_DATA_BLOCK")).asString()
                .contains("painModelResponse: Dor: perda de agenda por falhas operacionais.")
                .contains("resultModelResponse: Resultado: agenda previsível com menos retrabalho.");
    }

    /** Cria o cliente de backend apenas com dependências necessárias para montagem local do contexto. */
    private HypothesisMechanismBackendClient newClient() {
        HypothesisMechanismWorkerProperties properties = new HypothesisMechanismWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-mechanism.md",
                "prompts/hypothesis-pipeline/hypothesis-mechanism-schema.json",
                "hypothesis_pipeline_mechanism",
                "gpt-5.5",
                "default",
                Duration.ofMinutes(30));
        return new HypothesisMechanismBackendClient(WebClient.builder(), properties, new ObjectMapper());
    }
}
