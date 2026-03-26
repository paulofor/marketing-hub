package com.marketinghub.experiment.service;

import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.dto.ExperimentPlaybookStageDto;
import com.marketinghub.experiment.dto.ExperimentPlaybookVariableDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides the canonical list of experiment stages and suggested variables to test.
 */
@Service
public class ExperimentPlaybookService {
    private final List<ExperimentPlaybookStageDto> stages;

    public ExperimentPlaybookService() {
        stages = List.of(
                new ExperimentPlaybookStageDto(
                        ExperimentStage.AD,
                        "Anúncio (Instagram/Facebook)",
                        "Validar qual ângulo abre melhor a conversa (dor, resultado, prova ou CTA).",
                        "CTR de link (%)",
                        List.of("LPV", "CPC como guardrail"),
                        List.of(
                                new ExperimentPlaybookVariableDto(
                                        "PAIN_VS_RESULT",
                                        "Dor vs Resultado",
                                        "Comparar criativos que abrem com a dor principal versus promessas explícitas de transformação.",
                                        List.of("CREATIVE_COPY"),
                                        "CTR de link (%)"),
                                new ExperimentPlaybookVariableDto(
                                        "PROOF_TEASER",
                                        "Teaser de prova",
                                        "Inserir antes/depois ou prova social já no criativo versus manter foco apenas no desejo.",
                                        List.of("CREATIVE_COPY"),
                                        "CTR de link (%)"),
                                new ExperimentPlaybookVariableDto(
                                        "CTA_PITCH",
                                        "CTA da oferta",
                                        "Testar CTA orientado a amostra personalizada versus CTA orientado a diagnóstico/agenda.",
                                        List.of("CREATIVE_COPY"),
                                        "CTR de link (%)"))),
                new ExperimentPlaybookStageDto(
                        ExperimentStage.LANDING,
                        "Landing / Formulário",
                        "Transformar o clique em intenção clara coletando só o que melhora a prova.",
                        "Taxa de envio do formulário",
                        List.of("Start rate", "Abandono do formulário"),
                        List.of(
                                new ExperimentPlaybookVariableDto(
                                        "PROOF_LAYOUT",
                                        "Formato da prova",
                                        "Comparar páginas que destacam prévias visuais versus narrativas com depoimento/roteiro.",
                                        List.of("LANDING_COPY"),
                                        "Taxa de envio do formulário"),
                                new ExperimentPlaybookVariableDto(
                                        "FORM_DEPTH",
                                        "Profundidade do formulário",
                                        "Formulário enxuto (4 perguntas) versus formulário diagnóstico com perguntas sobre posicionamento/diferencial.",
                                        List.of("LANDING_COPY"),
                                        "Taxa de envio do formulário"),
                                new ExperimentPlaybookVariableDto(
                                        "CTA_CHANNEL",
                                        "Canal de entrega",
                                        "Prometer entrega da amostra no e-mail versus WhatsApp para entender preferência do lead.",
                                        List.of("LANDING_COPY"),
                                        "Taxa de envio do formulário"))),
                new ExperimentPlaybookStageDto(
                        ExperimentStage.SAMPLE,
                        "Amostra / Prova",
                        "Converter promessa em experiência personalizada (e rastrear resposta).",
                        "Taxa de resposta ao envio",
                        List.of("Taxa de abertura", "Taxa de clique"),
                        List.of(
                                new ExperimentPlaybookVariableDto(
                                        "SAMPLE_DEPTH",
                                        "Quantidade da amostra",
                                        "Enviar 6 imagens de alto impacto versus pacote com 12 itens para medir percepção de valor.",
                                        List.of("SAMPLE_EMAIL"),
                                        "Taxa de resposta ao envio"),
                                new ExperimentPlaybookVariableDto(
                                        "MECHANISM_EXPLANATION",
                                        "Explicação do mecanismo",
                                        "Acompanha texto explicando como a IA personaliza a peça versus deixar apenas a prova visual.",
                                        List.of("SAMPLE_EMAIL"),
                                        "Taxa de resposta ao envio"),
                                new ExperimentPlaybookVariableDto(
                                        "CTA_NEXT_STEP",
                                        "Próximo passo",
                                        "CTA chamando para conversar no WhatsApp versus CTA direto para checkout/proposta.",
                                        List.of("SAMPLE_EMAIL"),
                                        "Taxa de resposta ao envio"))),
                new ExperimentPlaybookStageDto(
                        ExperimentStage.SALES,
                        "Oferta / Venda",
                        "Empacotar entregáveis e risco para fechar negócio com clareza.",
                        "Taxa de fechamento",
                        List.of("Ticket médio", "Tempo até fechamento"),
                        List.of(
                                new ExperimentPlaybookVariableDto(
                                        "PACKAGE_SIZE",
                                        "Quantidade do pacote",
                                        "Oferta com 10 imagens x 30 imagens para medir ancoragem de preço.",
                                        List.of("OFFER_COPY"),
                                        "Taxa de fechamento"),
                                new ExperimentPlaybookVariableDto(
                                        "BONUS_STACK",
                                        "Bônus / aceleração",
                                        "Adicionar bônus (calendário/editorial) versus sem bônus para entender impacto em decisão.",
                                        List.of("OFFER_COPY"),
                                        "Taxa de fechamento"),
                                new ExperimentPlaybookVariableDto(
                                        "RISK_REVERSAL",
                                        "Redução de risco",
                                        "Garantia/revisão sem custo versus urgência/slot limitado.",
                                        List.of("OFFER_COPY"),
                                        "Taxa de fechamento"))));
    }

    public List<ExperimentPlaybookStageDto> list() {
        return stages;
    }

    public ExperimentPlaybookStageDto get(ExperimentStage stage) {
        return stages.stream()
                .filter(entry -> entry.stage() == stage)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "playbook stage not found: " + stage));
    }
}
