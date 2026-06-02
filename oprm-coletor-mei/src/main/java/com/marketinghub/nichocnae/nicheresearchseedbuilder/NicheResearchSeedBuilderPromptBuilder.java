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
        prompt.add("Objetivo: conhecer como o nicho funciona na rotina, sem criar oferta, produto, campanha ou landing page.");
        prompt.add("Use o eixo Dor → Resultado → Mecanismo → Prova → Oferta apenas como referência distante; nesta etapa gere apenas seed e frases de pesquisa.");
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
        prompt.add("2. Gere um seed que responda quem é o nicho pesquisado.");
        prompt.add("3. Gere de 12 a 15 queries, cada uma em linha lógica própria no array queries.");
        prompt.add("4. Cada query deve conter o nome do nicho ou algum objeto comercial específico do nicho.");
        prompt.add("5. Não gere query genérica como 'como vender mais'.");
        prompt.add("6. Cubra rotina, perguntas do profissional, perguntas do cliente final, dores comerciais e produtos/serviços.");
        prompt.add("7. Todas as queries devem sair com status PENDING e createdBy AI.");
        prompt.add("8. Use queryGoal somente entre ROUTINE_DISCOVERY, NICHE_OWNER_QUESTION_DISCOVERY, FINAL_CUSTOMER_QUESTION_DISCOVERY, SALES_PAIN_DISCOVERY, PRODUCT_SERVICE_DISCOVERY e OFFER_PATTERN_DISCOVERY.");
        prompt.add("9. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.");
        return prompt.toString();
    }

    /** Devolve texto seguro para evitar valores nulos no prompt enviado à IA. */
    private String safe(String value) {
        return value == null || value.isBlank() ? "não informado" : value;
    }
}
