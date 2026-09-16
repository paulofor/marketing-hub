package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar e consumir o texto Opala fixado pelo backend, sem fallback local. */
final class CatalogPromptInput {
  /** Impede instanciação de um contrato de entrada sem estado. */
  private CatalogPromptInput() {}

  /** Reconhece somente o fluxo explicitamente migrado para o catálogo textual. */
  static boolean migrated(Map<String, Object> task) {
    return "opala-commercial-preparation-v1".equals(task.get("processCode"));
  }

  /** Exige identidade, versão, hash e schema exatos antes de enviar a instrução ao modelo. */
  static String text(Map<String, Object> task, String schemaResource) throws IOException {
    if (!(task.get("catalogPrompt") instanceof Map<?, ?> prompt))
      throw new IllegalArgumentException(
          "Catálogo Vivo: tarefa Opala sem prompt fixado pelo backend.");
    if (!"DATABASE".equals(prompt.get("origin"))
        || !"meta-ad-approver-worker".equals(prompt.get("executorModule"))
        || !Objects.equals(task.get("agentKey"), prompt.get("agentKey"))
        || !Objects.equals(task.get("activityId"), prompt.get("activityId"))
        || !(prompt.get("processVersion") instanceof Number version)
        || !(task.get("processVersion") instanceof Number taskVersion)
        || version.intValue() != taskVersion.intValue()
        || !(prompt.get("versionId") instanceof Number id)
        || id.longValue() <= 0
        || !(prompt.get("versionNumber") instanceof Number number)
        || number.intValue() <= 0
        || !schemaResource.equals(prompt.get("schemaId")))
      throw new IllegalArgumentException(
          "Catálogo Vivo: identidade ou contrato incompatível com o executor.");
    if (!(prompt.get("text") instanceof String text)
        || text.isBlank()
        || !sha256(text).equals(prompt.get("sha256")))
      throw new IllegalArgumentException("Catálogo Vivo: texto ausente ou hash divergente.");
    String schema =
        new ClassPathResource(schemaResource).getContentAsString(StandardCharsets.UTF_8);
    if (!sha256(schema).equals(prompt.get("schemaSha256")))
      throw new IllegalArgumentException("Catálogo Vivo: schema empacotado incompatível.");
    String removed = text.replace("{{TASK_CONTEXT}}", "");
    if (text.indexOf("{{TASK_CONTEXT}}") < 0
        || text.indexOf("{{TASK_CONTEXT}}") != text.lastIndexOf("{{TASK_CONTEXT}}")
        || removed.contains("{{")
        || removed.contains("}}"))
      throw new IllegalArgumentException("Catálogo Vivo: placeholders incompatíveis.");
    return text;
  }

  /** Remove o próprio texto do contexto para evitar duplicação recursiva de instruções e custo. */
  static Map<String, Object> context(Map<String, Object> task) {
    if (!migrated(task)) return task;
    var context = new LinkedHashMap<String, Object>(task);
    Object raw = context.remove("catalogPrompt");
    if (raw instanceof Map<?, ?> p)
      context.put(
          "catalogPromptReference",
          Map.of("versionId", p.get("versionId"), "sha256", p.get("sha256")));
    return context;
  }

  /** Calcula o hash UTF-8 do texto exato recebido ou empacotado. */
  static String sha256(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      org.slf4j.LoggerFactory.getLogger(CatalogPromptInput.class)
          .error("catalogo-vivo sha256 indisponível", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }
}
