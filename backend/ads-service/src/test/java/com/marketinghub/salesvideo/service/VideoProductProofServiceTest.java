package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Garante que vídeos usem somente pixels homologados no contexto e versão exatos. */
class VideoProductProofServiceTest {
  private VideoProductProofService service;
  private VideoProject project;
  private AgentTask task;
  private VideoProductProofSource repository;

  /** Prepara prova sintética segregada, sem banco, storage ou IA externos. */
  @BeforeEach
  void setUp() {
    repository = mock(VideoProductProofSource.class);
    service =
        new VideoProductProofService(
            repository, mock(VideoProjectRepository.class), new ObjectMapper());
    project =
        VideoProject.builder()
            .id(91001L)
            .productId(91002L)
            .experimentId(91003L)
            .campaignKey("pde-v12")
            .referencePerformanceUri(
                "internal://agent-tasks/91004/visual-evidence/91005#crop=10,20,300,400")
            .build();
    task = new AgentTask();
    task.setId(91004L);
    task.setStatus("COMPLETED");
    task.setSourceReference("experiment:91003");
    task.setResultJson(
        """
        {"contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1","decision":"APPROVED",
         "productId":91002,"prototypeVersion":"pde-v12","trafficClass":"AGENT_VALIDATION",
         "commercialEvidenceClaimed":false,"artifacts":[{"artifactId":91005,"sha256":"abc"}]}
        """);
    var proof = new AgentTaskVisualEvidence();
    proof.setId(91005L);
    proof.setTask(task);
    proof.setSha256("abc");
    when(repository.find(91004L, 91005L))
        .thenAnswer(
            invocation ->
                Optional.of(
                    new VideoProductProofSource.Proof(
                        task.getId(),
                        proof.getId(),
                        task.getStatus(),
                        task.getSourceReference(),
                        task.getResultJson(),
                        proof.getSha256())));
  }

  /** Congela hash, rota interna e enquadramento sem conceder comprovação comercial. */
  @Test
  void resolvesOnlyApprovedPrivateProof() {
    assertThat(service.resolve(project))
        .containsEntry("sha256", "abc")
        .containsEntry("contentPath", "/api/sales-videos/projects/91001/product-proof")
        .containsEntry("commercialEvidenceClaimed", false)
        .containsEntry("crop", java.util.List.of(10, 20, 300, 400));
  }

  /** Bloqueia mistura de versão, produto, experimento, status e resultado reprovado. */
  @ParameterizedTest
  @ValueSource(
      strings = {"version", "product", "experiment", "status", "decision", "hash", "commercial"})
  void rejectsInvalidOwnership(String field) {
    switch (field) {
      case "version" -> project.setCampaignKey("pde-v11");
      case "product" -> project.setProductId(4L);
      case "experiment" -> project.setExperimentId(92L);
      case "status" -> task.setStatus("BLOCKED");
      case "decision" -> task.setResultJson(task.getResultJson().replace("APPROVED", "REJECTED"));
      case "hash" -> task.setResultJson(task.getResultJson().replace("abc", "different"));
      case "commercial" -> task.setResultJson(task.getResultJson().replace("false", "true"));
    }
    assertThatThrownBy(() -> service.resolve(project))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  /** Recusa outro tenant antes de ler pixels e permite a leitura pelo proprietário. */
  @Test
  void readsPixelsOnlyWithinTheProjectTenant() {
    var projects = mock(VideoProjectRepository.class);
    when(projects.findById(91001L)).thenReturn(Optional.of(project));
    var reader = new VideoProductProofService(repository, projects, new ObjectMapper());
    try {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(
          new com.marketinghub.salesvideo.tenant.TenantContext("other", "fixture", false));
      assertThatThrownBy(() -> reader.read(91001L))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
      verify(repository, never()).read(any(), any());
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(
          new com.marketinghub.salesvideo.tenant.TenantContext("default", "fixture", false));
      when(repository.read(91004L, 91005L)).thenReturn(new byte[] {1, 2});
      assertThat(reader.read(91001L)).containsExactly(1, 2);
    } finally {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.clear();
    }
  }

  /** Preserva projetos sem prova interna sem procurar uma captura por semelhança ou por título. */
  @Test
  void doesNotInventProofForLegacyProject() {
    project.setReferencePerformanceUri(null);
    assertThat(service.resolve(project)).isEmpty();
    verifyNoInteractions(repository);
  }
}
