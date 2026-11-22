package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.automation.v1.*;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: homologar continuidade isolada sem apagar provas nem iniciar trabalho pago. */
class ProcessVersionContinuityServiceTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  private final ProcessRunEventRepository events = mock(ProcessRunEventRepository.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private final ObjectMapper json = new ObjectMapper();
  private final ProcessVersionContinuityLookup lookup =
      new ProcessVersionContinuityLookup(events, json);
  private final ProcessVersionContinuityService service =
      new ProcessVersionContinuityService(
          products,
          cycles,
          processes,
          chains,
          definitions,
          instances,
          runs,
          events,
          activities,
          lookup,
          json);
  private final List<ProcessRunEvent> journal = new ArrayList<>();
  private final List<BusinessProcessActivityInstance> copies = new ArrayList<>();
  private final List<BusinessProcessActivityDefinition> targetDefinitions = new ArrayList<>();
  private ProcessRun source;
  private LearningSalesCycle cycle;
  private BusinessProcessDefinition target;
  private long productId;

  /** Monta outro contexto sintético a cada cenário, mantendo oito provas e três pendências. */
  void setup(long id) {
    productId = id;
    var product = Product.builder().id(id).name("Produto sintético").build();
    cycle = new LearningSalesCycle();
    cycle.setId(id + 100);
    cycle.setProductId(id);
    cycle.setChainDefinitionId(id + 200);
    cycle.setExperimentId(id + 300);
    cycle.setStatus("OPEN");
    var old = new BusinessProcessDefinition();
    old.setId(id + 400);
    old.setProcessCode("pde-construction-approval");
    old.setStatus("RETIRED");
    target = new BusinessProcessDefinition();
    target.setId(id + 500);
    target.setProcessCode(old.getProcessCode());
    target.setStatus("PUBLISHED");
    target.setDiagramJson("{}");
    source = new ProcessRun();
    source.setId(id + 600);
    source.setProductId(id);
    source.setProcessDefinitionId(old.getId());
    source.setChainDefinitionId(cycle.getChainDefinitionId());
    source.setLearningCycleId(cycle.getId());
    source.setSourceReference("experiment:" + cycle.getExperimentId());
    source.setStatus("PAUSED");
    source.setKnownCostUsd(new BigDecimal("113.33"));
    source.setCompletedActivities(8);
    var chain = chain(cycle.getChainDefinitionId(), old);
    var published = chain(id + 201, target);
    when(products.findById(id)).thenReturn(Optional.of(product));
    when(products.findLockedById(id)).thenReturn(Optional.of(product));
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(id, cycle.getId())).thenReturn(Optional.of(cycle));
    when(chains.findById(chain.getId())).thenReturn(Optional.of(chain));
    when(chains.findFirstByChainCodeAndStatusOrderByVersionNumberDesc("synthetic", "PUBLISHED"))
        .thenReturn(Optional.of(published));
    when(processes.findById(old.getId())).thenReturn(Optional.of(old));
    when(processes.findById(target.getId())).thenReturn(Optional.of(target));
    when(runs
            .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                id, old.getId(), chain.getId(), cycle.getId(), source.getSourceReference()))
        .thenReturn(Optional.of(source));
    when(runs.findById(source.getId())).thenReturn(Optional.of(source));
    when(events.continuations(id, cycle.getId(), source.getSourceReference()))
        .thenAnswer(
            i ->
                journal.stream()
                    .filter(e -> "VERSION_CONTINUATION".equals(e.getEventType()))
                    .toList());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var e = (ProcessRunEvent) i.getArgument(0);
              e.setId(900L + journal.size());
              journal.add(e);
              return e;
            });
    when(runs.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var r = (ProcessRun) i.getArgument(0);
              r.setId(800L);
              return r;
            });
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var a = (BusinessProcessActivityInstance) i.getArgument(0);
              a.setId(1000L + copies.size());
              copies.add(a);
              return a;
            });
    var oldDefinitions = new ArrayList<BusinessProcessActivityDefinition>();
    var groups = new ArrayList<ProductProcessActivityExecutionGroupResponse>();
    for (int n = 0; n < 11; n++) {
      String activity = "step-" + n;
      var before = definition(old, n, activity);
      var after = definition(target, n, activity);
      oldDefinitions.add(before);
      targetDefinitions.add(after);
      when(definitions.findByProcessDefinitionIdAndActivityId(target.getId(), activity))
          .thenReturn(Optional.of(after));
      var original = new BusinessProcessActivityInstance();
      original.setId(2000L + n);
      original.setActivityDefinition(before);
      original.setSourceReference(source.getSourceReference());
      original.setObjectiveAchieved(n < 8);
      original.setStatus(n < 8 ? "COMPLETED" : "BLOCKED");
      original.setObjectiveEvidenceJson("{\"artifact\":\"proof-" + n + "\"}");
      original.setEvidenceQuality("DIRECT");
      original.setKnownCostUsd(new BigDecimal("2.50"));
      when(instances.findById(original.getId())).thenReturn(Optional.of(original));
      var group = mock(ProductProcessActivityExecutionGroupResponse.class);
      when(group.activityId()).thenReturn(activity);
      when(group.activityName()).thenReturn(activity);
      when(group.activityInstanceId()).thenReturn(original.getId());
      when(group.selectedVersionActivity()).thenReturn(true);
      when(group.objectiveAchieved()).thenReturn(n < 8);
      var task =
          mock(
              com.marketinghub
                  .businessprocess
                  .execution
                  .service
                  .recentExecutions
                  .BusinessProcessActivityExecutionResponse
                  .class);
      when(task.taskId()).thenReturn(3000L + n);
      when(task.processDefinitionId()).thenReturn(old.getId());
      when(task.status()).thenReturn(n < 8 ? "COMPLETED" : "BLOCKED");
      when(group.tasks()).thenReturn(List.of(task));
      groups.add(group);
    }
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(old.getId()))
        .thenReturn(oldDefinitions);
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(target.getId()))
        .thenReturn(targetDefinitions);
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.activities()).thenReturn(groups);
    when(activities.productProcessExecutions(
            old.getId(), id, cycle.getId(), chain.getId(), false, source.getSourceReference()))
        .thenReturn(history);
  }

  /** Constrói uma cadeia sem dependências externas. */
  private BusinessProcessChainDefinition chain(long id, BusinessProcessDefinition process) {
    var c = new BusinessProcessChainDefinition();
    c.setId(id);
    c.setChainCode("synthetic");
    c.setStatus("PUBLISHED");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    c.setItems(new ArrayList<>(List.of(item)));
    return c;
  }

  /** Constrói contrato idêntico com identidade relacional diferente. */
  private BusinessProcessActivityDefinition definition(
      BusinessProcessDefinition process, int n, String activity) {
    var d = new BusinessProcessActivityDefinition();
    d.setId(process.getId() * 100 + n);
    d.setProcessDefinition(process);
    d.setActivityId(activity);
    d.setDefinitionJson("{\"id\":\"" + activity + "\",\"description\":\"Aceite original\"}");
    return d;
  }

  /** Produz comando com conferência vinculada à versão consultada. */
  private ProcessVersionContinuityService.Request request(
      ProcessVersionContinuityService.Preview p, Map<String, String> evidence) {
    return new ProcessVersionContinuityService.Request(
        p.fingerprint(), "Operador sintético", "Continuar construção", evidence);
  }

  /** Preserva oito entregas, identidade e custos; recarga/repetição não duplicam ocorrências. */
  @ParameterizedTest
  @ValueSource(longs = {4, 71})
  void preservesEightDeliveriesAndIsIdempotent(long id) {
    setup(id);
    var preview = service.preview(id, cycle.getId(), target.getId());
    assertThat(preview.canPrepare()).isTrue();
    var result = service.prepare(id, cycle.getId(), target.getId(), request(preview, Map.of()));
    assertThat(result.successorRunId()).isEqualTo(800L);
    assertThat(lookup.reusedTasks(id, cycle.getId(), source.getSourceReference(), target.getId()))
        .containsExactlyInAnyOrder(3000L, 3001L, 3002L, 3003L, 3004L, 3005L, 3006L, 3007L);
    assertThat(copies)
        .hasSize(8)
        .allSatisfy(
            a -> {
              assertThat(a.isObjectiveAchieved()).isTrue();
              assertThat(a.getKnownCostUsd()).isNull();
              assertThat(a.getSourceReference()).isEqualTo(source.getSourceReference());
            });
    assertThat(source.getStatus()).isEqualTo("PAUSED");
    assertThat(source.getCompletedActivities()).isEqualTo(8);
    assertThat(source.getKnownCostUsd()).isEqualByComparingTo("113.33");
    assertThat(cycle.getChainDefinitionId()).isEqualTo(id + 200);
    assertThat(cycle.getExperimentId()).isEqualTo(id + 300);
    service.prepare(id, cycle.getId(), target.getId(), request(preview, Map.of()));
    assertThat(copies).hasSize(8);
    verify(runs, times(1)).saveAndFlush(any());
    assertThat(
            lookup.permits(
                id + 1,
                cycle.getId(),
                source.getSourceReference(),
                cycle.getChainDefinitionId(),
                target.getId()))
        .isFalse();
    verify(products, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /** Uma versão já adotada pode ser sucedida sem voltar à execução histórica inicial. */
  @Test
  void previewsSuccessiveRetirementFromAdoptedVersion() {
    setup(71);
    var first = service.preview(productId, cycle.getId(), target.getId());
    service.prepare(productId, cycle.getId(), target.getId(), request(first, Map.of()));
    target.setStatus("RETIRED");
    var successor = new BusinessProcessDefinition();
    successor.setId(971L);
    successor.setProcessCode(target.getProcessCode());
    successor.setStatus("PUBLISHED");
    successor.setDiagramJson("{}");
    when(processes.findById(successor.getId())).thenReturn(Optional.of(successor));
    when(chains.findFirstByChainCodeAndStatusOrderByVersionNumberDesc("synthetic", "PUBLISHED"))
        .thenReturn(Optional.of(chain(999L, successor)));
    var adoptedRun = new ProcessRun();
    adoptedRun.setId(800L);
    adoptedRun.setStatus("PAUSED");
    when(runs
            .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                productId,
                target.getId(),
                cycle.getChainDefinitionId(),
                cycle.getId(),
                source.getSourceReference()))
        .thenReturn(Optional.of(adoptedRun));
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.activities()).thenReturn(List.of());
    when(activities.productProcessExecutions(
            target.getId(),
            productId,
            cycle.getId(),
            cycle.getChainDefinitionId(),
            false,
            source.getSourceReference()))
        .thenReturn(history);
    var next = service.preview(productId, cycle.getId(), successor.getId());
    assertThat(next.sourceProcessId()).isEqualTo(target.getId());
    assertThat(next.sourceRunId()).isEqualTo(800L);
    assertThat(next.targetProcessId()).isEqualTo(successor.getId());
    assertThat(next.canPrepare()).isFalse(); // Sem provas carregadas, a passagem fica protegida.
  }

  /** Conserva artefato alterado como pendência e impede texto genérico de fabricar aceite. */
  @ParameterizedTest
  @ValueSource(longs = {4, 71})
  void preparesPartialReuseWithoutFabricatingAcceptance(long id) {
    setup(id);
    targetDefinitions
        .getFirst()
        .setDefinitionJson(
            "{\"id\":\"step-0\",\"description\":\"Aceite original Novo requisito\"}");
    var p = service.preview(productId, cycle.getId(), target.getId());
    assertThat(p.outputs().getFirst().criteria())
        .extracting(ProcessVersionContinuityService.Criterion::status)
        .containsExactly("HISTORICAL_ACCEPTANCE", "REVALIDATION_PENDING");
    assertThatThrownBy(
            () ->
                service.prepare(
                    productId,
                    cycle.getId(),
                    target.getId(),
                    request(p, Map.of("step-0", "Tudo compatível"))))
        .isInstanceOf(ResponseStatusException.class);
    verify(runs, never()).saveAndFlush(any());
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(copies).hasSize(8);
    assertThat(copies.getFirst().isObjectiveAchieved()).isFalse();
    assertThat(copies.getFirst().getStatus()).isEqualTo("BLOCKED");
    var savedRun = org.mockito.ArgumentCaptor.forClass(ProcessRun.class);
    verify(runs).saveAndFlush(savedRun.capture());
    assertThat(savedRun.getValue().getStatus()).isEqualTo("PAUSED");
    assertThat(savedRun.getValue().getCompletedActivities()).isEqualTo(7);
    assertThat(savedRun.getValue().getRemainingActivities()).isEqualTo(4);

    assertThat(copies.getFirst().getObjectiveEvidenceJson()).contains("proof-0");
    assertThat(copies.stream().filter(BusinessProcessActivityInstance::isObjectiveAchieved))
        .hasSize(7);
    assertThat(lookup.reusedTasks(id, cycle.getId(), source.getSourceReference(), target.getId()))
        .doesNotContain(3000L);
    assertThat(journal.getLast().getDetailsJson()).contains("revalidation", "REVALIDATION_PENDING");
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(copies).hasSize(8);
    verify(products, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /** Ausência de prova não ganha aceite mesmo quando o texto do contrato não mudou. */
  @Test
  void missingEvidenceRemainsPending() {
    setup(72);
    instances.findById(2000L).orElseThrow().setObjectiveEvidenceJson(null);
    var p = service.preview(productId, cycle.getId(), target.getId());
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(copies.getFirst().isObjectiveAchieved()).isFalse();
    assertThat(
            lookup.reusedTasks(
                productId, cycle.getId(), source.getSourceReference(), target.getId()))
        .doesNotContain(3000L);
  }

  /** Protege mudança concorrente, ciclo encerrado e identidade de outro produto. */
  @Test
  void rejectsStaleClosedAndForeignContexts() {
    setup(9);
    var p = service.preview(productId, cycle.getId(), target.getId());
    target.setDiagramJson("{\"changed\":true}");
    assertThatThrownBy(
            () -> service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of())))
        .isInstanceOf(ResponseStatusException.class);
    cycle.setStatus("CLOSED");
    assertThat(service.preview(productId, cycle.getId(), target.getId()).canPrepare()).isFalse();
    assertThatThrownBy(() -> service.preview(999L, cycle.getId(), target.getId()))
        .isInstanceOf(ResponseStatusException.class);
    verify(runs, never()).saveAndFlush(any());
  }

  /** Carrega a proteção financeira original para o destino sem reexecutar qualquer dispensa. */
  @Test
  void inheritsFinancialPause() {
    setup(10);
    var pause = new ProcessRunEvent();
    pause.setMessage("Conferir gastos");
    pause.setDetailsJson("{\"reasonCode\":\"COST_RECONCILIATION\"}");
    when(events.findFirstByRunIdAndEventTypeOrderByIdDesc(source.getId(), "PAUSE_CONTEXT"))
        .thenReturn(Optional.of(pause));
    var p = service.preview(productId, cycle.getId(), target.getId());
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(journal)
        .anySatisfy(
            e -> {
              assertThat(e.getRunId()).isEqualTo(800L);
              assertThat(e.getEventType()).isEqualTo("PAUSE_CONTEXT");
              assertThat(e.getDetailsJson()).isEqualTo(pause.getDetailsJson());
            });
  }

  /**
   * Confere a passagem no resolvedor real: rejeita antes do recibo e aceita depois sem trocar
   * ciclo.
   */
  @Test
  void admitsPublishedVersionOnlyAfterPersistedAdoption() {
    setup(31);
    var context =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service
            .LearningCycleExecutionContext(
            cycles,
            chains,
            processes,
            mock(
                com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleJson
                    .class));
    org.springframework.test.util.ReflectionTestUtils.setField(
        context, "versionContinuity", lookup);
    var product = products.findById(productId).orElseThrow();
    assertThatThrownBy(() -> context.source(cycle.getId(), product, target, true))
        .isInstanceOf(ResponseStatusException.class);
    var p = service.preview(productId, cycle.getId(), target.getId());
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(context.source(cycle.getId(), product, target, true))
        .isEqualTo(source.getSourceReference());
    assertThat(cycle.getChainDefinitionId()).isEqualTo(231L);
  }

  /** Mudança estrutural e atividade removida não podem ser liberadas por uma nota textual. */
  @Test
  void refusesStructuralContractChanges() {
    setup(32);
    targetDefinitions.getFirst().setDefinitionJson("{\"id\":\"step-0\",\"schema\":\"novo\"}");
    assertThat(service.preview(productId, cycle.getId(), target.getId()).canPrepare()).isFalse();
    verify(runs, never()).saveAndFlush(any());
  }

  /** Reproduz oito entregas com critérios novos sem apagar originais ou inventar aceites. */
  @Test
  void revalidatesAllEightChangedOutputs() {
    setup(41);
    var evidence = new HashMap<String, String>();
    for (int i = 0; i < 8; i++) {
      targetDefinitions
          .get(i)
          .setDefinitionJson("{\"id\":\"step-" + i + "\",\"description\":\"Critério novo\"}");
      evidence.put("step-" + i, "Prova sintética " + i + " conferida no requisito novo");
    }
    var p = service.preview(productId, cycle.getId(), target.getId());
    assertThat(p.outputs().stream().filter(ProcessVersionContinuityService.Output::accepted))
        .hasSize(8);
    service.prepare(productId, cycle.getId(), target.getId(), request(p, Map.of()));
    assertThat(copies).hasSize(8);
    assertThat(source.getCompletedActivities()).isEqualTo(8);
    assertThat(copies).noneMatch(BusinessProcessActivityInstance::isObjectiveAchieved);
    assertThat(
            lookup.reusedTasks(
                productId, cycle.getId(), source.getSourceReference(), target.getId()))
        .isEmpty();
  }
}
