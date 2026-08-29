package com.marketinghub.pde.harness.v1.consultant;

import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Compõe o envelope versionado sem esconder as partes auditáveis do prompt. */
final class PdeConsultantPromptComposer {
  static final String ENVELOPE_VERSION = "consultant-envelope-v1";
  private static final String RESOURCE = "/prompts/consultant/v1/turn-envelope.md";
  private static final System.Logger LOGGER =
      System.getLogger(PdeConsultantPromptComposer.class.getName());

  private final String template;

  /** Carrega integralmente o envelope versionado do classpath. */
  PdeConsultantPromptComposer() {
    this.template = loadTemplate();
  }

  /** Insere canal, partes e mensagem atual sem executar interpretação local. */
  String compose(PdeConsultantChannel channel, PdeConsultantPromptParts parts) {
    return template
        .replace("{{channel}}", channel.name())
        .replace("{{agentPart}}", parts.agentPart())
        .replace("{{activityPart}}", parts.activityPart())
        .replace("{{customerMessage}}", parts.customerMessage());
  }

  /** Produz a versão composta preservando as versões das duas partes. */
  String composedVersion(PdeConsultantPromptParts parts) {
    return ENVELOPE_VERSION
        + "+agent:"
        + parts.agentVersion()
        + "+activity:"
        + parts.activityVersion();
  }

  /** Lê o template e registra a causa completa quando o recurso estiver ausente. */
  private String loadTemplate() {
    try (InputStream input = PdeConsultantPromptComposer.class.getResourceAsStream(RESOURCE)) {
      if (input == null) {
        throw new IOException("Recurso não encontrado: " + RESOURCE);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao carregar envelope do consultor", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Envelope versionado do consultor não está disponível",
          ex);
    }
  }
}
