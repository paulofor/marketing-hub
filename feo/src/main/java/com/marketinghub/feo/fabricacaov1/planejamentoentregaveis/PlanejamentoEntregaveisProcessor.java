package com.marketinghub.feo.fabricacaov1.planejamentoentregaveis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableSpec;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Planeja entregaveis digitais profissionais a partir da oferta validada.
 */
@Component
public class PlanejamentoEntregaveisProcessor implements StageProcessor<FabricationContext, DeliverablePlan> {

    private static final List<String> REQUIRED_COMPONENTS = List.of(
            "COMECE_AQUI",
            "DIAGNOSTICO_GUIADO",
            "MISSOES_7_DIAS",
            "PAINEL_PROGRESSO",
            "PLANO_EXECUCAO_RAPIDA",
            "CHECKLIST_APLICACAO",
            "TEMPLATES_PRONTOS",
            "EXEMPLO_PREENCHIDO",
            "PROVA_TANGIVEL",
            "BIBLIOTECA_APOIO",
            "RITUAL_ACOMPANHAMENTO",
            "BONUS_ANTI_OBJECAO",
            "GUIA_PRIMEIROS_RESULTADOS");

    private final ObjectMapper objectMapper;

    /**
     * Recebe serializador para publicar plano auditavel.
     */
    public PlanejamentoEntregaveisProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna a etapa canonica de planejamento dos entregaveis.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.PLANEJAMENTO_ENTREGAVEIS;
    }

    /**
     * Cria um plano de pacote final sem alterar promessa, nicho ou mecanismo validados.
     */
    @Override
    public StageResult<DeliverablePlan> process(StageContext<FabricationContext> context) {
        FabricationContext input = context.input();
        List<String> missing = requiredMissing(input);
        if (!missing.isEmpty()) {
            return StageResult.blocked("Contexto FEO incompleto: " + String.join(", ", missing), List.of());
        }

        DeliverablePlan plan = new DeliverablePlan(
                input.requestId(),
                "PDE - Produto Digital Experiencial - " + input.offerName(),
                buildSpecs(input));
        StageArtifact artifact = context.artifactStore().store(
                "FEO_OFFER_DELIVERABLE_PLAN",
                "feo-offer-deliverable-plan.json",
                "application/json",
                toJson(plan));
        return StageResult.completedWithNext(
                plan,
                List.of(artifact),
                Map.of("deliverableCount", plan.deliverables().size(), "qualityGate", "PLAN_READY"),
                StageCode.REDACAO_ENTREGAVEIS);
    }

    /**
     * Verifica campos obrigatorios para preservar a promessa validada.
     */
    private List<String> requiredMissing(FabricationContext input) {
        List<String> missing = new ArrayList<>();
        if (isBlank(input.offerName())) {
            missing.add("offerName");
        }
        if (isBlank(input.centralPromise())) {
            missing.add("centralPromise");
        }
        if (isBlank(input.promisedResult())) {
            missing.add("promisedResult");
        }
        if (isBlank(input.coreMechanism())) {
            missing.add("coreMechanism");
        }
        if (input.deliverables() == null || input.deliverables().isEmpty()) {
            missing.add("deliverables");
        }
        return missing;
    }

    /**
     * Monta especificacoes de entregaveis com formatos de produto final.
     */
    private List<DeliverableSpec> buildSpecs(FabricationContext input) {
        List<DeliverableSpec> specs = new ArrayList<>();
        for (String component : REQUIRED_COMPONENTS) {
            int index = specs.size() + 1;
            specs.add(specFor(input, component, index));
        }
        return specs;
    }

    /**
     * Cria uma especificacao obrigatoria do Produto Digital Experiencial.
     */
    private DeliverableSpec specFor(FabricationContext input, String componentType, int index) {
        String code = "KIT-" + String.format("%02d", index);
        return new DeliverableSpec(
                code,
                titleFor(componentType, input),
                componentType,
                formatFor(componentType),
                roleFor(componentType, input),
                String.valueOf(index),
                qualityCriteriaFor(componentType, input),
                sectionsFor(componentType));
    }

    /**
     * Define titulo comercial para cada componente obrigatorio do produto.
     */
    private String titleFor(String componentType, FabricationContext input) {
        return switch (componentType) {
            case "COMECE_AQUI" -> "Leia antes de abrir o armário";
            case "DIAGNOSTICO_GUIADO" -> "Espelho MUSA - o que hoje apaga sua presença";
            case "MISSOES_7_DIAS" -> "7 dias para ficar mais marcante com o que você já tem";
            case "PAINEL_PROGRESSO" -> "Seu antes e depois de presença";
            case "PLANO_EXECUCAO_RAPIDA" -> "Plano de 7 dias sem compra impulsiva";
            case "CHECKLIST_APLICACAO" -> "Checklist de presença em 12 minutos";
            case "TEMPLATES_PRONTOS" -> "Cartões de decisão para roupa, beleza e compras";
            case "EXEMPLO_PREENCHIDO" -> "Exemplo realista de uma semana MUSA";
            case "PROVA_TANGIVEL" -> "Antes e depois: de arrumada para memorável";
            case "BIBLIOTECA_APOIO" -> "Biblioteca MUSA de consulta rápida";
            case "RITUAL_ACOMPANHAMENTO" -> "Ritual semanal para manter sua assinatura";
            case "BONUS_ANTI_OBJECAO" -> "Quando bater dúvida, use este atalho";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Como perceber que sua presença mudou";
            default -> "Material complementar - " + input.offerName();
        };
    }

    /**
     * Define formato final esperado para cada componente do kit.
     */
    private String formatFor(String componentType) {
        return switch (componentType) {
            case "DIAGNOSTICO_GUIADO", "MISSOES_7_DIAS", "PAINEL_PROGRESSO" -> "EXPERIENCIA_GUIADA";
            case "BIBLIOTECA_APOIO" -> "BIBLIOTECA_DIGITAL";
            case "CHECKLIST_APLICACAO", "TEMPLATES_PRONTOS", "BONUS_ANTI_OBJECAO" -> "HTML_CSV_PREENCHIVEL";
            case "EXEMPLO_PREENCHIDO", "PROVA_TANGIVEL" -> "HTML_PDF_AMOSTRA";
            case "RITUAL_ACOMPANHAMENTO" -> "HTML_CALENDARIO";
            default -> "HTML_PDF";
        };
    }

    /**
     * Define o papel comercial de cada componente no produto final.
     */
    private String roleFor(String componentType, FabricationContext input) {
        return switch (componentType) {
            case "COMECE_AQUI" -> "Faz você começar sem se perder entre arquivos, missões e vontade de mudar tudo de uma vez.";
            case "DIAGNOSTICO_GUIADO" -> "Mostra por que você às vezes sai arrumada, mas ainda não se sente marcante.";
            case "MISSOES_7_DIAS" -> "Conduz uma pequena mudança por dia para criar presença sem gastar mais do que precisa.";
            case "PAINEL_PROGRESSO" -> "Ajuda você a enxergar o que mudou no espelho, na escolha e na sensação de entrar em um lugar.";
            case "PLANO_EXECUCAO_RAPIDA" -> "Entrega o caminho principal para chegar mais perto de " + input.promisedResult() + ".";
            case "CHECKLIST_APLICACAO" -> "Tira dúvida na hora de sair e evita excesso, pressa e combinação sem intenção.";
            case "TEMPLATES_PRONTOS" -> "Dá frases e campos simples para decidir o que usar, repetir, ajustar ou deixar para depois.";
            case "EXEMPLO_PREENCHIDO" -> "Mostra como uma mulher real pode sair do quase bom para uma presença mais coerente.";
            case "PROVA_TANGIVEL" -> "Transforma a promessa em comparação visível, sem depender de luxo ou mudança radical.";
            case "BIBLIOTECA_APOIO" -> "Reúne os materiais de consulta para quando você quiser revisar sem voltar à estaca zero.";
            case "RITUAL_ACOMPANHAMENTO" -> "Cria um ritmo leve para manter sua assinatura mesmo em semana corrida.";
            case "BONUS_ANTI_OBJECAO" -> "Destrava os momentos em que você pensa que não tem roupa, tempo, dinheiro ou criatividade.";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Ajuda você a reconhecer os sinais sutis de que sua presença ficou mais intencional.";
            default -> "Aumenta profundidade percebida sem criar uma promessa nova.";
        };
    }

    /**
     * Define criterios de qualidade inspirados em paginas quentes da biblioteca.
     */
    private List<String> qualityCriteriaFor(String componentType, FabricationContext input) {
        return List.of(
                "Mantém a promessa pública do produto: " + input.centralPromise(),
                "Reduz esforço de aplicação na rotina real",
                "Entrega material pronto para usar, preencher ou revisar",
                "Inclui prova, exemplo ou sinal visível de progresso",
                "Não cria promessa automática nem promessa nova");
    }

    /**
     * Define secoes obrigatorias para materializar cada componente.
     */
    private List<String> sectionsFor(String componentType) {
        return switch (componentType) {
            case "DIAGNOSTICO_GUIADO" -> List.of("Perguntas iniciais", "Ponto de partida", "Prioridade de ajuste", "Sinal de progresso", "Primeira missao");
            case "MISSOES_7_DIAS" -> List.of("Dia 1", "Dia 2", "Dias 3 a 5", "Dias 6 e 7", "Fechamento da jornada");
            case "PAINEL_PROGRESSO" -> List.of("Checklist de avanço", "Evidência registrada", "Obstáculo", "Próximo microajuste", "Conclusão");
            case "PLANO_EXECUCAO_RAPIDA" -> List.of("Dia 1", "Dia 2", "Dias 3 a 5", "Dias 6 e 7", "Criterio de conclusao");
            case "TEMPLATES_PRONTOS" -> List.of("Campos editaveis", "Modelo base", "Como preencher", "Exemplo de uso", "Quando reutilizar");
            case "PROVA_TANGIVEL" -> List.of("Estado antes", "Estado depois", "Miniresultado", "Sinais de progresso", "Limites da prova");
            case "RITUAL_ACOMPANHAMENTO" -> List.of("Ritmo diario", "Checkpoint", "Lembrete", "Revisao", "Continuidade");
            case "BONUS_ANTI_OBJECAO" -> List.of("Trava", "Resposta simples", "Atalho", "Perguntas frequentes", "Proxima acao");
            case "BIBLIOTECA_APOIO" -> List.of("E-book", "Checklists", "Templates", "Exemplos", "Como consultar sem travar");
            default -> List.of("Objetivo", "Quando usar", "Passo a passo", "Modelo preenchivel", "Criterio de conclusao");
        };
    }

    /**
     * Serializa plano como JSON auditavel.
     */
    private byte[] toJson(DeliverablePlan plan) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(plan);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar plano FEO", ex);
        }
    }

    /**
     * Indica se um texto obrigatorio esta ausente.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
