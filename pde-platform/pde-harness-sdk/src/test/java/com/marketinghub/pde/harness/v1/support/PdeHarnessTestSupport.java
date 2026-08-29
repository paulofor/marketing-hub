package com.marketinghub.pde.harness.v1.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeConversationScope;
import com.marketinghub.pde.harness.v1.PdeCustomerMemory;
import com.marketinghub.pde.harness.v1.PdeCustomerScope;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeMemoryEntry;
import com.marketinghub.pde.harness.v1.PdeRunContext;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Centraliza configuração, comando e schema sintéticos usados pelos testes do SDK. */
public final class PdeHarnessTestSupport {
  private static final System.Logger LOGGER =
      System.getLogger(PdeHarnessTestSupport.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Impede instanciação do utilitário de testes. */
  private PdeHarnessTestSupport() {}

  /** Cria uma configuração apontando para o App Server Java sintético. */
  public static PdeHarnessConfiguration configuration(
      Path codexHome, Path workspaceRoot, String mode, Duration turnTimeout) {
    String javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String classpath =
        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add("-cp");
    arguments.add(classpath);
    arguments.add(FakeCodexAppServerMain.class.getName());
    Map<String, String> environment =
        mode == null ? Map.of() : Map.of("PDE_FAKE_APP_SERVER_MODE", mode);
    return new PdeHarnessConfiguration(
        javaCommand,
        arguments,
        codexHome,
        workspaceRoot,
        Duration.ofSeconds(2),
        turnTimeout,
        "0.149.0",
        "pde_harness_test",
        "PDE Harness Test",
        "0.3.0-test",
        environment,
        false);
  }

  /** Cria um schema estruturado mínimo e estrito para o caminho feliz. */
  public static JsonNode validOutputSchema() {
    try {
      return MAPPER.readTree(
          """
          {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "message": { "type": "string" }
            },
            "required": ["message"]
          }
          """);
    } catch (Exception ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao carregar schema sintético", ex);
      throw new IllegalStateException("Schema de teste inválido", ex);
    }
  }

  /** Cria o escopo durável de um cliente sintético no tenant e produto de teste. */
  public static PdeCustomerScope customerScope(String customerReference) {
    return new PdeCustomerScope("tenant-teste", "produto-teste", customerReference);
  }

  /** Cria uma conversa sintética isolada para o cliente informado. */
  public static PdeConversationScope conversationScope(
      String customerReference, String conversationReference) {
    return new PdeConversationScope(customerScope(customerReference), "v1", conversationReference);
  }

  /** Cria os correlatores sintéticos de uma execução sem aceitar workspace externo. */
  public static PdeRunContext context(
      String customerReference,
      String conversationReference,
      String missionReference,
      String interactionReference) {
    return new PdeRunContext(
        conversationScope(customerReference, conversationReference),
        missionReference,
        interactionReference);
  }

  /** Cria memória vazia explícita para um cliente que ainda não possui histórico. */
  public static PdeCustomerMemory emptyMemory(String customerReference) {
    return PdeCustomerMemory.empty(
        customerScope(customerReference), Instant.parse("2026-08-28T12:00:00Z"));
  }

  /** Cria um snapshot sintético com revisão e fatos fornecidos pelo teste. */
  public static PdeCustomerMemory memory(
      String customerReference,
      long revision,
      String relationshipSummary,
      List<PdeMemoryEntry> entries) {
    return new PdeCustomerMemory(
        customerScope(customerReference),
        revision,
        Instant.parse("2026-08-28T12:00:00Z"),
        relationshipSummary,
        entries);
  }
}
