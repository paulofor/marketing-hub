package com.marketinghub.pde.harness.v1;

import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import java.util.Objects;

/** Restringe uma thread a uma conversa e versão específicas do relacionamento do cliente. */
public record PdeConversationScope(
    PdeCustomerScope customerScope, String productVersion, String conversationReference) {

  /** Valida a fronteira de relacionamento e os identificadores próprios da conversa. */
  public PdeConversationScope {
    customerScope = Objects.requireNonNull(customerScope, "customerScope");
    productVersion = requireText(productVersion, "productVersion");
    conversationReference = requireText(conversationReference, "conversationReference");
  }

  /** Calcula o fingerprint usado para vincular thread e execução sem expor identificadores. */
  public String fingerprint() {
    return PdeHashing.sha256(
        customerScope.fingerprint()
            + part("version", productVersion)
            + part("conversation", conversationReference));
  }

  /** Codifica tamanho e valor para impedir colisões por concatenação de campos. */
  private static String part(String field, String value) {
    return field + ":" + value.length() + ":" + value + "\n";
  }

  /** Exige um identificador não vazio sem alterar seu conteúdo interno. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
