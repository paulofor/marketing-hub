package com.marketinghub.experiment.pipeline;

public enum ExperimentPipelineGenerationJobStage {
    WAITING_AI_WORKER,
    SENT_TO_OPENAI,
    WAITING_OPENAI,
    COMPLETED,
    FAILED
}
