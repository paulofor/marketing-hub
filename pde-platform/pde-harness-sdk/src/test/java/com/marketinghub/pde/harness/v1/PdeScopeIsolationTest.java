package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** Protege todos os componentes que formam as fronteiras de memória e conversa. */
class PdeScopeIsolationTest {

  /** Garante que tenant, produto e cliente alterem o fingerprint da memória durável. */
  @Test
  void separatesEveryCustomerScopeDimension() {
    PdeCustomerScope base = new PdeCustomerScope("tenant-a", "produto-a", "cliente-a");

    assertNotEquals(
        base.fingerprint(),
        new PdeCustomerScope("tenant-b", "produto-a", "cliente-a").fingerprint());
    assertNotEquals(
        base.fingerprint(),
        new PdeCustomerScope("tenant-a", "produto-b", "cliente-a").fingerprint());
    assertNotEquals(
        base.fingerprint(),
        new PdeCustomerScope("tenant-a", "produto-a", "cliente-b").fingerprint());
  }

  /** Garante que versão e conversa alterem o fingerprint usado pelo vínculo da thread. */
  @Test
  void separatesEveryConversationScopeDimension() {
    PdeCustomerScope customer = new PdeCustomerScope("tenant-a", "produto-a", "cliente-a");
    PdeConversationScope base = new PdeConversationScope(customer, "v1", "conversa-a");

    assertNotEquals(
        base.fingerprint(), new PdeConversationScope(customer, "v2", "conversa-a").fingerprint());
    assertNotEquals(
        base.fingerprint(), new PdeConversationScope(customer, "v1", "conversa-b").fingerprint());
  }
}
