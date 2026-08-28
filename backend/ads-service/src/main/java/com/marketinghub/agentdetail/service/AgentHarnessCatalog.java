package com.marketinghub.agentdetail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentdetail.service.getDetail.AgentBehaviorFileResponse;
import com.marketinghub.agentdetail.service.getDetail.AgentHarnessArtifactResponse;
import com.marketinghub.agentdetail.service.getDetail.AgentHarnessResponse;
import com.marketinghub.agentdetail.service.getDetail.AgentHarnessSectionResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: carregar e validar o manifesto versionado dos harnesses dos agentes. */
@Component
@Slf4j
public class AgentHarnessCatalog {
  static final String MANIFEST_PATH = "agent-harness/agent-harness-v2.json";
  static final String BEHAVIOR_FILES_ROOT = "agent-behavior-files/";
  private static final Set<String> REQUIRED_SECTIONS =
      Set.of("executor", "runtime", "orchestration", "memory", "security", "observability");
  private static final Set<String> BEHAVIOR_ARTIFACT_TYPES =
      Set.of("PROMPT", "OUTPUT_SCHEMA", "BEHAVIOR_LIBRARY");

  private final HarnessCatalogDocument document;
  private final Map<String, HarnessAgentDefinition> definitionsByAgentKey;
  private final Map<String, List<AgentBehaviorFileResponse>> behaviorFilesByAgentKey;

  /**
   * Carrega o catálogo do classpath e interrompe o bootstrap quando o contrato estiver inválido.
   */
  public AgentHarnessCatalog(ObjectMapper objectMapper) {
    this.document = load(objectMapper);
    this.definitionsByAgentKey = indexAndValidate(document);
    this.behaviorFilesByAgentKey = loadBehaviorFiles(definitionsByAgentKey);
  }

  /**
   * Recupera o harness canônico ou sinaliza de forma explícita que ele ainda não foi registrado.
   */
  public AgentHarnessResponse getByAgentKey(String agentKey) {
    HarnessAgentDefinition definition = definitionsByAgentKey.get(agentKey);
    if (definition == null) {
      return AgentHarnessResponse.notRegistered(
          document.contractVersion(), document.sourceReference(), document.sensitiveValuesPolicy());
    }
    return new AgentHarnessResponse(
        "COMPLETE",
        document.contractVersion(),
        document.sourceReference(),
        document.sensitiveValuesPolicy(),
        List.copyOf(definition.sections()),
        List.copyOf(definition.artifacts()),
        behaviorFilesByAgentKey.get(definition.agentKey()));
  }

  /** Lê o manifesto imutável empacotado com o backend. */
  private HarnessCatalogDocument load(ObjectMapper objectMapper) {
    ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
    try (InputStream input = resource.getInputStream()) {
      return objectMapper.readValue(input, HarnessCatalogDocument.class);
    } catch (IOException ex) {
      log.error(
          "agent-harness-catalog-load-failed manifestPath={} operation=load-agent-harness-catalog",
          MANIFEST_PATH,
          ex);
      throw new IllegalStateException(
          "Não foi possível carregar o catálogo de harness dos agentes.", ex);
    }
  }

  /** Garante unicidade, seções obrigatórias e coleções não nulas antes de servir o catálogo. */
  private Map<String, HarnessAgentDefinition> indexAndValidate(HarnessCatalogDocument catalog) {
    if (catalog == null
        || blank(catalog.contractVersion())
        || blank(catalog.sourceReference())
        || blank(catalog.sensitiveValuesPolicy())
        || catalog.agents() == null) {
      throw new IllegalStateException("Manifesto de harness sem metadados obrigatórios.");
    }
    Map<String, HarnessAgentDefinition> indexed =
        catalog.agents().stream()
            .peek(this::validateAgent)
            .collect(
                Collectors.toMap(
                    HarnessAgentDefinition::agentKey,
                    Function.identity(),
                    (first, duplicate) -> {
                      throw new IllegalStateException(
                          "Harness duplicado para o agente " + duplicate.agentKey());
                    },
                    LinkedHashMap::new));
    return Map.copyOf(indexed);
  }

  /** Valida a cobertura mínima que caracteriza um harness operacional completo. */
  private void validateAgent(HarnessAgentDefinition agent) {
    if (agent == null
        || blank(agent.agentKey())
        || agent.sections() == null
        || agent.artifacts() == null) {
      throw new IllegalStateException("Definição de harness incompleta.");
    }
    Set<String> sectionCodes =
        agent.sections().stream()
            .map(AgentHarnessSectionResponse::code)
            .collect(Collectors.toSet());
    if (!sectionCodes.containsAll(REQUIRED_SECTIONS)) {
      throw new IllegalStateException(
          "Harness do agente " + agent.agentKey() + " não contém todas as seções obrigatórias.");
    }
    boolean hasPrompt =
        agent.artifacts().stream().anyMatch(artifact -> "PROMPT".equals(artifact.artifactType()));
    boolean hasSchema =
        agent.artifacts().stream()
            .anyMatch(artifact -> "OUTPUT_SCHEMA".equals(artifact.artifactType()));
    if (!hasPrompt || !hasSchema) {
      throw new IllegalStateException(
          "Harness do agente " + agent.agentKey() + " precisa registrar prompt e schema.");
    }
  }

  /** Carrega na inicialização todas as fontes comportamentais declaradas para cada agente. */
  private Map<String, List<AgentBehaviorFileResponse>> loadBehaviorFiles(
      Map<String, HarnessAgentDefinition> definitions) {
    Map<String, List<AgentBehaviorFileResponse>> behaviorFiles = new LinkedHashMap<>();
    definitions.forEach(
        (agentKey, definition) ->
            behaviorFiles.put(
                agentKey,
                definition.artifacts().stream()
                    .filter(artifact -> BEHAVIOR_ARTIFACT_TYPES.contains(artifact.artifactType()))
                    .map(this::loadBehaviorFile)
                    .toList()));
    return Map.copyOf(behaviorFiles);
  }

  /** Lê o conteúdo empacotado e calcula a identidade exata de uma fonte comportamental. */
  private AgentBehaviorFileResponse loadBehaviorFile(AgentHarnessArtifactResponse artifact) {
    String resourcePath = BEHAVIOR_FILES_ROOT + artifact.path();
    try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
      byte[] bytes = input.readAllBytes();
      return new AgentBehaviorFileResponse(
          artifact.artifactType(),
          artifact.name(),
          artifact.version(),
          artifact.path(),
          artifact.description(),
          artifact.path().endsWith(".json") ? "application/json" : "text/markdown",
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
          new String(bytes, StandardCharsets.UTF_8));
    } catch (IOException | NoSuchAlgorithmException ex) {
      log.error(
          "agent-behavior-file-load-failed path={} operation=load-agent-behavior-file",
          artifact.path(),
          ex);
      throw new IllegalStateException(
          "Não foi possível carregar o arquivo de comportamento " + artifact.path() + ".", ex);
    }
  }

  /** Informa se um texto obrigatório está ausente. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** Representa o documento raiz lido do manifesto versionado. */
  private record HarnessCatalogDocument(
      String contractVersion,
      String sourceReference,
      String sensitiveValuesPolicy,
      List<HarnessAgentDefinition> agents) {}

  /** Representa a configuração completa de harness associada a uma chave canônica de agente. */
  private record HarnessAgentDefinition(
      String agentKey,
      List<AgentHarnessSectionResponse> sections,
      List<AgentHarnessArtifactResponse> artifacts) {}
}
