package com.marketinghub.safira.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: preservar provas e limites ao preparar Safira para Instagram Ads. */
class SafiraCommercialChecksTest {
  private final ExperimentReadinessService readiness = mock(ExperimentReadinessService.class);
  private final SafiraCommercialChecks checks = new SafiraCommercialChecks(readiness);
  private final Experiment experiment = new Experiment();
  private final CommercialPlan plan = new CommercialPlan();
  private SafiraCommercialContext.Scope scope;
  private ObjectNode snapshot;

  /** Monta uma jornada sintética aprovada, independente de participantes ou IDs de produção. */
  @BeforeEach
  void setup() {
    experiment.setId(710L);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setInstagramAccount(mock(InstagramAccount.class));
    experiment.setSampleSize(6);
    experiment.setUnitPrice(new BigDecimal("39"));
    plan.setStatus(CommercialPlanStatus.DRAFT);
    plan.setMainChannel("Instagram Ads (Meta Ads)");
    scope =
        new SafiraCommercialContext.Scope(
            experiment,
            Product.builder().id(44L).currentPriceBrl(new BigDecimal("39")).build(),
            "candidate-v2",
            null,
            null,
            PdeProductionSlot.builder()
                .status(PdeProductionSlotStatus.READY)
                .validationStatus("OK")
                .build(),
            plan);
    snapshot = new ObjectMapper().createObjectNode();
    for (String field :
        List.of(
            "singlePain",
            "funnelPromise",
            "primaryCta",
            "desireTerritoryCode",
            "freeReward",
            "backendUrl",
            "experienceHash",
            "deliverable",
            "deliveryMode",
            "checkoutMonetization",
            "riskReversal",
            "valueUnit",
            "valueEvidenceMetric")) {
      snapshot.put(field, "evidência sintética " + field);
    }
    snapshot.putObject("desireTerritory").put("code", "CLARITY");
    snapshot.putObject("publishedExperience").put("version", "candidate-v2");
    snapshot.put("publicUrl", "https://example.test/candidate");
    snapshot.put("checkoutUrl", "https://example.test/checkout");
    snapshot.put("checkoutPriceBrl", new BigDecimal("39"));
    snapshot.put("commercialJourneyIntegrated", true);
    snapshot.putArray("creatives").addObject().put("id", 23);
    snapshot.putArray("savedAudience").addObject().put("id", 91);
    var commercial = snapshot.putObject("commercialPlan");
    commercial.put("mainOffer", "entrega útil");
    commercial.put("successCriteria", "compra e uso");
    commercial.put("stopCriteria", "falha ou limite");
    commercial.put("targetAudience", "público salvo e aprovado por Atena");
    var summary = mock(ExperimentReadinessSummaryDto.class);
    when(readiness.summarize(710L)).thenReturn(summary);
    when(summary.runningGateRequirements())
        .thenReturn(
            List.of(
                new ExperimentRunningGateRequirementDto(
                    "CREATIVE_APPROVED", "material", true, "ok", "ok"),
                new ExperimentRunningGateRequirementDto(
                    "TARGETING_READY", "canal", true, "ok", "ok")));
  }

  /** Aceita Instagram Ads somente com identidade, público e plano coerentes. */
  @Test
  void acceptsPaidInstagramJourneyWithOfficialIdentity() {
    assertThatCode(() -> checks.check("journey", scope, snapshot)).doesNotThrowAnyException();
  }

  /** Recusa canal direto e rótulos que não declaram literalmente Instagram Ads. */
  @Test
  void rejectsDirectAndContradictoryChannel() {
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
        .hasMessageContaining("somente aquisição paga no Instagram");
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    for (String invalidChannel : List.of("Meta", "DIRECT_ONE_TO_ONE_NOT_APPROVED")) {
      plan.setMainChannel(invalidChannel);
      assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
          .hasMessageContaining("Instagram Ads");
    }
  }

  /** Recusa a ausência da identidade ou do público oficial da campanha. */
  @Test
  void rejectsMissingInstagramIdentityOrSavedAudience() {
    experiment.setInstagramAccount(null);
    assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
        .hasMessageContaining("identidade pública do Instagram");
    experiment.setInstagramAccount(mock(InstagramAccount.class));
    ((com.fasterxml.jackson.databind.node.ArrayNode) snapshot.path("savedAudience")).removeAll();
    assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
        .hasMessageContaining("público oficial");
  }

  /** Mantém prova pública, checkout, material aprovado e economia obrigatórios. */
  @Test
  void paidChannelDoesNotBypassCommercialGates() {
    snapshot.put("commercialJourneyIntegrated", false);
    assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
        .hasMessageContaining("Processo 4");
    snapshot.put("commercialJourneyIntegrated", true);
    snapshot.remove("checkoutUrl");
    assertThatThrownBy(() -> checks.check("journey", scope, snapshot))
        .hasMessageContaining("checkout");
    assertThatThrownBy(() -> checks.check("economics", scope, snapshot))
        .hasMessageContaining("financeiro LIVE");
  }
}
