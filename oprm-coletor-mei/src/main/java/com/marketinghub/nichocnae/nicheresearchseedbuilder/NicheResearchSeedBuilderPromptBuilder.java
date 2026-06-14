package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/** Monta o prompt da etapa dois mantendo a fronteira OPRM de conhecer o nicho sem criar oferta comercial. */
@Component
public class NicheResearchSeedBuilderPromptBuilder {

    /** Cria instruções para transformar CNAE em seed e queries de rotina com aquisição como eixo operacional. */
    public String buildPrompt(NicheResearchSeedBuilderPending input) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Você é um especialista em Marketing e Comportamento do Consumidor no Digital, focado em entender a rotina real do profissional brasileiro MEI/autônomo antes de qualquer oferta.");
        prompt.add("Objetivo: antes de gerar o seed, quebrar o CNAE amplo em 3 a 7 subnichos operacionais mais vendáveis, escolher o melhor e transformar apenas esse subnicho em pesquisas sobre o profissional brasileiro MEI/autônomo que executa o trabalho, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
        prompt.add("Gere apenas seed operacional e frases de pesquisa sobre comportamento, rotina manual executada, operação comercial real, tarefas concretas do dia a dia, atendimento real, procedimentos práticos, decisões, materiais, clientes, cobrança, entrega, retrabalho, dores práticas e emocionais, sonhos, medos, inseguranças, canais usados e linguagem real em pt-BR.");
        prompt.add("A aquisição de clientes deve aparecer como eixo obrigatório da realidade operacional do profissional: busque evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, orçamento, agenda vazia, atendimento inicial, retorno, fidelização, faltas, remarcações e clientes que somem, cancelamento, reativação, pacotes e recorrência, incluindo precificação, cobrança, pacotes e recorrência.");
        prompt.add("Trate aquisição de clientes somente como comportamento operacional observado no trabalho real do MEI/autônomo, não como recomendação de marketing, criação de campanha, funil, anúncio, oferta, promessa ou estratégia de venda.");
        prompt.add("Não transforme essas queries em aconselhamento de marketing, criação de campanha, funil, anúncio, oferta, promessa ou estratégia de venda.");
        prompt.add("Não proponha solução. Não procure produto. Não procure oferta. Não procure ferramenta. Não procure campanha ou landing page. Não direcione a pesquisa para IA, automação, software, sistema, app, curso ou template; quando esses termos aparecerem, trate como risco e gere queries alternativas focadas em execução manual e relato real.");
        prompt.add("");
        prompt.add("Dados do ciclo:");
        prompt.add("researchCycleId: " + input.researchCycleId());
        prompt.add("cnaeCode: " + safe(input.cnaeCode()));
        prompt.add("cnaeDescription: " + safe(input.cnaeDescription()));
        prompt.add("nicheName: " + safe(input.nicheName()));
        prompt.add("sourceScore: " + input.sourceScore());
        prompt.add("meiVolume: " + (input.meiVolume() == null ? "não informado" : input.meiVolume()));
        prompt.add("");
        prompt.add("Orientações operacionais:");
        prompt.add("1. Responda JSON válido aderente ao schema estrutural solicitado.");
        prompt.add("2. Leia CNAE, volume MEI, score OPRM, descrição e nome do nicho; gere mentalmente 3 a 7 subnichos operacionais focados em executor MEI/autônomo, como manicure autônoma com agenda via WhatsApp, cabeleireira que atende em domicílio, profissional de beleza iniciante buscando primeiros clientes e manicure/pedicure que quer aumentar recorrência e pacotes mensais quando fizer sentido para o CNAE.");
        prompt.add("3. Pontue cada subnicho de 1 a 5 por recorrência, urgência da dor, capacidade de pagar, clareza do resultado e compatibilidade com produto digital; escolha como seed apenas o subnicho com maior potencial de venda futura.");
        prompt.add("4. O campo seed.nicheName deve ser o subnicho vencedor, não o CNAE amplo; initialAssumptions deve resumir os subnichos avaliados, a pontuação comparativa e o motivo da escolha sem virar oferta.");
        prompt.add("5. Gere um seed que descreva o profissional pesquisado e o contexto operacional do subnicho vencedor sem transformar a pesquisa em oferta.");
        prompt.add("6. Gere queries suficientes em português do Brasil para orientar as próximas etapas de busca, coleta e extração de sinais.");
        prompt.add("7. Priorize as primeiras queries para descobrir rotina manual, tarefas do dia a dia, atendimento real e linguagem usada pelo executor, antes de dores genéricas ou temas comerciais.");
        prompt.add("8. Inclua variações práticas com termos como: o que faz no dia a dia, rotina de trabalho, tarefas diárias, procedimentos, atendimento cliente, higiene, esterilização, CBO, guia profissional e relato de profissional.");
        prompt.add("9. Exemplos de formato para adaptar ao nicho: o que faz uma manicure no dia a dia; rotina de trabalho manicure pedicure atendimento cliente; tarefas diárias cabeleireiro salão; procedimentos manicure pedicure higiene esterilização atendimento; cabeleireiro lavar cortar escovar colorir rotina profissional.");
        prompt.add("10. Prefira frases de busca sobre clientes, atendimento real, aquisição, fidelização, recorrência, cobrança, agenda, materiais, entrega, retrabalho, dores práticas, dores emocionais, sonhos, medos, inseguranças, canais usados e linguagem real do próprio profissional.");
        prompt.add("11. Gere famílias de queries explícitas: aquisição de clientes; faltas, remarcações e clientes que somem; precificação, cobrança, pacotes e recorrência; materiais, tempo de atendimento e retrabalho; relatos reais em fóruns, vídeos, comentários e perguntas frequentes.");
        prompt.add("12. Exemplos obrigatórios adaptáveis: manicure clientes pelo WhatsApp indicação Instagram; manicure cliente falta remarca some; manicure preço pacote cobrança sinal recorrência; manicure material tempo atendimento retrabalho; relatos manicure autônoma comentários dúvidas frequentes.");
        prompt.add("13. Use priority menor para queries de rotina executada, tarefas diárias, procedimentos concretos e relatos de profissionais; use CBO, tabelas salariais e páginas institucionais com prioridade menor, apenas como apoio secundário.");
        prompt.add("14. Evite que a pesquisa dependa demais de CBO, tabelas salariais, páginas institucionais ou descrições oficiais; procure relatos, perguntas e linguagem real do executor.");
        prompt.add("15. A etapa confia no modelo: não force marcador literal em toda query quando a intenção de pesquisa estiver clara.");
        prompt.add("16. Respeite os limites maxLength definidos no JSON Schema, especialmente queryGoal curto e queryText como frase de busca objetiva.");
        prompt.add("17. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }
}
