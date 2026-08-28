package com.marketinghub.pde.harness.v1;

import java.util.Objects;

/** Identifica conversa, missão e interação sem permitir escolha arbitrária de workspace. */
public record PdeRunContext(
    PdeConversationScope conversationScope, String missionReference, String interactionReference) {

  /** Valida os correlatores usados pelo backend para lease, memória e auditoria. */
  public PdeRunContext {
    conversationScope = Objects.requireNonNull(conversationScope, "conversationScope");
    missionReference = requireText(missionReference, "missionReference");
    interactionReference = requireText(interactionReference, "interactionReference");
  }

  /** Expõe o escopo durável de memória derivado da conversa. */
  public PdeCustomerScope customerScope() {
    return conversationScope.customerScope();
  }

  /** Expõe o produto para logs operacionais sem repetir a estrutura de escopo. */
  public String productCode() {
    return customerScope().productCode();
  }

  /** Valida um correlator textual obrigatório sem aceitar espaços vazios. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
