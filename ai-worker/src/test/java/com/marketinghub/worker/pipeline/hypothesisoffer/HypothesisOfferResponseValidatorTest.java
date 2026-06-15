package com.marketinghub.worker.pipeline.hypothesisoffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contrato JSON aceito para a etapa Oferta da hipótese. */
class HypothesisOfferResponseValidatorTest {
    private final HypothesisOfferResponseValidator validator = new HypothesisOfferResponseValidator(new ObjectMapper());

    /** Valida que uma resposta com oferta low-ticket plausível e entregáveis operacionais é aceita. */
    @Test
    void validateAndParseAcceptsValidOfferJson() {
        String json = """
                {
                  "offerName": "Kit Agenda Cheia sem Retrabalho",
                  "offerPositioning": "Produto digital low-ticket para profissionais que precisam organizar a agenda sem contratar sistema complexo.",
                  "entryPromise": "Confirmar clientes com mais clareza e reduzir horários perdidos por falta ou desmarcação.",
                  "pricePositioning": "Faixa sugerida de R$ 27 a R$ 47, compatível com compra de baixo atrito.",
                  "coreOffer": "Um pacote digital com diagnóstico rápido, checklists e mensagens prontas para reduzir falhas de agenda no salão.",
                  "howItWorks": "O cliente segue um roteiro simples gerado com apoio de IA para organizar atendimentos, prevenir esquecimentos e recuperar horários críticos.",
                  "deliverables": [
                    "Diagnóstico rápido para mapear gargalos de agenda.",
                    "Checklist de confirmação antes do atendimento.",
                    "Banco de mensagens prontas para WhatsApp.",
                    "Modelo simples de política de remarcação e cancelamento.",
                    "Planilha simples para medir horários perdidos."
                  ],
                  "valueStack": [
                    "Diagnóstico de agenda para revelar onde o horário se perde.",
                    "Checklist de confirmação para usar antes de cada atendimento.",
                    "Mensagens prontas para reduzir conversas improvisadas.",
                    "Política leve de remarcação para comunicar regras sem atrito.",
                    "Planilha de prejuízo para visualizar dinheiro parado.",
                    "Prompts de IA para adaptar o tom das mensagens."
                  ],
                  "valuePerception": "O pacote reúne vários ativos prontos por preço baixo, aumentando a sensação de receber muito mais do que um material isolado.",
                  "quickWinAsset": "Checklist de confirmação em duas etapas que pode ser aplicado no próximo atendimento.",
                  "productionFormat": "PDF curto, planilha simples e biblioteca de mensagens copiáveis para uso imediato.",
                  "steps": [
                    "Mapear os gargalos de agenda mais frequentes do salão.",
                    "Aplicar checklists e mensagens prontas antes e depois do atendimento.",
                    "Revisar semanalmente os ajustes de rotina com um plano simples."
                  ],
                  "aiLeverage": "A IA acelera a criação dos roteiros, mensagens e planos semanais sem exigir consultoria contínua.",
                  "effortReduction": "O profissional evita criar processos do zero e passa a executar um caminho pronto.",
                  "whyBelievable": "A oferta atua sobre organização e comunicação da rotina, não promete demanda garantida.",
                  "boundaryConditions": "Depende de uso consistente e não substitui qualidade técnica, atendimento ou gestão financeira.",
                  "nextStageReadiness": "A oferta deixa claros promessa, entregáveis, vitória rápida e prova funcional para futura página de vendas e isca digital.",
                  "summary": "Oferta digital low-ticket para reduzir retrabalho de agenda e tornar a rotina do salão mais previsível.",
                  "evidenceSignals": ["dor concluída", "resultado concluído", "mecanismo concluído"]
                }
                """;

        HypothesisOfferOutput output = validator.validateAndParse(json);

        assertThat(output.offerName()).isEqualTo("Kit Agenda Cheia sem Retrabalho");
        assertThat(output.pricePositioning()).contains("R$ 27");
        assertThat(output.deliverables()).hasSize(5);
        assertThat(output.valueStack()).hasSize(6);
        assertThat(output.valuePerception()).contains("preço baixo");
        assertThat(output.steps()).hasSize(3);
    }

    /** Valida que respostas sem oferta central são rejeitadas. */
    @Test
    void validateAndParseRejectsMissingCoreOffer() {
        String json = """
                {
                  "offerName": "Kit Agenda Cheia sem Retrabalho",
                  "offerPositioning": "Produto digital low-ticket para agenda.",
                  "entryPromise": "Confirmar clientes com mais clareza.",
                  "pricePositioning": "R$ 27 a R$ 47.",
                  "coreOffer": "",
                  "howItWorks": "Explica a rotina.",
                  "deliverables": ["Diagnóstico rápido", "Checklist útil", "Mensagens prontas", "Política leve", "Planilha simples"],
                  "valueStack": ["Diagnóstico rápido", "Checklist útil", "Mensagens prontas", "Política leve", "Planilha simples", "Prompts úteis"],
                  "valuePerception": "Pacote robusto por preço baixo.",
                  "quickWinAsset": "Checklist de confirmação.",
                  "productionFormat": "PDF e mensagens copiáveis.",
                  "steps": ["Mapear gargalos", "Aplicar mensagens", "Revisar ajustes"],
                  "aiLeverage": "Gera ativos.",
                  "effortReduction": "Reduz esforço.",
                  "whyBelievable": "Atua na rotina.",
                  "boundaryConditions": "Sem garantias.",
                  "nextStageReadiness": "Pronto para próxima etapa.",
                  "summary": "Resumo válido.",
                  "evidenceSignals": ["contexto"]
                }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(StageWorkerException.class)
                .hasMessageContaining("Resposta inválida da etapa Oferta");
    }
}
