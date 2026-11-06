package com.marketinghub.salesvideo.autonomy.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: converter a entrega aceita de Íris em entrada privada rastreável, sem aprovação
 * editorial.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoPrivateBriefPreparation {
  private final AgentTaskRepository tasks;
  private final BusinessProcessDefinitionRepository definitions;
  private final AgentTaskService taskService;
  private final SalesVideoScriptRepository scripts;
  private final SalesVideoProfileRepository profiles;
  private final ObjectMapper json;
  private final VideoProjectRepository projects;

  /** Reconhece a entrada editorial já aprovada no caminho anterior sem criar nova inferência. */
  public boolean hasApprovedScript(VideoProject project) {
    return scripts
        .findFirstByProfileIdAndStatusOrderByVersionDesc(
            project.getSalesVideoProfileId(), SalesVideoScriptStatus.APPROVED)
        .isPresent();
  }

  /**
   * Enfileira uma única preparação com Íris pelo processo existente, preservando BRIEF_ONLY antigo.
   */
  public void request(VideoProductionCycle cycle, VideoProject project) {
    if (cycle.getIrisAgentTaskId() != null) return;
    var process =
        definitions
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                "pde-communication-sales-journey", "PUBLISHED")
            .orElseThrow();
    String reference = "experiment:" + project.getExperimentId();
    var task =
        taskService.createByAgent(
            new CreateAgentTaskByAgentRequest(
                "videomaker",
                "communication-director",
                "Preparar fala e briefing do vídeo privado",
                "Pedido governado de vídeo #"
                    + cycle.getId()
                    + ", projeto #"
                    + project.getId()
                    + ", perfil #"
                    + project.getSalesVideoProfileId()
                    + ". Revalidar/completar somente a comunicação para este vídeo privado,"
                    + " preservando produto, versão, oferta e fontes. Entregar [FALA_FINAL] literal"
                    + " em audiovisualBrief, sem marcadores narrados, com cerca de "
                    + Math.round(project.getTargetDurationSeconds() * 110.0 / 60)
                    + " palavras para "
                    + project.getTargetDurationSeconds()
                    + " segundos e "
                    + cutCount(project)
                    + " cortes editoriais."
                    + " Não gerar imagem/vídeo, aprovar a própria peça, publicar ou ampliar teto."
                    + " Briefing persistido do projeto:\n"
                    + describe(project),
                "HIGH",
                reference,
                process.getId(),
                "communicationContract",
                false,
                null));
    cycle.setIrisAgentTaskId(task.id());
  }

  /** Informa conclusão aceita sem reutilizar tarefa anterior ao pedido ou de outro responsável. */
  public boolean completed(VideoProductionCycle cycle, VideoProject project) {
    if (cycle.getIrisAgentTaskId() == null) return false;
    var task = source(cycle, project);
    return "COMPLETED".equals(task.getStatus());
  }

  /** Expõe a fala aceita a Plutus sem confundi-la com vídeo produzido ou parecer premium. */
  public Map<String, Object> input(VideoProductionCycle cycle, VideoProject project) {
    var task = source(cycle, project);
    if (!"COMPLETED".equals(task.getStatus()))
      return Map.of("taskId", task.getId(), "status", task.getStatus());
    return Map.of(
        "taskId",
        task.getId(),
        "status",
        task.getStatus(),
        "finalSpeech",
        speech(task),
        "sourceReference",
        task.getSourceReference(),
        "qualityApproved",
        false);
  }

  /**
   * Materializa entrada técnica somente depois do parecer independente de Plutus, sem preencher
   * approvedBy.
   */
  public void materialize(VideoProductionCycle cycle, VideoProject project) {
    if (hasApprovedScript(project)) return;
    if (!"APPROVED".equals(cycle.getFinancialDecision()))
      throw new IllegalStateException("Plutus ainda não liberou a produção privada.");
    var task = source(cycle, project);
    if (!"COMPLETED".equals(task.getStatus()))
      throw new IllegalStateException("Íris ainda não concluiu o briefing.");
    var existing =
        scripts.findFirstByProfileIdAndStatusOrderByVersionDesc(
            project.getSalesVideoProfileId(), SalesVideoScriptStatus.PRIVATE_READY);
    if (existing.isPresent()) {
      try {
        var evidence = json.readTree(existing.get().getSourceEvidenceJson());
        if (evidence.path("videoProductionCycleId").asLong() != cycle.getId()
            || evidence.path("irisTaskId").asLong() != task.getId()
            || !evidence.path("productVersion").asText().equals(project.getCampaignKey()))
          throw new IllegalArgumentException(
              "Entrada privada pertence a outra execução ou versão.");
      } catch (Exception ex) {
        log.error(
            "Entrada privada anterior incompatível; cycleId={} taskId={}",
            cycle.getId(),
            task.getId(),
            ex);
        throw new IllegalStateException(
            "Preservar roteiro anterior; não reutilizar outra execução.", ex);
      }
      return;
    }
    var profile = profiles.findById(project.getSalesVideoProfileId()).orElseThrow();
    if (!Objects.equals(profile.getProduct().getId(), project.getProductId())
        || !Objects.equals(profile.getTenantId(), project.getTenantId()))
      throw new IllegalArgumentException("Perfil de outro contexto.");
    String speech = speech(task);
    var script = new SalesVideoScript();
    script.setProfile(profile);
    script.setVersion(
        scripts
            .findFirstByProfileIdOrderByVersionDesc(profile.getId())
            .map(s -> s.getVersion() + 1)
            .orElse(1));
    script.setCreatedBy("IRIS_TASK:" + task.getId());
    script.setScriptText(speech);
    script.setHookText(project.getHookText());
    script.setCtaText(project.getCtaText());
    script.setCaptionText(speech);
    script.setSource(SalesVideoScriptSource.IRIS_PRIVATE_BRIEF);
    script.setStatus(SalesVideoScriptStatus.PRIVATE_READY);
    try {
      script.setSourceEvidenceJson(
          json.writeValueAsString(
              Map.of(
                  "contractVersion",
                  "IRIS_PRIVATE_VIDEO_INPUT_V1",
                  "videoProductionCycleId",
                  cycle.getId(),
                  "irisTaskId",
                  task.getId(),
                  "videoProjectId",
                  project.getId(),
                  "productVersion",
                  project.getCampaignKey(),
                  "editorialApproval",
                  false,
                  "humanApproval",
                  false,
                  "previousScriptText",
                  Objects.toString(project.getScriptText(), ""),
                  "previousCaptionPlan",
                  Objects.toString(project.getCaptionPlan(), ""))));
    } catch (Exception ex) {
      log.error(
          "Falha ao preservar fonte do roteiro privado; cycleId={} taskId={}",
          cycle.getId(),
          task.getId(),
          ex);
      throw new IllegalStateException("Fonte do roteiro não preservada", ex);
    }
    String oldSpeech =
        Arrays.stream(Objects.toString(project.getCaptionPlan(), "").split("\\s*\\|\\s*"))
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .collect(java.util.stream.Collectors.joining(" "));
    project.setScriptText(speech);
    if (!oldSpeech.equals(speech)) project.setCaptionPlan(captions(speech, cutCount(project)));
    try {
      var frozen =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              json.readTree(script.getSourceEvidenceJson());
      frozen
          .put("acceptedScriptText", speech)
          .put("acceptedHookText", Objects.toString(project.getHookText(), ""))
          .put("acceptedCtaText", Objects.toString(project.getCtaText(), ""))
          .put("acceptedCaptionPlan", Objects.toString(project.getCaptionPlan(), ""));
      script.setSourceEvidenceJson(frozen.toString());
    } catch (java.io.IOException ex) {
      log.error(
          "Falha ao congelar entrada privada; cycleId={} taskId={}",
          cycle.getId(),
          task.getId(),
          ex);
      throw new IllegalStateException("Entrada privada não preservada", ex);
    }
    projects.saveAndFlush(project);
    scripts.saveAndFlush(script);
    profile.setStatus(SalesVideoStatus.SCRIPT_READY);
    profiles.save(profile);
  }

  /** Usa a mesma cadência editorial canônica, sem exceção por produto ou identificador. */
  private int cutCount(VideoProject project) {
    return Math.max(5, Math.min(48, (int) Math.ceil(project.getTargetDurationSeconds() / 4.0)));
  }

  /** Divide a fala literal em trechos, preservando todas as palavras na ordem aceita por Íris. */
  private String captions(String speech, int count) {
    String[] words = speech.split("\\s+");
    List<String> segments = new ArrayList<>();
    for (int i = 0; i < Math.min(count, words.length); i++) {
      int start = i * words.length / Math.min(count, words.length);
      int end = (i + 1) * words.length / Math.min(count, words.length);
      segments.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
    }
    return String.join(" | ", segments);
  }

  /**
   * Confere fonte BPM, produto e horário; nenhum título ou texto substitui identidade persistida.
   */
  private AgentTask source(VideoProductionCycle cycle, VideoProject project) {
    var task = tasks.findById(cycle.getIrisAgentTaskId()).orElseThrow();
    if (task.getAssignedAgent() == null
        || !"communication-director".equals(task.getAssignedAgent().getAgentKey())
        || !Objects.equals(task.getSourceReference(), "experiment:" + project.getExperimentId())
        || task.getCreatedAt() == null
        || task.getCreatedAt().isBefore(cycle.getCreatedAt())
        || task.getProcessDefinition() == null
        || !"pde-communication-sales-journey".equals(task.getProcessDefinition().getProcessCode())
        || !"communicationContract".equals(task.getProcessActivityId()))
      throw new IllegalArgumentException("Briefing de outra execução ou anterior ao pedido.");
    return task;
  }

  /** Extrai a fala literal versionada, sem copiar direção visual para a narração. */
  private String speech(AgentTask task) {
    try {
      String brief =
          json.readTree(task.getResultJson()).at("/functionalOutput/audiovisualBrief").asText();
      int start = brief.indexOf("[FALA_FINAL]"), end = brief.indexOf("[PROVA_E_LIMITES]");
      if (start < 0 || end <= start)
        throw new IllegalArgumentException("Íris não entregou fala final utilizável.");
      String value = brief.substring(start + "[FALA_FINAL]".length(), end).trim();
      if (value.isBlank()
          || value.contains("[DIRECAO_VISUAL]")
          || java.util.regex.Pattern.compile(
                  "(?imu)^(?:narração|locução|voz|texto na tela|instruções)[^\\n]*(?::|—)")
              .matcher(value)
              .find())
        throw new IllegalArgumentException("Fala final vazia ou misturada à direção visual.");
      return value;
    } catch (RuntimeException | java.io.IOException ex) {
      log.error("Fala de Íris inválida; taskId={}", task.getId(), ex);
      throw new IllegalArgumentException(
          "Briefing aceito sem fala utilizável; não iniciar render", ex);
    }
  }

  /**
   * Congela as entradas existentes para Íris sem criar produto, oferta, versão ou autorização nova.
   */
  private String describe(VideoProject project) {
    try {
      return json.writeValueAsString(project);
    } catch (Exception ex) {
      log.error("Falha ao descrever briefing; projectId={}", project.getId(), ex);
      throw new IllegalStateException("Briefing não disponível", ex);
    }
  }
}
