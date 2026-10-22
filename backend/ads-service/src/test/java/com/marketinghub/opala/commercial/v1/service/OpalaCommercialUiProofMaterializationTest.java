package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.*;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: preservar o anúncio ajustado e criar uma revisão comercial própria a partir da
 * prova atual.
 */
class OpalaCommercialUiProofMaterializationTest {
  /**
   * A captura substitui somente a candidata e nunca converte parecer privado em aprovação
   * comercial.
   */
  @Test
  void createsNewVersionWithoutOverwritingRejectionAndReusesDuplicateCallback() throws Exception {
    var json = new ObjectMapper();
    var proofs = mock(OpalaCommercialUiProof.class);
    var storage = mock(AssetStorageService.class);
    var assets = mock(AssetRepository.class);
    var creatives = mock(CreativeRepository.class);
    var creativeService = mock(CreativeService.class);
    var service =
        new OpalaCommercialUiProofMaterialization(
            proofs, storage, assets, creatives, creativeService, json);
    var experiment =
        Experiment.builder()
            .id(106L)
            .status(ExperimentStatus.PLANNED)
            .commercialCheckoutUrl("https://checkout.sandbox.local")
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(13L);
    cycle.setProductId(4L);
    cycle.setProductVersion("fixture-v14");
    var scope = new OpalaCommercialContext.Scope(cycle, experiment);
    var candidate =
        new OpalaCommercialContext.Candidate(
            List.of(), "https://candidate.sandbox.local", "SLOT", json.createObjectNode());
    var task = new AgentTask();
    task.setId(801L);
    var receipt = json.createObjectNode();
    var instruction =
        json.createObjectNode()
            .put("headline", "Um ajuste com o que você já tem")
            .put("primaryText", "Veja um ajuste gratuito.")
            .put("description", "Primeiro passo gratuito");
    var input =
        json.createObjectNode().put("copySourceTaskId", 788).put("copySourceArtifactId", 1118);
    var content =
        new OpalaCommercialUiProof.Content(new byte[] {1, 2, 3}, 1200L, "a".repeat(64), input);
    when(proofs.validate(scope, candidate, task, receipt, instruction)).thenReturn(content);
    var previous =
        Creative.builder()
            .id(541L)
            .format("IMAGE")
            .imageUrl("https://assets.sandbox.local/old.png")
            .agentReviewStatus(CreativeAgentReviewStatus.ADJUST)
            .agentReviewJson("parecer anterior preservado")
            .build();
    var next =
        Creative.builder()
            .id(542L)
            .format("IMAGE")
            .imageUrl("https://assets.sandbox.local/current.png")
            .sourceCreative(previous)
            .build();
    when(creatives.findByExperimentId(106L)).thenReturn(List.of(previous), List.of(previous, next));
    when(storage.storeBytes(any(), anyString(), eq("image/png"), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "proof.png", next.getImageUrl(), 3, "image/png", true));
    when(creativeService.createVersion(eq(541L), any())).thenReturn(next);
    var stored = new java.util.ArrayList<Asset>();
    when(assets.findByUrlIn(anyList())).thenAnswer(i -> List.copyOf(stored));
    when(assets.save(any()))
        .thenAnswer(
            i -> {
              stored.add(i.getArgument(0));
              return i.getArgument(0);
            });
    service.apply(scope, candidate, task, receipt, instruction);
    service.apply(scope, candidate, task, receipt, instruction);
    var request = ArgumentCaptor.forClass(CreateCreativeRequest.class);
    verify(creativeService).createVersion(eq(541L), request.capture());
    verify(storage).storeBytes(any(), anyString(), eq("image/png"), any());
    verify(creativeService).requestAgentReview(542L);
    assertThat(request.getValue().getCostUsd()).isZero();
    assertThat(request.getValue().getStatus()).isEqualTo(CreativeStatus.DRAFT);
    assertThat(previous.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.ADJUST);
    assertThat(previous.getAgentReviewJson()).isEqualTo("parecer anterior preservado");
    assertThat(
            json.readTree(stored.getFirst().getPayload())
                .path("commercialApprovalGranted")
                .asBoolean())
        .isFalse();
  }
}
