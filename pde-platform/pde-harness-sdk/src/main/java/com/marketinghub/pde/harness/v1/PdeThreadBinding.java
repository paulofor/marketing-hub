package com.marketinghub.pde.harness.v1;

import java.time.Instant;
import java.util.Objects;

/** Vincula uma thread persistida ao escopo exato da conversa e à memória já utilizada. */
public record PdeThreadBinding(
    String threadId,
    String conversationScopeFingerprint,
    long memoryRevision,
    int completedTurns,
    boolean ephemeral,
    Instant createdAt,
    Instant lastUsedAt) {

  /** Rejeita vínculo incompleto, revisão regressiva ou cronologia inconsistente. */
  public PdeThreadBinding {
    threadId = requireText(threadId, "threadId");
    conversationScopeFingerprint =
        requireText(conversationScopeFingerprint, "conversationScopeFingerprint");
    if (memoryRevision < 0) {
      throw new IllegalArgumentException("memoryRevision não pode ser negativa");
    }
    if (completedTurns < 0) {
      throw new IllegalArgumentException("completedTurns não pode ser negativo");
    }
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
    lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt");
    if (lastUsedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("lastUsedAt não pode preceder createdAt");
    }
  }

  /** Confirma se o vínculo pertence exatamente à conversa informada. */
  public boolean belongsTo(PdeConversationScope scope) {
    return scope != null && conversationScopeFingerprint.equals(scope.fingerprint());
  }

  /** Exige um identificador textual preenchido. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
