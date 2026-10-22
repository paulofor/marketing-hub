package com.marketinghub.product.bigidea.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import jakarta.validation.Validation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifica aceite real da estratégia, preservação de direção e recusa de callback inválido. */
class BigIdeaStrategyCompletionTest {
  /** Comprova criação de Atena e bloqueio de alteração silenciosa com duas identidades. */
  @Test
  void approvesInitialIdeaButNeverOverwritesPinnedDirection() throws Exception {
    var json = new ObjectMapper();
    var service = mock(BigIdeaService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var contexts = new BigIdeaTaskContext(service, targets, json);
    var hook =
        new BigIdeaStrategyCompletion(
            service,
            contexts,
            targets,
            json,
            Validation.buildDefaultValidatorFactory().getValidator());
    for (long id : new long[] {99101L, 99102L}) {
      var task = new AgentTask();
      task.setId(id);
      task.setSourceReference("experiment:" + id);
      var agent = new Agent();
      agent.setAgentKey("experiment-strategist");
      task.setAssignedAgent(agent);
      var process = new BusinessProcessDefinition();
      process.setProcessCode("pde-commercial-plan-offer");
      process.setVersionNumber(13);
      task.setProcessDefinition(process);
      task.setProcessActivityId("marketStrategy");
      task.setProductBigIdeaJson("{\"status\":\"MISSING\"}");
      when(targets.resolve(task.getSourceReference()))
          .thenReturn(
              Optional.of(
                  new AgentTaskTargetResponse(
                      task.getSourceReference(),
                      id,
                      id,
                      "fixture",
                      "Produto",
                      "Sintético",
                      "v1",
                      null,
                      null,
                      null,
                      null,
                      null)));
      var content = BigIdeaServiceTest.content("Direção " + id);
      var result = json.createObjectNode().put("decision", "APPROVE");
      result.set("bigIdea", json.valueToTree(content));
      var request = mock(CompleteAgentTaskRequest.class);
      when(request.resultJson()).thenReturn(result.toString());
      assertThat(hook.supports(task)).isTrue();
      assertThat(hook.apply(task, request))
          .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
      verify(service).initializeFromStrategy(id, content, id);
      var snapshot = json.createObjectNode();
      snapshot.putObject("revision").set("content", json.valueToTree(content));
      task.setProductBigIdeaJson(snapshot.toString());
      result.set("bigIdea", json.valueToTree(BigIdeaServiceTest.content("Outra direção")));
      when(request.resultJson()).thenReturn(result.toString());
      assertThatThrownBy(() -> hook.apply(task, request)).hasMessageContaining("divergente");
      task.setProductBigIdeaJson(null);
      assertThat(hook.supports(task)).isFalse();
    }
  }

  /** Mantém rejeição sem criar produto, versão ou autorização comercial. */
  @Test
  void rejectedStrategyHasNoSideEffect() {
    var service = mock(BigIdeaService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var json = new ObjectMapper();
    var hook =
        new BigIdeaStrategyCompletion(
            service,
            new BigIdeaTaskContext(service, targets, json),
            targets,
            json,
            Validation.buildDefaultValidatorFactory().getValidator());
    var request = mock(CompleteAgentTaskRequest.class);
    when(request.resultJson()).thenReturn("{\"decision\":\"ADJUST\"}");
    hook.apply(new AgentTask(), request);
    verifyNoInteractions(service, targets);
    when(request.resultJson()).thenReturn("{\"decision\":\"APPROVE\",\"bigIdea\":{}}");
    assertThatThrownBy(() -> hook.apply(new AgentTask(), request)).hasMessageContaining("inválida");
    verifyNoInteractions(service, targets);
  }
}
