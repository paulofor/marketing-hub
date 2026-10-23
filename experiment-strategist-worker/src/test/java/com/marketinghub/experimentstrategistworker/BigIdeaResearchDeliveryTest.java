package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprova pesquisa isolada, recibo, recuperação sem modelo e validações entre produtos. */
class BigIdeaResearchDeliveryTest {
  @TempDir Path temp;
  final ObjectMapper json = new ObjectMapper();

  /** Configura processo sintético com os mesmos argumentos do modelo e armazenamento local. */
  private WorkerProperties properties(Path directory) {
    var properties = new WorkerProperties();
    properties.setCodexCommand(
        Path.of("src/test/resources/research/big-idea-codex.mjs").toAbsolutePath().toString());
    properties.setRepositoryPath(Path.of(".").toAbsolutePath().toString());
    properties.setBackendUrl("http://127.0.0.1:1");
    properties.setBpmStateDirectory(directory.toString());
    properties.setModel("gpt-6-astra");
    return properties;
  }

  /** Cria contexto sintético sem credenciais, autorizações comerciais ou rede. */
  private StrategistJob job(long id) {
    return new StrategistJob(
        id,
        id + 100,
        "RUNNING",
        BigIdeaResearchContract.MODE,
        "Pesquisa local sintética",
        "{\"bigIdeaResearch\":{\"productId\":" + id + "}}");
  }

  /**
   * Percorre processo real, perde callback e reutiliza bytes após reinício para duas identidades.
   */
  @Test
  void recoversRealLocalProcessWithoutAnotherInference() throws Exception {
    for (long id : new long[] {99101, 99288}) {
      Path directory = temp.resolve("job-" + id);
      var properties = properties(directory);
      var runner = spy(new CodexStrategistRunner(properties, json));
      var backend = mock(StrategistBackendClient.class);
      doThrow(new IllegalStateException("resposta perdida"))
          .doNothing()
          .when(backend)
          .complete(eq(id), any());
      var delivery = new BigIdeaResearchDelivery(backend, runner, properties, json);
      delivery.start(job(id));
      assertThat(directory.resolve("big-idea-research/pending.json")).exists();
      assertThat(new BigIdeaResearchDelivery(backend, runner, properties, json).replay()).isTrue();
      verify(runner, times(1)).run(eq(job(id)), any(), any());
      var callback = org.mockito.ArgumentCaptor.forClass(Map.class);
      verify(backend, times(2)).complete(eq(id), callback.capture());
      assertThat(json.writeValueAsString(callback.getAllValues().get(0)))
          .isEqualTo(json.writeValueAsString(callback.getAllValues().get(1)));
      assertThat(((Number) callback.getValue().get("inputTokens")).longValue()).isEqualTo(240L);
      assertThat(((Number) callback.getValue().get("outputTokens")).longValue()).isEqualTo(130L);
      assertThat(directory.resolve("big-idea-research/receipts/" + id + "/model-prompt.md"))
          .exists();
      assertThat(directory.resolve("big-idea-research/pending.json")).doesNotExist();
      assertThat(delivery.replay()).isFalse();
      verify(backend, never()).fail(any(), any());
    }
  }

  /**
   * Um processo interrompido sem saída não pode ser interpretado como permissão para consumir
   * novamente.
   */
  @Test
  void interruptionWithoutReceiptFailsWithoutInference() throws Exception {
    var properties = properties(temp);
    var runner = mock(CodexStrategistRunner.class);
    var backend = mock(StrategistBackendClient.class);
    Files.createDirectories(temp.resolve("big-idea-research"));
    var store = new PdeMarketStrategyOutbox(temp.resolve("big-idea-research"), json);
    store.save(
        new PdeMarketStrategyOutbox.Pending(
            json.convertValue(job(99101), Map.class), Map.of(), true, "MODEL", null));
    assertThat(new BigIdeaResearchDelivery(backend, runner, properties, json).replay()).isTrue();
    verifyNoInteractions(runner);
    verify(backend).fail(eq(99101L), contains("não repetir inferência"));
  }

  /** Reconstrói callback após interrupção entre a saída do modelo e a gravação do recibo. */
  @Test
  void recoversCompletedOutputAfterProcessCrash() throws Exception {
    var properties = properties(temp);
    var runner = spy(new CodexStrategistRunner(properties, json));
    var backend = mock(StrategistBackendClient.class);
    Path directory = temp.resolve("big-idea-research");
    Files.createDirectories(directory);
    var store = new PdeMarketStrategyOutbox(directory, json);
    store.save(
        new PdeMarketStrategyOutbox.Pending(
            json.convertValue(job(99101), Map.class), Map.of(), true, "MODEL", null));
    Files.copy(Path.of("../infra/testing/product-big-idea/research-result.json"), store.output());
    Files.writeString(
        store.events(),
        "{\"usage\":{\"input_tokens\":240,\"cached_input_tokens\":40,\"output_tokens\":130}}\n");
    assertThat(new BigIdeaResearchDelivery(backend, runner, properties, json).replay()).isTrue();
    verify(runner, never()).run(any(), any(), any());
    verify(backend)
        .complete(
            eq(99101L),
            argThat(value -> value.get("rawModelResponse").toString().contains("ocasião")));
    verify(backend, never()).fail(any(), any());
    assertThat(directory.resolve("receipts/99101/model-result.json")).exists();
  }

  /** Rejeita respostas cruzadas, certeza comercial inventada e evidências incompletas. */
  @Test
  void rejectsCrossProductAndIncompleteDemandEvidence() throws Exception {
    var original =
        json.readTree(Path.of("../infra/testing/product-big-idea/research-result.json").toFile());
    var input = json.readTree(job(99101).evidenceSnapshot());
    BigIdeaResearchContract.validate(original, input);
    for (String fault : List.of("identity", "certainty", "mechanism", "source", "alternatives")) {
      var result = original.deepCopy();
      switch (fault) {
        case "identity" ->
            ((com.fasterxml.jackson.databind.node.ObjectNode) result).put("productId", 99288);
        case "certainty" ->
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("demandAssessment"))
                .put("status", "PROVEN");
        case "mechanism" ->
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("bigIdea"))
                .put("mechanism", " ");
        case "source" ->
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("sources").get(1))
                .put("evidenceClass", "INDUSTRY_RESEARCH");
        default ->
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("alternatives").get(0))
                .remove("risk");
      }
      assertThatThrownBy(() -> BigIdeaResearchContract.validate(result, input))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
