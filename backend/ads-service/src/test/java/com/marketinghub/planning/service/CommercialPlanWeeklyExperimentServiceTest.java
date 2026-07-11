package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanWeekDto;
import com.marketinghub.planning.dto.CommercialPlanWeekExperimentDto;
import com.marketinghub.planning.dto.UpdateCommercialPlanWeekObjectivesRequest;
import com.marketinghub.repository.jpa.planning.CommercialPlanWeekObjectiveRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a leitura semanal e a janela de edicao dos objetivos comerciais. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanWeeklyExperimentServiceTest {
    @Mock
    private CommercialPlanService planService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CommercialPlanWeekObjectiveRepository objectiveRepository;

    private CommercialPlan plan;

    /** Prepara um plano de julho sem experimentos para isolar a regra de janela semanal. */
    @BeforeEach
    void setUp() {
        plan = CommercialPlan.builder()
                .id(1L)
                .name("Julho")
                .deadline(LocalDate.of(2026, 7, 31))
                .build();
        when(planService.getPlan(1L)).thenReturn(plan);
    }

    /** Deve liberar edicao de objetivos dois dias antes ate dois dias depois do fim da semana. */
    @Test
    void listWeeksMarksObjectivesEditableOnlyInsideWindow() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 8));
        when(objectiveRepository.findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(any(), any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(
                        anyString(),
                        any(RowMapper.class),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.<CommercialPlanWeekExperimentDto>of());

        List<CommercialPlanWeekDto> weeks = service.listWeeks(1L);

        assertThat(weeks).hasSize(5);
        assertThat(weeks.get(0).objectivesEditable()).isTrue();
        assertThat(weeks.get(1).objectivesEditable()).isFalse();
        assertThat(weeks.get(0).objectiveEditWindowMessage()).contains("2026-07-09");
    }

    /** Deve calcular tempo medio com todas as sessoes de analytics, alinhado ao detalhe do experimento. */
    @Test
    void listWeeksUsesAllLandingAnalyticsSessionsAsAverageTimeDenominator() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 8));
        when(objectiveRepository.findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(any(), any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(
                        anyString(),
                        any(RowMapper.class),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.<CommercialPlanWeekExperimentDto>of());

        service.listWeeks(1L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(5)).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        String sql = sqlCaptor.getAllValues().getFirst();
        assertThat(sql).contains("count(distinct event_times.session_id)");
        assertThat(sql).contains("when lower(lae.event_type) = 'section_view_time'");
        assertThat(sql).doesNotContain("where lower(lae.event_type) = 'section_view_time'");
        assertThat(sql).doesNotContain("where event_times.elapsed_ms is not null");
    }

    /** Deve impedir gravacao de objetivos quando a semana esta fora da janela comercial. */
    @Test
    void updateObjectivesRejectsWeekOutsideEditWindow() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 8));

        assertThatThrownBy(() -> service.updateObjectives(
                        1L,
                        2,
                        new UpdateCommercialPlanWeekObjectivesRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Objetivos disponiveis");
        verify(objectiveRepository, never()).deleteByPlanIdAndWeekNumber(1L, 2);
    }

    /** Cria o servico com data fixa para testar a regra de disponibilidade. */
    private CommercialPlanWeeklyExperimentService serviceAt(LocalDate date) {
        Clock clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new CommercialPlanWeeklyExperimentService(planService, jdbcTemplate, objectiveRepository, clock);
    }
}
