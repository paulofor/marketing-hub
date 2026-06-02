package com.marketinghub.nichocnae.routineresearchorchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.NoopArtifactStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o serviço manual da etapa zero do pipeline OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class RoutineResearchOrchestratorServiceTest {
    @Mock private RoutineResearchOrchestratorBackendClient backendClient;

    /** Confirma que a listagem de pendências é delegada para o contrato backend correto. */
    @Test
    void shouldListPendingCandidates() {
        RoutineResearchOrchestratorPending pending = new RoutineResearchOrchestratorPending(
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "PENDING",
                null,
                Instant.parse("2026-06-02T03:00:00Z"));
        when(backendClient.listPendingCandidates()).thenReturn(List.of(pending));
        RoutineResearchOrchestratorService service = newService();

        List<RoutineResearchOrchestratorPending> result = service.listPendingCandidates();

        assertThat(result).containsExactly(pending);
    }

    /** Confirma que a execução manual usa o processor da etapa e retorna a resposta do backend. */
    @Test
    void shouldRunNextWithoutScheduler() {
        RoutineResearchOrchestratorOutput output = new RoutineResearchOrchestratorOutput(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                "Nenhum nicho CNAE pendente com score disponível para pesquisa de rotina.");
        when(backendClient.runNext()).thenReturn(output);
        RoutineResearchOrchestratorService service = newService();

        RoutineResearchOrchestratorOutput result = service.runNext("TEST");

        assertThat(result).isEqualTo(output);
        verify(backendClient).runNext();
    }

    /** Monta o serviço com processor real e cliente mockado para manter o teste focado na etapa zero. */
    private RoutineResearchOrchestratorService newService() {
        RoutineResearchOrchestratorProcessor processor = new RoutineResearchOrchestratorProcessor(backendClient);
        return new RoutineResearchOrchestratorService(backendClient, processor, new NoopArtifactStore());
    }
}
