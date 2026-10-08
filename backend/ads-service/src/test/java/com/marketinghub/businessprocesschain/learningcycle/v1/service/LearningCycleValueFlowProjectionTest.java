package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: preservar a dependência real e a diferença entre dado ausente, teste e venda
 * conciliada.
 */
class LearningCycleValueFlowProjectionTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final KitPrototypeCapabilities capabilities = mock(KitPrototypeCapabilities.class);
  private final KitPrivateArtifactRepository artifacts = mock(KitPrivateArtifactRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCyclePrototypeContext prototype = mock(LearningCyclePrototypeContext.class);
  private final LearningCycleValueFlowProjection projection =
      new LearningCycleValueFlowProjection(
          capabilities, artifacts, tasks, events, prototype, new LearningCycleJson(mapper));

  /**
   * Mostra Dédalo como dependência de Psique sem fabricar execução, decisão humana ou métrica zero.
   */
  @Test
  void preservesImplementationOwnerAndUnknownMetrics() {
    var cycle = fixture();
    var flow = projection.resolve(cycle, work());
    assertThat(flow.resolvingResponsible()).contains("Dédalo");
    assertThat(flow.awaitingResponsible()).isEqualTo("Psique");
    assertThat(flow.activeExecution()).isFalse();
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.marketMeasurement()).isNull();
    assertThat(flow.deliverables().getFirst().usableOutput())
        .contains("não comprova implementação");
  }

  /**
   * O relógio de espera usa a entrega do pacote mais recente, sem manter a data da especificação
   * anterior.
   */
  @Test
  void measuresWaitFromLatestUsableDelivery() {
    var cycle = fixture();
    Instant delivered = Instant.now().minusSeconds(10);
    var result = mock(KitPrivateArtifactRepository.Summary.class);
    when(result.getStatus()).thenReturn("READY");
    when(result.getFinishedAt()).thenReturn(delivered);
    when(artifacts.summaries(55L)).thenReturn(List.of(result));
    var flow = projection.resolve(cycle, work());
    assertThat(flow.stalledSince()).isEqualTo(delivered);
    assertThat(flow.readyPackages()).isEqualTo(1);
    assertThat(flow.activeExecution()).isFalse();
  }

  /**
   * Conciliação válida da própria ocorrência aceita zero medido; dado inválido mantém o valor
   * desconhecido.
   */
  @Test
  void acceptsOnlyValidReconciledCommercialMeasurement() {
    var cycle = fixture();
    var event = new LearningSalesCycleEvent();
    event.setId(71L);
    event.setCycleId(55L);
    event.setAction("MEASURE");
    event.setToStage("AUTHORIZATION");
    event.setEvidenceJson(
        "{\"netSales\":0,\"revenueBrl\":0,\"dataValid\":false,\"testDataExcluded\":true}");
    when(events.findByCycleIdOrderByRevisionAsc(55L)).thenReturn(List.of(event));
    assertThat(projection.resolve(cycle, work()).marketMeasurement()).isNull();
    event.setEvidenceJson(event.getEvidenceJson().replace("false", "true"));
    assertThat(projection.resolve(cycle, work()).marketMeasurement().path("netSales").asInt(-1))
        .isZero();
  }

  /** Configura um produto independente para prevenir exceções por identidade de Capella. */
  private LearningSalesCycle fixture() {
    var cycle = new LearningSalesCycle();
    cycle.setId(55L);
    cycle.setProductId(75L);
    cycle.setExperimentId(95L);
    cycle.setProductVersion("other-kit-v1");
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setCreatedAt(Instant.EPOCH);
    when(capabilities.resolve(cycle))
        .thenReturn(
            new Capability(
                true,
                "barber-v1",
                "Contrato aprovado",
                "https://pagamentopalf.site/mh-api/pde/kit/private/v1/prototype"));
    when(prototype.resolve(cycle)).thenReturn(Optional.empty());
    when(tasks.findFunctionalSnapshots(eq("experiment:95"), anySet(), eq(Instant.EPOCH)))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    999L,
                    117L,
                    "pde-construction-approval",
                    "access",
                    "landing-generator",
                    "COMPLETED",
                    Instant.EPOCH,
                    Instant.EPOCH,
                    "{}")));
    return cycle;
  }

  /** Mantém a atividade real de homologação independente como quem aguarda a implementação. */
  private Work work() {
    return new Work(
        117L,
        3,
        "Construção",
        "technicalHomologation",
        6,
        "Homologação",
        "Psique",
        "BLOCKED",
        "Falta implementação.",
        "/products/75/value-chain-history");
  }
}
