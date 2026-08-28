package com.marketinghub.pde.harness.v1.internal;

import com.marketinghub.pde.harness.v1.PdeCustomerMemory;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.marketinghub.pde.harness.v1.PdeMemoryAudit;
import com.marketinghub.pde.harness.v1.PdeMemoryEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Renderiza memória autorizada como dados delimitados por um template versionado. */
public final class PdeMemoryContextRenderer {
  private static final System.Logger LOGGER =
      System.getLogger(PdeMemoryContextRenderer.class.getName());
  private static final String RESOURCE_PATH = "/prompts/memory/v1/customer-memory-context.md";
  private static final String TEMPLATE_VERSION = "customer-memory-context-v1";

  private final String template;
  private final String templateSha256;

  /** Carrega uma única vez o contrato que impede memória de virar instrução operacional. */
  public PdeMemoryContextRenderer() {
    this.template = loadTemplate();
    this.templateSha256 = PdeHashing.sha256(template);
  }

  /** Produz o bloco efetivo, remove itens expirados e cria a auditoria correspondente. */
  public PdeRenderedMemory render(PdeCustomerMemory memory, Instant effectiveAt) {
    Objects.requireNonNull(memory, "memory");
    Objects.requireNonNull(effectiveAt, "effectiveAt");
    List<PdeMemoryEntry> activeEntries = memory.activeEntriesAt(effectiveAt);
    String context =
        template
            .replace("{{SCOPE_FINGERPRINT}}", memory.scope().fingerprint())
            .replace("{{MEMORY_REVISION}}", Long.toString(memory.revision()))
            .replace("{{RELATIONSHIP_SUMMARY}}", renderSummary(memory.relationshipSummary()))
            .replace("{{MEMORY_ITEMS}}", renderEntries(activeEntries));
    PdeMemoryAudit audit =
        new PdeMemoryAudit(
            memory.scope().fingerprint(),
            memory.revision(),
            activeEntries.size(),
            memory.fingerprintAt(effectiveAt),
            TEMPLATE_VERSION,
            templateSha256);
    return new PdeRenderedMemory(context, audit);
  }

  /** Devolve a versão estável para testes de contrato e persistência do backend. */
  public String templateVersion() {
    return TEMPLATE_VERSION;
  }

  /** Carrega o recurso do classpath e falha fechado quando o pacote estiver incompleto. */
  private String loadTemplate() {
    try (InputStream input = PdeMemoryContextRenderer.class.getResourceAsStream(RESOURCE_PATH)) {
      if (input == null) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.CONFIGURATION,
            "Template versionado de memória não foi encontrado no classpath");
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao carregar template de memória; resource=" + RESOURCE_PATH,
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Não foi possível carregar o template versionado de memória",
          ex);
    }
  }

  /** Explicita ausência de histórico em vez de induzir o modelo a completar lacunas. */
  private String renderSummary(String summary) {
    return summary == null || summary.isBlank()
        ? "Nenhum resumo anterior autorizado."
        : escapeData(summary);
  }

  /** Preserva a ordem de relevância definida pelo backend e a procedência de cada fato. */
  private String renderEntries(List<PdeMemoryEntry> entries) {
    if (entries.isEmpty()) {
      return "- Nenhum fato anterior autorizado para esta missão.";
    }
    StringBuilder rendered = new StringBuilder();
    for (int index = 0; index < entries.size(); index++) {
      PdeMemoryEntry entry = entries.get(index);
      rendered
          .append("- Item ")
          .append(index + 1)
          .append(" | categoria=")
          .append(entry.category())
          .append(" | origem=")
          .append(entry.source())
          .append(" | confiança=")
          .append(String.format(Locale.ROOT, "%.2f", entry.confidence()))
          .append(" | observado_em=")
          .append(entry.observedAt())
          .append(" | conteúdo=\"")
          .append(escapeData(entry.content()))
          .append("\"");
      if (index + 1 < entries.size()) {
        rendered.append('\n');
      }
    }
    return rendered.toString();
  }

  /** Neutraliza quebras e delimitadores que poderiam escapar do bloco de dados. */
  private String escapeData(String value) {
    return value
        .replace("\r", " ")
        .replace("\n", " ")
        .replace("```", "'''")
        .replace("<", "‹")
        .replace(">", "›")
        .trim();
  }
}
