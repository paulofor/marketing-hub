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
        prompt.add("Objetivo: transformar o CNAE em pesquisas sobre o profissional brasileiro MEI/autônomo que executa o trabalho, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.");
        prompt.add("Gere apenas seed operacional e frases de pesquisa sobre comportamento, rotina, tarefas, decisões, atendimento, agenda, materiais, clientes, cobrança, entrega, retrabalho, sonhos, medos, inseguranças, canais usados e linguagem real em pt-BR.");
        prompt.add("Não proponha solução. Não procure produto. Não procure oferta. Não procure ferramenta. Não procure campanha ou landing page. Não direcione a pesquisa para IA, automação, software, sistema, app, curso ou template.");
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
        prompt.add("2. Gere um seed que responda quem é o profissional MEI/autônomo pesquisado no Brasil sem transformar a pesquisa em oferta.");
        prompt.add("3. Gere de 12 a 15 queries em português do Brasil, cada uma em linha lógica própria no array queries.");
        prompt.add("4. Cada query deve conter o nicho ou CNAE e pelo menos um marcador explícito de pessoa pesquisada: MEI, autônomo, trabalhador por conta própria, profissional autônomo ou dono-operador.");
        prompt.add("5. Cada query deve conter marcador Brasil/brasileiro/pt-BR/estado/cidade ou buscar fonte brasileira recente quando fizer sentido.");
        prompt.add("6. Pesquise como o profissional consegue clientes, atende, cobra, agenda, compra materiais, entrega serviço e lida com retrabalho.");
        prompt.add("7. Pesquise sonhos, objetivos pessoais/profissionais, medos, inseguranças, canais usados e linguagem real em pt-BR do MEI/autônomo.");
        prompt.add("8. Não gere query genérica como 'como vender mais'.");
        prompt.add("9. Cubra somente rotina, modo de trabalho autônomo, aquisição de clientes, dificuldades, dores operacionais, dores emocionais, sonhos, medos, canais, linguagem orgânica e atualidade de fontes.");
        prompt.add("10. Não use termos de solução quando eles não fizerem parte literal da descrição CNAE: IA, inteligência artificial, automação, software, sistema, app, ferramenta, curso, template, produto, oferta, campanha ou landing page.");
        prompt.add("11. Todas as queries devem sair com status PENDING e createdBy AI.");
        prompt.add("12. Use queryGoal somente entre MEI_ROUTINE_DISCOVERY, AUTONOMOUS_WORK_MODE_DISCOVERY, CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY, DAILY_OPERATION_PAIN_DISCOVERY, EMOTIONAL_PAIN_DISCOVERY, DREAM_DISCOVERY, FEAR_DISCOVERY, CHANNEL_BEHAVIOR_DISCOVERY, LANGUAGE_DISCOVERY e SOURCE_FRESHNESS_DISCOVERY.");
        prompt.add("13. Priorize termos de busca que tragam fontes do Brasil: domínios .br, órgãos brasileiros, entidades setoriais brasileiras, fóruns brasileiros, notícias brasileiras e páginas em pt-BR recentes.");
        prompt.add("14. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }
}
