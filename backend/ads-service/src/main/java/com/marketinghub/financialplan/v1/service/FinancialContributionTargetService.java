package com.marketinghub.financialplan.v1.service;

import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.contributiontarget.SaveContributionTargetRequest;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.SavePlanRequest;
import jakarta.validation.Validator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: registrar somente a meta explícita sem substituir as demais premissas. */
@Service
@RequiredArgsConstructor
public class FinancialContributionTargetService {
  private final FinancialPlanService plans;
  private final Validator validator;

  /** Cria revisão pelo mecanismo canônico e recusa contexto trocado ou concorrência. */
  @Transactional
  public PlanView save(
      Long productId,
      Environment environment,
      SaveContributionTargetRequest request,
      String actor) {
    if (request == null || !validator.validate(request).isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Informe a revisão e uma margem entre 0,01% e 99,99% da receita líquida.");
    var source = plans.get("PRODUCT", productId, environment, request.sourceRevisionId());
    var history = plans.list("PRODUCT", productId, environment);
    if (!"PRODUCT".equals(source.scope())
        || !Objects.equals(source.scopeId(), productId)
        || source.environment() != environment
        || !Objects.equals(source.id(), request.sourceRevisionId())
        || history.isEmpty()
        || !Objects.equals(history.getFirst().id(), source.id()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A revisão financeira mudou. Recarregue o contexto antes de registrar a meta.");
    var a = source.assumptions();
    if (a.minimumMarginPercent() != null
        && a.minimumMarginPercent().compareTo(request.minimumMarginPercent()) == 0) return source;
    var changed =
        new PlanAssumptions(
            a.productVersion(),
            a.periodDays(),
            a.validUntil(),
            a.evidence(),
            a.ai(),
            a.costs(),
            a.priceBrl(),
            request.minimumMarginPercent(),
            a.maximumCacBrl(),
            a.scenarios(),
            a.preparation(),
            a.variableCostEnvelope(),
            a.fixedCostEnvelope(),
            a.realizedCostBaseline());
    return plans.create(
        "PRODUCT",
        productId,
        environment,
        new SavePlanRequest(
            source.name(),
            actor == null || actor.isBlank() ? "Operador via decisão de margem" : actor,
            source.revision(),
            source.commercialPlanId(),
            source.templateId(),
            changed));
  }
}
