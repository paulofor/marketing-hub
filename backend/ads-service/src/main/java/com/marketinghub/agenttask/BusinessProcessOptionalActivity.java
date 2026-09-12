package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: reconhecer dispensa explícita e auditável sem convertê-la em objetivo atingido.
 */
@Slf4j
public final class BusinessProcessOptionalActivity {
  private static final ObjectMapper JSON = new ObjectMapper();

  /** Impede instanciar o validador sem estado. */
  private BusinessProcessOptionalActivity() {}

  /** Confere identidade e motivo da dispensa registrada pelo resolvedor backend de formatos. */
  public static boolean isOmitted(BusinessProcessActivityInstance instance) {
    if (instance == null
        || !"NOT_APPLICABLE".equals(instance.getStatus())
        || instance.isObjectiveAchieved()
        || instance.getActivityDefinition() == null
        || instance.getObjectiveEvidenceJson() == null) return false;
    try {
      var evidence = JSON.readTree(instance.getObjectiveEvidenceJson());
      return "OPTIONAL_ACTIVITY_NOT_REQUIRED_V1".equals(evidence.path("evidenceType").asText())
          && "audiovisual".equals(instance.getActivityDefinition().getActivityId())
          && "creative-production-approval"
              .equals(instance.getActivityDefinition().getProcessDefinition().getProcessCode())
          && instance
              .getActivityDefinition()
              .getActivityId()
              .equals(evidence.path("activityId").asText())
          && instance.getSourceReference().equals(evidence.path("sourceReference").asText())
          && evidence.path("communicationTaskId").asLong() > 0
          && evidence.has("audiovisualRequired")
          && !evidence.path("audiovisualRequired").asBoolean(true);
    } catch (Exception ex) {
      log.warn(
          "Dispensa de atividade inválida. activityInstanceId={} sourceReference={}",
          instance.getId(),
          instance.getSourceReference(),
          ex);
      return false;
    }
  }
}
