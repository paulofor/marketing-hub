package com.marketinghub.worker.pipeline.hypothesismechanism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contrato JSON aceito para a etapa Mecanismo da hipótese. */
class HypothesisMechanismResponseValidatorTest {
    private final HypothesisMechanismResponseValidator validator = new HypothesisMechanismResponseValidator(new ObjectMapper());

    /** Valida que uma resposta com mecanismo plausível e passos operacionais é aceita. */
    @Test
    void validateAndParseAcceptsValidMechanismJson() {
        String json = """
                {
                  "mechanismName": "Agenda Guiada por IA",
                  "coreMechanism": "Transforma a rotina real do salão em uma sequência simples de priorização, atendimento e recuperação de agenda.",
                  "howItWorks": "A IA organiza sinais de falha operacional e sugere ações prontas para reduzir retrabalho e horários perdidos.",
                  "steps": [
                    "Mapear os gargalos de agenda mais frequentes do salão.",
                    "Gerar mensagens e checklists para prevenir falhas antes do atendimento.",
                    "Revisar a semana com recomendações simples de ajuste operacional."
                  ],
                  "aiLeverage": "A IA acelera a criação de mensagens, checklists e rotinas sem exigir consultoria manual contínua.",
                  "effortReduction": "O profissional evita começar do zero e passa a seguir um roteiro pronto de execução diária.",
                  "whyBelievable": "O mecanismo atua sobre rotina, comunicação e prevenção de falhas, não promete demanda garantida.",
                  "boundaryConditions": "Depende de uso consistente e não substitui atendimento, qualidade técnica ou gestão financeira.",
                  "summary": "Um sistema guiado por IA para reduzir falhas de agenda e tornar a rotina do salão mais previsível.",
                  "evidenceSignals": ["rotina observada", "dor concluída", "resultado concluído"]
                }
                """;

        HypothesisMechanismOutput output = validator.validateAndParse(json);

        assertThat(output.mechanismName()).isEqualTo("Agenda Guiada por IA");
        assertThat(output.steps()).hasSize(3);
    }

    /** Valida que respostas sem mecanismo central são rejeitadas. */
    @Test
    void validateAndParseRejectsMissingCoreMechanism() {
        String json = """
                {
                  "mechanismName": "Agenda Guiada por IA",
                  "coreMechanism": "",
                  "howItWorks": "Explica a rotina.",
                  "steps": ["Mapear gargalos", "Gerar checklists", "Revisar ajustes"],
                  "aiLeverage": "Gera ativos.",
                  "effortReduction": "Reduz esforço.",
                  "whyBelievable": "Atua na rotina.",
                  "boundaryConditions": "Sem garantias.",
                  "summary": "Resumo válido.",
                  "evidenceSignals": ["sinal"]
                }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(StageWorkerException.class)
                .hasMessageContaining("Resposta inválida da etapa Mecanismo");
    }
}
