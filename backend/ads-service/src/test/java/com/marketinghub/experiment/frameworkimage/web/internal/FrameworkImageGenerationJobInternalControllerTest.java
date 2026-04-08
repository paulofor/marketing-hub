package com.marketinghub.experiment.frameworkimage.web.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = FrameworkImageGenerationJobInternalController.class)
class FrameworkImageGenerationJobInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FrameworkImageGenerationService service;

    @BeforeEach
    void setup() {
        doNothing().when(service).updateJobStage(any(UUID.class), any(FrameworkImageGenerationJobStage.class));
        doNothing().when(service).completeJob(any(UUID.class), any(FrameworkImageGenerationJobCompletionRequest.class));
        doNothing().when(service).failJob(any(UUID.class), any(String.class));
    }

    @Test
    void listPendingReturnsJobs() throws Exception {
        UUID jobId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        FrameworkImageGenerationJobDto dto = FrameworkImageGenerationJobDto.builder()
                .id(jobId)
                .experimentId(15L)
                .planningItemKey("hero-1")
                .status("PENDING")
                .stage("WAITING_AI_WORKER")
                .build();

        when(service.listPendingJobs(10)).thenReturn(List.of(dto));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/internal/framework-image/jobs/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$[0].planningItemKey").value("hero-1"));
    }

    @Test
    void claimDelegatesToService() throws Exception {
        UUID jobId = UUID.randomUUID();
        FrameworkImageGenerationJobDto dto = FrameworkImageGenerationJobDto.builder()
                .id(jobId)
                .experimentId(20L)
                .planningItemKey("item-2")
                .status("PROCESSING")
                .stage("CLAIMED")
                .workerId("worker-a")
                .build();
        when(service.claimJob(jobId, "worker-a")).thenReturn(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/internal/framework-image/jobs/{jobId}/claim", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("CLAIMED"));

        verify(service).claimJob(jobId, "worker-a");
    }

    @Test
    void failValidatesBody() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/internal/framework-image/jobs/{jobId}/fail", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"errorMessage\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).failJob(eq(jobId), any(String.class));
    }

    @Test
    void completeDelegatesToService() throws Exception {
        UUID jobId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new FrameworkImageGenerationJobCompletionRequest(
                FrameworkImageGenerationJobStage.NOTIFIED_BACKEND,
                "gpt-image-1",
                "prompt final",
                "batch_1",
                77L,
                "https://cdn/source.jpg",
                "https://cdn/web.jpg"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/internal/framework-image/jobs/{jobId}/complete", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(service).completeJob(eq(jobId), any(FrameworkImageGenerationJobCompletionRequest.class));
    }
}
