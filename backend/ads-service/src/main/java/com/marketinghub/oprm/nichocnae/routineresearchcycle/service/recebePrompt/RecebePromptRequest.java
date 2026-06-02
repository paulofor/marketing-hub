package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.recebePrompt;

/** Contrato para registrar o prompt enviado à IA quando a etapa passar a usar processamento inteligente. */
public record RecebePromptRequest(
    String prompt, String promptMarkdownContent, String schemaJson, String requestBodyJson, String jobidopenai) {}
