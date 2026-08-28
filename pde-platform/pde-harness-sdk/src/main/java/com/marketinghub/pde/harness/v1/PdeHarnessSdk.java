package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.internal.CodexVersionVerifier;
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import com.marketinghub.pde.harness.v1.internal.PdeOutputSchemaPolicy;
import com.marketinghub.pde.harness.v1.internal.PdeProtocolContract;
import com.marketinghub.pde.harness.v1.internal.PdeStructuredOutputValidator;
import com.marketinghub.pde.harness.v1.internal.PdeTurnCollector;
import com.marketinghub.pde.harness.v1.internal.PdeTurnOutcome;
import com.marketinghub.pde.harness.v1.internal.transport.CodexAppServerClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Oferece a fachada Java do PDE Harness para executar um agente sem assumir a orquestração do
 * backend.
 */
public final class PdeHarnessSdk implements AutoCloseable {
  private static final System.Logger LOGGER = System.getLogger(PdeHarnessSdk.class.getName());

  private final PdeHarnessConfiguration configuration;
  private final ObjectMapper mapper;
  private final PdeProtocolContract protocolContract;
  private final CodexVersionVerifier versionVerifier;
  private final PdeStructuredOutputValidator structuredOutputValidator;
  private final CodexAppServerClient client;

  private volatile PdeHarnessHealth health;

  /** Cria o SDK e valida a integridade do contrato embarcado sem iniciar processo externo. */
  public PdeHarnessSdk(PdeHarnessConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration");
    this.mapper = new ObjectMapper();
    this.protocolContract = new PdeProtocolContract(mapper);
    this.versionVerifier = new CodexVersionVerifier();
    this.structuredOutputValidator = new PdeStructuredOutputValidator(mapper);
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
    PdeOutputSchemaPolicy.validate(request.outputSchema());
    Path workspace = validateWorkspace(request.context().workspace());
    start();
    Instant startedAt = Instant.now();

    JsonNode threadResponse = openThread(request, workspace);
    String threadId = requireIdentifier(threadResponse, "thread", "thread/start ou thread/resume");
    PdeTurnCollector collector = new PdeTurnCollector(threadId, observer);
    collector.recordThreadReady(threadResponse);

    AutoCloseable listenerRegistration = client.addNotificationListener(collector::accept);
    try {
      ObjectNode turnParams = mapper.createObjectNode();
      turnParams.put("threadId", threadId);
      ArrayNode input = turnParams.putArray("input");
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
      return new PdeAgentRunResult(
          request.context(),
          threadId,
          outcome.turnId(),
          status,
          outcome.output(),
          structuredOutput,
          outcome.errorMessage(),
          startedAt,
          Instant.now(),
          outcome.events(),
          outcome.tokenUsage(),
          protocolContract.codexVersion(),
          configuration.sdkVersion(),
          request.model(),
          request.promptVersion(),
          request.outputSchemaVersion(),
          PdeHashing.sha256(request.prompt()),
          PdeHashing.sha256(mapper, request.outputSchema()));
    } finally {
      closeListener(listenerRegistration, threadId, collector.turnId());
    }
  }

  /** Retorna a última prova de prontidão, iniciando o transporte quando necessário. */
  public PdeHarnessHealth health() {
    return start();
  }

  /** Encerra somente o processo local; estado e avanço continuam pertencendo ao backend. */
  @Override
  public void close() {
    client.close();
    health = null;
  }

  /** Cria uma thread nova ou retoma a persistida usando os mesmos limites de sandbox. */
  private JsonNode openThread(PdeAgentRunRequest request, Path workspace) {
    ObjectNode params = mapper.createObjectNode();
    params.put("model", request.model());
    params.put("cwd", workspace.toString());
    params.put("approvalPolicy", "never");
    params.put("sandbox", "read-only");
    if (request.existingThreadId() == null) {
      params.put("ephemeral", request.ephemeralThread());
      return client.request("thread/start", params);
    }
    params.put("threadId", request.existingThreadId());
    return client.request("thread/resume", params);
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
      Files.createDirectories(normalized);
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
