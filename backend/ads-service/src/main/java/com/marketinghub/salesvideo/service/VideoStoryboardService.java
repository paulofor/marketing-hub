package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.ProviderTaskConsumptionView;
import com.marketinghub.financialagent.service.StudioProviderTaskConsumptionQueryService;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.storyboard.CommercialSceneEvaluationRequest;
import com.marketinghub.salesvideo.dto.storyboard.VideoStoryboardResponse;
import com.marketinghub.salesvideo.dto.storyboard.VideoStoryboardSceneResponse;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: compor a visão auditável de storyboard a partir das fontes canônicas. */
@Component
public class VideoStoryboardService {
  private final VideoProjectRepository projectRepository;
  private final SalesVideoJobRepository jobRepository;
  private final StudioProviderTaskConsumptionQueryService taskConsumptionQueryService;
  private final ObjectMapper objectMapper;

  /** Inicializa a composição com projeto, jobs, consumo financeiro e parser JSON. */
  public VideoStoryboardService(
      VideoProjectRepository projectRepository,
      SalesVideoJobRepository jobRepository,
      StudioProviderTaskConsumptionQueryService taskConsumptionQueryService,
      ObjectMapper objectMapper) {
    this.projectRepository = projectRepository;
    this.jobRepository = jobRepository;
    this.taskConsumptionQueryService = taskConsumptionQueryService;
    this.objectMapper = objectMapper;
  }

  /** Lista planejamento, consumo, arquivo e aproveitamento real de cada cena do projeto. */
  @Transactional(readOnly = true)
  public VideoStoryboardResponse getStoryboard(Long projectId) {
    VideoProject project = loadAccessibleProject(projectId);
    List<String> plans = plannedScenes(project.getScenePlan());
    List<SalesVideoJob> jobs =
        project.getSalesVideoProfileId() == null
            ? List.of()
            : jobRepository.findByProfileIdOrderByRequestedAtDesc(project.getSalesVideoProfileId());
    Map<Long, SalesVideoJob> projectJobs = new LinkedHashMap<>();
    for (SalesVideoJob job : jobs) {
      if (projectId.equals(readProjectId(job))) projectJobs.put(job.getId(), job);
    }
    List<ProviderTaskConsumptionView> tasks =
        projectJobs.isEmpty()
            ? List.of()
            : taskConsumptionQueryService.findBySalesVideoJobIds(projectJobs.keySet());
    Set<Long> usedSourceJobs = usedSourceJobs(jobs, projectId);
    List<VideoStoryboardSceneResponse> scenes = new ArrayList<>();
    Set<Integer> scenesWithTask = new HashSet<>();
    for (ProviderTaskConsumptionView task : tasks) {
      SalesVideoJob job = projectJobs.get(task.salesVideoJobId());
      int sceneNumber = positive(task.sceneNumber(), 1);
      scenesWithTask.add(sceneNumber);
      boolean produced =
          job != null && job.getAsset() != null && StringUtils.hasText(job.getAsset().getUrl());
      boolean used = produced && usedSourceJobs.contains(job.getId());
      scenes.add(
          new VideoStoryboardSceneResponse(
              task.id(),
              sceneNumber,
              commercialRole(
                  job, sceneNumber, Math.max(plans.size(), positive(task.plannedSceneCount(), 1))),
              planAt(plans, sceneNumber),
              job != null ? job.getId() : task.salesVideoJobId(),
              job != null && job.getStatus() != null ? job.getStatus().name() : null,
              task.providerTaskId(),
              task.durationSeconds(),
              task.estimatedCredits(),
              task.billedCredits(),
              produced ? job.getAsset().getUrl() : null,
              task.commercialUtilizationPercent() != null
                  ? task.commercialUtilizationPercent()
                  : produced ? (used ? 100 : 0) : null,
              produced
                  ? used ? "USED_IN_READY_MONTAGE" : "PRODUCED_NOT_USED_IN_READY_MONTAGE"
                  : "NO_PRODUCED_FILE",
              task.commercialEvaluationStatus(),
              task.commercialEvaluationNotes(),
              task.commercialEvaluatedBy(),
              task.commercialEvaluatedAt()));
    }
    appendUnrequestedPlans(plans, scenesWithTask, scenes);
    scenes.sort(
        java.util.Comparator.comparingInt(VideoStoryboardSceneResponse::sceneNumber)
            .thenComparing(scene -> scene.jobId() == null ? Long.MAX_VALUE : scene.jobId()));
    int expectedCredits =
        tasks.stream().mapToInt(task -> positive(task.estimatedCredits(), 0)).sum();
    int consumedCredits = tasks.stream().mapToInt(task -> positive(task.billedCredits(), 0)).sum();
    List<VideoStoryboardSceneResponse> producedScenes =
        scenes.stream().filter(scene -> scene.utilizationPercent() != null).toList();
    Integer utilization =
        producedScenes.isEmpty()
            ? null
            : producedScenes.stream()
                    .mapToInt(VideoStoryboardSceneResponse::utilizationPercent)
                    .sum()
                / producedScenes.size();
    JsonNode planning = latestPlanning(projectJobs.values());
    return new VideoStoryboardResponse(
        projectId,
        plans.size(),
        expectedCredits,
        consumedCredits,
        utilization,
        planning.path("apollo_planner_status").asText(null),
        planning.path("apollo_planner_model").asText(null),
        planning.path("budgetGate").asText(null),
        planning.has("expectedCostUsd") ? planning.path("expectedCostUsd").decimalValue() : null,
        scenes);
  }

  /** Valida o vínculo da task ao projeto antes de persistir sua avaliação comercial. */
  @Transactional
  public VideoStoryboardResponse evaluateScene(
      Long projectId, Long consumptionId, CommercialSceneEvaluationRequest request) {
    VideoProject project = loadAccessibleProject(projectId);
    List<Long> jobIds =
        project.getSalesVideoProfileId() == null
            ? List.of()
            : jobRepository
                .findByProfileIdOrderByRequestedAtDesc(project.getSalesVideoProfileId())
                .stream()
                .filter(job -> projectId.equals(readProjectId(job)))
                .map(SalesVideoJob::getId)
                .toList();
    boolean belongs =
        taskConsumptionQueryService.findBySalesVideoJobIds(jobIds).stream()
            .anyMatch(task -> consumptionId.equals(task.id()));
    if (!belongs) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.BAD_REQUEST, "Cena do storyboard não encontrada.");
    }
    taskConsumptionQueryService.evaluate(
        consumptionId,
        request.status(),
        request.utilizationPercent(),
        request.notes(),
        request.evaluatedBy());
    return getStoryboard(projectId);
  }

  /** Localiza o planejamento de IA mais recente preservado em um job do projeto. */
  private JsonNode latestPlanning(java.util.Collection<SalesVideoJob> jobs) {
    return jobs.stream()
        .sorted(
            java.util.Comparator.comparing(
                SalesVideoJob::getRequestedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
        .map(this::effectiveMetadata)
        .filter(metadata -> metadata.has("apollo_planner_status"))
        .findFirst()
        .orElse(objectMapper.createObjectNode());
  }

  /** Acrescenta cenas planejadas que ainda não chegaram a um provider. */
  private void appendUnrequestedPlans(
      List<String> plans, Set<Integer> scenesWithTask, List<VideoStoryboardSceneResponse> scenes) {
    for (int index = 0; index < plans.size(); index++) {
      int sceneNumber = index + 1;
      if (scenesWithTask.contains(sceneNumber)) continue;
      scenes.add(
          new VideoStoryboardSceneResponse(
              null,
              sceneNumber,
              fallbackRole(sceneNumber, plans.size()),
              plans.get(index),
              null,
              "NOT_REQUESTED",
              null,
              null,
              null,
              null,
              null,
              null,
              "NO_PROVIDER_TASK",
              null,
              null,
              null,
              null));
    }
  }

  /** Carrega somente projeto pertencente ao tenant atual. */
  private VideoProject loadAccessibleProject(Long projectId) {
    VideoProject project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.BAD_REQUEST, "Projeto de vídeo não encontrado."));
    if (!TenantContextHolder.requireTenant().equals(project.getTenantId())) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.BAD_REQUEST, "Projeto de vídeo não encontrado.");
    }
    return project;
  }

  /** Extrai as linhas não vazias do plano editorial persistido. */
  private List<String> plannedScenes(String scenePlan) {
    if (!StringUtils.hasText(scenePlan)) return List.of();
    return scenePlan.lines().map(String::trim).filter(StringUtils::hasText).toList();
  }

  /** Identifica jobs de cena efetivamente usados por uma montagem pronta do mesmo projeto. */
  private Set<Long> usedSourceJobs(List<SalesVideoJob> jobs, Long projectId) {
    Set<Long> used = new HashSet<>();
    Map<Long, SalesVideoJob> byId = new LinkedHashMap<>();
    jobs.forEach(job -> byId.put(job.getId(), job));
    for (SalesVideoJob job : jobs) {
      if (job.getJobType() != SalesVideoJobType.RENDER
          || !"VIDEO_READY".equals(String.valueOf(job.getStatus()))) continue;
      JsonNode sourceIds = readJson(job.getMetadataJson()).path("sourceJobIds");
      if (!sourceIds.isArray()) continue;
      for (JsonNode sourceId : sourceIds) {
        SalesVideoJob source = byId.get(sourceId.asLong());
        if (source != null && projectId.equals(readProjectId(source))) used.add(source.getId());
      }
    }
    return used;
  }

  /** Lê a identidade do projeto preservada no metadata ou snapshot do job. */
  private Long readProjectId(SalesVideoJob job) {
    JsonNode value = effectiveMetadata(job).path("studio_project_id");
    return value.canConvertToLong() && value.asLong() > 0 ? value.asLong() : null;
  }

  /** Resolve o metadata original da cena mesmo após a conclusão substituir seus campos. */
  private JsonNode effectiveMetadata(SalesVideoJob job) {
    JsonNode metadata = readJson(job.getMetadataJson());
    if (metadata.has("studio_project_id")) return metadata;
    String renderMetadata =
        readJson(job.getAuditSnapshotJson()).path("renderMetadataJson").asText(null);
    return StringUtils.hasText(renderMetadata) ? readJson(renderMetadata) : metadata;
  }

  /** Resolve a função comercial persistida, com fallback determinístico do plano. */
  private String commercialRole(SalesVideoJob job, int sceneNumber, int sceneCount) {
    if (job != null) {
      String role = effectiveMetadata(job).path("scene").path("role").asText(null);
      if (StringUtils.hasText(role)) return role;
    }
    return fallbackRole(sceneNumber, sceneCount);
  }

  /** Classifica a função comercial quando a versão histórica ainda não a persistia. */
  private String fallbackRole(int sceneNumber, int sceneCount) {
    if (sceneNumber == 1) return "DOR";
    if (sceneNumber == sceneCount) return "CTA";
    if (sceneNumber == 2) return "RESULTADO";
    return "MECANISMO";
  }

  /** Obtém a descrição planejada para uma posição sem inventar conteúdo legado. */
  private String planAt(List<String> plans, int sceneNumber) {
    return sceneNumber > 0 && sceneNumber <= plans.size() ? plans.get(sceneNumber - 1) : null;
  }

  /** Converte inteiro nulo ou não positivo para um fallback seguro. */
  private int positive(Integer value, int fallback) {
    return value != null && value > 0 ? value : fallback;
  }

  /** Lê JSON tolerando metadados legados vazios ou inválidos. */
  private JsonNode readJson(String json) {
    if (!StringUtils.hasText(json)) return objectMapper.createObjectNode();
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      return objectMapper.createObjectNode();
    }
  }
}
