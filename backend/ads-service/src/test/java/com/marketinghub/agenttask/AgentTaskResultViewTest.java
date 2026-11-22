package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir exposição de falhas técnicas como explicação de negócio. */
class AgentTaskResultViewTest {
  /** Explica interrupção legada sem inventar resultado ou consumo zero. */
  @Test void summarizesInterruptedContextWithoutRawError() {
    var task=new AgentTask();
    task.setStatus("BLOCKED");
    task.setExecutionError("com.internal.Exception: input_too_large {raw:true}");
    var summary=AgentTaskResultView.humanSummary(task);
    assertThat(summary.text()).contains("Nenhuma entrega", "consumo").doesNotContain("Exception", "raw");
    task.setResultJson("{\"humanSummary\":\"Entrega preservada para recuperação do callback.\"}");
    assertThat(AgentTaskResultView.humanSummary(task).text()).contains("Entrega preservada");
  }
}
