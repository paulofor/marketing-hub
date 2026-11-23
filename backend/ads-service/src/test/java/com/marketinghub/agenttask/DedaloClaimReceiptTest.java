package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir que replay de transporte ou preparação demorada repita inferência. */
class DedaloClaimReceiptTest {
  /**
   * Preserva identidade em produtos distintos e concede início somente uma vez após preparação
   * longa.
   */
  @ParameterizedTest
  @ValueSource(longs = {6001L, 7001L})
  void replaysReceiptAndFencesSingleStart(long id) {
    var repo = mock(AgentTaskRepository.class);
    var task = new AgentTask();
    var owner = Agent.builder().id(4L).agentKey("landing-generator").build();
    var process = new BusinessProcessDefinition();
    process.setId(id + 10);
    process.setProcessCode("pde-construction-approval");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    task.setId(id);
    task.setAssignedAgent(owner);
    task.setProcessDefinition(process);
    task.setProcessActivityId("journey");
    task.setSourceReference("experiment:" + id);
    task.setStatus("IN_PROGRESS");
    task.setExecutionClaimId(UUID.randomUUID().toString());
    task.setExecutionClaimPhase("CLAIMED");
    task.setReceivedAt(Instant.parse("2026-01-01T00:00:00Z"));
    when(repo.findById(id)).thenReturn(Optional.of(task));
    when(repo.findLockedById(id)).thenReturn(Optional.of(task));
    when(repo.findByExecutionClaimId(task.getExecutionClaimId())).thenReturn(Optional.of(task));
    var now = Instant.parse("2026-01-01T00:10:00Z");
    var service =
        new AgentTaskService(
            repo,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            Clock.fixed(now, ZoneOffset.UTC));
    assertThat(
            service
                .claimDedaloReceipt(
                    "pde-construction-approval", "journey", task.getExecutionClaimId())
                .orElseThrow()
                .taskId())
        .isEqualTo(id);
    verify(repo, never()).findPendingForClaim(anyString(), anyString(), anyString());
    when(repo.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of(task));
    Optional<?> recovered =
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            service,
            "recoverOrphanedDedaloLease",
            "landing-generator",
            "pde-construction-approval",
            "journey",
            null);
    assertThat(recovered).isEmpty();
    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(task.getExecutionError()).isNull();
    service.heartbeatDedaloClaim(id, task.getExecutionClaimId());
    assertThat(task.getExecutionClaimPhase()).isEqualTo("PREPARING");
    assertThat(task.getExecutionClaimHeartbeatAt()).isEqualTo(now);
    assertThat(task.getEstimatedCostUsd()).isNull();
    assertThatThrownBy(() -> service.startDedaloClaim(id, UUID.randomUUID().toString(), audit()))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.requireCallbackClaim(
                    "landing-generator", id, task.getExecutionClaimId(), audit()))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    service.startDedaloClaim(id, task.getExecutionClaimId(), audit());
    assertThat(task.getExecutionClaimPhase()).isEqualTo("START_AUTHORIZED");
    assertThatThrownBy(() -> service.startDedaloClaim(id, task.getExecutionClaimId(), audit()))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThatThrownBy(() -> service.requireCallbackClaim("landing-generator", id, null))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    service.requireCallbackClaim("landing-generator", id, task.getExecutionClaimId());
    service.requireCallbackClaim("landing-generator", id, task.getExecutionClaimId(), audit());
    assertThatThrownBy(
            () ->
                service.requireCallbackClaim(
                    "landing-generator",
                    id,
                    task.getExecutionClaimId(),
                    new AgentTaskExecutionAuditRequest(
                        "MODEL", "changed", "high", "different", "a", "b", List.of())))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(task.getExecutionMode()).isEqualTo("MODEL");
    assertThat(task.getEstimatedCostUsd()).isNull();
  }

  /** Preserva captura determinística com recibo e impede convertê-la em nova inferência. */
  @org.junit.jupiter.api.Test
  void acceptsDeterministicCaptureWithoutGrantingModelStart() {
    var repo = mock(AgentTaskRepository.class);
    var task = new AgentTask();
    task.setId(8004L);
    task.setAssignedAgent(Agent.builder().id(4L).agentKey("landing-generator").build());
    var process = new BusinessProcessDefinition();
    process.setId(8104L);
    process.setProcessCode("opala-commercial-preparation-v1");
    process.setDiagramJson("{}");
    task.setProcessDefinition(process);
    task.setProcessActivityId("creative");
    task.setSourceReference("experiment:8204");
    task.setStatus("IN_PROGRESS");
    task.setExecutionClaimId(UUID.randomUUID().toString());
    task.setExecutionClaimPhase("PREPARING");
    when(repo.findById(8004L)).thenReturn(Optional.of(task));
    when(repo.findLockedById(8004L)).thenReturn(Optional.of(task));
    when(repo.save(task)).thenReturn(task);
    var service =
        new AgentTaskService(
            repo,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            Clock.systemUTC());
    service.recordClaimedProcessTaskExecutionAudit(
        "landing-generator",
        8004L,
        new AgentTaskExecutionAuditRequest(
            "DETERMINISTIC",
            "fixture-capture",
            "NOT_APPLICABLE",
            "input",
            null,
            "input",
            List.of()));
    assertThat(task.getExecutionClaimPhase()).isEqualTo("DETERMINISTIC");
    assertThat(task.getExecutionMode()).isEqualTo("DETERMINISTIC");
    assertThatThrownBy(() -> service.startDedaloClaim(8004L, task.getExecutionClaimId(), audit()))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  /** Fornece auditoria sintética composta sem modelo externo ou consumo fabricado. */
  private AgentTaskExecutionAuditRequest audit() {
    return new AgentTaskExecutionAuditRequest(
        "MODEL", "fixture", "high", "agente\n\natividade", "agente", "atividade", List.of());
  }
}
