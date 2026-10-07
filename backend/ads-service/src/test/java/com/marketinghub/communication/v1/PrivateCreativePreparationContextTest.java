package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: prevenir produção de vídeo ou mudança comercial implícita na preparação. */
class PrivateCreativePreparationContextTest {
  private final ObjectMapper json = new ObjectMapper();
  private final VideoProductionCycleRepository videos = mock(VideoProductionCycleRepository.class);
  private final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  private final PrivateCreativePreparationContext declaration =
      new PrivateCreativePreparationContext(videos, projects, json);

  /** Mantém a mensagem e o limite intactos para duas identidades privadas diferentes. */
  @ParameterizedTest
  @ValueSource(longs = {91010L, 91077L})
  void declaresInternalProofAndBriefWithoutChangingMessage(long productId) {
    var before = input(productId);
    var after = declaration.enrich(reference(productId), before);
    var contract = json.valueToTree(after).path(PrivateCreativePreparationContext.FIELD);
    assertThat(contract.path("nonAudiovisualEvidencePurpose").asText()).isEqualTo("INDEPENDENT_REVIEW");
    assertThat(contract.path("commercialFormatDecisionPreserved").asBoolean()).isTrue();
    assertThat(PrivateCreativePreparationContext.isBriefOnly(contract, reference(productId))).isTrue();
    assertThat(contract.path("videoProductionRequestId").asLong()).isZero();
    assertThat(IrisCommunicationInputFingerprint.hash(json, after))
        .isEqualTo(IrisCommunicationInputFingerprint.hash(json, before));
    assertThat(after.get("product")).isSameAs(before.get("product"));
    verifyNoInteractions(projects);
  }

  /** Pedido da mesma versão mantém produção necessária, inclusive antes de criar experimento. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void retainsExplicitRequestEvenBeforeFinancialApproval(boolean productOnly) {
    var cycle = requested();
    var currentProject = project();
    var currentInput = new LinkedHashMap<>(input(91010L));
    String reference = reference(91010L);
    if (productOnly) {
      currentProject.setExperimentId(null);
      currentInput.remove("experiment");
      currentInput.put("mode", "PRODUCT_PRIVATE");
      reference = "product:91010@agent-validation-v1";
      currentInput.put("sourceReference", reference);
    }
    when(videos.findTopByProductIdAndExperimentIdOrderByCreatedAtDescIdDesc(91010L, productOnly ? null : 92010L))
        .thenReturn(Optional.of(cycle));
    when(projects.findById(93010L)).thenReturn(Optional.of(currentProject));
    var contract = json.valueToTree(declaration.enrich(reference, currentInput))
        .path(PrivateCreativePreparationContext.FIELD);
    assertThat(contract.path("audiovisualProductionIntent").asText()).isEqualTo("GOVERNED_PRODUCTION_REQUESTED");
    assertThat(contract.path("videoProductionRequestId").asLong()).isEqualTo(94010L);
    assertThat(contract.path("spendAuthorized").asBoolean(true)).isFalse();
    assertThat(PrivateCreativePreparationContext.isBriefOnly(contract, reference)).isFalse();
  }

  /** Não transfere pedidos de vídeo antigos, de outro produto ou cancelados para a versão atual. */
  @ParameterizedTest
  @ValueSource(strings = {"VERSION", "PRODUCT", "EXPERIMENT", "CANCELLED", "STOPPED", "NO_REQUESTER", "NO_BUDGET"})
  void ignoresIncompatibleOrUnauthorizedRequest(String mismatch) {
    var cycle = requested();
    var project = project();
    switch (mismatch) {
      case "VERSION" -> project.setCampaignKey("sandbox-private-v0");
      case "PRODUCT" -> project.setProductId(99999L);
      case "EXPERIMENT" -> project.setExperimentId(99999L);
      case "CANCELLED", "STOPPED" -> cycle.setStatus(mismatch);
      case "NO_REQUESTER" -> cycle.setRequestedBy(null);
      case "NO_BUDGET" -> cycle.setBudgetLimitUsd(BigDecimal.ZERO);
      default -> throw new AssertionError("Caso desconhecido.");
    }
    when(videos.findTopByProductIdAndExperimentIdOrderByCreatedAtDescIdDesc(91010L, 92010L))
        .thenReturn(Optional.of(cycle));
    when(projects.findById(93010L)).thenReturn(Optional.of(project));
    var contract = json.valueToTree(declaration.enrich(reference(91010L), input(91010L)))
        .path(PrivateCreativePreparationContext.FIELD);
    assertThat(PrivateCreativePreparationContext.isBriefOnly(contract, reference(91010L))).isTrue();
  }

  /** Falha da consulta gera bloqueio explícito; não assume ausência de pedido governado. */
  @Test
  void blocksUnknownVideoStateWithoutDeclaringOmission() {
    when(videos.findTopByProductIdAndExperimentIdOrderByCreatedAtDescIdDesc(91010L, 92010L))
        .thenThrow(new IllegalStateException("Banco simulado indisponível"));
    var output = declaration.enrich(reference(91010L), input(91010L));
    assertThat(output).containsEntry("inputReadiness", "BLOCKED");
    assertThat(output).doesNotContainKey(PrivateCreativePreparationContext.FIELD);
  }

  /** Mantém o caminho comercial e o privado inválido fora da nova declaração. */
  @ParameterizedTest
  @ValueSource(strings = {"COMMERCIAL", "PUBLICATION", "PAYMENT", "SPEND", "REFERENCE", "BLOCKED"})
  void preservesExistingBoundaries(String boundary) {
    var before = new LinkedHashMap<>(input(91010L));
    switch (boundary) {
      case "COMMERCIAL" -> before.put("mode", "COMMERCIAL");
      case "PUBLICATION" -> before.put("publicationAuthorized", true);
      case "PAYMENT" -> before.put("paymentEnabled", true);
      case "SPEND" -> before.put("externalMediaSpendAuthorized", true);
      case "REFERENCE" -> before.put("sourceReference", "experiment:99999");
      case "BLOCKED" -> before.put("inputReadiness", "BLOCKED");
      default -> throw new AssertionError("Caso desconhecido.");
    }
    assertThat(declaration.enrich(reference(91010L), before)).isSameAs(before);
    verifyNoInteractions(videos, projects);
  }

  /** Exporta a declaração do backend para a validação local do executor real de Íris. */
  @Test
  void exportsBackendContractWhenIntegrationFileIsConfigured() throws Exception {
    String destination = System.getenv("PRIVATE_CREATIVE_IRIS_INPUT_FILE");
    if (destination == null || destination.isBlank()) return;
    var task = json.readTree(Files.readString(Path.of(System.getenv("VEGA_IRIS_INPUT_FILE"))));
    var context = (com.fasterxml.jackson.databind.node.ObjectNode)
        json.readTree(task.path("processContextJson").asText());
    var input = json.convertValue(context.path("communicationMaterializationContext"), Map.class);
    context.set("communicationMaterializationContext",
        json.valueToTree(declaration.enrich(task.path("sourceReference").asText(), input)));
    ((com.fasterxml.jackson.databind.node.ObjectNode) task).put("processContextJson", context.toString());
    Files.writeString(Path.of(destination), task.toPrettyString());
  }

  /** Monta uma entrada privada íntegra com orçamento somente de IA e preço preservado. */
  private Map<String, Object> input(long productId) {
    return Map.ofEntries(
        Map.entry("mode", "LEARNING_CYCLE_PRIVATE"), Map.entry("availability", "AVAILABLE"),
        Map.entry("inputReadiness", "READY"), Map.entry("sourceReference", reference(productId)),
        Map.entry("prototypeVersion", "sandbox-private-v1"),
        Map.entry("product", Map.of("id", productId, "unitPriceBrl", 49)),
        Map.entry("experiment", Map.of("id", productId + 1000)),
        Map.entry("publicationAuthorized", false), Map.entry("paymentEnabled", false),
        Map.entry("externalMediaSpendAuthorized", false));
  }

  /** Calcula uma referência segregada sem identificar registros produtivos. */
  private String reference(long productId) { return "experiment:" + (productId + 1000); }

  /** Simula o registro explícito de vídeo, cuja aprovação financeira continua pendente. */
  private VideoProductionCycle requested() {
    var cycle = new VideoProductionCycle();
    cycle.setId(94010L); cycle.setVideoProjectId(93010L); cycle.setRequestedBy("sandbox-only");
    cycle.setBudgetLimitUsd(BigDecimal.TEN); cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    return cycle;
  }

  /** Liga o pedido governado à mesma versão do produto e do experimento. */
  private VideoProject project() {
    var project = new VideoProject();
    project.setId(93010L); project.setProductId(91010L); project.setExperimentId(92010L);
    project.setCampaignKey("sandbox-private-v1"); return project;
  }
}
