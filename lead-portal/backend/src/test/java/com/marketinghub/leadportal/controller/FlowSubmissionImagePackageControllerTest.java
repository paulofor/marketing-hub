package com.marketinghub.leadportal.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.leadportal.entity.FlowSubmissionEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlowSubmissionImagePackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlowSubmissionRepository submissionRepository;

    @Autowired
    private FlowSubmissionImagePackageRepository imagePackageRepository;

    @BeforeEach
    void setup() {
        imagePackageRepository.deleteAll();
        submissionRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
        imagePackageRepository.deleteAll();
        submissionRepository.deleteAll();
    }

    @Test
    void listPendingPackagesReturnsRecentAndReceived() throws Exception {
        FlowSubmissionEntity recentSubmission = saveSubmission("fluxo-a");
        FlowSubmissionEntity receivedSubmission = saveSubmission("fluxo-b");
        FlowSubmissionEntity processingSubmission = saveSubmission("fluxo-c");

        FlowSubmissionImagePackageEntity recentPackage = saveImagePackage(
                recentSubmission.getId(), FlowSubmissionImagePackageEntity.Status.RECENT.name(), "Modelo A", "Prompt A");
        FlowSubmissionImagePackageEntity receivedPackage = saveImagePackage(
                receivedSubmission.getId(), FlowSubmissionImagePackageEntity.Status.RECEIVED.name(), "Modelo B", "Prompt B");
        saveImagePackage(
                processingSubmission.getId(),
                FlowSubmissionImagePackageEntity.Status.PROCESSING.name(),
                "Modelo C",
                "Prompt C");

        mockMvc.perform(get("/api/image-packages/pending").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(recentPackage.getId()))
                .andExpect(jsonPath("$[0].submissionId").value(recentSubmission.getId().toString()))
                .andExpect(jsonPath("$[0].flowSlug").value(recentSubmission.getFlowSlug()))
                .andExpect(jsonPath("$[0].status").value("RECENT"))
                .andExpect(jsonPath("$[1].id").value(receivedPackage.getId()))
                .andExpect(jsonPath("$[1].flowSlug").value(receivedSubmission.getFlowSlug()))
                .andExpect(jsonPath("$[1].status").value("RECEIVED"));
    }

    private FlowSubmissionEntity saveSubmission(String flowSlug) {
        FlowSubmissionEntity submission = new FlowSubmissionEntity();
        submission.setId(UUID.randomUUID());
        submission.setFlowSlug(flowSlug);
        submission.setName("Cliente");
        submission.setEmail("cliente@example.com");
        submission.setAnswers(Map.of());
        submission.setCreatedAt(Instant.now());
        return submissionRepository.save(submission);
    }

    private FlowSubmissionImagePackageEntity saveImagePackage(
            UUID submissionId, String status, String model, String prompt) {
        FlowSubmissionImagePackageEntity imagePackage = new FlowSubmissionImagePackageEntity();
        imagePackage.setSubmissionId(submissionId);
        imagePackage.setStatus(status);
        imagePackage.setModel(model);
        imagePackage.setPrompt(prompt);
        return imagePackageRepository.save(imagePackage);
    }
}
