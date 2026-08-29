package com.marketinghub.agenttask;

import com.marketinghub.customeragent.memory.CustomerAgentMemoryProperties;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Responsabilidade: guardar e entregar provas visuais privadas vinculadas a tarefas de agentes. */
@Service
public class AgentTaskVisualEvidenceService {
  private static final Logger log = LoggerFactory.getLogger(AgentTaskVisualEvidenceService.class);
  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._-]+");
  private static final Set<String> EVIDENCE_TYPES = Set.of("FULL_PAGE", "FOLD");
  private static final Set<String> DEVICE_PROFILES =
      Set.of("IPHONE_15_PRO", "PIXEL_7", "DESKTOP_1440");
  private static final Set<String> SENSITIVE_QUERY_PARAMETERS =
      Set.of(
          "accesstoken",
          "apikey",
          "authorization",
          "credential",
          "idtoken",
          "jwt",
          "password",
          "refreshtoken",
          "secret",
          "session",
          "signature",
          "token");
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

  private final CustomerAgentMemoryProperties properties;
  private final S3Client s3;
  private final AgentTaskRepository taskRepository;
  private final AgentTaskVisualEvidenceRepository evidenceRepository;
  private final AgentTaskTargetContextProvider targetContextProvider;
  private final Clock clock;

  /** Inicializa o armazenamento privado com as fontes canônicas de tarefa e configuração. */
  @Autowired
  public AgentTaskVisualEvidenceService(
      CustomerAgentMemoryProperties properties,
      @Qualifier("customerAgentMemoryS3Client") S3Client s3,
      AgentTaskRepository taskRepository,
      AgentTaskVisualEvidenceRepository evidenceRepository,
      AgentTaskTargetContextProvider targetContextProvider) {
    this(
        properties,
        s3,
        taskRepository,
        evidenceRepository,
        targetContextProvider,
        Clock.systemUTC());
  }

  /** Permite testes determinísticos de horário sem alterar o contrato de produção. */
  AgentTaskVisualEvidenceService(
      CustomerAgentMemoryProperties properties,
      S3Client s3,
      AgentTaskRepository taskRepository,
      AgentTaskVisualEvidenceRepository evidenceRepository,
      AgentTaskTargetContextProvider targetContextProvider,
      Clock clock) {
    this.properties = properties;
    this.s3 = s3;
    this.taskRepository = taskRepository;
    this.evidenceRepository = evidenceRepository;
    this.targetContextProvider = targetContextProvider;
    this.clock = clock;
  }

  /** Persiste um PNG validado antes que o worker possa iniciar o parecer de Psique. */
  @Transactional
  public AgentTaskVisualEvidenceResponse store(
      String agentKey, Long taskId, AgentTaskVisualEvidenceRequest request, MultipartFile file)
      throws IOException {
    validateConfiguration();
    AgentTask task = claimedTask(agentKey, taskId);
    ValidatedMetadata metadata = validate(task, request, file);
    String sha256 = sha256(metadata.bytes());
    var duplicate =
        evidenceRepository.findByTaskIdAndCaptureSessionIdAndEvidenceKey(
            taskId, metadata.captureSessionId(), metadata.evidenceKey());
    if (duplicate.isPresent()) {
      if (!MessageDigest.isEqual(
              duplicate.get().getSha256().getBytes(StandardCharsets.UTF_8),
              sha256.getBytes(StandardCharsets.UTF_8))
          || !sameMetadata(duplicate.get(), metadata)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "A mesma chave visual recebeu conteúdo ou metadados diferentes.");
      }
      return response(duplicate.get());
    }

    String objectKey = objectKey(taskId, metadata, sha256);
    putObject(taskId, metadata, sha256, objectKey);
    AgentTaskVisualEvidence evidence = entity(task, metadata, sha256, objectKey);
    try {
      return response(evidenceRepository.saveAndFlush(evidence));
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao persistir metadados da prova visual. taskId={} captureSessionId={} evidenceKey={}",
          taskId,
          metadata.captureSessionId(),
          metadata.evidenceKey(),
          ex);
      deleteOrphan(objectKey, taskId);
      throw ex;
    }
  }

  /** Recupera os pixels somente quando a evidência pertence à tarefa informada. */
  @Transactional(readOnly = true)
  public EvidenceContent read(Long taskId, Long evidenceId) {
    validateConfiguration();
    AgentTaskVisualEvidence evidence =
        evidenceRepository
            .findByIdAndTaskId(evidenceId, taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    try {
      ResponseBytes<GetObjectResponse> object =
          s3.getObjectAsBytes(
              GetObjectRequest.builder()
                  .bucket(properties.getBucket())
                  .key(evidence.getObjectKey())
                  .build());
      return new EvidenceContent(evidence.getContentType(), object.asByteArray());
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao ler prova visual privada. taskId={} evidenceId={} objectKey={}",
          taskId,
          evidenceId,
          evidence.getObjectKey(),
          ex);
      throw new IllegalStateException("Falha ao recuperar prova visual privada.", ex);
    }
  }

  /** Confirma identidade do agente e lease ativa antes de aceitar qualquer arquivo. */
  private AgentTask claimedTask(String agentKey, Long taskId) {
    if (agentKey == null || agentKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agente visual não informado.");
    }
    AgentTask task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!task.getAssignedAgent().getAgentKey().equals(agentKey.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tarefa pertence a outro agente.");
    }
    if (!"IN_PROGRESS".equals(task.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tarefa não está reservada.");
    }
    return task;
  }

  /** Valida contrato, dimensões, URL, limite e assinatura real do PNG. */
  private ValidatedMetadata validate(
      AgentTask task, AgentTaskVisualEvidenceRequest request, MultipartFile file)
      throws IOException {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadados visuais ausentes.");
    }
    String captureSessionId = identifier(request.captureSessionId(), 64, "sessão de captura");
    String evidenceKey = identifier(request.evidenceKey(), 160, "chave da evidência");
    String evidenceType = upper(request.evidenceType());
    String deviceProfile = upper(request.deviceProfile());
    if (!EVIDENCE_TYPES.contains(evidenceType) || !DEVICE_PROFILES.contains(deviceProfile)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Tipo de evidência ou dispositivo inválido.");
    }
    positive(request.pageNumber(), "página");
    positive(request.viewportWidth(), "largura do viewport");
    positive(request.viewportHeight(), "altura do viewport");
    positive(request.pageHeightPx(), "altura da página");
    if (request.viewportWidth() > 5000
        || request.viewportHeight() > 5000
        || request.pageHeightPx() > 200000
        || request.pageHeightPx() < request.viewportHeight()
        || request.scrollY() == null
        || request.scrollY() < 0
        || request.scrollY() > request.pageHeightPx()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dimensões visuais inválidas.");
    }
    if (("FOLD".equals(evidenceType) && (request.foldNumber() == null || request.foldNumber() < 1))
        || ("FULL_PAGE".equals(evidenceType)
            && (request.foldNumber() != null || request.scrollY() != 0))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Numeração incompatível com o tipo de evidência.");
    }
    String sourceUrl = publicUrl(request.sourceUrl(), "URL solicitada");
    String finalUrl = publicUrl(request.finalUrl(), "URL final");
    validateFrozenTarget(task, sourceUrl);
    if (request.capturedAt() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário de captura ausente.");
    }
    if (file == null || file.isEmpty() || file.getSize() > properties.getMaxBytes()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Snapshot vazio ou acima do limite.");
    }
    byte[] bytes = file.getBytes();
    if (bytes.length < PNG_SIGNATURE.length) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snapshot PNG inválido.");
    }
    for (int index = 0; index < PNG_SIGNATURE.length; index++) {
      if (bytes[index] != PNG_SIGNATURE[index]) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snapshot PNG inválido.");
      }
    }
    validateDecodablePng(request, bytes);
    return new ValidatedMetadata(
        captureSessionId,
        evidenceKey,
        evidenceType,
        deviceProfile,
        request.pageNumber(),
        request.foldNumber(),
        request.viewportWidth(),
        request.viewportHeight(),
        request.pageHeightPx(),
        request.scrollY(),
        sourceUrl,
        finalUrl,
        request.capturedAt().truncatedTo(ChronoUnit.SECONDS),
        bytes);
  }

  /**
   * Confirma que a URL fotografada pertence ao produto, experimento e versão congelados na tarefa.
   */
  private void validateFrozenTarget(AgentTask task, String sourceUrl) {
    String processCode =
        task.getProcessDefinition() == null ? null : task.getProcessDefinition().getProcessCode();
    String expectedUrl =
        targetContextProvider
            .resolve(task.getSourceReference(), processCode)
            .map(AgentTaskTargetResponse::publicUrl)
            .filter(value -> !value.isBlank())
            .map(value -> publicUrl(value, "URL congelada da tarefa"))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A tarefa não possui uma URL visual congelada e auditável."));
    if (!sameHttpTarget(expectedUrl, sourceUrl)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O snapshot não pertence ao produto e à versão congelados na tarefa.");
    }
  }

  /** Compara URLs HTTP canônicas tolerando apenas a barra raiz acrescentada pelo navegador. */
  private boolean sameHttpTarget(String expected, String actual) {
    URI left = URI.create(expected);
    URI right = URI.create(actual);
    return left.getScheme().equalsIgnoreCase(right.getScheme())
        && left.getHost().equalsIgnoreCase(right.getHost())
        && effectivePort(left) == effectivePort(right)
        && normalizedPath(left).equals(normalizedPath(right))
        && Objects.equals(left.getRawQuery(), right.getRawQuery());
  }

  /** Normaliza somente a ausência de caminho, sem mudar rota ou query do produto. */
  private String normalizedPath(URI uri) {
    return uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
  }

  /** Trata a porta padrão explícita como equivalente ao contrato HTTP usual. */
  private int effectivePort(URI uri) {
    if (uri.getPort() >= 0) return uri.getPort();
    return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }

  /**
   * Confirma que os pixels formam uma imagem PNG decodificável, não apenas um cabeçalho forjado.
   */
  private void validateDecodablePng(AgentTaskVisualEvidenceRequest request, byte[] bytes) {
    try {
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
        throw new IllegalArgumentException("PNG sem dimensões válidas.");
      }
    } catch (Exception ex) {
      log.warn(
          "Snapshot PNG não pôde ser decodificado. captureSessionId={} evidenceKey={}",
          request.captureSessionId(),
          request.evidenceKey(),
          ex);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snapshot PNG inválido.", ex);
    }
  }

  /** Exige que uma repetição idempotente preserve todos os metadados originalmente auditados. */
  private boolean sameMetadata(AgentTaskVisualEvidence existing, ValidatedMetadata requested) {
    return Objects.equals(existing.getEvidenceType(), requested.evidenceType())
        && Objects.equals(existing.getDeviceProfile(), requested.deviceProfile())
        && Objects.equals(existing.getPageNumber(), requested.pageNumber())
        && Objects.equals(existing.getFoldNumber(), requested.foldNumber())
        && Objects.equals(existing.getViewportWidth(), requested.viewportWidth())
        && Objects.equals(existing.getViewportHeight(), requested.viewportHeight())
        && Objects.equals(existing.getPageHeightPx(), requested.pageHeightPx())
        && Objects.equals(existing.getScrollY(), requested.scrollY())
        && Objects.equals(existing.getSourceUrl(), requested.sourceUrl())
        && Objects.equals(existing.getFinalUrl(), requested.finalUrl())
        && Objects.equals(existing.getCapturedAt(), requested.capturedAt())
        && Objects.equals(existing.getSizeBytes(), (long) requested.bytes().length);
  }

  /** Exige valor inteiro positivo para os metadados de viewport e página. */
  private void positive(Integer value, String label) {
    if (value == null || value < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " inválida.");
    }
  }

  /** Normaliza enums textuais sem aceitar ausência implícita. */
  private String upper(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  /** Exige um identificador limitado e seguro para compor chave privada do objeto. */
  private String identifier(String value, int maxLength, String label) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isEmpty()
        || normalized.length() > maxLength
        || !SAFE_IDENTIFIER.matcher(normalized).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " inválida.");
    }
    return normalized;
  }

  /** Aceita apenas URL HTTP(S) sem credencial ou parâmetro sensível. */
  private String publicUrl(String value, String label) {
    try {
      if (value == null || value.isBlank() || value.length() > 2048) {
        throw new IllegalArgumentException();
      }
      URI uri = URI.create(value.trim());
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || privateLiteralHost(uri.getHost())) {
        throw new IllegalArgumentException();
      }
      if (uri.getRawQuery() != null) {
        for (String part : uri.getRawQuery().split("&")) {
          String name = part.contains("=") ? part.substring(0, part.indexOf('=')) : part;
          String normalized =
              URLDecoder.decode(name, StandardCharsets.UTF_8)
                  .replaceAll("[^A-Za-z0-9]", "")
                  .toLowerCase(Locale.ROOT);
          if (SENSITIVE_QUERY_PARAMETERS.contains(normalized)) {
            throw new IllegalArgumentException();
          }
        }
      }
      return uri.toString();
    } catch (IllegalArgumentException ex) {
      log.warn("URL recusada na prova visual. campo={}", label, ex);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " inválida.", ex);
    }
  }

  /** Bloqueia endereços literais privados sem fazer resolução DNS dentro do backend. */
  private boolean privateLiteralHost(String host) {
    String normalized = host.toLowerCase(Locale.ROOT);
    if ("localhost".equals(normalized) || "0.0.0.0".equals(normalized)) return true;
    boolean ipLiteral = normalized.contains(":") || normalized.matches("[0-9.]+");
    if (!ipLiteral) return false;
    try {
      InetAddress address = InetAddress.getByName(normalized);
      byte[] bytes = address.getAddress();
      boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
      return address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()
          || uniqueLocalIpv6;
    } catch (Exception ex) {
      log.warn("Host literal inválido na prova visual. host={}", host, ex);
      return true;
    }
  }

  /** Confirma que o bucket privado obrigatório está configurado. */
  private void validateConfiguration() {
    if (properties.getBucket() == null || properties.getBucket().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Bucket privado de Psique não configurado.");
    }
  }

  /** Monta chave segregada por tarefa, sessão, evidência e conteúdo. */
  private String objectKey(Long taskId, ValidatedMetadata metadata, String sha256) {
    return properties.getPrefix()
        + "/task-visual-evidence/task-"
        + taskId
        + "/"
        + metadata.captureSessionId()
        + "/"
        + metadata.evidenceKey()
        + "-"
        + sha256
        + "-"
        + UUID.randomUUID()
        + ".png";
  }

  /** Envia o PNG ao bucket com criptografia e correlação mínima nos metadados. */
  private void putObject(Long taskId, ValidatedMetadata metadata, String sha256, String objectKey) {
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(properties.getBucket())
              .key(objectKey)
              .contentType("image/png")
              .serverSideEncryption(ServerSideEncryption.AES256)
              .metadata(
                  java.util.Map.of(
                      "sha256", sha256,
                      "agent-task-id", taskId.toString(),
                      "capture-session-id", metadata.captureSessionId(),
                      "evidence-key", metadata.evidenceKey()))
              .build(),
          RequestBody.fromBytes(metadata.bytes()));
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao armazenar prova visual privada. taskId={} captureSessionId={} evidenceKey={} objectKey={}",
          taskId,
          metadata.captureSessionId(),
          metadata.evidenceKey(),
          objectKey,
          ex);
      throw new IllegalStateException("Falha ao armazenar prova visual privada.", ex);
    }
  }

  /** Remove o objeto quando a persistência relacional falha após o upload. */
  private void deleteOrphan(String objectKey, Long taskId) {
    try {
      s3.deleteObject(
          DeleteObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build());
    } catch (RuntimeException cleanupEx) {
      log.error(
          "Falha ao remover prova visual órfã. taskId={} objectKey={}",
          taskId,
          objectKey,
          cleanupEx);
    }
  }

  /** Converte metadados validados na entidade canônica vinculada à tarefa. */
  private AgentTaskVisualEvidence entity(
      AgentTask task, ValidatedMetadata metadata, String sha256, String objectKey) {
    AgentTaskVisualEvidence evidence = new AgentTaskVisualEvidence();
    evidence.setTask(task);
    evidence.setCaptureSessionId(metadata.captureSessionId());
    evidence.setEvidenceKey(metadata.evidenceKey());
    evidence.setEvidenceType(metadata.evidenceType());
    evidence.setDeviceProfile(metadata.deviceProfile());
    evidence.setPageNumber(metadata.pageNumber());
    evidence.setFoldNumber(metadata.foldNumber());
    evidence.setViewportWidth(metadata.viewportWidth());
    evidence.setViewportHeight(metadata.viewportHeight());
    evidence.setPageHeightPx(metadata.pageHeightPx());
    evidence.setScrollY(metadata.scrollY());
    evidence.setSourceUrl(metadata.sourceUrl());
    evidence.setFinalUrl(metadata.finalUrl());
    evidence.setObjectKey(objectKey);
    evidence.setContentType("image/png");
    evidence.setSizeBytes((long) metadata.bytes().length);
    evidence.setSha256(sha256);
    evidence.setCapturedAt(metadata.capturedAt());
    evidence.setCreatedAt(Instant.now(clock));
    return evidence;
  }

  /** Calcula a identidade imutável dos pixels persistidos. */
  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível ao identificar prova visual. sizeBytes={}", bytes.length, ex);
      throw new IllegalStateException("SHA-256 indisponível.", ex);
    }
  }

  /** Converte a entidade sem expor chave privada ou bucket. */
  static AgentTaskVisualEvidenceResponse response(AgentTaskVisualEvidence value) {
    String label =
        "FULL_PAGE".equals(value.getEvidenceType())
            ? "Página " + value.getPageNumber() + " · visão completa"
            : "Página " + value.getPageNumber() + " · dobra " + value.getFoldNumber();
    return new AgentTaskVisualEvidenceResponse(
        value.getId(),
        value.getCaptureSessionId(),
        value.getEvidenceKey(),
        value.getEvidenceType(),
        label,
        value.getDeviceProfile(),
        value.getPageNumber(),
        value.getFoldNumber(),
        value.getViewportWidth(),
        value.getViewportHeight(),
        value.getPageHeightPx(),
        value.getScrollY(),
        value.getSourceUrl(),
        value.getFinalUrl(),
        "/api/agent-tasks/"
            + value.getTask().getId()
            + "/visual-evidence/"
            + value.getId()
            + "/content",
        value.getSizeBytes(),
        value.getSha256(),
        value.getCapturedAt());
  }

  /** Mantém os bytes privados junto de seu tipo para resposta HTTP governada. */
  public record EvidenceContent(String contentType, byte[] bytes) {}

  /** Agrupa somente metadados já normalizados e os pixels validados. */
  private record ValidatedMetadata(
      String captureSessionId,
      String evidenceKey,
      String evidenceType,
      String deviceProfile,
      Integer pageNumber,
      Integer foldNumber,
      Integer viewportWidth,
      Integer viewportHeight,
      Integer pageHeightPx,
      Integer scrollY,
      String sourceUrl,
      String finalUrl,
      Instant capturedAt,
      byte[] bytes) {}
}
