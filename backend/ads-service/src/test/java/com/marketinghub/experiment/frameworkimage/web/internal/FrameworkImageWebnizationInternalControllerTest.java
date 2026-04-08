package com.marketinghub.experiment.frameworkimage.web.internal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageWebnizationPendingAssetDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = FrameworkImageWebnizationInternalController.class)
class FrameworkImageWebnizationInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrameworkImageGenerationService service;

    @Test
    void listPendingWebnizationReturnsAssets() throws Exception {
        UUID jobId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(service.listPendingWebnizationAssets(20)).thenReturn(List.of(new FrameworkImageWebnizationPendingAssetDto(
                jobId,
                88L,
                "hero",
                700L,
                "https://cdn/source.jpg",
                Instant.parse("2026-04-08T10:00:00Z"))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/internal/framework-image/assets/pending-webnization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(jobId.toString()))
                .andExpect(jsonPath("$[0].assetId").value(700L));
    }

    @Test
    void webReadyDelegatesToService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/internal/framework-image/assets/{assetId}/web-ready", 700L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webUrl\":\"https://cdn/final.webp\"}"))
                .andExpect(status().isOk());

        verify(service).markAssetAsWebReady(eq(700L), eq("https://cdn/final.webp"));
    }
}
