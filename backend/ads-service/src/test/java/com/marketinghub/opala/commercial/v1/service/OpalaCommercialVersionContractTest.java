package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir checkout ou acesso cruzado entre versões comerciais do Opala. */
class OpalaCommercialVersionContractTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialVersionContract resolver =
      new OpalaCommercialVersionContract(new PdeCommercialCheckoutContractResolver(json));

  /** Resolve a candidata exata, inclusive em nova execução com identificadores diferentes. */
  @Test
  void resolvesExactCandidateForCurrentAndFutureExecutions() throws Exception {
    for (var fixture :
        List.of(fixture(4L, 92L, 2L, "vega-v12"), fixture(14L, 192L, 22L, "vega-v13"))) {
      var result = resolver.resolve(fixture.scope(), fixture.candidate());

      assertThat(result.productContract().path("experienceVersion").asText())
          .isEqualTo(fixture.scope().cycle().getProductVersion());
      assertThat(result.checkout().offerReference()).isEqualTo("owm6x");
      assertThat(result.checkout().priceBrl()).isEqualByComparingTo("67.00");
      assertThat(result.accessDays()).isEqualTo(90);
    }
  }

  /** Recusa a versão predecessora mesmo quando ela reutiliza exatamente o mesmo checkout. */
  @Test
  void rejectsPredecessorContractWithSameCheckout() throws Exception {
    var fixture = fixture(4L, 92L, 2L, "vega-v12");
    ((ObjectNode) fixture.candidate().productContract()).put("experienceVersion", "vega-v7");

    assertThatThrownBy(() -> resolver.resolve(fixture.scope(), fixture.candidate()))
        .hasMessageContaining("versão vigente");
  }

  /** Recusa checkout, preço e acesso que não comprovem a mesma ocorrência comercial. */
  @Test
  void rejectsDivergentCheckoutPriceAndAccess() throws Exception {
    var wrongExperiment = fixture(4L, 92L, 2L, "vega-v12");
    ((ObjectNode) wrongExperiment.candidate().productContract().path("commercialBinding"))
        .put("experimentId", 91);
    assertThatThrownBy(() -> resolver.resolve(wrongExperiment.scope(), wrongExperiment.candidate()))
        .hasMessageContaining("vínculo comercial");

    var wrongCheckout = fixture(4L, 92L, 2L, "vega-v12");
    ((ObjectNode) wrongCheckout.candidate().productContract().path("commercialCheckout"))
        .put("checkoutUrl", "https://checkout.sandbox.local/outro");
    assertThatThrownBy(() -> resolver.resolve(wrongCheckout.scope(), wrongCheckout.candidate()))
        .hasMessageContaining("outro checkout");

    var wrongAccess = fixture(4L, 92L, 2L, "vega-v12");
    ((ObjectNode) wrongAccess.candidate().productContract().path("commercialAccess"))
        .put("experienceVersion", "vega-v7");
    assertThatThrownBy(() -> resolver.resolve(wrongAccess.scope(), wrongAccess.candidate()))
        .hasMessageContaining("acesso pago");
  }

  /** Monta uma ocorrência sintética sem chamar pagamento, publicação ou concessão de acesso. */
  private Fixture fixture(long productId, long experimentId, long cycleId, String version)
      throws Exception {
    Product product = Product.builder().id(productId).slug("opala-" + productId).build();
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .unitPrice(new BigDecimal("67"))
            .commercialCheckoutUrl("https://go.pepper.com.br/owm6x")
            .build();
    LearningSalesCycle cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(version);
    ObjectNode contract =
        (ObjectNode)
            json.readTree(
                """
                {"experienceVersion":"%s",
                 "commercialBinding":{"experimentId":%d,"priceBrl":67,"billingModel":"ONE_TIME"},
                 "commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","offerReference":"owm6x","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"},
                 "commercialAccess":{"experienceVersion":"%s","accessDays":90,"renewal":false,"activationTrigger":"PAYMENT_APPROVED","scope":"DAYS_2_TO_7_AND_SUPPORT_MATERIALS"}}
                """
                    .formatted(version, experimentId, version));
    return new Fixture(
        new OpalaCommercialContext.Scope(cycle, experiment),
        new OpalaCommercialContext.Candidate(
            List.of(), "https://opala.sandbox.local", "VERSION_SLOT", contract));
  }

  /** Agrupa a identidade sintética e o contrato candidato usados em cada cenário. */
  private record Fixture(
      OpalaCommercialContext.Scope scope, OpalaCommercialContext.Candidate candidate) {}
}
