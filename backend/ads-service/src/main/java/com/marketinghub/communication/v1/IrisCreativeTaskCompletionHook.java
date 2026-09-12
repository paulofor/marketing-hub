package com.marketinghub.communication.v1;

import com.marketinghub.agenttask.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: exigir a imagem final rastreável antes de concluir a produção criativa de Íris.
 */
@Component
@RequiredArgsConstructor
public class IrisCreativeTaskCompletionHook implements AgentTaskCompletionHook {
  private final CreativeVisualEvidenceService visuals;

  /** Reconhece apenas a produção não audiovisual atribuída a Íris. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProcessDefinition() != null
        && "creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
        && "nonAudiovisual".equals(task.getProcessActivityId());
  }

  /**
   * Bloqueia um briefing sem PNG ou com origem divergente, preservando a auditoria da tentativa.
   */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    visuals.validateCompletion(task, request.resultJson());
    return CompletionDisposition.COMPLETE;
  }
}
