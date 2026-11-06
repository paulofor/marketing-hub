package com.marketinghub.videocontract.v1;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Responsabilidade: validar a previsão restante integral antes de reservar uma produção. */
public record ProductionCostPlan(
    Long cycleId,
    Long productId,
    Long experimentId,
    String capabilityRevision,
    String providerPlan,
    int finalSeconds,
    String expiresAt,
    Map<String, Item> remainingCosts,
    List<String> unresolvedAttemptIds) {
  public static final List<String> REQUIRED =
      List.of("AGENTS", "REFERENCES", "VOICE", "MUSIC", "POST_PRODUCTION", "FEES", "RETRIES");

  /** Exige fonte para cada parcela, inclusive zero explícito, e conciliação das tentativas. */
  public BigDecimal maximumRemainingUsd(Instant now) {
    if (!VideoCapabilities.catalog().revision().equals(capabilityRevision)
        || expiresAt == null
        || !now.isBefore(Instant.parse(expiresAt)))
      throw new IllegalArgumentException("Previsão integral vencida; atualizar custos e revisão");
    if (unresolvedAttemptIds == null || !unresolvedAttemptIds.isEmpty())
      throw new IllegalArgumentException(
          "Conciliar tentativas com resultado ou custo incerto antes de repetir");
    if (remainingCosts != null
        && !new java.util.HashSet<>(REQUIRED).containsAll(remainingCosts.keySet()))
      throw new IllegalArgumentException(
          "Parcela de custo não prevista no contrato; revisar cobertura antes de gerar");
    BigDecimal total = BigDecimal.ZERO;
    for (String name : REQUIRED) {
      Item item = remainingCosts == null ? null : remainingCosts.get(name);
      if (item == null
          || item.maximumUsd() == null
          || item.maximumUsd().signum() < 0
          || item.evidence() == null
          || item.evidence().isBlank())
        throw new IllegalArgumentException(
            "Custo restante desconhecido: "
                + name
                + "; obter estimativa e fonte; desconhecido não é zero");
      total = total.add(item.maximumUsd());
    }
    return total;
  }

  /** Registra o máximo restante da parcela e sua fonte, sem confundir teto com custo realizado. */
  public record Item(BigDecimal maximumUsd, String evidence) {}
}
