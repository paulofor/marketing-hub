package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.marketinghub.repository.jpa.salesvideo.VideoReferenceRepository;
import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.VideoReferenceStatus;
import com.marketinghub.salesvideo.dto.CreateVideoReferenceRequest;
import com.marketinghub.salesvideo.dto.VideoReferenceDto;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a fila de vídeos externos enviados para análise comercial. */
@ExtendWith(MockitoExtension.class)
class VideoReferenceServiceTest {
  @Mock private VideoReferenceRepository repository;

  private VideoReferenceService service;

  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-musa", "editor@marketinghub.io", false));
    service = new VideoReferenceService(repository);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** Cadastra vídeo externo preservando tenant, objetivo de aprendizado e status inicial. */
  @Test
  void shouldCreateVideoReferenceQueuedForCurrentTenant() {
    CreateVideoReferenceRequest request =
        new CreateVideoReferenceRequest(
            "Reels com gancho de transformação visual",
            "https://social.example/video",
            "Instagram",
            "MUSA",
            "AWARENESS",
            "Aprender como o vídeo prende atenção nos primeiros três segundos.",
            "1M views, muitos comentários e prova social forte.",
            "editor@marketinghub.io");
    given(repository.save(any(VideoReference.class)))
        .willAnswer(
            invocation -> {
              VideoReference reference = invocation.getArgument(0);
              reference.setId(41L);
              return reference;
            });

    VideoReferenceDto result = service.createReference(request);

    ArgumentCaptor<VideoReference> captor = ArgumentCaptor.forClass(VideoReference.class);
    org.mockito.Mockito.verify(repository).save(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-musa");
    assertThat(captor.getValue().getStatus()).isEqualTo(VideoReferenceStatus.QUEUED);
    assertThat(result.id()).isEqualTo(41L);
    assertThat(result.primaryLearningGoal()).contains("primeiros três segundos");
  }
}
