package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/** Monta o prompt da etapa dois mantendo a fronteira OPRM de conhecer o nicho sem criar oferta comercial. */
@Component
public class NicheResearchSeedBuilderPromptBuilder {

    /** Cria instruções para transformar CNAE em seed e queries de rotina com aquisição como eixo operacional. */
    public String buildPrompt(NicheResearchSeedBuilderPending input) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Você é o construtor da etapa 2 do pipeline OPRM nichocnae.");
        prompt.add("Objetivo: transformar o CNAE em pesquisas sobre o profissional brasileiro MEI/autônomo que executa o trabalho, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
        prompt.add("Gere apenas seed operacional e frases de pesquisa sobre comportamento, rotina executada, tarefas do dia a dia, procedimentos práticos, decisões, atendimento, agenda, materiais, clientes, cobrança, entrega, retrabalho, sonhos, medos, inseguranças, canais usados e linguagem real em pt-BR.");
        prompt.add("Gere apenas seed operacional e frases de pesquisa sobre comportamento, rotina, tarefas, decisões, atendimento, agenda, materiais, clientes, cobrança, entrega, retrabalho, sonhos, medos, inseguranças, canais usados e linguagem real em pt-BR.");
        prompt.add("A aquisição de clientes deve aparecer como eixo obrigatório da realidade operacional do profissional: busque evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência.");
        prompt.add("Trate aquisição de clientes somente como comportamento operacional observado no trabalho real do MEI/autônomo, não como recomendação de marketing, criação de campanha, funil, anúncio, oferta, promessa ou estratégia de venda.");
        prompt.add("Não proponha solução. Não procure produto. Não procure oferta. Não procure ferramenta. Não procure campanha ou landing page. Não direcione a pesquisa para IA, automação, software, sistema, app, curso ou template.");
        prompt.add("");
        prompt.add("Dados do ciclo:");
        prompt.add("researchCycleId: " + input.researchCycleId());
        prompt.add("cnaeCode: " + safe(input.cnaeCode()));
        prompt.add("cnaeDescription: " + safe(input.cnaeDescription()));
        prompt.add("nicheName: " + safe(input.nicheName()));
        prompt.add("sourceScore: " + input.sourceScore());
        prompt.add("");
        prompt.add("Orientações operacionais:");
        prompt.add("1. Responda JSON válido aderente ao schema estrutural solicitado.");
        prompt.add("2. Gere um seed que descreva o profissional pesquisado e o contexto operacional do nicho sem transformar a pesquisa em oferta.");
        prompt.add("3. Gere queries suficientes em português do Brasil para orientar as próximas etapas de busca, coleta e extração de sinais.");
        prompt.add("4. Priorize as primeiras queries para descobrir a rotina executada e as tarefas do dia a dia do executor real, antes de dores genéricas ou temas comerciais.");
        prompt.add("5. Inclua variações práticas com termos como: o que faz no dia a dia, rotina de trabalho, tarefas diárias, procedimentos, atendimento cliente, higiene, esterilização, CBO, guia profissional e relato de profissional.");
        prompt.add("6. Exemplos de formato para adaptar ao nicho: o que faz uma manicure no dia a dia; rotina de trabalho manicure pedicure atendimento cliente; tarefas diárias cabeleireiro salão; procedimentos manicure pedicure higiene esterilização atendimento; cabeleireiro lavar cortar escovar colorir rotina profissional.");
        prompt.add("7. Prefira frases de busca sobre clientes, atendimento, cobrança, agenda, materiais, entrega, retrabalho, sonhos, medos, inseguranças, canais usados e linguagem real.");
        prompt.add("8. Use priority menor para queries de rotina executada, tarefas diárias, procedimentos concretos, CBO, guias profissionais e relatos de profissionais.");
        prompt.add("9. A etapa confia no modelo: não force marcador literal em toda query quando a intenção de pesquisa estiver clara.");
        prompt.add("10. Respeite os limites maxLength definidos no JSON Schema, especialmente queryGoal curto e queryText como frase de busca objetiva.");
        prompt.add("11. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }
}
