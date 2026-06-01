package com.marketinghub.oprmcoletormei.opportunity.service;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmNicheCandidateRequestDto;
import java.text.Normalizer;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Responsável por transformar scores de CNAE em sinais concretos de rotina, dor, mecanismo, prova e oferta para candidatos de nicho.
 */
@Component
public class OprmCnaeRoutineSignalBuilder {

    /** Monta o enriquecimento operacional de um CNAE com sinais de rotina e candidato de nicho pronto para decisão humana. */
    public OprmCnaeEnrichmentRequestDto buildEnrichment(OprmCnaeOpportunityScoreResponseDto score, String cycleId) {
        RoutineArchetype archetype = classify(score.cnaeDescription());
        String persona = buildPersona(score, archetype);
        String routine = buildRoutineSignals(score, archetype);
        String pain = buildPainSignals(archetype);
        String outcome = buildDesiredOutcome(archetype);
        String mechanism = buildMechanismSignals(archetype);
        String proof = buildProofSignals(score, archetype);
        String offer = buildOfferSignals(score, archetype);
        String marketSignals = buildMarketSignals(score, archetype);
        String sourceSummary = buildSourceSummary(score, archetype);
        OprmNicheCandidateRequestDto candidate = new OprmNicheCandidateRequestDto(
                score.cnaeCode(),
                score.cnaeDescription(),
                buildCandidateNicheName(score, archetype),
                persona,
                pain,
                outcome,
                mechanism,
                proof,
                offer,
                marketSignals,
                score.opportunityScore(),
                score.cycleId(),
                cycleId,
                "ENRICHED",
                "enrichmentCycleId=" + cycleId + "; scoreCycleId=" + score.cycleId() + "; source=oprm-cnae-routine-signal-builder-v2");
        return new OprmCnaeEnrichmentRequestDto(
                score.cnaeCode(),
                cycleId,
                routine,
                pain,
                mechanism,
                proof,
                offer,
                sourceSummary,
                List.of(candidate));
    }

    /** Classifica o CNAE em arquétipos de rotina usados para enriquecer sinais comerciais de forma determinística. */
    private RoutineArchetype classify(String description) {
        String text = normalize(description);
        if (containsAny(text, "cabeleireiro", "manicure", "pedicure", "beleza", "estetica", "cosmetico")) {
            return new RoutineArchetype(
                    "beleza e bem-estar local",
                    "agenda presencial com recorrência por relacionamento",
                    "agenda, confirmação de horários, atendimento, venda complementar, pós-atendimento e reativação de clientes",
                    "faltas na agenda, dependência de indicação, dificuldade de vender pacotes e baixa previsibilidade de retorno",
                    "agenda mais cheia, clientes retornando com frequência e oferta de pacote clara sem aumentar o esforço diário",
                    "roteiro de agenda com IA, mensagens prontas de WhatsApp, calendário de reativação e kit de ofertas de manutenção",
                    "comparar taxa de retorno, ocupação da agenda e vendas de pacotes antes e depois do kit",
                    "Kit Agenda Cheia com IA para profissionais de beleza");
        }
        if (containsAny(text, "educacao", "ensino", "treinamento", "curso", "instrutor", "professor")) {
            return new RoutineArchetype(
                    "educação e treinamento prático",
                    "captação de alunos e entrega recorrente de conhecimento",
                    "prospecção, diagnóstico do aluno, preparação de aula, acompanhamento, cobrança e renovação",
                    "dificuldade de diferenciar a promessa, manter alunos engajados e vender turmas recorrentes",
                    "mais matrículas qualificadas, maior permanência e trilha de aprendizado simples de executar",
                    "mapa de transformação do aluno, plano de aulas com IA, mensagens de matrícula e checklists de evolução",
                    "validar por matrículas geradas, presença nas aulas e depoimentos de evolução dos alunos",
                    "Playbook Turmas Recorrentes com IA");
        }
        if (containsAny(text, "restaurante", "lanchonete", "alimentacao", "alimentos", "bebidas", "refeicao", "doces", "salgados")) {
            return new RoutineArchetype(
                    "alimentação local",
                    "produção diária com demanda sensível a horário e recompra",
                    "planejamento de cardápio, compra de insumos, produção, atendimento, delivery, divulgação diária e controle de sobras",
                    "oscilação de pedidos, desperdício, cardápio pouco claro e dificuldade de criar recompra sem promoção agressiva",
                    "vendas mais previsíveis por horário, cardápio enxuto e campanhas simples de recompra",
                    "calendário de ofertas com IA, combos por ocasião, mensagens de cardápio e rotina de pós-venda para recompra",
                    "validar por pedidos por faixa de horário, ticket médio, recompra e redução de sobras",
                    "Kit Cardápio que Vende Todo Dia com IA");
        }
        if (containsAny(text, "obra", "construcao", "instalacao", "manutencao", "reparacao", "eletric", "hidraul", "pintura")) {
            return new RoutineArchetype(
                    "serviços técnicos em campo",
                    "orçamento sob demanda com deslocamento e execução por projeto",
                    "receber pedido, diagnosticar problema, estimar material, fazer orçamento, executar, cobrar e pedir indicação",
                    "perda de tempo em orçamento improdutivo, dificuldade de explicar valor e ausência de processo de indicação",
                    "mais orçamentos aprovados, menos visitas perdidas e indicações estruturadas depois da entrega",
                    "diagnóstico guiado por IA, calculadora simples de orçamento, scripts de objeção e sequência de indicação",
                    "validar por taxa de aprovação de orçamento, tempo até fechamento e indicações por serviço concluído",
                    "Kit Orçamento Aprovado com IA para serviços técnicos");
        }
        if (containsAny(text, "comercio", "varejista", "loja", "mercadoria", "vestuario", "calcados", "acessorios", "equipamentos")) {
            return new RoutineArchetype(
                    "comércio varejista de nicho",
                    "venda por estoque, atendimento e campanhas de giro",
                    "compra de estoque, exposição, atendimento, divulgação, negociação, pagamento, entrega e reativação de compradores",
                    "estoque parado, comunicação genérica, baixa recompra e dependência de movimento espontâneo",
                    "giro de estoque com campanhas simples, comunicação por ocasião de compra e recompra mais frequente",
                    "calendário comercial com IA, descrições de produto, combos de giro e mensagens segmentadas para clientes antigos",
                    "validar por giro de itens priorizados, ticket médio e compradores reativados",
                    "Kit Estoque em Vendas com IA");
        }
        return new RoutineArchetype(
                "serviço local de pequeno negócio",
                "operação enxuta com venda, entrega e relacionamento concentrados no dono",
                "prospecção, atendimento inicial, diagnóstico, proposta, entrega, cobrança, pós-venda e reativação",
                "falta de processo comercial simples, mensagens improvisadas e dificuldade de transformar entrega em recompra",
                "mais clareza de oferta, aquisição previsível e rotina comercial leve para vender sem depender só de indicação",
                "playbook de rotina com IA, diagnóstico de cliente, scripts de WhatsApp, calendário de ações e checklist de prova",
                "validar por leads respondidos, propostas enviadas, fechamentos e depoimentos obtidos",
                "Playbook Venda Mais com IA para pequenos negócios");
    }

    /** Monta a persona operacional do candidato a partir do CNAE e do arquétipo de rotina. */
    private String buildPersona(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return "MEI ou pequeno negócio de " + score.cnaeDescription() + " atuando em " + archetype.marketContext() + ", com rotina de " + archetype.routineModel() + ".";
    }

    /** Monta sinais de rotina observáveis para orientar pesquisa posterior, oferta e validação comercial. */
    private String buildRoutineSignals(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return "CNAE " + score.cnaeCode() + " - " + score.cnaeDescription() + ": rotina principal envolve " + archetype.routineSteps() + ". Contexto operacional: " + archetype.routineModel() + ".";
    }

    /** Monta sinais de dor conectados à rotina concreta do arquétipo. */
    private String buildPainSignals(RoutineArchetype archetype) {
        return "Dor principal: " + archetype.painSignals() + ". Impacto comercial: mais esforço para vender, menor previsibilidade e perda de oportunidades de recompra.";
    }

    /** Monta o resultado desejado de negócio do candidato de nicho. */
    private String buildDesiredOutcome(RoutineArchetype archetype) {
        return "Resultado desejado: " + archetype.desiredOutcome() + ".";
    }

    /** Monta o mecanismo digital plausível para reduzir esforço e aumentar vendas do nicho. */
    private String buildMechanismSignals(RoutineArchetype archetype) {
        return "Mecanismo: " + archetype.mechanism() + ". O produto deve reduzir decisão manual, padronizar mensagens e transformar rotina repetitiva em ações comerciais executáveis.";
    }

    /** Monta direção de prova usando métricas simples antes/depois aderentes à rotina do CNAE. */
    private String buildProofSignals(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return "Prova recomendada: " + archetype.proofDirection() + ". Score OPRM atual=" + score.opportunityScore() + ", calculado no ciclo=" + score.cycleId() + ".";
    }

    /** Monta ideia de oferta digital alinhada ao arquétipo e ao CNAE específico. */
    private String buildOfferSignals(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return archetype.offerIdea() + " para " + score.cnaeDescription() + ", com templates, checklists, prompts e calendário de execução.";
    }

    /** Monta nome comercial inicial do candidato de nicho para decisão humana. */
    private String buildCandidateNicheName(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return archetype.offerIdea() + " - " + score.cnaeDescription();
    }

    /** Monta sinais de volume e score para rastrear por que o CNAE foi priorizado. */
    private String buildMarketSignals(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return "opportunityScore=" + score.opportunityScore()
                + "; marketVolumeScore=" + score.marketVolumeScore()
                + "; meiDensityScore=" + score.meiDensityScore()
                + "; digitalFitScore=" + score.digitalFitScore()
                + "; painClarityScore=" + score.painClarityScore()
                + "; archetype=" + archetype.marketContext()
                + "; algorithmVersion=" + score.algorithmVersion();
    }

    /** Monta resumo de fontes e limites do enriquecimento sem simular pesquisa externa ainda não executada. */
    private String buildSourceSummary(OprmCnaeOpportunityScoreResponseDto score, RoutineArchetype archetype) {
        return "Enriquecimento OPRM baseado na descrição oficial do CNAE, scores persistidos e matriz determinística de rotinas por arquétipo. Arquétipo="
                + archetype.marketContext() + "; cnaeCode=" + score.cnaeCode() + ".";
    }

    /** Verifica se algum termo operacional aparece no texto normalizado. */
    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** Normaliza descrição de CNAE para comparação determinística de palavras-chave. */
    private String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Estrutura interna com sinais de rotina usados pelo builder de enriquecimento. */
    private record RoutineArchetype(
            String marketContext,
            String routineModel,
            String routineSteps,
            String painSignals,
            String desiredOutcome,
            String mechanism,
            String proofDirection,
            String offerIdea) {}
}
