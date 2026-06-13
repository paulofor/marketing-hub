package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/** Monta o prompt da etapa dois mantendo a fronteira OPRM de conhecer o nicho sem criar oferta comercial. */
@Component
public class NicheResearchSeedBuilderPromptBuilder {

    /** Cria instruções para transformar CNAE em seed e queries de rotina com aquisição como eixo operacional. */
    public String buildPrompt(NicheResearchSeedBuilderPending input) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Você é um especialista em Marketing e Comportamento do Consumidor no Digital, focado em entender a operação comercial real do profissional brasileiro MEI/autônomo antes de qualquer oferta.");
        prompt.add("Objetivo: transformar o CNAE em pesquisas comerciais e operacionais sobre o profissional brasileiro MEI/autônomo que executa o trabalho, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
        prompt.add("Gere seed operacional e frases de pesquisa sobre rotina executada, aquisição de clientes, atendimento, agenda, faltas, remarcações, cobrança, precificação, pacotes, recorrência, materiais, tempo de atendimento, retrabalho, dores, linguagem real e canais usados em pt-BR.");
        prompt.add("As queries devem cobrir obrigatoriamente cinco famílias: aquisição por WhatsApp/Instagram/indicação; agenda, faltas, remarcações e clientes que somem; precificação, cobrança, pacotes e recorrência; materiais, tempo de atendimento e retrabalho; relatos reais em fóruns, vídeos, comentários e perguntas frequentes.");
        prompt.add("A aquisição de clientes deve aparecer como eixo obrigatório da realidade operacional do profissional: busque evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, Instagram, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência.");
        prompt.add("Trate aquisição, preço e cobrança como comportamento operacional observado no trabalho real do MEI/autônomo, não como recomendação de marketing, criação de campanha, funil, anúncio, oferta, promessa ou estratégia de venda.");
        prompt.add("Evite que a pesquisa dependa demais de CBO, tabelas salariais, páginas institucionais ou descrições oficiais; use esses termos apenas como apoio secundário quando ajudarem a entender contexto, nunca como eixo dominante das queries.");
        prompt.add("Não transforme essas queries em aconselhamento de marketing, criação de campanha, funil, anúncio, oferta, promessa ou estratégia de venda.");
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
        prompt.add("4. Priorize as primeiras queries para descobrir como o profissional consegue clientes, organiza agenda, cobra, precifica, compra materiais, lida com faltas e evita retrabalho.");
        prompt.add("5. Inclua variações práticas com termos como: WhatsApp, Instagram, indicação, cliente sumiu, falta, remarcar horário, cobrar sinal, preço, pacote, mensalidade, material, tempo de atendimento, retrabalho, fórum, vídeo, comentário, reclamação e perguntas frequentes.");
        prompt.add("6. Exemplos de formato para adaptar ao nicho: manicure clientes pelo WhatsApp indicação Instagram; manicure cliente falta remarca some; manicure preço pacote cobrança sinal recorrência; manicure material tempo atendimento retrabalho; relatos manicure autônoma comentários dúvidas frequentes.");
        prompt.add("7. Prefira frases de busca que revelem operação comercial vivida: clientes, atendimento, cobrança, agenda, materiais, entrega, retrabalho, sonhos, medos, inseguranças, canais usados e linguagem real.");
        prompt.add("8. Use priority menor para queries comerciais/operacionais com WhatsApp, Instagram, indicação, agenda, faltas, preço, cobrança, pacotes, materiais, retrabalho e relatos reais; deixe CBO, tabelas salariais e páginas institucionais com prioridade menor apenas quando forem necessárias como apoio.");
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
