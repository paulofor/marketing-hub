package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.*;
import com.marketinghub.pde.service.promotion.PdeCommercialPublicationPolicy;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: preparar e ativar entrega paga pelo gate existente, sem exigir vídeo não
 * contratado.
 */
class PaidVisualPublicationTest {
  /** Mantém identidade, checkout e homologação obrigatórios para dois produtos independentes. */
  @Test
  void paidDeliveryUsesExistingPublicationGate() {
    for (long id : List.of(97L, 709L)) {
      var slot =
          PdeProductionSlot.builder()
              .productSlug("product-" + id)
              .experienceVersion("paid-v1")
              .sourceExperimentId(id)
              .layoutKey("paid-visual-v1")
              .status(PdeProductionSlotStatus.CANDIDATE)
              .validationStatus("OK")
              .build();
      var exp =
          Experiment.builder()
              .id(id)
              .product(Product.builder().slug(slot.getProductSlug()).build())
              .unitPrice(new BigDecimal("79"))
              .primaryCta("Criar combinações")
              .commercialCheckoutUrl("https://example.test/checkout")
              .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_PAID_DELIVERY)
              .build();
      var contract =
          new ObjectMapper()
              .createObjectNode()
              .put("slug", slot.getProductSlug())
              .put("experienceVersion", "paid-v1")
              .put("layoutKey", "paid-visual-v1");
      contract
          .putObject("commercialBinding")
          .put("experimentId", id)
          .put("priceBrl", 79)
          .put("primaryCta", exp.getPrimaryCta())
          .put("billingModel", "ONE_TIME");
      contract
          .putObject("commercialCheckout")
          .put("checkoutUrl", exp.getCommercialCheckoutUrl())
          .put("priceBrl", 79)
          .put("currency", "BRL")
          .put("billingModel", "ONE_TIME")
          .put("provider", "MERCADO_PAGO");
      contract.putObject("paidVisualDelivery").put("contractVersion", PaidVisualPolicy.VERSION);
      assertThat(PdeCommercialPublicationPolicy.appliesTo(slot)).isTrue();
      assertThat(PdeCommercialPublicationPolicy.blockers(slot, exp, List.of(), contract, false))
          .isEmpty();
      assertThat(PdeCommercialPublicationPolicy.blockers(slot, exp, List.of(), contract, true))
          .isNotEmpty();
      slot.setStatus(PdeProductionSlotStatus.READY);
      assertThat(PdeCommercialPublicationPolicy.blockers(slot, exp, List.of(), contract, true))
          .isEmpty();
      slot.setValidationStatus("FAILED");
      assertThat(PdeCommercialPublicationPolicy.blockers(slot, exp, List.of(), contract, false))
          .isNotEmpty();
      slot.setValidationStatus("OK");
      exp.setId(id + 1);
      assertThat(PdeCommercialPublicationPolicy.blockers(slot, exp, List.of(), contract, false))
          .isNotEmpty();
    }
  }
}
