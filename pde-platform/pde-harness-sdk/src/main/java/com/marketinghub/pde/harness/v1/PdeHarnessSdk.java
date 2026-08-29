package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.internal.CodexVersionVerifier;
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import com.marketinghub.pde.harness.v1.internal.PdeMemoryContextRenderer;
import com.marketinghub.pde.harness.v1.internal.PdeMemoryPolicy;
import com.marketinghub.pde.harness.v1.internal.PdeOutputSchemaPolicy;
import com.marketinghub.pde.harness.v1.internal.PdeProtocolContract;
import com.marketinghub.pde.harness.v1.internal.PdeRenderedMemory;
import com.marketinghub.pde.harness.v1.internal.PdeStructuredOutputValidator;
import com.marketinghub.pde.harness.v1.internal.PdeTurnCollector;
import com.marketinghub.pde.harness.v1.internal.PdeTurnOutcome;
import com.marketinghub.pde.harness.v1.internal.transport.CodexAppServerClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Oferece a fachada Java do PDE Harness para executar um agente sem assumir a orquestração do
 * backend.
 */
public final class PdeHarnessSdk implements AutoCloseable {
  private static final System.Logger LOGGER = System.getLogger(PdeHarnessSdk.class.getName());
  private static final long MAX_IMAGE_BYTES = 15L * 1024L * 1024L;

  private final PdeHarnessConfiguration configuration;
  private final ObjectMapper mapper;
  private final PdeProtocolContract protocolContract;
  private final CodexVersionVerifier versionVerifier;
  private final PdeStructuredOutputValidator structuredOutputValidator;
  private final PdeMemoryContextRenderer memoryContextRenderer;
  private final CodexAppServerClient client;
  private final ConcurrentMap<String, Boolean> activeConversations = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> threadScopes = new ConcurrentHashMap<>();

  private volatile PdeHarnessHealth health;

  /** Cria o SDK e valida a integridade do contrato embarcado sem iniciar processo externo. */
  public PdeHarnessSdk(PdeHarnessConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration");
    this.mapper = new ObjectMapper();
    this.protocolContract = new PdeProtocolContract(mapper);
    this.versionVerifier = new CodexVersionVerifier();
    this.structuredOutputValidator = new PdeStructuredOutputValidator(mapper);
    this.memoryContextRenderer = new PdeMemoryContextRenderer();
    this.client = new CodexAppServerClient(configuration, mapper);
  }

  /** Verifica versão, inicia o App Server e devolve a prova ativa de prontidão. */
  public synchronized PdeHarnessHealth start() {
    if (health != null && client.isReady()) {
      return health;
    }
    protocolContract.verifyConfiguration(configuration);
    versionVerifier.verify(configuration);
    JsonNode initialized = client.start();
    Path actualCodexHome =
        Path.of(initialized.path("codexHome").asText()).toAbsolutePath().normalize();
    ensureSameCodexHome(actualCodexHome);
    health =
        new PdeHarnessHealth(
            true,
            protocolContract.codexVersion(),
            configuration.sdkVersion(),
            actualCodexHome,
            initialized.path("platformFamily").asText(),
            initialized.path("platformOs").asText(),
            initialized.path("userAgent").asText());
    return health;
  }

  /** Executa um turno completo e devolve saída, integridade e eventos ao worker chamador. */
  public PdeAgentRunResult execute(PdeAgentRunRequest request) {
    return execute(request, PdeExecutionObserver.noop());
  }

  /** Executa um turno completo emitindo telemetria incremental para o observer informado. */
  public PdeAgentRunResult execute(PdeAgentRunRequest request, PdeExecutionObserver observer) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(observer, "observer");
    PdeMemoryPolicy.validate(request);
    PdeOutputSchemaPolicy.validate(request.outputSchema());
    String conversationFingerprint = request.context().conversationScope().fingerprint();
    acquireConversation(conversationFingerprint);
    try {
      return executeExclusively(request, observer, conversationFingerprint);
    } finally {
      activeConversations.remove(conversationFingerprint);
    }
  }

  /** Exclui a thread local após o backend autorizar e remover sua memória e vínculo canônicos. */
  public void forgetThread(PdeConversationScope scope, PdeThreadBinding binding) {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(binding, "binding");
    if (!binding.belongsTo(scope)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Vínculo de thread não pertence à conversa autorizada para esquecimento");
    }
    String conversationFingerprint = scope.fingerprint();
    acquireConversation(conversationFingerprint);
    try {
      validateRuntimeThreadScope(binding.threadId(), conversationFingerprint);
      if (!binding.ephemeral()) {
        start();
        ObjectNode params = mapper.createObjectNode();
        params.put("threadId", binding.threadId());
        client.request("thread/delete", params);
      }
      threadScopes.remove(binding.threadId(), conversationFingerprint);
    } finally {
      activeConversations.remove(conversationFingerprint);
    }
  }

  /** Executa o turno sob exclusão mútua local para uma única conversa. */
  private PdeAgentRunResult executeExclusively(
      PdeAgentRunRequest request, PdeExecutionObserver observer, String conversationFingerprint) {
    Path workspace = validateWorkspace(configuration.workspaceFor(request.context()));
    RuntimeException executionFailure = null;
    try {
      return executeInWorkspace(request, observer, conversationFingerprint, workspace);
    } catch (RuntimeException ex) {
      executionFailure = ex;
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Execução PDE encerrada com falha; produto="
              + request.context().productCode()
              + ", missão="
              + request.context().missionReference()
              + ", interação="
              + request.context().interactionReference(),
          ex);
      throw ex;
    } finally {
      deleteWorkspace(workspace, executionFailure);
    }
  }

  /** Executa o protocolo dentro do workspace efêmero já validado para a interação. */
  private PdeAgentRunResult executeInWorkspace(
      PdeAgentRunRequest request,
      PdeExecutionObserver observer,
      String conversationFingerprint,
      Path workspace) {
    Instant startedAt = Instant.now();
    PdeRenderedMemory renderedMemory = memoryContextRenderer.render(request.memory(), startedAt);
    List<Path> localImagePaths = materializeLocalImages(request, workspace);
    if (request.existingThreadBinding() != null) {
      validateRuntimeThreadScope(
          request.existingThreadBinding().threadId(), conversationFingerprint);
    }
    start();

    JsonNode threadResponse = openThread(request, workspace);
    String threadId = requireIdentifier(threadResponse, "thread", "thread/start ou thread/resume");
    requireExpectedThread(request, threadId);
    validateRuntimeThreadScope(threadId, conversationFingerprint);
    PdeTurnCollector collector = new PdeTurnCollector(threadId, observer);
    collector.recordThreadReady(threadResponse);

    AutoCloseable listenerRegistration = client.addNotificationListener(collector::accept);
    try {
      ObjectNode turnParams = mapper.createObjectNode();
      turnParams.put("threadId", threadId);
      ArrayNode input = turnParams.putArray("input");
      ObjectNode memory = input.addObject();
      memory.put("type", "text");
      memory.put("text", renderedMemory.contextText());
      for (Path localImagePath : localImagePaths) {
        ObjectNode image = input.addObject();
        image.put("type", "localImage");
        image.put("path", localImagePath.toString());
      }
      ObjectNode text = input.addObject();
      text.put("type", "text");
      text.put("text", request.prompt());
      turnParams.set("outputSchema", request.outputSchema());

      JsonNode turnResponse = client.request("turn/start", turnParams);
      String turnId = requireIdentifier(turnResponse, "turn", "turn/start");
      collector.bindTurn(turnId);
      PdeTurnOutcome outcome;
      try {
        outcome = collector.await(configuration.turnTimeout());
      } catch (PdeHarnessException ex) {
        if (ex.category() == PdeHarnessFailureCategory.TIMEOUT) {
          interruptTurn(threadId, collector.turnId());
        }
        LOGGER.log(
            System.Logger.Level.ERROR,
            "Execução PDE falhou; produto="
                + request.context().productCode()
                + ", missão="
                + request.context().missionReference()
                + ", threadId="
                + threadId
                + ", turnId="
                + collector.turnId(),
            ex);
        throw ex;
      }

      PdeRunStatus status = mapStatus(outcome.status(), outcome.errorMessage());
      JsonNode structuredOutput =
          status == PdeRunStatus.COMPLETED
              ? structuredOutputValidator.validate(outcome.output(), request.outputSchema())
              : null;
      Instant finishedAt = Instant.now();
      PdeThreadBinding threadBinding =
          updatedThreadBinding(request, threadId, conversationFingerprint, startedAt, finishedAt);
      return new PdeAgentRunResult(
          request.context(),
          threadId,
          threadBinding,
          outcome.turnId(),
          status,
          outcome.output(),
          structuredOutput,
          outcome.errorMessage(),
          startedAt,
          finishedAt,
          outcome.events(),
          outcome.tokenUsage(),
          renderedMemory.audit(),
          protocolContract.codexVersion(),
          configuration.sdkVersion(),
          request.model(),
          request.promptVersion(),
          request.outputSchemaVersion(),
          PdeHashing.sha256(request.prompt()),
          effectiveInputSha256(renderedMemory.contextText(), request),
          PdeHashing.sha256(mapper, request.outputSchema()));
    } finally {
      closeListener(listenerRegistration, threadId, collector.turnId());
    }
  }

  /** Copia imagens validadas para o workspace efêmero sem expor o caminho original ao modelo. */
  private List<Path> materializeLocalImages(PdeAgentRunRequest request, Path workspace) {
    if (request.imageInputs().isEmpty()) {
      return List.of();
    }
    Path inputDirectory = workspace.resolve("inputs").normalize();
    if (!inputDirectory.startsWith(workspace)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Diretório de imagens saiu do workspace segregado");
    }
    try {
      Files.createDirectories(inputDirectory);
      restrictDirectoryAccess(inputDirectory);
      List<Path> materialized = new ArrayList<>();
      for (int index = 0; index < request.imageInputs().size(); index++) {
        PdeLocalImageInput imageInput = request.imageInputs().get(index);
        Path source = imageInput.sourcePath();
        if (Files.isSymbolicLink(source)
            || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
          throw new PdeHarnessException(
              PdeHarnessFailureCategory.INPUT_INVALID,
              "Imagem de entrada não é um arquivo regular: " + imageInput.reference());
        }
        long size = Files.size(source);
        if (size <= 0 || size > MAX_IMAGE_BYTES) {
          throw new PdeHarnessException(
              PdeHarnessFailureCategory.INPUT_INVALID,
              "Imagem de entrada possui tamanho inválido: " + imageInput.reference());
        }
        byte[] content = Files.readAllBytes(source);
        try {
          if (!imageInput.matchesMediaSignature(content)) {
            throw new PdeHarnessException(
                PdeHarnessFailureCategory.INPUT_INVALID,
                "Conteúdo da imagem diverge do tipo declarado: " + imageInput.reference());
          }
          String actualSha256 = PdeHashing.sha256(content);
          if (!actualSha256.equals(imageInput.sha256())) {
            throw new PdeHarnessException(
                PdeHarnessFailureCategory.INPUT_INVALID,
                "Hash da imagem de entrada diverge da auditoria: " + imageInput.reference());
          }
          Path target =
              inputDirectory
                  .resolve("image-" + (index + 1) + imageInput.safeExtension())
                  .normalize();
          if (!target.startsWith(inputDirectory)) {
            throw new PdeHarnessException(
                PdeHarnessFailureCategory.ISOLATION_VIOLATION,
                "Destino da imagem saiu do workspace segregado");
          }
          Files.write(target, content);
          restrictFileAccess(target);
          materialized.add(target.toRealPath(LinkOption.NOFOLLOW_LINKS));
        } finally {
          Arrays.fill(content, (byte) 0);
        }
      }
      return List.copyOf(materialized);
    } catch (PdeHarnessException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Imagem privada rejeitada antes do turno; produto="
              + request.context().productCode()
              + ", interação="
              + request.context().interactionReference(),
          ex);
      throw ex;
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao materializar imagem privada; produto="
              + request.context().productCode()
              + ", interação="
              + request.context().interactionReference(),
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.INPUT_INVALID,
          "Não foi possível preparar a imagem privada da interação",
          ex);
    }
  }

  /** Calcula a prova do contexto, prompt e imagens efetivamente autorizados para o turno. */
  private String effectiveInputSha256(String memoryContext, PdeAgentRunRequest request) {
    StringBuilder canonical =
        new StringBuilder()
            .append(memoryContext.length())
            .append(':')
            .append(memoryContext)
            .append(request.prompt().length())
            .append(':')
            .append(request.prompt());
    for (PdeLocalImageInput imageInput : request.imageInputs()) {
      canonical
          .append('\n')
          .append(imageInput.reference().length())
          .append(':')
          .append(imageInput.reference())
          .append(':')
          .append(imageInput.mediaType())
          .append(':')
          .append(imageInput.sha256());
    }
    return PdeHashing.sha256(canonical.toString());
  }

  /** Retorna a última prova de prontidão, iniciando o transporte quando necessário. */
  public PdeHarnessHealth health() {
    return start();
  }

  /** Encerra somente o processo local; estado e avanço continuam pertencendo ao backend. */
  @Override
  public void close() {
    client.close();
    activeConversations.clear();
    threadScopes.clear();
    health = null;
  }

  /** Cria uma thread nova ou retoma a persistida usando os mesmos limites de sandbox. */
  private JsonNode openThread(PdeAgentRunRequest request, Path workspace) {
    ObjectNode params = mapper.createObjectNode();
    params.put("model", request.model());
    params.put("cwd", workspace.toString());
    params.put("approvalPolicy", "never");
    params.put("sandbox", "read-only");
    if (request.existingThreadBinding() == null) {
      params.put("ephemeral", request.ephemeralThread());
      return client.request("thread/start", params);
    }
    params.put("threadId", request.existingThreadBinding().threadId());
    return client.request("thread/resume", params);
  }

  /** Impede duas execuções locais simultâneas de lerem a mesma revisão de conversa. */
  private void acquireConversation(String conversationFingerprint) {
    if (activeConversations.putIfAbsent(conversationFingerprint, Boolean.TRUE) != null) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONVERSATION_BUSY,
          "Já existe uma execução ativa para esta conversa");
    }
  }

  /** Mantém uma defesa local contra reutilização da mesma thread por outro escopo. */
  private void validateRuntimeThreadScope(String threadId, String conversationFingerprint) {
    String existing = threadScopes.putIfAbsent(threadId, conversationFingerprint);
    if (existing != null && !existing.equals(conversationFingerprint)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Thread já está vinculada a outra conversa neste processo");
    }
  }

  /** Confirma que o App Server retomou exatamente a thread autorizada pelo backend. */
  private void requireExpectedThread(PdeAgentRunRequest request, String returnedThreadId) {
    if (request.existingThreadBinding() != null
        && !request.existingThreadBinding().threadId().equals(returnedThreadId)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "App Server retomou thread diferente da autorizada");
    }
  }

  /** Atualiza o vínculo persistível sem transformar a thread em memória canônica. */
  private PdeThreadBinding updatedThreadBinding(
      PdeAgentRunRequest request,
      String threadId,
      String conversationFingerprint,
      Instant startedAt,
      Instant finishedAt) {
    PdeThreadBinding previous = request.existingThreadBinding();
    return new PdeThreadBinding(
        threadId,
        conversationFingerprint,
        request.memory().revision(),
        previous == null ? 1 : previous.completedTurns() + 1,
        previous == null && request.ephemeralThread(),
        previous == null ? startedAt : previous.createdAt(),
        finishedAt);
  }

  /** Interrompe o turno conhecido após timeout sem iniciar retentativa automática. */
  private void interruptTurn(String threadId, String turnId) {
    if (turnId == null || turnId.isBlank()) {
      return;
    }
    ObjectNode params = mapper.createObjectNode();
    params.put("threadId", threadId);
    params.put("turnId", turnId);
    try {
      client.request("turn/interrupt", params);
    } catch (RuntimeException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao interromper turno após timeout; threadId=" + threadId + ", turnId=" + turnId,
          ex);
    }
  }

  /** Exige o formato atual `{ entidade: { id } }` antes de prosseguir. */
  private String requireIdentifier(JsonNode response, String entity, String operation) {
    String identifier = response.path(entity).path("id").asText();
    if (identifier.isBlank()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Resposta " + operation + " não contém " + entity + ".id");
    }
    return identifier;
  }

  /** Garante que o workspace real seja uma subpasta da raiz explícita e não um symlink externo. */
  private Path validateWorkspace(Path requestedWorkspace) {
    Path normalized = requestedWorkspace.toAbsolutePath().normalize();
    Path configuredRoot = configuration.workspaceRoot().toAbsolutePath().normalize();
    if (normalized.equals(configuredRoot) || !normalized.startsWith(configuredRoot)) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Workspace da execução está fora da raiz segregada");
    }
    try {
      Files.createDirectories(configuredRoot);
      restrictDirectoryAccess(configuredRoot);
      Files.createDirectories(normalized);
      restrictDirectoryAccess(normalized);
      Path realRoot = configuredRoot.toRealPath();
      Path realWorkspace = normalized.toRealPath();
      if (realWorkspace.equals(realRoot) || !realWorkspace.startsWith(realRoot)) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.CONFIGURATION,
            "Workspace real da execução está fora da raiz segregada");
      }
      return realWorkspace;
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR, "Falha ao validar workspace PDE; workspace=" + normalized, ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Não foi possível preparar o workspace segregado",
          ex);
    }
  }

  /** Restringe um diretório ao usuário do worker quando o filesystem oferece permissões POSIX. */
  private void restrictDirectoryAccess(Path directory) throws IOException {
    PosixFileAttributeView attributeView =
        Files.getFileAttributeView(
            directory, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    if (attributeView != null) {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
    }
  }

  /**
   * Restringe uma mídia privada ao usuário do worker quando o filesystem oferece permissões POSIX.
   */
  private void restrictFileAccess(Path file) throws IOException {
    PosixFileAttributeView attributeView =
        Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    if (attributeView != null) {
      Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
    }
  }

  /** Remove somente o workspace derivado da interação e preserva a causa original quando houver. */
  private void deleteWorkspace(Path workspace, RuntimeException executionFailure) {
    if (!Files.exists(workspace)) {
      return;
    }
    try (var paths = Files.walk(workspace)) {
      List<Path> deletionOrder = paths.sorted(Comparator.reverseOrder()).toList();
      for (Path path : deletionOrder) {
        Files.deleteIfExists(path);
      }
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao descartar workspace PDE segregado; workspace=" + workspace,
          ex);
      PdeHarnessException cleanupFailure =
          new PdeHarnessException(
              PdeHarnessFailureCategory.ISOLATION_VIOLATION,
              "Não foi possível descartar o workspace segregado da interação",
              ex);
      if (executionFailure != null) {
        executionFailure.addSuppressed(cleanupFailure);
        return;
      }
      throw cleanupFailure;
    }
  }

  /** Compara caminhos reais para impedir que o processo use outra sessão Codex. */
  private void ensureSameCodexHome(Path actualCodexHome) {
    try {
      Path expected = configuration.codexHome().toRealPath();
      Path actual = actualCodexHome.toRealPath();
      if (!expected.equals(actual)) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "Codex App Server iniciou com CODEX_HOME diferente do configurado");
      }
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao validar CODEX_HOME retornado pelo App Server; actual=" + actualCodexHome,
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Não foi possível validar o CODEX_HOME do App Server",
          ex);
    }
  }

  /** Converte o status do protocolo e classifica autenticação como bloqueio recuperável. */
  private PdeRunStatus mapStatus(String status, String errorMessage) {
    return switch (status) {
      case "completed" -> PdeRunStatus.COMPLETED;
      case "interrupted" -> PdeRunStatus.INTERRUPTED;
      case "failed" ->
          isAuthenticationError(errorMessage) ? PdeRunStatus.BLOCKED : PdeRunStatus.FAILED;
      default -> PdeRunStatus.FAILED;
    };
  }

  /** Detecta somente marcadores objetivos de sessão ausente ou inválida. */
  private boolean isAuthenticationError(String errorMessage) {
    if (errorMessage == null) {
      return false;
    }
    String normalized = errorMessage.toLowerCase(Locale.ROOT);
    return normalized.contains("unauthorized")
        || normalized.contains("authentication")
        || normalized.contains("login");
  }

  /** Remove o observer mesmo quando a execução termina por exceção. */
  private void closeListener(AutoCloseable registration, String threadId, String turnId) {
    try {
      registration.close();
    } catch (Exception ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao remover observer; threadId=" + threadId + ", turnId=" + turnId,
          ex);
    }
  }
}
