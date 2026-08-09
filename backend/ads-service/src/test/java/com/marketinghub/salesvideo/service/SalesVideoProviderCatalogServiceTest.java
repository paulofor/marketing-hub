package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderModelRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger os gates de homologação do catálogo de modelos de vídeo. */
class SalesVideoProviderCatalogServiceTest {

  /** Impede ativar Hailuo enquanto o adaptador MiniMax ainda não estiver implementado. */
  @Test
  void shouldRejectActiveHailuoWithoutImplementedAdapter() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("MINIMAX");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    UpdateSalesVideoProviderModelRequest request =
        new UpdateSalesVideoProviderModelRequest(
            "Teste controlado", "ACTIVE", true, true, true, true, "QA aprovada");

    assertThatThrownBy(() -> service.update(10L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("adaptador, preço, licença e qualidade");
  }

  /** Permite manter candidato sem adaptador no estado de homologação, sem execução produtiva. */
  @Test
  void shouldKeepCandidateInHomologationWithoutPretendingAdapterExists() {
    SalesVideoProviderModelRepository repository = mock(SalesVideoProviderModelRepository.class);
    SalesVideoProviderModel model = model("MINIMAX");
    when(repository.findById(10L)).thenReturn(Optional.of(model));
    when(repository.save(model)).thenReturn(model);
    SalesVideoProviderCatalogService service = new SalesVideoProviderCatalogService(repository);

    var result = service.update(
        10L,
        new UpdateSalesVideoProviderModelRequest(
            "Teste controlado", "HOMOLOGATION", true, true, false, false, "Sem adapter"));

    assertThat(result.lifecycleStatus()).isEqualTo("HOMOLOGATION");
    assertThat(result.adapterVerified()).isFalse();
  }

  /** Cria um modelo mínimo para validar as transições administrativas. */
  private SalesVideoProviderModel model(String adapterKey) {
    SalesVideoProviderModel model = new SalesVideoProviderModel();
    model.setId(10L);
    model.setCode("minimax-hailuo-2-3");
    model.setDisplayName("MiniMax Hailuo 2.3");
    model.setProviderName("MINIMAX_HAILUO_2_3");
    model.setProviderFamily("EXTERNAL_VIDEO_MODULE");
    model.setAdapterKey(adapterKey);
    model.setExternalModelId("MiniMax-Hailuo-2.3");
    model.setRecommendedUse("Candidato");
    model.setLifecycleStatus("DRAFT");
    model.setClipDurationSeconds(6);
    model.setMaxDirectDurationSeconds(10);
    model.setDocumentationUrl("https://platform.minimax.io/docs/");
    model.setCreatedAt(Instant.now());
    model.setUpdatedAt(Instant.now());
    return model;
  }
}
