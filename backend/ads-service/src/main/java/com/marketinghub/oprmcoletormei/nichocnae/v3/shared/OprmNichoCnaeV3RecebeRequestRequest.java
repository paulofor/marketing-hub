package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

/** Payload recebido do módulo executor para registrar auditoria de request da etapa NichoCNAE v3. */
public record OprmNichoCnaeV3RecebeRequestRequest(String request, String plataforma, String prompt, String schema) {
}
