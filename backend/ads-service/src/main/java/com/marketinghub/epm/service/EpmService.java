package com.marketinghub.epm.service;

import com.marketinghub.epm.*;
import com.marketinghub.epm.service.createExperimentBudget.CreateExperimentBudgetRequest;
import com.marketinghub.epm.service.createExperimentDecision.CreateExperimentDecisionRequest;
import com.marketinghub.epm.service.createExperimentMetric.CreateExperimentMetricRequest;
import com.marketinghub.epm.service.createFinancialPlan.CreateFinancialPlanRequest;
import com.marketinghub.epm.service.createPlanHypothesis.CreatePlanHypothesisRequest;
import com.marketinghub.epm.service.createPlanNiche.CreatePlanNicheRequest;
import com.marketinghub.epm.service.createProductPriceScenario.CreateProductPriceScenarioRequest;
import com.marketinghub.epm.service.getExperimentBudget.ExperimentBudgetResponse;
import com.marketinghub.epm.service.getFinancialPlan.FinancialPlanResponse;
import com.marketinghub.epm.service.getFinancialPlanSummary.FinancialPlanSummaryResponse;
import com.marketinghub.epm.service.getLatestExperimentMetric.ExperimentMetricResponse;
import com.marketinghub.epm.service.getPlanHypothesis.FinancialPlanHypothesisResponse;
import com.marketinghub.epm.service.getPlanNiche.FinancialPlanNicheResponse;
import com.marketinghub.epm.service.listExperimentBudgets.ExperimentBudgetListResponse;
import com.marketinghub.epm.service.listExperimentDecisions.ExperimentDecisionListResponse;
import com.marketinghub.epm.service.listExperimentDecisions.ExperimentDecisionResponse;
import com.marketinghub.epm.service.listFinancialPlans.FinancialPlanListResponse;
import com.marketinghub.epm.service.listPlanHypotheses.FinancialPlanHypothesisListResponse;
import com.marketinghub.epm.service.listPlanNiches.FinancialPlanNicheListResponse;
import com.marketinghub.epm.service.listProductPriceScenarios.ProductPriceScenarioListResponse;
import com.marketinghub.epm.service.listProductPriceScenarios.ProductPriceScenarioResponse;
import com.marketinghub.epm.service.updateExperimentBudget.UpdateExperimentBudgetRequest;
import com.marketinghub.epm.service.updateFinancialPlan.UpdateFinancialPlanRequest;
import com.marketinghub.repository.jpa.epm.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Orquestra as operações manuais do Experiment Profit Manager com cálculos financeiros simples.
 */
@Service
@RequiredArgsConstructor
public class EpmService {
    private final FinancialPlanRepository financialPlanRepository;
    private final FinancialPlanNicheRepository financialPlanNicheRepository;
    private final FinancialPlanHypothesisRepository financialPlanHypothesisRepository;
    private final ExperimentBudgetRepository experimentBudgetRepository;
    private final ExperimentFinancialMetricRepository experimentFinancialMetricRepository;
    private final ExperimentFinancialDecisionRepository experimentFinancialDecisionRepository;
    private final ProductPriceScenarioRepository productPriceScenarioRepository;

    /** Cria um plano financeiro para controlar a verba de um ciclo de experimentos. */
    @Transactional
    public FinancialPlanResponse createFinancialPlan(CreateFinancialPlanRequest request) {
        FinancialPlan plan = FinancialPlan.builder()
                .name(request.name())
                .cycleStartDate(request.cycleStartDate())
                .cycleEndDate(request.cycleEndDate())
                .totalBudgetCents(value(request.totalBudgetCents()))
                .defaultDailyBudgetCents(request.defaultDailyBudgetCents())
                .defaultExperimentDurationDays(request.defaultExperimentDurationDays())
                .defaultExperimentsPerHypothesis(request.defaultExperimentsPerHypothesis())
                .status(request.status() == null ? FinancialPlanStatus.DRAFT : request.status())
                .notes(request.notes())
                .build();
        return toPlan(financialPlanRepository.save(plan));
    }

    /** Lista todos os planos financeiros existentes. */
    @Transactional(readOnly = true)
    public FinancialPlanListResponse listFinancialPlans() {
        return new FinancialPlanListResponse(financialPlanRepository.findAll().stream().map(this::toPlan).toList());
    }

    /** Busca um plano financeiro pelo identificador. */
    @Transactional(readOnly = true)
    public FinancialPlanResponse getFinancialPlan(Long planId) {
        return toPlan(findPlan(planId));
    }

    /** Atualiza os dados operacionais de um plano financeiro existente. */
    @Transactional
    public FinancialPlanResponse updateFinancialPlan(Long planId, UpdateFinancialPlanRequest request) {
        FinancialPlan plan = findPlan(planId);
        plan.setName(request.name());
        plan.setCycleStartDate(request.cycleStartDate());
        plan.setCycleEndDate(request.cycleEndDate());
        plan.setTotalBudgetCents(value(request.totalBudgetCents()));
        plan.setDefaultDailyBudgetCents(request.defaultDailyBudgetCents());
        plan.setDefaultExperimentDurationDays(request.defaultExperimentDurationDays());
        plan.setDefaultExperimentsPerHypothesis(request.defaultExperimentsPerHypothesis());
        plan.setStatus(request.status() == null ? plan.getStatus() : request.status());
        plan.setNotes(request.notes());
        return toPlan(financialPlanRepository.save(plan));
    }

    /** Cria um nicho planejado dentro de um plano financeiro. */
    @Transactional
    public FinancialPlanNicheResponse createPlanNiche(Long planId, CreatePlanNicheRequest request) {
        FinancialPlanNiche niche = FinancialPlanNiche.builder()
                .financialPlan(findPlan(planId))
                .externalNicheId(request.externalNicheId())
                .nicheName(request.nicheName())
                .plannedBudgetCents(value(request.plannedBudgetCents()))
                .spendLimitCents(request.spendLimitCents() == null ? value(request.plannedBudgetCents()) : value(request.spendLimitCents()))
                .status(request.status() == null ? FinancialPlanNicheStatus.PLANNED : request.status())
                .notes(request.notes())
                .build();
        return toNiche(financialPlanNicheRepository.save(niche));
    }

    /** Lista os nichos planejados de um plano financeiro. */
    @Transactional(readOnly = true)
    public FinancialPlanNicheListResponse listPlanNiches(Long planId) {
        findPlan(planId);
        return new FinancialPlanNicheListResponse(financialPlanNicheRepository.findByFinancialPlanIdOrderByIdAsc(planId).stream().map(this::toNiche).toList());
    }

    /** Busca um nicho planejado pelo identificador. */
    @Transactional(readOnly = true)
    public FinancialPlanNicheResponse getPlanNiche(Long planNicheId) {
        return toNiche(findNiche(planNicheId));
    }

    /** Cria uma hipótese financeira dentro de um nicho planejado. */
    @Transactional
    public FinancialPlanHypothesisResponse createPlanHypothesis(Long planNicheId, CreatePlanHypothesisRequest request) {
        FinancialPlanNiche niche = findNiche(planNicheId);
        long costPerExperiment = request.plannedCostPerExperimentCents() == null ? 0L : request.plannedCostPerExperimentCents();
        long lossLimit = request.lossLimitCents() == null ? costPerExperiment * request.plannedExperiments() : request.lossLimitCents();
        FinancialPlanHypothesis hypothesis = FinancialPlanHypothesis.builder()
                .financialPlanNiche(niche)
                .externalHypothesisId(request.externalHypothesisId())
                .title(request.title())
                .plannedExperiments(request.plannedExperiments())
                .plannedCostPerExperimentCents(costPerExperiment)
                .lossLimitCents(lossLimit)
                .status(request.status() == null ? FinancialPlanHypothesisStatus.PLANNED : request.status())
                .notes(request.notes())
                .build();
        return toHypothesis(financialPlanHypothesisRepository.save(hypothesis));
    }

    /** Lista as hipóteses financeiras de um nicho planejado. */
    @Transactional(readOnly = true)
    public FinancialPlanHypothesisListResponse listPlanHypotheses(Long planNicheId) {
        findNiche(planNicheId);
        return new FinancialPlanHypothesisListResponse(financialPlanHypothesisRepository.findByFinancialPlanNicheIdOrderByIdAsc(planNicheId).stream().map(this::toHypothesis).toList());
    }

    /** Busca uma hipótese financeira pelo identificador. */
    @Transactional(readOnly = true)
    public FinancialPlanHypothesisResponse getPlanHypothesis(Long planHypothesisId) {
        return toHypothesis(findHypothesis(planHypothesisId));
    }

    /** Cria um orçamento para executar experimento financeiro dentro de uma hipótese. */
    @Transactional
    public ExperimentBudgetResponse createExperimentBudget(Long planHypothesisId, CreateExperimentBudgetRequest request) {
        long plannedTotal = value(request.plannedDailyBudgetCents()) * request.plannedDurationDays();
        long spendLimit = request.spendLimitCents() == null ? plannedTotal : value(request.spendLimitCents());
        ExperimentBudget budget = ExperimentBudget.builder()
                .financialPlanHypothesis(findHypothesis(planHypothesisId))
                .externalExperimentId(request.externalExperimentId())
                .name(request.name())
                .plannedDailyBudgetCents(value(request.plannedDailyBudgetCents()))
                .plannedDurationDays(request.plannedDurationDays())
                .plannedTotalBudgetCents(plannedTotal)
                .spendLimitCents(spendLimit)
                .actualSpendCents(0L)
                .remainingBudgetCents(spendLimit)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status() == null ? ExperimentBudgetStatus.PLANNED : request.status())
                .notes(request.notes())
                .build();
        return toBudget(experimentBudgetRepository.save(budget));
    }

    /** Lista os orçamentos de experimento de uma hipótese financeira. */
    @Transactional(readOnly = true)
    public ExperimentBudgetListResponse listExperimentBudgets(Long planHypothesisId) {
        findHypothesis(planHypothesisId);
        return new ExperimentBudgetListResponse(experimentBudgetRepository.findByFinancialPlanHypothesisIdOrderByIdAsc(planHypothesisId).stream().map(this::toBudget).toList());
    }

    /** Busca um orçamento de experimento pelo identificador. */
    @Transactional(readOnly = true)
    public ExperimentBudgetResponse getExperimentBudget(Long experimentBudgetId) {
        return toBudget(findBudget(experimentBudgetId));
    }

    /** Atualiza orçamento e gasto real de um experimento financeiro. */
    @Transactional
    public ExperimentBudgetResponse updateExperimentBudget(Long experimentBudgetId, UpdateExperimentBudgetRequest request) {
        ExperimentBudget budget = findBudget(experimentBudgetId);
        long plannedTotal = value(request.plannedDailyBudgetCents()) * request.plannedDurationDays();
        long spendLimit = request.spendLimitCents() == null ? plannedTotal : value(request.spendLimitCents());
        long actualSpend = value(request.actualSpendCents());
        budget.setExternalExperimentId(request.externalExperimentId());
        budget.setName(request.name());
        budget.setPlannedDailyBudgetCents(value(request.plannedDailyBudgetCents()));
        budget.setPlannedDurationDays(request.plannedDurationDays());
        budget.setPlannedTotalBudgetCents(plannedTotal);
        budget.setSpendLimitCents(spendLimit);
        budget.setActualSpendCents(actualSpend);
        budget.setRemainingBudgetCents(spendLimit - actualSpend);
        budget.setStartDate(request.startDate());
        budget.setEndDate(request.endDate());
        budget.setStatus(request.status() == null ? budget.getStatus() : request.status());
        budget.setNotes(request.notes());
        return toBudget(experimentBudgetRepository.save(budget));
    }

    /** Registra métricas manuais e calcula lucro, ROAS, CPL, CPA e conversões. */
    @Transactional
    public ExperimentMetricResponse createExperimentMetric(Long experimentBudgetId, CreateExperimentMetricRequest request) {
        ExperimentBudget budget = findBudget(experimentBudgetId);
        long adSpend = value(request.adSpendCents());
        long revenue = value(request.revenueCents());
        long grossProfit = revenue - adSpend;
        long netProfit = grossProfit - value(request.paymentFeeCents()) - value(request.platformFeeCents()) - value(request.aiCostCents()) - value(request.taxEstimateCents());
        ExperimentFinancialMetric metric = ExperimentFinancialMetric.builder()
                .experimentBudget(budget)
                .measuredAt(request.measuredAt() == null ? Instant.now() : request.measuredAt())
                .impressions(value(request.impressions()))
                .clicks(value(request.clicks()))
                .visitors(request.visitors() == null ? 0 : request.visitors())
                .leads(request.leads() == null ? 0 : request.leads())
                .sampleRequests(request.sampleRequests() == null ? 0 : request.sampleRequests())
                .checkoutClicks(request.checkoutClicks() == null ? 0 : request.checkoutClicks())
                .purchases(request.purchases() == null ? 0 : request.purchases())
                .adSpendCents(adSpend)
                .revenueCents(revenue)
                .paymentFeeCents(value(request.paymentFeeCents()))
                .platformFeeCents(value(request.platformFeeCents()))
                .aiCostCents(value(request.aiCostCents()))
                .taxEstimateCents(value(request.taxEstimateCents()))
                .grossProfitCents(grossProfit)
                .estimatedNetProfitCents(netProfit)
                .ctrDecimal(ratio(value(request.clicks()), value(request.impressions()), 6))
                .cpcCents(centsPer(adSpend, value(request.clicks())))
                .cplCents(centsPer(adSpend, request.leads() == null ? 0 : request.leads()))
                .cpaCents(centsPer(adSpend, request.purchases() == null ? 0 : request.purchases()))
                .roasDecimal(ratio(revenue, adSpend, 4))
                .landingConversionDecimal(ratio(request.leads() == null ? 0 : request.leads(), request.visitors() == null ? 0 : request.visitors(), 6))
                .purchaseConversionDecimal(ratio(request.purchases() == null ? 0 : request.purchases(), request.visitors() == null ? 0 : request.visitors(), 6))
                .notes(request.notes())
                .build();
        budget.setActualSpendCents(adSpend);
        budget.setRemainingBudgetCents(value(budget.getSpendLimitCents()) - adSpend);
        experimentBudgetRepository.save(budget);
        return toMetric(experimentFinancialMetricRepository.save(metric));
    }

    /** Busca a métrica manual mais recente de um experimento. */
    @Transactional(readOnly = true)
    public ExperimentMetricResponse getLatestExperimentMetric(Long experimentBudgetId) {
        findBudget(experimentBudgetId);
        return experimentFinancialMetricRepository.findFirstByExperimentBudgetIdOrderByMeasuredAtDesc(experimentBudgetId).map(this::toMetric).orElseThrow(() -> notFound("Métrica financeira não encontrada"));
    }

    /** Registra uma decisão financeira para um experimento. */
    @Transactional
    public ExperimentDecisionResponse createExperimentDecision(Long experimentBudgetId, CreateExperimentDecisionRequest request) {
        ExperimentFinancialDecision decision = ExperimentFinancialDecision.builder()
                .experimentBudget(findBudget(experimentBudgetId))
                .decisionType(request.decisionType())
                .reason(request.reason())
                .decidedAt(request.decidedAt() == null ? Instant.now() : request.decidedAt())
                .decidedBy(request.decidedBy())
                .build();
        return toDecision(experimentFinancialDecisionRepository.save(decision));
    }

    /** Lista as decisões financeiras registradas para um experimento. */
    @Transactional(readOnly = true)
    public ExperimentDecisionListResponse listExperimentDecisions(Long experimentBudgetId) {
        findBudget(experimentBudgetId);
        return new ExperimentDecisionListResponse(experimentFinancialDecisionRepository.findByExperimentBudgetIdOrderByDecidedAtDesc(experimentBudgetId).stream().map(this::toDecision).toList());
    }

    /** Cria um cenário de preço calculando receita líquida por venda e vendas para empatar. */
    @Transactional
    public ProductPriceScenarioResponse createProductPriceScenario(Long planId, CreateProductPriceScenarioRequest request) {
        FinancialPlan plan = findPlan(planId);
        long totalBudget = request.totalBudgetCents() == null ? value(plan.getTotalBudgetCents()) : value(request.totalBudgetCents());
        long netRevenue = value(request.priceCents()) - value(request.expectedPaymentFeeCents()) - value(request.expectedPlatformFeeCents()) - value(request.expectedTaxCents());
        ProductPriceScenario scenario = ProductPriceScenario.builder()
                .financialPlan(plan)
                .name(request.name())
                .priceCents(value(request.priceCents()))
                .expectedPaymentFeeCents(value(request.expectedPaymentFeeCents()))
                .expectedPlatformFeeCents(value(request.expectedPlatformFeeCents()))
                .expectedTaxCents(value(request.expectedTaxCents()))
                .expectedNetRevenuePerSaleCents(netRevenue)
                .totalBudgetCents(totalBudget)
                .breakEvenSales(ceilSales(totalBudget, netRevenue))
                .notes(request.notes())
                .build();
        return toScenario(productPriceScenarioRepository.save(scenario));
    }

    /** Lista os cenários de preço de um plano financeiro. */
    @Transactional(readOnly = true)
    public ProductPriceScenarioListResponse listProductPriceScenarios(Long planId) {
        findPlan(planId);
        return new ProductPriceScenarioListResponse(productPriceScenarioRepository.findByFinancialPlanIdOrderByIdAsc(planId).stream().map(this::toScenario).toList());
    }

    /** Consolida o resumo financeiro de um plano com base nos experimentos e métricas manuais. */
    @Transactional(readOnly = true)
    public FinancialPlanSummaryResponse getFinancialPlanSummary(Long planId) {
        FinancialPlan plan = findPlan(planId);
        List<FinancialPlanNiche> niches = financialPlanNicheRepository.findByFinancialPlanIdOrderByIdAsc(planId);
        List<FinancialPlanHypothesis> hypotheses = niches.stream().flatMap(n -> financialPlanHypothesisRepository.findByFinancialPlanNicheIdOrderByIdAsc(n.getId()).stream()).toList();
        List<ExperimentBudget> budgets = hypotheses.stream().flatMap(h -> experimentBudgetRepository.findByFinancialPlanHypothesisIdOrderByIdAsc(h.getId()).stream()).toList();
        List<ExperimentFinancialMetric> latestMetrics = budgets.stream().map(b -> experimentFinancialMetricRepository.findFirstByExperimentBudgetIdOrderByMeasuredAtDesc(b.getId()).orElse(null)).filter(Objects::nonNull).toList();
        long actualSpend = latestMetrics.stream().mapToLong(ExperimentFinancialMetric::getAdSpendCents).sum();
        long revenue = latestMetrics.stream().mapToLong(ExperimentFinancialMetric::getRevenueCents).sum();
        long grossProfit = latestMetrics.stream().mapToLong(ExperimentFinancialMetric::getGrossProfitCents).sum();
        long netProfit = latestMetrics.stream().mapToLong(ExperimentFinancialMetric::getEstimatedNetProfitCents).sum();
        int withPurchase = (int) latestMetrics.stream().filter(m -> m.getPurchases() > 0).count();
        int withoutSignal = (int) latestMetrics.stream().filter(m -> m.getPurchases() == 0 && m.getLeads() == 0 && m.getCheckoutClicks() == 0).count();
        long plannedExperimentBudget = budgets.stream().mapToLong(ExperimentBudget::getPlannedTotalBudgetCents).sum();
        return new FinancialPlanSummaryResponse(planId, plan.getTotalBudgetCents(), plannedExperimentBudget, actualSpend, revenue, grossProfit, netProfit, niches.size(), hypotheses.size(), budgets.size(), withPurchase, withoutSignal);
    }

    /** Converte plano financeiro em DTO de resposta. */
    private FinancialPlanResponse toPlan(FinancialPlan plan) {
        return new FinancialPlanResponse(plan.getId(), plan.getName(), plan.getCycleStartDate(), plan.getCycleEndDate(), plan.getTotalBudgetCents(), plan.getDefaultDailyBudgetCents(), plan.getDefaultExperimentDurationDays(), plan.getDefaultExperimentsPerHypothesis(), plan.getStatus(), plan.getNotes(), plan.getCreatedAt(), plan.getUpdatedAt());
    }

    /** Converte nicho financeiro em DTO de resposta. */
    private FinancialPlanNicheResponse toNiche(FinancialPlanNiche niche) {
        return new FinancialPlanNicheResponse(niche.getId(), niche.getFinancialPlan().getId(), niche.getExternalNicheId(), niche.getNicheName(), niche.getPlannedBudgetCents(), niche.getSpendLimitCents(), niche.getStatus(), niche.getNotes(), niche.getCreatedAt(), niche.getUpdatedAt());
    }

    /** Converte hipótese financeira em DTO de resposta. */
    private FinancialPlanHypothesisResponse toHypothesis(FinancialPlanHypothesis hypothesis) {
        return new FinancialPlanHypothesisResponse(hypothesis.getId(), hypothesis.getFinancialPlanNiche().getId(), hypothesis.getExternalHypothesisId(), hypothesis.getTitle(), hypothesis.getPlannedExperiments(), hypothesis.getPlannedCostPerExperimentCents(), hypothesis.getLossLimitCents(), hypothesis.getStatus(), hypothesis.getNotes(), hypothesis.getCreatedAt(), hypothesis.getUpdatedAt());
    }

    /** Converte orçamento de experimento em DTO de resposta. */
    private ExperimentBudgetResponse toBudget(ExperimentBudget budget) {
        return new ExperimentBudgetResponse(budget.getId(), budget.getFinancialPlanHypothesis().getId(), budget.getExternalExperimentId(), budget.getName(), budget.getPlannedDailyBudgetCents(), budget.getPlannedDurationDays(), budget.getPlannedTotalBudgetCents(), budget.getSpendLimitCents(), budget.getActualSpendCents(), budget.getRemainingBudgetCents(), budget.getStartDate(), budget.getEndDate(), budget.getStatus(), budget.getNotes(), budget.getCreatedAt(), budget.getUpdatedAt());
    }

    /** Converte métrica financeira em DTO de resposta. */
    private ExperimentMetricResponse toMetric(ExperimentFinancialMetric metric) {
        return new ExperimentMetricResponse(metric.getId(), metric.getExperimentBudget().getId(), metric.getMeasuredAt(), metric.getImpressions(), metric.getClicks(), metric.getVisitors(), metric.getLeads(), metric.getSampleRequests(), metric.getCheckoutClicks(), metric.getPurchases(), metric.getAdSpendCents(), metric.getRevenueCents(), metric.getPaymentFeeCents(), metric.getPlatformFeeCents(), metric.getAiCostCents(), metric.getTaxEstimateCents(), metric.getGrossProfitCents(), metric.getEstimatedNetProfitCents(), metric.getCtrDecimal(), metric.getCpcCents(), metric.getCplCents(), metric.getCpaCents(), metric.getRoasDecimal(), metric.getLandingConversionDecimal(), metric.getPurchaseConversionDecimal(), metric.getNotes(), metric.getCreatedAt());
    }

    /** Converte decisão financeira em DTO de resposta. */
    private ExperimentDecisionResponse toDecision(ExperimentFinancialDecision decision) {
        return new ExperimentDecisionResponse(decision.getId(), decision.getExperimentBudget().getId(), decision.getDecisionType(), decision.getReason(), decision.getDecidedAt(), decision.getDecidedBy(), decision.getCreatedAt());
    }

    /** Converte cenário de preço em DTO de resposta. */
    private ProductPriceScenarioResponse toScenario(ProductPriceScenario scenario) {
        return new ProductPriceScenarioResponse(scenario.getId(), scenario.getFinancialPlan().getId(), scenario.getName(), scenario.getPriceCents(), scenario.getExpectedPaymentFeeCents(), scenario.getExpectedPlatformFeeCents(), scenario.getExpectedTaxCents(), scenario.getExpectedNetRevenuePerSaleCents(), scenario.getTotalBudgetCents(), scenario.getBreakEvenSales(), scenario.getNotes(), scenario.getCreatedAt(), scenario.getUpdatedAt());
    }

    /** Busca plano financeiro ou retorna erro HTTP 404. */
    private FinancialPlan findPlan(Long id) { return financialPlanRepository.findById(id).orElseThrow(() -> notFound("Plano financeiro não encontrado")); }
    /** Busca nicho financeiro ou retorna erro HTTP 404. */
    private FinancialPlanNiche findNiche(Long id) { return financialPlanNicheRepository.findById(id).orElseThrow(() -> notFound("Nicho financeiro não encontrado")); }
    /** Busca hipótese financeira ou retorna erro HTTP 404. */
    private FinancialPlanHypothesis findHypothesis(Long id) { return financialPlanHypothesisRepository.findById(id).orElseThrow(() -> notFound("Hipótese financeira não encontrada")); }
    /** Busca orçamento de experimento ou retorna erro HTTP 404. */
    private ExperimentBudget findBudget(Long id) { return experimentBudgetRepository.findById(id).orElseThrow(() -> notFound("Orçamento de experimento não encontrado")); }
    /** Cria erro padronizado de recurso inexistente. */
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(NOT_FOUND, message); }
    /** Normaliza valores nulos para zero. */
    private long value(Number value) { return value == null ? 0L : value.longValue(); }
    /** Calcula proporção decimal ou retorna nulo quando o denominador é zero. */
    private BigDecimal ratio(long numerator, long denominator, int scale) { return denominator == 0 ? null : BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), scale, RoundingMode.HALF_UP); }
    /** Calcula custo médio em centavos ou retorna nulo quando a quantidade é zero. */
    private Long centsPer(long cents, long quantity) { return quantity == 0 ? null : BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(quantity), 0, RoundingMode.HALF_UP).longValue(); }
    /** Calcula vendas necessárias para empatar com arredondamento para cima. */
    private int ceilSales(long totalBudget, long netRevenuePerSale) { return netRevenuePerSale <= 0 ? 0 : BigDecimal.valueOf(totalBudget).divide(BigDecimal.valueOf(netRevenuePerSale), 0, RoundingMode.CEILING).intValue(); }
}
