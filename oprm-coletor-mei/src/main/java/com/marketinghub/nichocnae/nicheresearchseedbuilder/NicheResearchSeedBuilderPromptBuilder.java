package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/** Monta o prompt da etapa dois mantendo a fronteira OPRM de conhecer o nicho sem criar oferta comercial. */
@Component
public class NicheResearchSeedBuilderPromptBuilder {

    /** Cria instruções objetivas para a IA transformar CNAE em seed operacional e queries não genéricas. */
    public String buildPrompt(NicheResearchSeedBuilderPending input) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Você é o construtor da etapa 2 do pipeline OPRM nichocnae.");
        prompt.add("Objetivo: pesquisar a rotina real do nicho CNAE no mercado brasileiro, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
        prompt.add("Gere apenas seed operacional e frases de pesquisa sobre dia a dia, tarefas, dificuldades, decisões, atendimento, agenda, materiais, clientes, retrabalho, perdas, sazonalidade, cobrança, comunicação e linguagem do público brasileiro.");
        prompt.add("Não proponha solução. Não procure produto. Não procure oferta. Não procure ferramenta. Não procure campanha ou landing page.");
        prompt.add("");
        prompt.add("Dados do ciclo:");
        prompt.add("researchCycleId: " + input.researchCycleId());
        prompt.add("cnaeCode: " + safe(input.cnaeCode()));
        prompt.add("cnaeDescription: " + safe(input.cnaeDescription()));
        prompt.add("nicheName: " + safe(input.nicheName()));
        prompt.add("sourceScore: " + input.sourceScore());
        prompt.add("");
        prompt.add("Regras obrigatórias:");
        prompt.add("1. Responda somente JSON válido aderente ao schema solicitado.");
        prompt.add("2. Gere um seed que responda quem é o nicho pesquisado no Brasil sem transformar a pesquisa em oferta.");
        prompt.add("3. Gere de 12 a 15 queries em português do Brasil, cada uma em linha lógica própria no array queries.");
        prompt.add("4. Cada query deve conter o nicho, CNAE, tarefa, ator, local, material, cliente ou dificuldade operacional específica e um marcador Brasil/brasileiro/estado/cidade quando fizer sentido.");
        prompt.add("5. Não gere query genérica como 'como vender mais'.");
        prompt.add("6. Cubra somente rotina, tarefas, dificuldades operacionais, perguntas do profissional, perguntas do cliente final, linguagem orgânica e contexto operacional.");
        prompt.add("7. Não use termos de solução quando eles não fizerem parte literal da descrição CNAE: IA, inteligência artificial, automação, software, sistema, app, ferramenta, curso, template, oferta ou landing page.");
        prompt.add("8. Todas as queries devem sair com status PENDING e createdBy AI.");
        prompt.add("9. Use queryGoal somente entre ROUTINE_DISCOVERY, ROUTINE_TASK_DISCOVERY, OPERATIONAL_DIFFICULTY_DISCOVERY, NICHE_OWNER_QUESTION_DISCOVERY, FINAL_CUSTOMER_QUESTION_DISCOVERY, LANGUAGE_DISCOVERY e OPERATIONAL_CONTEXT_DISCOVERY.");
        prompt.add("10. Priorize termos de busca que tragam fontes do Brasil: domínios .br, órgãos brasileiros, entidades setoriais brasileiras, fóruns brasileiros, notícias brasileiras e páginas em pt-BR.");
        prompt.add("11. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }
}
