package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderModelRequest;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderPricingRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger os gates de homologação do catálogo de modelos de vídeo. */
class SalesVideoProviderCatalogServiceTest {

  /** Normaliza preço oficial por vídeo sem confundir evidência com autorização de gasto. */
  @Test
  void shouldPersistAuditableProviderPricing() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("VEO");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    when(repository.save(model)).thenReturn(model);
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    var result =
        service.updatePricing(
            10L,
            new UpdateSalesVideoProviderPricingRequest(
                new BigDecimal("1.20"),
                "VIDEO",
                BigDecimal.ONE,
                "1080p",
                true,
                "https://example.com/official-pricing",
                Instant.now(),
                "VERIFIED",
                "Preço oficial da plataforma",
                "raw",
                "codex"));

    assertThat(result.normalizedCostPerSecondUsd()).isEqualByComparingTo("0.120000");
    assertThat(result.pricingStale()).isFalse();
    assertThat(result.pricingVerified()).isTrue();
    assertThat(result.lifecycleStatus()).isEqualTo("DRAFT");
  }

  /** Impede ativar Hailuo enquanto o QA comercial ainda não estiver concluído. */
  @Test
  void shouldRejectActiveHailuoWithoutImplementedAdapter() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("RUNWAY");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    UpdateSalesVideoProviderModelRequest request =
        new UpdateSalesVideoProviderModelRequest(
            "Teste controlado", "ACTIVE", true, true, true, false, "QA pendente");

    assertThatThrownBy(() -> service.update(10L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("adaptador, preço, licença e qualidade");
  }

  /** Permite manter candidato Runway em homologação sem fingir que o QA foi concluído. */
  @Test
  void shouldKeepCandidateInHomologationWithoutPretendingAdapterExists() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("RUNWAY");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    when(repository.save(model)).thenReturn(model);
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    var result =
        service.update(
            10L,
            new UpdateSalesVideoProviderModelRequest(
                "Teste controlado", "HOMOLOGATION", true, true, false, false, "Sem adapter"));

    assertThat(result.lifecycleStatus()).isEqualTo("HOMOLOGATION");
    assertThat(result.adapterVerified()).isTrue();
    assertThat(result.qualityGateVerified()).isFalse();
  }

  /** Impede ativar candidato Wan3.0 antes de existir um adapter implementado no executor. */
  @Test
  void shouldRejectWanPreviewWithoutImplementedAdapter() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("ALIBABA_MODEL_STUDIO");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    UpdateSalesVideoProviderModelRequest request =
        new UpdateSalesVideoProviderModelRequest(
            "Teste multimodal controlado", "ACTIVE", true, true, true, true, "Preview");

    assertThatThrownBy(() -> service.update(10L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("adaptador, preço, licença e qualidade");
  }

  /** Cria um modelo mínimo para validar as transições administrativas. */
  private SalesVideoProviderModel model(String adapterKey) {
    SalesVideoProviderModel model = new SalesVideoProviderModel();
    model.setId(10L);
    model.setCode("runway-hailuo-3");
    model.setDisplayName("Hailuo 3 via Runway");
    model.setProviderName("RUNWAY_HAILUO_3");
    model.setProviderFamily("EXTERNAL_VIDEO_MODULE");
    model.setAdapterKey(adapterKey);
    model.setExternalModelId("hailuo3");
    model.setRecommendedUse("Candidato");
    model.setLifecycleStatus("DRAFT");
    model.setClipDurationSeconds(10);
    model.setMaxDirectDurationSeconds(10);
    model.setDocumentationUrl("https://docs.dev.runwayml.com/guides/models/");
    model.setCreatedAt(Instant.now());
    model.setUpdatedAt(Instant.now());
    return model;
  }
}
