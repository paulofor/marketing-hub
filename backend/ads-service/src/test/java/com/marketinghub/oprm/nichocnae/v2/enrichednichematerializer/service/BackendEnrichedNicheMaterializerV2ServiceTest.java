package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution.EnrichedNicheMaterializerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution.EnrichedNicheMaterializerCreateRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BackendEnrichedNicheMaterializerV2ServiceTest {
    @Test
    void createsOnlyPendingStageExecutionWithoutBusinessDecision() {
        OprmNichoCnaeV2StageExecutionRepository repository = mock(OprmNichoCnaeV2StageExecutionRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution execution = invocation.getArgument(0);
            execution.setId(12L);
            return execution;
        });
        BackendEnrichedNicheMaterializerService service = new BackendEnrichedNicheMaterializerService(repository, true);

        var response = service.create(new EnrichedNicheMaterializerCreateRequest(
                "job-1", 80L, 44L, "4781400", 1, 3, false, "{}"));

        assertThat(response.stageExecutionId()).isEqualTo("12");
        assertThat(response.stageCode()).isEqualTo("enriched-niche-materializer");
        verify(repository).save(any(OprmNichoCnaeV2StageExecution.class));
    }

    @Test
    void completesWithDecisionReceivedFromExternalExecutor() {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(12L);
        execution.setStageCode("enriched-niche-materializer");
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        OprmNichoCnaeV2StageExecutionRepository repository = mock(OprmNichoCnaeV2StageExecutionRepository.class);
        when(repository.findByIdAndStageCode(12L, "enriched-niche-materializer")).thenReturn(Optional.of(execution));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BackendEnrichedNicheMaterializerService service = new BackendEnrichedNicheMaterializerService(repository, true);

        var response = service.complete(12L, new EnrichedNicheMaterializerCompletionRequest(
                "DO_NOT_MATERIALIZE", "E2_ROUTINE_PAIN", 0.61, null, null, "{}"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.materializationDecision()).isEqualTo("DO_NOT_MATERIALIZE");
        assertThat(response.validationLevel()).isEqualTo("E2_ROUTINE_PAIN");
    }
}
