package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.contributiontarget.SaveContributionTargetRequest;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.SavePlanRequest;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: impedir perda de premissas, mistura de contextos e decisões econômicas
 * implícitas.
 */
class FinancialContributionTargetServiceTest {
  private final FinancialPlanService plans = mock(FinancialPlanService.class);
  private final FinancialContributionTargetService targets =
      new FinancialContributionTargetService(
          plans, Validation.buildDefaultValidatorFactory().getValidator());
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Monta a revisão imutável com dados sintéticos e campos desconhecidos. */
  private PlanView source(long productId, long revisionId, Environment environment)
      throws Exception {
    var tree = json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree).putNull("minimumMarginPercent");
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("ai")).putNull("perAttempt");
    return new PlanView(
        revisionId,
        "PRODUCT",
        productId,
        environment,
        "Revisão sintética",
        2,
        null,
        34L,
        4,
        "Origem local",
        Instant.EPOCH,
        json.treeToValue(tree, PlanAssumptions.class),
        null,
        false,
        List.of(),
        false,
        null);
  }

  /** Exercita o caso observado e outra identidade, mantendo todo o conteúdo além da meta. */
  @ParameterizedTest
  @CsvSource({"11,12,LIVE", "710,912,LIVE", "95111,1912,TEST"})
  void preservesAssumptionsAndScope(long productId, long revisionId, Environment environment)
      throws Exception {
    var source = source(productId, revisionId, environment);
    when(plans.get("PRODUCT", productId, environment, revisionId)).thenReturn(source);
    when(plans.list("PRODUCT", productId, environment)).thenReturn(List.of(source));
    var request = new SaveContributionTargetRequest(revisionId, new BigDecimal("31.25"));
    targets.save(productId, environment, request, "Operador sintético");
    var saved = ArgumentCaptor.forClass(SavePlanRequest.class);
    verify(plans).create(eq("PRODUCT"), eq(productId), eq(environment), saved.capture());
    assertThat(saved.getValue().expectedRevision()).isEqualTo(source.revision());
    assertThat(saved.getValue().commercialPlanId()).isEqualTo(source.commercialPlanId());
    assertThat(saved.getValue().createdBy()).isEqualTo("Operador sintético");
    var before = json.valueToTree(source.assumptions());
    var after = json.valueToTree(saved.getValue().assumptions());
    ((com.fasterxml.jackson.databind.node.ObjectNode) before)
        .put("minimumMarginPercent", new BigDecimal("31.25"));
    assertThat(after).isEqualTo(before);
    assertThat(source.assumptions().minimumMarginPercent()).isNull();
    verify(plans, never()).requestAnalysis(any(), any(), any());
  }

  /** A referência antiga não sobrescreve uma revisão mais recente ou outro contexto. */
  @Test
  void staleReferenceCannotOverwriteLatest() throws Exception {
    var source = source(11, 12, Environment.LIVE);
    when(plans.get("PRODUCT", 11L, Environment.LIVE, 12L)).thenReturn(source);
    when(plans.list("PRODUCT", 11L, Environment.LIVE))
        .thenReturn(List.of(source(11, 13, Environment.LIVE)));
    assertThatThrownBy(
            () ->
                targets.save(
                    11L,
                    Environment.LIVE,
                    new SaveContributionTargetRequest(12L, new BigDecimal("31.25")),
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
    verify(plans, never()).create(any(), any(), any(), any());
  }

  /** Não converte uma meta ausente ou inválida em decisão do usuário. */
  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "100", "null"})
  void refusesInvalidTargetBeforeConsultingData(String value) {
    var target = "null".equals(value) ? null : new BigDecimal(value);
    assertThatThrownBy(
            () ->
                targets.save(
                    11L, Environment.LIVE, new SaveContributionTargetRequest(12L, target), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
    verifyNoInteractions(plans);
  }

  /** Uma mesma meta já registrada não cria outra revisão nem muda sua autoria. */
  @Test
  void currentIdenticalTargetIsReused() throws Exception {
    var original = source(11, 12, Environment.LIVE);
    var tree = json.valueToTree(original.assumptions());
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree)
        .put("minimumMarginPercent", new BigDecimal("31.25"));
    var source =
        new PlanView(
            original.id(),
            original.scope(),
            original.scopeId(),
            original.environment(),
            original.name(),
            original.revision(),
            original.templateId(),
            original.commercialPlanId(),
            original.commercialPlanVersion(),
            original.createdBy(),
            original.createdAt(),
            json.treeToValue(tree, PlanAssumptions.class),
            original.evaluation(),
            original.stale(),
            original.pendingActions(),
            false,
            null);
    when(plans.get("PRODUCT", 11L, Environment.LIVE, 12L)).thenReturn(source);
    when(plans.list("PRODUCT", 11L, Environment.LIVE)).thenReturn(List.of(source));
    assertThat(
            targets.save(
                11L,
                Environment.LIVE,
                new SaveContributionTargetRequest(12L, new BigDecimal("31.250")),
                null))
        .isSameAs(source);
    verify(plans, never()).create(any(), any(), any(), any());
  }
}
