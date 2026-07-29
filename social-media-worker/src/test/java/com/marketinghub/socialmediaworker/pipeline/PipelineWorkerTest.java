package com.marketinghub.socialmediaworker.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PipelineWorkerTest {

    @Test
    void runReturnsSuccessWhenProcessorCompletes() {
        PipelineWorker<String, String> worker = new PipelineWorker<>((context, input) -> input + "-ok");

        StageResult<String> result = worker.run(StageContext.of("test", 1L, "job-1"), "input");

        assertTrue(result.success());
        assertEquals("input-ok", result.output());
    }

    @Test
    void runReturnsValidationFailureWhenProcessorRejectsInput() {
        PipelineWorker<String, String> worker = new PipelineWorker<>((context, input) -> {
            throw new IllegalArgumentException("entrada invalida");
        });

        StageResult<String> result = worker.run(StageContext.of("test", 1L, "job-1"), "input");

        assertFalse(result.success());
        assertEquals("VALIDATION_ERROR", result.errorCategory());
    }
}
