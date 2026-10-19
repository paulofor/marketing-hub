package com.marketinghub.agenttask;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    if ("nonAudiovisual".equals(task.getProcessActivityId())) return approvedSources(task).inputs();
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
            .flatMap(input -> readableInputs(input).stream())
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
    var approved = approvedSources(task);
    List<VisualInput> output = new ArrayList<>();
    Set<Long> ids = new java.util.HashSet<>();
    for (JsonNode render : renders) {
      var source = renderSource(task, render, approved);
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
              AgentTaskVisualEvidenceService.response(stored),
              "meta-ad-approver".equals(task.getAssignedAgent().getAgentKey())
                  ? creativeProof(render, source, approved)
                  : null));
    }
    return List.copyOf(output);
  }

  /**
   * Extrai arquivos explicitamente aprovados pelo gate, sem aceitar tarefas históricas por
   * aproximação.
   */
  private ApprovedSources approvedSources(AgentTask task) {
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
    VisualScope scope = visualScope(task, input, version);
    List<VisualInput> output = new ArrayList<>();
    for (JsonNode artifact : scope.artifacts()) {
      JsonNode proof = artifact.path("result");
      if (!"APPROVED".equals(proof.path("decision").asText())
          || !version.equals(proof.path("prototypeVersion").asText())
          || !scope.proofSourceReference().equals(proof.path("sourceReference").asText())) continue;
      if (scope.reauthorized()
          && (!"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(proof.path("contractVersion").asText())
              || scope.productId() != proof.path("productId").asLong()
              || !scope.publicUrl().equals(proof.path("publicUrl").asText()))) continue;
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
    var captures = supplementalCaptures(task, scope);
    var latest =
        captures.stream()
            .max(java.util.Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElse(null);
    if (latest != null && "COMPLETED".equals(latest.status()))
      output.addAll(supplementalInputs(task, version, scope, latest));
    require(
        !output.isEmpty(),
        "Não há captura aprovada e persistida da versão atual para produzir a imagem.");
    return new ApprovedSources(
        version, scope, List.copyOf(output), captures, input.path("visualProofAuthorization"));
  }

  /**
   * Localiza as capturas funcionais sem modificar o gate ou os pareceres aceitos. A referência
   * histórica video-proof também identifica provas usadas pela comunicação estática.
   */
  private List<AgentTaskFunctionalSnapshot> supplementalCaptures(
      AgentTask task, VisualScope scope) {
    if (!scope.reauthorized() || !task.getSourceReference().matches("experiment:[1-9][0-9]*"))
      return List.of();
    return tasks
        .findFunctionalSnapshots(
            task.getSourceReference() + "@video-proof-v1",
            Set.of("pde-construction-approval"),
            null)
        .stream()
        .filter(t -> "technicalHomologation".equals(t.processActivityId()))
        .filter(t -> "customer-agent".equals(t.agentKey()))
        .toList();
  }

  /**
   * Valida uma captura exata e seus arquivos, sem transformar captura histórica em autorização
   * atual.
   */
  private List<VisualInput> supplementalInputs(
      AgentTask task, String version, VisualScope scope, AgentTaskFunctionalSnapshot capture) {
    String reference = task.getSourceReference() + "@video-proof-v1";
    require("COMPLETED".equals(capture.status()), "A captura complementar não está concluída.");
    JsonNode proof = result(capture.resultJson());
    require(
        reference.equals(proof.path("sourceReference").asText())
            && scope.proofSourceReference().equals(proof.path("proofSourceReference").asText())
            && "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(proof.path("contractVersion").asText())
            && "PDE_KIT_CAPTURE_MOMENTS_V1".equals(proof.path("captureMomentsContract").asText())
            && "APPROVED".equals(proof.path("decision").asText())
            && scope.productId() == proof.path("productId").asLong()
            && version.equals(proof.path("prototypeVersion").asText())
            && scope.publicUrl().equals(proof.path("publicUrl").asText())
            && "AGENT_VALIDATION".equals(proof.path("trafficClass").asText())
            && "mh_internal_test".equals(proof.path("internalMarker").asText())
            && proof.path("providerCalls").asInt(-1) == 0
            && !proof.path("humanEvidenceClaimed").asBoolean(true)
            && !proof.path("commercialEvidenceClaimed").asBoolean(true)
            && !proof.path("sideEffects").path("paymentEnabled").asBoolean(true)
            && !proof.path("sideEffects").path("published").asBoolean(true)
            && !proof.path("sideEffects").path("campaignCreated").asBoolean(true)
            && proof.path("sideEffects").path("mediaSpendBrl").asDouble(-1) == 0,
        "A captura complementar não corresponde à versão privada autorizada e aos seus limites.");
    List<VisualInput> output = new ArrayList<>();
    for (JsonNode artifact : proof.path("artifacts")) {
      long id = artifact.path("artifactId").asLong();
      var stored = evidence.findByIdAndTaskId(id, capture.id()).orElse(null);
      require(
          stored != null
              && Set.of("FULL_PAGE", "FOLD").contains(stored.getEvidenceType())
              && stored.getSha256().equals(artifact.path("sha256").asText()),
          "A captura complementar não está persistida com o hash aprovado.");
      output.add(
          new VisualInput(capture.id(), version, AgentTaskVisualEvidenceService.response(stored)));
    }
    require(!output.isEmpty(), "A captura complementar não contém imagens funcionais.");
    return List.copyOf(output);
  }

  /** Mantém a fonte imutável da peça quando uma captura aprovada renova o mesmo destino. */
  private VisualInput renderSource(AgentTask task, JsonNode render, ApprovedSources approved) {
    var current =
        approved.inputs().stream().filter(input -> matchesSource(render, input)).findFirst();
    if (current.isPresent()) return current.get();
    require(
        approved.scope().reauthorized() && !approved.captures().isEmpty(),
        "A imagem usa prova de outra versão ou uma prova substituída.");
    var latest =
        approved.captures().stream()
            .max(java.util.Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElse(null);
    require(
        latest != null
            && "COMPLETED".equals(latest.status())
            && approved.inputs().stream()
                .anyMatch(input -> input.sourceTaskId().equals(latest.id())),
        "A captura atual não autoriza a continuidade da origem histórica.");
    var historical =
        approved.captures().stream()
            .filter(capture -> capture.id() == render.path("sourceTaskId").asLong())
            .findFirst()
            .orElseThrow(
                () -> blocked("A imagem usa prova de outra versão ou uma prova substituída."));
    return supplementalInputs(task, approved.version(), approved.scope(), historical).stream()
        .filter(input -> matchesSource(render, input))
        .findFirst()
        .orElseThrow(() -> blocked("A imagem usa prova de outra versão ou uma prova substituída."));
  }

  /** Compara a identidade completa dos pixels, incluindo versão e tarefa de captura. */
  private boolean matchesSource(JsonNode render, VisualInput input) {
    return input.sourceTaskId() == render.path("sourceTaskId").asLong()
        && input.evidence().id() == render.path("sourceArtifactId").asLong()
        && input.evidence().sha256().equals(render.path("sourceSha256").asText())
        && input.prototypeVersion().equals(render.path("prototypeVersion").asText());
  }

  /** Entrega ao revisor a fonte autorizada e o relatório mais recente, com horários explícitos. */
  private CreativeProof creativeProof(
      JsonNode render, VisualInput source, ApprovedSources approved) {
    var latest =
        approved.captures().stream()
            .max(java.util.Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElse(null);
    DestinationEvidence destination = null;
    if (latest != null && "COMPLETED".equals(latest.status())) {
      var captures =
          approved.inputs().stream()
              .filter(input -> input.sourceTaskId().equals(latest.id()))
              .filter(
                  input ->
                      input.evidence().evidenceKey() != null
                          && input.evidence().evidenceKey().endsWith("-CONTENT"))
              .toList();
      destination = new DestinationEvidence(latest.id(), result(latest.resultJson()), captures);
    }
    return new CreativeProof(
        source,
        render.path("crop"),
        render.path("templateVersion").asText(),
        approved.scope().proofSourceReference(),
        approved.scope().reauthorized() ? approved.scope().productId() : null,
        approved.scope().publicUrl(),
        approved.authorization(),
        destination);
  }

  /** Autoriza bytes somente da peça, da fonte exata e das capturas declaradas ao mesmo revisor. */
  private List<VisualInput> readableInputs(VisualInput input) {
    var result = new ArrayList<VisualInput>();
    result.add(input);
    if (input.creativeProof() != null) {
      result.add(input.creativeProof().source());
      if (input.creativeProof().currentDestination() != null)
        result.addAll(input.creativeProof().currentDestination().captures());
    }
    return result;
  }

  /**
   * Confere a autorização explícita para reutilizar no experimento a prova vigente do mesmo
   * produto.
   */
  private VisualScope visualScope(AgentTask task, JsonNode input, String version) {
    JsonNode authorization = input.path("visualProofAuthorization");
    if (authorization.isMissingNode() || authorization.isNull()) {
      return new VisualScope(
          task.getSourceReference(), input.path("approvedUpstreamArtifacts"), 0, "", false);
    }
    require(
        "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1"
            .equals(authorization.path("contractVersion").asText()),
        "A autorização da prova visual não pertence ao contrato vigente.");
    require(
        task.getSourceReference().equals(authorization.path("targetSourceReference").asText()),
        "A autorização visual pertence a outro experimento.");
    require(
        version.equals(authorization.path("prototypeVersion").asText()),
        "A autorização visual pertence a outra versão.");
    long productId = authorization.path("productId").asLong();
    require(
        productId > 0 && productId == input.path("product").path("id").asLong(),
        "A autorização visual pertence a outro produto.");
    String proofSourceReference = authorization.path("proofSourceReference").asText();
    String publicUrl = input.path("approvedDestination").path("url").asText();
    require(
        !proofSourceReference.isBlank()
            && !publicUrl.isBlank()
            && publicUrl.equals(authorization.path("publicUrl").asText())
            && authorization.path("gateInstanceId").asLong() > 0,
        "A autorização visual não identifica gate, origem e destino aprovados.");
    JsonNode artifacts = input.path("approvedVisualArtifacts");
    require(
        artifacts.isArray() && !artifacts.isEmpty(),
        "A autorização visual não contém capturas aprovadas.");
    return new VisualScope(proofSourceReference, artifacts, productId, publicUrl, true);
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record VisualInput(
      Long sourceTaskId,
      String prototypeVersion,
      AgentTaskVisualEvidenceResponse evidence,
      CreativeProof creativeProof) {
    /** Conserva o contrato anterior para produtores e revisores que recebem somente a peça. */
    public VisualInput(
        Long sourceTaskId, String prototypeVersion, AgentTaskVisualEvidenceResponse evidence) {
      this(sourceTaskId, prototypeVersion, evidence, null);
    }
  }

  /** Responsabilidade: relacionar a derivação exata e as provas do destino, sem aprovar seu uso. */
  public record CreativeProof(
      VisualInput source,
      JsonNode crop,
      String templateVersion,
      String proofSourceReference,
      Long productId,
      String publicUrl,
      JsonNode visualProofAuthorization,
      DestinationEvidence currentDestination) {}

  /**
   * Responsabilidade: transportar o relatório determinístico e suas capturas funcionais datadas.
   */
  public record DestinationEvidence(
      Long taskId, JsonNode technicalProof, List<VisualInput> captures) {}

  /** Responsabilidade: conservar o contexto validado durante a projeção das origens e derivados. */
  private record ApprovedSources(
      String version,
      VisualScope scope,
      List<VisualInput> inputs,
      List<AgentTaskFunctionalSnapshot> captures,
      JsonNode authorization) {}

  /** Representa a origem autorizada dos pixels sem alterar o parecer histórico que os produziu. */
  private record VisualScope(
      String proofSourceReference,
      JsonNode artifacts,
      long productId,
      String publicUrl,
      boolean reauthorized) {}
}
