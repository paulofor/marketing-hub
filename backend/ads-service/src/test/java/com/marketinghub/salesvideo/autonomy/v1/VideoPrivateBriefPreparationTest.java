package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: provar a passagem DRAFT → fala de Íris → entrada privada sem autoaprovação. */
class VideoPrivateBriefPreparationTest {
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  final AgentTaskService taskService = mock(AgentTaskService.class);
  final SalesVideoScriptRepository scripts = mock(SalesVideoScriptRepository.class);
  final SalesVideoProfileRepository profiles = mock(SalesVideoProfileRepository.class);
  final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final VideoPrivateBriefPreparation service =
      new VideoPrivateBriefPreparation(
          tasks, definitions, taskService, scripts, profiles, json, projects);
  VideoProject project;
  VideoProductionCycle cycle;
  AgentTask iris;

  /** Mantém produto e experimento sintéticos, roteiro ausente e entrega de Íris ainda pendente. */
  @BeforeEach
  void setup() {
    project = new VideoProject();
    project.setId(7014L);
    project.setProductId(7004L);
    project.setExperimentId(7114L);
    project.setSalesVideoProfileId(7069L);
    project.setTenantId("test");
    project.setCampaignKey("private-test-version");
    project.setTargetDurationSeconds(32);
    project.setScriptText("Rascunho antigo sem aprovação");
    project.setCaptionPlan("Texto | antigo");
    cycle = new VideoProductionCycle();
    cycle.setId(7047L);
    cycle.setVideoProjectId(project.getId());
    cycle.setCreatedAt(Instant.parse("2026-11-05T22:00:00Z"));
    cycle.setIrisAgentTaskId(7884L);
    var agent = new Agent();
    agent.setAgentKey("communication-director");
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    process.setId(7700L);
    iris = new AgentTask();
    iris.setId(7884L);
    iris.setAssignedAgent(agent);
    iris.setProcessDefinition(process);
    iris.setProcessActivityId("communicationContract");
    iris.setSourceReference("experiment:7114");
    iris.setCreatedAt(cycle.getCreatedAt().plusSeconds(1));
    iris.setStatus("PENDING");
    iris.setResultJson(
        "{\"functionalOutput\":{\"audiovisualBrief\":\"[ABERTURAS] Abra. [FALA_FINAL] Use suas"
            + " roupas para encontrar outra combinação. Salve a escolha e veja novamente."
            + " [PROVA_E_LIMITES] Demonstração privada. [DIRECAO_VISUAL] Mostrar a tela.\"}}");
    when(tasks.findById(iris.getId())).thenReturn(Optional.of(iris));
    when(profiles.findById(project.getSalesVideoProfileId()))
        .thenReturn(
            Optional.of(
                SalesVideoProfile.builder()
                    .id(project.getSalesVideoProfileId())
                    .product(Product.builder().id(project.getProductId()).build())
                    .tenantId("test")
                    .status(SalesVideoStatus.DRAFT)
                    .build()));
  }

  /** Nenhuma fala técnica vira aprovação humana ou parecer sobre o MP4 ainda inexistente. */
  @Test
  void privateScriptUsesAcceptedSpeechAndKeepsOriginalEvidence() throws Exception {
    assertThat(service.completed(cycle, project)).isFalse();
    assertThatThrownBy(() -> service.materialize(cycle, project)).hasMessageContaining("Plutus");
    cycle.setFinancialDecision("APPROVED");
    assertThatThrownBy(() -> service.materialize(cycle, project)).hasMessageContaining("concluiu");
    iris.setStatus("COMPLETED");
    assertThat(service.completed(cycle, project)).isTrue();
    service.materialize(cycle, project);
    var capture = ArgumentCaptor.forClass(SalesVideoScript.class);
    verify(scripts).saveAndFlush(capture.capture());
    var script = capture.getValue();
    assertThat(script.getStatus()).isEqualTo(SalesVideoScriptStatus.PRIVATE_READY);
    assertThat(script.getSource()).isEqualTo(SalesVideoScriptSource.IRIS_PRIVATE_BRIEF);
    assertThat(script.getApprovedAt()).isNull();
    assertThat(script.getApprovedBy()).isNull();
    assertThat(script.getScriptText()).doesNotContain("DIRECAO_VISUAL", "PROVA_E_LIMITES");
    assertThat(project.getCaptionPlan().split(" \\| ")).hasSize(8);
    assertThat(project.getCaptionPlan().replace(" | ", " ")).isEqualTo(script.getScriptText());
    var source = json.readTree(script.getSourceEvidenceJson());
    assertThat(source.path("previousScriptText").asText()).contains("Rascunho antigo");
    assertThat(source.path("irisTaskId").asLong()).isEqualTo(iris.getId());
    assertThat(source.path("humanApproval").asBoolean()).isFalse();
  }

  /** BRIEF_ONLY antigo, outro experimento e outro responsável permanecem fora do novo pedido. */
  @Test
  void rejectsHistoricalAndForeignBriefs() {
    iris.setCreatedAt(cycle.getCreatedAt().minusSeconds(1));
    assertThatThrownBy(() -> service.completed(cycle, project))
        .hasMessageContaining("outra execução");
    iris.setCreatedAt(cycle.getCreatedAt());
    iris.setSourceReference("experiment:9999");
    assertThatThrownBy(() -> service.completed(cycle, project))
        .hasMessageContaining("outra execução");
    iris.setSourceReference("experiment:7114");
    iris.getAssignedAgent().setAgentKey("videomaker");
    assertThatThrownBy(() -> service.completed(cycle, project))
        .hasMessageContaining("outra execução");
    verify(scripts, never()).saveAndFlush(any());
  }

  /** O caminho editorial já aprovado é reutilizado, sem inferência ou alteração desnecessária. */
  @Test
  void keepsPreviouslyApprovedScript() {
    when(scripts.findFirstByProfileIdAndStatusOrderByVersionDesc(
            7069L, SalesVideoScriptStatus.APPROVED))
        .thenReturn(Optional.of(new SalesVideoScript()));
    service.materialize(cycle, project);
    verifyNoInteractions(taskService);
    verify(scripts, never()).saveAndFlush(any());
  }

  /** Tarefa atual é reaproveitada sem repetir Íris ou reaproveitar o pedido histórico. */
  @Test
  void reusesCurrentTaskOnly() {
    service.request(cycle, project);
    service.request(cycle, project);
    assertThat(cycle.getIrisAgentTaskId()).isEqualTo(7884L);
    verifyNoInteractions(taskService);
  }
}
