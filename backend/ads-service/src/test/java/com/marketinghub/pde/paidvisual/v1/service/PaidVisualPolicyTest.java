package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.*;
import com.marketinghub.pde.*;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: impedir compra sem versão e liberação comercial próprias, mesmo com
 * infraestrutura disponível.
 */
class PaidVisualPolicyTest {
  /** Confere autorização, preço, vínculo financeiro e independência de nome do produto. */
  @Test
  void publicOfferDoesNotImplyCommercialAuthorization() throws Exception {
    var json = new ObjectMapper();
    var experiments = mock(ExperimentRepository.class);
    var slots = mock(PdeProductionSlotRepository.class);
    var plans = mock(CommercialPlanRepository.class);
    var service = new PaidVisualPolicy(experiments, slots, json, plans);
    for (long id : List.of(97L, 709L)) {
      var product = new Product();
      product.setId(id + 100);
      product.setName("Produto " + id);
      product.setSlug("produto-" + id);
      product.setAutomaticExecutionEnabled(true);
      var exp = new Experiment();
      exp.setId(id);
      exp.setProduct(product);
      exp.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_PAID_DELIVERY);
      exp.setUnitPrice(new BigDecimal("79"));
      exp.setStatus(ExperimentStatus.PLANNED);
      exp.setCommercialCheckoutUrl("https://mercadopago.com/local");
      var plan = new CommercialPlan();
      plan.setId(id + 200);
      plan.setExperiment(exp);
      when(plans.findById(id + 200)).thenReturn(Optional.of(plan));
      when(experiments.findById(id)).thenReturn(Optional.of(exp));
      var slot = new PdeProductionSlot();
      slot.setProductSlug(product.getSlug());
      slot.setExperienceVersion("commercial-v1");
      slot.setSourceExperimentId(id);
      slot.setStatus(PdeProductionSlotStatus.CANDIDATE);
      slot.setValidationStatus("OK");
      slot.setPublicUrl("https://example.test/paid-visual.html?experiment=" + id);
      var contract = json.createObjectNode();
      contract.put("slug", product.getSlug());
      contract.put("experienceVersion", "commercial-v1");
      contract.putObject("commercialBinding").put("experimentId", id).put("priceBrl", 79);
      contract
          .putObject("paidVisualDelivery")
          .put("contractVersion", PaidVisualPolicy.VERSION)
          .put("accessDays", 7)
          .put("maxRegenerations", 1)
          .put("commercialPlanId", id + 200)
          .put("model", "gpt-5.6")
          .put("supportEmail", "support@example.test")
          .put("termsUrl", "https://example.test/terms")
          .put("privacyUrl", "https://example.test/privacy")
          .put("refundPolicyUrl", "https://example.test/refunds")
          .put("maxGenerationUsd", new BigDecimal("0.5"));
      slot.setPublishedExperienceJson(json.writeValueAsString(contract));
      when(slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug())).thenReturn(List.of(slot));
      assertThat(service.resolve(id, false).path("selling").asBoolean()).isFalse();
      assertThatThrownBy(() -> service.resolveSettlement(id, "commercial-v1"))
          .isInstanceOf(ResponseStatusException.class);
      assertThatThrownBy(() -> service.resolve(id, true))
          .isInstanceOf(ResponseStatusException.class);
      exp.setStatus(ExperimentStatus.RUNNING);
      slot.setStatus(PdeProductionSlotStatus.ACTIVE);
      assertThat(service.resolve(id, true).path("selling").asBoolean()).isTrue();
      product.setAutomaticExecutionEnabled(false);
      assertThatThrownBy(() -> service.resolve(id, true))
          .isInstanceOf(ResponseStatusException.class);
      product.setAutomaticExecutionEnabled(true);
      exp.setUnitPrice(new BigDecimal("80"));
      assertThatThrownBy(() -> service.resolve(id, false))
          .isInstanceOf(ResponseStatusException.class);
      exp.setStatus(ExperimentStatus.FINISHED);
      slot.setStatus(PdeProductionSlotStatus.RETIRED);
      var settled = service.resolveSettlement(id, "commercial-v1");
      assertThat(settled.path("priceBrl").decimalValue()).isEqualByComparingTo("79");
      assertThat(settled.path("selling").asBoolean()).isFalse();
      assertThatThrownBy(() -> service.resolveSettlement(id, "another-version"))
          .isInstanceOf(ResponseStatusException.class);
    }
  }
}
