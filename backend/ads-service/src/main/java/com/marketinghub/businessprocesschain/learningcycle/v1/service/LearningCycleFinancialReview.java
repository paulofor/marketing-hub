package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.*;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: compor a demonstração do ciclo com fontes financeiras já existentes. */
@Component
@RequiredArgsConstructor
public class LearningCycleFinancialReview {
  private final CyclePreparationCostRepository costs;
  private final LearningCyclePreparationBudget preparation;
  private final LearningCycleVideoBudget video;
  private final CommercialPlanRepository commercialPlans;
  private final FinancialPlanService plans;

  /** Mostra estimativas, lacunas e parecer real sem inferir teto a partir de texto histórico. */
  public CycleFinancialReview read(LearningSalesCycle cycle, Product product, Instant now) {
    var slices = costs.summarize(cycle.getProductId());
    BigDecimal known =
        slices.stream()
            .map(CycleFinancialReview.CostSlice::knownUsd)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
    int missing = slices.stream().mapToInt(CycleFinancialReview.CostSlice::unreportedTasks).sum();
    var history = preparation.history(cycle);
    var auth =
        history.stream()
            .filter(CycleFinancialReview.Authorization::current)
            .findFirst()
            .orElse(null);
    var videoAuth = video.current(cycle);
    var linked = commercialPlans.findByExperimentReference(cycle.getExperimentId());
    PlanView plan =
        linked.size() == 1
            ? plans
                .latestForPlan(
                    "products", cycle.getProductId(), Environment.LIVE, linked.getFirst().getId())
                .orElse(null)
            : null;
    String cycleUrl =
        "/business-process-chains/learning-cycles?productId="
            + cycle.getProductId()
            + "&chainId="
            + cycle.getChainDefinitionId()
            + "&cycleId="
            + cycle.getId();
    String planUrl =
        "/financial/plans?productId="
            + cycle.getProductId()
            + (linked.size() == 1 ? "&commercialPlanId=" + linked.getFirst().getId() : "")
            + (plan == null ? "" : "&revisionId=" + plan.id());
    return new CycleFinancialReview(
        cycle.getProductId(),
        product.getName(),
        cycle.getChainDefinitionId(),
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        cycle.getRevision(),
        now,
        "OPEN".equals(cycle.getStatus()),
        "OPEN".equals(cycle.getStatus())
            ? null
            : "Ciclo encerrado: consulte o histórico sem alterar autorizações.",
        cycleUrl,
        cycleUrl + "#cycle-decision",
        video.financeUrl(cycle),
        planUrl,
        "/products/" + cycle.getProductId() + "/edit",
        "Estimativas de tarefas vinculadas aos experimentos deste produto, incluindo tentativas com custo informado. Não são fatura conciliada. Custos externos e referências sem vínculo ao experimento não entram neste subtotal. Confira reservas e autorizações anteriores antes de decidir. Aprovar este teto não inicia tarefas nem autoriza mídia ou audiovisual.",
        cycle.getBudgetLimitBrl(),
        videoAuth == null ? null : videoAuth.budgetLimitUsd(),
        known,
        missing,
        "PARTIAL_AGENT_TASK_ESTIMATES",
        auth == null || known == null
            ? null
            : auth.totalLimitUsd().subtract(known).subtract(auth.reserveUsd()),
        slices,
        auth,
        history,
        plan);
  }
}
