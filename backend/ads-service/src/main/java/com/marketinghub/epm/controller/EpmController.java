package com.marketinghub.epm.controller;

import com.marketinghub.epm.service.EpmService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Expõe os endpoints operacionais do módulo EPM para planejamento, métricas, decisões e simulação financeira.
 */
@RestController
@RequestMapping("/api/epm")
@RequiredArgsConstructor
@Validated
public class EpmController {
    private final EpmService epmService;

    /** Cria um plano financeiro do EPM. */
    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialPlanResponse createFinancialPlan(@Valid @RequestBody CreateFinancialPlanRequest request) {
        return epmService.createFinancialPlan(request);
    }

    /** Lista os planos financeiros do EPM. */
    @GetMapping("/plans")
    public FinancialPlanListResponse listFinancialPlans() {
        return epmService.listFinancialPlans();
    }

    /** Busca um plano financeiro pelo identificador. */
    @GetMapping("/plans/{planId}")
    public FinancialPlanResponse getFinancialPlan(@PathVariable Long planId) {
        return epmService.getFinancialPlan(planId);
    }

    /** Atualiza um plano financeiro existente. */
    @PutMapping("/plans/{planId}")
    public FinancialPlanResponse updateFinancialPlan(@PathVariable Long planId, @Valid @RequestBody UpdateFinancialPlanRequest request) {
        return epmService.updateFinancialPlan(planId, request);
    }

    /** Cria um nicho financeiro dentro de um plano. */
    @PostMapping("/plans/{planId}/niches")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialPlanNicheResponse createPlanNiche(@PathVariable Long planId, @Valid @RequestBody CreatePlanNicheRequest request) {
        return epmService.createPlanNiche(planId, request);
    }

    /** Lista os nichos financeiros de um plano. */
    @GetMapping("/plans/{planId}/niches")
    public FinancialPlanNicheListResponse listPlanNiches(@PathVariable Long planId) {
        return epmService.listPlanNiches(planId);
    }

    /** Busca um nicho financeiro pelo identificador. */
    @GetMapping("/niches/{planNicheId}")
    public FinancialPlanNicheResponse getPlanNiche(@PathVariable Long planNicheId) {
        return epmService.getPlanNiche(planNicheId);
    }

    /** Cria uma hipótese financeira dentro de um nicho. */
    @PostMapping("/niches/{planNicheId}/hypotheses")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialPlanHypothesisResponse createPlanHypothesis(@PathVariable Long planNicheId, @Valid @RequestBody CreatePlanHypothesisRequest request) {
        return epmService.createPlanHypothesis(planNicheId, request);
    }

    /** Lista as hipóteses financeiras de um nicho. */
    @GetMapping("/niches/{planNicheId}/hypotheses")
    public FinancialPlanHypothesisListResponse listPlanHypotheses(@PathVariable Long planNicheId) {
        return epmService.listPlanHypotheses(planNicheId);
    }

    /** Busca uma hipótese financeira pelo identificador. */
    @GetMapping("/hypotheses/{planHypothesisId}")
    public FinancialPlanHypothesisResponse getPlanHypothesis(@PathVariable Long planHypothesisId) {
        return epmService.getPlanHypothesis(planHypothesisId);
    }

    /** Cria um orçamento de experimento dentro de uma hipótese. */
    @PostMapping("/hypotheses/{planHypothesisId}/experiments")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentBudgetResponse createExperimentBudget(@PathVariable Long planHypothesisId, @Valid @RequestBody CreateExperimentBudgetRequest request) {
        return epmService.createExperimentBudget(planHypothesisId, request);
    }

    /** Lista os orçamentos de experimento de uma hipótese. */
    @GetMapping("/hypotheses/{planHypothesisId}/experiments")
    public ExperimentBudgetListResponse listExperimentBudgets(@PathVariable Long planHypothesisId) {
        return epmService.listExperimentBudgets(planHypothesisId);
    }

    /** Busca um orçamento de experimento pelo identificador. */
    @GetMapping("/experiments/{experimentBudgetId}")
    public ExperimentBudgetResponse getExperimentBudget(@PathVariable Long experimentBudgetId) {
        return epmService.getExperimentBudget(experimentBudgetId);
    }

    /** Atualiza orçamento, limite e gasto real de um experimento. */
    @PutMapping("/experiments/{experimentBudgetId}")
    public ExperimentBudgetResponse updateExperimentBudget(@PathVariable Long experimentBudgetId, @Valid @RequestBody UpdateExperimentBudgetRequest request) {
        return epmService.updateExperimentBudget(experimentBudgetId, request);
    }

    /** Registra métricas manuais de um experimento. */
    @PostMapping("/experiments/{experimentBudgetId}/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentMetricResponse createExperimentMetric(@PathVariable Long experimentBudgetId, @Valid @RequestBody CreateExperimentMetricRequest request) {
        return epmService.createExperimentMetric(experimentBudgetId, request);
    }

    /** Busca a métrica manual mais recente de um experimento. */
    @GetMapping("/experiments/{experimentBudgetId}/metrics/latest")
    public ExperimentMetricResponse getLatestExperimentMetric(@PathVariable Long experimentBudgetId) {
        return epmService.getLatestExperimentMetric(experimentBudgetId);
    }

    /** Registra uma decisão financeira para um experimento. */
    @PostMapping("/experiments/{experimentBudgetId}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentDecisionResponse createExperimentDecision(@PathVariable Long experimentBudgetId, @Valid @RequestBody CreateExperimentDecisionRequest request) {
        return epmService.createExperimentDecision(experimentBudgetId, request);
    }

    /** Lista as decisões financeiras de um experimento. */
    @GetMapping("/experiments/{experimentBudgetId}/decisions")
    public ExperimentDecisionListResponse listExperimentDecisions(@PathVariable Long experimentBudgetId) {
        return epmService.listExperimentDecisions(experimentBudgetId);
    }

    /** Cria cenário de preço e ponto de equilíbrio para um plano. */
    @PostMapping("/plans/{planId}/price-scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductPriceScenarioResponse createProductPriceScenario(@PathVariable Long planId, @Valid @RequestBody CreateProductPriceScenarioRequest request) {
        return epmService.createProductPriceScenario(planId, request);
    }

    /** Lista cenários de preço e ponto de equilíbrio de um plano. */
    @GetMapping("/plans/{planId}/price-scenarios")
    public ProductPriceScenarioListResponse listProductPriceScenarios(@PathVariable Long planId) {
        return epmService.listProductPriceScenarios(planId);
    }

    /** Busca o resumo financeiro consolidado de um plano. */
    @GetMapping("/plans/{planId}/summary")
    public FinancialPlanSummaryResponse getFinancialPlanSummary(@PathVariable Long planId) {
        return epmService.getFinancialPlanSummary(planId);
    }
}
