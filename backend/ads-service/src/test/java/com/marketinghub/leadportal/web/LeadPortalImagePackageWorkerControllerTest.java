package com.marketinghub.leadportal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageFailureRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImagePackageDto;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageRetryRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest;
import com.marketinghub.leadportal.service.LeadPortalImagePackageWorkerService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Testes unitários para o controller de integração com o worker do Lead Portal.
 */
@WebMvcTest(controllers = LeadPortalImagePackageWorkerController.class)
class LeadPortalImagePackageWorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeadPortalImagePackageWorkerService workerService;

    @BeforeEach
    void setup() {
        doNothing().when(workerService).markProcessing(any(Long.class));
        doNothing().when(workerService).markFailed(any(Long.class), any(String.class));
        doNothing().when(workerService).retry(any(Long.class), any(String.class));
        doNothing().when(workerService).submitResults(any(Long.class), any(LeadPortalWorkerImageResultRequest.class));
    }

    @Test
    void listRecentReturnsPackages() throws Exception {
        LeadPortalWorkerImagePackageDto dto = new LeadPortalWorkerImagePackageDto(
                10L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "original.png",
                5,
                2,
                "gpt-image",
                "Prompt base",
                "Tratar variante A",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        when(workerService.listRecentPackages()).thenReturn(List.of(dto));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/worker/image-packages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].submission_id").value("123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(jsonPath("$[0].stored_file_name").value("original.png"));

        verify(workerService).listRecentPackages();
    }

    @Test
    void startProcessingCallsService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/start", 42L))
                .andExpect(status().isNoContent());

        verify(workerService).markProcessing(42L);
    }

    @Test
    void markFailedForwardsPayload() throws Exception {
        String body = objectMapper.writeValueAsString(new LeadPortalWorkerImageFailureRequest("Erro qualquer"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/fail", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(workerService).markFailed(100L, "Erro qualquer");
    }

    @Test
    void retryDelegatesToService() throws Exception {
        String body = objectMapper.writeValueAsString(new LeadPortalWorkerImageRetryRequest("Erro temporário"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/retry", 55L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(workerService).retry(55L, "Erro temporário");
    }

    @Test
    void markFailedWithBlankReasonReturnsBadRequest() throws Exception {
        String body = "{\"reason\":\"\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/fail", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(workerService, never()).markFailed(any(Long.class), any(String.class));
    }

    @Test
    void submitResultsDelegatesToService() throws Exception {
        String body = """
                {
                  "model": "gpt-image",
                  "prompt": "prompt final",
                  "images": [
                    {
                      "stored_file_name": "file-1.png",
                      "public_url": "https://cdn/example1.png",
                      "model": "gpt-image",
                      "prompt": "prompt individual",
                      "source": "openai"
                    }
                  ]
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/results", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<LeadPortalWorkerImageResultRequest> captor = ArgumentCaptor.forClass(LeadPortalWorkerImageResultRequest.class);
        verify(workerService).submitResults(eq(5L), captor.capture());
        LeadPortalWorkerImageResultRequest captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, captured.images().size());
        org.junit.jupiter.api.Assertions.assertEquals("prompt final", captured.prompt());
    }

    @Test
    void submitResultsWithoutImagesFailsValidation() throws Exception {
        String body = """
                {
                  "model": "gpt-image",
                  "prompt": "prompt final",
                  "images": []
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/worker/image-packages/{id}/results", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(workerService, never()).submitResults(any(Long.class), any(LeadPortalWorkerImageResultRequest.class));
    }
}
