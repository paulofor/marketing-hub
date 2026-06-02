package com.marketinghub.nichocnae.routineresearchcycle;

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

/** Valida o serviço manual da etapa um do pipeline OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class RoutineResearchCycleServiceTest {
    @Mock private RoutineResearchCycleBackendClient backendClient;

    /** Confirma que a listagem de pendências é delegada para o contrato backend correto. */
    @Test
    void shouldListPendingCycles() {
        RoutineResearchCyclePending pending = pendingCycle();
        when(backendClient.listPendingCycles()).thenReturn(List.of(pending));
        RoutineResearchCycleService service = newService();

        List<RoutineResearchCyclePending> result = service.listPendingCycles();

        assertThat(result).containsExactly(pending);
    }

    /** Confirma que a etapa um processa pendências usando o worker genérico e devolve detalhes canônicos. */
    @Test
    void shouldProcessPendingCyclesWithoutScheduler() {
        RoutineResearchCyclePending pending = pendingCycle();
        RoutineResearchCycleDetail detail = detailCycle();
        when(backendClient.listPendingCycles()).thenReturn(List.of(pending));
        when(backendClient.detailStageExecution(1001L)).thenReturn(detail);
        RoutineResearchCycleService service = newService();

        List<RoutineResearchCycleDetail> result = service.processPending("TEST");

        assertThat(result).containsExactly(detail);
        verify(backendClient).listPendingCycles();
        verify(backendClient).detailStageExecution(1001L);
    }

    /** Confirma que o detalhe direto preserva a fonte de verdade do backend. */
    @Test
    void shouldDetailStageExecution() {
        RoutineResearchCycleDetail detail = detailCycle();
        when(backendClient.detailStageExecution(1001L)).thenReturn(detail);
        RoutineResearchCycleService service = newService();

        RoutineResearchCycleDetail result = service.detailStageExecution(1001L);

        assertThat(result).isEqualTo(detail);
    }

    /** Confirma que o histórico por nicho é delegado para o backend OPRM. */
    @Test
    void shouldListBySourceNicheId() {
        RoutineResearchCycleSummary summary = new RoutineResearchCycleSummary(
                1001L,
                55L,
                "9602501",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "RUNNING",
                0,
                0,
                Instant.parse("2026-06-02T03:00:00Z"),
                null);
        when(backendClient.listBySourceNicheId(55L)).thenReturn(List.of(summary));
        RoutineResearchCycleService service = newService();

        List<RoutineResearchCycleSummary> result = service.listBySourceNicheId(55L);

        assertThat(result).containsExactly(summary);
    }

    /** Monta uma unidade de trabalho pendente reutilizável para os testes da etapa um. */
    private RoutineResearchCyclePending pendingCycle() {
        return new RoutineResearchCyclePending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.parse("2026-06-02T03:00:00Z"),
                Instant.parse("2026-06-02T03:00:00Z"));
    }

    /** Monta o detalhe canônico reutilizável de ciclo retornado pelo backend OPRM. */
    private RoutineResearchCycleDetail detailCycle() {
        return new RoutineResearchCycleDetail(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "RUNNING",
                0,
                0,
                0,
                0,
                Instant.parse("2026-06-02T03:00:00Z"),
                null,
                null);
    }

    /** Monta o serviço com processor real e cliente mockado para manter o teste focado na etapa um. */
    private RoutineResearchCycleService newService() {
        RoutineResearchCycleProcessor processor = new RoutineResearchCycleProcessor(backendClient);
        return new RoutineResearchCycleService(backendClient, processor, new NoopArtifactStore());
    }
}
