package com.marketinghub.creative.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.WebConfig;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.mapper.CreativeMapper;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.service.TemisCreativeTaskOrchestrationService;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetUploadCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Responsabilidade: validar o contrato CORS dos endpoints de ativos de criativos. */
@WebMvcTest(CreativeController.class)
@Import(WebConfig.class)
class CreativeControllerCorsTest {

  private static final String REQUEST_ORIGIN = "http://app.local";

  @Autowired private MockMvc mockMvc;

  @MockBean private CreativeService creativeService;

  @MockBean private CreativeMapper creativeMapper;

  @MockBean private AssetRepository assetRepository;

  @MockBean private TemisCreativeTaskOrchestrationService temisCreativeTaskOrchestrationService;

  @Test
  void uploadImageRespondsWithCorsHeaders() throws Exception {
    AssetUploadResponse mockResponse =
        new AssetUploadResponse(
            "https://cdn.test/mock.png",
            "experiments/creatives/mock.png",
            AssetUploadCategory.EXPERIMENT_CREATIVE);
    when(creativeService.uploadImage(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mockResponse);

    MockMultipartFile file =
        new MockMultipartFile("file", "image.png", "image/png", "dummy".getBytes());

    mockMvc
        .perform(
            multipart("/api/assets")
                .file(file)
                .param("prompt", "simple-form-test")
                .header(HttpHeaders.ORIGIN, REQUEST_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REQUEST_ORIGIN))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
  }

  @Test
  void uploadLargeImageStillHonorsCorsHeaders() throws Exception {
    AssetUploadResponse largeResponse =
        new AssetUploadResponse(
            "https://cdn.test/large.png",
            "experiments/creatives/large.png",
            AssetUploadCategory.EXPERIMENT_CREATIVE);
    when(creativeService.uploadImage(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(largeResponse);

    byte[] largePayload = new byte[4 * 1024 * 1024];
    MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largePayload);

    mockMvc
        .perform(
            multipart("/api/assets")
                .file(file)
                .param("prompt", "large-image")
                .header(HttpHeaders.ORIGIN, REQUEST_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REQUEST_ORIGIN));
  }

  @Test
  void preflightRequestIncludesCorsMetadata() throws Exception {
    mockMvc
        .perform(
            options("/api/assets")
                .header(HttpHeaders.ORIGIN, REQUEST_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REQUEST_ORIGIN))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
  }

  /** Comprova que o agente pode enviar a arte binária sem depender de URL informada por pessoa. */
  @Test
  void temisCanUploadImprovementArtifact() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "temis.png", "image/png", "generated-image".getBytes());

    mockMvc
        .perform(
            multipart("/api/internal/creatives/326/agent-improvement/artifact")
                .file(file)
                .param("model", "gpt-image-2")
                .param("producerExecutionId", "temis-producer-326")
                .param("requestJson", "{\"prompt\":\"Demonstre o produto digital\"}")
                .param("responseJson", "{\"data\":[{\"b64_json\":\"auditado\"}]}")
                .param("usageJson", "{\"input_tokens\":12}")
                .param("costUsd", "0.15"))
        .andExpect(status().isOk());

    verify(creativeService)
        .uploadAgentImprovementArtifact(
            org.mockito.ArgumentMatchers.eq(326L),
            any(),
            org.mockito.ArgumentMatchers.eq("gpt-image-2"),
            org.mockito.ArgumentMatchers.eq("temis-producer-326"),
            org.mockito.ArgumentMatchers.contains("Demonstre o produto digital"),
            org.mockito.ArgumentMatchers.contains("b64_json"),
            org.mockito.ArgumentMatchers.contains("input_tokens"),
            org.mockito.ArgumentMatchers.eq(new java.math.BigDecimal("0.15")));
  }
}
