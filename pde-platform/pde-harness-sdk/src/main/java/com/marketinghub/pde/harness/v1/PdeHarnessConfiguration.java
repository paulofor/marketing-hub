package com.marketinghub.pde.harness.v1;

import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Define o processo Codex, os limites e as raízes permitidas para o PDE Harness SDK. */
public record PdeHarnessConfiguration(
    String codexCommand,
    List<String> codexArguments,
    Path codexHome,
    Path workspaceRoot,
    Duration requestTimeout,
    Duration turnTimeout,
    String expectedCodexVersion,
    String clientName,
    String clientTitle,
    String sdkVersion,
    Map<String, String> environmentOverrides,
    boolean verifyCodexVersion) {

  private static final List<String> FORBIDDEN_ENVIRONMENT_KEYS =
      List.of("OPENAI_API_KEY", "OPENAI_API_KEY_FILE");

  /** Valida a configuração, normaliza caminhos e impede injeção de chave da API. */
  public PdeHarnessConfiguration {
    codexCommand = requireText(codexCommand, "codexCommand");
    codexArguments = List.copyOf(Objects.requireNonNull(codexArguments, "codexArguments"));
    if (codexArguments.isEmpty()) {
      throw new IllegalArgumentException("codexArguments não pode ser vazio");
    }
    codexHome = Objects.requireNonNull(codexHome, "codexHome").toAbsolutePath().normalize();
    workspaceRoot =
        Objects.requireNonNull(workspaceRoot, "workspaceRoot").toAbsolutePath().normalize();
    requestTimeout = requirePositive(requestTimeout, "requestTimeout");
    turnTimeout = requirePositive(turnTimeout, "turnTimeout");
    expectedCodexVersion = requireText(expectedCodexVersion, "expectedCodexVersion");
    clientName = requireText(clientName, "clientName");
    clientTitle = requireText(clientTitle, "clientTitle");
    sdkVersion = requireText(sdkVersion, "sdkVersion");
    environmentOverrides =
        Map.copyOf(Objects.requireNonNull(environmentOverrides, "environmentOverrides"));
    for (String key : environmentOverrides.keySet()) {
      if (isForbiddenEnvironmentKey(key)) {
        throw new IllegalArgumentException(key + " é proibida no PDE Harness SDK");
      }
    }
  }

  /** Cria a configuração canônica para um worker Java executando o App Server localmente. */
  public static PdeHarnessConfiguration standard(Path codexHome, Path workspaceRoot) {
    return new PdeHarnessConfiguration(
        "codex",
        List.of("app-server", "--listen", "stdio://"),
        codexHome,
        workspaceRoot,
        Duration.ofSeconds(60),
        Duration.ofMinutes(15),
        "0.149.0",
        "marketing_hub_pde_harness",
        "Marketing Hub PDE Harness",
        "0.2.0",
        Map.of(),
        true);
  }

  /** Deriva um workspace sem PII a partir da conversa, missão e interação autorizadas. */
  public Path workspaceFor(PdeRunContext context) {
    Objects.requireNonNull(context, "context");
    String executionFingerprint =
        PdeHashing.sha256(
            context.missionReference().length()
                + ":"
                + context.missionReference()
                + "\n"
                + context.interactionReference().length()
                + ":"
                + context.interactionReference());
    return workspaceRoot
        .resolve("conversations")
        .resolve(context.conversationScope().fingerprint())
        .resolve("interactions")
        .resolve(executionFingerprint)
        .toAbsolutePath()
        .normalize();
  }

  /** Informa se uma variável de ambiente criaria acesso direto proibido à OpenAI API. */
  public static boolean isForbiddenEnvironmentKey(String key) {
    return key != null
        && FORBIDDEN_ENVIRONMENT_KEYS.stream()
            .anyMatch(forbidden -> forbidden.equalsIgnoreCase(key));
  }

  /** Valida um texto obrigatório e devolve seu valor sem espaços externos. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }

  /** Valida que uma duração operacional seja positiva. */
  private static Duration requirePositive(Duration value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " deve ser positivo");
    }
    return value;
  }
}
