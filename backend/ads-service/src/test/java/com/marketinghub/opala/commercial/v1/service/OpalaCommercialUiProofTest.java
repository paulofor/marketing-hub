package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.communication.v1.PrivateCommunicationCreativeProof;
import com.marketinghub.experiment.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Responsabilidade: impedir que pixels privados, outra identidade ou consumo externo virem prova
 * comercial.
 */
class OpalaCommercialUiProofTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final PrivateCommunicationCreativeProof privateProofs =
      mock(PrivateCommunicationCreativeProof.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskVisualEvidenceRepository artifacts =
      mock(AgentTaskVisualEvidenceRepository.class);
  private final AgentTaskVisualEvidenceService visuals = mock(AgentTaskVisualEvidenceService.class);
  private final OpalaCommercialUiProof service =
      new OpalaCommercialUiProof(instances, privateProofs, tasks, artifacts, visuals, json);

  /**
   * Confere os mesmos critérios em Vega e outra identidade sem depender de nomes ou IDs especiais.
   */
  @ParameterizedTest
  @CsvSource({"4,106,13", "407,922,71"})
  void acceptsCurrentLocalProofAndPreservesIndependentReview(
      long productId, long experimentId, long cycleId) throws Exception {
    var fixture = fixture(productId, experimentId, cycleId);
    var verified =
        service.validate(
            fixture.scope(),
            fixture.candidate(),
            fixture.task(),
            (ObjectNode) json.readTree(fixture.receipt().toString()),
            fixture.input().path("copy"));
    assertThat(verified.sha256()).isEqualTo(fixture.receipt().path("sha256").asText());
    assertThat(verified.input().path("copySourceTaskId").asLong()).isEqualTo(788L);
    assertThat(fixture.scope().experiment().getStatus()).isEqualTo(ExperimentStatus.PLANNED);
  }

  /** Recusa arquivo alterado, custo externo, ausência de origem e prova de outra versão. */
  @Test
  void rejectsChangedBytesPaidProviderAndCrossVersion() throws Exception {
    var f = fixture(4, 106, 13);
    var original = f.receipt().deepCopy();
    for (var change :
        java.util.List.of(
            "productVersion", "rendererSha256", "costUsd", "externalImagesIncluded")) {
      var receipt = original.deepCopy();
      if ("costUsd".equals(change)) receipt.put(change, 1);
      else if ("externalImagesIncluded".equals(change)) receipt.put(change, true);
      else receipt.put(change, "divergente");
      assertThatThrownBy(
              () ->
                  service.validate(
                      f.scope(), f.candidate(), f.task(), receipt, f.input().path("copy")))
          .hasMessageContaining("comprovar");
    }
    for (var invalidId :
        java.util.List.of(
            json.getNodeFactory().textNode("4"),
            json.getNodeFactory().numberNode(4.0),
            json.getNodeFactory().numberNode(new java.math.BigInteger("18446744073709551620")))) {
      var changed = original.deepCopy();
      changed.set("productId", invalidId);
      assertThatThrownBy(
              () ->
                  service.validate(
                      f.scope(), f.candidate(), f.task(), changed, f.input().path("copy")))
          .hasRootCauseMessage("A prova comercial diverge da entrada: productId");
    }
    var redirected =
        original
            .deepCopy()
            .put("inspectedUrl", "https://another.sandbox.local/?mh_preview=qa&pde_analytics=off");
    assertThatThrownBy(
            () ->
                service.validate(
                    f.scope(), f.candidate(), f.task(), redirected, f.input().path("copy")))
        .hasRootCauseMessage("A página inspecionada saiu do destino candidato autorizado.");
    var wrongDimensions = original.deepCopy().put("pixelWidth", 2);
    assertThatThrownBy(
            () ->
                service.validate(
                    f.scope(), f.candidate(), f.task(), wrongDimensions, f.input().path("copy")))
        .hasRootCauseMessage("As dimensões da prova comercial não correspondem aos pixels.");
    when(visuals.read(801L, 1200L))
        .thenReturn(
            new AgentTaskVisualEvidenceService.EvidenceContent("image/png", new byte[] {0}));
    assertThatThrownBy(
            () ->
                service.validate(
                    f.scope(), f.candidate(), f.task(), original, f.input().path("copy")))
        .hasRootCauseMessage("Os pixels comerciais foram alterados.");
  }

  /**
   * Não oferece este adaptador para vídeos, perguntas diferentes ou comunicação ainda sem aceite.
   */
  @Test
  void keepsOtherRoutesAndMissingPrivateApprovalClosed() throws Exception {
    var f = fixture(4, 106, 13);
    ((ObjectNode) f.candidate().productContract()).putArray("heroVideos").addObject().put("id", 1);
    assertThat(service.input(f.scope(), f.candidate()).isNull()).isTrue();
    ((ObjectNode) f.candidate().productContract()).putArray("heroVideos");
    f.scope().cycle().setProductVersion("version-with-unknown-provider");
    assertThat(service.input(f.scope(), f.candidate()).isNull()).isTrue();
    f.scope().cycle().setProductVersion("musa-pde-entry-v14-primeiro-ajuste-aplicavel");
    when(privateProofs.resolve(any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("Sem aceite"));
    assertThat(service.input(f.scope(), f.candidate()).isNull()).isTrue();
  }

  /** Cria somente dados locais e fontes conferidas; nenhum modelo ou serviço externo é chamado. */
  private Fixture fixture(long productId, long experimentId, long cycleId) throws Exception {
    var product = Product.builder().id(productId).slug("fixture-opala").build();
    var experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion("musa-pde-entry-v14-primeiro-ajuste-aplicavel");
    var scope = new OpalaCommercialContext.Scope(cycle, experiment);
    var contract = json.createObjectNode();
    contract.putObject("commercialAccess").put("scope", "DAYS_2_TO_7_AND_SUPPORT_MATERIALS");
    contract.putArray("publicDiagnosticQuestions").addObject().put("key", "occasion");
    contract.withArray("publicDiagnosticQuestions").addObject().put("key", "existingSelection");
    contract.putArray("heroVideos");
    var candidate =
        new OpalaCommercialContext.Candidate(
            List.of(), "https://candidate.sandbox.local", "SLOT", contract);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("creatives");
    var call = new BusinessProcessActivityInstance();
    call.setActivityDefinition(activity);
    call.setStatus("COMPLETED");
    call.setObjectiveAchieved(true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                anyString(), anyString()))
        .thenReturn(List.of(call));
    var privateProof = json.createObjectNode().put("producerTaskId", 788);
    privateProof.putArray("renderedAssets").addObject().put("artifactId", 1118);
    when(privateProofs.resolve(any(), anyString(), anyString())).thenReturn(privateProof);
    var source = new AgentTask();
    source.setId(788L);
    source.setSourceReference("experiment:" + experimentId);
    source.setResultJson(
        "{\"functionalOutput\":{\"copy\":{\"headline\":\"Um ajuste com o que você já tem\",\"primaryText\":\"Experimente seu primeiro ajuste.\",\"description\":\"Primeiro passo gratuito\"}}}");
    when(tasks.findById(788L)).thenReturn(Optional.of(source));
    var input = (ObjectNode) service.input(scope, candidate);
    var agent = new Agent();
    agent.setAgentKey("landing-generator");
    var process = new BusinessProcessDefinition();
    process.setProcessCode(OpalaCommercialContext.CODE);
    var task = new AgentTask();
    task.setId(801L);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId("creative");
    task.setSourceReference("experiment:" + experimentId);
    task.setExecutionMode("DETERMINISTIC");
    task.setExecutionModelCode(OpalaCommercialUiProof.CONTRACT);
    var pngOutput = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(
        new java.awt.image.BufferedImage(1035, 1200, java.awt.image.BufferedImage.TYPE_INT_RGB),
        "png",
        pngOutput);
    byte[] bytes = pngOutput.toByteArray();
    String hash = sha(bytes);
    var receipt =
        input
            .deepCopy()
            .put("inspectedUrl", "https://candidate.sandbox.local/?mh_preview=qa&pde_analytics=off")
            .put("artifactId", 1200)
            .put("sha256", hash)
            .put("pixelWidth", 1035)
            .put("pixelHeight", 1200)
            .put("testData", true)
            .put("commercialEvidenceEligible", false)
            .put("model", "MUSA_LOCAL_RULES_V1")
            .put("serviceTier", "LOCAL")
            .put("inputTokens", 0)
            .put("outputTokens", 0)
            .put("costUsd", 0)
            .put("sourceType", "OWN_PRODUCT_UI")
            .put("externalImagesIncluded", false)
            .put("responseSha256", hash)
            .put(
                "rendererSha256",
                sha(
                    new ClassPathResource(
                            "agent-behavior-files/landing-generator-agent-worker/src/main/resources/proofs/opala/v1/capture-ui-proof.mjs")
                        .getContentAsByteArray()));
    var artifact = new AgentTaskVisualEvidence();
    artifact.setId(1200L);
    artifact.setTask(task);
    artifact.setEvidenceKey("COMMERCIAL-FIRST-VALUE");
    artifact.setEvidenceType("FOLD");
    artifact.setSourceUrl(candidate.destinationUrl());
    artifact.setFinalUrl(candidate.destinationUrl());
    artifact.setSha256(hash);
    when(artifacts.findByIdAndTaskId(1200L, 801L)).thenReturn(Optional.of(artifact));
    when(visuals.read(801L, 1200L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", bytes));
    return new Fixture(scope, candidate, task, input, receipt);
  }

  /** Identifica os bytes da fixture da mesma forma que o contrato produtivo. */
  private String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  /** Responsabilidade: agrupar o contexto isolado de uma captura local. */
  private record Fixture(
      OpalaCommercialContext.Scope scope,
      OpalaCommercialContext.Candidate candidate,
      AgentTask task,
      ObjectNode input,
      ObjectNode receipt) {}
}
