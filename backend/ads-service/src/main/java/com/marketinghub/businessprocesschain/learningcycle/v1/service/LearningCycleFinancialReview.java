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
  private final LearningCycleFinancialAdvice advice;

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
        plan,
        advice.read(cycle, plan, planUrl),
        budgetSections(cycle, auth, videoAuth == null ? null : videoAuth.budgetLimitUsd()),
        mediaDecision(cycle));
  }

  /** Explica limites independentes e distingue ausência de registro de consumo igual a zero. */
  private java.util.List<CycleFinancialReview.BudgetSection> budgetSections(
      LearningSalesCycle cycle, CycleFinancialReview.Authorization preparation, BigDecimal videos) {
    BigDecimal media = cycle.getBudgetLimitBrl();
    return java.util.List.of(
        new CycleFinancialReview.BudgetSection(
            "PREPARATION",
            "Preparar o produto",
            "USD",
            "Trabalho dos agentes para pesquisar, construir e revisar o produto e sua comunicação.",
            "Teto acumulado: inclui as tarefas anteriores do produto e as próximas desta preparação.",
            preparation == null ? null : preparation.totalLimitUsd(),
            "Limite total de preparação",
            preparation == null
                ? "Sem limite registrado para esta versão"
                : "Limite registrado para esta versão",
            "Não libera geração paga de vídeos nem anúncios. A reserva ocupa parte deste limite; não é um valor extra.",
            "O subtotal abaixo reúne estimativas de tarefas, inclusive revisões vinculadas. Não é uma fatura nem o custo completo de produção."),
        new CycleFinancialReview.BudgetSection(
            "VIDEO",
            "Produzir vídeos",
            "USD",
            "Criar os vídeos: imagens e cenas, voz, montagem e revisões do anúncio e da demonstração.",
            "Um teto para os dois vídeos e suas revisões, incluindo consumo anterior desta produção. Não é por vídeo.",
            videos,
            "Limite total dos vídeos",
            videos == null
                ? "Sem autorização de vídeos nesta versão"
                : "Teto dos vídeos registrado",
            "É separado da preparação e dos anúncios. Reutilizar uma peça pronta não exige gerar outro vídeo. Ausência de limite não indica gasto zero nem obriga nova produção.",
            "Consumo e saldo dos vídeos não estão consolidados nesta demonstração. O limite não representa valor gasto."),
        new CycleFinancialReview.BudgetSection(
            "MEDIA",
            "Pagar anúncios",
            "BRL",
            "Valor pago à plataforma, como o Instagram, para exibir os anúncios ao público.",
            "Teto total do ciclo. O limite diário e o período são conferidos na aprovação, antes da ativação.",
            media,
            "Limite registrado para anúncios",
            media == null
                ? "Limite não informado"
                : media.signum() == 0
                    ? "Nenhuma verba registrada neste ciclo"
                    : "Limite registrado; conferir liberação comercial",
            "Não paga a criação dos vídeos. Um vídeo pronto não tem verba de divulgação incluída. Registrar um limite não ativa a campanha.",
            "O valor acima é limite, não gasto realizado. O consumo dos anúncios não está consolidado nesta demonstração."));
  }

  /**
   * Libera somente a abertura do formulário na etapa correta; o comando revalida todos os gates.
   */
  private CycleFinancialReview.MediaDecision mediaDecision(LearningSalesCycle cycle) {
    boolean open = "OPEN".equals(cycle.getStatus());
    boolean available = open && "AUTHORIZATION".equals(cycle.getStage());
    return new CycleFinancialReview.MediaDecision(
        available,
        !open
            ? "Ciclo encerrado: limites preservados para consulta."
            : available
                ? "Confira e aprove os limites abaixo. A campanha ainda depende da homologação e da liberação final."
                : "A aprovação de anúncios estará disponível aqui quando o ciclo chegar à etapa Autorizar orçamento. Conclua as etapas anteriores; nenhum anúncio foi autorizado por esta consulta.",
        cycle.getWindowStart(),
        cycle.getWindowEnd());
  }
}
