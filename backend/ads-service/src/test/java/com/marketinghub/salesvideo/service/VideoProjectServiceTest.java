package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.CreateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.ResearchIntelligenceSelectionDto;
import com.marketinghub.salesvideo.dto.UpdateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.VideoProjectDto;
import com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.List;
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
  @Mock private VideoProjectResearchIntelligenceMapper researchIntelligenceMapper;

  private VideoProjectService service;

  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-musa", "editor@marketinghub.io", false));
    service = new VideoProjectService(repository);
    service.setResearchIntelligenceMapper(researchIntelligenceMapper);
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
            99L,
            66L,
            12L,
            "musa-organico-001",
            "COMMERCIAL_SHORT",
            "ORGANIC",
            "MIXED_AI_SCENES",
            "INSTAGRAM_REELS",
            "VERTICAL_9_16",
            "Corte orgânico de presença visual",
            "Gerar clique qualificado para o diagnóstico MUSA",
            "Uma profissional percebe que sua presença digital não traduz sua autoridade.",
            "AWARENESS",
            "PROFILE_VISIT",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Sua imagem comunica antes da sua fala",
            "Roteiro curto",
            "Cena 1",
            "Visual premium",
            "Personagem Ana: rosto frontal, tres quartos, corpo inteiro, figurino principal e URL da imagem aprovada.",
            "Apartamento claro: plano geral, angulo oposto, lateral, entradas, objetos fixos e URL da imagem aprovada.",
            "Produto MUSA: tela do diagnostico, simbolo visual, objeto de escala e referencias de interface.",
            "Imagem limpa, premium, luz suave, pele natural, composicao vertical e paleta elegante.",
            "Gerar imagens mestre com OpenAI antes do video: personagem, ambiente, produto e frames-chave.",
            "Nunca alterar rosto, figurino, paleta, posicao dos objetos fixos ou arquitetura entre cenas.",
            "Voz feminina",
            "Trilha leve",
            "Legenda curta",
            "Fazer diagnóstico",
            30,
            "Runway para cenas, FFmpeg para montagem",
            "image",
            "https://assets.example/personagem-autorizada.png",
            "https://assets.example/performance-autorizada.mp4",
            12,
            "consentimento-91",
            "direitos-91",
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
    assertThat(result.videoCategory()).isEqualTo("COMMERCIAL_SHORT");
    assertThat(result.storyText()).contains("presença digital");
    assertThat(result.referencePerformanceDurationSeconds()).isEqualTo(12);
    assertThat(result.performanceConsentEvidence()).isEqualTo("consentimento-91");
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
            null,
            "musa-campanha",
            "LONG_FORM",
            "CAMPAIGN",
            "MONTAGE",
            "META_ADS",
            "VERTICAL_9_16",
            "Video de campanha",
            "Aumentar compra do PDE",
            "A usuária sai de tentativa manual para uma rotina guiada por IA.",
            "CONVERSION",
            "PURCHASE",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Gancho",
            "Roteiro",
            "Cenas",
            "Referencias",
            "Personagem principal com imagens aprovadas em multiplos angulos.",
            "Ambiente principal com imagem mestra, angulo oposto e mapa simples.",
            "Produto, interface e objetos de prova salvos como referencias separadas.",
            "Direcao visual cinematografica, vertical, realista e com luz consistente.",
            "Solicitar imagens mestre na OpenAI antes dos takes e usar como referencia por cena.",
            "Preservar rosto, figurino, ambiente, objetos, escala, lente e temperatura de cor.",
            "Voz",
            "Trilha",
            "Legendas",
            "Comprar agora",
            210,
            "Provider plan",
            null,
            null,
            null,
            null,
            null,
            null,
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
    assertThat(result.commercialPlanId()).isNull();
  }

  /** Permite projeto comercial curto quando a categoria declara esse uso no funil. */
  @Test
  void shouldCreateCommercialShortVideoProjectWithThirtySeconds() {
    CreateVideoProjectRequest request =
        new CreateVideoProjectRequest(
            4L,
            99L,
            66L,
            12L,
            "musa-organico-001",
            "COMMERCIAL_SHORT",
            "ORGANIC",
            "MIXED_AI_SCENES",
            "INSTAGRAM_REELS",
            "VERTICAL_9_16",
            "Corte orgânico de presença visual",
            "Gerar clique qualificado para o diagnóstico MUSA",
            "Uma profissional percebe que sua presença digital não traduz sua autoridade.",
            "AWARENESS",
            "PROFILE_VISIT",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Sua imagem comunica antes da sua fala",
            "Roteiro curto",
            "Cena 1",
            "Visual premium",
            null,
            null,
            null,
            null,
            null,
            null,
            "Voz feminina",
            "Trilha leve",
            "Legenda curta",
            "Fazer diagnóstico",
            30,
            "Runway para cenas, FFmpeg para montagem",
            null,
            null,
            null,
            null,
            null,
            null,
            "Corte rápido",
            "Audio audível e CTA claro",
            VideoProjectStatus.READY_FOR_SCRIPT,
            "editor@marketinghub.io");
    given(repository.save(any(VideoProject.class)))
        .willAnswer(
            invocation -> {
              VideoProject project = invocation.getArgument(0);
              project.setId(92L);
              return project;
            });

    VideoProjectDto result = service.createProject(request);

    assertThat(result.videoCategory()).isEqualTo("COMMERCIAL_SHORT");
    assertThat(result.targetDurationSeconds()).isEqualTo(30);
  }

  /** Bloqueia edição que tente reduzir projeto longo para menos de três minutos. */
  @Test
  void shouldRejectUpdateThatMakesLongFormProjectShorterThanThreeMinutes() {
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
            99L,
            null,
            null,
            "musa-campanha",
            "LONG_FORM",
            "CAMPAIGN",
            "MONTAGE",
            "META_ADS",
            "VERTICAL_9_16",
            "Video de campanha",
            "Aumentar compra do PDE",
            "A usuária sai de tentativa manual para uma rotina guiada por IA.",
            "CONVERSION",
            "PURCHASE",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Gancho",
            "Roteiro",
            "Cenas",
            "Referencias",
            null,
            null,
            null,
            null,
            null,
            null,
            "Voz",
            "Trilha",
            "Legendas",
            "Comprar agora",
            120,
            "Provider plan",
            null,
            null,
            null,
            null,
            null,
            null,
            "Notas",
            "Gate",
            VideoProjectStatus.READY_FOR_RENDER,
            "editor@marketinghub.io");
    given(repository.findById(91L)).willReturn(Optional.of(project));

    assertThatThrownBy(() -> service.updateProject(91L, request))
        .hasMessageContaining("180 segundos ou mais");
  }

  /** Bloqueia renderização sem bíblia visual completa para preservar consistência premium. */
  @Test
  void shouldRejectRenderStatusWithoutCompleteVisualBible() {
    CreateVideoProjectRequest request =
        new CreateVideoProjectRequest(
            4L,
            99L,
            66L,
            12L,
            "musa-organico-001",
            "LONG_FORM",
            "PDE",
            "STORY_FIRST_AUDIO_VIDEO",
            "PDE_AND_SOCIAL",
            "VERTICAL_9_16",
            "Manifesto MUSA",
            "Aumentar inicio do diagnostico",
            "Historia premium.",
            "AWARENESS",
            "DIAGNOSTIC_START",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Gancho",
            "Roteiro",
            "Cenas",
            "Referencias soltas",
            "Personagem definido",
            null,
            "Produto definido",
            "Estilo definido",
            "OpenAI gera imagens mestre",
            "Continuidade definida",
            "Voz",
            "Trilha",
            "Legendas",
            "Fazer diagnostico",
            180,
            "Provider plan",
            null,
            null,
            null,
            null,
            null,
            null,
            "Notas",
            "Gate",
            VideoProjectStatus.READY_FOR_RENDER,
            "editor@marketinghub.io");

    assertThatThrownBy(() -> service.createProject(request))
        .hasMessageContaining("Bíblia visual completa");
  }

  /** Mantém a listagem leve e entrega os cartões somente ao abrir o detalhe do projeto. */
  @Test
  void shouldLoadResearchIntelligenceOnlyOnProjectDetail() {
    VideoProject project =
        VideoProject.builder()
            .id(91L)
            .tenantId("tenant-musa")
            .productId(4L)
            .experimentId(91L)
            .contextType("PDE")
            .productionMode("CINEMATIC_SCENE_BLUEPRINT")
            .targetChannel("INSTAGRAM_REELS_STORIES")
            .format("VERTICAL_9_16")
            .title("Vega #91 - O espelho antes de sair")
            .objective("Gerar clique qualificado para o diagnóstico")
            .status(VideoProjectStatus.READY_FOR_SCRIPT)
            .build();
    ResearchIntelligenceSelectionDto selection =
        new ResearchIntelligenceSelectionDto(
            "HARNESS_RESEARCH_INTELLIGENCE_V1",
            "a".repeat(64),
            56,
            List.of(
                route("communication-director"),
                route("videomaker"),
                route("customer-agent"),
                route("meta-ad-approver")),
            List.of("Artigos não comprovam venda."));
    given(repository.findByTenantIdOrderByUpdatedAtDesc("tenant-musa"))
        .willReturn(List.of(project));
    given(repository.findById(91L)).willReturn(Optional.of(project));
    given(researchIntelligenceMapper.selectForVideoProject(project)).willReturn(selection);

    assertThat(service.listProjects())
        .singleElement()
        .extracting(VideoProjectDto::researchIntelligence)
        .isNull();
    assertThat(service.getProject(91L).researchIntelligence()).isNotNull();
    assertThat(service.getProject(91L).researchIntelligence().routes()).hasSize(4);
  }

  /** Cria uma rota mínima pertencente ao contrato do próprio SalesVideo. */
  private ResearchIntelligenceSelectionDto.Route route(String agentKey) {
    return new ResearchIntelligenceSelectionDto.Route(
        agentKey, agentKey, "Orientar vídeo", "REVIEW_CRITERIA_ONLY", "Seleção local", List.of());
  }
}
