package com.marketinghub.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private StorageProperties properties;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setBucket("bucket");
        properties.setMaxDownloadBytes(16);
        service = new FileStorageService(properties, s3Client);
    }

    @Test
    void shouldStreamObjectAsResource() throws Exception {
        byte[] data = "streamed content".getBytes(StandardCharsets.UTF_8);
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(responseStream(data));

        Resource resource = service.loadAsResource("file.txt");

        try (InputStream in = resource.getInputStream()) {
            assertArrayEquals(data, in.readAllBytes());
        }
    }

    @Test
    void shouldRejectObjectsLargerThanConfiguredLimit() {
        properties.setMaxDownloadBytes(4);
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(responseStream("oversized".getBytes(StandardCharsets.UTF_8)));

        assertThrows(StorageException.class, () -> service.loadAsResource("too-big.bin"));
    }

    private ResponseInputStream<GetObjectResponse> responseStream(byte[] data) {
        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength((long) data.length)
                .build();
        return new ResponseInputStream<>(response, new ByteArrayInputStream(data));
    }
}
