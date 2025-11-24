package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.leadportal.config.StorageProperties;
import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import com.marketinghub.leadportal.storage.FileStorageService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class LeadServiceTest {

    private LeadService leadService;
    private S3Client s3Client;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setBucket("test-bucket");
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKeyId("test-key");
        properties.setSecretAccessKey("test-secret");

        s3Client = Mockito.mock(S3Client.class);
        FileStorageService fileStorageService = new FileStorageService(properties, s3Client);
        fileStorageService.init();

        Mockito.lenient()
                .when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        Mockito.lenient()
                .when(s3Client.getObjectAsBytes(Mockito.any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {1, 2, 3}));

        leadService = new LeadService(fileStorageService);
    }

    @Test
    void shouldCreateLeadAndStoreFile() {
        MockMultipartFile multipartFile =
                new MockMultipartFile("image", "example.png", "image/png", new byte[] {1, 2, 3});

        Lead lead = leadService.createLead("Ana", "ana@example.com", "Preciso de ajuda", multipartFile);

        assertThat(lead.getId()).isNotNull();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.PROCESSING);
        assertThat(leadService.getLead(lead.getId()).getEmail()).isEqualTo("ana@example.com");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        Mockito.verify(s3Client).putObject(putCaptor.capture(), Mockito.any(RequestBody.class));
        assertThat(putCaptor.getValue().bucket()).isEqualTo(properties.getBucket());
        assertThat(putCaptor.getValue().key()).isEqualTo(lead.getStoredFileName());
        assertThat(lead.getStoredFileName()).endsWith("example.png");
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
