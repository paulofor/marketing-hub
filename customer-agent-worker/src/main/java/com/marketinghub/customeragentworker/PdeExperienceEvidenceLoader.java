package com.marketinghub.customeragentworker;

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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/** Responsabilidade: carregar a experiência versionada que Psique deve revisar como cliente. */
final class PdeExperienceEvidenceLoader {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String CONTRACT_DIRECTORY = "pde-platform/contracts";
  private static final String BUNDLE_INDEX = "commercial-review-bundle-index-v1.json";
  private static final String PROMPT_MODE_FULL = "FULL";
  private static final String PROMPT_MODE_ATTESTED_REFERENCE = "ATTESTED_REFERENCE";
  private static final List<String> COMMERCIAL_EVIDENCE_COLLECTIONS =
      List.of("homologationEvidence", "implementationEvidence", "executableEvidence");
  private static final Pattern MANIFEST_REVISION = Pattern.compile("(?:^|[.-])v([1-9][0-9]*)$");
  private static final Pattern SHA256 = Pattern.compile("^[a-f0-9]{64}$");
  private static final List<String> EVIDENCE_PATHS =
      List.of(
          "pde-platform/contracts/kit-whatsapp-pronto-v1.json",
          "pde-platform/contracts/kit-whatsapp-pronto-commercial-v2.json",
          "pde-platform/frontend/src/AssistedServiceApp.tsx",
          "pde-platform/frontend/tests/assisted-service-local.spec.ts",
          "docs/homologacao/pde-kit-whatsapp-construcao-v1.md");

  private final Path repositoryRoot;

  /** Fixa a raiz autorizada e normalizada do repositório local. */
  PdeExperienceEvidenceLoader(String repositoryPath) {
    repositoryRoot = Path.of(repositoryPath).toAbsolutePath().normalize();
  }

  /** Lê contrato, experiência e homologação sem permitir caminho fora da raiz autorizada. */
  List<Map<String, Object>> load() throws IOException {
    List<Map<String, Object>> evidence = new ArrayList<>();
    for (String relativePath : EVIDENCE_PATHS) {
      Path artifact = repositoryRoot.resolve(relativePath).normalize();
      if (!artifact.startsWith(repositoryRoot)) {
        throw new IllegalArgumentException("Evidência PDE fora do repositório autorizado");
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

  /** Seleciona o manifesto do alvo e carrega a candidata atual sem misturar outros produtos. */
  List<Map<String, Object>> loadCommercialHomologationEvidence(Object targetValue)
      throws IOException {
    ReviewTarget target = ReviewTarget.from(targetValue);
    Path directory = repositoryRoot.resolve(CONTRACT_DIRECTORY).normalize();
    if (!directory.startsWith(repositoryRoot)
        || !Files.isDirectory(directory)
        || !directory.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Diretório de contratos PDE não encontrado");
    }
    BundleIndex bundleIndex = bundleIndex();
    List<ManifestCandidate> candidates = new ArrayList<>();
    try (Stream<Path> files = Files.list(directory)) {
      for (Path manifest :
          files
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> path.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList()) {
        String manifestPath = repositoryRoot.relativize(manifest).toString();
        Map<String, Object> manifestEvidence = readAuthorizedEvidence(manifestPath, bundleIndex);
        JsonNode contract = JSON.readTree(manifestEvidence.get("content").toString());
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

  /** Lê os sinais visuais mínimos que a superfície publicada deve exibir antes da revisão paga. */
  LiveVisualContract loadLiveVisualContract(Object targetValue) throws IOException {
    List<Map<String, Object>> evidence = loadCommercialHomologationEvidence(targetValue);
    if (evidence.isEmpty()) return LiveVisualContract.none();
    JsonNode manifest = JSON.readTree(evidence.getFirst().get("content").toString());
    JsonNode contract = manifest.path("liveVisualContract");
    if (contract.isMissingNode() || contract.isNull()) return LiveVisualContract.none();
    if (!contract.isObject()) {
      throw new IOException("Contrato visual ao vivo inválido no manifesto comercial");
    }
    return new LiveVisualContract(
        requiredTextValues(contract, "requiredFirstFoldCtas"),
        requiredTextValues(contract, "requiredVisibleTexts"),
        runtimeIdentity(contract),
        "");
  }

  /** Lê a identidade imutável que deve estar exposta pelo artefato público antes da revisão. */
  private RuntimeIdentity runtimeIdentity(JsonNode contract) throws IOException {
    JsonNode identity = contract.path("runtimeIdentity");
    if (identity.isMissingNode() || identity.isNull()) return RuntimeIdentity.none();
    if (!identity.isObject()) {
      throw new IOException("Identidade de runtime inválida no contrato visual ao vivo");
    }
    String version = identity.path("version").asText("").trim();
    String experienceVersion = identity.path("experienceVersion").asText("").trim();
    String frontendSourceSha256 = identity.path("frontendSourceSha256").asText("").trim();
    if (version.isBlank()
        || experienceVersion.isBlank()
        || !SHA256.matcher(frontendSourceSha256).matches()) {
      throw new IOException("Identidade de runtime incompleta no contrato visual ao vivo");
    }
    return new RuntimeIdentity(version, experienceVersion, frontendSourceSha256);
  }

  /** Valida uma lista de sinais obrigatórios sem aceitar valores vazios ou estrutura ambígua. */
  private List<String> requiredTextValues(JsonNode contract, String field) throws IOException {
    JsonNode values = contract.path(field);
    if (values.isMissingNode()) return List.of();
    if (!values.isArray()) {
      throw new IOException("Campo visual ao vivo inválido: " + field);
    }
    List<String> result = new ArrayList<>();
    for (JsonNode value : values) {
      String text = value.asText("").trim();
      if (text.isBlank()) throw new IOException("Sinal visual ao vivo vazio: " + field);
      result.add(text);
    }
    return List.copyOf(result);
  }

  /** Confirma se o JSON representa um manifesto que declara provas de homologação. */
  private boolean declaresCommercialEvidence(JsonNode contract) {
    return COMMERCIAL_EVIDENCE_COLLECTIONS.stream()
        .map(contract::path)
        .anyMatch(collection -> collection.isArray() && !collection.isEmpty());
  }

  /** Resolve a revisão numérica usada para escolher a versão mais recente do mesmo manifesto. */
  private int manifestRevision(JsonNode contract) {
    for (String field : List.of("contractVersion", "evidenceVersion")) {
      Matcher matcher = MANIFEST_REVISION.matcher(contract.path(field).asText(""));
      if (matcher.find()) return Integer.parseInt(matcher.group(1));
    }
    return 1;
  }

  /** Lê uma evidência regular sem permitir caminho absoluto, link ou saída da raiz. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath) throws IOException {
    return readAuthorizedEvidence(relativePath, BundleIndex.unattested());
  }

  /** Lê uma prova e confirma que ela pertence ao pacote imutável produzido no mesmo build. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath, BundleIndex bundleIndex)
      throws IOException {
    if (relativePath == null || relativePath.isBlank() || Path.of(relativePath).isAbsolute()) {
      throw new IOException("Caminho de evidência comercial inválido");
    }
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
        || !artifact.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Evidência comercial fora do repositório ou ausente: " + relativePath);
    }
    String content = Files.readString(artifact, StandardCharsets.UTF_8);
    String actualHash = sha256(content.getBytes(StandardCharsets.UTF_8));
    bundleIndex.verify(relativePath, actualHash);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("path", relativePath);
    result.put("contentLength", content.length());
    result.put("contentChecksum", checksum(content));
    result.put("content", content);
    result.put("sha256", actualHash);
    result.put("bundleIntegrity", bundleIndex.attested() ? "VERIFIED" : "LOCAL_SOURCE");
    return result;
  }

  /** Registra mudança desde a homologação anterior para que a candidata atual seja reavaliada. */
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

  /** Lê o índice da imagem para distinguir atualização legítima de pacote corrompido. */
  private BundleIndex bundleIndex() throws IOException {
    Path indexPath = repositoryRoot.resolve(BUNDLE_INDEX).normalize();
    if (!Files.isRegularFile(indexPath, LinkOption.NOFOLLOW_LINKS)) {
      return BundleIndex.unattested();
    }
    JsonNode index = JSON.readTree(Files.readString(indexPath, StandardCharsets.UTF_8));
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

  /** Calcula SHA-256 hexadecimal para congelar a versão revisada por Psique. */
  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
  }

  /** Expõe a lista fixa apenas para testes de contrato do carregador. */
  static List<String> evidencePaths() {
    return EVIDENCE_PATHS;
  }

  /** Calcula checksum determinístico para provar a versão efetivamente avaliada. */
  private String checksum(String content) {
    CRC32 checksum = new CRC32();
    checksum.update(content.getBytes(StandardCharsets.UTF_8));
    return Long.toHexString(checksum.getValue());
  }

  /** Representa o manifesto elegível e sua revisão versionada. */
  private record ManifestCandidate(
      String path, Map<String, Object> evidence, JsonNode contract, int revision) {}

  /** Congela a copy mínima que distingue a candidata homologada de uma superfície anterior. */
  record LiveVisualContract(
      List<String> requiredFirstFoldCtas,
      List<String> requiredVisibleTexts,
      RuntimeIdentity runtimeIdentity,
      String publicationSourceSha256) {
    /** Representa manifestos históricos que ainda não declaravam sinais visuais ao vivo. */
    static LiveVisualContract none() {
      return new LiveVisualContract(List.of(), List.of(), RuntimeIdentity.none(), "");
    }

    /** Vincula uma página publicada ao hash e à CTA auditados pelo backend. */
    static LiveVisualContract publishedPage(String publicationSourceSha256, String firstFoldCta) {
      if (!SHA256.matcher(Optional.ofNullable(publicationSourceSha256).orElse("")).matches()) {
        throw new IllegalArgumentException("Hash da página publicada inválido.");
      }
      if (firstFoldCta == null || firstFoldCta.isBlank()) {
        throw new IllegalArgumentException("CTA da página publicada ausente.");
      }
      return new LiveVisualContract(
          List.of(firstFoldCta.trim()), List.of(), RuntimeIdentity.none(), publicationSourceSha256);
    }

    /** Informa se existe alguma condição visual que precisa ser confrontada com o navegador. */
    boolean required() {
      return !requiredFirstFoldCtas.isEmpty()
          || !requiredVisibleTexts.isEmpty()
          || runtimeIdentity.required()
          || !publicationSourceSha256.isBlank();
    }
  }

  /** Identifica exatamente o código-fonte do frontend que a superfície pública deve servir. */
  record RuntimeIdentity(String version, String experienceVersion, String frontendSourceSha256) {
    /** Representa manifestos históricos sem identidade pública declarada. */
    static RuntimeIdentity none() {
      return new RuntimeIdentity("", "", "");
    }

    /** Informa se a revisão exige correspondência exata com o diagnóstico público. */
    boolean required() {
      return !frontendSourceSha256.isBlank();
    }
  }

  /** Define como uma prova atestada entra no prompt sem truncamento silencioso. */
  private record PromptEvidenceDirective(String mode, String reviewSummary) {
    /** Valida o modo explícito e exige resumo verificável para referência sem conteúdo integral. */
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

    /** Mantém conteúdo integral ou entrega resumo, tamanho e hashes do arquivo atestado. */
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
