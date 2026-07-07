package com.marketinghub.planning.service;

import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanMilestone;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Responsabilidade: sincronizar valores executados do planejamento com dados operacionais persistidos. */
@Service
public class CommercialPlanExecutionSyncService {
    private static final Logger log = LoggerFactory.getLogger(CommercialPlanExecutionSyncService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final JdbcTemplate jdbcTemplate;
    private final CurrencyConversionService currencyConversionService;

    /** Inicializa o sincronizador com acesso SQL somente leitura sobre fontes operacionais. */
    public CommercialPlanExecutionSyncService(
            JdbcTemplate jdbcTemplate,
            CurrencyConversionService currencyConversionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currencyConversionService = currencyConversionService;
    }

    /** Atualiza o executado mensal e semanal do plano recebido. */
    public void sync(CommercialPlan plan, List<CommercialPlanMilestone> milestones) {
        LocalDate planEnd = resolvePlanEnd(plan);
        LocalDate planStart = planEnd.withDayOfMonth(1);
        ExecutionTotals monthlyTotals = totals(planStart, planEnd.plusDays(1));
        applyPlanTotals(plan, monthlyTotals);

        LocalDate currentStart = planStart;
        for (CommercialPlanMilestone milestone : milestones) {
            LocalDate currentEnd = milestone.getDueDate();
            if (currentEnd == null || currentEnd.isBefore(currentStart)) {
                applyMilestoneTotals(milestone, ExecutionTotals.empty());
                continue;
            }
            applyMilestoneTotals(milestone, totals(currentStart, currentEnd.plusDays(1)));
            currentStart = currentEnd.plusDays(1);
        }
    }

    /** Define o periodo mensal do plano usando o prazo final como referencia principal. */
    private LocalDate resolvePlanEnd(CommercialPlan plan) {
        if (plan.getDeadline() != null) {
            return plan.getDeadline();
        }
        Instant createdAt = plan.getCreatedAt() == null ? Instant.now() : plan.getCreatedAt();
        LocalDate createdDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        return createdDate.withDayOfMonth(createdDate.lengthOfMonth());
    }

    /** Calcula custos, receita e quantidades no intervalo fechado-aberto informado. */
    private ExecutionTotals totals(LocalDate startInclusive, LocalDate endExclusive) {
        Timestamp start = Timestamp.from(startInclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp end = Timestamp.from(endExclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        BigDecimal campaignCost = money(queryDecimal("""
                select coalesce(sum(m.spend), 0)
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
                """, java.sql.Date.valueOf(endExclusive), java.sql.Date.valueOf(startInclusive), start, end));
        BigDecimal aiCostUsd = money(queryDecimal("""
                select coalesce(sum(cost_usd), 0)
                from (
                    select coalesce(sum(cost_usd), 0) as cost_usd
                    from ai_worker_generation
                    where created_at >= ? and created_at < ?
                    union all
                    select coalesce(sum(cost_usd), 0) as cost_usd
                    from experiment_pipeline_generation_job
                    where created_at >= ? and created_at < ?
                    union all
                    select coalesce(sum(cost_usd), 0) as cost_usd
                    from gera_sales_page_stage_execution
                    where created_at >= ? and created_at < ?
                ) actual_ai_costs
                """, start, end, start, end, start, end));
        BigDecimal aiCostFromMetrics = money(queryDecimal("""
                select coalesce(sum(ai_cost_cents), 0) / 100.0
                from experiment_financial_metric
                where measured_at >= ? and measured_at < ?
                """, start, end));
        BigDecimal aiCost = money(currencyConversionService.usdToBrl(aiCostUsd).add(aiCostFromMetrics));
        BigDecimal videoCost = money(queryDecimal("""
                select coalesce(sum(cost), 0)
                from experiment_video_asset
                where created_at >= ? and created_at < ?
                """, start, end));
        BigDecimal revenue = money(queryDecimal("""
                select coalesce(sum(revenue_cents), 0) / 100.0
                from experiment_financial_metric
                where measured_at >= ? and measured_at < ?
                """, start, end));
        Integer experimentsCreated = queryInteger("""
                select count(*)
                from experiment
                where created_at >= ? and created_at < ?
                """, start, end);
        Integer experimentsPublished = queryInteger("""
                select count(distinct experiment_id)
                from facebook_ads_campaign
                where created_at >= ? and created_at < ?
                """, start, end);
        return new ExecutionTotals(
                campaignCost,
                aiCost,
                money(campaignCost.add(aiCost).add(videoCost)),
                revenue,
                experimentsCreated,
                experimentsPublished,
                Instant.now());
    }

    /** Aplica o consolidado executado aos campos mensais do plano. */
    private void applyPlanTotals(CommercialPlan plan, ExecutionTotals totals) {
        plan.setActualCampaignCost(totals.campaignCost());
        plan.setActualAiCost(totals.aiCost());
        plan.setActualTotalCost(totals.totalCost());
        plan.setActualRevenue(totals.revenue());
        plan.setActualExperimentsCreated(totals.experimentsCreated());
        plan.setActualExperimentsPublished(totals.experimentsPublished());
        plan.setExecutionSyncedAt(totals.syncedAt());
    }

    /** Aplica o consolidado executado aos campos semanais do marco. */
    private void applyMilestoneTotals(CommercialPlanMilestone milestone, ExecutionTotals totals) {
        milestone.setActualCampaignCost(totals.campaignCost());
        milestone.setActualAiCost(totals.aiCost());
        milestone.setActualTotalCost(totals.totalCost());
        milestone.setActualRevenue(totals.revenue());
        milestone.setActualExperimentsCreated(totals.experimentsCreated());
        milestone.setActualExperimentsPublished(totals.experimentsPublished());
        milestone.setExecutionSyncedAt(totals.syncedAt());
    }

    /** Executa uma consulta decimal e retorna zero quando a fonte operacional ainda nao estiver disponivel. */
    private BigDecimal queryDecimal(String sql, Object... args) {
        try {
            BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return result == null ? ZERO : result;
        } catch (DataAccessException ex) {
            log.warn("Falha ao sincronizar valor decimal executado do planejamento. sql={}", compact(sql), ex);
            return ZERO;
        }
    }

    /** Executa uma consulta inteira e retorna zero quando a fonte operacional ainda nao estiver disponivel. */
    private Integer queryInteger(String sql, Object... args) {
        try {
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return result == null ? 0 : result;
        } catch (DataAccessException ex) {
            log.warn("Falha ao sincronizar quantidade executada do planejamento. sql={}", compact(sql), ex);
            return 0;
        }
    }

    /** Normaliza valores monetarios para exibicao em reais. */
    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Compacta SQL apenas para manter o log legivel. */
    private String compact(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    /** Responsabilidade: transportar o consolidado executado de um periodo. */
    private record ExecutionTotals(
            BigDecimal campaignCost,
            BigDecimal aiCost,
            BigDecimal totalCost,
            BigDecimal revenue,
            Integer experimentsCreated,
            Integer experimentsPublished,
            Instant syncedAt) {
        /** Retorna totais zerados para marcos sem periodo definido. */
        private static ExecutionTotals empty() {
            return new ExecutionTotals(ZERO, ZERO, ZERO, ZERO, 0, 0, Instant.now());
        }
    }
}
