package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a identidade comercial versionada do checkout PDE. */
class PdeCommercialCheckoutContractResolverTest {
  private final PdeCommercialCheckoutContractResolver resolver =
      new PdeCommercialCheckoutContractResolver(new ObjectMapper());

  /** Resolve provedor, oferta, preço e cobrança do contrato completo. */
  @Test
  void resolvesCompleteVersionedCheckout() {
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson(
                """
                {"commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","offerReference":"owm6x","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"}}
                """)
            .build();

    var checkout = resolver.resolve(product).orElseThrow();

    assertThat(checkout.provider()).isEqualTo("PEPPER");
    assertThat(checkout.checkoutUrl()).isEqualTo("https://go.pepper.com.br/owm6x");
    assertThat(checkout.offerReference()).isEqualTo("owm6x");
    assertThat(checkout.priceBrl()).isEqualByComparingTo("67.00");
    assertThat(checkout.currency()).isEqualTo("BRL");
    assertThat(checkout.billingModel()).isEqualTo("ONE_TIME");
  }

  /** Mantém produtos sem vínculo versionado no fallback comercial já existente. */
  @Test
  void returnsEmptyWhenContractDoesNotDeclareCheckout() {
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .pdeExperienceJson("{\"experienceVersion\":\"kit-whatsapp-pronto-pde-v2\"}")
            .build();

    assertThat(resolver.resolve(product)).isEmpty();
  }

  /** Rejeita declaração parcial antes que oferta, tarefa ou renovação usem outra identidade. */
  @Test
  void rejectsPartialVersionedCheckout() {
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson(
                "{\"commercialCheckout\":{\"provider\":\"PEPPER\",\"checkoutUrl\":\"http://inseguro.example\"}}")
            .build();

    assertThatThrownBy(() -> resolver.resolve(product))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("campo obrigatório ausente: offerReference");
  }

  /** Rejeita preço textual para não aceitar coerção silenciosa no vínculo comercial. */
  @Test
  void rejectsTextualPrice() {
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson(
                """
                {"commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","offerReference":"owm6x","priceBrl":"67","currency":"BRL","billingModel":"ONE_TIME"}}
                """)
            .build();

    assertThatThrownBy(() -> resolver.resolve(product))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("priceBrl precisa ser numérico");
  }

  /** Rejeita JSON nulo para diferenciar contrato ausente de conteúdo corrompido. */
  @Test
  void rejectsNullJsonDocument() {
    Product product =
        Product.builder().id(4L).slug("metodo-musa-7-dias").pdeExperienceJson("null").build();

    assertThatThrownBy(() -> resolver.resolve(product))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("contrato PDE precisa ser um objeto JSON");
  }
}
