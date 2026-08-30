package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Responsabilidade: carregar evidências versionadas do PDE para a revisão independente de Têmis.
 */
final class PdeReviewArtifactLoader {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final String COMMUNICATION_CONTRACT_DIRECTORY = "pde-platform/contracts";
  private static final String BUNDLE_INDEX = "commercial-review-bundle-index-v1.json";
  private static final String PROMPT_MODE_FULL = "FULL";
  private static final String PROMPT_MODE_ATTESTED_REFERENCE = "ATTESTED_REFERENCE";
  private static final List<String> COMMERCIAL_EVIDENCE_COLLECTIONS =
      List.of("homologationEvidence", "implementationEvidence", "executableEvidence");
  private static final Pattern MANIFEST_REVISION = Pattern.compile("(?:^|[.-])v([1-9][0-9]*)$");
  private static final List<String> EVIDENCE_COLLECTIONS =
      List.of("implementationEvidence", "executableEvidence");
  private static final List<String> COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS =
      List.of(
          "pde-platform/frontend/src/AssistedServiceApp.tsx",
          "pde-platform/frontend/src/assistedServiceTastingContracts.ts",
          "pde-platform/backend/src/main/java/com/marketinghub/pde/service/AccessService.java",
          "pde-platform/frontend/tests/assisted-service-local.spec.ts",
          "pde-platform/frontend/tests/assisted-service-public-analytics.spec.ts",
          "pde-platform/frontend/playwright.assisted-service-public-analytics.config.ts",
          "pde-platform/frontend/tests/public-health.spec.ts",
          "pde-platform/backend/src/test/java/com/marketinghub/pde/service/AccessServiceTest.java");
  private static final List<String> ARTIFACT_PATHS =
      List.of(
          "pde-platform/contracts/kit-whatsapp-pronto-v1.json",
          "pde-platform/contracts/kit-whatsapp-pronto-commercial-v2.json",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/01-comece-aqui.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/02-roteiro-de-briefing.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/03-biblioteca-de-respostas.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/04-qualificacao-e-followups.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/05-regras-de-escalonamento.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/06-modelo-microentrega-12h.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/07-guia-e-atualizacao.md");

  private final Path repositoryRoot;

  /** Fixa a raiz autorizada e normalizada do repositório local. */
  PdeReviewArtifactLoader(String repositoryPath) {
    repositoryRoot = Path.of(repositoryPath).toAbsolutePath().normalize();
  }

  /** Lê contrato e materiais integrais sem permitir caminho fora do repositório autorizado. */
  List<Map<String, Object>> load() throws IOException {
    List<Map<String, Object>> evidence = new ArrayList<>();
    for (String relativePath : ARTIFACT_PATHS) {
      Path artifact = repositoryRoot.resolve(relativePath).normalize();
      if (!artifact.startsWith(repositoryRoot)) {
        throw new IllegalArgumentException("Artefato PDE fora do repositório autorizado");
      }
      String content = Files.readString(artifact, StandardCharsets.UTF_8);
      evidence.add(
          Map.of(
              "path",
              relativePath,
              "contentLength",
              content.length(),
              "contentChecksum",
              checksum(content),
              "content",
              content));
    }
    return evidence;
  }

  /** Lê todos os contratos comerciais JSON versionados para produtos atuais e futuros. */
  List<Map<String, Object>> loadCommunicationContracts() throws IOException {
    Path contractsDirectory = repositoryRoot.resolve(COMMUNICATION_CONTRACT_DIRECTORY).normalize();
    if (!contractsDirectory.startsWith(repositoryRoot)
        || !Files.isDirectory(contractsDirectory)
        || !contractsDirectory.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Diretório de contratos comerciais PDE não encontrado");
    }
    List<Path> artifacts;
    try (Stream<Path> files = Files.list(contractsDirectory)) {
      artifacts =
          files
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> path.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList();
    }
    Set<Path> currentManifestPaths = currentManifestPaths(artifacts);
    List<Map<String, Object>> evidence = new ArrayList<>();
    for (Path artifact : artifacts) {
      if (!artifact.normalize().startsWith(contractsDirectory)) {
        throw new IllegalArgumentException("Contrato comercial fora do diretório autorizado");
      }
      String content = Files.readString(artifact, StandardCharsets.UTF_8);
      if (currentManifestPaths.contains(artifact)) {
        validateDeclaredEvidenceHashes(content, artifact);
      }
      evidence.add(
          Map.of(
              "path",
              repositoryRoot.relativize(artifact).toString(),
              "contentLength",
              content.length(),
              "contentChecksum",
              checksum(content),
              "content",
              content));
    }
    if (evidence.isEmpty()) {
      throw new IOException("Nenhum contrato comercial PDE versionado foi encontrado");
    }
    for (String relativePath : COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS) {
      evidence.add(readAuthorizedEvidence(relativePath));
    }
    return evidence;
  }

  /** Seleciona por produto somente o manifesto atual que deve coincidir com o código candidato. */
  private Set<Path> currentManifestPaths(List<Path> artifacts) throws IOException {
    Map<String, List<CommunicationManifestCandidate>> candidatesByProduct = new HashMap<>();
    for (Path artifact : artifacts) {
      JsonNode contract = JSON_MAPPER.readTree(Files.readString(artifact, StandardCharsets.UTF_8));
      if (!declaresCommercialEvidence(contract)) continue;
      String productSlug = contract.path("product").path("slug").asText("");
      if (productSlug.isBlank()) productSlug = contract.path("productSlug").asText("");
      if (productSlug.isBlank()) {
        throw new IOException(
            "Manifesto comercial sem produto: " + repositoryRoot.relativize(artifact));
      }
      candidatesByProduct
          .computeIfAbsent(productSlug, ignored -> new ArrayList<>())
          .add(new CommunicationManifestCandidate(artifact, manifestRevision(contract)));
    }
    Set<Path> current = new HashSet<>();
    for (Map.Entry<String, List<CommunicationManifestCandidate>> entry :
        candidatesByProduct.entrySet()) {
      int latestRevision =
          entry.getValue().stream()
              .mapToInt(CommunicationManifestCandidate::revision)
              .max()
              .orElseThrow();
      List<CommunicationManifestCandidate> latest =
          entry.getValue().stream()
              .filter(candidate -> candidate.revision() == latestRevision)
              .toList();
      if (latest.size() != 1) {
        throw new IOException(
            "Mais de um manifesto vigente corresponde ao produto " + entry.getKey());
      }
      current.add(latest.getFirst().path());
    }
    return Set.copyOf(current);
  }

  /** Seleciona o manifesto do alvo e entrega a candidata atual sem misturar produtos. */
  List<Map<String, Object>> loadCommercialHomologationEvidence(Object targetValue)
      throws IOException {
    ReviewTarget target = ReviewTarget.from(targetValue);
    Path contractsDirectory = repositoryRoot.resolve(COMMUNICATION_CONTRACT_DIRECTORY).normalize();
    if (!contractsDirectory.startsWith(repositoryRoot)
        || !Files.isDirectory(contractsDirectory)
        || !contractsDirectory.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Diretório de contratos comerciais PDE não encontrado");
    }
    BundleIndex bundleIndex = bundleIndex();
    List<ManifestCandidate> candidates = new ArrayList<>();
    try (Stream<Path> files = Files.list(contractsDirectory)) {
      for (Path manifest :
          files
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> path.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList()) {
        String manifestPath = repositoryRoot.relativize(manifest).toString();
        Map<String, Object> manifestEvidence = readAuthorizedEvidence(manifestPath, bundleIndex);
        JsonNode contract = JSON_MAPPER.readTree(manifestEvidence.get("content").toString());
        if (declaresCommercialEvidence(contract) && target.matches(contract)) {
          candidates.add(
              new ManifestCandidate(
                  manifestPath, manifestEvidence, contract, manifestRevision(contract)));
        }
      }
    }
    if (candidates.isEmpty()) {
      throw new IOException(
          "Nenhum manifesto de homologação corresponde ao produto " + target.productSlug());
    }
    int latestRevision =
        candidates.stream().mapToInt(ManifestCandidate::revision).max().orElseThrow();
    List<ManifestCandidate> latest =
        candidates.stream().filter(candidate -> candidate.revision() == latestRevision).toList();
    if (latest.size() != 1) {
      throw new IOException(
          "Mais de um manifesto vigente corresponde ao produto " + target.productSlug());
    }

    ManifestCandidate candidate = latest.getFirst();
    Map<String, Map<String, Object>> evidence = new LinkedHashMap<>();
    evidence.put(candidate.path(), candidate.evidence());
    Map<String, String> declaredHashes = new LinkedHashMap<>();
    Map<String, PromptEvidenceDirective> declaredDirectives = new LinkedHashMap<>();
    for (String collectionName : COMMERCIAL_EVIDENCE_COLLECTIONS) {
      JsonNode collection = candidate.contract().path(collectionName);
      if (collection.isMissingNode()) continue;
      if (!collection.isArray()) {
        throw new IOException(
            "Coleção de evidências inválida em " + candidate.path() + ": " + collectionName);
      }
      for (JsonNode declared : collection) {
        String relativePath = declared.path("path").asText("");
        String expectedHash = declared.path("sha256").asText("");
        if (relativePath.isBlank() || expectedHash.isBlank()) {
          throw new IOException("Manifesto comercial contém evidência sem path ou SHA-256");
        }
        String previous = declaredHashes.putIfAbsent(relativePath, expectedHash);
        if (previous != null && !previous.equals(expectedHash)) {
          throw new IOException("Manifesto comercial contém hashes conflitantes: " + relativePath);
        }
        PromptEvidenceDirective directive = PromptEvidenceDirective.from(declared, relativePath);
        PromptEvidenceDirective previousDirective =
            declaredDirectives.putIfAbsent(relativePath, directive);
        if (previousDirective != null && !previousDirective.equals(directive)) {
          throw new IOException(
              "Manifesto comercial contém modos de prompt conflitantes: " + relativePath);
        }
        Map<String, Object> artifact = readAuthorizedEvidence(relativePath, bundleIndex);
        evidence.putIfAbsent(
            relativePath, directive.apply(withBaselineHash(artifact, expectedHash)));
      }
    }
    if (declaredHashes.isEmpty()) {
      throw new IOException("Manifesto comercial sem evidências: " + candidate.path());
    }
    return new ArrayList<>(evidence.values());
  }

  /** Confirma se o JSON representa um manifesto que declara provas de homologação. */
  private boolean declaresCommercialEvidence(JsonNode contract) {
    return COMMERCIAL_EVIDENCE_COLLECTIONS.stream()
        .map(contract::path)
        .anyMatch(collection -> collection.isArray() && !collection.isEmpty());
  }

  /** Resolve a revisão numérica usada para escolher a versão mais recente do manifesto. */
  private int manifestRevision(JsonNode contract) {
    for (String field : List.of("contractVersion", "evidenceVersion")) {
      Matcher matcher = MANIFEST_REVISION.matcher(contract.path(field).asText(""));
      if (matcher.find()) return Integer.parseInt(matcher.group(1));
    }
    return 1;
  }

  /** Expõe a lista fixa apenas para testes de contrato do carregador. */
  static List<String> artifactPaths() {
    return ARTIFACT_PATHS;
  }

  /** Expõe as provas de implementação entregues ao gate apenas para teste de contrato. */
  static List<String> communicationImplementationEvidencePaths() {
    return COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS;
  }

  /** Lê uma prova de implementação somente dentro da raiz autorizada do repositório. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath) throws IOException {
    return readAuthorizedEvidence(relativePath, BundleIndex.unattested());
  }

  /** Lê a prova e confirma que ela pertence ao pacote imutável do mesmo build. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath, BundleIndex bundleIndex)
      throws IOException {
    if (relativePath == null || relativePath.isBlank() || Path.of(relativePath).isAbsolute()) {
      throw new IOException("Caminho de prova PDE inválido");
    }
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
        || !artifact.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Prova de implementação PDE não encontrada: " + relativePath);
    }
    String content = Files.readString(artifact, StandardCharsets.UTF_8);
    String actualHash = sha256(content.getBytes(StandardCharsets.UTF_8));
    bundleIndex.verify(relativePath, actualHash);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("path", relativePath);
    result.put("contentLength", content.length());
    result.put("contentChecksum", checksum(content));
    result.put("sha256", actualHash);
    result.put("content", content);
    result.put("bundleIntegrity", bundleIndex.attested() ? "VERIFIED" : "LOCAL_SOURCE");
    return result;
  }

  /** Registra mudança desde a homologação anterior para reavaliar a candidata atual. */
  private Map<String, Object> withBaselineHash(Map<String, Object> artifact, String expectedHash) {
    Map<String, Object> result = new LinkedHashMap<>(artifact);
    String actualHash = artifact.get("sha256").toString();
    result.put("baselineSha256", expectedHash);
    result.put(
        "baselineIntegrity",
        MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8))
            ? "MATCH"
            : "UPDATED_CANDIDATE");
    return result;
  }

  /** Lê o índice empacotado para distinguir atualização legítima de corrupção posterior. */
  private BundleIndex bundleIndex() throws IOException {
    Path indexPath = repositoryRoot.resolve(BUNDLE_INDEX).normalize();
    if (!Files.isRegularFile(indexPath, LinkOption.NOFOLLOW_LINKS)) {
      return BundleIndex.unattested();
    }
    JsonNode index = JSON_MAPPER.readTree(Files.readString(indexPath, StandardCharsets.UTF_8));
    if (!"pde-commercial-review-evidence-v1".equals(index.path("bundleVersion").asText())) {
      throw new IOException("Índice do pacote comercial possui versão inválida");
    }
    Map<String, String> hashes = new LinkedHashMap<>();
    for (JsonNode file : index.path("files")) {
      String path = file.path("path").asText("");
      String hash = file.path("sha256").asText("");
      if (path.isBlank() || hash.isBlank()) {
        throw new IOException("Índice do pacote comercial contém arquivo sem identidade");
      }
      hashes.put(path, hash);
    }
    return new BundleIndex(Map.copyOf(hashes), true);
  }

  /** Bloqueia a revisão quando um manifesto aponta para uma prova ausente ou alterada. */
  private void validateDeclaredEvidenceHashes(String contractContent, Path contractPath)
      throws IOException {
    JsonNode contract = JSON_MAPPER.readTree(contractContent);
    for (String collectionName : EVIDENCE_COLLECTIONS) {
      JsonNode collection = contract.path(collectionName);
      if (!collection.isMissingNode() && !collection.isArray()) {
        throw new IOException(
            "Coleção de evidências inválida em " + repositoryRoot.relativize(contractPath));
      }
      for (JsonNode evidence : collection) {
        validateDeclaredEvidenceHash(evidence, contractPath);
      }
    }
  }

  /** Confere caminho e SHA-256 de uma prova declarada pelo manifesto de homologação. */
  private void validateDeclaredEvidenceHash(JsonNode evidence, Path contractPath)
      throws IOException {
    String relativePath = evidence.path("path").asText("");
    String expectedHash = evidence.path("sha256").asText("");
    if (relativePath.isBlank() || expectedHash.isBlank()) {
      throw new IOException(
          "Evidência sem path ou sha256 em " + repositoryRoot.relativize(contractPath));
    }
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
        || !artifact.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Prova de homologação fora do repositório ou ausente: " + relativePath);
    }
    String actualHash = sha256(Files.readAllBytes(artifact));
    if (!MessageDigest.isEqual(
        expectedHash.getBytes(StandardCharsets.UTF_8),
        actualHash.getBytes(StandardCharsets.UTF_8))) {
      throw new IOException("SHA-256 divergente para a prova de homologação: " + relativePath);
    }
  }

  /** Calcula o SHA-256 hexadecimal usado pelos manifestos de homologação. */
  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
  }

  /** Calcula checksum determinístico para comprovar qual conteúdo foi revisado. */
  private String checksum(String content) {
    CRC32 checksum = new CRC32();
    checksum.update(content.getBytes(StandardCharsets.UTF_8));
    return Long.toHexString(checksum.getValue());
  }

  /** Representa o manifesto elegível e sua revisão versionada. */
  private record ManifestCandidate(
      String path, Map<String, Object> evidence, JsonNode contract, int revision) {}

  /** Identifica um manifesto geral durante a escolha da revisão atual por produto. */
  private record CommunicationManifestCandidate(Path path, int revision) {}

  /** Define como uma prova atestada entra no prompt sem permitir truncamento silencioso. */
  private record PromptEvidenceDirective(String mode, String reviewSummary) {
    /** Valida o modo explícito e exige resumo verificável quando o conteúdo for redundante. */
    private static PromptEvidenceDirective from(JsonNode declared, String relativePath)
        throws IOException {
      String mode = declared.path("promptMode").asText(PROMPT_MODE_FULL).trim();
      String summary = declared.path("reviewSummary").asText("").trim();
      if (!PROMPT_MODE_FULL.equals(mode) && !PROMPT_MODE_ATTESTED_REFERENCE.equals(mode)) {
        throw new IOException("Modo de prompt inválido para a prova comercial: " + relativePath);
      }
      if (PROMPT_MODE_ATTESTED_REFERENCE.equals(mode) && summary.isBlank()) {
        throw new IOException(
            "Prova comercial referenciada sem resumo verificável: " + relativePath);
      }
      return new PromptEvidenceDirective(mode, summary);
    }

    /** Mantém conteýo integral por padrão ou usa resumo declarado e hash atestado. */
    private Map<String, Object> apply(Map<String, Object> artifact) {
      Map<String, Object> result = new LinkedHashMap<>(artifact);
      result.put("promptMode", mode);
      if (PROMPT_MODE_ATTESTED_REFERENCE.equals(mode)) {
        result.remove("content");
        result.put("reviewSummary", reviewSummary);
      }
      return java.util.Collections.unmodifiableMap(result);
    }
  }

  /** Representa a identidade tipada enviada pelo backend para a atividade reservada. */
  private record ReviewTarget(
      Long experimentId, Long productId, String productSlug, String experienceVersion) {

    /** Converte o contrato HTTP e exige produto e versão explícitos. */
    private static ReviewTarget from(Object value) throws IOException {
      if (!(value instanceof Map<?, ?> map)) {
        throw new IOException("Tarefa de homologação sem alvo comercial tipado");
      }
      Long experimentId = number(map.get("experimentId"));
      Long productId = number(map.get("productId"));
      String productSlug = text(map.get("productSlug"));
      String experienceVersion = text(map.get("experienceVersion"));
      if (productId == null || productSlug == null || experienceVersion == null) {
        throw new IOException("Alvo comercial incompleto para a homologação");
      }
      return new ReviewTarget(experimentId, productId, productSlug, experienceVersion);
    }

    /** Confirma a identidade declarada sem aceitar correspondência apenas por nome livre. */
    private boolean matches(JsonNode contract) {
      JsonNode product = contract.path("product");
      String declaredSlug =
          firstText(product.path("slug").asText(null), contract.path("productSlug").asText(null));
      if (declaredSlug == null || !productSlug.equals(declaredSlug)) return false;
      Long declaredProductId = positiveLong(product.path("id"));
      if (declaredProductId != null && !productId.equals(declaredProductId)) return false;
      Long declaredExperimentId = positiveLong(contract.path("experimentId"));
      if (declaredExperimentId != null && !declaredExperimentId.equals(experimentId)) return false;
      String declaredVersion =
          firstText(
              product.path("experienceVersion").asText(null),
              contract.path("experienceVersion").asText(null));
      return declaredVersion == null || experienceVersion.equals(declaredVersion);
    }

    /** Converte número JSON desserializado sem aceitar zero ou texto ambíguo. */
    private static Long number(Object value) {
      return value instanceof Number number && number.longValue() > 0 ? number.longValue() : null;
    }

    /** Normaliza texto do contrato HTTP. */
    private static String text(Object value) {
      return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    /** Lê identificador positivo opcional de um manifesto JSON. */
    private static Long positiveLong(JsonNode value) {
      return value.isIntegralNumber() && value.longValue() > 0 ? value.longValue() : null;
    }

    /** Escolhe o primeiro texto não vazio preservando a ordem canônica. */
    private static String firstText(String first, String second) {
      if (first != null && !first.isBlank()) return first.trim();
      return second == null || second.isBlank() ? null : second.trim();
    }
  }

  /** Representa os hashes atuais congelados dentro da imagem do executor. */
  private record BundleIndex(Map<String, String> hashes, boolean attested) {
    /** Cria a leitura local usada por testes e execução fora da imagem. */
    private static BundleIndex unattested() {
      return new BundleIndex(Map.of(), false);
    }

    /** Bloqueia alteração posterior ao empacotamento, mas não uma nova candidata legítima. */
    private void verify(String path, String actualHash) throws IOException {
      if (!attested) return;
      String expectedHash = Optional.ofNullable(hashes.get(path)).orElse("");
      if (!MessageDigest.isEqual(
          expectedHash.getBytes(StandardCharsets.UTF_8),
          actualHash.getBytes(StandardCharsets.UTF_8))) {
        throw new IOException("SHA-256 divergente dentro do pacote comercial: " + path);
      }
    }
  }
}
