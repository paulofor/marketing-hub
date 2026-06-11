package com.marketinghub.worker.pipeline.hypothesisoffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contrato JSON aceito para a etapa Oferta da hipótese. */
class HypothesisOfferResponseValidatorTest {
    private final HypothesisOfferResponseValidator validator = new HypothesisOfferResponseValidator(new ObjectMapper());

    /** Valida que uma resposta com oferta plausível e passos operacionais é aceita. */
    @Test
    void validateAndParseAcceptsValidOfferJson() {
        String json = """
                {
                  "offerName": "Kit Agenda Cheia sem Retrabalho",
                  "coreOffer": "Um pacote digital com diagnóstico rápido, checklists e mensagens prontas para reduzir falhas de agenda no salão.",
                  "howItWorks": "O cliente segue um roteiro simples gerado com apoio de IA para organizar atendimentos, prevenir esquecimentos e recuperar horários críticos.",
                  "steps": [
                    "Mapear os gargalos de agenda mais frequentes do salão.",
                    "Aplicar checklists e mensagens prontas antes e depois do atendimento.",
                    "Revisar semanalmente os ajustes de rotina com um plano simples."
                  ],
                  "aiLeverage": "A IA acelera a criação dos roteiros, mensagens e planos semanais sem exigir consultoria contínua.",
                  "effortReduction": "O profissional evita criar processos do zero e passa a executar um caminho pronto.",
                  "whyBelievable": "A oferta atua sobre organização e comunicação da rotina, não promete demanda garantida.",
                  "boundaryConditions": "Depende de uso consistente e não substitui qualidade técnica, atendimento ou gestão financeira.",
                  "summary": "Oferta digital para reduzir retrabalho de agenda e tornar a rotina do salão mais previsível.",
                  "evidenceSignals": ["dor concluída", "resultado concluído", "mecanismo concluído"]
                }
                """;

        HypothesisOfferOutput output = validator.validateAndParse(json);

        assertThat(output.offerName()).isEqualTo("Kit Agenda Cheia sem Retrabalho");
        assertThat(output.steps()).hasSize(3);
    }

    /** Valida que respostas sem oferta central são rejeitadas. */
    @Test
    void validateAndParseRejectsMissingCoreOffer() {
        String json = """
                {
                  "offerName": "Kit Agenda Cheia sem Retrabalho",
                  "coreOffer": "",
                  "howItWorks": "Explica a rotina.",
                  "steps": ["Mapear gargalos", "Aplicar mensagens", "Revisar ajustes"],
                  "aiLeverage": "Gera ativos.",
                  "effortReduction": "Reduz esforço.",
                  "whyBelievable": "Atua na rotina.",
                  "boundaryConditions": "Sem garantias.",
                  "summary": "Resumo válido.",
                  "evidenceSignals": ["contexto"]
                }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(StageWorkerException.class)
                .hasMessageContaining("Resposta inválida da etapa Oferta");
    }
}
