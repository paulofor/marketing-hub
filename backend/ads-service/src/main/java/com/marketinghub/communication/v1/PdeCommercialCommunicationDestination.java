package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: reconciliar um slot PDE comercial já publicado como destino do Processo 4. */
@Component
@Slf4j
public class PdeCommercialCommunicationDestination {
  static final String EVIDENCE_TYPE = "PDE_COMMERCIAL_DESTINATION_V1";
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("experiment:([1-9][0-9]*)");
  private final ExperimentRepository experiments;
  private final PdeProductionSlotRepository slots;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;
  private final Clock clock;

  /** Configura as fontes canônicas do experimento, do slot e da ocorrência do processo. */
  @Autowired
  public PdeCommercialCommunicationDestination(
      ExperimentRepository experiments,
      PdeProductionSlotRepository slots,
      BusinessProcessActivityInstanceRepository instances,
      ObjectMapper json) {
    this(experiments, slots, instances, json, Clock.systemUTC());
  }

  /** Permite testar a reconciliação com um relógio determinístico. */
  PdeCommercialCommunicationDestination(
      ExperimentRepository experiments,
      PdeProductionSlotRepository slots,
      BusinessProcessActivityInstanceRepository instances,
      ObjectMapper json,
      Clock clock) {
    this.experiments = experiments;
    this.slots = slots;
    this.instances = instances;
    this.json = json;
    this.clock = clock;
  }

  /**
   * Oferece a reutilização somente quando a referência possui um slot comercial próprio; sem slot,
   * o subprocesso normal de criação de destino continua aplicável.
   */
  @Transactional(readOnly = true)
  public Optional<BackendProductProcessActivityReadiness> readiness(
      Product product, String sourceReference) {
    Optional<Inspection> inspection = inspect(product, sourceReference);
    if (inspection.isEmpty()) return Optional.empty();
    if (!inspection.orElseThrow().blockers().isEmpty()) {
      return Optional.of(
          new BackendProductProcessActivityReadiness(
              false,
              "O destino comercial publicado precisa ser reconciliado: "
                  + String.join(" ", inspection.orElseThrow().blockers())));
    }
    return Optional.of(
        new BackendProductProcessActivityReadiness(
            true,
            "O slot PDE comercial publicado já é o destino canônico deste experimento.",
            "Reconciliar destino publicado",
            "Registra no Processo 4 a versão, a URL e o contrato comercial já homologados, sem gerar outra landing.",
            null,
            null,
            List.of()));
  }

  /** Registra a prova direta do slot publicado na atividade de destino do Processo 4. */
  @Transactional
  public BackendProductProcessActivityExecutionResult complete(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    Inspection inspection =
        inspect(product, sourceReference)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O experimento ainda não possui um slot PDE comercial para reconciliar."));
    if (!inspection.blockers().isEmpty()) {
      throw new IllegalStateException(String.join(" ", inspection.blockers()));
    }
    PdeProductionSlot slot = inspection.slot();
    Optional<BusinessProcessActivityInstance> latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), sourceReference);
    if (latest.filter(instance -> currentEvidence(instance, slot)).isPresent()) {
      return result(sourceReference, slot);
    }

    Instant now = Instant.now(clock);
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(evidence(product, inspection.experiment(), slot).toString());
    instance.setBlockedReason(null);
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instances.save(instance);
    return result(sourceReference, slot);
  }

  /** Reabre uma conclusão quando o slot, o contrato ou sua validação material mudaram. */
  @Transactional(readOnly = true)
  public boolean requiresFreshExecution(
      BusinessProcessActivityDefinition activity, Product product, String sourceReference) {
    Optional<Inspection> inspection = inspect(product, sourceReference);
    if (inspection.isEmpty()) return false;
    Inspection current = inspection.orElseThrow();
    Optional<BusinessProcessActivityInstance> latest =
        instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), sourceReference);
    return latest
        .filter(
            instance -> "COMPLETED".equals(instance.getStatus()) && instance.isObjectiveAchieved())
        .filter(
            instance -> !current.blockers().isEmpty() || !currentEvidence(instance, current.slot()))
        .isPresent();
  }

  /** Confere identidade, publicação, oferta e validação sem alterar o slot. */
  private Optional<Inspection> inspect(Product product, String sourceReference) {
    Matcher matcher =
        EXPERIMENT_REFERENCE.matcher(Objects.requireNonNullElse(sourceReference, "").trim());
    if (!matcher.matches()) return Optional.empty();
    Long experimentId = Long.valueOf(matcher.group(1));
    Experiment experiment = experiments.findById(experimentId).orElse(null);
    if (experiment == null) {
      return Optional.of(
          new Inspection(null, null, List.of("O experimento informado não foi encontrado.")));
    }
    PdeProductionSlot slot =
        slots
            .findFirstBySourceExperimentIdAndStatusInOrderByPublishedAtDesc(
                experimentId,
                List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE))
            .orElse(null);
    if (slot == null) return Optional.empty();

    List<String> blockers = new ArrayList<>();
    if (experiment.getProduct() == null
        || !Objects.equals(product.getId(), experiment.getProduct().getId())) {
      blockers.add("O experimento pertence a outro produto.");
    }
    if (!Objects.equals(experimentId, slot.getSourceExperimentId())
        || !Objects.equals(product.getSlug(), slot.getProductSlug())) {
      blockers.add("O slot pertence a outro produto ou experimento.");
    }
    if (!List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)
        .contains(slot.getStatus())) {
      blockers.add("O slot precisa estar homologado ou publicado.");
    }
    if (!StringUtils.hasText(slot.getPublishedExperienceJson()) || slot.getPublishedAt() == null) {
      blockers.add("O contrato comercial do slot ainda não foi publicado.");
    }
    if (!"OK".equals(slot.getValidationStatus())) {
      blockers.add("A URL pública precisa estar validada no contrato vigente.");
    }
    if (!https(slot.getPublicUrl())) {
      blockers.add("A URL pública do slot precisa usar HTTPS.");
    }
    validateContract(experiment, slot, blockers);
    return Optional.of(new Inspection(experiment, slot, List.copyOf(blockers)));
  }

  /** Confirma que o snapshot publicado preserva o experimento e o checkout correntes. */
  private void validateContract(
      Experiment experiment, PdeProductionSlot slot, List<String> blockers) {
    if (!StringUtils.hasText(slot.getPublishedExperienceJson())) return;
    try {
      JsonNode contract = json.readTree(slot.getPublishedExperienceJson());
      boolean experimentMatches =
          contract.path("commercialBinding").path("experimentId").asLong(0L) == experiment.getId();
      String checkoutUrl = contract.path("commercialCheckout").path("checkoutUrl").asText();
      if (!experimentMatches
          || !StringUtils.hasText(experiment.getCommercialCheckoutUrl())
          || !Objects.equals(experiment.getCommercialCheckoutUrl(), checkoutUrl)) {
        blockers.add("O contrato publicado diverge do experimento ou do checkout canônico.");
      }
    } catch (Exception ex) {
      log.error(
          "Falha ao validar o contrato do destino comercial. experimentId={} slotId={}",
          experiment.getId(),
          slot.getId(),
          ex);
      blockers.add("O contrato comercial publicado contém JSON inválido.");
    }
  }

  /** Monta a evidência mínima que liga a atividade à mesma versão comercial publicada. */
  private ObjectNode evidence(Product product, Experiment experiment, PdeProductionSlot slot) {
    ObjectNode evidence = json.createObjectNode();
    evidence.put("evidenceType", EVIDENCE_TYPE);
    evidence.put("source", "PUBLISHED_PDE_SLOT");
    evidence.put("productId", product.getId());
    evidence.put("experimentId", experiment.getId());
    evidence.put("slotId", slot.getId());
    evidence.put("slotCode", slot.getSlotCode());
    evidence.put("experienceVersion", slot.getExperienceVersion());
    evidence.put("publicUrl", slot.getPublicUrl());
    evidence.put("validationStatus", slot.getValidationStatus());
    evidence.put("publishedContractSha256", sha256(slot.getPublishedExperienceJson()));
    evidence.put("publicationAuthorized", false);
    evidence.put("mediaSpendAuthorized", false);
    return evidence;
  }

  /** Confirma que a conclusão anterior representa exatamente o slot ainda vigente. */
  private boolean currentEvidence(
      BusinessProcessActivityInstance instance, PdeProductionSlot slot) {
    if (slot == null
        || !"COMPLETED".equals(instance.getStatus())
        || !instance.isObjectiveAchieved()
        || !StringUtils.hasText(instance.getObjectiveEvidenceJson())) return false;
    try {
      JsonNode evidence = json.readTree(instance.getObjectiveEvidenceJson());
      return EVIDENCE_TYPE.equals(evidence.path("evidenceType").asText())
          && Objects.equals(slot.getId(), evidence.path("slotId").longValue())
          && Objects.equals(
              slot.getExperienceVersion(), evidence.path("experienceVersion").asText())
          && Objects.equals(slot.getPublicUrl(), evidence.path("publicUrl").asText())
          && Objects.equals(
              sha256(slot.getPublishedExperienceJson()),
              evidence.path("publishedContractSha256").asText())
          && "OK".equals(slot.getValidationStatus());
    } catch (Exception ex) {
      log.warn(
          "Prova do destino comercial está inválida. activityInstanceId={} slotId={}",
          instance.getId(),
          slot.getId(),
          ex);
      return false;
    }
  }

  /** Devolve o resultado funcional comum às execuções novas e idempotentes. */
  private BackendProductProcessActivityExecutionResult result(
      String sourceReference, PdeProductionSlot slot) {
    return new BackendProductProcessActivityExecutionResult(
        sourceReference,
        "COMPLETED",
        true,
        "O destino comercial "
            + slot.getExperienceVersion()
            + " foi reconciliado sem gerar outra landing.");
  }

  /** Identifica os bytes do contrato publicado para detectar qualquer alteração posterior. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha ao identificar o contrato do destino comercial.", ex);
      throw new IllegalStateException("Não foi possível identificar o contrato comercial.", ex);
    }
  }

  /** Reconhece somente URLs públicas HTTPS completas. */
  private boolean https(String value) {
    return StringUtils.hasText(value) && value.trim().startsWith("https://");
  }

  /** Agrupa as fontes exatas e todos os impedimentos encontrados na mesma inspeção. */
  private record Inspection(Experiment experiment, PdeProductionSlot slot, List<String> blockers) {}
}
