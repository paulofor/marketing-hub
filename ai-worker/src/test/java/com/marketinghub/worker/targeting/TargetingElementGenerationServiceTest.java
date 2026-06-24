package com.marketinghub.worker.targeting;

import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do executor de targeting que consome e reporta pendências exclusivamente pelo backend.
 */
@ExtendWith(MockitoExtension.class)
class TargetingElementGenerationServiceTest {
    @Mock
    BackendTargetingElementClient backendClient;

    @Mock
    TargetingElementChatGptClient chatGptClient;

    /** Garante que o worker busca pendências via backend e reporta os resultados sem repository. */
    @Test
    void generateShouldUseBackendClientForPendingAndResults() {
        TargetingElementGenerationPendingDto pending = new TargetingElementGenerationPendingDto(
                23L,
                "Comércio varejista",
                "Lojas de roupas",
                null,
                null,
                null,
                null,
                null,
                "Varejo",
                TargetingElementType.JOB_TITLE,
                1,
                "gpt-5.5");
        CreateTargetingElementRequest generated = new CreateTargetingElementRequest();
        generated.setTerm("Gerente de loja");
        when(backendClient.listPending(20)).thenReturn(List.of(pending));
        when(chatGptClient.generateBatch(anyList())).thenReturn(Map.of(23L, List.of(generated)));
        TargetingElementGenerationService service = new TargetingElementGenerationService(backendClient, chatGptClient);

        service.generate();

        ArgumentCaptor<List<CreateTargetingElementRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(backendClient).sendResults(eq(23L), eq(TargetingElementType.JOB_TITLE), captor.capture());
        assertThat(captor.getValue()).containsExactly(generated);
    }

    /** Garante que falha de OpenAI é reportada ao backend para liberar a pendência. */
    @Test
    void generateShouldReportFailureWhenOpenAiFails() {
        TargetingElementGenerationPendingDto pending = new TargetingElementGenerationPendingDto(
                23L,
                "Comércio varejista",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TargetingElementType.INTEREST,
                1,
                "gpt-5.5");
        when(backendClient.listPending(20)).thenReturn(List.of(pending));
        when(chatGptClient.generateBatch(anyList())).thenThrow(new IllegalStateException("openai indisponível"));
        TargetingElementGenerationService service = new TargetingElementGenerationService(backendClient, chatGptClient);

        service.generate();

        verify(backendClient).sendFailure(23L, TargetingElementType.INTEREST, "openai indisponível");
    }
}
