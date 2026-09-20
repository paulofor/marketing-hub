package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.controller.FinancialPlanController;
import com.marketinghub.financialplan.v1.service.prepareplan.*;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.planning.*;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: proteger sugestões, fontes, isolamento e concorrência da preparação simples.
 */
class FinancialPlanPreparationTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final FinancialPlanRevisionRepository revisions =
      mock(FinancialPlanRevisionRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final CommercialPlanVersionRepository versions =
      mock(CommercialPlanVersionRepository.class);
  private final FinancialAgentService plutus = mock(FinancialAgentService.class);
  private final Product product = new Product();
  private final CommercialPlan plan = new CommercialPlan();
  private FinancialPlanService service;

  /** Configura fontes sintéticas e persistência capturável sem acessar qualquer serviço externo. */
  @BeforeEach
  void setup() {
    service =
        new FinancialPlanService(
            revisions,
            products,
            mock(ProductTypeDefinitionRepository.class),
            plans,
            versions,
            mock(FinancialAgentExecutionRepository.class),
            plutus,
            json,
            Validation.buildDefaultValidatorFactory().getValidator());
    product.setId(95101L);
    product.setValidationDefinitionVersion("fixture-v1");
    product.setCurrentPriceBrl(new BigDecimal("67"));
    var type = new ProductTypeDefinition();
    type.setId(951L);
    type.setCode("LOW_TICKET_DIGITAL_PRODUCT");
    product.setProductTypeDefinition(type);
    when(products.findById(95101L)).thenReturn(Optional.of(product));
    when(products.findLockedById(95101L)).thenReturn(Optional.of(product));
    plan.setId(95102L);
    plan.setExpectedCacBrl(new BigDecimal("25"));
    when(plans.findByProductId(95101L)).thenReturn(List.of(plan));
    when(plans.findIdsByProductId(95101L)).thenReturn(List.of(plan.getId()));
    var version = new CommercialPlanVersion();
    version.setVersionNumber(4);
    when(versions.findTopByPlanIdOrderByVersionNumberDesc(plan.getId()))
        .thenReturn(Optional.of(version));
    when(revisions.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              FinancialPlanRevision r = i.getArgument(0);
              r.setId(999L);
              return r;
            });
  }

  /** Monta requisição com contexto canônico e somente duas decisões operacionais. */
  private PreparePlanRequest request(int revision, int days, Boolean ai) {
    return new PreparePlanRequest(revision, plan.getId(), 4, "fixture-v1", days, ai);
  }

  /** Lê premissas completas independentes dos produtos e tarifas produtivos. */
  private PlanAssumptions complete() throws Exception {
    var node = json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) node)
        .putObject("preparation")
        .put("supportDays", 7)
        .put("personalizedAi", true);
    return json.treeToValue(node, PlanAssumptions.class);
  }

  /** Converte premissas em histórico segregado com a mesma identidade comercial. */
  private FinancialPlanRevision prior(PlanAssumptions a) throws Exception {
    var r = new FinancialPlanRevision();
    r.setId(998L);
    r.setScopeKind("PRODUCT");
    r.setScopeId(product.getId());
    r.setProductId(product.getId());
    r.setProductTypeId(951L);
    r.setEnvironment(Environment.LIVE);
    r.setRevisionNumber(1);
    r.setName("Plano sintético");
    r.setCommercialPlanId(plan.getId());
    r.setCommercialPlanVersion(4);
    r.setCreatedAt(Instant.now());
    r.setAssumptionsJson(json.writeValueAsString(a));
    r.setEvaluationJson(json.writeValueAsString(FinancialPlanCalculator.evaluate(a)));
    when(revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", product.getId(), Environment.LIVE))
        .thenReturn(List.of(r));
    return r;
  }

  /** Sugere sete dias e IA sem transformar fontes ausentes em margem aprovada. */
  @Test
  void defaultsAndMissingSources() {
    var preview = service.preparation(product.getId(), Environment.LIVE);
    assertThat(preview.supportDays()).isEqualTo(7);
    assertThat(preview.personalizedAi()).isTrue();
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, null), null);
    assertThat(saved.assumptions().priceBrl()).isEqualByComparingTo("67");
    assertThat(saved.assumptions().maximumCacBrl()).isEqualByComparingTo("25");
    assertThat(saved.assumptions().periodDays()).isEqualTo(30);
    assertThat(saved.assumptions().preparation().supportDays()).isEqualTo(7);
    assertThat(saved.assumptions().ai().perAttempt()).isNull();
    assertThat(saved.assumptions().costs().otherVariableBrl()).isNull();
    assertThat(saved.canRequestAnalysis()).isFalse();
    assertThat(saved.evaluation().status()).isEqualTo("MISSING_INPUTS");
    assertThat(
            FinancialPlanPreparation.prepare(
                saved.assumptions(), request(0, 7, true), plan, saved.assumptions().priceBrl()))
        .isEqualTo(saved.assumptions());
    verifyNoInteractions(plutus);
  }

  /** Sugere trinta dias para tipos diferentes sem atrelar regra ao nome Capella. */
  @Test
  void otherTypeSuggestion() {
    product.getProductTypeDefinition().setCode("PDE");
    assertThat(service.preparation(product.getId(), Environment.TEST).supportDays()).isEqualTo(30);
  }

  /** Mantém custos e cálculos documentados quando as escolhas permanecem compatíveis. */
  @Test
  void preservesCompatibleSources() throws Exception {
    var a = complete();
    var result = FinancialPlanPreparation.prepare(a, request(0, 7, true), plan, a.priceBrl());
    assertThat(result.costs()).isEqualTo(a.costs());
    assertThat(result.ai()).isEqualTo(a.ai());
    assertThat(FinancialPlanCalculator.evaluate(result))
        .isEqualTo(FinancialPlanCalculator.evaluate(a));
    assertThat(result.evidence()).contains(a.evidence());
    var next = FinancialPlanPreparation.prepare(result, request(1, 7, true), plan, a.priceBrl());
    assertThat(next).isEqualTo(result);
  }

  /** Mudar suporte invalida seu custo sem apagar custos independentes nem alterar período. */
  @Test
  void changingSupportInvalidatesOnlySupportCost() throws Exception {
    var a = complete();
    var result = FinancialPlanPreparation.prepare(a, request(0, 30, true), plan, a.priceBrl());
    assertThat(result.costs().supportBrl()).isNull();
    assertThat(result.costs().initialAiBrl()).isEqualTo(a.costs().initialAiBrl());
    assertThat(result.periodDays()).isEqualTo(a.periodDays());
  }

  /** Desligar IA remove apenas geração variável; religar exige fonte de tarifa. */
  @Test
  void togglingAiPreservesInitialInvestmentAndRequiresRealPricing() throws Exception {
    var a = complete();
    var without = FinancialPlanPreparation.prepare(a, request(0, 7, false), plan, a.priceBrl());
    assertThat(without.ai().perAttempt()).isZero();
    assertThat(without.scenarios()).allMatch(s -> s.attemptsPerCustomer() == 0);
    assertThat(without.costs().initialAiBrl()).isEqualTo(a.costs().initialAiBrl());
    var with = FinancialPlanPreparation.prepare(without, request(1, 7, true), plan, a.priceBrl());
    assertThat(with.ai().perAttempt()).isNull();
    assertThat(FinancialPlanCalculator.evaluate(with).status()).isEqualTo("MISSING_INPUTS");
  }

  /** Edição avançada não pode apresentar custo zero enquanto promete geração personalizada. */
  @Test
  void rejectsPricingContradictingPersonalization() throws Exception {
    var a = complete();
    var without = FinancialPlanPreparation.prepare(a, request(0, 7, false), plan, a.priceBrl());
    var node = json.valueToTree(without);
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("preparation"))
        .put("personalizedAi", true);
    assertThat(
            FinancialPlanCalculator.evaluate(json.treeToValue(node, PlanAssumptions.class))
                .blockers())
        .anyMatch(b -> b.contains("geração personalizada"));
  }

  /** Recusa submissão após outra revisão e não busca fontes de LIVE durante TEST. */
  @Test
  void concurrencyAndEnvironmentIsolation() throws Exception {
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 14, false), null);
    prior(saved.assumptions());
    assertThat(service.preparation(product.getId(), Environment.LIVE).personalizedAi()).isFalse();
    assertThat(service.preparation(product.getId(), Environment.LIVE).supportDays()).isEqualTo(14);
    assertThatThrownBy(
            () -> service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("contexto mudou");
    assertThat(service.preparation(product.getId(), Environment.TEST).sourceRevisionId()).isNull();
    assertThatThrownBy(() -> service.preparation(95103L, Environment.LIVE))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Alteração comercial impede adotar revisão anterior como se as fontes fossem atuais. */
  @Test
  void commercialDriftAndAmbiguityBlock() throws Exception {
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    var old = prior(saved.assumptions());
    old.setCommercialPlanVersion(3);
    assertThat(service.preparation(product.getId(), Environment.LIVE).canPrepare()).isFalse();
    assertThatThrownBy(
            () -> service.prepare(product.getId(), Environment.LIVE, request(1, 7, true), null))
        .hasMessageContaining("mudou de contexto");
    when(plans.findByProductId(product.getId())).thenReturn(List.of(plan, new CommercialPlan()));
    assertThat(service.preparation(product.getId(), Environment.TEST).canPrepare()).isFalse();
  }

  /** Valida limites pelo endpoint real, inclusive números fracionários que não podem truncar. */
  @Test
  void httpRejectsInvalidDaysAndAcceptsTwoChoices() throws Exception {
    var mvc = MockMvcBuilders.standaloneSetup(new FinancialPlanController(service)).build();
    for (String days : List.of("0", "-1", "1.5", "3661", "null")) {
      mvc.perform(
              post("/api/financial-plans/v1/products/95101/preparation")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"expectedRevision\":0,\"commercialPlanId\":95102,\"commercialPlanVersion\":4,\"productVersion\":\"fixture-v1\",\"supportDays\":"
                          + days
                          + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/api/financial-plans/v1/products/95101/preparation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(0, 7, true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assumptions.preparation.personalizedAi").value(true));
  }
}
