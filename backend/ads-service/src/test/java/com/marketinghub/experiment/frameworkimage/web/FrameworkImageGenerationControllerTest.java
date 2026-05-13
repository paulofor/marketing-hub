package com.marketinghub.experiment.frameworkimage.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationItemStatusDto;
import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationSummaryDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = FrameworkImageGenerationController.class)
class FrameworkImageGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrameworkImageGenerationService service;

    @Test
    void generateCreatesJobs() throws Exception {
        when(service.enqueueJobsForExperiment(41L)).thenReturn(List.of(FrameworkImageGenerationJobDto.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .planningItemKey("hero")
                .status("PENDING")
                .stage("WAITING_AI_WORKER")
                .build()));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/experiments/41/framework-images/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].planningItemKey").value("hero"));

        verify(service).enqueueJobsForExperiment(41L);
    }

    @Test
    void listReturnsProgressByPlanningItem() throws Exception {
        when(service.listJobsByExperiment(8L)).thenReturn(List.of(
                new FrameworkImageGenerationItemStatusDto(
                        "hero",
                        "Hero",
                        "prompt",
                        UUID.randomUUID(),
                        "PROCESSING",
                        "CLAIMED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                new FrameworkImageGenerationItemStatusDto(
                        "faq",
                        "FAQ",
                        "prompt faq",
                        null,
                        "PLANNED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/experiments/8/framework-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$[1].status").value("PLANNED"));

        verify(service).listJobsByExperiment(8L);
    }

    @Test
    void summaryReturnsAggregatedCounters() throws Exception {
        when(service.summarizeJobsByExperiment(20L)).thenReturn(
                new FrameworkImageGenerationSummaryDto(8, 0, 8, 8, 0, 0, Instant.parse("2026-05-13T20:09:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/experiments/20/framework-images/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(8))
                .andExpect(jsonPath("$.processingCount").value(8))
                .andExpect(jsonPath("$.waitingOpenAiBatchCount").value(8));

        verify(service).summarizeJobsByExperiment(20L);
    }
}
