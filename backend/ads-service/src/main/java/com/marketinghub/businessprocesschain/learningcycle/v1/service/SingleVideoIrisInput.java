package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.agenttask.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: delimitar o briefing de vídeo autorizado pelo usuário, independente da
 * estratégia ainda em planejamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SingleVideoIrisInput implements AgentTaskPrivateVideoInputProvider {
  private final VideoProductionCycleRepository videoCycles;
  private final VideoProjectRepository projects;
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleSingleVideoBudget budgets;
  private final AgentTaskRepository tasks;
  private final LearningCycleJson json;

  /**
   * Reúne projeto e fontes herdadas como fatos, sem revalidar pareceres ou renovar autorizações
   * antigas.
   */
  @Override
  public Map<String, Object> forTask(AgentTask task) {
    if (task == null
        || task.getAssignedAgent() == null
        || task.getId() == null
        || !"communication-director".equals(task.getAssignedAgent().getAgentKey())
        || task.getProcessDefinition() == null
        || !"pde-communication-sales-journey".equals(task.getProcessDefinition().getProcessCode())
        || !"communicationContract".equals(task.getProcessActivityId())) return Map.of();
    var linked = videoCycles.findByIrisAgentTaskId(task.getId());
    if (linked.isEmpty()) return Map.of();
    if (linked.size() != 1)
      throw new IllegalStateException("Briefing vinculado a mais de um pedido de vídeo.");
    var video = linked.getFirst();
    var project = projects.findById(video.getVideoProjectId()).orElseThrow();
    var receipt = budgets.protectedReceipt(project);
    if (receipt == null
        || !receipt.current()
        || !Objects.equals(task.getSourceReference(), "experiment:" + project.getExperimentId())
        || !Objects.equals(video.getProductId(), project.getProductId())
        || !Objects.equals(video.getExperimentId(), project.getExperimentId()))
      throw new IllegalStateException("Briefing de vídeo sem recibo e contexto vigentes.");
    var commercial = cycles.findByExperimentId(project.getExperimentId()).orElseThrow();
    var inputs = new LinkedHashMap<String, Object>();
    inputs.put("contractVersion", "STUDIO_PRIVATE_VIDEO_BRIEF_V1");
    inputs.put("scope", "SINGLE_PRIVATE_VIDEO_ONLY");
    inputs.put("sourceReference", task.getSourceReference());
    inputs.put("taskId", task.getId());
    inputs.put("videoProductionCycleId", video.getId());
    inputs.put("videoProjectId", project.getId());
    inputs.put("receiptId", receipt.eventId());
    inputs.put("totalLimitUsd", receipt.totalLimitUsd());
    inputs.put("productVersion", project.getCampaignKey());
    inputs.put("project", json.read(json.write(project)));
    inputs.put("inheritedCommunication", inherited(commercial.getInheritedLearningJson()));
    inputs.put("commercialPublicationAuthorized", false);
    inputs.put("newStrategyApproved", false);
    inputs.put("mediaAuthorized", false);
    inputs.put("qualityApproved", false);
    try {
      inputs.put(
          "inputSha256",
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(json.write(inputs).getBytes(StandardCharsets.UTF_8))));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error(
          "Falha de integridade do briefing privado; taskId={} projectId={}",
          task.getId(),
          project.getId(),
          ex);
      throw new IllegalStateException("Briefing sem integridade", ex);
    }
    return Collections.unmodifiableMap(inputs);
  }

  /**
   * Lê apenas comunicações explicitamente citadas na herança; BRIEF_ONLY continua sendo
   * especificação.
   */
  private List<Map<String, Object>> inherited(String inheritedJson) {
    var history = json.read(inheritedJson);
    var ids = new LinkedHashSet<Long>();
    var pattern = Pattern.compile("agent-task:([1-9][0-9]*)");
    for (var event : history.path("events")) {
      var matcher = pattern.matcher(event.path("evidenceReference").asText());
      while (matcher.find()) ids.add(Long.valueOf(matcher.group(1)));
    }
    var results = new ArrayList<Map<String, Object>>();
    for (Long id : ids) {
      var old = tasks.findById(id).orElse(null);
      if (old == null
          || old.getAssignedAgent() == null
          || !"COMPLETED".equals(old.getStatus())
          || !"communication-director".equals(old.getAssignedAgent().getAgentKey())
          || old.getResultJson() == null) continue;
      var result = json.read(old.getResultJson());
      if (!"IRIS_COMMUNICATION_V1".equals(result.path("contractVersion").asText())) continue;
      results.add(
          Map.of(
              "taskId",
              old.getId(),
              "sourceReference",
              old.getSourceReference(),
              "result",
              result,
              "authorityInherited",
              false,
              "reuse",
              "FACTS_AND_SPECIFICATION_ONLY"));
    }
    return List.copyOf(results);
  }
}
