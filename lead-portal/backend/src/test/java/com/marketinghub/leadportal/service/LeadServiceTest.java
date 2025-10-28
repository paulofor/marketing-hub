package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.leadportal.config.StorageProperties;
import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import com.marketinghub.leadportal.storage.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LeadServiceTest {

    private Path tempDir;
    private LeadService leadService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("lead-service-test");
        StorageProperties properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        FileStorageService fileStorageService = new FileStorageService(properties);
        fileStorageService.init();
        leadService = new LeadService(fileStorageService);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    @Test
    void shouldCreateLeadAndStoreFile() {
        MockMultipartFile multipartFile =
                new MockMultipartFile("image", "example.png", "image/png", new byte[] {1, 2, 3});

        Lead lead = leadService.createLead("Ana", "ana@example.com", "Preciso de ajuda", multipartFile);

        assertThat(lead.getId()).isNotNull();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.PROCESSING);
        assertThat(Files.exists(tempDir.resolve(lead.getStoredFileName()))).isTrue();
        assertThat(leadService.getLead(lead.getId()).getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void shouldCompleteLead() {
        MockMultipartFile multipartFile =
                new MockMultipartFile("image", "example.png", "image/png", new byte[] {1, 2, 3});
        Lead lead = leadService.createLead("Ana", "ana@example.com", null, multipartFile);

        leadService.completeLead(lead.getId(), "Resultado pronto");

        Lead completed = leadService.getLead(lead.getId());
        assertThat(completed.getStatus()).isEqualTo(LeadStatus.COMPLETED);
        assertThat(completed.getResult()).isEqualTo("Resultado pronto");
    }

    @Test
    void completeLeadShouldBeIdempotent() {
        MockMultipartFile multipartFile =
                new MockMultipartFile("image", "example.png", "image/png", new byte[] {1, 2, 3});
        Lead lead = leadService.createLead("Ana", "ana@example.com", null, multipartFile);
        UUID id = lead.getId();

        leadService.completeLead(id, "Primeiro");
        leadService.completeLead(id, "Segundo");

        Lead completed = leadService.getLead(id);
        assertThat(completed.getResult()).isEqualTo("Primeiro");
    }
}
