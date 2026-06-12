package com.marketinghub.worker.pipeline.hypothesisproof;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a resposta estruturada da etapa Prova da hipótese. */
class HypothesisProofResponseValidatorTest {
    private final HypothesisProofResponseValidator validator = new HypothesisProofResponseValidator(new ObjectMapper());

    /** Deve aceitar JSON válido com os campos essenciais da etapa Prova. */
    @Test
    void shouldParseValidProofResponse() {
        String json = """
                {
                  "proofType":"Mini diagnóstico operacional",
                  "proofAsset":"Checklist de gargalos da agenda",
                  "proofMessage":"Mostra onde a rotina perde tempo antes da compra.",
                  "evidenceSignals":["gargalo identificado", "tempo recuperável"],
                  "collectionMethod":"Aplicar perguntas simples sobre agenda e retrabalho.",
                  "credibilityRationale":"A prova usa dados da própria rotina do cliente.",
                  "objectionReduced":"Reduz a dúvida de que o método serve para a rotina real.",
                  "boundaryConditions":"Não promete agenda cheia nem resultado automático.",
                  "summary":"Prova prática para tangibilizar o mecanismo antes da oferta."
                }
                """;

        HypothesisProofOutput output = validator.validateAndParse(json);

        assertThat(output.proofType()).isEqualTo("Mini diagnóstico operacional");
        assertThat(output.evidenceSignals()).contains("gargalo identificado");
    }

    /** Deve rejeitar JSON sem campos obrigatórios para evitar prova fraca ou vazia. */
    @Test
    void shouldRejectMissingProofMessage() {
        String json = """
                {
                  "proofType":"Mini diagnóstico operacional",
                  "proofAsset":"Checklist de gargalos da agenda",
                  "proofMessage":"",
                  "evidenceSignals":["gargalo identificado"],
                  "collectionMethod":"Aplicar perguntas simples.",
                  "credibilityRationale":"Usa dados da rotina.",
                  "objectionReduced":"Reduz dúvida.",
                  "boundaryConditions":"Sem promessa absoluta.",
                  "summary":"Prova prática."
                }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(StageWorkerException.class)
                .hasMessageContaining("Resposta inválida da etapa Prova");
    }
}
