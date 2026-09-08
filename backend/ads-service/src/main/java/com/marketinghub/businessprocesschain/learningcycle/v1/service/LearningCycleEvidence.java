package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: conferir evidências reais dos gates e da publicação sem chamar serviços
 * externos.
 */
@Component
@RequiredArgsConstructor
public class LearningCycleEvidence {
  private final BusinessProcessActivityInstanceRepository instances;
  private final ExperimentRunRepository runs;
  private final LearningCycleJson json;

  /** Exige gate canônico aprovado para o mesmo produto e versão depois do último ajuste. */
  public void validation(LearningSalesCycle cycle, JsonNode data) {
    require(
        data.path("approvalInstanceId").canConvertToLong(),
        "Informe a instância do gate multiagente aprovado.");
    var gate = instances.findById(data.path("approvalInstanceId").asLong()).orElse(null);
    require(
        gate != null && gate.isObjectiveAchieved() && "COMPLETED".equals(gate.getStatus()),
        "O gate multiagente precisa estar concluído com objetivo atingido.");
    require(
        "agentValidationGate".equals(gate.getActivityDefinition().getActivityId())
            && "pde-construction-approval"
                .equals(gate.getActivityDefinition().getProcessDefinition().getProcessCode()),
        "A instância informada não é o gate multiagente canônico.");
    require(
        latestGate(cycle).map(value -> value.getId().equals(gate.getId())).orElse(false),
        "Existe uma revisão mais recente do gate. Use o resultado vigente para esta versão.");
    JsonNode proof = json.read(gate.getObjectiveEvidenceJson());
    require(
        "PDE_AGENT_VALIDATION_GATE_V1".equals(proof.path("evidenceType").asText())
            && proof.path("productId").asLong(-1) == cycle.getProductId()
            && cycle.getProductVersion().equals(proof.path("prototypeVersion").asText())
            && ("product:" + cycle.getProductId() + "@agent-validation-v1")
                .equals(gate.getSourceReference()),
        "A aprovação deve pertencer ao produto e à versão exatos deste ciclo.");
    require(
        gate.getExitedAt() != null && !gate.getExitedAt().isBefore(cycle.getVersionChangedAt()),
        "A aprovação é anterior ao ciclo ou à última correção. Execute nova homologação.");
    text(data, "journeyEvidence");
    if (!data.path("humanObservationEvidence").asText().isBlank())
      text(data, "humanObservationEvidence");
    require(
        data.path("instrumentationVerified").asBoolean(false),
        "Valide a instrumentação e a segregação dos testes antes de autorizar.");
  }

  /**
   * Oferece somente gates persistidos da mesma versão, sem exigir que o usuário procure ids
   * técnicos.
   */
  public List<LearningCycleResponse.ApprovalOption> approvals(LearningSalesCycle cycle) {
    return latestGate(cycle).stream()
        .filter(
            gate ->
                gate.isObjectiveAchieved()
                    && "COMPLETED".equals(gate.getStatus())
                    && gate.getExitedAt() != null
                    && !gate.getExitedAt().isBefore(cycle.getVersionChangedAt())
                    && "agentValidationGate".equals(gate.getActivityDefinition().getActivityId()))
        .filter(
            gate -> {
              var proof = json.read(gate.getObjectiveEvidenceJson());
              return "PDE_AGENT_VALIDATION_GATE_V1".equals(proof.path("evidenceType").asText())
                  && proof.path("productId").asLong(-1) == cycle.getProductId()
                  && cycle.getProductVersion().equals(proof.path("prototypeVersion").asText());
            })
        .map(
            gate ->
                new LearningCycleResponse.ApprovalOption(
                    gate.getId(),
                    "Versão "
                        + cycle.getProductVersion()
                        + " · aprovação #"
                        + gate.getId()
                        + " · "
                        + gate.getExitedAt()))
        .toList();
  }

  /** Usa a última ocorrência do gate; uma reprovação posterior invalida aprovações históricas. */
  private Optional<BusinessProcessActivityInstance> latestGate(LearningSalesCycle cycle) {
    return instances
        .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
            "pde-construction-approval", "product:" + cycle.getProductId() + "@agent-validation-v1")
        .stream()
        .filter(gate -> "agentValidationGate".equals(gate.getActivityDefinition().getActivityId()))
        .findFirst();
  }

  /** Confere autorização explícita sem escrever limites de mídia no experimento. */
  public void authorization(
      LearningSalesCycle cycle, Experiment experiment, JsonNode data, Instant now) {
    require(
        data.path("confirmed").asBoolean(false),
        "Confirme explicitamente orçamento e janela desta versão.");
    require(
        cycle.getProductVersion().equals(text(data, "productVersion")),
        "A autorização deve identificar a versão homologada.");
    require(
        data.path("budgetLimitBrl").isNumber()
            && data.path("budgetLimitBrl").decimalValue().compareTo(cycle.getBudgetLimitBrl()) == 0,
        "A autorização precisa confirmar o teto total exato do ciclo.");
    require(now.isBefore(cycle.getWindowEnd()), "A janela terminou; planeje outro experimento.");
    require(
        experiment.getPlatform() != ExperimentPlatform.FACEBOOK
            || (experiment.getMediaSpendLimit() != null
                && experiment.getMediaSpendLimit().compareTo(cycle.getBudgetLimitBrl()) == 0),
        "O teto total do experimento deve coincidir com o ciclo antes da autorização.");
  }

  /** Confere o resultado do fluxo oficial, incluindo run produtivo e preflight persistido. */
  public void publication(LearningSalesCycle cycle, Experiment experiment, Instant authorizedAt) {
    require(
        experiment.getStatus() == ExperimentStatus.RUNNING,
        "Publique pelo fluxo oficial do experimento e volte para conferir o resultado.");
    var run =
        runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
                experiment.getId(), ExperimentRunMode.PRODUCTION)
            .orElse(null);
    require(
        run != null && run.getPublishedAt() != null && run.getPreflightCompletedAt() != null,
        "Ainda não existe publicação produtiva com preflight comprovado para este experimento.");
    require(
        authorizedAt != null
            && !run.getPublishedAt().isBefore(authorizedAt)
            && !run.getPublishedAt().isBefore(cycle.getWindowStart())
            && !run.getPublishedAt().isAfter(cycle.getWindowEnd()),
        "A publicação deve ocorrer depois da autorização e dentro da janela do ciclo.");
    require(
        experiment.getPlatform() != ExperimentPlatform.FACEBOOK
            || (experiment.getMediaSpendLimit() != null
                && experiment.getMediaSpendLimit().compareTo(cycle.getBudgetLimitBrl()) == 0),
        "O orçamento publicado diverge do teto aprovado neste ciclo.");
  }

  /** Reconhece somente referências históricas que realmente chegaram à operação produtiva. */
  public boolean operated(Experiment experiment) {
    return runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            experiment.getId(), ExperimentRunMode.PRODUCTION)
        .filter(run -> run.getPublishedAt() != null)
        .isPresent();
  }
}
