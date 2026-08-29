package com.marketinghub.planning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar um pacote criativo aprovado antes de qualquer persistência. */
final class ApprovedCreativePackageArchive {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ApprovedCreativePackageArchive.class);
  private static final long MAX_ARCHIVE_BYTES = 100L * 1024 * 1024;
  private static final long MAX_EXPANDED_BYTES = 200L * 1024 * 1024;
  private static final int MAX_ENTRIES = 96;
  private static final Set<String> REQUIRED_METADATA =
      Set.of(
          "metadata/contract.json",
          "metadata/manifest.json",
          "metadata/iris-direction.json",
          "metadata/apollo-storyboard.json",
          "metadata/psique-review.json",
          "metadata/temis-review.json",
          "metadata/agent-executions.json");

  private final ObjectMapper objectMapper;

  /** Configura a leitura estrita de JSON do pacote. */
  ApprovedCreativePackageArchive(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Lê, confere hashes, agentes, direitos e separação das revisões do pacote. */
  ValidatedPackage validate(byte[] archiveBytes, long planId) {
    if (archiveBytes == null
        || archiveBytes.length == 0
        || archiveBytes.length > MAX_ARCHIVE_BYTES) {
      throw invalid("O pacote criativo deve ter entre 1 byte e 100 MB.");
    }
    try {
      Map<String, byte[]> entries = readEntries(archiveBytes);
      if (!entries.keySet().containsAll(REQUIRED_METADATA)) {
        throw invalid("O pacote criativo não contém todos os metadados obrigatórios.");
      }
      JsonNode contract = json(entries, "metadata/contract.json");
      JsonNode manifest = json(entries, "metadata/manifest.json");
      JsonNode direction = json(entries, "metadata/iris-direction.json");
      JsonNode apollo = json(entries, "metadata/apollo-storyboard.json");
      JsonNode psique = json(entries, "metadata/psique-review.json");
      JsonNode temis = json(entries, "metadata/temis-review.json");
      JsonNode executions = json(entries, "metadata/agent-executions.json");
      validateAgentAudit(entries, executions);

      require(
          contract.path("product").path("commercialPlanId").asLong(-1) == planId,
          "O pacote pertence a outro plano comercial.");
      require(
          contract
              .path("contractVersion")
              .asText("")
              .equals(manifest.path("contractVersion").asText("")),
          "Contrato e manifesto usam versões diferentes.");
      require(
          "SELECTED".equals(direction.path("decision").asText()),
          "Íris não materializou a rota criativa aprovada.");
      require(
          direction
              .path("chosenRoute")
              .asText("")
              .equals(contract.path("routeDecision").path("selected").asText()),
          "A rota materializada por Íris diverge do contrato.");
      require(
          apollo.path("cuts").isArray() && !apollo.path("cuts").isEmpty(),
          "Apolo não entregou a decupagem obrigatória.");
      requireApprovedReview(psique, "Psique");
      requireApprovedReview(temis, "Têmis");
      require(
          !manifest.path("published").asBoolean(true),
          "O pacote não pode declarar publicação prévia.");
      require(
          !manifest.path("externalMediaProviderCalled").asBoolean(true)
              && manifest.path("externalMediaCostUsd").asDouble(-1) == 0,
          "O pacote declarou provider ou gasto externo de mídia.");
      requireContractFiles(contract, manifest);

      AgentExecution directionExecution = execution(entries, executions, "IRIS");
      AgentExecution apolloExecution = execution(entries, executions, "APOLLO");
      AgentExecution psiqueExecution = execution(entries, executions, "PSIQUE");
      AgentExecution temisExecution = execution(entries, executions, "TEMIS_INDEPENDENT");
      require(
          new HashSet<>(
                      List.of(
                          directionExecution.executionId(),
                          apolloExecution.executionId(),
                          psiqueExecution.executionId(),
                          temisExecution.executionId()))
                  .size()
              == 4,
          "Materialização, audiovisual, percepção e integridade precisam usar execuções independentes.");
      require(
          !manifest.path("producerExecutionId").asText("").equals(temisExecution.executionId()),
          "A produção e a revisão independente de Têmis não podem ser a mesma execução.");

      List<PackageAsset> assets = new ArrayList<>();
      Set<String> contentHashes = new HashSet<>();
      for (JsonNode proof : manifest.path("sourceProofs")) {
        String file = requiredFile(proof, "file");
        String path = "proof/" + file;
        validateHash(entries, path, proof.path("sha256").asText(""));
        require(
            "PRODUCT_PROOF".equals(proof.path("purpose").asText()),
            "Uma prova de produto perdeu a finalidade PRODUCT_PROOF.");
        require(
            contentHashes.add(proof.path("sha256").asText()),
            "O pacote repetiu o mesmo conteúdo visual.");
        assets.add(
            new PackageAsset(
                file,
                path,
                "IMAGE",
                List.of("PRODUCT_PROOF"),
                proof.path("origin").asText("Prova real do produto"),
                proof.path("rightsStatement").asText("Uso autorizado para este produto"),
                proof.path("sha256").asText()));
      }
      for (JsonNode asset : manifest.path("assets")) {
        String file = requiredFile(asset, "file");
        String path = "assets/" + file;
        validateHash(entries, path, asset.path("sha256").asText(""));
        List<String> purposes = strings(asset.path("purposes"));
        require(!purposes.isEmpty(), "Um ativo final não declarou finalidade.");
        String mediaType = asset.path("mediaType").asText();
        require(
            Set.of("IMAGE", "VIDEO").contains(mediaType),
            "Um ativo final declarou tipo de mídia inválido.");
        require(
            purposes.stream().allMatch(Set.of("ADS", "LANDING", "SOCIAL")::contains),
            "Um ativo final declarou finalidade desconhecida.");
        require(
            contentHashes.add(asset.path("sha256").asText()),
            "O pacote repetiu o mesmo conteúdo visual.");
        assets.add(
            new PackageAsset(
                file,
                path,
                mediaType,
                purposes,
                "Compositor determinístico versionado do Marketing Hub",
                "Código, interface e composição próprios; prova sintética sem dados pessoais",
                asset.path("sha256").asText()));
      }
      require(!assets.isEmpty(), "O pacote criativo não contém ativos auditáveis.");
      validateSupplementalEvidence(entries, manifest.path("reviewFrames"), "review-frames/");
      validateSupplementalEvidence(entries, manifest.path("channelPreviews"), "channel-previews/");
      for (JsonNode screenshot : manifest.path("destinationEvidence").path("screenshots")) {
        validateHash(
            entries,
            "proof/" + requiredFile(screenshot, "file"),
            screenshot.path("sha256").asText(""));
      }

      String packageId = sha256(entries.get("metadata/manifest.json"));
      return new ValidatedPackage(
          packageId,
          contract,
          manifest,
          direction,
          apollo,
          psique,
          temis,
          executions,
          directionExecution,
          apolloExecution,
          psiqueExecution,
          temisExecution,
          List.copyOf(assets),
          Map.copyOf(entries));
    } catch (IOException ex) {
      LOGGER.error(
          "Falha ao ler pacote criativo aprovado: planId={}, archiveBytes={}",
          planId,
          archiveBytes.length,
          ex);
      throw invalid("O pacote criativo não pôde ser lido: " + ex.getMessage());
    }
  }

  /** Lê entradas regulares, únicas e limitadas sem aceitar travessia de diretório. */
  private Map<String, byte[]> readEntries(byte[] archiveBytes) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    long expanded = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archiveBytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        String name = entry.getName().replace('\\', '/');
        if (name.startsWith("/") || name.contains("../") || name.equals("..")) {
          throw invalid("O pacote contém caminho inseguro.");
        }
        if (entries.size() >= MAX_ENTRIES || entries.containsKey(name)) {
          throw invalid("O pacote contém entradas demais ou duplicadas.");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = zip.read(buffer)) >= 0) {
          expanded += read;
          if (expanded > MAX_EXPANDED_BYTES) {
            throw invalid("O pacote excede 200 MB após descompactação.");
          }
          output.write(buffer, 0, read);
        }
        entries.put(name, output.toByteArray());
      }
    }
    return entries;
  }

  /** Lê um documento JSON obrigatório. */
  private JsonNode json(Map<String, byte[]> entries, String path) throws IOException {
    return objectMapper.readTree(entries.get(path));
  }

  /** Exige aprovação sem mudanças obrigatórias no parecer importado. */
  private void requireApprovedReview(JsonNode review, String reviewer) {
    require(
        "APPROVED".equals(review.path("decision").asText()), reviewer + " não aprovou o pacote.");
    require(
        review.path("requiredChanges").isArray() && review.path("requiredChanges").isEmpty(),
        reviewer + " deixou mudanças obrigatórias.");
  }

  /** Confere se todos os arquivos congelados no contrato chegaram ao manifesto, sem sobras. */
  private void requireContractFiles(JsonNode contract, JsonNode manifest) {
    Set<String> contractedProofs = new HashSet<>();
    contract.path("sourceProofs").forEach(item -> contractedProofs.add(requiredFile(item, "file")));
    Set<String> manifestedProofs = new HashSet<>();
    manifest.path("sourceProofs").forEach(item -> manifestedProofs.add(requiredFile(item, "file")));
    require(
        !contractedProofs.isEmpty() && contractedProofs.equals(manifestedProofs),
        "As provas do manifesto divergem do contrato.");

    Set<String> contractedAssets = new HashSet<>();
    for (JsonNode format : contract.path("formats")) {
      if (format.path("files").isArray()) {
        format.path("files").forEach(value -> contractedAssets.add(value.asText()));
      }
      if (format.hasNonNull("file")) contractedAssets.add(format.path("file").asText());
    }
    Set<String> manifestedAssets = new HashSet<>();
    manifest.path("assets").forEach(item -> manifestedAssets.add(requiredFile(item, "file")));
    require(
        !contractedAssets.isEmpty() && contractedAssets.equals(manifestedAssets),
        "Os entregáveis do manifesto divergem dos formatos contratados.");
  }

  /** Localiza a última execução íntegra de um agente no ledger local. */
  private AgentExecution execution(Map<String, byte[]> entries, JsonNode executions, String agent) {
    require(executions.isArray(), "O ledger de agentes não é uma lista.");
    JsonNode selected = null;
    for (JsonNode execution : executions) {
      if (agent.equals(execution.path("agent").asText())) selected = execution;
    }
    require(selected != null, "Execução ausente de " + agent + ".");
    require(
        selected.path("exitCode").asInt(-1) == 0
            && selected.path("agentModelCalled").asBoolean(false),
        "A execução de " + agent + " falhou ou não chamou o modelo.");
    require(
        "gpt-5.6-sol".equals(selected.path("model").asText()),
        "A execução de " + agent + " usou modelo não homologado.");
    String reasoningEffort = selected.path("reasoningEffort").asText("").trim();
    require(!reasoningEffort.isEmpty(), "A execução de " + agent + " não registrou o raciocínio.");
    String requestFile = selected.path("requestFile").asText("");
    byte[] promptBytes = entries.get(requestFile);
    require(
        promptBytes != null && promptBytes.length > 0,
        "A execução de " + agent + " não preservou o prompt.");
    String prompt = new String(promptBytes, StandardCharsets.UTF_8);
    byte[] agentPromptBytes = entries.get(selected.path("agentPromptFile").asText(""));
    byte[] activityPromptBytes = entries.get(selected.path("activityPromptFile").asText(""));
    require(
        agentPromptBytes != null && agentPromptBytes.length > 0,
        "A execução de " + agent + " não preservou a parte do agente.");
    require(
        activityPromptBytes != null && activityPromptBytes.length > 0,
        "A execução de " + agent + " não preservou a parte da atividade.");
    String agentPromptPart = new String(agentPromptBytes, StandardCharsets.UTF_8).trim();
    String activityPromptPart = new String(activityPromptBytes, StandardCharsets.UTF_8).trim();
    require(
        prompt.equals(agentPromptPart + "\n\n" + activityPromptPart),
        "A execução de " + agent + " não preservou a composição exata do prompt.");
    JsonNode usage = selected.path("usage");
    require(
        usage.path("input_tokens").canConvertToLong()
            && usage.path("output_tokens").canConvertToLong(),
        "A execução de " + agent + " não possui telemetria de tokens.");
    return new AgentExecution(
        selected.path("executionId").asText(),
        selected.path("model").asText(),
        reasoningEffort,
        prompt,
        agentPromptPart,
        activityPromptPart,
        usage.path("input_tokens").asLong(),
        usage.path("cached_input_tokens").asLong(0),
        usage.path("output_tokens").asLong());
  }

  /** Confere que request, response e log brutos de cada chamada de agente vieram no arquivo. */
  private void validateAgentAudit(Map<String, byte[]> entries, JsonNode executions) {
    require(
        executions.isArray() && !executions.isEmpty(),
        "O ledger de agentes não é uma lista auditável.");
    for (JsonNode execution : executions) {
      for (String field :
          List.of(
              "requestFile", "agentPromptFile", "activityPromptFile", "responseFile", "logFile")) {
        String path = execution.path(field).asText("");
        String hash = execution.path(field + "Sha256").asText("");
        require(
            path.startsWith("audit/") && !path.contains(".."),
            "O ledger aponta para evidência de agente insegura.");
        validateHash(entries, path, hash);
      }
    }
  }

  /** Confere todas as imagens auxiliares usadas pelos revisores. */
  private void validateSupplementalEvidence(
      Map<String, byte[]> entries, JsonNode collection, String directory) {
    require(
        collection.isArray() && !collection.isEmpty(), "Faltam evidências auxiliares de revisão.");
    for (JsonNode item : collection) {
      validateHash(entries, directory + requiredFile(item, "file"), item.path("sha256").asText(""));
    }
  }

  /** Confere a presença e o SHA-256 de um arquivo do pacote. */
  private void validateHash(Map<String, byte[]> entries, String path, String expected) {
    byte[] content = entries.get(path);
    require(content != null && content.length > 0, "Arquivo ausente no pacote: " + path);
    require(
        expected.matches("[a-f0-9]{64}") && expected.equals(sha256(content)),
        "SHA-256 divergente no arquivo " + path + ".");
  }

  /** Lê um nome de arquivo simples sem permitir diretórios. */
  private String requiredFile(JsonNode node, String field) {
    String file = node.path(field).asText("");
    require(
        !file.isBlank() && !file.contains("/") && !file.contains("\\"),
        "Nome de arquivo inválido no manifesto.");
    return file;
  }

  /** Converte uma lista JSON de finalidades em valores textuais únicos. */
  private List<String> strings(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (node.isArray()) node.forEach(value -> values.add(value.asText()));
    return values.stream().filter(value -> !value.isBlank()).distinct().toList();
  }

  /** Calcula o SHA-256 hexadecimal de uma evidência. */
  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", ex);
    }
  }

  /** Interrompe a importação com causa funcional legível pela tela. */
  private void require(boolean condition, String message) {
    if (!condition) throw invalid(message);
  }

  /** Cria uma resposta de validação sem expor detalhes internos. */
  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Representa uma execução local real importada para a trilha auditável. */
  record AgentExecution(
      String executionId,
      String model,
      String reasoningEffort,
      String prompt,
      String agentPromptPart,
      String activityPromptPart,
      long inputTokens,
      long cachedInputTokens,
      long outputTokens) {}

  /** Representa um arquivo visual validado e pronto para armazenamento. */
  record PackageAsset(
      String fileName,
      String archivePath,
      String mediaType,
      List<String> purposes,
      String origin,
      String rightsStatement,
      String sha256) {}

  /** Consolida o pacote já validado sem misturar persistência com leitura do arquivo. */
  record ValidatedPackage(
      String packageId,
      JsonNode contract,
      JsonNode manifest,
      JsonNode direction,
      JsonNode apollo,
      JsonNode psiqueReview,
      JsonNode temisReview,
      JsonNode executions,
      AgentExecution directionExecution,
      AgentExecution apolloExecution,
      AgentExecution psiqueExecution,
      AgentExecution temisExecution,
      List<PackageAsset> assets,
      Map<String, byte[]> entries) {}
}
