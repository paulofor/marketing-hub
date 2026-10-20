package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.service.VideoProductProofService;
import com.marketinghub.salesvideo.service.VideoProductProofSource;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Impede leitura de pixels fora da mídia e do experimento efetivamente reservados para Têmis. */
class CreativeEditorialEvidenceServiceTest {
  /** Serve a mesma captura somente no escopo auditado, com outra identidade e falhas isoladas. */
  @ParameterizedTest
  @ValueSource(longs = {540, 9540})
  void bindsPixelsToCurrentEditorialSources(long id) throws Exception {
    var creatives = mock(CreativeRepository.class);
    var governance = mock(CreativeMediaGovernanceEvidenceService.class);
    var projects = mock(VideoProjectRepository.class);
    var storage = mock(VideoProductProofSource.class);
    var proofs = new VideoProductProofService(storage, projects, new ObjectMapper());
    var reader = new CreativeEditorialEvidenceService(creatives, governance, proofs, projects);
    long experimentId = id + 1,
        productId = id + 2,
        projectId = id + 3,
        taskId = id + 4,
        evidenceId = id + 5;
    var creative =
        Creative.builder()
            .id(id)
            .experiment(
                Experiment.builder()
                    .id(experimentId)
                    .product(Product.builder().id(productId).build())
                    .build())
            .build();
    byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
    var json = new ObjectMapper().findAndRegisterModules();
    var data =
        json.readTree(
            """
        {"contractVersion":"CREATIVE_MEDIA_GOVERNANCE_V5","verificationStatus":"VERIFIED",
         "editorialMediaEvidence":{"projectId":%d,"productId":%d,"experimentId":%d,
             "productVersion":"fixture-v1",
             "productProofSources":[{"taskId":%d,"evidenceId":%d,"tenantId":"fixture-tenant","sha256":"%s"}],
           "deliverySources":[],"blockers":[]}}
        """
                .formatted(projectId, productId, experimentId, taskId, evidenceId, hash));
    when(creatives.findByIdWithExperiment(id)).thenReturn(Optional.of(creative));
    when(governance.resolve(creative))
        .thenAnswer(x -> json.treeToValue(data, CreativeMediaGovernanceEvidenceDto.class));
    var project =
        VideoProject.builder()
            .id(projectId)
            .productId(productId)
            .experimentId(experimentId)
            .campaignKey("fixture-v1")
            .tenantId("fixture-tenant")
            .build();
    when(projects.findById(projectId)).thenReturn(Optional.of(project));
    String technical =
        """
        {"contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1","decision":"APPROVED",
          "sourceReference":"experiment:%d","productId":%d,"prototypeVersion":"fixture-v1",
          "trafficClass":"AGENT_VALIDATION","commercialEvidenceClaimed":false,
          "artifacts":[{"artifactId":%d,"sha256":"%s"}]}
        """
            .formatted(experimentId, productId, evidenceId, hash);
    when(storage.find(taskId, evidenceId))
        .thenReturn(
            Optional.of(
                new VideoProductProofSource.Proof(
                    taskId,
                    evidenceId,
                    "COMPLETED",
                    "experiment:" + experimentId,
                    technical,
                    hash)));
    when(storage.read(taskId, evidenceId)).thenReturn(png);
    com.marketinghub.salesvideo.tenant.TenantContextHolder.clear();
    assertThatThrownBy(() -> proofs.read(projectId, taskId, evidenceId))
        .hasMessageContaining("X-Tenant-ID");
    assertThat(reader.readProof(id, experimentId, taskId, evidenceId)).containsExactly(png);
    var controller =
        new com.marketinghub.creative.web.CreativeController(
            mock(CreativeService.class),
            mock(com.marketinghub.creative.mapper.CreativeMapper.class),
            mock(com.marketinghub.repository.jpa.media.AssetRepository.class),
            mock(com.marketinghub.creative.service.video.VideoCreativeService.class),
            mock(com.marketinghub.creative.service.visual.ApprovedVisualAssetCreativeService.class),
            reader);
    var http =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.marketinghub.web.ApiExceptionHandler())
            .build();
    http.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/internal/creatives/"
                        + id
                        + "/agent-review/product-proofs/"
                        + taskId
                        + "/"
                        + evidenceId)
                .param("experimentId", Long.toString(experimentId)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(png));
    try {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(
          new com.marketinghub.salesvideo.tenant.TenantContext("other-tenant", "fixture", false));
      assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId))
          .hasMessageContaining("tenant ativo");
    } finally {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.clear();
    }
    assertThatThrownBy(() -> reader.readProof(id, experimentId + 1, taskId, evidenceId))
        .hasMessageContaining("404");
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId + 1, evidenceId))
        .hasMessageContaining("404");
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId + 1))
        .hasMessageContaining("404");
    ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("verificationStatus", "INCOMPLETE");
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId))
        .hasMessageContaining("409");
    ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("verificationStatus", "VERIFIED");
    ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("editorialMediaEvidence"))
        .put("productId", productId + 1);
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId))
        .hasMessageContaining("409");
    ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("editorialMediaEvidence"))
        .put("productId", productId);
    when(storage.read(taskId, evidenceId)).thenReturn(new byte[] {1, 2, 3});
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId))
        .hasMessageContaining("PNG");
    byte[] altered = png.clone();
    altered[10] = 9;
    when(storage.read(taskId, evidenceId)).thenReturn(altered);
    assertThatThrownBy(() -> reader.readProof(id, experimentId, taskId, evidenceId))
        .hasMessageContaining("Pixels");
  }
}
