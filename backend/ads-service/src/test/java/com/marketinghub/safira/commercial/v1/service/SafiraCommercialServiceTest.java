package com.marketinghub.safira.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskReviewSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: testar a orquestração idempotente e os callbacks comerciais de Safira. */
class SafiraCommercialServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final SafiraCommercialContext context = mock(SafiraCommercialContext.class);
  private final SafiraCommercialChecks checks = mock(SafiraCommercialChecks.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final SafiraCommercialService service =
      new SafiraCommercialService(
          context, checks, instances, definitions, processes, tasks, predecessors);
  private final Map<Long, BusinessProcessActivityInstance> saved = new HashMap<>();
  private final Map<String, BusinessProcessActivityDefinition> activities = new LinkedHashMap<>();
  private final Map<String, AgentTaskReviewSnapshot> reviews = new HashMap<>();
  private final Product product = Product.builder().id(10L).build();
  private final Experiment experiment = new Experiment();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private ObjectNode snapshot;

  /** Monta uma candidata sintética completa, sem depender de Mira ou de IDs de produção. */
  @BeforeEach
  void setup() throws Exception {
    product.setProductTypeDefinition(ProductTypeDefinition.builder().code("AI_PRODUCT").build());
    when(context.applies(product)).thenReturn(true);
    experiment.setId(301L);
    experiment.setProduct(product);
    process.setId(90L);
    process.setProcessCode(SafiraCommercialContext.CODE);
    process.setVersionNumber(2);
    process.setStatus("PUBLISHED");
    var scope =
        new SafiraCommercialContext.Scope(experiment, product, "public-v1", null, null, null, null);
    when(context.scope(anyString(), any(), any(Boolean.class))).thenReturn(scope);
    when(context.read(any())).thenAnswer(call -> json.readTree(call.getArgument(0, String.class)));
    snapshot =
        (ObjectNode)
            json.readTree(
                """
                {"productId":10,"experimentId":301,"productVersion":"public-v1",
                 "fingerprint":"safira-frozen","financialPlan":{"revision":1},
                 "experienceHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                """);
    when(context.snapshot("experiment:301")).thenAnswer(call -> snapshot.deepCopy());
    when(predecessors.readiness(any(), any(), anyString()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "ok"));

    for (String step :
        List.of(
            "journey",
            "economics",
            "humanExperienceReview",
            "commercialIntegrityReview",
            "ready")) {
      var definition = new BusinessProcessActivityDefinition();
      definition.setId((long) activities.size() + 1);
      definition.setActivityId(step);
      definition.setProcessDefinition(process);
      activities.put(step, definition);
      when(definitions.findByProcessDefinitionIdAndActivityId(90L, step))
          .thenReturn(Optional.of(definition));
    }
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq("experiment:301")))
        .thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(0, Long.class))));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq("experiment:301")))
        .thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(0, Long.class))));
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              BusinessProcessActivityInstance instance = call.getArgument(0);
              saved.put(instance.getActivityDefinition().getId(), instance);
              return instance;
            });
    when(tasks.findLatestReviewSnapshots(eq(90L), eq("experiment:301"), anyString(), any()))
        .thenAnswer(
            call -> {
              var review = reviews.get(call.getArgument(2, String.class));
              return review == null ? List.of() : List.of(review);
            });
    when(processes.findByProcessCodeAndVersionNumber(SafiraCommercialContext.CODE, 2))
        .thenReturn(Optional.of(process));
  }

  /** Conclui preparação, revisões e consolidação uma vez, sem duplicar ocorrências. */
  @Test
  void completesExactCandidateIdempotently() throws Exception {
    service.execute(process, activities.get("journey"), product, "experiment:301");
    service.execute(process, activities.get("economics"), product, "experiment:301");
    completeReview("humanExperienceReview");
    completeReview("commercialIntegrityReview");

    assertThat(
            service
                .execute(process, activities.get("ready"), product, "experiment:301")
                .objectiveAchieved())
        .isTrue();
    assertThat(service.completed(product, "experiment:301")).isTrue();
    service.execute(process, activities.get("ready"), product, "experiment:301");

    assertThat(saved).hasSize(3);
    assertThat(saved.get(activities.get("ready").getId()).getOccurrenceNumber()).isEqualTo(1);
  }

  /** Recusa parecer incompleto ou vinculado a outra fotografia comercial. */
  @Test
  void rejectsIncompleteOrStaleReview() throws Exception {
    service.execute(process, activities.get("journey"), product, "experiment:301");
    service.execute(process, activities.get("economics"), product, "experiment:301");
    var task = task("humanExperienceReview");
    var approved = approved();
    var incomplete = (ObjectNode) json.readTree(approved.resultJson());
    incomplete.withArray("gateChecks").remove(0);
    assertThatThrownBy(
            () ->
                service.apply(
                    task,
                    new CompleteAgentTaskRequest(incomplete.toString(), approved.evidenceJson())))
        .hasMessageContaining("omitiu");

    snapshot.put("fingerprint", "changed-after-review");
    assertThatThrownBy(() -> service.apply(task, approved)).hasMessageContaining("mudaram");
  }

  /** Oferece a criação dentro de Safira sem gravar ou chamar agente quando falta experimento. */
  @Test
  void preparesMissingCommercialContextWithoutExecution() {
    product.setMarketNiche(MarketNiche.builder().id(73L).build());
    assertThat(service.supportsReadinessWithoutExecutionContext()).isTrue();
    for (String source : new String[] {null, "", "product:10@agent-validation-v1"}) {
      var entry = service.readiness(process, activities.get("journey"), product, source);
      assertThat(entry.ready()).isFalse();
      assertThat(entry.actionLabel()).isEqualTo("Criar experimento comercial");
      assertThat(entry.navigationUrl()).isEqualTo("/experiments/new?nicheId=73&productId=10");
      assertThat(entry.targetProcessDefinitionId()).isNull();
      assertThat(entry.requirements()).hasSize(4);
      assertThatThrownBy(() -> service.execute(process, activities.get("journey"), product, source))
          .hasMessageContaining("experimento comercial explícito");
    }
    verify(context, never()).scope(any(), any(), any(Boolean.class));
    verifyNoInteractions(checks, instances, tasks, predecessors);
  }

  /** Mostra cadastro e pré-requisitos em vez de tentar executar atividades sem identidade. */
  @Test
  void guidesMissingNicheAndKeepsLaterActivitiesBlocked() {
    var entry = service.readiness(process, activities.get("journey"), product, null);
    assertThat(entry.navigationUrl()).isEqualTo("/products/10/edit");
    assertThat(entry.actionLabel()).isEqualTo("Completar cadastro comercial");
    for (String step : List.of("economics", "ready")) {
      var later = service.readiness(process, activities.get(step), product, null);
      assertThat(later.ready()).isFalse();
      assertThat(later.reason()).contains("Prepare primeiro o experimento comercial");
      assertThat(later.navigationUrl()).isNull();
    }
    verifyNoInteractions(checks, instances, tasks, predecessors);
  }

  /** Recusa produtos de outro tipo antes de projetar um formulário ou consultar o experimento. */
  @Test
  void rejectsAnotherTypeWithoutSuggestingSafiraExperiment() {
    when(context.applies(product)).thenReturn(false);
    var entry = service.readiness(process, activities.get("journey"), product, null);
    assertThat(entry.ready()).isFalse();
    assertThat(entry.reason()).contains("tipo cadastrado Safira");
    assertThat(entry.navigationUrl()).isEqualTo("/products/10/edit");
    verify(context, never()).scope(any(), any(), any(Boolean.class));
    verifyNoInteractions(checks, instances, tasks, predecessors);
  }

  /** Não converte identidade comercial inválida ou de outro produto em novo cadastro. */
  @Test
  void preservesInvalidOrForeignReferenceBlock() {
    when(context.scope("experiment:404", 10L, true))
        .thenThrow(new IllegalStateException("O experimento Safira pertence a outro produto."));
    var entry = service.readiness(process, activities.get("journey"), product, "experiment:404");
    assertThat(entry.ready()).isFalse();
    assertThat(entry.reason()).contains("outro produto");
    assertThat(entry.actionLabel()).isNotEqualTo("Criar experimento comercial");
    verifyNoInteractions(checks, instances, tasks, predecessors);
  }

  /** Persiste um parecer aprovado sintético como se tivesse retornado do agente responsável. */
  private void completeReview(String activity) throws Exception {
    var task = task(activity);
    var request = approved();
    service.apply(task, request);
    reviews.put(
        activity,
        new AgentTaskReviewSnapshot(
            task.getId(), "COMPLETED", request.evidenceJson(), request.resultJson()));
  }

  /** Cria uma tarefa Safira vinculada ao processo e à candidata sintética. */
  private AgentTask task(String activity) {
    var task = new AgentTask();
    task.setId((long) reviews.size() + 1);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activity);
    task.setSourceReference("experiment:301");
    return task;
  }

  /** Monta os dez gates obrigatórios com evidência verificável e escopo congelado. */
  private CompleteAgentTaskRequest approved() {
    var result = json.createObjectNode();
    result.put("decision", "APPROVED");
    result.putArray("requiredChanges");
    result.putArray("evidence").add("experiência pública capturada");
    var gates = result.putArray("gateChecks");
    for (String gate : SafiraCommercialService.REVIEW_GATES)
      gates
          .addObject()
          .put("gate", gate)
          .put("status", "PASS")
          .put("customerEvidence", "evidência " + gate);
    var evidence = json.createObjectNode();
    evidence.set("safiraScope", snapshot.deepCopy());
    return new CompleteAgentTaskRequest(result.toString(), evidence.toString());
  }
}
