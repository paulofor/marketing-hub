package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: proteger a cobertura e o confinamento da captura visual de Psique. */
class BpmVisualEvidenceRunnerTest {
  @TempDir Path temporaryDirectory;

  /** Aceita uma página completa e dobras sequenciais produzidas dentro da sessão. */
  @Test
  void acceptsFullPageAndEverySequentialFold() throws Exception {
    Path script = visualScript(1);
    BpmVisualEvidenceRunner runner =
        new BpmVisualEvidenceRunner(
            new ObjectMapper().findAndRegisterModules(), "/bin/sh", script.toString());

    var bundle = runner.capture("https://example.com/jornada", temporaryDirectory.resolve("ok"));

    assertThat(bundle.capture().deviceProfile()).isEqualTo("IPHONE_15_PRO");
    assertThat(bundle.capture().artifacts())
        .extracting(BpmVisualEvidenceRunner.VisualArtifact::evidenceType)
        .containsExactly("FULL_PAGE", "FOLD");
    assertThat(bundle.capture().artifacts().get(1).foldNumber()).isEqualTo(1);
  }

  /** Bloqueia uma sequência cuja primeira dobra foi omitida pelo capturador. */
  @Test
  void rejectsMissingFirstFold() throws Exception {
    Path script = visualScript(2);
    BpmVisualEvidenceRunner runner =
        new BpmVisualEvidenceRunner(
            new ObjectMapper().findAndRegisterModules(), "/bin/sh", script.toString());

    assertThatThrownBy(
            () ->
                runner.capture(
                    "https://example.com/jornada", temporaryDirectory.resolve("missing")))
        .isInstanceOf(BpmVisualEvidenceRunner.VisualEvidenceException.class)
        .hasMessageContaining("Sequência de dobras");
  }

  /** Rejeita rede privada antes de iniciar qualquer subprocesso de browser. */
  @Test
  void rejectsPrivateUrlBeforeBrowser() {
    BpmVisualEvidenceRunner runner =
        new BpmVisualEvidenceRunner(new ObjectMapper(), "/bin/false", "/arquivo-inexistente");

    assertThatThrownBy(
            () ->
                runner.capture(
                    "http://127.0.0.1:8080/jornada", temporaryDirectory.resolve("private")))
        .isInstanceOf(BpmVisualEvidenceRunner.VisualEvidenceException.class)
        .hasMessageContaining("rede privada");
  }

  /** Rejeita token na URL antes de abrir navegador ou produzir snapshot persistível. */
  @Test
  void rejectsCredentialInUrlBeforeBrowser() {
    BpmVisualEvidenceRunner runner =
        new BpmVisualEvidenceRunner(new ObjectMapper(), "/bin/false", "/arquivo-inexistente");

    assertThatThrownBy(
            () ->
                runner.capture(
                    "https://example.com/jornada?access_token=segredo",
                    temporaryDirectory.resolve("credential")))
        .isInstanceOf(BpmVisualEvidenceRunner.VisualEvidenceException.class)
        .hasMessageContaining("credencial");
  }

  /** Cria um capturador determinístico que imita o contrato JSON e os PNGs do Playwright. */
  private Path visualScript(int foldNumber) throws Exception {
    Path script = temporaryDirectory.resolve("visual-" + foldNumber + ".sh");
    Files.writeString(
        script,
        """
        set -eu
        input="$1"
        output="$2"
        evidence="$3"
        mkdir -p "$evidence"
        session=$(sed -n 's/.*"captureSessionId":"\\([^"]*\\)".*/\\1/p' "$input")
        printf '\\211PNG\\r\\n\\032\\n' > "$evidence/full.png"
        printf '\\211PNG\\r\\n\\032\\n' > "$evidence/fold.png"
        printf '{"captureSessionId":"%s","deviceProfile":"IPHONE_15_PRO","pages":[{"pageNumber":1,"requestedUrl":"https://example.com/jornada","finalUrl":"https://example.com/jornada","status":200,"title":"Jornada","viewport":{},"headings":[],"visibleCtas":[]}],"artifacts":[{"captureSessionId":"%s","evidenceKey":"full","evidenceType":"FULL_PAGE","deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":null,"viewportWidth":393,"viewportHeight":852,"pageHeightPx":852,"scrollY":0,"sourceUrl":"https://example.com/jornada","finalUrl":"https://example.com/jornada","capturedAt":"2026-08-29T10:00:00Z","localPath":"%s/full.png"},{"captureSessionId":"%s","evidenceKey":"fold-%s","evidenceType":"FOLD","deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":%s,"viewportWidth":393,"viewportHeight":852,"pageHeightPx":852,"scrollY":0,"sourceUrl":"https://example.com/jornada","finalUrl":"https://example.com/jornada","capturedAt":"2026-08-29T10:00:01Z","localPath":"%s/fold.png"}]}' "$session" "$session" "$evidence" "$session" "FOLD_NUMBER" "FOLD_NUMBER" "$evidence" > "$output"
        """
            .replace("FOLD_NUMBER", Integer.toString(foldNumber)));
    return script;
  }
}
