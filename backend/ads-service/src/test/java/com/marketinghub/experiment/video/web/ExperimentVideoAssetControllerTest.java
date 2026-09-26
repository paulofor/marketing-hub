package com.marketinghub.experiment.video.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.experiment.video.dto.UploadExperimentAdVideoRequest;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import com.marketinghub.experiment.video.service.ExperimentVideoPerformanceDashboardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP administrativo dos vídeos vinculados a experimentos. */
@WebMvcTest(ExperimentVideoAssetController.class)
class ExperimentVideoAssetControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private ExperimentVideoAssetService service;
  @MockBean private ExperimentVideoPerformanceDashboardService performanceDashboardService;

  /** Recebe o MP4 e encaminha todos os critérios comerciais e de proveniência ao serviço. */
  @Test
  void shouldUploadVersionedAdVideo() throws Exception {
    byte[] mp4Bytes = new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
    MockMultipartFile file = new MockMultipartFile("file", "capella-v1.mp4", "video/mp4", mp4Bytes);
    when(service.uploadUserAdVideo(eq(94L), any(), any())).thenReturn(null);

    mockMvc
        .perform(
            multipart("/api/experiments/94/video-assets/ad-uploads")
                .file(file)
                .header("X-Tenant-ID", "default")
                .param("objective", "Comparar vídeo com estático")
                .param("primaryMetric", "Compras líquidas")
                .param("script", "Seu trabalho é caprichado.")
                .param("durationSeconds", "18")
                .param("hasAudio", "true")
                .param("visualSourceKey", "capella-exp88-approved-assets-v1")
                .param("visualSourceDescription", "Posts e stories aprovados do #88")
                .param(
                    "productionReference", "scripts/marketing/create-capella-successor-video-v1.sh")
                .param("requiredForRelease", "true"))
        .andExpect(status().isCreated());

    ArgumentCaptor<UploadExperimentAdVideoRequest> request =
        ArgumentCaptor.forClass(UploadExperimentAdVideoRequest.class);
    verify(service).uploadUserAdVideo(eq(94L), any(), request.capture());
    assertThat(request.getValue().durationSeconds()).isEqualTo(18);
    assertThat(request.getValue().hasAudio()).isTrue();
    assertThat(request.getValue().visualSourceKey()).isEqualTo("capella-exp88-approved-assets-v1");
  }
}
