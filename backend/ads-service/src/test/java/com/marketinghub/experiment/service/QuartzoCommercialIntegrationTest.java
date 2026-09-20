package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: prevenir recorrência da exigência de slot Opala no percurso de venda Quartzo.
 */
class QuartzoCommercialIntegrationTest {
  /** Roteia o produto sem ciclo mantendo a referência explícita do experimento no link do filho. */
  @Test
  void routesQuartzoWithoutInventingLearningCycle() throws Exception {
    var json = new ObjectMapper();
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var context = mock(QuartzoCommercialContext.class);
    var executor = new PdeCommercialPreparationActivityExecutor(processes, cycles, json);
    ReflectionTestUtils.setField(executor, "quartzoContext", context);
    var product = product();
    var target = new BusinessProcessDefinition();
    target.setId(90L);
    target.setStatus("PUBLISHED");
    target.setName("Quartzo");
    when(processes.findByProcessCodeAndVersionNumber(QuartzoCommercialContext.CODE, 1))
        .thenReturn(Optional.of(target));
    when(context.applies(product)).thenReturn(true);
    when(context.scope("experiment:88", 7L, true))
        .thenReturn(new QuartzoCommercialContext.Scope(null, product, "v1", null, null, null));
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("commercialPreparation");
    activity.setDefinitionJson(
        """
        {"subprocessRoutes":[{"productTypeCode":"LOW_TICKET_DIGITAL_PRODUCT","subprocessCode":"quartzo-commercial-preparation-v1","subprocessVersion":1}]}
        """);
    var result =
        executor.readiness(new BusinessProcessDefinition(), activity, product, "experiment:88");
    assertThat(result.ready()).isTrue();
    assertThat(result.targetProcessDefinitionId()).isEqualTo(90L);
    assertThat(result.navigationUrl())
        .contains("sourceReference=experiment%3A88")
        .doesNotContain("learningCycleId");
    verifyNoInteractions(cycles);
  }

  /**
   * Reproduz o contrato que faltou à tarefa histórica: URL auditada sem experienceVersion Opala.
   */
  @Test
  void exposesAuditedKitPageToCommercialReviewer() throws Exception {
    var json = new ObjectMapper();
    var experiments = mock(ExperimentRepository.class);
    var products = mock(ProductRepository.class);
    var context = mock(QuartzoCommercialContext.class);
    var provider = new ExperimentAgentTaskTargetContextProvider(experiments, products, json);
    ReflectionTestUtils.setField(provider, "quartzoContext", context);
    var product = product();
    var experiment = new Experiment();
    experiment.setId(88L);
    experiment.setProduct(product);
    experiment.setUnitPrice(new BigDecimal("67"));
    when(experiments.findById(88L)).thenReturn(Optional.of(experiment));
    when(context.applies(product)).thenReturn(true);
    when(context.scope("experiment:88", 7L, false))
        .thenReturn(
            new QuartzoCommercialContext.Scope(experiment, product, "v1", null, null, null));
    when(context.snapshot("experiment:88"))
        .thenReturn(
            (com.fasterxml.jackson.databind.node.ObjectNode)
                json.readTree(
                    """
        {"destinationUrl":"https://example.test/audited-kit","checkoutUrl":"https://example.test/pay","productId":7,"experimentId":88}
        """));
    for (String code :
        java.util.List.of(
            QuartzoCommercialContext.CODE, "pde-commercial-homologation-activation")) {
      var target = provider.resolve("experiment:88", code).orElseThrow();
      assertThat(target.publicUrl()).isEqualTo("https://example.test/audited-kit");
      assertThat(target.experienceVersion()).isEqualTo("v1");
      assertThat(target.commercialCheckoutUrl()).isEqualTo("https://example.test/pay");
    }
  }

  /** Cria identidade sintética classificada pelo código oficial do catálogo. */
  private Product product() {
    return Product.builder()
        .id(7L)
        .slug("synthetic-kit")
        .productTypeDefinition(
            ProductTypeDefinition.builder().code(QuartzoCommercialContext.TYPE).build())
        .build();
  }
}
