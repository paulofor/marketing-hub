package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.CreateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.UpdateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.VideoProjectDto;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o cadastro editorial de projetos de vídeo. */
@ExtendWith(MockitoExtension.class)
class VideoProjectServiceTest {
  @Mock private VideoProjectRepository repository;

  private VideoProjectService service;

  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-musa", "editor@marketinghub.io", false));
    service = new VideoProjectService(repository);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** Cria projeto preservando briefing comercial completo e tenant ativo. */
  @Test
  void shouldCreateVideoProjectForCurrentTenant() {
    CreateVideoProjectRequest request =
        new CreateVideoProjectRequest(
            4L,
            66L,
            12L,
            "musa-organico-001",
            "ORGANIC",
            "MIXED_AI_SCENES",
            "INSTAGRAM_REELS",
            "VERTICAL_9_16",
            "Corte orgânico de presença visual",
            "Gerar clique qualificado para o diagnóstico MUSA",
            "Uma profissional percebe que sua presença digital não traduz sua autoridade.",
            "AWARENESS",
            "PROFILE_VISIT",
            "Sua imagem comunica antes da sua fala",
            "Roteiro curto",
            "Cena 1",
            "Visual premium",
            "Voz feminina",
            "Trilha leve",
            "Legenda curta",
            "Fazer diagnóstico",
            180,
            "Runway para cenas, FFmpeg para montagem",
            "Corte rápido",
            "Audio audível e CTA claro",
            VideoProjectStatus.READY_FOR_SCRIPT,
            "editor@marketinghub.io");
    given(repository.save(any(VideoProject.class)))
        .willAnswer(
            invocation -> {
              VideoProject project = invocation.getArgument(0);
              project.setId(91L);
              return project;
            });

    VideoProjectDto result = service.createProject(request);

    ArgumentCaptor<VideoProject> captor = ArgumentCaptor.forClass(VideoProject.class);
    org.mockito.Mockito.verify(repository).save(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-musa");
    assertThat(result.id()).isEqualTo(91L);
    assertThat(result.contextType()).isEqualTo("ORGANIC");
    assertThat(result.storyText()).contains("presença digital");
    assertThat(result.status()).isEqualTo(VideoProjectStatus.READY_FOR_SCRIPT);
  }

  /** Atualiza projeto existente mantendo isolamento por tenant. */
  @Test
  void shouldUpdateVideoProjectForCurrentTenant() {
    VideoProject project =
        VideoProject.builder()
            .id(91L)
            .tenantId("tenant-musa")
            .contextType("PDE")
            .productionMode("AVATAR")
            .targetChannel("PDE")
            .format("VERTICAL_9_16")
            .title("Original")
            .objective("Objetivo original")
            .status(VideoProjectStatus.DRAFT)
            .build();
    UpdateVideoProjectRequest request =
        new UpdateVideoProjectRequest(
            4L,
            null,
            null,
            "musa-campanha",
            "CAMPAIGN",
            "MONTAGE",
            "META_ADS",
            "VERTICAL_9_16",
            "Video de campanha",
            "Aumentar compra do PDE",
            "A usuária sai de tentativa manual para uma rotina guiada por IA.",
            "CONVERSION",
            "PURCHASE",
            "Gancho",
            "Roteiro",
            "Cenas",
            "Referencias",
            "Voz",
            "Trilha",
            "Legendas",
            "Comprar agora",
            210,
            "Provider plan",
            "Notas",
            "Gate",
            VideoProjectStatus.READY_FOR_RENDER,
            "editor@marketinghub.io");
    given(repository.findById(91L)).willReturn(Optional.of(project));
    given(repository.save(any(VideoProject.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    VideoProjectDto result = service.updateProject(91L, request);

    assertThat(result.title()).isEqualTo("Video de campanha");
    assertThat(result.contextType()).isEqualTo("CAMPAIGN");
    assertThat(result.storyText()).contains("rotina guiada por IA");
    assertThat(result.status()).isEqualTo(VideoProjectStatus.READY_FOR_RENDER);
  }

  /** Bloqueia criação de projeto abaixo de três minutos para preservar o escopo do Estúdio. */
  @Test
  void shouldRejectProjectShorterThanThreeMinutes() {
    CreateVideoProjectRequest request =
        new CreateVideoProjectRequest(
            4L,
            66L,
            12L,
            "musa-organico-001",
            "ORGANIC",
            "MIXED_AI_SCENES",
            "INSTAGRAM_REELS",
            "VERTICAL_9_16",
            "Corte orgânico de presença visual",
            "Gerar clique qualificado para o diagnóstico MUSA",
            "Uma profissional percebe que sua presença digital não traduz sua autoridade.",
            "AWARENESS",
            "PROFILE_VISIT",
            "Sua imagem comunica antes da sua fala",
            "Roteiro curto",
            "Cena 1",
            "Visual premium",
            "Voz feminina",
            "Trilha leve",
            "Legenda curta",
            "Fazer diagnóstico",
            179,
            "Runway para cenas, FFmpeg para montagem",
            "Corte rápido",
            "Audio audível e CTA claro",
            VideoProjectStatus.READY_FOR_SCRIPT,
            "editor@marketinghub.io");

    assertThatThrownBy(() -> service.createProject(request))
        .hasMessageContaining("180 segundos ou mais");
  }

  /** Bloqueia edição que tente reduzir projeto do Estúdio para menos de três minutos. */
  @Test
  void shouldRejectUpdateThatMakesProjectShorterThanThreeMinutes() {
    VideoProject project =
        VideoProject.builder()
            .id(91L)
            .tenantId("tenant-musa")
            .contextType("PDE")
            .productionMode("AVATAR")
            .targetChannel("PDE")
            .format("VERTICAL_9_16")
            .title("Original")
            .objective("Objetivo original")
            .targetDurationSeconds(180)
            .status(VideoProjectStatus.DRAFT)
            .build();
    UpdateVideoProjectRequest request =
        new UpdateVideoProjectRequest(
            4L,
            null,
            null,
            "musa-campanha",
            "CAMPAIGN",
            "MONTAGE",
            "META_ADS",
            "VERTICAL_9_16",
            "Video de campanha",
            "Aumentar compra do PDE",
            "A usuária sai de tentativa manual para uma rotina guiada por IA.",
            "CONVERSION",
            "PURCHASE",
            "Gancho",
            "Roteiro",
            "Cenas",
            "Referencias",
            "Voz",
            "Trilha",
            "Legendas",
            "Comprar agora",
            120,
            "Provider plan",
            "Notas",
            "Gate",
            VideoProjectStatus.READY_FOR_RENDER,
            "editor@marketinghub.io");
    given(repository.findById(91L)).willReturn(Optional.of(project));

    assertThatThrownBy(() -> service.updateProject(91L, request))
        .hasMessageContaining("180 segundos ou mais");
  }
}
