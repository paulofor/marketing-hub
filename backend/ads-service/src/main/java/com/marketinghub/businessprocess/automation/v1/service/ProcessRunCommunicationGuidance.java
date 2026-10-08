package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunUserAction;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: tornar visível a decisão econômica ausente na preparação da comunicação. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessRunCommunicationGuidance {
  private final BusinessProcessDefinitionRepository processes;
  private final ExperimentRepository experiments;
  private final CommercialPlanRepository plans;
  private final FinancialPlanRevisionRepository revisions;
  private final ObjectMapper json;

  /** Consulta fontes da referência exata, sem repetir agentes ou alterar o histórico financeiro. */
  public ProcessRunUserAction resolve(ProcessRun run) {
    if (!"WAITING_INPUT".equals(run.getStatus())
        || run.getLearningCycleId() != null
        || run.getFailureCount() > 0
        || !"communicationContract".equals(run.getCurrentActivityId())
        || run.getSourceReference() == null
        || !run.getSourceReference().matches("experiment:[1-9][0-9]*")) return null;
    var process = processes.findById(run.getProcessDefinitionId()).orElse(null);
    if (process == null || !"pde-communication-sales-journey".equals(process.getProcessCode()))
      return null;
    Long experimentId;
    try {
      experimentId = Long.valueOf(run.getSourceReference().substring(11));
    } catch (NumberFormatException ex) {
      log.warn(
          "Referência fora do intervalo ao consultar decisão econômica. runId={} productId={} sourceReference={}",
          run.getId(),
          run.getProductId(),
          run.getSourceReference(),
          ex);
      return null;
    }
    var experiment = experiments.findById(experimentId).orElse(null);
    if (experiment == null
        || experiment.getProduct() == null
        || !Objects.equals(run.getProductId(), experiment.getProduct().getId())
        || Boolean.FALSE.equals(experiment.getProduct().getAutomaticExecutionEnabled())
        || !"PLANNED".equals(String.valueOf(experiment.getStatus()))) return null;
    var candidates = plans.findByExperimentReference(experiment.getId());
    if (candidates.size() != 1) return null;
    var history =
        revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", run.getProductId(), Environment.LIVE);
    if (history.isEmpty()) return null;
    var revision = history.getFirst();
    if (!"PRODUCT".equals(revision.getScopeKind())
        || revision.getEnvironment() != Environment.LIVE
        || !Objects.equals(run.getProductId(), revision.getProductId())
        || !Objects.equals(run.getProductId(), revision.getScopeId())
        || !Objects.equals(candidates.getFirst().getId(), revision.getCommercialPlanId()))
      return null;
    try {
      var assumptions = json.readTree(revision.getAssumptionsJson());
      if (assumptions == null
          || !assumptions.isObject()
          || !assumptions.path("minimumMarginPercent").isNull()
              && !assumptions.path("minimumMarginPercent").isMissingNode()) return null;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler meta econômica da comunicação. runId={} productId={} financialRevisionId={}",
          run.getId(),
          run.getProductId(),
          revision.getId(),
          ex);
      return null;
    }
    return new ProcessRunUserAction(
        "DEFINE_CONTRIBUTION_TARGET",
        "Falta sua meta de quanto deve sobrar por venda",
        "A revisão financeira #"
            + revision.getId()
            + " não registra a margem mínima escolhida. É a menor sobra por venda após entrega, taxas/impostos e aquisição; ajuda a pagar custos fixos e gerar lucro, não é lucro líquido. Neste plano, o percentual usa a receita líquida positiva após deduções comerciais, incluindo reembolsos, sem descontá-las duas vezes. Definir a meta não resolve os custos ou a entrega personalizada ainda pendentes."
            + (run.getReason() == null || run.getReason().isBlank() ? "" : " " + run.getReason()),
        "Você · decisão da meta econômica",
        "Definir meta no plano financeiro",
        "/financial/plans?productId="
            + run.getProductId()
            + "&commercialPlanId="
            + revision.getCommercialPlanId()
            + "&revisionId="
            + revision.getId()
            + "&edit=contribution-target",
        "No plano financeiro, responda à decisão da margem e use Salvar margem mínima. Exemplo fictício: venda de R$ 100, entrega de R$ 20, taxas/impostos de R$ 10 e aquisição de R$ 30 deixam R$ 40: 40% do valor cobrado, ou 44,44% da receita líquida de R$ 90 usada neste campo. Plutus avaliará a meta quando os custos estiverem completos; Atena e Dédalo conservam suas pendências. Isso não autoriza mídia, vídeo pago, cobrança ou nova inferência.",
        "internal://financial-plans/products/"
            + run.getProductId()
            + "/revisions/"
            + revision.getId());
  }
}
