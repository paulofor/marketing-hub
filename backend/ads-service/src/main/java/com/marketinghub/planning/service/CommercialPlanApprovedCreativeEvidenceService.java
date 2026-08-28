package com.marketinghub.planning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: entregar à landing o pacote criativo aprovado do mesmo plano e versão. */
@Service
public class CommercialPlanApprovedCreativeEvidenceService {
  private static final Logger log =
      LoggerFactory.getLogger(CommercialPlanApprovedCreativeEvidenceService.class);
  private static final String PROCESS_CODE = "creative-production-approval";
  private static final Set<String> REQUIRED_ACTIVITIES =
      Set.of("nonAudiovisual", "audiovisual", "customer", "commercial");
  private final CommercialPlanRepository planRepository;
  private final CommercialPlanVersionService versionService;
  private final AgentTaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa a ligação entre experimento, plano versionado e tarefas aprovadas. */
  public CommercialPlanApprovedCreativeEvidenceService(
      CommercialPlanRepository planRepository,
      CommercialPlanVersionService versionService,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this.planRepository = planRepository;
    this.versionService = versionService;
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
  }

  /** Consolida materializações e pareceres do pacote sem confundir aprovação com publicação. */
  @Transactional(readOnly = true)
  public Map<String, Object> resolve(Long experimentId) {
    if (experimentId == null) return unavailable("Experimento não informado.");
    List<CommercialPlan> plans = planRepository.findByExperimentReference(experimentId);
    if (plans.isEmpty()) return unavailable("Experimento sem plano comercial.");
    CommercialPlan plan = plans.getFirst();
    int version = versionService.current(plan.getId()).versionNumber();
    String sourceReference = "commercial-plan:" + plan.getId() + "@v" + version;
    Map<String, AgentTask> latestByActivity = new LinkedHashMap<>();
    taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
        .filter(task -> "COMPLETED".equals(task.getStatus()))
        .filter(this::belongsToCreativeApproval)
        .filter(task -> REQUIRED_ACTIVITIES.contains(task.getProcessActivityId()))
        .forEach(task -> latestByActivity.put(task.getProcessActivityId(), task));
    if (!latestByActivity.keySet().containsAll(REQUIRED_ACTIVITIES)) {
      return unavailable("Pacote criativo sem todos os gates concluídos.");
    }
    try {
      JsonNode nonAudiovisual = result(latestByActivity.get("nonAudiovisual"));
      JsonNode audiovisual = result(latestByActivity.get("audiovisual"));
      JsonNode customer = result(latestByActivity.get("customer"));
      JsonNode commercial = result(latestByActivity.get("commercial"));
      Map<String, JsonNode> evidenceByActivity = new LinkedHashMap<>();
      Set<String> packageIds = new LinkedHashSet<>();
      for (String activity : REQUIRED_ACTIVITIES) {
        JsonNode activityEvidence = evidence(latestByActivity.get(activity));
        evidenceByActivity.put(activity, activityEvidence);
        String packageId = approvedPackageId(activityEvidence);
        if (packageId == null) {
          return unavailable("Pacote criativo sem evidência íntegra em " + activity + ".");
        }
        packageIds.add(packageId);
      }
      JsonNode packageEvidence = evidenceByActivity.get("commercial");
      if (!"SELECTED".equals(nonAudiovisual.path("decision").asText())
          || !"APPROVED".equals(customer.path("decision").asText())
          || !"APPROVED".equals(commercial.path("decision").asText())
          || !audiovisual.isObject()
          || audiovisual.isEmpty()
          || packageIds.size() != 1) {
        return unavailable("Pacote criativo sem aprovação independente ou segregação local.");
      }
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("status", "APPROVED");
      payload.put("sourceReference", sourceReference);
      payload.put("commercialPlanId", plan.getId());
      payload.put("planVersion", version);
      payload.put("creativePackageId", packageIds.iterator().next());
      payload.put("nonAudiovisualMaterialization", nonAudiovisual);
      payload.put("audiovisualMaterialization", audiovisual);
      payload.put("customerReview", customer);
      payload.put("commercialReview", commercial);
      payload.put("packageEvidence", packageEvidence);
      payload.put("published", false);
      payload.put("externalMediaSpendUsd", 0);
      return Map.copyOf(payload);
    } catch (Exception ex) {
      log.error(
          "Falha ao consolidar pacote criativo aprovado. experimentId={} planId={} sourceReference={}",
          experimentId,
          plan.getId(),
          sourceReference,
          ex);
      return unavailable("Pacote criativo com JSON de auditoria inválido.");
    }
  }

  /** Restringe a seleção ao processo canônico de criação e aprovação de criativos. */
  private boolean belongsToCreativeApproval(AgentTask task) {
    BusinessProcessDefinition process = task.getProcessDefinition();
    return process != null && PROCESS_CODE.equals(process.getProcessCode());
  }

  /** Lê o resultado funcional obrigatório de uma tarefa concluída. */
  private JsonNode result(AgentTask task) throws Exception {
    return objectMapper.readTree(task.getResultJson());
  }

  /** Lê a linhagem, os hashes e os ativos do pacote aprovado. */
  private JsonNode evidence(AgentTask task) throws Exception {
    return objectMapper.readTree(task.getEvidenceJson());
  }

  /** Aceita somente a mesma evidência humana, íntegra e ainda não publicada em cada gate. */
  private String approvedPackageId(JsonNode evidence) {
    if (evidence == null) return null;
    String packageId = evidence.path("creativePackageId").asText();
    boolean hasConcreteAsset = false;
    for (JsonNode asset : evidence.path("assets")) {
      if (StringUtils.hasText(asset.path("url").asText())) {
        hasConcreteAsset = true;
        break;
      }
    }
    boolean accepted =
        packageId.matches("[0-9a-f]{64}")
            && evidence.path("assets").isArray()
            && !evidence.path("assets").isEmpty()
            && hasConcreteAsset
            && evidence.path("importedByHuman").asBoolean(false)
            && evidence.has("published")
            && !evidence.path("published").asBoolean(true)
            && evidence.has("externalMediaSpendUsd")
            && evidence.path("externalMediaSpendUsd").decimalValue().signum() == 0;
    return accepted ? packageId : null;
  }

  /** Expõe ausência comprovada sem inventar anúncio ou tratar vazio como aprovação. */
  private Map<String, Object> unavailable(String reason) {
    return Map.of("status", "UNAVAILABLE", "blockReason", reason);
  }
}
