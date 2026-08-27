package com.marketinghub.geralanding.agent.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o snapshot comercial entregue aos revisores da landing. */
class LandingCheckoutEvidenceResolverTest {

  /** Aprova o binding persistido quando produto, experimento, preço e cobrança coincidem. */
  @Test
  void validatesCanonicalOneTimeCheckoutBinding() {
    Experiment experiment = experiment(BigDecimal.valueOf(349), BigDecimal.valueOf(349));

    var evidence = resolver().resolve(experiment);

    assertThat(evidence)
        .containsEntry("validationStatus", "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING")
        .containsEntry("productName", "Kit WhatsApp Pronto")
        .containsEntry("amountBrl", BigDecimal.valueOf(349))
        .containsEntry("billingModel", "ONE_TIME")
        .containsEntry("externalSideEffects", false);
    assertThat(evidence.get("blockers")).isEqualTo(java.util.List.of());
  }

  /** Bloqueia um checkout cujo preço congelado deixou de representar o experimento. */
  @Test
  void blocksDivergentPersistedPrice() {
    Experiment experiment = experiment(BigDecimal.valueOf(349), BigDecimal.valueOf(299));

    var evidence = resolver().resolve(experiment);

    assertThat(evidence).containsEntry("validationStatus", "BLOCKED");
    assertThat(evidence.get("blockers").toString()).contains("Preço do binding comercial diverge");
  }

  /** Bloqueia a landing quando nenhum destino comercial canônico foi persistido. */
  @Test
  void blocksMissingCommercialCheckout() {
    Experiment experiment = experiment(BigDecimal.valueOf(349), BigDecimal.valueOf(349));
    experiment.setCommercialCheckoutUrl(null);

    var evidence = resolver().resolve(experiment);

    assertThat(evidence).containsEntry("validationStatus", "BLOCKED");
    assertThat(evidence.get("blockers").toString()).contains("Checkout comercial canônico ausente");
  }

  /** Monta um experimento PDE mínimo com binding comercial versionado. */
  private Experiment experiment(BigDecimal experimentPrice, BigDecimal bindingPrice) {
    Product product = new Product();
    product.setId(9L);
    product.setName("Kit WhatsApp Pronto");
    product.setSlug("kit-whatsapp-pronto");
    product.setPublicUrl("https://kit-whatsapp-pronto.example");
    product.setPdeExperienceJson(
        "{\"commercialBinding\":{\"experimentId\":89,\"priceBrl\":"
            + bindingPrice
            + ",\"billingModel\":\"ONE_TIME\"}}");
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(product);
    experiment.setUnitPrice(experimentPrice);
    experiment.setCommercialCheckoutUrl("https://checkout.example/rigel");
    return experiment;
  }

  /** Cria o resolvedor com fallback de publicação isolado para o teste. */
  private LandingCheckoutEvidenceResolver resolver() {
    var repository = mock(GeraSalesPagePublicationAuditRepository.class);
    return new LandingCheckoutEvidenceResolver(
        new LandingCheckoutContractResolver(repository), new ObjectMapper());
  }
}
