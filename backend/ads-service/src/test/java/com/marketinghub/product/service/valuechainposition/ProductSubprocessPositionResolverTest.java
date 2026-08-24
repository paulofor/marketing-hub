package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
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
          {"id":"end","type":"END","label":"Fim"}
        ],"flows":[
          {"from":"start","to":"contract"},{"from":"contract","to":"creatives"},
          {"from":"creatives","to":"destination"},{"from":"destination","to":"end"}
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
}
