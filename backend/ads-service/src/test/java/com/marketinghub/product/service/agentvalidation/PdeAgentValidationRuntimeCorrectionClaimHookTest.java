package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskClaimPreparationHook;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o escopo da preparação do runtime nas correções de Dédalo. */
class PdeAgentValidationRuntimeCorrectionClaimHookTest {

  /** Encaminha somente a correção canônica do produto para a reconciliação compartilhada. */
  @Test
  void supportsAndPreparesOnlyCanonicalPrototypeCorrection() {
    var acceptance = mock(PdeAgentValidationRuntimeAcceptanceHook.class);
    var hook = new PdeAgentValidationRuntimeCorrectionClaimHook(acceptance);
    AgentTask task = task();
    var ready = AgentTaskClaimPreparationHook.Preparation.ready("runtime reconciliado");
    when(acceptance.prepareCorrection(task)).thenReturn(ready);

    assertThat(hook.supports(task)).isTrue();
    assertThat(hook.prepare(task)).isSameAs(ready);
    verify(acceptance).prepareCorrection(task);

    task.setProcessActivityId("access");
    assertThat(hook.supports(task)).isFalse();
    task.setProcessActivityId("prototypeCorrection");
    task.setSourceReference("experiment:94");
    assertThat(hook.supports(task)).isFalse();
  }

  /** Monta a tarefa mínima do contrato multiagente para o teste de escopo. */
  private AgentTask task() {
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    var task = new AgentTask();
    task.setId(7001L);
    task.setProcessDefinition(process);
    task.setProcessActivityId("prototypeCorrection");
    task.setAssignedAgent(Agent.builder().agentKey("landing-generator").build());
    task.setSourceReference("product:11@agent-validation-v1");
    return task;
  }
}
