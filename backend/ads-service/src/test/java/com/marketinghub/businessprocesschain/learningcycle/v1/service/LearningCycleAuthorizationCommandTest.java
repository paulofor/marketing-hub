package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: comprovar que a tela oferece uma autorização financeira atômica e segura. */
class LearningCycleAuthorizationCommandTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleEvidence evidence = mock(LearningCycleEvidence.class);
  private final LearningCycleVideoEvidence videoEvidence = mock(LearningCycleVideoEvidence.class);
  private final LearningCycleVideoBudget videoBudget = mock(LearningCycleVideoBudget.class);
  private final BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
  private final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          experiments,
          chains,
          processes,
          null,
          new LearningCycleJson(new ObjectMapper()),
          null,
          evidence,
          videoEvidence,
          null,
          null);
  private LearningSalesCycle cycle;
  private Experiment experiment;

  /** Monta o ciclo realista de autorização com sua homologação vigente. */
  @BeforeEach
  void setUp() {
    Instant now = Instant.parse("2026-09-15T04:00:00Z");
    cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setProcessDefinitionId(75L);
    cycle.setStage("AUTHORIZATION");
    cycle.setStatus("OPEN");
    cycle.setProductVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    cycle.setBudgetLimitBrl(new BigDecimal("100.00"));
    cycle.setRevision(13L);
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    cycle.setVersionChangedAt(now.minusSeconds(60));

    experiment = Experiment.builder().id(92L).build();
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(75L);
    process.setVersionNumber(6);
    process.setDiagramJson("{\"nodes\":[]}");
    chain.setId(14L);
    chain.setItems(List.of());
    LearningSalesCycleEvent validation = new LearningSalesCycleEvent();
    validation.setId(16L);
    validation.setCycleId(2L);
    validation.setRevision(12L);
    validation.setFromStage("VALIDATION");
    validation.setToStage("AUTHORIZATION");
    validation.setAction("COMPLETE");
    validation.setOperatorName("Marketing Hub");
    validation.setSummary("Homologação concluída.");
    validation.setEvidenceReference("agent-validation-gate:289");
    validation.setEvidenceJson("{\"approvalInstanceId\":289}");
    validation.setRequestJson("{}");
    validation.setRequestKey("00000000-0000-0000-0000-000000000016");
    validation.setCreatedAt(now);

    when(products.findById(4L)).thenReturn(Optional.of(Product.builder().id(4L).build()));
    when(cycles.findByProductIdOrderByIdDesc(4L)).thenReturn(List.of(cycle));
    when(cycles.findByPreviousCycleId(2L)).thenReturn(Optional.empty());
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    when(processes.findById(75L)).thenReturn(Optional.of(process));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    when(events.findByCycleIdOrderByRevisionAsc(2L)).thenReturn(List.of(validation));
    when(evidence.approvals(cycle))
        .thenReturn(List.of(new LearningCycleResponse.ApprovalOption(289L, "Gate #289")));
    ReflectionTestUtils.setField(service, "videoBudget", videoBudget);
  }

  /** Permite confirmar o teto na própria ação sem exigir edição duplicada do experimento. */
  @Test
  void exposesAuthorizationCommandWhenExperimentBudgetIsMissing() {
    LearningCycleResponse.CommandOption command = authorizationCommand();

    assertThat(command.available()).isTrue();
    assertThat(command.reason()).contains("backend conferirá");
  }

  /** Libera o comando quando o teto operacional coincide exatamente com a decisão do ciclo. */
  @Test
  void exposesAuthorizationCommandWhenExperimentBudgetMatchesCycle() {
    experiment.setMediaSpendLimit(new BigDecimal("100.00"));

    LearningCycleResponse.CommandOption command = authorizationCommand();

    assertThat(command.available()).isTrue();
  }

  /** Mantém a publicação orientada ao processo comercial em vez de abandonar o ciclo no detalhe. */
  @Test
  void directsPublicationToCommercialHomologation() {
    cycle.setStage("PUBLICATION");
    BusinessProcessDefinition commercial = new BusinessProcessDefinition();
    commercial.setId(56L);
    commercial.setProcessCode("pde-commercial-homologation-activation");
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setProcessDefinition(commercial);
    chain.setItems(List.of(item));

    LearningCycleResponse response = service.list(4L).getFirst();

    assertThat(response.workUrl())
        .isEqualTo(
            "/products/4/value-chain-history/processes/56/activities?learningCycleId=2&chainId=14");
  }

  /** Obtém a opção exibida pela API para concluir a etapa de autorização. */
  private LearningCycleResponse.CommandOption authorizationCommand() {
    return service.list(4L).getFirst().commands().stream()
        .filter(command -> "COMPLETE".equals(command.action()))
        .findFirst()
        .orElseThrow();
  }
}
