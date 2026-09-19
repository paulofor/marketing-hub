package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: reconhecer a publicação PDE sustentada pelo preflight produtivo do mesmo
 * experimento, slot e versão.
 */
@Service
public class PublishedPdePreflightEvidenceService {
  private static final String VALIDATION_OK = "OK";
  private static final Set<String> REQUIRED_FACEBOOK_GATES =
      Set.of(
          ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
          ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED,
          ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED,
          ExperimentRunGateCodes.DATA_FRESHNESS_VALID);

  private final ExperimentRunRepository runRepository;
  private final ExperimentRunGateResultRepository gateRepository;
  private final PdeProductionSlotRepository slotRepository;

  /** Configura as fontes persistidas do run, dos gates e do slot publicado. */
  public PublishedPdePreflightEvidenceService(
      ExperimentRunRepository runRepository,
      ExperimentRunGateResultRepository gateRepository,
      PdeProductionSlotRepository slotRepository) {
    this.runRepository = runRepository;
    this.gateRepository = gateRepository;
    this.slotRepository = slotRepository;
  }

  /**
   * Confirma que a campanha Facebook aponta para a versão PDE publicada e homologada no run atual.
   */
  @Transactional(readOnly = true)
  public boolean isReady(Experiment experiment) {
    if (!isFacebookPde(experiment)) {
      return false;
    }
    PdeProductionSlot slot =
        slotRepository
            .findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId())
            .orElse(null);
    if (!isPublishedSlotForExperiment(slot, experiment)) {
      return false;
    }
    ExperimentRun run =
        runRepository
            .findTopByExperimentIdAndModeOrderByRunNumberDesc(
                experiment.getId(), ExperimentRunMode.PRODUCTION)
            .orElse(null);
    if (!isApprovedRun(run)) {
      return false;
    }
    List<ExperimentRunGateResult> gates =
        gateRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId());
    return allGatesApproved(gates)
        && REQUIRED_FACEBOOK_GATES.stream().allMatch(code -> hasAuditedPass(gates, code))
        && landingEvidenceMatchesPublishedSlot(gates, slot);
  }

  /** Restringe esta prova ao funil PDE publicado pelo canal Facebook. */
  private boolean isFacebookPde(Experiment experiment) {
    return experiment != null
        && experiment.getId() != null
        && experiment.getProduct() != null
        && experiment.getPlatform() == ExperimentPlatform.FACEBOOK
        && experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL;
  }

  /** Exige publicação real, validação verde e identidade coincidente do destino. */
  private boolean isPublishedSlotForExperiment(PdeProductionSlot slot, Experiment experiment) {
    return slot != null
        && slot.getStatus() == PdeProductionSlotStatus.ACTIVE
        && VALIDATION_OK.equals(slot.getValidationStatus())
        && slot.getPublishedAt() != null
        && StringUtils.hasText(slot.getPublishedExperienceJson())
        && Objects.equals(slot.getProductSlug(), experiment.getProduct().getSlug())
        && Objects.equals(slot.getSourceExperimentId(), experiment.getId())
        && Objects.equals(
            normalizeUrl(slot.getPublicUrl()), normalizeUrl(experiment.getFollowUpActionUrl()));
  }

  /** Aceita somente o run produtivo atual concluído com dados válidos. */
  private boolean isApprovedRun(ExperimentRun run) {
    return run != null
        && run.getId() != null
        && run.getDataQualityStatus() == ExperimentRunDataQualityStatus.VALID
        && (run.getStatus() == ExperimentRunStatus.READY_TO_PUBLISH
            || run.getStatus() == ExperimentRunStatus.RUNNING);
  }

  /** Impede que qualquer falha, pendência ou alerta do mesmo run seja ignorado. */
  private boolean allGatesApproved(List<ExperimentRunGateResult> gates) {
    return gates != null
        && !gates.isEmpty()
        && gates.stream()
            .allMatch(
                gate ->
                    gate.getStatus() == ExperimentRunGateStatus.PASS
                        || gate.getStatus() == ExperimentRunGateStatus.NOT_APPLICABLE);
  }

  /** Exige aprovação positiva e referência auditável para cada gate operacional obrigatório. */
  private boolean hasAuditedPass(List<ExperimentRunGateResult> gates, String gateCode) {
    return gates.stream()
        .filter(gate -> gateCode.equals(gate.getGateCode()))
        .anyMatch(
            gate ->
                gate.getStatus() == ExperimentRunGateStatus.PASS
                    && StringUtils.hasText(gate.getEvidenceReference()));
  }

  /** Impede reutilizar a revisão visual de outra URL ou versão PDE. */
  private boolean landingEvidenceMatchesPublishedSlot(
      List<ExperimentRunGateResult> gates, PdeProductionSlot slot) {
    return gates.stream()
        .filter(
            gate ->
                ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED.equals(gate.getGateCode()))
        .map(ExperimentRunGateResult::getEvidenceReference)
        .filter(StringUtils::hasText)
        .anyMatch(
            evidence ->
                evidence.contains(normalizeUrl(slot.getPublicUrl()))
                    && evidence.contains(slot.getExperienceVersion()));
  }

  /** Normaliza somente a barra final para comparar a URL canônica sem ampliar o destino. */
  private String normalizeUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim().replaceFirst("/+$", "");
  }
}
