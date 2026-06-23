package com.marketinghub.worker.pipeline.hypothesispain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a montagem do contexto de prompt da etapa Dor da hipótese. */
class HypothesisPainBackendClientTest {

    /** Garante que campos opcionais nulos do nicho virem texto vazio antes de criar o input. */
    @Test
    void shouldConvertNullOptionalNicheFieldsToEmptyText() {
        HypothesisPainBackendClient client = newClient();
        Map<String, Object> niche = new LinkedHashMap<>();
        niche.put("name", "Cabeleireiros, manicure e pedicure");
        niche.put("promises", null);
        niche.put("offers", null);
        niche.put("extraTips", "Usar rotina real");
        Map<String, Object> enrichmentProfile = new LinkedHashMap<>();
        enrichmentProfile.put("personaSummary", "Manicure autônoma em domicílio.");
        enrichmentProfile.put("languagePatterns", "agenda quebrada; cliente some");
        enrichmentProfile.put("commercialTriggers", "busca previsibilidade");
        enrichmentProfile.put("objections", "medo de parecer complicado");
        Map<String, Object> existingHypothesis = new LinkedHashMap<>();
        existingHypothesis.put("title", "CPM-H001");
        existingHypothesis.put("problem", "Agenda quebrada por remarcações.");
        existingHypothesis.put("promise", "Agenda previsível em 7 dias.");
        existingHypothesis.put("persona", "Manicure autônoma em domicílio");
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("marketNicheId", 18L);
        pending.put("niche", niche);
        pending.put("enrichmentProfile", enrichmentProfile);
        pending.put("existingHypotheses", java.util.List.of(existingHypothesis));

        Map<String, Object> promptData = client.buildPromptDataFromPending(pending);
        HypothesisPainInput input = new HypothesisPainInput(18L, "hypothesis-pain", "job-1", promptData);

        assertThat(promptData).containsEntry("promises", "");
        assertThat(promptData).containsEntry("offers", "");
        assertThat(input.promptData()).containsEntry("promises", "");
        assertThat(input.promptData()).containsEntry("offers", "");
        assertThat(promptData.get("CASE_DATA_BLOCK")).asString()
                .contains("promises: ")
                .contains("offers: ")
                .contains("extraTips: Usar rotina real")
                .contains("existingHypothesesSummary: 1) código: CPM-H001; dor: Agenda quebrada por remarcações.; promessa: Agenda previsível em 7 dias.; persona: Manicure autônoma em domicílio;")
                .contains("enrichedPersonaSummary: Manicure autônoma em domicílio.")
                .contains("enrichedLanguagePatterns: agenda quebrada; cliente some")
                .contains("enrichedCommercialTriggers: busca previsibilidade")
                .contains("enrichedObjections: medo de parecer complicado");
    }

    /** Cria o cliente de backend apenas com dependências necessárias para montagem local do contexto. */
    private HypothesisPainBackendClient newClient() {
        HypothesisPainWorkerProperties properties = new HypothesisPainWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-pain.md",
                "prompts/hypothesis-pipeline/hypothesis-pain-schema.json",
                "hypothesis_pipeline_pain",
                "gpt-5.5",
                Duration.ofMinutes(30));
        return new HypothesisPainBackendClient(WebClient.builder(), properties, new ObjectMapper());
    }
}
