package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import com.marketinghub.pipelines.dossie.v1.dossiersynthesis.DossierDossierSynthesisOutput;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Valida os contratos HTTP montados pelo cliente do backend do dossiê MOIS v1. */
class DossierV1BackendClientTest {

    /** Garante que a saída funcional tipada seja enviada como JSON, não como record.toString(). */
    @Test
    void deveSerializarSaidaFuncionalComoJsonLimpo() {
        DossierV1BackendClient client = new DossierV1BackendClient(RestClient.builder().build(), new ObjectMapper());
        DossierDossierSynthesisOutput output = new DossierDossierSynthesisOutput(
                123L,
                "DONE",
                "priorizar promessa de economia de tempo",
                "O produto resolve dor operacional clara.",
                List.of("evidencia comercial"),
                List.of("testar criativo com prova"),
                "APPROVED",
                Map.of("source", "dossie"));

        String response = client.responseFrom(StageResult.done(Map.of("dossier-synthesis", output), List.of()));

        assertThat(response).contains("\"dossierId\":123");
        assertThat(response).contains("\"businessDecision\":\"priorizar promessa de economia de tempo\"");
        assertThat(response).doesNotContain("DossierDossierSynthesisOutput[");
    }
}
