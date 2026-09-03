package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
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

  /** Usa o slot da versão exata no PDE sem trocar a landing usada pelo processo de landing. */
  @Test
  void resolvesProcessSpecificPublicUrlWithoutMixingExperienceVersions() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .name("Kit WhatsApp Pronto")
            .internalName("Rigel")
            .publicUrl("https://produto-generico.example")
            .pdeExperienceJson(
                """
                {"experienceVersion":"kit-whatsapp-pronto-pde-v2","commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/checkout-v2","offerReference":"checkout-v2","priceBrl":349,"currency":"BRL","billingModel":"ONE_TIME"}}
                """)
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .followUpActionUrl("https://landing-rigel.example")
            .commercialCheckoutUrl("https://checkout-fallback.example/rigel")
            .build();
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .productSlug("kit-whatsapp-pronto")
            .experienceVersion("kit-whatsapp-pronto-pde-v2")
            .status(PdeProductionSlotStatus.READY)
            .publicUrl("https://pde-v2-rigel.example")
            .build();
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(slots.findFirstByProductSlugAndExperienceVersionAndStatusInOrderByPublishedAtDesc(
            "kit-whatsapp-pronto",
            "kit-whatsapp-pronto-pde-v2",
            java.util.List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)))
        .thenReturn(Optional.of(slot));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(
            experiments, products, new ObjectMapper(), slots);

    var landing = provider.resolve("experiment:89", "landing-page-generation").orElseThrow();
    var pde =
        provider.resolve("experiment:89", "pde-commercial-homologation-activation").orElseThrow();

    assertThat(landing.publicUrl()).isEqualTo("https://landing-rigel.example");
    assertThat(landing.commercialCheckoutUrl())
        .isEqualTo("https://checkout-fallback.example/rigel");
    assertThat(pde.publicUrl()).isEqualTo("https://pde-v2-rigel.example");
    assertThat(pde.experienceVersion()).isEqualTo("kit-whatsapp-pronto-pde-v2");
    assertThat(pde.commercialCheckoutProvider()).isEqualTo("PEPPER");
    assertThat(pde.commercialCheckoutReference()).isEqualTo("checkout-v2");
    assertThat(pde.commercialCheckoutUrl()).isEqualTo("https://go.pepper.com.br/checkout-v2");
    assertThat(pde.unitPriceBrl()).isEqualByComparingTo("349.00");
  }

  /** Usa a versão privada aceita sem exigir slot público ou checkout comercial real. */
  @Test
  void resolvesAcceptedPrivatePrototypeWithoutProductionContract() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    Product product =
        Product.builder()
            .id(19L)
            .slug("pde-planejado-301")
            .name("PDE privado")
            .internalName("PDE privado #301")
            .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
            .validationDefinitionJson(
                """
                {"privatePrototypeAcceptance":{"status":"READY",
                  "privateAccessUrl":"https://private.local/prototype"}}
                """)
            .pdeExperienceJson(
                """
                {"contractVersion":"PDE_HARNESS_PLAN_V1",
                 "experienceVersion":"private-validation-v1",
                 "marketStrategy":{"buyer":"Mulher com produtos de skincare",
                   "problem":"Organizar recomendações dispersas em uma rotina pessoal"},
                 "economics":{"commercialSpendAuthorized":false},
                 "harness":{"privatePrototype":{"simpleInput":"Lista de produtos",
                   "readyResult":"Rotina pronta"}},
                 "privateValidationPlan":{"purchaseScene":{"trigger":"Antes de usar os itens"}},
                 "publicationBoundary":"Construção privada sem publicação ou cobrança"}
                """)
            .currentPriceBrl(new BigDecimal("97.00"))
            .build();
    when(products.findById(19L)).thenReturn(Optional.of(product));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(experiments, products, new ObjectMapper());

    var target =
        provider
            .resolve("product:19@private-validation-v1", "pde-construction-approval")
            .orElseThrow();

    assertThat(target.publicUrl()).isEqualTo("https://private.local/prototype");
    assertThat(target.commercialCheckoutProvider()).isNull();
    assertThat(target.commercialCheckoutReference()).isNull();
    assertThat(target.commercialCheckoutUrl()).isNull();
    assertThat(target.unitPriceBrl()).isEqualByComparingTo("97.00");
    assertThat(target.pdeContext().path("contractVersion").asText())
        .isEqualTo("PDE_HARNESS_PLAN_V1");
    assertThat(target.pdeContext().path("marketStrategy").path("buyer").asText())
        .isEqualTo("Mulher com produtos de skincare");
    assertThat(
            target
                .pdeContext()
                .path("harness")
                .path("privatePrototype")
                .path("readyResult")
                .asText())
        .isEqualTo("Rotina pronta");
  }

  /** Não amplia prompts comerciais com o contrato privado fora da construção governada. */
  @Test
  void omitsPrivatePdeContextOutsideConstructionProcess() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    Product product =
        Product.builder()
            .id(19L)
            .slug("pde-planejado-301")
            .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
            .pdeExperienceJson(
                "{\"contractVersion\":\"PDE_HARNESS_PLAN_V1\",\"experienceVersion\":\"private-validation-v1\"}")
            .build();
    when(products.findById(19L)).thenReturn(Optional.of(product));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(experiments, products, new ObjectMapper());

    var target =
        provider.resolve("product:19@private-validation-v1", "product-research").orElseThrow();

    assertThat(target.pdeContext()).isNull();
  }

  /** Bloqueia uma revisão que combine o checkout versionado com preço de outro experimento. */
  @Test
  void rejectsVersionedCheckoutWithDivergentExperimentPrice() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson(
                """
                {"experienceVersion":"musa-v7","commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","offerReference":"owm6x","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"}}
                """)
            .build();
    Experiment experiment =
        Experiment.builder().id(90L).product(product).unitPrice(new BigDecimal("97.00")).build();
    when(experiments.findById(90L)).thenReturn(Optional.of(experiment));
    when(slots.findFirstByProductSlugAndExperienceVersionAndStatusInOrderByPublishedAtDesc(
            "metodo-musa-7-dias",
            "musa-v7",
            java.util.List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)))
        .thenReturn(Optional.empty());
    var provider =
        new ExperimentAgentTaskTargetContextProvider(
            experiments, products, new ObjectMapper(), slots);

    assertThatThrownBy(
            () -> provider.resolve("experiment:90", "pde-commercial-homologation-activation"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Preço do alvo comercial diverge");
  }

  /** Resolve a landing pelo experimento segregado do plano e recusa vínculo cruzado. */
  @Test
  void resolvesCommercialPlanReferenceOnlyForItsDeclaredExperiment() {
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .name("Kit WhatsApp Pronto")
            .internalName("Rigel")
            .pdeExperienceJson("{\"experienceVersion\":\"kit-whatsapp-pronto-pde-v2\"}")
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .followUpActionUrl("https://landing-rigel.example")
            .build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).experiment(experiment).build();
    when(plans.findById(4L)).thenReturn(Optional.of(plan));
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(experiments.findById(90L))
        .thenReturn(
            Optional.of(
                Experiment.builder()
                    .id(90L)
                    .product(Product.builder().id(10L).slug("outro-produto").build())
                    .build()));
    var provider =
        new ExperimentAgentTaskTargetContextProvider(
            experiments, products, new ObjectMapper(), slots, plans);

    var target =
        provider
            .resolve(
                "commercial-plan:4@v3:journey:experiment-89:attempt:2", "landing-page-generation")
            .orElseThrow();

    assertThat(target.experimentId()).isEqualTo(89L);
    assertThat(target.publicUrl()).isEqualTo("https://landing-rigel.example");
    assertThat(
            provider.resolve(
                "commercial-plan:4@v3:journey:experiment-90", "landing-page-generation"))
        .isEmpty();
    assertThat(
            provider
                .resolve("commercial-plan:4@v3:journey", "landing-page-generation")
                .orElseThrow()
                .experimentId())
        .isEqualTo(89L);
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
