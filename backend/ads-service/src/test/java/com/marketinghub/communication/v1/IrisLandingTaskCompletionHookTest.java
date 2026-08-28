package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.geralanding.agent.v1.GovernedLandingHtmlService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: garantir que o HTML de Íris espere o Quality Review independente. */
class IrisLandingTaskCompletionHookTest {

  /** Aplica o HTML no experimento correto e mantém a tarefa em processamento. */
  @Test
  void shouldDeferCompletionUntilQualityReview() {
    IrisCommunicationMaterializationContextProvider context =
        mock(IrisCommunicationMaterializationContextProvider.class);
    GovernedLandingHtmlService landing = mock(GovernedLandingHtmlService.class);
    when(context.experimentId("commercial-plan:1@v2:journey")).thenReturn(Optional.of(88L));
    IrisLandingTaskCompletionHook hook =
        new IrisLandingTaskCompletionHook(context, landing, new ObjectMapper());
    AgentTask task = task("communication-director", "landing-page-generation", "html");
    task.setId(41L);
    task.setSourceReference("commercial-plan:1@v2:journey");
    String html = "<!doctype html><html><body>" + "conteúdo ".repeat(80) + "</body></html>";

    AgentTaskCompletionHook.CompletionDisposition disposition =
        hook.apply(
            task,
            new CompleteAgentTaskRequest(
                "{\"functionalOutput\":{\"landingHtml\":" + quote(html) + "}}", "{}"));

    assertThat(disposition).isEqualTo(AgentTaskCompletionHook.CompletionDisposition.DEFERRED);
    verify(landing).apply(88L, html, "agent-task:41");
  }

  /** Não intercepta tarefa de Dédalo, outra atividade ou outro processo. */
  @Test
  void shouldSupportOnlyIrisLandingHtml() {
    IrisLandingTaskCompletionHook hook =
        new IrisLandingTaskCompletionHook(
            mock(IrisCommunicationMaterializationContextProvider.class),
            mock(GovernedLandingHtmlService.class),
            new ObjectMapper());

    assertThat(hook.supports(task("communication-director", "landing-page-generation", "html")))
        .isTrue();
    assertThat(hook.supports(task("landing-generator", "landing-page-generation", "html")))
        .isFalse();
    assertThat(hook.supports(task("communication-director", "landing-page-generation", "compose")))
        .isFalse();
    assertThat(
            hook.supports(task("communication-director", "creative-production-approval", "html")))
        .isFalse();
  }

  /** Bloqueia uma landing sem experimento segregado em vez de aplicá-la em outro produto. */
  @Test
  void shouldRejectTaskWithoutOwnedExperiment() {
    IrisCommunicationMaterializationContextProvider context =
        mock(IrisCommunicationMaterializationContextProvider.class);
    IrisLandingTaskCompletionHook hook =
        new IrisLandingTaskCompletionHook(
            context, mock(GovernedLandingHtmlService.class), new ObjectMapper());
    AgentTask task = task("communication-director", "landing-page-generation", "html");
    task.setId(42L);
    task.setSourceReference("commercial-plan:9@v1:journey");
    when(context.experimentId(task.getSourceReference())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                hook.apply(
                    task,
                    new CompleteAgentTaskRequest(
                        "{\"functionalOutput\":{\"landingHtml\":\"<html></html>\"}}", "{}")))
        .hasMessageContaining("não pôde ser aplicado");
  }

  /** Cria uma tarefa mínima para testar a fronteira do hook. */
  private AgentTask task(String agentKey, String processCode, String activityId) {
    AgentTask task = new AgentTask();
    task.setAssignedAgent(Agent.builder().agentKey(agentKey).build());
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode(processCode);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    return task;
  }

  /** Escapa o HTML usando o mesmo serializador do contrato de produção. */
  private String quote(String value) {
    try {
      return new ObjectMapper().writeValueAsString(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalArgumentException("HTML de teste inválido.", ex);
    }
  }
}
