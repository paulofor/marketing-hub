package com.marketinghub.oprm.nichocnae.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditService;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Testa a auditoria de interações OpenAI do pipeline NichoCNAE v2. */
class OpenAiInteractionAuditServiceTest {
    /** Deve persistir request, response, tokens, custo e vínculo operacional com job/etapa. */
    @Test
    void shouldPersistOpenAiInteractionAudit() {
        OprmNichoCnaeV2OpenAiInteractionRepository repository = Mockito.mock(OprmNichoCnaeV2OpenAiInteractionRepository.class);
        when(repository.saveAll(anyIterable())).thenAnswer(invocation -> invocation.getArgument(0));
        OpenAiInteractionAuditService service = new OpenAiInteractionAuditService(repository);
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(42L);
        execution.setJobId("job-1");
        execution.setStageCode("candidate-generator");
        execution.setAttemptNumber(1);
        execution.setTechnicalRetryNumber(0);

        service.record(
                execution,
                List.of(new OpenAiInteractionAuditRequest(
                        "gpt-4.1-mini",
                        "flex",
                        100,
                        40,
                        140,
                        new BigDecimal("0.001234"),
                        "resp_123",
                        "{\"request\":true}",
                        "{\"response\":true}",
                        "completed",
                        null)));

        ArgumentCaptor<Iterable<OprmNichoCnaeV2OpenAiInteraction>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        OprmNichoCnaeV2OpenAiInteraction saved = captor.getValue().iterator().next();
        assertThat(saved.getStageExecutionId()).isEqualTo(42L);
        assertThat(saved.getJobId()).isEqualTo("job-1");
        assertThat(saved.getStageCode()).isEqualTo("candidate-generator");
        assertThat(saved.getServiceTier()).isEqualTo("flex");
        assertThat(saved.getInputTokens()).isEqualTo(100);
        assertThat(saved.getOutputTokens()).isEqualTo(40);
        assertThat(saved.getTotalTokens()).isEqualTo(140);
        assertThat(saved.getCostUsd()).isEqualByComparingTo("0.001234");
        assertThat(saved.getRawRequest()).contains("request");
        assertThat(saved.getRawResponse()).contains("response");
    }
}
