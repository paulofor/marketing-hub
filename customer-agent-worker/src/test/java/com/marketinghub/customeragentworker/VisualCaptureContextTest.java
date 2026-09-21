package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprova que fatos observados sobrevivem ao prompt, reinício e callback sem refazer a revisão.
 */
class VisualCaptureContextTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Mantém identidade e hashes auditáveis sem confundir checkout aberto com venda comprovada. */
  @Test
  void preservesObservedFactsInPromptOutboxAndCallbackForDifferentTasks() throws Exception {
    for (long id : List.of(901L, 1203L)) {
      var task =
          Map.<String, Object>of(
              "taskId",
              id,
              "processCode",
              "quartzo-commercial-preparation-v1",
              "processContextJson",
              "{\"quartzoCommercial\":{\"fingerprint\":\"scope-" + id + "\"}}",
              "sourceReference",
              "experiment:" + id,
              "activityId",
              "humanExperienceReview");
      String session = "capture-" + id;
      var identity =
          new BpmVisualEvidenceRunner.RuntimeIdentity(
              null, null, null, null, null, "a".repeat(64), "b".repeat(64));
      var page =
          new BpmVisualEvidenceRunner.PageFacts(
              1,
              "https://example.com/kit",
              "https://example.com/kit",
              200,
              "Kit",
              Map.of("width", 393),
              List.of("Kit"),
              List.of("Comprar"),
              List.of("Comprar"),
              "Prazo não informado",
              identity,
              "c".repeat(64));
      var bundle =
          new BpmVisualEvidenceRunner.VisualEvidenceBundle(
              new BpmVisualEvidenceRunner.CaptureOutput(
                  session, "IPHONE_15_PRO", List.of(page), List.of()),
              directory);
      var artifact =
          new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
              id + 10,
              session,
              "page-1-full",
              "FULL_PAGE",
              "Página",
              "IPHONE_15_PRO",
              1,
              null,
              393,
              852,
              852,
              0,
              page.requestedUrl(),
              page.finalUrl(),
              "/private/evidence",
              80L,
              "d".repeat(64),
              Instant.parse("2026-01-01T00:00:00Z"),
              "/temporary/only-for-model.png");
      var enriched = CustomerBpmTaskConsumer.withCaptureFacts(task, bundle, List.of(artifact));
      assertThat(task).doesNotContainKey("visualCapture");
      var before = json.readTree(json.writeValueAsString(enriched.get("visualCapture")));
      assertThat(before.path("pages").get(0).path("documentSha256").asText())
          .isEqualTo("c".repeat(64));
      assertThat(before.path("artifacts").get(0).path("sha256").asText()).isEqualTo("d".repeat(64));
      assertThat(before.toString()).doesNotContain("temporary", "localPath");

      var outbox = new CustomerBpmTaskOutbox(directory.resolve("task-" + id), json);
      outbox.save(
          new CustomerBpmTaskOutbox.Pending(
              enriched, Map.of(), List.of(artifact), true, null, null));
      var restored = new CustomerBpmTaskOutbox(directory.resolve("task-" + id), json).read();
      assertThat(
              json.<com.fasterxml.jackson.databind.JsonNode>valueToTree(
                  restored.task().get("visualCapture")))
          .isEqualTo(before);
      var worker =
          new CustomerBpmTaskConsumer(
              "http://127.0.0.1:1",
              "/never-start-a-model",
              "gpt-5.6-sol",
              "max",
              "/missing",
              "/missing",
              json);
      assertThat(worker.prompt(restored.task(), restored.visualEvidence()))
          .contains(
              "documentSha256",
              "a".repeat(64),
              "b".repeat(64),
              "Prazo não informado",
              "não comprova",
              "READ_ONLY_INITIAL_PAGES");
      var callback =
          CustomerBpmTaskConsumer.class.getDeclaredMethod("evidence", Map.class, List.class);
      callback.setAccessible(true);
      var evidence =
          json.readTree(
              (String) callback.invoke(worker, restored.task(), restored.visualEvidence()));
      assertThat(evidence.path("visualCapture")).isEqualTo(before);
      assertThat(evidence.path("quartzoScope").path("fingerprint").asText())
          .isEqualTo("scope-" + id);
    }
  }

  /** Não cria prova fictícia para atividades sem captura visual. */
  @Test
  void preservesTasksWithoutBrowserEvidence() {
    var task = Map.<String, Object>of("taskId", 321L);
    assertThat(CustomerBpmTaskConsumer.withCaptureFacts(task, null, List.of())).isSameAs(task);
  }

  /** Exige checkout da própria tarefa Quartzo e preserva os demais processos sem URL adicional. */
  @Test
  void selectsOnlyOfficialQuartzoCheckoutAndBlocksMissingInput() throws Exception {
    var worker =
        new CustomerBpmTaskConsumer(
            "http://127.0.0.1:1",
            "/never-start-a-model",
            "gpt-5.6-sol",
            "max",
            "/missing",
            "/missing",
            json);
    assertThat(worker.additionalVisualUrls(Map.of("processCode", "landing-approval"))).isEmpty();
    assertThat(
            worker.additionalVisualUrls(
                Map.of(
                    "processCode",
                    "quartzo-commercial-preparation-v1",
                    "processContextJson",
                    "{\"quartzoCommercial\":{\"checkoutUrl\":\"https://example.com/checkout\"}}")))
        .containsExactly("https://example.com/checkout");
    assertThatThrownBy(
            () ->
                worker.additionalVisualUrls(
                    Map.of(
                        "processCode",
                        "quartzo-commercial-preparation-v1",
                        "processContextJson",
                        "{}")))
        .hasMessageContaining("sem checkout oficial");
  }
}
