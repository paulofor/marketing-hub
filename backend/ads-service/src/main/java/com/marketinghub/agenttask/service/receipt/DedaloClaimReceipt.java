package com.marketinghub.agenttask.service.receipt;

/**
 * Responsabilidade: confirmar reserva pequena sem transportar contexto nem autorizar inferência.
 */
public record DedaloClaimReceipt(Long taskId, String claimId, String phase, String status) {}
