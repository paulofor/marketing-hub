package com.marketinghub.experimentstrategist.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistMemoryArtifactRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistMemoryRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Responsabilidade: registrar memoria estruturada e artefatos anonimizados do Estrategista. */
@Service
public class ExperimentStrategistMemoryService {
  private static final Set<String> MECHANISMS =
      Set.of(
          "FRICTION",
          "UNCERTAINTY",
          "PERCEIVED_RISK",
          "PROOF",
          "CHOICE_OVERLOAD",
          "DELAYED_REWARD",
          "REGRET_AVERSION",
          "TRUST",
          "PROMISE_FIT");
  private static final Set<String> EVIDENCE_LEVELS =
      Set.of(
          "OBSERVATION",
          "MULTIPLE_OBSERVATIONS",
          "HUMAN_RESULT",
          "COMMERCIAL_RESULT",
          "SCIENTIFIC_SOURCE");
  private static final Set<String> STATUSES =
      Set.of("HYPOTHESIS", "CONFIRMED", "CONTRADICTED", "INCONCLUSIVE");
  private static final Set<String> CONFIDENCES = Set.of("LOW", "MEDIUM", "HIGH");
  private final ExperimentStrategistMemoryRepository memoryRepository;
  private final ExperimentStrategistMemoryArtifactRepository artifactRepository;
  private final ExperimentStrategistMemoryProperties properties;
  private final ExperimentStrategistAnonymizer anonymizer;
  private final ObjectMapper objectMapper;
  private final S3Client s3;

  /** Configura persistencia canônica, anonimização e armazenamento privado. */
  public ExperimentStrategistMemoryService(
      ExperimentStrategistMemoryRepository memoryRepository,
      ExperimentStrategistMemoryArtifactRepository artifactRepository,
      ExperimentStrategistMemoryProperties properties,
      ExperimentStrategistAnonymizer anonymizer,
      ObjectMapper objectMapper,
      @Qualifier("experimentStrategistMemoryS3Client") S3Client s3) {
    this.memoryRepository = memoryRepository;
    this.artifactRepository = artifactRepository;
    this.properties = properties;
    this.anonymizer = anonymizer;
    this.objectMapper = objectMapper;
    this.s3 = s3;
  }

  /** Registra uma hipotese ou aprendizado sem confundir inferencia com resultado. */
  @Transactional
  public MemoryResponse create(CreateMemoryRequest request) {
    if (request == null
        || request.commercialPlanId() == null
        || request.validityDays() != null
            && (request.validityDays() < 1 || request.validityDays() > 730)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Planejamento ou validade da memoria invalido.");
    }
    String mechanism = normalized(request.behavioralMechanism());
    String evidence = normalized(request.evidenceLevel());
    String status = normalized(request.validationStatus());
    String confidence = normalized(request.confidence());
    if (!MECHANISMS.contains(mechanism)
        || !EVIDENCE_LEVELS.contains(evidence)
        || !STATUSES.contains(status)
        || !CONFIDENCES.contains(confidence)
        || request.statement() == null
        || request.statement().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contrato de memoria comportamental invalido.");
    }
    if (status.equals("CONFIRMED")
        && !Set.of("HUMAN_RESULT", "COMMERCIAL_RESULT").contains(evidence)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Confirmacao exige resultado humano ou comercial.");
    }
    ExperimentStrategistMemory memory = new ExperimentStrategistMemory();
    memory.setCommercialPlanId(request.commercialPlanId());
    memory.setExecutionId(request.executionId());
    memory.setBehavioralMechanism(mechanism);
    memory.setStatement(anonymizer.anonymize(request.statement().trim()));
    memory.setEvidenceLevel(evidence);
    memory.setConfidence(confidence);
    memory.setValidationStatus(status);
    memory.setSourceReferencesJson(
        json(request.sourceReferences() == null ? List.of() : request.sourceReferences()));
    memory.setObservedOutcome(anonymizer.anonymize(request.observedOutcome()));
    memory.setValidUntil(
        Instant.now()
            .plus(request.validityDays() == null ? 180 : request.validityDays(), ChronoUnit.DAYS));
    return response(memoryRepository.save(memory));
  }

  /** Lista apenas memorias vigentes para compor uma nova pesquisa. */
  @Transactional(readOnly = true)
  public List<MemoryResponse> activeForPlan(Long planId) {
    return memoryRepository
        .findByCommercialPlanIdAndValidUntilAfterOrderByCreatedAtDesc(planId, Instant.now())
        .stream()
        .map(this::response)
        .toList();
  }

  /** Anonimiza um artefato textual e armazena uma unica copia criptografada no S3. */
  @Transactional
  public ArtifactResponse storeArtifact(Long memoryId, ArtifactRequest request) {
    validateStorage();
    if (request == null
        || request.artifactType() == null
        || request.artifactType().isBlank()
        || request.content() == null
        || request.content().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artefato textual vazio.");
    }
    ExperimentStrategistMemory memory =
        memoryRepository
            .findById(memoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String sanitized = anonymizer.anonymize(request.content());
    byte[] bytes = sanitized.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > properties.getMaxBytes()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artefato acima do limite.");
    }
    String digest = sha256(bytes);
    var duplicate = artifactRepository.findByMemoryIdAndSha256(memoryId, digest);
    if (duplicate.isPresent()) return artifactResponse(duplicate.get());
    String key =
        properties.getPrefix()
            + "/plan-"
            + memory.getCommercialPlanId()
            + "/memory-"
            + memoryId
            + "/"
            + digest
            + ".txt";
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(properties.getBucket())
              .key(key)
              .contentType("text/plain; charset=utf-8")
              .serverSideEncryption(ServerSideEncryption.AES256)
              .metadata(java.util.Map.of("sha256", digest, "anonymization-version", "v1"))
              .build(),
          RequestBody.fromBytes(bytes));
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Falha ao armazenar artefato anonimizado do Estrategista no S3", ex);
    }
    ExperimentStrategistMemoryArtifact artifact = new ExperimentStrategistMemoryArtifact();
    artifact.setMemory(memory);
    artifact.setArtifactType(normalized(request.artifactType()));
    artifact.setSourceUrl(request.sourceUrl());
    artifact.setObjectKey(key);
    artifact.setContentType("text/plain; charset=utf-8");
    artifact.setSizeBytes((long) bytes.length);
    artifact.setSha256(digest);
    artifact.setAnonymizationVersion("v1");
    artifact.setRetentionUntil(Instant.now().plus(properties.getRetentionDays(), ChronoUnit.DAYS));
    return artifactResponse(artifactRepository.save(artifact));
  }

  /** Valida se o bucket foi configurado antes de consumir o S3. */
  private void validateStorage() {
    if (properties.getBucket() == null || properties.getBucket().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Bucket da memoria do Estrategista nao configurado.");
    }
  }

  /** Normaliza valores de contrato enumerados. */
  private String normalized(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  /** Serializa referencias preservando estrutura auditavel. */
  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Referencias de fonte invalidas", ex);
    }
  }

  /** Calcula hash de integridade e deduplicacao. */
  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponivel", ex);
    }
  }

  /** Converte a entidade em contrato seguro. */
  private MemoryResponse response(ExperimentStrategistMemory value) {
    return new MemoryResponse(
        value.getId(),
        value.getCommercialPlanId(),
        value.getExecutionId(),
        value.getBehavioralMechanism(),
        value.getStatement(),
        value.getEvidenceLevel(),
        value.getConfidence(),
        value.getValidationStatus(),
        value.getSourceReferencesJson(),
        value.getObservedOutcome(),
        value.getValidUntil(),
        value.getCreatedAt());
  }

  /** Converte metadados do artefato sem revelar bucket ou chave. */
  private ArtifactResponse artifactResponse(ExperimentStrategistMemoryArtifact value) {
    return new ArtifactResponse(
        value.getId(),
        value.getMemory().getId(),
        value.getArtifactType(),
        value.getSourceUrl(),
        value.getContentType(),
        value.getSizeBytes(),
        value.getSha256(),
        value.getAnonymizationVersion(),
        value.getRetentionUntil(),
        value.getCreatedAt());
  }

  public record SourceReference(String url, String title, Instant accessedAt, String learning) {}

  public record CreateMemoryRequest(
      Long commercialPlanId,
      Long executionId,
      String behavioralMechanism,
      String statement,
      String evidenceLevel,
      String confidence,
      String validationStatus,
      List<SourceReference> sourceReferences,
      String observedOutcome,
      Integer validityDays) {}

  public record ArtifactRequest(String artifactType, String sourceUrl, String content) {}

  public record MemoryResponse(
      Long id,
      Long commercialPlanId,
      Long executionId,
      String behavioralMechanism,
      String statement,
      String evidenceLevel,
      String confidence,
      String validationStatus,
      String sourceReferencesJson,
      String observedOutcome,
      Instant validUntil,
      Instant createdAt) {}

  public record ArtifactResponse(
      Long id,
      Long memoryId,
      String artifactType,
      String sourceUrl,
      String contentType,
      Long sizeBytes,
      String sha256,
      String anonymizationVersion,
      Instant retentionUntil,
      Instant createdAt) {}
}
