package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Responsabilidade: capturar full-page e todas as dobras mobile antes do parecer de Psique. */
@Component
public class BpmVisualEvidenceRunner {
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
  private static final Set<String> SENSITIVE_QUERY_PARAMETERS =
      Set.of(
          "accesstoken",
          "apikey",
          "authorization",
          "credential",
          "idtoken",
          "jwt",
          "password",
          "refreshtoken",
          "secret",
          "session",
          "signature",
          "token");
  private final ObjectMapper json;
  private final String nodeBinary;
  private final String scriptPath;

  /** Inicializa o executor Playwright versionado e seus binários configuráveis. */
  public BpmVisualEvidenceRunner(
      ObjectMapper json,
      @Value("${CUSTOMER_AGENT_NODE_BIN:node}") String nodeBinary,
      @Value("${CUSTOMER_AGENT_BPM_VISUAL_SCRIPT:/app/browser/bpm-visual-evidence.mjs}")
          String scriptPath) {
    this.json = json;
    this.nodeBinary = nodeBinary;
    this.scriptPath = scriptPath;
  }

  /** Abre a URL pública em iPhone 15 Pro e valida a cobertura contínua das dobras. */
  VisualEvidenceBundle capture(String sourceUrl, Path workDirectory) throws Exception {
    validatePublicUrl(sourceUrl);
    Files.createDirectories(workDirectory);
    Path input = workDirectory.resolve("visual-input.json");
    Path output = workDirectory.resolve("visual-output.json");
    Path evidenceDirectory = workDirectory.resolve("visual-evidence");
    String captureSessionId = UUID.randomUUID().toString();
    Files.writeString(
        input,
        json.writeValueAsString(
            java.util.Map.of("sourceUrl", sourceUrl, "captureSessionId", captureSessionId)),
        StandardCharsets.UTF_8);
    Process process =
        new ProcessBuilder(
                nodeBinary,
                scriptPath,
                input.toString(),
                output.toString(),
                evidenceDirectory.toString())
            .redirectErrorStream(true)
            .redirectOutput(workDirectory.resolve("visual-browser.log").toFile())
            .start();
    if (!process.waitFor(3, TimeUnit.MINUTES)) {
      process.destroyForcibly();
      throw new VisualEvidenceException("Timeout ao capturar as dobras mobile de Psique.");
    }
    if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
      throw new VisualEvidenceException(
          "Falha ao capturar as dobras mobile: "
              + Files.readString(
                  workDirectory.resolve("visual-browser.log"), StandardCharsets.UTF_8));
    }
    CaptureOutput capture = json.readValue(output.toFile(), CaptureOutput.class);
    validateCapture(captureSessionId, evidenceDirectory, capture);
    return new VisualEvidenceBundle(capture, workDirectory);
  }

  /** Confirma sessão, full-page, sequência de dobras e arquivos dentro do diretório autorizado. */
  private void validateCapture(
      String expectedSession, Path evidenceDirectory, CaptureOutput capture) throws Exception {
    if (capture == null
        || !expectedSession.equals(capture.captureSessionId())
        || !"IPHONE_15_PRO".equals(capture.deviceProfile())
        || capture.pages() == null
        || capture.pages().size() != 1
        || capture.artifacts() == null
        || capture.artifacts().isEmpty()) {
      throw new VisualEvidenceException("Contrato da captura visual de Psique está incompleto.");
    }
    List<VisualArtifact> fullPages =
        capture.artifacts().stream()
            .filter(artifact -> "FULL_PAGE".equals(artifact.evidenceType()))
            .toList();
    List<VisualArtifact> folds =
        capture.artifacts().stream()
            .filter(artifact -> "FOLD".equals(artifact.evidenceType()))
            .sorted(java.util.Comparator.comparing(VisualArtifact::foldNumber))
            .toList();
    if (fullPages.size() != 1 || folds.isEmpty()) {
      throw new VisualEvidenceException("Captura exige full-page e ao menos uma dobra mobile.");
    }
    VisualArtifact fullPage = fullPages.getFirst();
    if (fullPage.pageNumber() == null
        || fullPage.pageNumber() != 1
        || fullPage.foldNumber() != null
        || fullPage.scrollY() == null
        || fullPage.scrollY() != 0
        || fullPage.viewportWidth() == null
        || fullPage.viewportWidth() < 1
        || fullPage.viewportHeight() == null
        || fullPage.viewportHeight() < 1
        || fullPage.pageHeightPx() == null
        || fullPage.pageHeightPx() < fullPage.viewportHeight()) {
      throw new VisualEvidenceException("Metadados da captura full-page estão inválidos.");
    }
    List<Integer> expectedPositions = new ArrayList<>();
    int maxScroll = Math.max(0, fullPage.pageHeightPx() - fullPage.viewportHeight());
    for (int position = 0; position <= maxScroll; position += fullPage.viewportHeight()) {
      expectedPositions.add(position);
    }
    if (expectedPositions.getLast() != maxScroll) expectedPositions.add(maxScroll);
    if (folds.size() != expectedPositions.size()) {
      throw new VisualEvidenceException("Quantidade de dobras mobile está incompleta.");
    }
    for (int index = 0; index < folds.size(); index++) {
      VisualArtifact fold = folds.get(index);
      if (fold.foldNumber() == null
          || fold.foldNumber() != index + 1
          || !Objects.equals(fold.scrollY(), expectedPositions.get(index))
          || !Objects.equals(fold.pageNumber(), fullPage.pageNumber())
          || !Objects.equals(fold.viewportWidth(), fullPage.viewportWidth())
          || !Objects.equals(fold.viewportHeight(), fullPage.viewportHeight())
          || !Objects.equals(fold.pageHeightPx(), fullPage.pageHeightPx())
          || !Objects.equals(fold.sourceUrl(), fullPage.sourceUrl())
          || !Objects.equals(fold.finalUrl(), fullPage.finalUrl())) {
        throw new VisualEvidenceException("Sequência de dobras mobile está incompleta.");
      }
    }
    Path realEvidenceDirectory = evidenceDirectory.toRealPath();
    Set<Path> artifactFiles = new LinkedHashSet<>();
    Set<String> evidenceKeys = new LinkedHashSet<>();
    for (VisualArtifact artifact : capture.artifacts()) {
      Path file = Path.of(artifact.localPath()).toAbsolutePath().normalize();
      if (!expectedSession.equals(artifact.captureSessionId())
          || !"IPHONE_15_PRO".equals(artifact.deviceProfile())
          || !artifactFiles.add(file)
          || artifact.evidenceKey() == null
          || !evidenceKeys.add(artifact.evidenceKey())
          || !file.startsWith(evidenceDirectory.toAbsolutePath().normalize())
          || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
          || !file.toRealPath().startsWith(realEvidenceDirectory)
          || Files.size(file) < PNG_SIGNATURE.length
          || !pngSignature(file)) {
        throw new VisualEvidenceException("Arquivo visual ausente ou fora da sessão autorizada.");
      }
    }
  }

  /** Confirma a assinatura PNG sem carregar o snapshot inteiro na memória do worker. */
  private boolean pngSignature(Path file) throws Exception {
    try (var input = Files.newInputStream(file)) {
      return Arrays.equals(PNG_SIGNATURE, input.readNBytes(PNG_SIGNATURE.length));
    }
  }

  /** Bloqueia esquema, credencial e endereço privado antes de iniciar o Chromium. */
  private void validatePublicUrl(String value) throws Exception {
    URI uri = URI.create(value == null ? "" : value.trim());
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null) {
      throw new VisualEvidenceException("Psique não recebeu uma URL pública visual válida.");
    }
    if (uri.getRawQuery() != null) {
      for (String part : uri.getRawQuery().split("&")) {
        String name = part.contains("=") ? part.substring(0, part.indexOf('=')) : part;
        String normalized =
            URLDecoder.decode(name, StandardCharsets.UTF_8)
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        if (SENSITIVE_QUERY_PARAMETERS.contains(normalized)) {
          throw new VisualEvidenceException(
              "Psique não pode capturar uma URL que contenha credencial.");
        }
      }
    }
    for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
      byte[] bytes = address.getAddress();
      boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()
          || uniqueLocalIpv6) {
        throw new VisualEvidenceException("Psique não pode capturar uma URL de rede privada.");
      }
    }
  }

  /** Representa a captura inteira e seu diretório temporário ainda disponível ao modelo. */
  record VisualEvidenceBundle(CaptureOutput capture, Path workDirectory) {}

  /** Representa o contrato determinístico produzido pelo Playwright. */
  record CaptureOutput(
      String captureSessionId,
      String deviceProfile,
      List<PageFacts> pages,
      List<VisualArtifact> artifacts) {}

  /** Registra fatos técnicos da página sem substituir a interpretação estética. */
  record PageFacts(
      Integer pageNumber,
      String requestedUrl,
      String finalUrl,
      Integer status,
      String title,
      java.util.Map<String, Object> viewport,
      List<String> headings,
      List<String> visibleCtas) {}

  /** Descreve um arquivo local e todos os metadados que serão persistidos no backend. */
  record VisualArtifact(
      String captureSessionId,
      String evidenceKey,
      String evidenceType,
      String deviceProfile,
      Integer pageNumber,
      Integer foldNumber,
      Integer viewportWidth,
      Integer viewportHeight,
      Integer pageHeightPx,
      Integer scrollY,
      String sourceUrl,
      String finalUrl,
      Instant capturedAt,
      String localPath) {}

  /** Distingue ausência de prova visual de uma falha posterior do modelo. */
  static final class VisualEvidenceException extends IllegalStateException {
    /** Cria o bloqueio explícito com a causa que a pessoa operadora deve corrigir. */
    VisualEvidenceException(String message) {
      super(message);
    }

    /** Preserva a causa técnica completa quando a captura ou persistência falha. */
    VisualEvidenceException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
