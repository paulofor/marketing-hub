package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protege limites, procedência, validade e integridade do snapshot canônico de memória. */
class PdeCustomerMemoryTest {
  private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

  /** Rejeita dois fatos com o mesmo identificador para não ocultar uma sobrescrita. */
  @Test
  void rejectsDuplicateMemoryIdentifiers() {
    PdeMemoryEntry first = entry("memoria-1", "primeiro fato", null);
    PdeMemoryEntry duplicate = entry("memoria-1", "segundo fato", null);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PdeCustomerMemory(
                PdeHarnessTestSupport.customerScope("cliente-a"),
                1,
                NOW,
                "",
                List.of(first, duplicate)));
  }

  /** Filtra item expirado e mantém item vigente sem alterar a lista original. */
  @Test
  void selectsOnlyActiveEntries() {
    PdeMemoryEntry active = entry("ativa", "fato ativo", null);
    PdeMemoryEntry expired =
        entry("expirada", "fato expirado", Instant.parse("2026-08-01T00:00:00Z"));
    PdeCustomerMemory memory =
        new PdeCustomerMemory(
            PdeHarnessTestSupport.customerScope("cliente-a"), 2, NOW, "", List.of(active, expired));

    assertEquals(List.of(active), memory.activeEntriesAt(NOW));
    assertEquals(2, memory.entries().size());
  }

  /** Produz hashes diferentes quando cliente, revisão ou conteúdo autorizado mudam. */
  @Test
  void fingerprintsEffectiveMemoryDeterministically() {
    PdeCustomerMemory first =
        PdeHarnessTestSupport.memory(
            "cliente-a", 1, "resumo", List.of(entry("memoria", "fato", null)));
    PdeCustomerMemory same =
        PdeHarnessTestSupport.memory(
            "cliente-a", 1, "resumo", List.of(entry("memoria", "fato", null)));
    PdeCustomerMemory anotherCustomer =
        PdeHarnessTestSupport.memory(
            "cliente-b", 1, "resumo", List.of(entry("cliente-b", "memoria", "fato", null)));

    assertEquals(first.fingerprintAt(NOW), same.fingerprintAt(NOW));
    assertNotEquals(first.fingerprintAt(NOW), anotherCustomer.fingerprintAt(NOW));
  }

  /** Rejeita fato de outro cliente mesmo quando o snapshot foi rotulado com o escopo esperado. */
  @Test
  void rejectsEntryFromAnotherCustomer() {
    PdeCustomerScope expectedScope = PdeHarnessTestSupport.customerScope("cliente-a");
    PdeMemoryEntry leaked = entry("cliente-b", "memoria-vazada", "fato de B", null);

    assertThrows(
        IllegalArgumentException.class,
        () -> new PdeCustomerMemory(expectedScope, 1, NOW, "", List.of(leaked)));
  }

  /** Cria um fato sintético com procedência declarada pelo usuário. */
  private PdeMemoryEntry entry(String id, String content, Instant expiresAt) {
    return entry("cliente-a", id, content, expiresAt);
  }

  /** Cria um fato sintético dentro do escopo informado para testar segregação. */
  private PdeMemoryEntry entry(
      String customerReference, String id, String content, Instant expiresAt) {
    return new PdeMemoryEntry(
        PdeHarnessTestSupport.customerScope(customerReference),
        id,
        PdeMemoryCategory.PREFERENCE,
        content,
        PdeMemorySource.USER_STATED,
        "interacao-origem",
        Instant.parse("2026-01-01T00:00:00Z"),
        expiresAt,
        1.0d);
  }
}
