package com.marketinghub.pde.harness.v1;

import java.time.Instant;
import java.util.Objects;

/** Representa um fato de memória com procedência, confiança e validade explícitas. */
public record PdeMemoryEntry(
    PdeCustomerScope scope,
    String memoryId,
    PdeMemoryCategory category,
    String content,
    PdeMemorySource source,
    String sourceInteractionReference,
    Instant observedAt,
    Instant expiresAt,
    double confidence) {

  private static final int MAX_ID_LENGTH = 200;
  private static final int MAX_CONTENT_LENGTH = 4_000;

  /** Valida o fato sem permitir conteúdo ilimitado, validade invertida ou confiança inválida. */
  public PdeMemoryEntry {
    scope = Objects.requireNonNull(scope, "scope");
    memoryId = requireBoundedText(memoryId, "memoryId", MAX_ID_LENGTH);
    category = Objects.requireNonNull(category, "category");
    content = requireBoundedText(content, "content", MAX_CONTENT_LENGTH);
    source = Objects.requireNonNull(source, "source");
    sourceInteractionReference =
        requireBoundedText(sourceInteractionReference, "sourceInteractionReference", MAX_ID_LENGTH);
    observedAt = Objects.requireNonNull(observedAt, "observedAt");
    if (expiresAt != null && !expiresAt.isAfter(observedAt)) {
      throw new IllegalArgumentException("expiresAt deve ser posterior a observedAt");
    }
    if (!Double.isFinite(confidence) || confidence < 0.0d || confidence > 1.0d) {
      throw new IllegalArgumentException("confidence deve estar entre 0 e 1");
    }
  }

  /** Confirma que o fato pertence ao relacionamento autorizado no snapshot. */
  public boolean belongsTo(PdeCustomerScope expectedScope) {
    return scope.equals(expectedScope);
  }

  /** Informa se o fato ainda pode ser entregue no instante da execução. */
  public boolean isActiveAt(Instant instant) {
    Objects.requireNonNull(instant, "instant");
    return expiresAt == null || instant.isBefore(expiresAt);
  }

  /** Produz a representação determinística usada no hash do snapshot efetivo. */
  String canonicalValue() {
    return scope.fingerprint()
        + "\n"
        + memoryId
        + "\n"
        + category
        + "\n"
        + content
        + "\n"
        + source
        + "\n"
        + sourceInteractionReference
        + "\n"
        + observedAt
        + "\n"
        + (expiresAt == null ? "" : expiresAt)
        + "\n"
        + Double.toString(confidence)
        + "\n";
  }

  /** Exige texto não vazio dentro do limite definido pelo contrato. */
  private static String requireBoundedText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " excede " + maxLength + " caracteres");
    }
    return normalized;
  }
}
