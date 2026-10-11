package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.service.postproduction.DeliveryExampleShot;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Garante fontes do próprio produto, segregação de tenant e variedade antes de gastar com voz. */
class VideoDeliveryExampleServiceTest {
  /** Limpa a identidade sintética após cada cenário. */
  @AfterEach
  void clear() {
    TenantContextHolder.clear();
  }

  /** Reproduz a seleção em produtos distintos, sem exceções por nome ou identificador. */
  @ParameterizedTest
  @ValueSource(longs = {91007, 92010})
  void resolvesAndRejectsForeignOrRepeatedSources(long productId) {
    var repository = mock(VideoProjectRepository.class);
    var source = mock(VideoDeliveryExampleSource.class);
    var project =
        VideoProject.builder()
            .id(90001L)
            .tenantId("local-test")
            .productId(productId)
            .experimentId(productId + 1)
            .campaignKey("local-v3")
            .build();
    when(repository.findById(90001L)).thenReturn(Optional.of(project));
    when(source.approved(productId)).thenReturn(List.of(example(1, "a"), example(2, "b")));
    TenantContextHolder.set(new TenantContext("local-test", "fixture@sandbox.local", false));
    var service = new VideoDeliveryExampleService(repository, source);
    var shots = List.of(new DeliveryExampleShot(1L, 0), new DeliveryExampleShot(2L, 1));
    assertThat(service.sequence(90001L, shots, "Veja uma amostra. | Conheça o kit."))
        .containsEntry("productId", productId)
        .containsEntry("runtimeCaptureClaimed", false)
        .containsEntry("commercialEvidenceClaimed", false);
    assertThatThrownBy(
            () ->
                service.sequence(
                    90001L,
                    List.of(new DeliveryExampleShot(1L, 0), new DeliveryExampleShot(999L, 1)),
                    "A | B"))
        .hasMessageContaining("não está aprovada");
    assertThatThrownBy(
            () ->
                service.sequence(
                    90001L,
                    List.of(new DeliveryExampleShot(1L, 0), new DeliveryExampleShot(1L, 1)),
                    "A | B"))
        .hasMessageContaining("duas artes diferentes");
    assertThatThrownBy(
            () ->
                service.sequence(
                    90001L,
                    List.of(new DeliveryExampleShot(1L, 1), new DeliveryExampleShot(2L, 2)),
                    "A | B | C"))
        .hasMessageContaining("primeiro trecho");
    TenantContextHolder.set(new TenantContext("outro", "fixture@sandbox.local", false));
    assertThatThrownBy(() -> service.available(90001L)).hasMessageContaining("404");
  }

  /** Cria registro sintético com identidade estável e autorização de teste, sem rede externa. */
  private com.marketinghub.salesvideo.dto.DeliveryExampleDto example(long id, String hash) {
    return new com.marketinghub.salesvideo.dto.DeliveryExampleDto(
        id,
        902L,
        "Amostra " + id,
        "https://example.invalid/" + id,
        hash.repeat(64),
        "Fixture",
        "Somente testes locais");
  }
}
