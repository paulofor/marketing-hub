package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que tarefas comerciais percam ou misturem a identidade do PDE. */
class ExperimentAgentTaskTargetContextProviderTest {

  /** Resolve produto, experimento e versão a partir da referência canônica da tarefa. */
  @Test
  void resolvesTypedTargetFromExperimentReference() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .name("Kit WhatsApp Pronto")
            .internalName("Rigel")
            .publicUrl("https://produto.example")
            .pdeExperienceJson("{\"experienceVersion\":\"kit-whatsapp-pronto-pde-v2\"}")
            .currentPriceBrl(new BigDecimal("349.00"))
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .commercialCheckoutUrl("https://checkout.example/rigel")
            .unitPrice(new BigDecimal("349.00"))
            .build();
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(experiments, products, new ObjectMapper());

    var target = provider.resolve("experiment:89").orElseThrow();
    var targetWithStage = provider.resolve("experiment:89@v6:humanExperienceReview").orElseThrow();

    assertThat(target.experimentId()).isEqualTo(89L);
    assertThat(target.productId()).isEqualTo(9L);
    assertThat(target.productSlug()).isEqualTo("kit-whatsapp-pronto");
    assertThat(target.productInternalName()).isEqualTo("Rigel");
    assertThat(target.experienceVersion()).isEqualTo("kit-whatsapp-pronto-pde-v2");
    assertThat(target.commercialCheckoutUrl()).isEqualTo("https://checkout.example/rigel");
    assertThat(target.unitPriceBrl()).isEqualByComparingTo("349.00");
    assertThat(targetWithStage.productId()).isEqualTo(9L);
  }

  /** Recusa referência livre ou produto sem versão, pois ambos permitiriam seleção ambígua. */
  @Test
  void rejectsAmbiguousOrIncompleteTarget() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    Product incomplete = Product.builder().id(9L).slug("kit-whatsapp-pronto").build();
    when(products.findById(9L)).thenReturn(Optional.of(incomplete));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(experiments, products, new ObjectMapper());

    assertThat(provider.resolve("Rigel")).isEmpty();
    assertThat(provider.resolve("product:9@v2")).isEmpty();
  }
}
