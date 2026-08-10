package com.marketinghub.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.AgentVersion;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a persistência e a auditoria do contrato operacional dos agentes. */
class AgentServiceTest {

  /** Persiste uma imagem válida no storage oficial e no catálogo de assets. */
  @Test
  void uploadsValidPortrait() throws Exception {
    AssetStorageService storage = mock(AssetStorageService.class);
    AssetRepository assetRepository = mock(AssetRepository.class);
    when(storage.store(any(), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "agents/portrait.png", "https://assets.test/portrait.png", 3, "image/png", true));
    when(assetRepository.save(any(com.marketinghub.media.Asset.class)))
        .thenAnswer(
            invocation -> {
              com.marketinghub.media.Asset asset = invocation.getArgument(0);
              asset.setId(42L);
              return asset;
            });
    AgentService service =
        new AgentService(
            mock(AgentRepository.class),
            mock(AgentThemeService.class),
            mock(AgentVersionRepository.class),
            new ObjectMapper(),
            assetRepository,
            storage);

    var response =
        service.uploadPortrait(
            new MockMultipartFile(
                "file",
                "portrait.png",
                "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}));

    assertThat(response.assetId()).isEqualTo(42L);
    assertThat(response.url()).isEqualTo("https://assets.test/portrait.png");
  }

  /** Impede que conteúdo arbitrário seja salvo como imagem de agente. */
  @Test
  void rejectsUnsupportedPortraitFile() {
    AgentService service =
        new AgentService(
            mock(AgentRepository.class),
            mock(AgentThemeService.class),
            mock(AgentVersionRepository.class),
            new ObjectMapper(),
            mock(AssetRepository.class),
            mock(AssetStorageService.class));

    assertThatThrownBy(
            () ->
                service.uploadPortrait(
                    new MockMultipartFile("file", "script.svg", "image/svg+xml", "x".getBytes())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("PNG, JPEG ou WebP");
  }

  /** Impede que um arquivo arbitrário contorne a validação declarando content type de imagem. */
  @Test
  void rejectsSpoofedPortraitContent() {
    AgentService service =
        new AgentService(
            mock(AgentRepository.class),
            mock(AgentThemeService.class),
            mock(AgentVersionRepository.class),
            new ObjectMapper(),
            mock(AssetRepository.class),
            mock(AssetStorageService.class));

    assertThatThrownBy(
            () ->
                service.uploadPortrait(
                    new MockMultipartFile("file", "fake.png", "image/png", "script".getBytes())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não corresponde a uma imagem válida");
  }

  /** Confirma que as regras administrativas entram no agente e em sua versão imutável. */
  @Test
  void createsVersionedOperatingContract() {
    AgentRepository repository = mock(AgentRepository.class);
    AgentVersionRepository versionRepository = mock(AgentVersionRepository.class);
    AgentThemeService themeService = mock(AgentThemeService.class);
    AgentTheme theme = new AgentTheme();
    theme.setId(7L);
    when(themeService.get(7L)).thenReturn(theme);
    when(repository.save(any(Agent.class)))
        .thenAnswer(
            invocation -> {
              Agent saved = invocation.getArgument(0);
              saved.setId(11L);
              return saved;
            });

    SaveAgentRequest request = new SaveAgentRequest();
    request.setName("Especialista comercial");
    request.setNickname("Closer");
    request.setAgentKey("commercial-specialist");
    request.setExecutionMode("DECISION_GATE");
    request.setThemeId(7L);
    request.setResponsibilityContract("Avaliar a viabilidade comercial.");
    request.setOrchestratorPolicy("Acionar após evidências mínimas; bloquear gasto.");
    request.setAnalysisPolicy("Comparar conversão, risco e evidências.");
    request.setOfferingPolicy("Entregar parecer e próximo teste.");

    AgentService service =
        new AgentService(
            repository,
            themeService,
            versionRepository,
            new ObjectMapper(),
            mock(AssetRepository.class),
            mock(AssetStorageService.class));
    Agent saved = service.create(request);

    assertThat(saved.getResponsibilityContract()).isEqualTo(request.getResponsibilityContract());
    assertThat(saved.getNickname()).isEqualTo("Closer");
    assertThat(saved.getOrchestratorPolicy()).isEqualTo(request.getOrchestratorPolicy());
    assertThat(saved.getAnalysisPolicy()).isEqualTo(request.getAnalysisPolicy());
    assertThat(saved.getOfferingPolicy()).isEqualTo(request.getOfferingPolicy());

    ArgumentCaptor<AgentVersion> version = ArgumentCaptor.forClass(AgentVersion.class);
    verify(versionRepository).save(version.capture());
    assertThat(version.getValue().getContractSnapshot())
        .contains(
            "nickname",
            "Closer",
            "responsibilityContract",
            "orchestratorPolicy",
            "analysisPolicy",
            "offeringPolicy");
  }

  /** Impede que dois agentes sejam conhecidos pelo mesmo apelido. */
  @Test
  void rejectsDuplicatedNickname() {
    AgentRepository repository = mock(AgentRepository.class);
    when(repository.existsByNicknameIgnoreCase("Closer")).thenReturn(true);
    AgentService service =
        new AgentService(
            repository,
            mock(AgentThemeService.class),
            mock(AgentVersionRepository.class),
            new ObjectMapper(),
            mock(AssetRepository.class),
            mock(AssetStorageService.class));
    SaveAgentRequest request = new SaveAgentRequest();
    request.setNickname(" Closer ");

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Já existe um agente com este apelido");
  }
}
