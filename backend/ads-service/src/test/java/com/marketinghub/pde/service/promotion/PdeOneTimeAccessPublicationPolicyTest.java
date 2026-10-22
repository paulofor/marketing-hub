package com.marketinghub.pde.service.promotion;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.pde.service.publishslotcontract.PublishPdeProductionSlotContractRequest;
import com.marketinghub.pde.service.versionoverview.PdeVersionOverviewService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: validar a preparação sem vídeos sem antecipar promoção ou liberação comercial.
 */
class PdeOneTimeAccessPublicationPolicyTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Percorre a publicação preparatória real e a visão usada pelo botão do frontend. */
  @ParameterizedTest
  @CsvSource({"4,106,musa-v14", "407,922,outra-v18"})
  void publishesCandidateWithoutActivatingExperiment(
      long productId, long experimentId, String version) throws Exception {
    var slot = slot(experimentId, version);
    var experiment = experiment(productId, experimentId);
    var contract = contract(experimentId, version);
    slot.setDraftExperienceJson(contract.toString());
    var slots = mock(PdeProductionSlotRepository.class);
    var experiments = mock(ExperimentRepository.class);
    var videos = mock(ExperimentVideoAssetRepository.class);
    when(slots.findByProductSlugAndSlotCode("fixture-opala", "candidate"))
        .thenReturn(Optional.of(slot));
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala")).thenReturn(List.of(slot));
    when(slots.save(any())).thenAnswer(i -> i.getArgument(0));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(experiments.findAllById(any())).thenReturn(List.of(experiment));
    var service =
        new PdeProductionSlotService(slots, videos, experiments, mock(HttpClient.class), json);
    var overview = new PdeVersionOverviewService(slots, experiments, videos, service, json);
    var presentation = overview.list(experiment.getProduct()).getFirst();
    assertThat(presentation.canPublishContract()).isTrue();
    assertThat(presentation.canPreparePublication()).isFalse();
    assertThat(presentation.pendingItems()).noneMatch(s -> s.toLowerCase().contains("vídeo"));
    assertThatThrownBy(
            () -> service.findPublishedExperienceJson("fixture-opala", "candidate", version))
        .hasMessageContaining("não publicado");
    service.publishProductionSlotContract(
        "fixture-opala",
        "candidate",
        new PublishPdeProductionSlotContractRequest(contract.toString(), "Preparação local"));
    assertThat(json.readTree(slot.getPublishedExperienceJson()).toString())
        .isEqualTo(contract.toString());
    assertThat(
            json.readTree(
                    service
                        .findPublishedExperienceJson("fixture-opala", "candidate", version)
                        .orElseThrow())
                .path("experienceVersion")
                .asText())
        .isEqualTo(version);
    assertThat(slot.getStatus()).isEqualTo(PdeProductionSlotStatus.CANDIDATE);
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
    assertThat(experiment.getFacebookReleaseRequestedAt()).isNull();
    assertThatThrownBy(
            () -> service.prepareProductionSlotForPublication("fixture-opala", "candidate"))
        .hasMessageContaining("checkout");
    experiment.setCommercialCheckoutUrl("https://checkout.sandbox.local/oferta");
    experiment.setFollowUpActionUrl(slot.getPublicUrl());
    slot.setValidationStatus("OK");
    service.prepareProductionSlotForPublication("fixture-opala", "candidate");
    assertThat(slot.getStatus()).isEqualTo(PdeProductionSlotStatus.READY);
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
  }

  /**
   * Mantém rejeição de outra identidade, preço ou versão de acesso apesar da ausência de vídeos.
   */
  @Test
  void rejectsCrossContextPriceAndAccess() {
    var slot = slot(106L, "musa-v14");
    var experiment = experiment(4L, 106L);
    var contract = contract(106L, "musa-v14");
    ((ObjectNode) contract.path("commercialBinding")).put("experimentId", 922L);
    ((ObjectNode) contract.path("commercialCheckout")).put("priceBrl", 1);
    ((ObjectNode) contract.path("commercialAccess")).put("experienceVersion", "anterior");
    assertThat(PdeOneTimeAccessPublicationPolicy.preparationBlockers(slot, experiment, contract))
        .anyMatch(s -> s.contains("experimento"))
        .anyMatch(s -> s.contains("checkout"))
        .anyMatch(s -> s.contains("acesso"));
  }

  /** Uma campanha já liberada não pode receber outra publicação preparatória. */
  @Test
  void refusesPreparationAfterReleaseAndPreservesLegacyPolicy() {
    var slot = slot(106L, "musa-v14");
    var experiment = experiment(4L, 106L);
    experiment.setStatus(ExperimentStatus.RUNNING);
    assertThat(
            PdeOneTimeAccessPublicationPolicy.preparationBlockers(
                slot, experiment, contract(106L, "musa-v14")))
        .anyMatch(s -> s.contains("liberação"));
    slot.setExperienceVersion(PdeV12PublicationPolicy.EXPERIENCE_VERSION);
    assertThat(PdeOneTimeAccessPublicationPolicy.appliesTo(slot, contract(106L, "musa-v14")))
        .isFalse();
    assertThat(PdeV12PublicationPolicy.appliesTo(slot)).isTrue();
  }

  /** Cria uma candidata sintética sem preço nem autorização implícita de mídia. */
  private PdeProductionSlot slot(long experimentId, String version) {
    return PdeProductionSlot.builder()
        .id(11L)
        .slotCode("candidate")
        .productSlug("fixture-opala")
        .domain("candidate.sandbox.local")
        .publicUrl("https://candidate.sandbox.local")
        .layoutKey("espelho-antes-de-sair")
        .experienceVersion(version)
        .sourceExperimentId(experimentId)
        .status(PdeProductionSlotStatus.CANDIDATE)
        .build();
  }

  /** Monta produto e experimento da própria ocorrência sem conta ou cobrança real. */
  private Experiment experiment(long productId, long experimentId) {
    var type = new ProductTypeDefinition();
    type.setCode("PDE");
    var product =
        Product.builder().id(productId).slug("fixture-opala").productTypeDefinition(type).build();
    return Experiment.builder()
        .id(experimentId)
        .product(product)
        .status(ExperimentStatus.PLANNED)
        .unitPrice(new BigDecimal("67"))
        .hypothesis("Ajuste antes do compromisso")
        .build();
  }

  /** Declara a capacidade de entrega sem inventar pareceres, tráfego ou compras. */
  private ObjectNode contract(long experimentId, String version) {
    var node =
        json.createObjectNode()
            .put("slug", "fixture-opala")
            .put("experienceVersion", version)
            .put("layoutKey", "espelho-antes-de-sair");
    node.putObject("commercialBinding")
        .put("experimentId", experimentId)
        .put("priceBrl", 67)
        .put("billingModel", "ONE_TIME");
    node.putObject("commercialCheckout")
        .put("provider", "FIXTURE")
        .put("checkoutUrl", "https://checkout.sandbox.local/oferta")
        .put("priceBrl", 67)
        .put("currency", "BRL")
        .put("billingModel", "ONE_TIME");
    node.putObject("commercialAccess")
        .put("experienceVersion", version)
        .put("scope", "DAYS_2_TO_7_AND_SUPPORT_MATERIALS")
        .put("accessDays", 90)
        .put("renewal", false)
        .put("activationTrigger", "PAYMENT_APPROVED");
    var missions = node.putArray("missions");
    for (int day = 1; day <= 7; day++) missions.addObject().put("day", day);
    node.putArray("supportMaterials").addObject().put("url", "/materials/fixture.pdf");
    return node;
  }
}
