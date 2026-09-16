package com.marketinghub.pde.service.versionoverview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.pde.service.promotion.PdeV12PublicationPolicy;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoPanelDto;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consolidar a trajetória de negócio das versões PDE de produtos Opala. */
@Service
@Slf4j
public class PdeVersionOverviewService {

  private static final String OPALA_PRODUCT_TYPE_CODE = "PDE";
  private static final String OPALA_PRODUCT_TYPE_INTERNAL_NAME = "OPALA";
  private static final String STEP_DONE = "DONE";
  private static final String STEP_CURRENT = "CURRENT";
  private static final String STEP_PENDING = "PENDING";
  private static final String STEP_BLOCKED = "BLOCKED";

  private final PdeProductionSlotRepository slotRepository;
  private final ExperimentRepository experimentRepository;
  private final ExperimentVideoAssetRepository videoAssetRepository;
  private final PdeProductionSlotService slotService;
  private final ObjectMapper objectMapper;

  /** Inicializa a leitura consolidada com as fontes canônicas de versão, experimento e vídeo. */
  public PdeVersionOverviewService(
      PdeProductionSlotRepository slotRepository,
      ExperimentRepository experimentRepository,
      ExperimentVideoAssetRepository videoAssetRepository,
      PdeProductionSlotService slotService,
      ObjectMapper objectMapper) {
    this.slotRepository = slotRepository;
    this.experimentRepository = experimentRepository;
    this.videoAssetRepository = videoAssetRepository;
    this.slotService = slotService;
    this.objectMapper = objectMapper;
  }

  /** Lista versões do produto Opala sem criar uma identidade privada paralela para homologação. */
  @Transactional(readOnly = true)
  public List<ProductPdeVersionOverviewDto> list(Product product) {
    validateOpalaProduct(product);
    List<PdeProductionSlot> slots =
        slotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug());
    Map<Long, Experiment> experiments = loadExperiments(slots);
    Map<String, PdeProductionSlotVideoPanelDto> videosBySlot =
        slotService.listProductionSlotVideosForProduct(product.getSlug()).stream()
            .collect(
                Collectors.toMap(
                    panel -> panel.slot().slotCode(),
                    panel -> panel,
                    (current, ignored) -> current,
                    LinkedHashMap::new));
    return slots.stream()
        .map(
            slot ->
                toOverview(
                    product,
                    slot,
                    Optional.ofNullable(slot.getSourceExperimentId())
                        .map(experiments::get)
                        .orElse(null),
                    videosBySlot.get(slot.getSlotCode())))
        .toList();
  }

  /** Impede que a tela específica de Opala seja alimentada por outro tipo de produto. */
  private void validateOpalaProduct(Product product) {
    String typeCode =
        product.getProductTypeDefinition() == null
            ? null
            : product.getProductTypeDefinition().getCode();
    String internalName =
        product.getProductTypeDefinition() == null
            ? null
            : product.getProductTypeDefinition().getInternalName();
    boolean opala =
        OPALA_PRODUCT_TYPE_CODE.equalsIgnoreCase(typeCode)
            || OPALA_PRODUCT_TYPE_INTERNAL_NAME.equalsIgnoreCase(internalName);
    if (!opala) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Versões PDE estão disponíveis somente para produtos Opala.");
    }
  }

  /** Carrega em lote os experimentos que contextualizam as versões cadastradas. */
  private Map<Long, Experiment> loadExperiments(List<PdeProductionSlot> slots) {
    List<Long> ids =
        slots.stream()
            .map(PdeProductionSlot::getSourceExperimentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, Experiment> experiments = new LinkedHashMap<>();
    experimentRepository
        .findAllById(ids)
        .forEach(experiment -> experiments.put(experiment.getId(), experiment));
    return experiments;
  }

  /** Converte as evidências persistidas da versão em uma visão administrativa única. */
  private ProductPdeVersionOverviewDto toOverview(
      Product product,
      PdeProductionSlot slot,
      Experiment experiment,
      PdeProductionSlotVideoPanelDto videoPanel) {
    JsonNode contract = readContract(slot);
    int videoCount = videoPanel == null ? 0 : videoPanel.videos().size();
    int approvedVideoCount =
        videoPanel == null
            ? 0
            : (int)
                videoPanel.videos().stream()
                    .filter(
                        video ->
                            video.status() == ExperimentVideoStatus.READY
                                && video.reviewStatus() == ExperimentVideoReviewStatus.APPROVED)
                    .count();
    String hypothesis =
        firstText(
            experiment == null ? null : experiment.getHypothesis(), product.getPrimaryHypothesis());
    String primaryChange =
        firstText(
            slot.getNotes(),
            text(contract, "changeSummary"),
            text(contract, "versionChange"),
            text(contract.path("commercialBinding"), "changeSummary"));
    String lifecycleStage = lifecycleStage(slot.getStatus());
    List<String> pendingItems =
        pendingItems(
            slot,
            experiment,
            hypothesis,
            videoCount,
            approvedVideoCount,
            contract,
            slot.getSourceExperimentId() == null
                ? List.of()
                : videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(
                    slot.getSourceExperimentId()));
    boolean canPreparePublication =
        PdeV12PublicationPolicy.appliesTo(slot)
            && slot.getStatus() == PdeProductionSlotStatus.CANDIDATE
            && pendingItems.stream().allMatch("Concluir a homologação comercial da v12."::equals);
    boolean canPublishContract =
        pendingItems.isEmpty()
            && (slot.getStatus() == PdeProductionSlotStatus.READY
                || slot.getStatus() == PdeProductionSlotStatus.ACTIVE);
    return new ProductPdeVersionOverviewDto(
        slot.getId(),
        slot.getSlotCode(),
        firstText(text(contract, "name"), slot.getExperienceVersion()),
        slot.getExperienceVersion(),
        lifecycleStage,
        lifecycleLabel(lifecycleStage),
        slot.getStatus(),
        hypothesis,
        primaryChange,
        slot.getSourceExperimentId(),
        experiment == null ? null : experiment.getName(),
        experiment == null || experiment.getStatus() == null ? null : experiment.getStatus().name(),
        videoCount,
        approvedVideoCount,
        experiment == null ? null : experiment.getUnitPrice(),
        experiment == null ? null : experiment.getPrimaryCta(),
        experiment == null ? null : experiment.getCommercialCheckoutUrl(),
        slot.getPublicUrl(),
        slot.getValidationStatus(),
        slot.getValidationSummary(),
        slot.getValidationCheckedAt(),
        homologationSummary(slot),
        StringUtils.hasText(slot.getPublishedExperienceJson()),
        canPreparePublication,
        canPublishContract,
        pendingItems,
        lifecycle(slot, hypothesis, approvedVideoCount, experiment),
        slot.getUpdatedAt());
  }

  /** Lê o contrato mais recente sem tratar JSON inválido como evidência de construção concluída. */
  private JsonNode readContract(PdeProductionSlot slot) {
    String content =
        StringUtils.hasText(slot.getDraftExperienceJson())
            ? slot.getDraftExperienceJson()
            : slot.getPublishedExperienceJson();
    if (!StringUtils.hasText(content)) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(content);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler contrato da visão de versões PDE: productSlug={}, slotCode={}, experienceVersion={}",
          slot.getProductSlug(),
          slot.getSlotCode(),
          slot.getExperienceVersion(),
          ex);
      return objectMapper.createObjectNode();
    }
  }

  /** Deriva somente pendências comprováveis pelas fontes persistidas da própria versão. */
  private List<String> pendingItems(
      PdeProductionSlot slot,
      Experiment experiment,
      String hypothesis,
      int videoCount,
      int approvedVideoCount,
      JsonNode contract,
      List<ExperimentVideoAsset> experimentVideos) {
    List<String> pending = new ArrayList<>();
    if (!StringUtils.hasText(hypothesis)) pending.add("Definir a hipótese da versão.");
    if (!StringUtils.hasText(slot.getDraftExperienceJson())
        && !StringUtils.hasText(slot.getPublishedExperienceJson())) {
      pending.add("Construir o contrato da experiência.");
    }
    if (PdeV12PublicationPolicy.appliesTo(slot)) {
      pending.addAll(
          PdeV12PublicationPolicy.blockers(slot, experiment, experimentVideos, contract, true));
    } else {
      addGenericCommercialPendingItems(pending, slot, experiment, videoCount, approvedVideoCount);
    }
    return List.copyOf(pending);
  }

  /** Preserva a leitura das versões históricas que ainda não usam o contrato de promoção v12. */
  private void addGenericCommercialPendingItems(
      List<String> pending,
      PdeProductionSlot slot,
      Experiment experiment,
      int videoCount,
      int approvedVideoCount) {
    if (videoCount == 0) {
      pending.add("Vincular o vídeo de apresentação.");
    } else if (approvedVideoCount == 0) {
      pending.add("Aprovar o vídeo de apresentação.");
    }
    if (experiment == null) {
      pending.add("Vincular o experimento que utilizará a versão.");
    } else {
      if (experiment.getUnitPrice() == null
          || experiment.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
        pending.add("Definir o preço da oferta.");
      }
      if (!StringUtils.hasText(experiment.getCommercialCheckoutUrl())) {
        pending.add("Configurar o checkout da oferta.");
      }
    }
    if (!"OK".equals(slot.getValidationStatus())) {
      pending.add("Concluir os testes da URL e da jornada pública.");
    }
    if (slot.getStatus() != PdeProductionSlotStatus.READY
        && slot.getStatus() != PdeProductionSlotStatus.ACTIVE) {
      pending.add("Concluir a homologação comercial.");
    }
  }

  /** Monta a trajetória ordenada e destaca a primeira etapa ainda não comprovada. */
  private List<PdeVersionLifecycleStepDto> lifecycle(
      PdeProductionSlot slot, String hypothesis, int approvedVideoCount, Experiment experiment) {
    boolean commercialPreparation =
        experiment != null
            && experiment.getUnitPrice() != null
            && experiment.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
            && StringUtils.hasText(experiment.getCommercialCheckoutUrl());
    boolean homologated =
        slot.getStatus() == PdeProductionSlotStatus.READY
            || slot.getStatus() == PdeProductionSlotStatus.ACTIVE;
    List<LifecycleEvidence> evidence =
        List.of(
            new LifecycleEvidence(
                "CONCEPT", "Conceito", () -> StringUtils.hasText(hypothesis), false),
            new LifecycleEvidence(
                "BUILD",
                "Construção",
                () ->
                    StringUtils.hasText(slot.getDraftExperienceJson())
                        || StringUtils.hasText(slot.getPublishedExperienceJson()),
                false),
            new LifecycleEvidence("ASSETS", "Ativos", () -> approvedVideoCount > 0, false),
            new LifecycleEvidence(
                "TESTS",
                "Testes",
                () -> "OK".equals(slot.getValidationStatus()),
                "FAILED".equals(slot.getValidationStatus())),
            new LifecycleEvidence("HOMOLOGATION", "Homologação", () -> homologated, false),
            new LifecycleEvidence(
                "COMMERCIAL_PREPARATION",
                "Preparação comercial",
                () -> commercialPreparation,
                false),
            new LifecycleEvidence(
                "PUBLICATION",
                "Publicação",
                () -> slot.getStatus() == PdeProductionSlotStatus.ACTIVE,
                false));
    List<PdeVersionLifecycleStepDto> steps = new ArrayList<>();
    boolean currentAssigned = false;
    for (LifecycleEvidence item : evidence) {
      String status;
      if (item.complete().getAsBoolean()) {
        status = STEP_DONE;
      } else if (item.blocked()) {
        status = STEP_BLOCKED;
        currentAssigned = true;
      } else if (!currentAssigned) {
        status = STEP_CURRENT;
        currentAssigned = true;
      } else {
        status = STEP_PENDING;
      }
      steps.add(new PdeVersionLifecycleStepDto(item.code(), item.label(), status));
    }
    return List.copyOf(steps);
  }

  /** Traduz o estado operacional persistido para o ciclo de vida apresentado à pessoa usuária. */
  private String lifecycleStage(PdeProductionSlotStatus status) {
    if (status == null) return "DRAFT";
    return switch (status) {
      case PLANNED -> "DRAFT";
      case CANDIDATE -> "CANDIDATE";
      case READY -> "HOMOLOGATED";
      case ACTIVE -> "PUBLISHED";
      case PAUSED -> "PAUSED";
      case RETIRED -> "ARCHIVED";
    };
  }

  /** Retorna o rótulo de negócio do estágio sem expor o código operacional bruto. */
  private String lifecycleLabel(String stage) {
    return switch (stage) {
      case "CANDIDATE" -> "Candidata";
      case "HOMOLOGATED" -> "Homologada";
      case "PUBLISHED" -> "Publicada";
      case "PAUSED" -> "Pausada";
      case "ARCHIVED" -> "Arquivada";
      default -> "Rascunho";
    };
  }

  /** Resume a homologação sem transformar ausência de evidência em aprovação. */
  private String homologationSummary(PdeProductionSlot slot) {
    if (slot.getStatus() == PdeProductionSlotStatus.ACTIVE) {
      return "Versão publicada; a URL e as métricas ainda devem ser acompanhadas.";
    }
    if (slot.getStatus() == PdeProductionSlotStatus.READY) {
      return "Homologação concluída; versão pronta para a decisão de publicação.";
    }
    if ("FAILED".equals(slot.getValidationStatus())) {
      return firstText(slot.getValidationSummary(), "Testes técnicos reprovados.");
    }
    if ("OK".equals(slot.getValidationStatus())) {
      return "Testes técnicos aprovados; homologação comercial ainda pendente.";
    }
    return "Homologação ainda não concluída.";
  }

  /** Extrai um texto simples de um contrato opcional. */
  private String text(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field) || !node.get(field).isTextual()) return null;
    return normalize(node.get(field).asText());
  }

  /** Seleciona o primeiro texto preenchido sem inventar fallback comercial. */
  private String firstText(String... values) {
    for (String value : values) {
      String normalized = normalize(value);
      if (normalized != null) return normalized;
    }
    return null;
  }

  /** Normaliza textos opcionais preservando o conteúdo persistido. */
  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Agrupa a evidência mínima necessária para uma etapa da trajetória. */
  private record LifecycleEvidence(
      String code, String label, BooleanSupplier complete, boolean blocked) {}
}
