package com.marketinghub.customeragent.memory;

import com.marketinghub.customeragent.CustomerAgentMemoryEvidence;
import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentMemoryEvidenceRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerDigitalObservationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Responsabilidade: armazenar evidencias pesadas no S3 com metadados auditaveis no MySQL. */
@Service
public class CustomerAgentMemoryEvidenceService {
  private static final List<String> LAYERS =
      List.of(
          "EXTERNAL_OBSERVATION",
          "SIMULATED_INTERPRETATION",
          "COMMERCIAL_HYPOTHESIS",
          "HUMAN_RESULT",
          "CONFIRMED_LEARNING");
  private final CustomerAgentMemoryProperties properties;
  private final S3Client s3;
  private final CustomerAgentMemoryEvidenceRepository evidenceRepository;
  private final CustomerPersonaRepository personaRepository;
  private final CustomerDigitalObservationRepository observationRepository;

  /** Inicializa o armazenamento híbrido com dependencias canônicas e cliente S3 privado. */
  public CustomerAgentMemoryEvidenceService(
      CustomerAgentMemoryProperties properties,
      @Qualifier("customerAgentMemoryS3Client") S3Client s3,
      CustomerAgentMemoryEvidenceRepository evidenceRepository,
      CustomerPersonaRepository personaRepository,
      CustomerDigitalObservationRepository observationRepository) {
    this.properties = properties;
    this.s3 = s3;
    this.evidenceRepository = evidenceRepository;
    this.personaRepository = personaRepository;
    this.observationRepository = observationRepository;
  }

  /** Armazena o objeto uma unica vez e persiste sua procedencia canônica. */
  @Transactional
  public EvidenceResponse store(
      Long personaId, Long observationId, String memoryLayer, String sourceUrl, MultipartFile file)
      throws IOException {
    validateConfiguration();
    if (file == null || file.isEmpty() || file.getSize() > properties.getMaxBytes()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Evidencia vazia ou acima do limite.");
    }
    String layer = memoryLayer == null ? "" : memoryLayer.trim().toUpperCase(Locale.ROOT);
    if (!LAYERS.contains(layer)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Camada de memoria invalida.");
    }
    byte[] bytes = file.getBytes();
    String sha256 = sha256(bytes);
    var duplicate =
        evidenceRepository.findByPersonaIdAndMemoryLayerAndSha256(personaId, layer, sha256);
    if (duplicate.isPresent()) return response(duplicate.get());

    CustomerPersona persona =
        personaRepository
            .findById(personaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    CustomerDigitalObservation observation = null;
    if (observationId != null) {
      observation =
          observationRepository
              .findById(observationId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      if (!observation.getPersona().getId().equals(personaId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Observacao pertence a outra persona.");
      }
    }
    String key =
        properties.getPrefix()
            + "/persona-"
            + personaId
            + "/"
            + layer.toLowerCase(Locale.ROOT)
            + "/"
            + sha256;
    String contentType =
        file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(properties.getBucket())
              .key(key)
              .contentType(contentType)
              .serverSideEncryption(ServerSideEncryption.AES256)
              .metadata(
                  java.util.Map.of(
                      "sha256", sha256, "persona-id", personaId.toString(), "memory-layer", layer))
              .build(),
          RequestBody.fromBytes(bytes));
    } catch (RuntimeException ex) {
      throw new IllegalStateException("Falha ao armazenar evidencia do Agente Cliente no S3", ex);
    }
    CustomerAgentMemoryEvidence evidence = new CustomerAgentMemoryEvidence();
    evidence.setPersona(persona);
    evidence.setObservation(observation);
    evidence.setMemoryLayer(layer);
    evidence.setSourceUrl(sourceUrl);
    evidence.setObjectKey(key);
    evidence.setContentType(contentType);
    evidence.setSizeBytes((long) bytes.length);
    evidence.setSha256(sha256);
    evidence.setRetentionUntil(Instant.now().plus(properties.getRetentionDays(), ChronoUnit.DAYS));
    return response(evidenceRepository.save(evidence));
  }

  /** Lista metadados sem expor URL publica ou credencial do bucket. */
  @Transactional(readOnly = true)
  public List<EvidenceResponse> list(Long personaId) {
    return evidenceRepository.findByPersonaIdOrderByCreatedAtDesc(personaId).stream()
        .map(this::response)
        .toList();
  }

  /** Recupera bytes pelo backend para manter o bucket privado. */
  @Transactional(readOnly = true)
  public EvidenceContent read(Long id) {
    validateConfiguration();
    CustomerAgentMemoryEvidence evidence =
        evidenceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    ResponseBytes<GetObjectResponse> object =
        s3.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(evidence.getObjectKey())
                .build());
    return new EvidenceContent(evidence.getContentType(), object.asByteArray());
  }

  /** Valida configuração obrigatoria antes de qualquer acesso remoto. */
  private void validateConfiguration() {
    if (properties.getBucket() == null || properties.getBucket().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Bucket da memoria do Agente Cliente nao configurado.");
    }
  }

  /** Calcula identificador deterministico para deduplicacao e integridade. */
  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponivel", ex);
    }
  }

  /** Converte metadados persistidos em contrato publico seguro. */
  private EvidenceResponse response(CustomerAgentMemoryEvidence value) {
    return new EvidenceResponse(
        value.getId(),
        value.getPersona().getId(),
        value.getObservation() == null ? null : value.getObservation().getId(),
        value.getMemoryLayer(),
        value.getSourceUrl(),
        value.getContentType(),
        value.getSizeBytes(),
        value.getSha256(),
        value.getRetentionUntil(),
        value.getCreatedAt());
  }

  public record EvidenceResponse(
      Long id,
      Long personaId,
      Long observationId,
      String memoryLayer,
      String sourceUrl,
      String contentType,
      Long sizeBytes,
      String sha256,
      Instant retentionUntil,
      Instant createdAt) {}

  public record EvidenceContent(String contentType, byte[] bytes) {}
}
