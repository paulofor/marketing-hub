package com.marketinghub.pde.harness.v1;

import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Entrega ao SDK um snapshot autorizado, limitado e revisado da memória do cliente. */
public record PdeCustomerMemory(
    PdeCustomerScope scope,
    long revision,
    Instant generatedAt,
    String relationshipSummary,
    List<PdeMemoryEntry> entries) {

  private static final int MAX_SUMMARY_LENGTH = 8_000;
  private static final int MAX_ENTRIES = 64;
  private static final int MAX_TOTAL_CONTENT_LENGTH = 48_000;

  /** Congela o snapshot e rejeita revisão, volume ou identificadores inconsistentes. */
  public PdeCustomerMemory {
    scope = Objects.requireNonNull(scope, "scope");
    if (revision < 0) {
      throw new IllegalArgumentException("revision não pode ser negativa");
    }
    generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    relationshipSummary = normalizeSummary(relationshipSummary);
    entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    if (entries.size() > MAX_ENTRIES) {
      throw new IllegalArgumentException("entries excede " + MAX_ENTRIES + " itens");
    }
    Set<String> identifiers = new HashSet<>();
    int totalContentLength = relationshipSummary.length();
    for (PdeMemoryEntry entry : entries) {
      Objects.requireNonNull(entry, "entry");
      if (!entry.belongsTo(scope)) {
        throw new IllegalArgumentException(
            "entry não pertence ao tenant, produto e cliente do snapshot");
      }
      if (!identifiers.add(entry.memoryId())) {
        throw new IllegalArgumentException("memoryId duplicado: " + entry.memoryId());
      }
      totalContentLength += entry.content().length();
    }
    if (totalContentLength > MAX_TOTAL_CONTENT_LENGTH) {
      throw new IllegalArgumentException(
          "memória excede " + MAX_TOTAL_CONTENT_LENGTH + " caracteres autorizados");
    }
  }

  /** Cria um snapshot inicial explícito para relacionamento sem memória anterior. */
  public static PdeCustomerMemory empty(PdeCustomerScope scope, Instant generatedAt) {
    return new PdeCustomerMemory(scope, 0, generatedAt, "", List.of());
  }

  /** Seleciona somente itens vigentes no início da execução sem alterar a ordem de relevância. */
  public List<PdeMemoryEntry> activeEntriesAt(Instant instant) {
    Objects.requireNonNull(instant, "instant");
    return entries.stream().filter(entry -> entry.isActiveAt(instant)).toList();
  }

  /** Calcula o hash do snapshot efetivamente entregue, excluindo itens já expirados. */
  public String fingerprintAt(Instant instant) {
    StringBuilder canonical =
        new StringBuilder()
            .append(scope.fingerprint())
            .append('\n')
            .append(revision)
            .append('\n')
            .append(generatedAt)
            .append('\n')
            .append(relationshipSummary)
            .append('\n');
    activeEntriesAt(instant).forEach(entry -> canonical.append(entry.canonicalValue()));
    return PdeHashing.sha256(canonical.toString());
  }

  /** Normaliza o resumo opcional e impede que ele domine o contexto do produto. */
  private static String normalizeSummary(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.length() > MAX_SUMMARY_LENGTH) {
      throw new IllegalArgumentException(
          "relationshipSummary excede " + MAX_SUMMARY_LENGTH + " caracteres");
    }
    return normalized;
  }
}
