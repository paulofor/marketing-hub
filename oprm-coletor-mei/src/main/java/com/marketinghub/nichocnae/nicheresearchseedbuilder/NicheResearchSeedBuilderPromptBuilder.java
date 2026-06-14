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
        prompt.add("Objetivo: usar o CNAE amplo apenas como fonte inicial de descoberta, quebrar esse CNAE em 3 a 7 subnichos operacionais mais vendáveis, escolher o melhor e transformar apenas esse subnicho específico em pesquisas sobre o profissional brasileiro MEI/autônomo que executa o trabalho, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
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
        if (hasText(input.previousQualityStatus()) || hasText(input.previousNextMoveCode()) || hasText(input.previousLearningNotes())) {
            prompt.add("");
            prompt.add("Aprendizado automático do ciclo anterior:");
            prompt.add("previousQualityStatus: " + safe(input.previousQualityStatus()));
            prompt.add("previousNextMoveCode: " + safe(input.previousNextMoveCode()));
            prompt.add("previousNextMove: " + safe(input.previousNextMove()));
            prompt.add("previousLearningNotes: " + safe(input.previousLearningNotes()));
            prompt.add("Use esse aprendizado como restrição obrigatória: não repita a mesma causa de reprovação; altere o subnicho, as famílias de queries e a estratégia de fontes para executar o próximo movimento indicado.");
        }
        prompt.add("");
        prompt.add("Orientações operacionais:");
        prompt.add("1. Responda JSON válido aderente ao schema estrutural solicitado.");
        prompt.add("2. Leia CNAE, volume MEI, score OPRM, descrição e nome amplo apenas como matéria-prima; não crie nem materialize o nicho amplo.");
        prompt.add("3. Gere mentalmente 3 a 7 subnichos operacionais focados em executor MEI/autônomo, como manicure autônoma com agenda via WhatsApp, cabeleireira que atende em domicílio, profissional de beleza iniciante buscando primeiros clientes e manicure/pedicure que quer aumentar recorrência e pacotes mensais quando fizer sentido para o CNAE.");
        prompt.add("4. Pontue cada subnicho de 1 a 5 por recorrência, urgência da dor, capacidade de pagar, clareza do resultado e compatibilidade com produto digital; escolha como seed apenas o subnicho com maior potencial de venda futura.");
        prompt.add("5. O campo seed.nicheName deve ser o subnicho específico vencedor, nunca o CNAE amplo nem uma simples variação plural/singular da descrição CNAE; use uma formulação com público, contexto operacional e dor/resultado observável.");
        prompt.add("6. Execute um pré-gate comercial antes de gerar queries profundas: só escolha o subnicho se houver sinais mínimos de recorrência, urgência da dor, capacidade de pagar, clareza do resultado, compatibilidade com produto digital e possibilidade de evidência pública.");
        prompt.add("7. Se nenhum subnicho passar no pré-gate comercial, escolha o mais promissor e deixe claro em initialAssumptions quais critérios ainda precisam ser validados nas primeiras queries, sem inventar oferta.");
        prompt.add("8. initialAssumptions deve resumir os subnichos avaliados, a pontuação comparativa e o motivo da escolha com base nos critérios comerciais, citando explicitamente recorrência, urgência da dor, capacidade de pagar, clareza do resultado e compatibilidade com produto digital, sem virar oferta.");
        prompt.add("9. Gere um seed que descreva o profissional pesquisado e o contexto operacional do subnicho vencedor sem transformar a pesquisa em oferta.");
        prompt.add("10. Gere queries suficientes em português do Brasil para orientar as próximas etapas de busca, coleta e extração de sinais apenas do subnicho vencedor.");
        prompt.add("11. Priorize as primeiras queries para validar demanda comercial antes da pesquisa profunda: recorrência, dor urgente, pagamento/cobrança/preço, resultado desejado, aquisição/fidelização e evidência pública.");
        prompt.add("12. Depois dessa validação comercial inicial, gere queries para descobrir rotina manual, tarefas do dia a dia, atendimento real e linguagem usada pelo executor, antes de dores genéricas ou temas comerciais.");
        prompt.add("13. Inclua variações práticas com termos como: o que faz no dia a dia, rotina de trabalho, tarefas diárias, procedimentos, atendimento cliente, higiene, esterilização, CBO, guia profissional e relato de profissional.");
        prompt.add("14. Exemplos de formato para adaptar ao nicho: o que faz uma manicure no dia a dia; rotina de trabalho manicure pedicure atendimento cliente; tarefas diárias cabeleireiro salão; procedimentos manicure pedicure higiene esterilização atendimento; cabeleireiro lavar cortar escovar colorir rotina profissional.");
        prompt.add("15. Prefira frases de busca sobre clientes, atendimento real, aquisição, fidelização, recorrência, cobrança, agenda, materiais, entrega, retrabalho, dores práticas, dores emocionais, sonhos, medos, inseguranças, canais usados e linguagem real do próprio profissional.");
        prompt.add("16. Gere famílias de queries explícitas: aquisição de clientes; faltas, remarcações e clientes que somem; precificação, cobrança, pacotes e recorrência; materiais, tempo de atendimento e retrabalho; relatos reais em fóruns, vídeos, comentários e perguntas frequentes.");
        prompt.add("17. Exemplos obrigatórios adaptáveis: manicure clientes pelo WhatsApp indicação Instagram; manicure cliente falta remarca some; manicure preço pacote cobrança sinal recorrência; manicure material tempo atendimento retrabalho; relatos manicure autônoma comentários dúvidas frequentes.");
        prompt.add("18. Use priority menor para queries de pré-gate comercial, rotina executada, tarefas diárias, procedimentos concretos e relatos de profissionais; use CBO, tabelas salariais e páginas institucionais com prioridade menor, apenas como apoio secundário.");
        prompt.add("19. Evite que a pesquisa dependa demais de CBO, tabelas salariais, páginas institucionais ou descrições oficiais; procure relatos, perguntas e linguagem real do executor.");
        prompt.add("20. A etapa confia no modelo: não force marcador literal em toda query quando a intenção de pesquisa estiver clara.");
        prompt.add("21. Respeite os limites maxLength definidos no JSON Schema, especialmente queryGoal curto e queryText como frase de busca objetiva.");
        prompt.add("22. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        prompt.add("23. Se houver aprendizado anterior REFAZER_BUSCA_SEM_SOLUCAO, exclua termos de solução e priorize relatos manuais; se BUSCAR_FONTES_BRASILEIRAS_RECENTES, gere queries com recorte Brasil e últimos 24 meses; se VALIDAR_DOR_VENDAVEL, priorize perda financeira, recorrência, cobrança, preço, sinal, agenda vazia e tentativa de resolver; se VALIDAR_AQUISICAO_CANAIS, priorize WhatsApp, Instagram, indicação, retorno, fidelização e cliente que some; se TROCAR_PARA_DONO_OPERADOR, troque o foco para executor MEI/autônomo e não empresa estruturada.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }

    /** Informa se um texto opcional tem conteúdo útil para entrar no prompt. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
