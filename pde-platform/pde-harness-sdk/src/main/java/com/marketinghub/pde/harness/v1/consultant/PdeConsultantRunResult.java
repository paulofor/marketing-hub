package com.marketinghub.pde.harness.v1.consultant;

import com.marketinghub.pde.harness.v1.PdeAgentRunResult;
import java.util.Objects;

/** Expõe resultado e prompt dividido para persistência auditável pelo worker e backend. */
public record PdeConsultantRunResult(
    PdeConsultantChannel channel,
    String productTypeCode,
    String agentPrompt,
    String activityPrompt,
    String completePrompt,
    String completePromptVersion,
    PdeAgentRunResult agentRun) {

  /** Garante que a auditoria nunca seja devolvida sem o resultado correlacionado. */
  public PdeConsultantRunResult {
    channel = Objects.requireNonNull(channel, "channel");
    productTypeCode = Objects.requireNonNull(productTypeCode, "productTypeCode");
    agentPrompt = Objects.requireNonNull(agentPrompt, "agentPrompt");
    activityPrompt = Objects.requireNonNull(activityPrompt, "activityPrompt");
    completePrompt = Objects.requireNonNull(completePrompt, "completePrompt");
    completePromptVersion = Objects.requireNonNull(completePromptVersion, "completePromptVersion");
    agentRun = Objects.requireNonNull(agentRun, "agentRun");
  }
}
