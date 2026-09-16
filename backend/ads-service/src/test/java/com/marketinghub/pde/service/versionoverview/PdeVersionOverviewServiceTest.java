package com.marketinghub.pde.service.versionoverview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoAssetDto;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoPanelDto;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a visão consolidada do ciclo de vida das versões PDE Opala. */
@ExtendWith(MockitoExtension.class)
class PdeVersionOverviewServiceTest {

  @Mock private PdeProductionSlotRepository slotRepository;

  @Mock private ExperimentRepository experimentRepository;

  @Mock private PdeProductionSlotService slotService;

  /** Deve consolidar candidata, experimento, oferta, vídeo e trajetória sem criar outra versão. */
  @Test
  void listsCandidateWithCommercialEvidenceAndPendingHomologation() {
    Product product = opalaProduct();
    Instant updatedAt = Instant.parse("2026-09-16T12:00:00Z");
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(12L)
            .slotCode("v12")
            .productSlug(product.getSlug())
            .domain("v12.clubemusa.com.br")
            .publicUrl("https://v12.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel")
            .layoutKey("video-explicativo")
            .targetEnvironment("production-v12")
            .status(PdeProductionSlotStatus.CANDIDATE)
            .sourceExperimentId(92L)
            .notes("Inclui vídeo de apresentação na entrada")
            .draftExperienceJson(
                "{\"name\":\"Vega com vídeo de apresentação\",\"slug\":\"metodo-musa-7-dias\"}")
            .validationStatus("OK")
            .validationSummary("URL produtiva validada")
            .validationCheckedAt(updatedAt)
            .updatedAt(updatedAt)
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(92L)
            .name("Vega · vídeo de apresentação")
            .status(ExperimentStatus.PLANNED)
            .hypothesis("Vídeo de apresentação aumenta o avanço para o primeiro valor")
            .unitPrice(new BigDecimal("67.00"))
            .primaryCta("Começar agora")
            .commercialCheckoutUrl("https://checkout.example/v12")
            .build();
    PostDeployPdeProductionSlotDto slotDto = slotDto(slot);
    PdeProductionSlotVideoAssetDto video =
        new PdeProductionSlotVideoAssetDto(
            42L,
            92L,
            "SOURCE_EXPERIMENT",
            "Apresentar a experiência",
            "PDE_ENTRY",
            "HEYGEN",
            "avatar",
            ExperimentVideoStatus.READY,
            ExperimentVideoReviewStatus.APPROVED,
            "https://cdn.example/v12.mp4",
            "/assets/hls/v12/index.m3u8",
            null,
            55,
            null,
            null,
            null,
            null);
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of(slot));
    when(experimentRepository.findAllById(List.of(92L))).thenReturn(List.of(experiment));
    when(slotService.listProductionSlotVideosForProduct(product.getSlug()))
        .thenReturn(
            List.of(new PdeProductionSlotVideoPanelDto(slotDto, List.of(video), List.of())));

    ProductPdeVersionOverviewDto result = service().list(product).get(0);

    assertThat(result.lifecycleLabel()).isEqualTo("Candidata");
    assertThat(result.name()).isEqualTo("Vega com vídeo de apresentação");
    assertThat(result.hypothesis()).contains("primeiro valor");
    assertThat(result.approvedVideoCount()).isEqualTo(1);
    assertThat(result.priceBrl()).isEqualByComparingTo("67.00");
    assertThat(result.pendingItems()).containsExactly("Concluir a homologação comercial.");
    assertThat(result.lifecycle())
        .extracting(PdeVersionLifecycleStepDto::status)
        .containsExactly("DONE", "DONE", "DONE", "DONE", "CURRENT", "DONE", "PENDING");
  }

  /** Deve rejeitar acesso à visão quando o produto não pertence ao tipo Opala. */
  @Test
  void rejectsNonOpalaProduct() {
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-digital")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code("LOW_TICKET_DIGITAL_PRODUCT").build())
            .build();

    assertThatThrownBy(() -> service().list(product))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("somente para produtos Opala");
  }

  /** Deve aceitar o nome interno Opala durante a transição de cadastros legados. */
  @Test
  void acceptsCanonicalInternalNameWhenLegacyCodeDiffers() {
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code("LEGACY_PDE").internalName("Opala").build())
            .build();
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of());
    when(slotService.listProductionSlotVideosForProduct(product.getSlug())).thenReturn(List.of());

    assertThat(service().list(product)).isEmpty();
  }

  /** Cria o serviço com dependências isoladas para cada cenário. */
  private PdeVersionOverviewService service() {
    return new PdeVersionOverviewService(
        slotRepository, experimentRepository, slotService, new ObjectMapper());
  }

  /** Monta um produto classificado pelo código canônico que representa Opala. */
  private Product opalaProduct() {
    return Product.builder()
        .id(4L)
        .slug("metodo-musa-7-dias")
        .primaryHypothesis("Hipótese base do produto")
        .productTypeDefinition(
            ProductTypeDefinition.builder().code("PDE").internalName("Opala").build())
        .build();
  }

  /** Converte somente os campos do slot exigidos pelo painel de vídeo do teste. */
  private PostDeployPdeProductionSlotDto slotDto(PdeProductionSlot slot) {
    return new PostDeployPdeProductionSlotDto(
        slot.getId(),
        slot.getSlotCode(),
        slot.getProductSlug(),
        slot.getDomain(),
        slot.getPublicUrl(),
        slot.getBackendUrl(),
        slot.getExperienceVersion(),
        slot.getLayoutKey(),
        slot.getTargetEnvironment(),
        slot.getStatus(),
        slot.getSourceExperimentId(),
        slot.getNotes(),
        slot.getDraftExperienceJson(),
        slot.getPublishedExperienceJson(),
        slot.getPublishedBy(),
        slot.getPublishedAt(),
        slot.getValidationStatus(),
        slot.getValidationCheckedAt(),
        slot.getValidationHttpStatus(),
        slot.getValidationSummary(),
        slot.getValidationDetail(),
        slot.getValidationContractSlug(),
        slot.getValidationContractHealthPath(),
        slot.getValidationResolvedUrl(),
        slot.getCreatedAt(),
        slot.getUpdatedAt());
  }
}
