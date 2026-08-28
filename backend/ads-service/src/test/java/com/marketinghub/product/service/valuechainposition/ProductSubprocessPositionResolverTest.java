package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a resolução auditável de subprocessos nos cards de produto. */
class ProductSubprocessPositionResolverTest {
  private final BusinessProcessDefinitionRepository processRepository =
      mock(BusinessProcessDefinitionRepository.class);
  private final CommercialPlanRepository planRepository = mock(CommercialPlanRepository.class);
  private final AgentTaskRepository taskRepository = mock(AgentTaskRepository.class);
  private final ProductSubprocessPositionResolver resolver =
      new ProductSubprocessPositionResolver(
          processRepository, planRepository, taskRepository, new ObjectMapper());

  /**
   * Mostra a atividade do processo pai e o primeiro subprocesso quando a execução ainda não
   * começou.
   */
  @Test
  void exposesFirstPlannedSubprocessWithoutFabricatingExecution() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of());

    var result = resolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("PLANNED");
    assertThat(result.currentActivityName()).isEqualTo("Congelar contrato da jornada");
    assertThat(result.currentSubprocessDefinitionId()).isNull();
    assertThat(result.nextSubprocessName()).isEqualTo("Criação e aprovação de criativos");
    assertThat(result.nextSubprocessObjective()).isEqualTo("Criativos aprovados e prontos.");
  }

  /** Usa a atividade persistida no subprocesso para destacar o atual e o seguinte. */
  @Test
  void exposesCurrentAndNextSubprocessFromAgentHistory() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    AgentTask task = new AgentTask();
    task.setId(88L);
    task.setProcessDefinition(children.get(0));
    task.setProcessActivityId("brief");
    task.setProcessActivityName("Definir briefing do criativo");
    task.setStatus("IN_PROGRESS");
    task.setUpdatedAt(Instant.parse("2026-08-24T12:00:00Z"));
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(task));

    var result = resolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("IN_PROGRESS");
    assertThat(result.currentSubprocessName()).isEqualTo("Criação e aprovação de criativos");
    assertThat(result.currentActivityName()).isEqualTo("Definir briefing do criativo");
    assertThat(result.nextSubprocessName()).isEqualTo("Geração de landing page");
  }

  /** Expõe o próximo subprocesso como atual somente depois do objetivo anterior comprovado. */
  @Test
  void exposesReadySubprocessWithoutFabricatingItsFirstExecution() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    AgentTask completedCreativeTask = new AgentTask();
    completedCreativeTask.setId(90L);
    completedCreativeTask.setProcessDefinition(children.getFirst());
    completedCreativeTask.setStatus("COMPLETED");
    completedCreativeTask.setUpdatedAt(Instant.parse("2026-08-25T12:00:00Z"));
    ProductStageMeasurementResolver measurements = mock(ProductStageMeasurementResolver.class);
    ProductSubprocessPositionResolver readyResolver =
        new ProductSubprocessPositionResolver(
            processRepository, planRepository, taskRepository, new ObjectMapper(), measurements);
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(completedCreativeTask));
    when(measurements.objectiveAchieved(product, children.getFirst())).thenReturn(true);
    when(measurements.resolveSubprocessMeasurements(product, children, children.get(1), null, true))
        .thenReturn(List.of());

    var result = readyResolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("PLANNED");
    assertThat(result.currentSubprocessSequenceNumber()).isEqualTo(2);
    assertThat(result.currentSubprocessName()).isEqualTo("Geração de landing page");
    assertThat(result.currentActivityName()).isNull();
    assertThat(result.nextSubprocessDefinitionId()).isNull();
  }

  /** Avança ao objetivo principal seguinte quando a landing já atingiu o gate persistido. */
  @Test
  void exposesCommercialIntegrationAfterApprovedLanding() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    AgentTask task = new AgentTask();
    task.setId(89L);
    task.setProcessDefinition(children.get(1));
    task.setProcessActivityId("commercial");
    task.setProcessActivityName("Executar revisão comercial independente");
    task.setStatus("COMPLETED");
    task.setUpdatedAt(Instant.parse("2026-08-26T12:00:00Z"));
    ProductStageMeasurementResolver measurements = mock(ProductStageMeasurementResolver.class);
    ProductSubprocessPositionResolver completedResolver =
        new ProductSubprocessPositionResolver(
            processRepository, planRepository, taskRepository, new ObjectMapper(), measurements);
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(task));
    when(measurements.objectiveAchieved(product, children.get(1))).thenReturn(true);
    when(measurements.resolveSubprocessMeasurements(product, children, null, null))
        .thenReturn(List.of());

    var result = completedResolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.currentSubprocessDefinitionId()).isNull();
    assertThat(result.currentActivityName())
        .isEqualTo("Integrar canal, checkout, acesso e eventos");
    assertThat(result.nextSubprocessDefinitionId()).isNull();
  }

  /**
   * Faz a instância BPM concluída prevalecer sobre a tentativa bloqueada preservada no histórico.
   */
  @Test
  void exposesNextActivityAfterCompletedRetrySupersedesBlockedTask() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    BusinessProcessActivityInstance completedInstance = new BusinessProcessActivityInstance();
    completedInstance.setId(129L);
    completedInstance.setStatus("COMPLETED");

    AgentTask blockedAttempt = new AgentTask();
    blockedAttempt.setId(244L);
    blockedAttempt.setProcessDefinition(children.get(1));
    blockedAttempt.setProcessActivityId("customer");
    blockedAttempt.setProcessActivityName("Avaliar percepção da cliente");
    blockedAttempt.setSourceReference("commercial-plan:4@v3:journey");
    blockedAttempt.setStatus("BLOCKED");
    blockedAttempt.setActivityInstance(completedInstance);
    blockedAttempt.setUpdatedAt(Instant.parse("2026-08-27T03:38:19Z"));

    AgentTask approvedRetry = new AgentTask();
    approvedRetry.setId(248L);
    approvedRetry.setProcessDefinition(children.get(1));
    approvedRetry.setProcessActivityId("customer");
    approvedRetry.setProcessActivityName("Avaliar percepção da cliente");
    approvedRetry.setSourceReference("commercial-plan:4@v3:journey");
    approvedRetry.setStatus("COMPLETED");
    approvedRetry.setActivityInstance(completedInstance);
    approvedRetry.setUpdatedAt(Instant.parse("2026-08-28T03:08:17Z"));

    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(approvedRetry, blockedAttempt));
    ProductStageMeasurementResolver measurements = mock(ProductStageMeasurementResolver.class);
    ProductSubprocessPositionResolver completedResolver =
        new ProductSubprocessPositionResolver(
            processRepository, planRepository, taskRepository, new ObjectMapper(), measurements);
    when(measurements.objectiveAchieved(product, children.get(1))).thenReturn(true);
    when(measurements.resolveSubprocessMeasurements(product, children, null, null))
        .thenReturn(List.of());

    var result = completedResolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.currentSubprocessDefinitionId()).isNull();
    assertThat(result.currentActivityName())
        .isEqualTo("Integrar canal, checkout, acesso e eventos");
  }

  /** Ignora bloqueio de uma execução anterior quando a tentativa vigente já concluiu o objetivo. */
  @Test
  void ignoresBlockedTaskFromPreviousExecutionReference() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    List<BusinessProcessDefinition> children = children();
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    AgentTask blockedAttempt =
        subprocessTask(
            244L,
            children.get(1),
            "BLOCKED",
            "commercial-plan:4@v3:journey:attempt:1",
            "2026-08-27T03:38:19Z");
    AgentTask approvedRetry =
        subprocessTask(
            248L,
            children.get(1),
            "COMPLETED",
            "commercial-plan:4@v3:journey:attempt:2",
            "2026-08-28T03:08:17Z");
    ProductStageMeasurementResolver measurements = mock(ProductStageMeasurementResolver.class);
    ProductSubprocessPositionResolver completedResolver =
        new ProductSubprocessPositionResolver(
            processRepository, planRepository, taskRepository, new ObjectMapper(), measurements);
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(children);
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(approvedRetry, blockedAttempt));
    when(measurements.objectiveAchieved(product, children.get(1))).thenReturn(true);
    when(measurements.resolveSubprocessMeasurements(product, children, null, null))
        .thenReturn(List.of());

    var result = completedResolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.currentActivityName())
        .isEqualTo("Integrar canal, checkout, acesso e eventos");
  }

  /** Não fabrica continuação quando a composição publicada termina no próprio subprocesso. */
  @Test
  void doesNotFabricateNextActivityAtEndOfComposition() {
    Product product = Product.builder().id(9L).build();
    BusinessProcessDefinition parent = parentProcess();
    parent.setDiagramJson(
        """
        {"nodes":[
          {"id":"start","type":"START","label":"Início"},
          {"id":"destination","type":"TASK","label":"Criar destino","subprocessCode":"landing-page-generation"},
          {"id":"end","type":"END","label":"Fim"}
        ],"flows":[
          {"from":"start","to":"destination"},{"from":"destination","to":"end"}
        ]}
        """);
    BusinessProcessDefinition landing = children().get(1);
    CommercialPlan plan = CommercialPlan.builder().id(31L).build();
    AgentTask completed =
        subprocessTask(
            248L, landing, "COMPLETED", "commercial-plan:4@v3:journey", "2026-08-28T03:08:17Z");
    ProductStageMeasurementResolver measurements = mock(ProductStageMeasurementResolver.class);
    ProductSubprocessPositionResolver completedResolver =
        new ProductSubprocessPositionResolver(
            processRepository, planRepository, taskRepository, new ObjectMapper(), measurements);
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(List.of(landing));
    when(planRepository.findByProductId(9L)).thenReturn(List.of(plan));
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:31@"))
        .thenReturn(List.of(completed));
    when(measurements.objectiveAchieved(product, landing)).thenReturn(true);
    when(measurements.resolveSubprocessMeasurements(product, List.of(landing), null, null))
        .thenReturn(List.of());

    var result = completedResolver.resolve(product, parent);

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.currentActivityName()).isNull();
    assertThat(result.nextSubprocessDefinitionId()).isNull();
  }

  /** Não cria informação de subprocesso em processo que não possui composição especializada. */
  @Test
  void reportsNotApplicableForProcessWithoutSubprocesses() {
    BusinessProcessDefinition parent = parentProcess();
    when(processRepository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            parent.getProcessCode(), "PUBLISHED"))
        .thenReturn(List.of());

    var result = resolver.resolve(Product.builder().id(6L).build(), parent);

    assertThat(result.trackingStatus()).isEqualTo("NOT_APPLICABLE");
    assertThat(result.subprocessCount()).isZero();
  }

  /** Cria a definição pai com duas delegações em sequência. */
  private BusinessProcessDefinition parentProcess() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(43L);
    process.setProcessCode("pde-communication-sales-journey");
    process.setDiagramJson(
        """
        {"nodes":[
          {"id":"start","type":"START","label":"Início"},
          {"id":"contract","type":"TASK","label":"Congelar contrato da jornada"},
          {"id":"creatives","type":"TASK","label":"Criar peças","subprocessCode":"creative-production-approval"},
          {"id":"destination","type":"TASK","label":"Criar destino","subprocessCode":"landing-page-generation"},
          {"id":"integration","type":"TASK","label":"Integrar canal, checkout, acesso e eventos"},
          {"id":"end","type":"END","label":"Fim"}
        ],"flows":[
          {"from":"start","to":"contract"},{"from":"contract","to":"creatives"},
          {"from":"creatives","to":"destination"},{"from":"destination","to":"integration"},
          {"from":"integration","to":"end"}
        ]}
        """);
    return process;
  }

  /** Cria os subprocessos publicados usados pelo processo pai. */
  private List<BusinessProcessDefinition> children() {
    return List.of(
        child(
            17L,
            "creative-production-approval",
            "Criação e aprovação de criativos",
            "Criativos aprovados e prontos."),
        child(
            18L,
            "landing-page-generation",
            "Geração de landing page",
            "Landing aprovada para publicação."));
  }

  /** Cria uma definição enxuta de subprocesso publicada. */
  private BusinessProcessDefinition child(Long id, String code, String name, String objective) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(name);
    process.setOutcomeDescription(objective);
    return process;
  }

  /** Monta uma tentativa de subprocesso com referência e ordem temporal explícitas. */
  private AgentTask subprocessTask(
      Long id,
      BusinessProcessDefinition process,
      String status,
      String sourceReference,
      String updatedAt) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(process);
    task.setProcessActivityId("customer");
    task.setProcessActivityName("Avaliar percepção da cliente");
    task.setStatus(status);
    task.setSourceReference(sourceReference);
    task.setUpdatedAt(Instant.parse(updatedAt));
    return task;
  }
}
