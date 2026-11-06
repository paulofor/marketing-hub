package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget.SingleVideoBudgetResponse.Authorization;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: provar briefing privado rastreável sem transformar planejamento em parecer de
 * Atena.
 */
class SingleVideoIrisInputTest {
  /**
   * O vínculo persistido e o recibo atual governam a tarefa; metadados de outra execução são
   * recusados.
   */
  @Test
  void buildsOnlyForLinkedCurrentPrivateVideo() {
    var videos = mock(VideoProductionCycleRepository.class);
    var projects = mock(VideoProjectRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var budgets = mock(LearningCycleSingleVideoBudget.class);
    var tasks = mock(AgentTaskRepository.class);
    var json = new LearningCycleJson(new ObjectMapper().findAndRegisterModules());
    var service = new SingleVideoIrisInput(videos, projects, cycles, budgets, tasks, json);
    var task = new AgentTask();
    task.setId(7884L);
    task.setSourceReference("experiment:7114");
    task.setProcessActivityId("communicationContract");
    var agent = new Agent();
    agent.setAgentKey("communication-director");
    task.setAssignedAgent(agent);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    task.setProcessDefinition(process);
    var video = new VideoProductionCycle();
    video.setId(7047L);
    video.setIrisAgentTaskId(7884L);
    video.setVideoProjectId(7014L);
    video.setProductId(7004L);
    video.setExperimentId(7114L);
    var project = new VideoProject();
    project.setId(7014L);
    project.setProductId(7004L);
    project.setExperimentId(7114L);
    project.setCampaignKey("video-private");
    when(videos.findByIrisAgentTaskId(7884L)).thenReturn(List.of(video));
    when(projects.findById(7014L)).thenReturn(Optional.of(project));
    when(budgets.protectedReceipt(project))
        .thenReturn(
            new Authorization(
                7128L,
                "local",
                7014L,
                7069L,
                new BigDecimal("15"),
                "test",
                "local",
                Instant.now(),
                true));
    var cycle = new LearningSalesCycle();
    cycle.setInheritedLearningJson("{\"events\":[]}");
    cycle.setStage("PLANNING");
    when(cycles.findByExperimentId(7114L)).thenReturn(Optional.of(cycle));
    var input = service.forTask(task);
    assertThat(input)
        .containsEntry("scope", "SINGLE_PRIVATE_VIDEO_ONLY")
        .containsEntry("newStrategyApproved", false)
        .containsEntry("qualityApproved", false)
        .containsEntry("receiptId", 7128L);
    assertThat(String.valueOf(input.get("inputSha256"))).matches("[a-f0-9]{64}");
    task.setSourceReference("experiment:9999");
    assertThatThrownBy(() -> service.forTask(task)).hasMessageContaining("contexto");
    task.setId(999L);
    assertThat(service.forTask(task)).isEmpty();
    verifyNoInteractions(tasks);
  }
}
