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
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetStorageService.StoredObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** Valida a fila de vídeos externos enviados para análise comercial. */
@ExtendWith(MockitoExtension.class)
class VideoReferenceServiceTest {
  @Mock private VideoReferenceRepository repository;
  @Mock private AssetStorageService storageService;

  private VideoReferenceService service;

  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-musa", "editor@marketinghub.io", false));
    service = new VideoReferenceService(repository, storageService);
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

  /** Cadastra upload de vídeo usando a URL pública retornada pelo storage. */
  @Test
  void shouldUploadVideoReferenceAndQueueStoredFile() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "video.mp4", "video/mp4", "conteudo".getBytes());
    given(storageService.store(any(), any()))
        .willReturn(
            new StoredObject(
                "sales-videos/2026/08/01/misc/video.mp4",
                "https://cdn.example/video.mp4",
                8L,
                "video/mp4",
                true));
    given(repository.save(any(VideoReference.class)))
        .willAnswer(
            invocation -> {
              VideoReference reference = invocation.getArgument(0);
              reference.setId(42L);
              return reference;
            });

    VideoReferenceDto result =
        service.uploadReference(
            file,
            "Vídeo vencedor enviado pelo usuário",
            "",
            "MUSA",
            "AWARENESS",
            "Aprender ritmo, prova e CTA.",
            "Alta retenção observada.",
            "editor@marketinghub.io");

    ArgumentCaptor<VideoReference> captor = ArgumentCaptor.forClass(VideoReference.class);
    org.mockito.Mockito.verify(repository).save(captor.capture());
    assertThat(captor.getValue().getSourceUrl()).isEqualTo("https://cdn.example/video.mp4");
    assertThat(captor.getValue().getSourcePlatform()).isEqualTo("Upload");
    assertThat(result.id()).isEqualTo(42L);
  }

  /** Consulta vídeo de referência preservando isolamento do tenant atual. */
  @Test
  void shouldGetVideoReferenceForCurrentTenant() {
    VideoReference reference =
        VideoReference.builder()
            .tenantId("tenant-musa")
            .title("Tik Tok Flavio")
            .sourceUrl("https://cdn.example/tiktok-flavio.mp4")
            .primaryLearningGoal("Aprender ritmo e gancho.")
            .analysisNotes("**Diagnóstico comercial**\n- Criativo de topo de funil.")
            .status(VideoReferenceStatus.ANALYZED)
            .build();
    reference.setId(43L);
    given(repository.findById(43L)).willReturn(java.util.Optional.of(reference));

    VideoReferenceDto result = service.getReference(43L);

    assertThat(result.id()).isEqualTo(43L);
    assertThat(result.title()).isEqualTo("Tik Tok Flavio");
    assertThat(result.analysisNotes()).contains("Diagnóstico comercial");
    assertThat(result.status()).isEqualTo(VideoReferenceStatus.ANALYZED);
  }
}
