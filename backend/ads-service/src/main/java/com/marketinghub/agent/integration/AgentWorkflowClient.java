package com.marketinghub.agent.integration;

import java.io.IOException;
import java.util.List;

/** Responsabilidade: definir a leitura dos workflows concluídos que atualizam os executores. */
public interface AgentWorkflowClient {
  /** Consulta no provedor os workflows concluídos da branch operacional configurada. */
  List<WorkflowRun> listCompletedRuns(String repository, String branch)
      throws IOException, InterruptedException;

  /** Representa os dados mínimos de uma execução de workflow para a tela de agentes. */
  record WorkflowRun(
      String workflowFile,
      String workflowName,
      String branch,
      String status,
      String conclusion,
      java.time.Instant completedAt,
      String url) {}
}
