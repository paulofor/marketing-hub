package com.marketinghub.hypothesis;

public enum HypothesisFrameworkGenerationJobStage {
    WAITING_AI_WORKER,
    SENT_TO_OPENAI,
    WAITING_OPENAI,
    COMPLETED,
    FAILED
}
