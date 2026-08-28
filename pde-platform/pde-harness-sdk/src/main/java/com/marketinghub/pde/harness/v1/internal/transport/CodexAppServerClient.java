package com.marketinghub.pde.harness.v1.internal.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.marketinghub.pde.harness.v1.internal.SecretSanitizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Gerencia o processo local e a correlação JSON-RPC bidirecional do Codex App Server. */
public final class CodexAppServerClient implements AutoCloseable {
  private static final System.Logger LOGGER =
      System.getLogger(CodexAppServerClient.class.getName());

  private final PdeHarnessConfiguration configuration;
  private final ObjectMapper mapper;
  private final AtomicLong nextRequestId = new AtomicLong(1);
  private final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<Consumer<CodexAppServerNotification>> notificationListeners =
      new CopyOnWriteArrayList<>();
  private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private final Object writeLock = new Object();

  private volatile Process process;
  private volatile BufferedWriter writer;
  private volatile boolean ready;
  private volatile boolean closing;
  private volatile boolean protocolBroken;
  private volatile JsonNode initializeResponse;

  /** Cria o cliente de transporte sem iniciar o processo externo. */
  public CodexAppServerClient(PdeHarnessConfiguration configuration, ObjectMapper mapper) {
    this.configuration = Objects.requireNonNull(configuration, "configuration");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /** Inicia o processo, executa `initialize` e confirma `initialized` uma única vez. */
  public synchronized JsonNode start() {
    if (ready) {
      return initializeResponse.deepCopy();
    }
    closing = false;
    protocolBroken = false;
    prepareDirectories();
    ProcessBuilder builder = new ProcessBuilder(buildCommand());
    builder.directory(configuration.workspaceRoot().toFile());
    sanitizeEnvironment(builder.environment());
    try {
      process = builder.start();
      writer =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      ioExecutor.submit(this::readStdout);
      ioExecutor.submit(this::readStderr);
      process.onExit().thenAccept(this::handleExit);

      ObjectNode initializeParams = mapper.createObjectNode();
      ObjectNode clientInfo = initializeParams.putObject("clientInfo");
      clientInfo.put("name", configuration.clientName());
      clientInfo.put("title", configuration.clientTitle());
      clientInfo.put("version", configuration.sdkVersion());
      initializeResponse = requestInternal("initialize", initializeParams);
      validateInitializeResponse(initializeResponse);
      sendNotification("initialized", mapper.createObjectNode());
      ready = true;
      return initializeResponse.deepCopy();
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao iniciar o Codex App Server por stdio; command=" + configuration.codexCommand(),
          ex);
      close();
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Não foi possível iniciar o Codex App Server",
          ex);
    } catch (RuntimeException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha durante o handshake do Codex App Server; client=" + configuration.clientName(),
          ex);
      close();
      throw ex;
    }
  }

  /** Envia uma request após o handshake e devolve somente o campo `result`. */
  public JsonNode request(String method, JsonNode params) {
    if (!ready) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Codex App Server não está pronto para " + method);
    }
    return requestInternal(method, params);
  }

  /** Registra um listener e devolve uma ação idempotente para removê-lo. */
  public AutoCloseable addNotificationListener(Consumer<CodexAppServerNotification> listener) {
    Consumer<CodexAppServerNotification> safeListener =
        Objects.requireNonNull(listener, "listener");
    notificationListeners.add(safeListener);
    return () -> notificationListeners.remove(safeListener);
  }

  /** Informa se processo e handshake continuam disponíveis. */
  public boolean isReady() {
    Process current = process;
    return ready && current != null && current.isAlive();
  }

  /** Retorna a quantidade atual de requests pendentes para testes de correlação. */
  public int pendingRequestCount() {
    return pendingRequests.size();
  }

  /** Encerra streams, processo, futures e threads virtuais sem deixar request órfã. */
  @Override
  public synchronized void close() {
    if (closing) {
      return;
    }
    closing = true;
    ready = false;
    failAllPending(
        new PdeHarnessException(
            PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
            "Codex App Server foi encerrado antes de responder"));

    BufferedWriter currentWriter = writer;
    writer = null;
    if (currentWriter != null) {
      try {
        currentWriter.close();
      } catch (IOException ex) {
        LOGGER.log(System.Logger.Level.WARNING, "Falha ao fechar stdin do Codex App Server", ex);
      }
    }

    Process current = process;
    process = null;
    if (current != null && current.isAlive()) {
      current.destroy();
      try {
        if (!current.waitFor(2, TimeUnit.SECONDS)) {
          current.destroyForcibly();
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        LOGGER.log(
            System.Logger.Level.ERROR, "Encerramento do Codex App Server foi interrompido", ex);
        current.destroyForcibly();
      }
    }
    ioExecutor.shutdownNow();
  }

  /** Monta o comando sem shell para impedir expansão ou injeção de argumentos. */
  private ArrayList<String> buildCommand() {
    ArrayList<String> command = new ArrayList<>();
    command.add(configuration.codexCommand());
    command.addAll(configuration.codexArguments());
    return command;
  }

  /** Garante que as duas raízes explícitas existam antes de iniciar o processo. */
  private void prepareDirectories() {
    try {
      Files.createDirectories(configuration.codexHome());
      Files.createDirectories(configuration.workspaceRoot());
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao preparar diretórios do PDE Harness SDK; codexHome="
              + configuration.codexHome()
              + ", workspaceRoot="
              + configuration.workspaceRoot(),
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Não foi possível preparar os diretórios do PDE Harness SDK",
          ex);
    }
  }

  /** Remove credenciais de API herdadas e fixa o diretório de sessão gerenciado pelo Codex. */
  private void sanitizeEnvironment(Map<String, String> environment) {
    environment.keySet().removeIf(PdeHarnessConfiguration::isForbiddenEnvironmentKey);
    environment.putAll(configuration.environmentOverrides());
    environment.put("CODEX_HOME", configuration.codexHome().toString());
  }

  /** Envia uma request correlacionada e aplica o timeout individual do contrato. */
  private JsonNode requestInternal(String method, JsonNode params) {
    long requestId = nextRequestId.getAndIncrement();
    ObjectNode request = mapper.createObjectNode();
    request.put("method", method);
    request.put("id", requestId);
    if (params != null) {
      request.set("params", params);
    }
    CompletableFuture<JsonNode> response = new CompletableFuture<>();
    pendingRequests.put(requestId, response);
    try {
      write(request);
      return response.get(configuration.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      pendingRequests.remove(requestId);
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Timeout em request do Codex App Server; method=" + method + ", requestId=" + requestId,
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.TIMEOUT, "Timeout no Codex App Server durante " + method, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      pendingRequests.remove(requestId);
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Request do Codex App Server interrompida; method=" + method + ", requestId=" + requestId,
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Request do Codex App Server foi interrompida durante " + method,
          ex);
    } catch (ExecutionException ex) {
      pendingRequests.remove(requestId);
      Throwable cause = ex.getCause() == null ? ex : ex.getCause();
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Codex App Server rejeitou request; method=" + method + ", requestId=" + requestId,
          cause);
      if (cause instanceof PdeHarnessException harnessException) {
        throw harnessException;
      }
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.EXECUTION_FAILED, "Codex App Server rejeitou " + method, cause);
    } catch (RuntimeException ex) {
      pendingRequests.remove(requestId);
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao enviar request ao Codex App Server; method="
              + method
              + ", requestId="
              + requestId,
          ex);
      throw ex;
    }
  }

  /** Envia uma notificação sem criar future de resposta. */
  private void sendNotification(String method, JsonNode params) {
    ObjectNode notification = mapper.createObjectNode();
    notification.put("method", method);
    if (params != null) {
      notification.set("params", params);
    }
    write(notification);
  }

  /** Serializa uma mensagem JSONL inteira sob lock para impedir linhas intercaladas. */
  private void write(JsonNode message) {
    BufferedWriter currentWriter = writer;
    if (currentWriter == null) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Codex App Server está sem stdin gravável");
    }
    synchronized (writeLock) {
      try {
        currentWriter.write(mapper.writeValueAsString(message));
        currentWriter.newLine();
        currentWriter.flush();
      } catch (IOException ex) {
        LOGGER.log(System.Logger.Level.ERROR, "Falha ao escrever JSONL no Codex App Server", ex);
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
            "Falha ao escrever no Codex App Server",
            ex);
      }
    }
  }

  /** Lê respostas, requests reversas e notificações até o encerramento do stdout. */
  private void readStdout() {
    Process current = process;
    if (current == null) {
      return;
    }
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(current.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        handleLine(line);
      }
    } catch (IOException ex) {
      if (!closing && !protocolBroken) {
        LOGGER.log(System.Logger.Level.ERROR, "Falha ao ler stdout do Codex App Server", ex);
        failAllPending(
            new PdeHarnessException(
                PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
                "Fluxo de saída do Codex App Server falhou",
                ex));
      }
    }
  }

  /** Lê stderr e registra somente alertas ou erros sanitizados do processo externo. */
  private void readStderr() {
    Process current = process;
    if (current == null) {
      return;
    }
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(current.getErrorStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String sanitized = SecretSanitizer.sanitize(line);
        if (isOperationalDiagnostic(sanitized)) {
          LOGGER.log(System.Logger.Level.WARNING, "Codex App Server stderr: " + sanitized);
        }
      }
    } catch (IOException ex) {
      if (!closing && !protocolBroken) {
        LOGGER.log(System.Logger.Level.ERROR, "Falha ao ler stderr do Codex App Server", ex);
      }
    }
  }

  /** Reconhece níveis operacionais sem registrar tracing INFO que pode conter payload funcional. */
  private boolean isOperationalDiagnostic(String line) {
    if (line == null || line.isBlank()) {
      return false;
    }
    String normalized = line.toUpperCase(java.util.Locale.ROOT);
    return normalized.startsWith("WARN")
        || normalized.startsWith("ERROR")
        || normalized.contains(" WARN ")
        || normalized.contains(" ERROR ");
  }

  /** Classifica uma linha do protocolo sem confundir requests reversas com responses. */
  private void handleLine(String line) {
    if (line == null || line.isBlank()) {
      return;
    }
    JsonNode message;
    try {
      message = mapper.readTree(line);
    } catch (JsonProcessingException ex) {
      PdeHarnessException protocolFailure =
          new PdeHarnessException(
              PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
              "Codex App Server retornou uma linha JSON inválida",
              ex);
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Codex App Server retornou JSON inválido: " + SecretSanitizer.sanitize(line),
          ex);
      protocolBroken = true;
      ready = false;
      failAllPending(protocolFailure);
      Process current = process;
      if (current != null && current.isAlive()) {
        current.destroy();
      }
      return;
    }

    if (message.hasNonNull("method") && message.has("id")) {
      refuseServerRequest(message);
      return;
    }
    if (message.has("id")) {
      handleResponse(message);
      return;
    }
    if (message.hasNonNull("method")) {
      dispatchNotification(message);
    }
  }

  /** Completa somente a future que possui o mesmo identificador numérico. */
  private void handleResponse(JsonNode message) {
    if (!message.path("id").canConvertToLong()) {
      LOGGER.log(System.Logger.Level.WARNING, "Resposta Codex App Server possui id não numérico");
      return;
    }
    long id = message.path("id").asLong();
    CompletableFuture<JsonNode> pending = pendingRequests.remove(id);
    if (pending == null) {
      LOGGER.log(
          System.Logger.Level.WARNING,
          "Resposta Codex App Server sem request pendente; requestId=" + id);
      return;
    }
    if (message.hasNonNull("error")) {
      JsonNode error = message.path("error");
      String detail = SecretSanitizer.sanitize(error.path("message").asText("erro não informado"));
      pending.completeExceptionally(
          new PdeHarnessException(classifyProtocolError(detail), "Codex App Server: " + detail));
      return;
    }
    pending.complete(message.path("result").deepCopy());
  }

  /** Recusa requests reversas não autorizadas para que o SDK nunca aprove ação sozinho. */
  private void refuseServerRequest(JsonNode message) {
    ObjectNode response = mapper.createObjectNode();
    response.set("id", message.path("id"));
    ObjectNode error = response.putObject("error");
    error.put("code", -32601);
    error.put("message", "Solicitação do App Server não autorizada pelo PDE Harness SDK v1");
    write(response);
  }

  /** Distribui uma notificação isolando falhas de observers do fluxo principal. */
  private void dispatchNotification(JsonNode message) {
    CodexAppServerNotification notification =
        new CodexAppServerNotification(message.path("method").asText(), message.get("params"));
    for (Consumer<CodexAppServerNotification> listener : notificationListeners) {
      try {
        listener.accept(notification);
      } catch (RuntimeException ex) {
        LOGGER.log(
            System.Logger.Level.ERROR,
            "Observer falhou ao processar notificação do Codex App Server; method="
                + notification.method(),
            ex);
      }
    }
  }

  /** Verifica os quatro campos mínimos definidos pelo contrato de initialize atual. */
  private void validateInitializeResponse(JsonNode response) {
    for (String field : new String[] {"codexHome", "platformFamily", "platformOs", "userAgent"}) {
      if (!response.hasNonNull(field) || response.path(field).asText().isBlank()) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "Resposta initialize não contém " + field);
      }
    }
  }

  /** Marca a conexão como indisponível e conclui qualquer request que ficou pendente. */
  private void handleExit(Process exitedProcess) {
    ready = false;
    if (!closing && !protocolBroken) {
      int exitCode = exitedProcess.exitValue();
      PdeHarnessException error =
          new PdeHarnessException(
              PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
              "Codex App Server encerrou inesperadamente com código " + exitCode);
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Codex App Server encerrou inesperadamente; exitCode=" + exitCode,
          error);
      failAllPending(error);
    }
  }

  /** Encerra todas as futures pendentes usando a mesma causa operacional. */
  private void failAllPending(Throwable error) {
    pendingRequests.forEach((id, future) -> future.completeExceptionally(error));
    pendingRequests.clear();
  }

  /** Converte erros de autenticação em bloqueio recuperável sem fallback de runtime. */
  private PdeHarnessFailureCategory classifyProtocolError(String detail) {
    String normalized = detail.toLowerCase(java.util.Locale.ROOT);
    if (normalized.contains("unauthorized")
        || normalized.contains("authentication")
        || normalized.contains("login")) {
      return PdeHarnessFailureCategory.AUTHENTICATION_REQUIRED;
    }
    return PdeHarnessFailureCategory.EXECUTION_FAILED;
  }
}
