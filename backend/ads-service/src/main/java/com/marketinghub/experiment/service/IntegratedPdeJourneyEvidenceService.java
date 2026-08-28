package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: reconhecer a integração comercial PDE concluída por evidência persistida. */
@Service
@Slf4j
public class IntegratedPdeJourneyEvidenceService {
  private static final String PROCESS_CODE = "pde-communication-sales-journey";
  private static final String ACTIVITY_ID = "integration";

  private final PdeProductionSlotRepository slotRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes que provam slot, atribuição e conclusão BPM da mesma jornada. */
  public IntegratedPdeJourneyEvidenceService(
      PdeProductionSlotRepository slotRepository,
      CommercialPlanRepository commercialPlanRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ObjectMapper objectMapper) {
    this.slotRepository = slotRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.objectMapper = objectMapper;
  }

  /** Informa se o experimento possui uma superfície PDE versionada vinculada ao próprio produto. */
  @Transactional(readOnly = true)
  public boolean appliesTo(Experiment experiment) {
    if (experiment == null || experiment.getId() == null || experiment.getProduct() == null) {
      return false;
    }
    return slotRepository
        .findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId())
        .filter(slot -> Objects.equals(slot.getProductSlug(), experiment.getProduct().getSlug()))
        .isPresent();
  }

  /**
   * Exige slot válido e instância BPM concluída com IDs coincidentes antes de liberar prontidão.
   */
  @Transactional(readOnly = true)
  public boolean isReady(Experiment experiment) {
    if (!appliesTo(experiment)) {
      return false;
    }
    PdeProductionSlot slot =
        slotRepository
            .findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId())
            .orElseThrow();
    if ((slot.getStatus() != PdeProductionSlotStatus.READY
            && slot.getStatus() != PdeProductionSlotStatus.ACTIVE)
        || !"OK".equals(slot.getValidationStatus())) {
      return false;
    }
    return integrationInstances(experiment).values().stream()
        .filter(instance -> "COMPLETED".equals(instance.getStatus()))
        .filter(BusinessProcessActivityInstance::isObjectiveAchieved)
        .anyMatch(instance -> evidenceMatches(instance, experiment, slot));
  }

  /** Reúne ocorrências do experimento e de seus planos sem duplicar a mesma instância. */
  private Map<Long, BusinessProcessActivityInstance> integrationInstances(Experiment experiment) {
    Map<Long, BusinessProcessActivityInstance> instances = new LinkedHashMap<>();
    activityInstanceRepository
        .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
            PROCESS_CODE, "experiment:" + experiment.getId())
        .stream()
        .filter(this::isIntegration)
        .forEach(instance -> instances.put(instance.getId(), instance));
    for (CommercialPlan plan :
        commercialPlanRepository.findByExperimentReference(experiment.getId())) {
      activityInstanceRepository
          .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
              PROCESS_CODE, "commercial-plan:" + plan.getId() + "@")
          .stream()
          .filter(this::isIntegration)
          .forEach(instance -> instances.put(instance.getId(), instance));
    }
    return instances;
  }

  /** Confirma que a ocorrência pertence à atividade de integração, não apenas ao processo pai. */
  private boolean isIntegration(BusinessProcessActivityInstance instance) {
    return instance.getActivityDefinition() != null
        && ACTIVITY_ID.equals(instance.getActivityDefinition().getActivityId());
  }

  /** Valida os identificadores estruturados sem aceitar evidência textual ou de outro produto. */
  private boolean evidenceMatches(
      BusinessProcessActivityInstance instance, Experiment experiment, PdeProductionSlot slot) {
    try {
      JsonNode evidence = objectMapper.readTree(instance.getObjectiveEvidenceJson());
      return "PDE_SALES_JOURNEY_INTEGRATION_V1".equals(evidence.path("evidenceType").asText())
          && Objects.equals(experiment.getId(), evidence.path("experimentId").longValue())
          && Objects.equals(experiment.getProduct().getId(), evidence.path("productId").longValue())
          && Objects.equals(slot.getId(), evidence.path("slotId").longValue())
          && !evidence.path("publicationAuthorized").asBoolean(true)
          && !evidence.path("mediaSpendAuthorized").asBoolean(true);
    } catch (Exception ex) {
      log.warn(
          "Falha ao validar evidência da integração PDE. activityInstanceId={} experimentId={}",
          instance.getId(),
          experiment.getId(),
          ex);
      return false;
    }
  }
}
