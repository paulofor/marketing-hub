package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir que kits usem checkout ou pós-compra de outro contrato. */
class LowTicketCheckoutContractTest {
  private final LowTicketCheckoutContract resolver =
      new LowTicketCheckoutContract(new ObjectMapper());

  /** Confere a identidade de pagamento distinta do slug sem exigir uma área PDE. */
  @Test
  void resolvesVersionedLowTicketDelivery() {
    var contract = resolver.resolve(experiment(7L, 105L, "nails-kit"));
    assertThat(contract.productKey()).isEqualTo("nails-kit");
    assertThat(contract.deliveryPageUrl()).isEqualTo("https://payments.example/nails-kit/briefing");
  }

  /** Protege outra identidade sem depender do nome ou dos IDs de Capella. */
  @Test
  void resolvesAnotherProductAndExperiment() {
    var contract = resolver.resolve(experiment(207L, 9105L, "other-kit"));
    assertThat(contract.productKey()).isEqualTo("other-kit");
    assertThat(contract.deliveryPageUrl()).isEqualTo("https://payments.example/other-kit/briefing");
  }

  /** Recusa contratos incompletos e não deduz URLs a partir de texto livre. */
  @Test
  void refusesMissingExplicitCheckoutContract() {
    var experiment = experiment(7L, 105L, "nails-kit");
    experiment
        .getProduct()
        .setValidationDefinitionJson(
            "{\"delivery\":{\"briefingChannel\":\"https://payments.example/old\"}}");
    assertThatThrownBy(() -> resolver.resolve(experiment))
        .hasMessageContaining("commercialCheckout");
  }

  /**
   * Bloqueia versão, preço, modalidade, origem e URL incompatíveis antes de falar com pagamentos.
   */
  @Test
  void refusesChangedOrUnsafeContracts() {
    String original = experiment(7L, 105L, "nails-kit").getProduct().getValidationDefinitionJson();
    for (String changed :
        new String[] {
          original.replace("\"productVersion\":\"v1\"", "\"productVersion\":\"v0\""),
          original.replace("\"priceBrl\":67", "\"priceBrl\":66"),
          original.replace("\"ONE_TIME\"", "\"SUBSCRIPTION\""),
          original.replace("\"MERCADO_PAGO\"", "\"UNKNOWN\""),
          original.replace("https://payments.example", "http://payments.example"),
          original.replace("https://payments.example", "https://user:secret@payments.example"),
          original.replace("\"BRL\"", "\"USD\""),
          original.replace("\"nails-kit\"", "\"bad key\"")
        }) {
      var experiment = experiment(7L, 105L, "nails-kit");
      experiment.getProduct().setValidationDefinitionJson(changed);
      assertThatThrownBy(() -> resolver.resolve(experiment))
          .isInstanceOf(ResponseStatusException.class);
    }
  }

  /** Não permite que o contrato de kit seja usado para outro tipo ou preço persistido. */
  @Test
  void refusesAnotherTypeAndProductPriceMismatch() {
    var experiment = experiment(7L, 105L, "nails-kit");
    experiment.getProduct().getProductTypeDefinition().setCode("PDE_MEMBERSHIP_SUBSCRIPTION");
    assertThatThrownBy(() -> resolver.resolve(experiment)).hasMessageContaining("tipo");
    var changed = experiment(7L, 105L, "nails-kit");
    changed.getProduct().setCurrentPriceBrl(BigDecimal.ONE);
    assertThatThrownBy(() -> resolver.resolve(changed)).hasMessageContaining("preço");
  }

  /** Constrói um contrato controlado com preço e versão do produto, sem pagar ou publicar. */
  static Experiment experiment(long productId, long experimentId, String paymentKey) {
    var product =
        Product.builder()
            .id(productId)
            .slug("public-" + paymentKey)
            .name("Kit útil")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code("LOW_TICKET_DIGITAL_PRODUCT").build())
            .currentPriceBrl(new BigDecimal("67"))
            .validationDefinitionVersion("v1")
            .validationDefinitionJson(
                """
          {"checkoutIdentity":{"externalReference":"%s"},
           "commercialCheckout":{"contractVersion":"LOW_TICKET_COMMERCIAL_CHECKOUT_V1",
             "productVersion":"v1","provider":"MERCADO_PAGO","currency":"BRL",
             "billingModel":"ONE_TIME","priceBrl":67,
             "deliveryPageUrl":"https://payments.example/%s/briefing"}}
          """
                    .formatted(paymentKey, paymentKey))
            .build();
    return Experiment.builder()
        .id(experimentId)
        .product(product)
        .status(ExperimentStatus.PLANNED)
        .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
        .unitPrice(new BigDecimal("67.00"))
        .build();
  }
}
