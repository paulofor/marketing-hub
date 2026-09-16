package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir regressão de posição, mistura de experimentos e retornos implícitos.
 */
class SalesFlowResolverTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final SalesFlowResolver resolver =
      new SalesFlowResolver(cycles, events, processes, chains, new LearningCycleJson(mapper));

  /** Reproduz adoção do Vega e conciliação sem vendas, sem fabricar conclusão de Hermes. */
  @Test
  void historicalBaselineMovesToDecisionWithoutRetroactiveApprovals() throws Exception {
    var flow =
        resolver.describe(
            cycle(), model(), List.of(event(0, "ADOPT_BASELINE", "{}"), measure(0, true)));
    assertThat(flow.currentActivityId()).isEqualTo("learningCycle");
    assertThat(flow.currentActivitySequenceNumber()).isEqualTo(4);
    assertThat(flow.activities())
        .extracting("state")
        .containsExactly("HISTORICAL", "NOT_APPLICABLE", "COMPLETED", "IN_PROGRESS");
    assertThat(flow.activities().getFirst().objectiveAchieved()).isFalse();
    assertThat(flow.activities().get(1).objectiveAchieved()).isFalse();
    assertThat(flow.transitions())
        .extracting("flowId")
        .containsExactly("historical-entry", "measurement-ready");
    verifyNoInteractions(cycles, events);
  }

  /** Mantém a entrega na frente quando existe venda sem comprovante de entrega. */
  @Test
  void paidSalesRequireDeliveryEvidence() throws Exception {
    var flow = resolver.describe(cycle(), model(), List.of(measure(2, false)));
    assertThat(flow.currentActivityId()).isEqualTo("delivery");
    assertThat(flow.state()).isEqualTo("BLOCKED");
    assertThat(flow.activities().get(3).state()).isEqualTo("WAITING");
    assertThat(flow.transitions().getLast().flowId()).isEqualTo("delivery-blocked");
  }

  /** Não exige entrega de compras cujos reembolsos foram integralmente conciliados. */
  @Test
  void reconciledRefundsDoNotCreateFictitiousDelivery() throws Exception {
    var proof = measure(0, false);
    proof.setEvidenceJson(proof.getEvidenceJson().replace("\"refunds\":0", "\"refunds\":2"));
    var flow = resolver.describe(cycle(), model(), List.of(proof));
    assertThat(flow.currentActivityId()).isEqualTo("learningCycle");
    assertThat(flow.activities().get(1).state()).isEqualTo("NOT_APPLICABLE");
    assertThat(flow.activities().get(1).reason()).contains("Reembolsos conciliados");
    assertThat(flow.activities().get(1).objectiveAchieved()).isFalse();
  }

  /** Permite decidir com vendas entregues sem dispensar a entrega pela ausência de tarefas. */
  @Test
  void deliveredSalesUnlockDecision() throws Exception {
    var flow = resolver.describe(cycle(), model(), List.of(measure(2, true)));
    assertThat(flow.currentActivityId()).isEqualTo("learningCycle");
    assertThat(flow.activities().get(1).objectiveAchieved()).isTrue();
  }

  /** Não interpreta fonte indisponível como zero vendas nem como consolidação concluída. */
  @Test
  void unavailableSourceKeepsConsolidationBlocked() throws Exception {
    var cycle = cycle();
    cycle.setStage("MEASUREMENT");
    var flow =
        resolver.describe(
            cycle,
            model(),
            List.of(event(0, "ADOPT_BASELINE", "{}"), event(1, "MEASUREMENT_BLOCKED", "{}")));
    assertThat(flow.currentActivityId()).isEqualTo("consolidate");
    assertThat(flow.state()).isEqualTo("BLOCKED");
    assertThat(flow.activities().get(1).state()).isEqualTo("WAITING");
    assertThat(flow.activities()).noneMatch(a -> a.objectiveAchieved());
  }

  /** Invalida a consolidação da passagem anterior quando a nova coleta ainda está bloqueada. */
  @Test
  void newPassageDoesNotReuseCompletedMeasurement() throws Exception {
    var cycle = cycle();
    cycle.setStage("MEASUREMENT");
    cycle.setBaseline(false);
    var flow =
        resolver.describe(
            cycle,
            model(),
            List.of(
                measure(0, true),
                event(2, "CONTINUE", "{}"),
                event(3, "MEASUREMENT_BLOCKED", "{}")));
    assertThat(flow.currentActivityId()).isEqualTo("consolidate");
    assertThat(flow.activities().get(2).objectiveAchieved()).isFalse();
    assertThat(flow.transitions().get(1).returnFlow()).isTrue();
    assertThat(flow.transitions().get(1).flowId()).isEqualTo("continue-collection");
  }

  /** Exige que cada espécie de retorno possua uma aresta identificável no BPM versionado. */
  @Test
  void allReturnsAreModeledAndKeepAudit() throws Exception {
    for (String action :
        List.of("FIX_MEASUREMENT", "CONTINUE", "ADJUST", "REWORK", "SCALE", "AUTHORIZE_SCALE")) {
      var flow =
          resolver.describe(cycle(), model(), List.of(measure(0, true), event(2, action, "{}")));
      var transition = flow.transitions().getLast();
      assertThat(transition.flowId()).as(action).isNotBlank();
      assertThat(transition.returnFlow()).as(action).isTrue();
      assertThat(transition.responsible()).isEqualTo("Operador local");
      assertThat(transition.evidenceReference()).isEqualTo("internal://fixture/2");
      assertThat(transition.occurredAt()).isNotNull();
    }
  }

  /** Faz a consulta atual retomar ciclo antigo da mesma cadeia sem alterar seu vínculo original. */
  @Test
  void currentChainResumesOriginalCycleAndBlocksOldOperation() throws Exception {
    var model = model();
    var chain = chain(model);
    var cycle = cycle();
    when(chains.findByProcessDefinitionId(model.getId())).thenReturn(List.of(chain));
    when(cycles.findByProductIdAndChainCodeOrderByIdDesc(4L, SalesFlowResolver.CHAIN_CODE))
        .thenReturn(List.of(cycle));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            SalesFlowResolver.PARENT_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(model));
    var originalChain = chain(model);
    originalChain.setId(13L);
    when(chains.findById(13L)).thenReturn(Optional.of(originalChain));
    when(events.findByCycleIdOrderByRevisionAsc(1L))
        .thenReturn(List.of(event(0, "ADOPT_BASELINE", "{}"), measure(0, true)));
    var flow = resolver.resolve(4L, model, null, null);
    assertThat(flow.chainDefinitionId()).isEqualTo(13L);
    assertThat(flow.navigationUrl()).contains("chainId=13", "productId=4", "cycleId=1");
    var operation = new BusinessProcessDefinition();
    operation.setProcessCode(SalesFlowResolver.OPERATION_CODE);
    assertThat(resolver.executionBlocker(4L, operation, "experiment:91"))
        .contains("não autoriza reiniciar");
    assertThat(resolver.executionBlocker(4L, operation, "experiment:90"))
        .contains("não corresponde");
    assertThat(resolver.resolve(5L, model, null, null)).isNull();
    verify(cycles, never()).save(any());
  }

  /** Reproduz o ciclo antigo consultado pela cadeia atual sem misturar processo e cadeia. */
  @Test
  void publishedUpgradeKeepsOriginalProcessAndExecutableContext() throws Exception {
    var original = model();
    original.setId(75L);
    original.setVersionNumber(6);
    var latest = model();
    latest.setId(78L);
    latest.setVersionNumber(7);
    var originalChain = chain(original);
    originalChain.setId(14L);
    var latestChain = chain(latest);
    latestChain.setId(15L);
    var cycle = cycle();
    cycle.setId(2L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setStage("PUBLICATION");
    cycle.setBaseline(false);
    when(chains.findById(14L)).thenReturn(Optional.of(originalChain));
    when(chains.findById(15L)).thenReturn(Optional.of(latestChain));
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    when(cycles.findByProductIdAndChainCodeOrderByIdDesc(4L, SalesFlowResolver.CHAIN_CODE))
        .thenReturn(List.of(cycle));
    when(processes.findById(75L)).thenReturn(Optional.of(original));
    var product = new com.marketinghub.product.Product();
    product.setId(4L);
    var execution =
        new LearningCycleExecutionContext(cycles, chains, processes, new LearningCycleJson(mapper));
    for (Long explicitCycle : java.util.Arrays.asList(null, 2L)) {
      var flow = resolver.resolve(4L, latest, 15L, explicitCycle);
      assertThat(flow.modelProcessDefinitionId()).isEqualTo(75L);
      assertThat(flow.chainDefinitionId()).isEqualTo(14L);
      assertThat(flow.currentActivitySequenceNumber()).isEqualTo(4);
      assertThat(
              execution.source(
                  flow.cycleId(),
                  product,
                  processes.findById(flow.modelProcessDefinitionId()).orElseThrow(),
                  false))
          .isEqualTo("experiment:92");
    }
    assertThat(resolver.resolve(4L, original, 14L, 2L).modelProcessDefinitionId()).isEqualTo(75L);
    assertThatThrownBy(() -> execution.source(2L, product, latest, false))
        .hasMessageContaining("não pertence à versão");
    verify(cycles, never()).save(any());
  }

  /** Mantém o modelo novo quando ele pertence de fato à cadeia persistida no ciclo. */
  @Test
  void newCycleUsesItsOwnPublishedModel() throws Exception {
    var latest = model();
    latest.setId(78L);
    latest.setVersionNumber(7);
    var chain = chain(latest);
    var cycle = cycle();
    cycle.setChainDefinitionId(chain.getId());
    when(chains.findById(chain.getId())).thenReturn(Optional.of(chain));
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    assertThat(
            resolver.resolve(4L, latest, chain.getId(), cycle.getId()).modelProcessDefinitionId())
        .isEqualTo(78L);
  }

  /** Recusa contexto explícito de outro produto antes de usar qualquer evidência. */
  @Test
  void explicitForeignCycleIsRejected() throws Exception {
    var model = model();
    when(chains.findByProcessDefinitionId(model.getId())).thenReturn(List.of(chain(model)));
    when(cycles.findById(1L)).thenReturn(Optional.of(cycle()));
    assertThatThrownBy(() -> resolver.resolve(5L, model, null, 1L))
        .hasMessageContaining("não pertence");
  }

  /** Fecha a passagem sem anunciar uma nova operação quando o ciclo foi encerrado. */
  @Test
  void stopHasNoImplicitNextOperation() throws Exception {
    var cycle = cycle();
    cycle.setStatus("STOPPED");
    cycle.setClosedAt(Instant.now());
    var flow = resolver.describe(cycle, model(), List.of(measure(0, true), event(2, "STOP", "{}")));
    assertThat(flow.currentActivityId()).isNull();
    assertThat(flow.state()).isEqualTo("COMPLETED");
    assertThat(flow.transitions().getLast().flowId()).isEqualTo("close-cycle");
  }

  /** Encaminha ao filho sem marcar o pai como execução ativa e provocar espera sem trabalho. */
  @Test
  void dispatchesOpalaBeforeWaitingForCommercialPublication() throws Exception {
    var routing = mock(com.marketinghub.opala.commercial.v1.service.OpalaCommercialRouting.class);
    org.springframework.test.util.ReflectionTestUtils.setField(resolver, "opalaRouting", routing);
    var cycle = cycle();
    cycle.setStage("PUBLICATION");
    cycle.setBaseline(false);
    var model = model();
    var diagram =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(model.getDiagramJson());
    ((com.fasterxml.jackson.databind.node.ArrayNode) diagram.path("nodes"))
        .addObject()
        .put("id", "commercialPreparation")
        .put("type", "TASK");
    model.setDiagramJson(diagram.toString());
    when(routing.target(cycle)).thenReturn(new BusinessProcessDefinition());
    var waiting = resolver.describe(cycle, model, List.of());
    assertThat(waiting.currentActivityId()).isEqualTo("commercialPreparation");
    assertThat(waiting.activities().getFirst().state()).isEqualTo("NOT_STARTED");
    assertThat(waiting.activities()).noneMatch(a -> "IN_PROGRESS".equals(a.state()));
    when(routing.completed(cycle)).thenReturn(true);
    var ready = resolver.describe(cycle, model, List.of());
    assertThat(ready.currentActivityId()).isEqualTo("learningCycle");
    assertThat(ready.activities().getFirst().objectiveAchieved()).isTrue();
    when(routing.target(cycle)).thenReturn(null);
    var legacy = resolver.describe(cycle, model, List.of());
    assertThat(legacy.activities().getFirst().state()).isEqualTo("NOT_APPLICABLE");
  }

  /** Gera o ciclo histórico mínimo com identidade explícita, independente de datas de tarefa. */
  private LearningSalesCycle cycle() {
    var cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(91L);
    cycle.setChainDefinitionId(13L);
    cycle.setChainCode(SalesFlowResolver.CHAIN_CODE);
    cycle.setStage("DECISION");
    cycle.setStatus("OPEN");
    cycle.setBaseline(true);
    return cycle;
  }

  /**
   * Lê o diagrama que será instalado pela migração, validando o contrato efetivamente versionado.
   */
  private BusinessProcessDefinition model() throws Exception {
    var resource =
        getClass()
            .getClassLoader()
            .getResourceAsStream("db/changelog/changesets/2026-09-09-pde-sales-flow-v6.yaml");
    String sql = new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    resource.close();
    int start = sql.indexOf("'{\"nodes\"") + 1;
    int end = sql.indexOf("',UTC_TIMESTAMP()", start);
    var model = new BusinessProcessDefinition();
    model.setId(74L);
    model.setProcessCode(SalesFlowResolver.PARENT_CODE);
    model.setDiagramJson(sql.substring(start, end));
    return model;
  }

  /** Cria a cadeia atual que referencia o modelo sem alterar a cadeia original do ciclo. */
  private BusinessProcessChainDefinition chain(BusinessProcessDefinition model) {
    var chain = new BusinessProcessChainDefinition();
    chain.setId(14L);
    chain.setVersionNumber(14);
    chain.setChainCode(SalesFlowResolver.CHAIN_CODE);
    chain.setStatus("PUBLISHED");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(model);
    item.setSequenceNumber(6);
    chain.getItems().add(item);
    return chain;
  }

  /** Produz evento imutável com os campos necessários à explicação da transição. */
  private LearningSalesCycleEvent event(long revision, String action, String evidence) {
    var event = new LearningSalesCycleEvent();
    event.setId(revision + 1);
    event.setRevision(revision);
    event.setAction(action);
    event.setFromStage("MEASUREMENT");
    event.setToStage("DECISION");
    event.setEvidenceJson(evidence);
    event.setSummary("Evidência local " + action);
    event.setOperatorName("Operador local");
    event.setEvidenceReference("internal://fixture/" + revision);
    event.setCreatedAt(Instant.parse("2026-09-09T06:00:00Z"));
    return event;
  }

  /** Produz uma fotografia válida e segregada sem contatos, despesas ou compras reais. */
  private LearningSalesCycleEvent measure(int sales, boolean delivered) {
    return event(
        1,
        "MEASURE",
        "{\"experimentId\":91,\"dataValid\":true,\"testDataExcluded\":true,\"netSales\":"
            + sales
            + ",\"refunds\":0,\"deliveryVerified\":"
            + delivered
            + "}");
  }
}
