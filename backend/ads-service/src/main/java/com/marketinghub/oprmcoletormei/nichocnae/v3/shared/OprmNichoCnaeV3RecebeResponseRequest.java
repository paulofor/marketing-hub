package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import java.math.BigDecimal;

/** Payload recebido do módulo executor para registrar auditoria de response da etapa NichoCNAE v3. */
public record OprmNichoCnaeV3RecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
