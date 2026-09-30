package com.marketinghub.product.service.agentvalidation;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskClaimPreparationHook;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Responsabilidade: condicionar a correção de Dédalo a uma nova versão privada comprovada. */
@Component
public class PdeAgentValidationRuntimeCorrectionClaimHook implements AgentTaskClaimPreparationHook {
  private static final Pattern SOURCE_REFERENCE =
      Pattern.compile("^product:([1-9][0-9]*)@agent-validation-v1$");
  private static final String PROCESS_CODE = "pde-construction-approval";
  private static final String ACTIVITY_ID = "prototypeCorrection";
  private final PdeAgentValidationRuntimeAcceptanceHook runtimeAcceptance;

  /** Configura a reconciliação compartilhada pelo acesso inicial e pelos retrabalhos. */
  public PdeAgentValidationRuntimeCorrectionClaimHook(
      PdeAgentValidationRuntimeAcceptanceHook runtimeAcceptance) {
    this.runtimeAcceptance = runtimeAcceptance;
  }

  /** Restringe a preparação às correções de protótipo de Dédalo no fluxo multiagente. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProcessDefinition() != null
        && PROCESS_CODE.equals(task.getProcessDefinition().getProcessCode())
        && ACTIVITY_ID.equals(task.getProcessActivityId())
        && task.getAssignedAgent() != null
        && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
        && task.getSourceReference() != null
        && SOURCE_REFERENCE.matcher(task.getSourceReference()).matches();
  }

  /** Prova e persiste a nova identidade antes de permitir qualquer consumo de modelo. */
  @Override
  public Preparation prepare(AgentTask task) {
    return runtimeAcceptance.prepareCorrection(task);
  }
}
