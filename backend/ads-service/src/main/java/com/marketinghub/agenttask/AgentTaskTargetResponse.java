package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * Responsabilidade: identificar o produto, a versão e o contrato PDE que um executor deve avaliar.
 */
public record AgentTaskTargetResponse(
    String sourceReference,
    Long experimentId,
    Long productId,
    String productSlug,
    String productName,
    String productInternalName,
    String experienceVersion,
    String publicUrl,
    String commercialCheckoutProvider,
    String commercialCheckoutReference,
    String commercialCheckoutUrl,
    BigDecimal unitPriceBrl,
    JsonNode pdeContext) {

  /** Preserva consumidores que precisam somente da identidade e do contrato comercial mínimo. */
  public AgentTaskTargetResponse(
      String sourceReference,
      Long experimentId,
      Long productId,
      String productSlug,
      String productName,
      String productInternalName,
      String experienceVersion,
      String publicUrl,
      String commercialCheckoutProvider,
      String commercialCheckoutReference,
      String commercialCheckoutUrl,
      BigDecimal unitPriceBrl) {
    this(
        sourceReference,
        experimentId,
        productId,
        productSlug,
        productName,
        productInternalName,
        experienceVersion,
        publicUrl,
        commercialCheckoutProvider,
        commercialCheckoutReference,
        commercialCheckoutUrl,
        unitPriceBrl,
        null);
  }
}
