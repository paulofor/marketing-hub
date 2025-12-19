package com.marketinghub.leadportal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.leadportal.FlowSubmissionImagePackageLifecycleStatus;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageDetailDto;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageSummaryDto;
import com.marketinghub.leadportal.service.LeadPortalImagePackageService;
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

@WebMvcTest(controllers = LeadPortalImagePackageController.class)
class LeadPortalImagePackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeadPortalImagePackageService imagePackageService;

    @Test
    void listUsesProvidedStatuses() throws Exception {
        LeadPortalImagePackageSummaryDto dto = new LeadPortalImagePackageSummaryDto(
                1L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "flow",
                "Nome",
                "email@example.com",
                "+5511999999999",
                FlowSubmissionImagePackageStatus.FAILED,
                FlowSubmissionImagePackageLifecycleStatus.FAILED,
                "Prompt",
                "gpt-image",
                10,
                5,
                3,
                2,
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:05:00Z"),
                "Erro temporário",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        when(imagePackageService.listImagePackages(any())).thenReturn(List.of(dto));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/lead-portal/image-packages")
                        .param("status", FlowSubmissionImagePackageStatus.FAILED.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value(FlowSubmissionImagePackageStatus.FAILED.name()))
                .andExpect(jsonPath("$[0].lifecycleStatus").value(FlowSubmissionImagePackageLifecycleStatus.FAILED.name()));

        verify(imagePackageService).listImagePackages(new java.util.LinkedHashSet<>(List.of(FlowSubmissionImagePackageStatus.FAILED)));
    }

    @Test
    void retryDelegatesToService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/lead-portal/image-packages/{id}/retry", 77L))
                .andExpect(status().isNoContent());

        verify(imagePackageService).retry(77L);
    }

    @Test
    void getReturnsHistoryEntriesForEngagementEvents() throws Exception {
        LeadPortalImagePackageDetailDto detail = new LeadPortalImagePackageDetailDto(
                5L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                FlowSubmissionImagePackageStatus.COMPLETED,
                FlowSubmissionImagePackageLifecycleStatus.SAMPLE_IMAGES_VIEWED,
                "Prompt",
                "model",
                Integer.valueOf(4),
                Integer.valueOf(1),
                Integer.valueOf(1),
                null,
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:05:00Z"),
                List.of(
                        new LeadPortalImagePackageDetailDto.StatusHistoryEntry(
                                FlowSubmissionImagePackageStatus.SAMPLE_EMAIL_OPENED,
                                null,
                                Instant.parse("2024-01-01T10:06:00Z")),
                        new LeadPortalImagePackageDetailDto.StatusHistoryEntry(
                                FlowSubmissionImagePackageStatus.SAMPLE_IMAGES_VIEWED,
                                null,
                                Instant.parse("2024-01-01T10:07:00Z"))),
                null,
                new LeadPortalImagePackageDetailDto.SubmissionInfo(
                        "flow",
                        "Name",
                        "email@example.com",
                        "+5511999999999",
                        "img-question"),
                null,
                List.<LeadPortalImagePackageDetailDto.ImageReference>of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        when(imagePackageService.getImagePackage(5L)).thenReturn(detail);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lead-portal/image-packages/{id}", 5L)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].status")
                        .value(FlowSubmissionImagePackageStatus.SAMPLE_EMAIL_OPENED.name()))
                .andExpect(jsonPath("$.history[1].status")
                        .value(FlowSubmissionImagePackageStatus.SAMPLE_IMAGES_VIEWED.name()));

        verify(imagePackageService).getImagePackage(5L);
    }
}
