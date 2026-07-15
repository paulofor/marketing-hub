package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.finance.CurrencyConversionProperties;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanWeekObjective;
import com.marketinghub.planning.dto.CommercialPlanWeekDto;
import com.marketinghub.planning.dto.CommercialPlanWeekExperimentDto;
import com.marketinghub.planning.dto.CommercialPlanWeekObjectiveDto;
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

    /** Deve liberar edicao dos objetivos da proxima semana dois dias antes ate dois dias depois do fim da semana atual. */
    @Test
    void listWeeksMarksObjectivesEditableOnlyInsideWindow() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 8));
        when(objectiveRepository.findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(any(), any()))
                .thenAnswer(invocation -> {
                    Integer weekNumber = invocation.getArgument(1);
                    if (Integer.valueOf(2).equals(weekNumber)) {
                        return List.of(CommercialPlanWeekObjective.builder()
                                .id(20L)
                                .weekNumber(2)
                                .sequenceOrder(1)
                                .objectiveText("Objetivo cadastrado para a semana seguinte.")
                                .build());
                    }
                    return List.of();
                });
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
        assertThat(weeks.get(0).objectives())
                .extracting(CommercialPlanWeekObjectiveDto::objectiveText)
                .containsExactly("Objetivo cadastrado para a semana seguinte.");
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

    /** Deve impedir gravacao de objetivos da proxima semana quando a semana atual esta fora da janela comercial. */
    @Test
    void updateObjectivesRejectsWeekOutsideEditWindow() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 8));

        assertThatThrownBy(() -> service.updateObjectives(
                        1L,
                        2,
                        new UpdateCommercialPlanWeekObjectivesRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Objetivos disponiveis");
        verify(objectiveRepository, never()).deleteByPlanIdAndWeekNumber(1L, 3);
    }

    /** Deve gravar os objetivos na semana seguinte ao card editado. */
    @Test
    void updateObjectivesStoresNextWeekObjectivesFromCurrentWeekCard() {
        CommercialPlanWeeklyExperimentService service = serviceAt(LocalDate.of(2026, 7, 15));
        when(objectiveRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<?> objectives = service.updateObjectives(
                1L,
                2,
                new UpdateCommercialPlanWeekObjectivesRequest(
                        List.of(new UpdateCommercialPlanWeekObjectivesRequest.Item(
                                null,
                                1,
                                "Focar a semana 3 em prova comercial real.",
                                12))));

        verify(objectiveRepository).deleteByPlanIdAndWeekNumber(1L, 3);
        assertThat(objectives).hasSize(1);
        ArgumentCaptor<Iterable<CommercialPlanWeekObjective>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(objectiveRepository).saveAll(captor.capture());
        CommercialPlanWeekObjective saved = captor.getValue().iterator().next();
        assertThat(saved.getWeekNumber()).isEqualTo(3);
        assertThat(saved.getObjectiveText()).isEqualTo("Focar a semana 3 em prova comercial real.");
        assertThat(saved.getScore()).isEqualTo(10);
    }

    /** Cria o servico com data fixa para testar a regra de disponibilidade. */
    private CommercialPlanWeeklyExperimentService serviceAt(LocalDate date) {
        Clock clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        CurrencyConversionService conversionService = new CurrencyConversionService(new CurrencyConversionProperties());
        return new CommercialPlanWeeklyExperimentService(
                planService,
                jdbcTemplate,
                objectiveRepository,
                conversionService,
                clock);
    }
}
