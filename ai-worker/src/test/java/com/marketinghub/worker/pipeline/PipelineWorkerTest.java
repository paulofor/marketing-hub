package com.marketinghub.worker.pipeline;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineWorkerTest {

    /** Valida o fluxo feliz do worker genérico marcando running e completed. */
    @Test
    void processPendingMarcaRunningECompletedQuandoProcessorRetornaSucesso() {
        FakeBackend backend = new FakeBackend();
        ArtifactStore artifactStore = new InMemoryArtifactStore();
        PipelineWorker<String, String> worker = new PipelineWorker<>(backend, context -> new StageResult<>("ok", List.of(), Map.of()), artifactStore);

        ProcessingSummary summary = worker.processPending(5);

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        assertThat(backend.running).containsExactly("job-1");
        assertThat(backend.completed).containsExactly("job-1");
        assertThat(backend.failed).isEmpty();
    }

    private static class FakeBackend implements StageBackendPort<String, String> {
        private final List<String> running = new ArrayList<>();
        private final List<String> completed = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();

        /** Retorna uma execução pendente fixa para o teste do worker. */
        @Override
        public List<StageExecution<String>> listPending(int limit) {
            return List.of(new StageExecution<>("job-1", 38L, "landing-page-deliverables", "INICIADO", Instant.now(), "input", Map.of()));
        }

        /** Registra que a execução foi marcada como em processamento. */
        @Override
        public void markRunning(StageExecution<String> execution) {
            running.add(execution.idJob());
        }

        /** Registra que a execução foi concluída com sucesso. */
        @Override
        public void markCompleted(StageExecution<String> execution, StageResult<String> result) {
            completed.add(execution.idJob());
        }

        /** Registra que a execução falhou. */
        @Override
        public void markFailed(StageExecution<String> execution, Throwable error) {
            failed.add(execution.idJob());
        }
    }
}
