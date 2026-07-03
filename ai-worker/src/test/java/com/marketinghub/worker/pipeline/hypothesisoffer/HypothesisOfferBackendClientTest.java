package com.marketinghub.worker.pipeline.hypothesisoffer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a montagem do contexto de prompt da etapa Oferta da hipótese. */
class HypothesisOfferBackendClientTest {

    /** Garante que o contexto da etapa Oferta carregue Dor, Resultado, Mecanismo e Prova concluídos. */
    @Test
    void shouldIncludePainResultMechanismAndProofResponsesInPromptData() {
        HypothesisOfferBackendClient client = newClient();
        Map<String, Object> niche = new LinkedHashMap<>();
        niche.put("name", "Cabeleireiros, manicure e pedicure");
        niche.put("description", "Rotina com agenda, atendimento e retrabalho.");
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("marketNicheId", 18L);
        pending.put("niche", niche);
        pending.put("painModelResponse", "Dor: perda de agenda por falhas operacionais.");
        pending.put("resultModelResponse", "Resultado: agenda previsível com menos retrabalho.");
        pending.put("mechanismModelResponse", "Mecanismo: agenda guiada por IA com checklists simples.");
        pending.put("proofModelResponse", "Prova: mini diagnóstico demonstrando gargalos da agenda.");

        Map<String, Object> promptData = client.buildPromptDataFromPending(pending);

        assertThat(promptData).containsEntry("painModelResponse", "Dor: perda de agenda por falhas operacionais.");
        assertThat(promptData).containsEntry("resultModelResponse", "Resultado: agenda previsível com menos retrabalho.");
        assertThat(promptData).containsEntry("mechanismModelResponse", "Mecanismo: agenda guiada por IA com checklists simples.");
        assertThat(promptData).containsEntry("proofModelResponse", "Prova: mini diagnóstico demonstrando gargalos da agenda.");
        assertThat(promptData.get("CASE_DATA_BLOCK")).asString()
                .contains("painModelResponse: Dor: perda de agenda por falhas operacionais.")
                .contains("resultModelResponse: Resultado: agenda previsível com menos retrabalho.")
                .contains("mechanismModelResponse: Mecanismo: agenda guiada por IA com checklists simples.")
                .contains("proofModelResponse: Prova: mini diagnóstico demonstrando gargalos da agenda.");
    }

    /** Cria o cliente de backend apenas com dependências necessárias para montagem local do contexto. */
    private HypothesisOfferBackendClient newClient() {
        HypothesisOfferWorkerProperties properties = new HypothesisOfferWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-offer.md",
                "prompts/hypothesis-pipeline/hypothesis-offer-schema.json",
                "hypothesis_pipeline_offer",
                "gpt-5.5",
                "default",
                Duration.ofMinutes(30));
        return new HypothesisOfferBackendClient(WebClient.builder(), properties, new ObjectMapper());
    }
}
