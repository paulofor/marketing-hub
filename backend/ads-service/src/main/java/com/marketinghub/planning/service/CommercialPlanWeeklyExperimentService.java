package com.marketinghub.planning.service;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanWeekDto;
import com.marketinghub.planning.dto.CommercialPlanWeekExperimentDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: montar a leitura semanal de experimentos do planejamento comercial. */
@Service
public class CommercialPlanWeeklyExperimentService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CommercialPlanService planService;
    private final JdbcTemplate jdbcTemplate;

    /** Inicializa o serviço com a fonte de plano e acesso SQL de leitura operacional. */
    public CommercialPlanWeeklyExperimentService(CommercialPlanService planService, JdbcTemplate jdbcTemplate) {
        this.planService = planService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Lista as semanas do mês do plano com os experimentos criados em cada período. */
    @Transactional(readOnly = true)
    public List<CommercialPlanWeekDto> listWeeks(Long planId) {
        CommercialPlan plan = planService.getPlan(planId);
        LocalDate monthEnd = resolvePlanEnd(plan);
        LocalDate monthStart = monthEnd.withDayOfMonth(1);
        List<CommercialPlanWeekDto> weeks = new ArrayList<>();
        LocalDate start = monthStart;
        int weekNumber = 1;
        while (!start.isAfter(monthEnd)) {
            LocalDate end = start.plusDays(6).isAfter(monthEnd) ? monthEnd : start.plusDays(6);
            List<CommercialPlanWeekExperimentDto> experiments = listExperiments(start, end.plusDays(1));
            weeks.add(new CommercialPlanWeekDto(
                    weekNumber,
                    start,
                    end,
                    experiments.size(),
                    sumCost(experiments),
                    sumRevenue(experiments),
                    experiments));
            start = end.plusDays(1);
            weekNumber++;
        }
        return weeks;
    }

    /** Resolve o mês de referência do plano a partir do prazo ou da criação. */
    private LocalDate resolvePlanEnd(CommercialPlan plan) {
        if (plan.getDeadline() != null) {
            return plan.getDeadline();
        }
        Instant createdAt = plan.getCreatedAt() == null ? Instant.now() : plan.getCreatedAt();
        LocalDate createdDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        return createdDate.withDayOfMonth(createdDate.lengthOfMonth());
    }

    /** Busca os experimentos criados no intervalo e agrega custos rastreáveis por experimento. */
    private List<CommercialPlanWeekExperimentDto> listExperiments(LocalDate startInclusive, LocalDate endExclusive) {
        Timestamp start = Timestamp.from(startInclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp end = Timestamp.from(endExclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        return jdbcTemplate.query("""
                select
                    e.id,
                    e.name,
                    coalesce(e.product_ai_subtype, e.experiment_type) as product_type,
                    e.status,
                    e.created_at,
                    coalesce(campaign.cost, 0) as campaign_cost,
                    coalesce(financial.ai_cost, 0) as ai_cost,
                    coalesce(video.cost, 0) as video_cost,
                    coalesce(financial.revenue, 0) as revenue
                from experiment e
                left join (
                    select m.experiment_id, sum(coalesce(m.spend, 0)) as cost
                    from experiment_campaign_metric m
                    where (
                        m.date_start is not null
                        and m.date_start < ?
                        and (m.date_stop is null or m.date_stop >= ?)
                    )
                    or (
                        m.date_start is null
                        and m.updated_at >= ?
                        and m.updated_at < ?
                    )
                    group by m.experiment_id
                ) campaign on campaign.experiment_id = e.id
                left join (
                    select b.external_experiment_id as experiment_id,
                           sum(coalesce(f.ai_cost_cents, 0)) / 100.0 as ai_cost,
                           sum(coalesce(f.revenue_cents, 0)) / 100.0 as revenue
                    from experiment_budget b
                    join experiment_financial_metric f on f.experiment_budget_id = b.id
                    where f.measured_at >= ? and f.measured_at < ?
                    group by b.external_experiment_id
                ) financial on financial.experiment_id = e.id
                left join (
                    select experiment_id, sum(coalesce(cost, 0)) as cost
                    from experiment_video_asset
                    where created_at >= ? and created_at < ?
                    group by experiment_id
                ) video on video.experiment_id = e.id
                where e.created_at >= ? and e.created_at < ?
                order by e.created_at asc, e.id asc
                """, this::mapExperiment, java.sql.Date.valueOf(endExclusive), java.sql.Date.valueOf(startInclusive),
                start, end, start, end, start, end, start, end);
    }

    /** Converte a linha SQL no contrato de experimento semanal. */
    private CommercialPlanWeekExperimentDto mapExperiment(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal campaignCost = money(rs.getBigDecimal("campaign_cost"));
        BigDecimal aiCost = money(rs.getBigDecimal("ai_cost"));
        BigDecimal videoCost = money(rs.getBigDecimal("video_cost"));
        BigDecimal revenue = money(rs.getBigDecimal("revenue"));
        BigDecimal totalCost = money(campaignCost.add(aiCost).add(videoCost));
        return new CommercialPlanWeekExperimentDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("product_type"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("created_at")),
                campaignCost,
                aiCost,
                videoCost,
                totalCost,
                revenue,
                resultLabel(revenue, totalCost));
    }

    /** Soma custo total dos experimentos da semana. */
    private BigDecimal sumCost(List<CommercialPlanWeekExperimentDto> experiments) {
        return money(experiments.stream()
                .map(CommercialPlanWeekExperimentDto::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Soma receita dos experimentos da semana. */
    private BigDecimal sumRevenue(List<CommercialPlanWeekExperimentDto> experiments) {
        return money(experiments.stream()
                .map(CommercialPlanWeekExperimentDto::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Classifica o resultado financeiro rastreável do experimento. */
    private String resultLabel(BigDecimal revenue, BigDecimal totalCost) {
        if (revenue.compareTo(BigDecimal.ZERO) > 0 && revenue.compareTo(totalCost) >= 0) {
            return "Receita cobre custo";
        }
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            return "Receita parcial";
        }
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return "Sem receita rastreada";
        }
        return "Sem custo rastreado";
    }

    /** Normaliza valores monetários para reais. */
    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Converte timestamp nulo ou preenchido para Instant. */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
