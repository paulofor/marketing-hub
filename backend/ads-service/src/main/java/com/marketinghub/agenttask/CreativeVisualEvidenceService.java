package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar a origem e a derivação dos pixels usados na produção e revisão
 * criativa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeVisualEvidenceService {
  private final AgentTaskRepository tasks;
  private final AgentTaskVisualEvidenceRepository evidence;
  private final AgentTaskVisualEvidenceService storage;
  private final CommunicationMaterializationContextProvider context;
  private final ObjectMapper json;

  /** Entrega somente entradas aprovadas do mesmo contexto à tarefa reservada do agente. */
  @Transactional(readOnly = true)
  public List<VisualInput> inputs(String agentKey, Long taskId) {
    AgentTask task = claimed(agentKey, taskId);
    if ("nonAudiovisual".equals(task.getProcessActivityId())) return approvedSources(task);
    var producer = latestProducer(task);
    require("COMPLETED".equals(producer.status()), "A produção criativa ainda não foi concluída.");
    return validatedRenders(producer.id(), task, result(producer.resultJson()));
  }

  /** Confere a entrada autorizada novamente antes de entregar seus bytes privados ao executor. */
  @Transactional(readOnly = true)
  public AgentTaskVisualEvidenceService.EvidenceContent read(
      String agentKey, Long taskId, Long sourceTaskId, Long evidenceId) {
    require(
        inputs(agentKey, taskId).stream()
            .anyMatch(
                i ->
                    Objects.equals(i.sourceTaskId(), sourceTaskId)
                        && Objects.equals(i.evidence().id(), evidenceId)),
        "A imagem não pertence às entradas aprovadas desta execução.");
    return storage.read(sourceTaskId, evidenceId);
  }

  /** Impede a conclusão da produção sem PNG persistido e derivação verificável. */
  @Transactional(readOnly = true)
  public void validateCompletion(AgentTask task, String resultJson) {
    validatedRenders(task.getId(), task, result(resultJson));
  }

  /**
   * Resolve o produtor mais recente dentro da mesma definição e referência, inclusive bloqueios.
   */
  private AgentTaskFunctionalSnapshot latestProducer(AgentTask task) {
    return tasks
        .findFunctionalSnapshotsByProcessSince(
            task.getProcessDefinition().getId(), task.getSourceReference(), null)
        .stream()
        .filter(t -> "nonAudiovisual".equals(t.processActivityId()))
        .max(java.util.Comparator.comparing(AgentTaskFunctionalSnapshot::id))
        .orElseThrow(() -> blocked("Produza a peça visual antes da revisão."));
  }

  /** Valida todos os derivados contra arquivos persistidos e provas vigentes da versão aprovada. */
  private List<VisualInput> validatedRenders(Long producerId, AgentTask task, JsonNode result) {
    require(
        task.getSourceReference().equals(result.path("sourceReference").asText()),
        "O pacote criativo pertence a outra referência.");
    var renders = result.path("functionalOutput").path("renderedAssets");
    var briefs = result.path("functionalOutput").path("staticAssets");
    require(
        renders.isArray() && !renders.isEmpty() && renders.size() == briefs.size(),
        "O briefing ainda não possui todas as imagens finais renderizadas.");
    var sources = approvedSources(task);
    List<VisualInput> output = new ArrayList<>();
    Set<Long> ids = new java.util.HashSet<>();
    for (JsonNode render : renders) {
      var source =
          sources.stream()
              .filter(
                  i ->
                      i.sourceTaskId() == render.path("sourceTaskId").asLong()
                          && i.evidence().id() == render.path("sourceArtifactId").asLong()
                          && i.evidence().sha256().equals(render.path("sourceSha256").asText())
                          && i.prototypeVersion().equals(render.path("prototypeVersion").asText()))
              .findFirst()
              .orElseThrow(
                  () -> blocked("A imagem usa prova de outra versão ou uma prova substituída."));
      long id = render.path("artifactId").asLong();
      require(ids.add(id), "O pacote repete a mesma imagem como peças distintas.");
      var stored =
          evidence
              .findByIdAndTaskId(id, producerId)
              .orElseThrow(() -> blocked("A imagem final não foi persistida nesta tarefa."));
      require(
          "CREATIVE_RENDER".equals(stored.getEvidenceType())
              && stored.getSha256().equals(render.path("sha256").asText())
              && "PROOF_CARD_V1".equals(render.path("templateVersion").asText())
              && render.path("crop").isObject(),
          "A identidade ou a derivação da imagem está incompleta.");
      output.add(
          new VisualInput(
              producerId,
              source.prototypeVersion(),
              AgentTaskVisualEvidenceService.response(stored)));
    }
    return List.copyOf(output);
  }

  /**
   * Extrai arquivos explicitamente aprovados pelo gate, sem aceitar tarefas históricas por
   * aproximação.
   */
  private List<VisualInput> approvedSources(AgentTask task) {
    JsonNode input =
        json.valueToTree(
            context
                .resolve(task.getSourceReference())
                .orElseThrow(
                    () -> blocked("O contexto de comunicação ainda não está disponível.")));
    require(
        "READY".equals(input.path("inputReadiness").asText()),
        "O contexto de comunicação perdeu a aprovação dos predecessores.");
    String version = input.path("prototypeVersion").asText();
    if (version.isBlank()) version = input.path("product").path("experienceVersion").asText();
    require(!version.isBlank(), "A versão da prova visual não está identificada.");
    List<VisualInput> output = new ArrayList<>();
    for (JsonNode artifact : input.path("approvedUpstreamArtifacts")) {
      JsonNode proof = artifact.path("result");
      if (!"APPROVED".equals(proof.path("decision").asText())
          || !version.equals(proof.path("prototypeVersion").asText())
          || !task.getSourceReference().equals(proof.path("sourceReference").asText())) continue;
      long sourceId = artifact.path("taskId").asLong();
      for (JsonNode image : proof.path("artifacts")) {
        long id = image.path("artifactId").asLong();
        var stored = evidence.findByIdAndTaskId(id, sourceId).orElse(null);
        if (stored == null || !Set.of("FULL_PAGE", "FOLD").contains(stored.getEvidenceType()))
          continue;
        require(
            stored.getSha256().equals(image.path("sha256").asText()),
            "O hash da prova aprovada diverge do arquivo persistido.");
        output.add(
            new VisualInput(sourceId, version, AgentTaskVisualEvidenceService.response(stored)));
      }
    }
    require(
        !output.isEmpty(),
        "Não há captura aprovada e persistida da versão atual para produzir a imagem.");
    return List.copyOf(output);
  }

  /**
   * Confirma contrato criativo, identidade do agente e reserva ativa sem permitir acesso cruzado.
   */
  private AgentTask claimed(String agentKey, Long taskId) {
    AgentTask task =
        tasks.findById(taskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    require(
        task.getAssignedAgent() != null
            && task.getAssignedAgent().getAgentKey().equals(agentKey)
            && "IN_PROGRESS".equals(task.getStatus()),
        "A tarefa não está reservada para este agente.");
    require(
        task.getProcessDefinition() != null
            && "creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
            && Set.of("nonAudiovisual", "customer", "commercial")
                .contains(task.getProcessActivityId()),
        "A tarefa não pertence ao contrato de imagens criativas.");
    return task;
  }

  /** Lê o contrato funcional sem ocultar erros de serialização. */
  private JsonNode result(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Contrato criativo inválido ao resolver provas visuais.", ex);
      throw blocked("O pacote criativo não contém JSON válido.");
    }
  }

  /** Mantém falhas de contrato explícitas e persistíveis pelo callback do executor. */
  private static void require(boolean condition, String message) {
    if (!condition) throw blocked(message);
  }

  /** Representa um bloqueio funcional sem convertê-lo em aprovação técnica. */
  private static ResponseStatusException blocked(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /**
   * Responsabilidade: transportar a identidade da origem e os metadados privados sem expor o
   * storage.
   */
  public record VisualInput(
      Long sourceTaskId, String prototypeVersion, AgentTaskVisualEvidenceResponse evidence) {}
}
