package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.salesvideo.service.VideoProductProofSource;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapta a prova técnica privada ao contrato de leitura do vídeo sem criar ou aprovar evidências.
 */
@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AgentTaskVideoProductProofSource implements VideoProductProofSource {
  private final AgentTaskVisualEvidenceRepository repository;
  private final AgentTaskVisualEvidenceService service;
  private final CommunicationMaterializationContextProvider context;
  private final ObjectMapper json;

  /** Conecta os repositórios e o storage pertencentes ao próprio módulo de agentes. */
  public AgentTaskVideoProductProofSource(
      AgentTaskVisualEvidenceRepository repository,
      AgentTaskVisualEvidenceService service,
      CommunicationMaterializationContextProvider context,
      ObjectMapper json) {
    this.repository = repository;
    this.service = service;
    this.context = context;
    this.json = json;
  }

  /** Traduz metadados da captura e da tarefa para a porta de prova técnica do vídeo. */
  @Override
  public Optional<Proof> find(Long taskId, Long evidenceId) {
    return repository
        .findByIdAndTaskId(evidenceId, taskId)
        .map(
            proof ->
                new Proof(
                    proof.getTask().getId(),
                    proof.getId(),
                    proof.getTask().getStatus(),
                    proof.getTask().getSourceReference(),
                    proof.getTask().getResultJson(),
                    proof.getSha256()));
  }

  /** Reutiliza o leitor privado existente sem permitir acesso direto do executor ao storage. */
  @Override
  public byte[] read(Long taskId, Long evidenceId) {
    return service.read(taskId, evidenceId).bytes();
  }

  /** Confere o contrato que permite reutilizar pixels homologados do produto no experimento. */
  @Override
  public boolean isAuthorizedFor(
      Proof proof, String targetSourceReference, Long productId, String prototypeVersion) {
    if (proof == null
        || targetSourceReference == null
        || productId == null
        || prototypeVersion == null) return false;
    return context
        .resolve(targetSourceReference)
        .<JsonNode>map(json::valueToTree)
        .filter(input -> "READY".equals(input.path("inputReadiness").asText()))
        .filter(
            input ->
                validAuthorization(
                    input, proof, targetSourceReference, productId, prototypeVersion))
        .isPresent();
  }

  /** Valida identidade, gate, destino e arquivo sem autorizar por aproximação histórica. */
  private boolean validAuthorization(
      JsonNode input,
      Proof proof,
      String targetSourceReference,
      Long productId,
      String prototypeVersion) {
    JsonNode authorization = input.path("visualProofAuthorization");
    String publicUrl = input.path("approvedDestination").path("url").asText();
    if (!"COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1"
            .equals(authorization.path("contractVersion").asText())
        || !targetSourceReference.equals(authorization.path("targetSourceReference").asText())
        || !proof.sourceReference().equals(authorization.path("proofSourceReference").asText())
        || !prototypeVersion.equals(authorization.path("prototypeVersion").asText())
        || productId.longValue() != authorization.path("productId").asLong()
        || productId.longValue() != input.path("product").path("id").asLong()
        || authorization.path("gateInstanceId").asLong() <= 0
        || publicUrl.isBlank()
        || !publicUrl.equals(authorization.path("publicUrl").asText())) return false;
    for (JsonNode artifact : input.path("approvedVisualArtifacts")) {
      JsonNode result = artifact.path("result");
      if (artifact.path("taskId").asLong() != proof.taskId()
          || !"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
          || !"APPROVED".equals(result.path("decision").asText())
          || !proof.sourceReference().equals(result.path("sourceReference").asText())
          || !prototypeVersion.equals(result.path("prototypeVersion").asText())
          || productId.longValue() != result.path("productId").asLong()
          || !publicUrl.equals(result.path("publicUrl").asText())) continue;
      for (JsonNode image : result.path("artifacts")) {
        if (image.path("artifactId").asLong() == proof.evidenceId()
            && proof.sha256().equals(image.path("sha256").asText())) return true;
      }
    }
    return false;
  }
}
