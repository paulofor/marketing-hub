package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.planning.CommercialPlanVersion;
import com.marketinghub.planning.service.CommercialPlanExecutionSyncService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: distinguir revisão atual restrita de leitura histórica por ID exato. */
class FinancialPlanCurrentRevisionTest {
  /** Duas identidades usam o serviço real e conservam custos ausentes como desconhecidos. */
  @ParameterizedTest
  @ValueSource(longs = {95111, 95222})
  void currentRevisionUsesExactScopeAndPlan(long owner) throws Exception {
    var fixture = fixture(owner);
    when(fixture
            .revisions()
            .findFirstByScopeKindAndScopeIdAndEnvironmentAndCommercialPlanIdOrderByRevisionNumberDesc(
                "PRODUCT", owner, Environment.LIVE, owner + 100))
        .thenReturn(Optional.of(fixture.revision()));
    var current =
        fixture
            .service()
            .latestForPlan("PRODUCT", owner, Environment.LIVE, owner + 100)
            .orElseThrow();
    assertThat(current.scopeId()).isEqualTo(owner);
    assertThat(current.commercialPlanId()).isEqualTo(owner + 100);
    assertThat(current.stale()).isFalse();
    assertThat(current.analysis().costUsd()).isNull();
    assertThat(current.analysis().costCoverage()).isEqualTo("NOT_REPORTED");
    verify(fixture.revisions(), never()).findById(any());
  }

  /** Referência histórica explícita não é substituída pela consulta de revisão atual. */
  @Test
  void exactRevisionContractRemainsExact() throws Exception {
    var fixture = fixture(95111);
    when(fixture.revisions().findById(fixture.revision().getId()))
        .thenReturn(Optional.of(fixture.revision()));
    var historical =
        fixture.service().get("PRODUCT", 95111L, Environment.LIVE, fixture.revision().getId());
    assertThat(historical.id()).isEqualTo(fixture.revision().getId());
    verify(fixture.revisions(), never())
        .findFirstByScopeKindAndScopeIdAndEnvironmentAndCommercialPlanIdOrderByRevisionNumberDesc(
            any(), any(), any(), any());
  }

  /** Fonte inexistente ou plano não definido não provocam uma consulta JPA com ID vazio. */
  @Test
  void missingCurrentRevisionDoesNotInvokeExactLookup() throws Exception {
    var fixture = fixture(95111);
    assertThat(fixture.service().latestForPlan("PRODUCT", 95111L, Environment.LIVE, 95112L))
        .isEmpty();
    assertThat(fixture.service().latestForPlan("PRODUCT", 95111L, Environment.LIVE, null))
        .isEmpty();
    verify(fixture.revisions())
        .findFirstByScopeKindAndScopeIdAndEnvironmentAndCommercialPlanIdOrderByRevisionNumberDesc(
            "PRODUCT", 95111L, Environment.LIVE, 95112L);
    verify(fixture.revisions(), never()).findById(any());
  }

  /** Monta o serviço produtivo com registros sintéticos e avaliação determinística real. */
  private Fixture fixture(long owner) throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var input =
        (ObjectNode)
            json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    input.put("validUntil", LocalDate.now().plusDays(30).toString());
    var assumptions = json.treeToValue(input, PlanAssumptions.class);
    var revision = new FinancialPlanRevision();
    revision.setId(owner + 200);
    revision.setScopeKind("PRODUCT");
    revision.setScopeId(owner);
    revision.setEnvironment(Environment.LIVE);
    revision.setCommercialPlanId(owner + 100);
    revision.setCommercialPlanVersion(1);
    revision.setRevisionNumber(5);
    revision.setAssumptionsJson(json.writeValueAsString(assumptions));
    revision.setEvaluationJson(
        json.writeValueAsString(FinancialPlanCalculator.evaluate(assumptions)));
    revision.setCreatedAt(Instant.now());
    revision.setFinancialExecutionId(owner + 300);
    var revisions = mock(FinancialPlanRevisionRepository.class);
    var products = mock(ProductRepository.class);
    when(products.findById(owner))
        .thenReturn(
            Optional.of(
                Product.builder()
                    .id(owner)
                    .validationDefinitionVersion(assumptions.productVersion())
                    .build()));
    var versions = mock(CommercialPlanVersionRepository.class);
    var version = new CommercialPlanVersion();
    version.setVersionNumber(1);
    when(versions.findTopByPlanIdOrderByVersionNumberDesc(owner + 100))
        .thenReturn(Optional.of(version));
    var executions = mock(FinancialAgentExecutionRepository.class);
    var execution = new FinancialAgentExecution();
    execution.setId(owner + 300);
    execution.setStatus(FinancialAgentExecutionStatus.COMPLETED);
    when(executions.findById(owner + 300)).thenReturn(Optional.of(execution));
    var service =
        new FinancialPlanService(
            revisions,
            products,
            mock(ProductTypeDefinitionRepository.class),
            mock(CommercialPlanRepository.class),
            versions,
            executions,
            mock(FinancialAgentService.class),
            json,
            mock(Validator.class),
            mock(CommercialPlanExecutionSyncService.class),
            mock(CommercialPlanMilestoneRepository.class));
    return new Fixture(service, revisions, revision);
  }

  /** Agrupa apenas os componentes locais necessários para verificar a fronteira de leitura. */
  private record Fixture(
      FinancialPlanService service,
      FinancialPlanRevisionRepository revisions,
      FinancialPlanRevision revision) {}
}
