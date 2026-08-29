package com.marketinghub.pde.harness.v1.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Simula o protocolo atual do App Server sem conta, rede, modelo ou efeito externo. */
public final class FakeCodexAppServerMain {
  private static final System.Logger LOGGER =
      System.getLogger(FakeCodexAppServerMain.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Object WRITE_LOCK = new Object();
  private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
  private static final AtomicInteger TURN_SEQUENCE = new AtomicInteger();
  private static final ScheduledExecutorService SCHEDULER =
      Executors.newSingleThreadScheduledExecutor();

  private static BufferedWriter writer;

  /** Impede instanciação do processo sintético. */
  private FakeCodexAppServerMain() {}

  /** Lê JSONL da entrada padrão e responde com o contrato sintético configurado. */
  public static void main(String[] args) throws Exception {
    writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        handle(MAPPER.readTree(line));
      }
    } finally {
      SCHEDULER.shutdownNow();
    }
  }

  /** Despacha requests e responses reversas do cliente de teste. */
  private static void handle(JsonNode message) throws Exception {
    if (!message.hasNonNull("method")) {
      if (message.path("id").asLong() == 900 && message.hasNonNull("error")) {
        sendNotification("test/serverRequestDenied", MAPPER.createObjectNode().put("denied", true));
      }
      return;
    }
    String method = message.path("method").asText();
    switch (method) {
      case "initialize" -> initialize(message);
      case "initialized" -> initialized();
      case "thread/start" -> startThread(message);
      case "thread/resume" -> resumeThread(message);
      case "thread/delete" -> deleteThread(message);
      case "turn/start" -> startTurn(message);
      case "turn/interrupt" -> interruptTurn(message);
      case "test/outOfOrderA" ->
          scheduleResponse(message, 60, MAPPER.createObjectNode().put("method", method));
      case "test/outOfOrderB" ->
          scheduleResponse(message, 10, MAPPER.createObjectNode().put("method", method));
      case "test/environment" -> environment(message);
      case "test/requestServerApproval" -> requestServerApproval(message);
      case "test/exit" -> System.exit(7);
      default -> sendResult(message, MAPPER.createObjectNode().put("ok", true));
    }
  }

  /** Responde ao handshake com os campos obrigatórios do protocolo 0.149.0. */
  private static void initialize(JsonNode message) throws Exception {
    if ("exit-on-initialize".equals(mode())) {
      System.exit(2);
    }
    String codexHome = System.getenv("CODEX_HOME");
    ObjectNode result = MAPPER.createObjectNode();
    result.put("codexHome", codexHome);
    result.put("platformFamily", "unix");
    result.put("platformOs", "linux");
    result.put("userAgent", "fake-codex-app-server/0.149.0");
    sendResult(message, result);
  }

  /** Emite uma notificação neutra depois que o cliente confirma o handshake. */
  private static void initialized() throws Exception {
    sendNotification("account/updated", MAPPER.createObjectNode().put("authMode", "chatgpt"));
  }

  /** Cria uma thread sintética no formato atual `{thread:{id}}`. */
  private static void startThread(JsonNode message) throws Exception {
    String threadId = "thread-" + THREAD_SEQUENCE.incrementAndGet();
    sendResult(message, threadResponse(threadId));
  }

  /** Retoma exatamente a thread informada pelo chamador. */
  private static void resumeThread(JsonNode message) throws Exception {
    String threadId = message.path("params").path("threadId").asText();
    sendResult(message, threadResponse(threadId));
  }

  /** Confirma a exclusão solicitada e emite a notificação oficial correspondente. */
  private static void deleteThread(JsonNode message) throws Exception {
    String threadId = message.path("params").path("threadId").asText();
    sendResult(message, MAPPER.createObjectNode());
    sendNotification("thread/deleted", MAPPER.createObjectNode().put("threadId", threadId));
  }

  /** Inicia um turno e emite deltas, uso e conclusão de forma assíncrona. */
  private static void startTurn(JsonNode message) throws Exception {
    String threadId = message.path("params").path("threadId").asText();
    String turnId = "turn-" + TURN_SEQUENCE.incrementAndGet();
    ObjectNode turn = MAPPER.createObjectNode();
    turn.put("id", turnId);
    turn.put("status", "inProgress");
    turn.putArray("items");
    sendResult(message, MAPPER.createObjectNode().set("turn", turn));
    if ("timeout".equals(mode())) {
      return;
    }
    if ("authentication-failure".equals(mode())) {
      scheduleTurnCompletion(
          threadId, turnId, "failed", "Unauthorized: ChatGPT login required", 20);
      return;
    }
    JsonNode input = message.path("params").path("input");
    String memoryContext = input.path(0).path("text").asText();
    String prompt = input.path(input.size() - 1).path("text").asText();
    String output =
        switch (mode()) {
          case "invalid-output" -> "{json-invalido";
          case "schema-mismatch" -> "{\"campoInesperado\":true}";
          case "memory-aware" -> memoryAwareOutput(prompt, memoryContext);
          case "image-aware" ->
              responseOutput(
                  hasMaterializedImage(input) ? "imagem-privada-copiada" : "imagem-ausente");
          case "consultant-aware" -> consultantOutput(prompt, input);
          default -> responseOutput("Resposta para " + prompt);
        };
    SCHEDULER.schedule(
        () -> sendUnchecked(agentDelta(threadId, turnId, output)), 10, TimeUnit.MILLISECONDS);
    SCHEDULER.schedule(
        () -> sendUnchecked(tokenUsage(threadId, turnId)), 20, TimeUnit.MILLISECONDS);
    long completionDelay = "slow-completion".equals(mode()) ? 350 : 30;
    scheduleTurnCompletion(threadId, turnId, "completed", null, completionDelay);
  }

  /** Devolve um sinal simples para comprovar que a memória correta chegou ao turno. */
  private static String memoryAwareOutput(String prompt, String memoryContext) throws Exception {
    String signal =
        memoryContext.contains("prefere azul")
            ? "azul"
            : memoryContext.contains("prefere verde") ? "verde" : "sem-memoria";
    return responseOutput("Resposta para " + prompt + " com memória " + signal);
  }

  /** Serializa a saída sintética sem concatenar texto não escapado em JSON. */
  private static String responseOutput(String message) throws Exception {
    return MAPPER.writeValueAsString(MAPPER.createObjectNode().put("message", message));
  }

  /** Confirma que o caminho recebido aponta para a cópia efêmera preparada pelo SDK. */
  private static boolean hasMaterializedImage(JsonNode input) {
    for (JsonNode item : input) {
      if ("localImage".equals(item.path("type").asText())) {
        String path = item.path("path").asText();
        Path image = Path.of(path);
        return path.contains("/inputs/image-")
            && Files.isRegularFile(image)
            && hasPrivatePermissions(image, "rw-------")
            && hasPrivatePermissions(image.getParent(), "rwx------")
            && hasPrivatePermissions(image.getParent().getParent(), "rwx------");
      }
    }
    return false;
  }

  /** Confirma permissões privadas quando o filesystem de teste oferece atributos POSIX. */
  private static boolean hasPrivatePermissions(Path path, String expected) {
    PosixFileAttributeView attributeView =
        Files.getFileAttributeView(path, PosixFileAttributeView.class);
    try {
      return attributeView == null
          || Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString(expected));
    } catch (java.io.IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao inspecionar permissões da mídia sintética; path=" + path,
          ex);
      return false;
    }
  }

  /** Devolve o schema completo de consultoria e sinais do envelope e da imagem observados. */
  private static String consultantOutput(String prompt, JsonNode input) throws Exception {
    boolean promptPartsPresent =
        prompt.contains("# Parte do agente")
            && prompt.contains("# Parte da atividade")
            && prompt.contains("# Mensagem atual do cliente");
    ObjectNode result = MAPPER.createObjectNode();
    result.put("message", "Orientação sintética da consultora.");
    result.put(
        "recommendation",
        hasMaterializedImage(input) ? "Imagem privada recebida." : "Orientação textual recebida.");
    result.put("why", promptPartsPresent ? "Prompt dividido e auditável." : "Prompt incompleto.");
    result.putNull("nextQuestion");
    result.putArray("memoryCandidates");
    ObjectNode blocker = result.putObject("blocker");
    blocker.put("blocked", false);
    blocker.putNull("reason");
    blocker.putNull("userGuidance");
    blocker.putArray("helpLinks");
    return MAPPER.writeValueAsString(result);
  }

  /** Confirma a interrupção pedida pelo SDK após timeout. */
  private static void interruptTurn(JsonNode message) throws Exception {
    sendResult(message, MAPPER.createObjectNode());
    String threadId = message.path("params").path("threadId").asText();
    String turnId = message.path("params").path("turnId").asText();
    scheduleTurnCompletion(threadId, turnId, "interrupted", null, 1);
  }

  /** Informa se o processo filho recebeu alguma chave de API proibida. */
  private static void environment(JsonNode message) throws Exception {
    if ("malformed-json".equals(mode())) {
      sendRaw("{linha-invalida");
      return;
    }
    ObjectNode result = MAPPER.createObjectNode();
    result.put("openAiApiKeyPresent", System.getenv("OPENAI_API_KEY") != null);
    result.put("openAiApiKeyFilePresent", System.getenv("OPENAI_API_KEY_FILE") != null);
    sendResult(message, result);
  }

  /** Solicita uma aprovação reversa para comprovar que o SDK a recusa por padrão. */
  private static void requestServerApproval(JsonNode message) throws Exception {
    sendResult(message, MAPPER.createObjectNode().put("requested", true));
    ObjectNode request = MAPPER.createObjectNode();
    request.put("method", "permissions/requestApproval");
    request.put("id", 900);
    request.set("params", MAPPER.createObjectNode().put("reason", "teste sintético"));
    send(request);
  }

  /** Monta os campos suficientes da resposta de thread para o facade Java. */
  private static ObjectNode threadResponse(String threadId) {
    ObjectNode result = MAPPER.createObjectNode();
    ObjectNode thread = result.putObject("thread");
    thread.put("id", threadId);
    result.put("model", "gpt-test");
    result.put("modelProvider", "openai");
    result.put("cwd", System.getProperty("user.dir"));
    result.put("approvalPolicy", "never");
    result.put("approvalsReviewer", "user");
    result.put("sandbox", "read-only");
    return result;
  }

  /** Monta uma notificação incremental de mensagem do agente. */
  private static ObjectNode agentDelta(String threadId, String turnId, String delta) {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("threadId", threadId);
    params.put("turnId", turnId);
    params.put("itemId", "item-agent-1");
    params.put("delta", delta);
    return notification("item/agentMessage/delta", params);
  }

  /** Monta uma notificação sintética de tokens sem estimar custo. */
  private static ObjectNode tokenUsage(String threadId, String turnId) {
    ObjectNode params = MAPPER.createObjectNode();
    params.put("threadId", threadId);
    params.put("turnId", turnId);
    ObjectNode usage = params.putObject("tokenUsage");
    usage.put("inputTokens", 12);
    usage.put("outputTokens", 7);
    return notification("thread/tokenUsage/updated", params);
  }

  /** Agenda a notificação terminal do turno com status e erro opcionais. */
  private static void scheduleTurnCompletion(
      String threadId, String turnId, String status, String error, long delayMs) {
    SCHEDULER.schedule(
        () -> {
          ObjectNode params = MAPPER.createObjectNode();
          params.put("threadId", threadId);
          ObjectNode turn = params.putObject("turn");
          turn.put("id", turnId);
          turn.put("status", status);
          turn.putArray("items");
          if (error != null) {
            turn.putObject("error").put("message", error);
          }
          sendUnchecked(notification("turn/completed", params));
        },
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  /** Agenda uma response para testar correlação fora de ordem. */
  private static void scheduleResponse(JsonNode request, long delayMs, JsonNode result) {
    SCHEDULER.schedule(
        () -> {
          try {
            sendResult(request, result);
          } catch (Exception ex) {
            LOGGER.log(
                System.Logger.Level.ERROR,
                "Falha ao responder request agendada do App Server sintético",
                ex);
            throw new IllegalStateException("Falha ao responder test double", ex);
          }
        },
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  /** Envia uma response com o mesmo identificador da request. */
  private static void sendResult(JsonNode request, JsonNode result) throws Exception {
    ObjectNode response = MAPPER.createObjectNode();
    response.set("id", request.path("id"));
    response.set("result", result);
    send(response);
  }

  /** Envia uma notificação sem identificador de request. */
  private static void sendNotification(String method, JsonNode params) throws Exception {
    send(notification(method, params));
  }

  /** Monta uma notificação JSON-RPC no formato sem cabeçalho usado pelo App Server. */
  private static ObjectNode notification(String method, JsonNode params) {
    ObjectNode notification = MAPPER.createObjectNode();
    notification.put("method", method);
    notification.set("params", params);
    return notification;
  }

  /** Envia uma mensagem agendada convertendo falha de teste em erro terminal do processo. */
  private static void sendUnchecked(JsonNode message) {
    try {
      send(message);
    } catch (Exception ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao emitir evento do App Server sintético", ex);
      throw new IllegalStateException("Falha ao emitir evento do test double", ex);
    }
  }

  /** Escreve uma linha deliberadamente inválida para testar quebra de protocolo. */
  private static void sendRaw(String line) throws Exception {
    synchronized (WRITE_LOCK) {
      writer.write(line);
      writer.newLine();
      writer.flush();
    }
  }

  /** Escreve uma linha JSON completa sem intercalar tarefas agendadas. */
  private static void send(JsonNode message) throws Exception {
    synchronized (WRITE_LOCK) {
      writer.write(MAPPER.writeValueAsString(message));
      writer.newLine();
      writer.flush();
    }
  }

  /** Retorna o cenário selecionado pelo teste atual. */
  private static String mode() {
    return System.getenv().getOrDefault("PDE_FAKE_APP_SERVER_MODE", "normal");
  }
}
