package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.creative.Creative;
import com.marketinghub.experiment.*;
import com.marketinghub.media.Asset;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialUiProof;
import com.marketinghub.pde.*;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: proteger revisão preparatória contra herança de aprovação, preço incorreto e
 * mistura de identidades.
 */
class CreativePreparationEvidenceResolverTest {
  private final ObjectMapper json = new ObjectMapper();
  private final AssetRepository assets = mock(AssetRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskVisualEvidenceRepository artifacts =
      mock(AgentTaskVisualEvidenceRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final CreativePreparationEvidenceResolver service =
      new CreativePreparationEvidenceResolver(
          assets,
          tasks,
          artifacts,
          cycles,
          slots,
          new PdeCommercialCheckoutContractResolver(json),
          json);

  /** Confere a candidata de dois produtos sem transformar preparação em autorização de venda. */
  @ParameterizedTest
  @CsvSource({"4,106,13", "407,922,71"})
  void distinguishesOwnPixelsFromCommercialActivation(
      long productId, long experimentId, long cycleId) throws Exception {
    var f = fixture(productId, experimentId, cycleId);
    var result = service.resolve(f.creative());
    assertThat(result.staticUiProof().verificationStatus()).isEqualTo("VERIFIED");
    assertThat(result.staticUiProof().testData()).isTrue();
    assertThat(result.staticUiProof().commercialApprovalGranted()).isFalse();
    assertThat(result.preparedCheckout().activationAuthorized()).isFalse();
    assertThat(result.preparedCheckout().productVersion()).isEqualTo("fixture-v14");
    f.creative().getExperiment().setStatus(ExperimentStatus.RUNNING);
    assertThat(service.resolve(f.creative()).preparedCheckout()).isNull();
    assertThat(service.resolve(f.creative()).staticUiProof()).isNotNull();
  }

  /** Divergência financeira ou slot de outra versão não comprova o checkout candidato. */
  @Test
  void rejectsWrongPriceOtherVersionAndMissingPublishedContract() throws Exception {
    var f = fixture(4, 106, 13);
    f.creative().getExperiment().setUnitPrice(new BigDecimal("79"));
    assertThat(service.resolve(f.creative()).preparedCheckout()).isNull();
    f.creative().getExperiment().setUnitPrice(new BigDecimal("67"));
    f.slot().setExperienceVersion("old");
    assertThat(service.resolve(f.creative()).preparedCheckout()).isNull();
    f.slot().setExperienceVersion("fixture-v14");
    f.slot().setPublishedExperienceJson(null);
    assertThat(service.resolve(f.creative()).preparedCheckout()).isNull();
    assertThat(service.resolve(f.creative()).staticUiProof()).isNotNull();
  }

  /** Outra ocorrência, tarefa não concluída ou pixels divergentes não geram prova de origem. */
  @Test
  void rejectsCrossContextAndIncompleteTaskWithoutWeakeningLegacyVideo() throws Exception {
    var f = fixture(4, 106, 13);
    f.task().setSourceReference("experiment:922");
    assertThat(service.resolve(f.creative()).staticUiProof()).isNull();
    f.task().setSourceReference("experiment:106");
    f.task().setStatus("IN_PROGRESS");
    assertThat(service.resolve(f.creative()).staticUiProof()).isNull();
    f.creative().setFormat("VIDEO");
    assertThat(service.resolve(f.creative()).staticUiProof()).isNull();
  }

  /** Monta uma captura própria já concluída e um checkout de contrato exclusivamente local. */
  private Fixture fixture(long productId, long experimentId, long cycleId) throws Exception {
    var product = Product.builder().id(productId).slug("fixture-opala").build();
    var experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .unitPrice(new BigDecimal("67"))
            .status(ExperimentStatus.PLANNED)
            .commercialCheckoutUrl("https://checkout.sandbox.local/offer")
            .build();
    var creative =
        Creative.builder()
            .id(542L)
            .experiment(experiment)
            .format("IMAGE")
            .imageUrl("https://assets.sandbox.local/current.png")
            .destinationUrl("https://candidate.sandbox.local")
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion("fixture-v14");
    cycle.setStatus("OPEN");
    when(cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    var task = new AgentTask();
    task.setId(801L);
    task.setSourceReference("experiment:" + experimentId);
    task.setAssignedAgent(Agent.builder().agentKey("landing-generator").build());
    task.setStatus("COMPLETED");
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode(
        com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.CODE);
    task.setProcessDefinition(process);
    task.setProcessActivityId("creative");
    task.setExecutionModelCode(OpalaCommercialUiProof.CONTRACT);
    task.setExecutionMode("DETERMINISTIC");
    when(tasks.findById(801L)).thenReturn(Optional.of(task));
    String sha = "a".repeat(64);
    var artifact = new AgentTaskVisualEvidence();
    artifact.setId(1200L);
    artifact.setTask(task);
    artifact.setSha256(sha);
    artifact.setSourceUrl(creative.getDestinationUrl());
    artifact.setCapturedAt(Instant.parse("2026-10-22T00:00:00Z"));
    when(artifacts.findByIdAndTaskId(1200L, 801L)).thenReturn(Optional.of(artifact));
    var audit =
        json.createObjectNode()
            .put("contractVersion", OpalaCommercialUiProof.CONTRACT)
            .put("productId", productId)
            .put("cycleId", cycleId)
            .put("experimentId", experimentId)
            .put("productVersion", "fixture-v14")
            .put("sourceTaskId", 801)
            .put("sourceArtifactId", 1200)
            .put("contentSha256", sha)
            .put("copySourceTaskId", 788)
            .put("copySourceArtifactId", 1118)
            .put("destinationUrl", creative.getDestinationUrl());
    audit
        .putObject("capture")
        .put("sha256", sha)
        .put("sourceType", "OWN_PRODUCT_UI")
        .put("testData", true)
        .put("externalImagesIncluded", false);
    when(assets.findByUrlIn(anyList()))
        .thenReturn(
            List.of(
                Asset.builder()
                    .model(OpalaCommercialUiProof.CONTRACT)
                    .payload(audit.toString())
                    .url(creative.getImageUrl())
                    .build()));
    var contract = json.createObjectNode().put("experienceVersion", "fixture-v14");
    contract
        .putObject("commercialBinding")
        .put("experimentId", experimentId)
        .put("priceBrl", 67)
        .put("billingModel", "ONE_TIME");
    contract
        .putObject("commercialCheckout")
        .put("provider", "PEPPER")
        .put("checkoutUrl", experiment.getCommercialCheckoutUrl())
        .put("offerReference", "fixture")
        .put("priceBrl", 67)
        .put("currency", "BRL")
        .put("billingModel", "ONE_TIME");
    var slot =
        PdeProductionSlot.builder()
            .id(11L)
            .productSlug(product.getSlug())
            .sourceExperimentId(experimentId)
            .status(PdeProductionSlotStatus.CANDIDATE)
            .experienceVersion("fixture-v14")
            .publicUrl(creative.getDestinationUrl())
            .publishedExperienceJson(contract.toString())
            .build();
    when(slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug())).thenReturn(List.of(slot));
    return new Fixture(creative, task, slot);
  }

  /**
   * Responsabilidade: manter referências mutáveis apenas para os cenários negativos deste teste.
   */
  private record Fixture(Creative creative, AgentTask task, PdeProductionSlot slot) {}
}
