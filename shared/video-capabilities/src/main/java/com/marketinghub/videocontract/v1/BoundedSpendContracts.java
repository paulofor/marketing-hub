package com.marketinghub.videocontract.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * Responsabilidade: transportar provas da chamada limitada sem aceitar teto informado pelo worker.
 */
public final class BoundedSpendContracts {
  /** Impede instanciação de um agrupador de contratos. */
  private BoundedSpendContracts() {}

  /** Informa se o contexto exige reserva integral antes de qualquer inferência. */
  public record Context(
      boolean required,
      Long receiptId,
      Long projectId,
      String blocker,
      Lease reservation,
      String voiceModel) {}

  /** Fixa o request contado pelo provedor e o limite de saída aplicado pelo transporte real. */
  public record Reserve(JsonNode request, JsonNode inputTokenCount) {}

  /** Preserva resposta e transporte; falta de resposta nunca libera a reserva. */
  public record Result(Integer httpStatus, String rawResponse, String providerRequestId) {}

  /** Entrega somente a chamada persistida e permite um único envio por operação. */
  public record Lease(
      Long id,
      Long receiptId,
      String state,
      boolean mayInvoke,
      BigDecimal maximumUsd,
      String requestSha256,
      JsonNode request,
      String rawResponse,
      java.time.Instant quoteExpiresAt) {}
}
