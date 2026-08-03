package com.marketinghub.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

/** Testa as politicas de storage de assets comerciais. */
@ExtendWith(MockitoExtension.class)
class AssetStorageServiceTest {

  @Mock private S3Client s3Client;

  private StorageProperties properties;
  private AssetStorageService service;

  /** Inicializa o servico sem credenciais R2 para validar bloqueios de configuracao. */
  @BeforeEach
  void setUp() {
    properties = new StorageProperties();
    service = new AssetStorageService(properties, s3Client);
  }

  /** Garante que video bucket-only falha antes de cair em uploads local. */
  @Test
  void shouldRejectBucketOnlyStoreWhenR2IsNotConfigured() {
    MockMultipartFile file =
        new MockMultipartFile("file", "video.mp4", "video/mp4", "demo".getBytes());
    Path localSalesVideosDir = Path.of("uploads", "sales-videos");

    StorageException exception =
        assertThrows(
            StorageException.class,
            () ->
                service.storeInBucketOnly(
                    file,
                    new AssetUploadContext(AssetUploadCategory.SALES_VIDEO, null, null, null)));

    assertThat(exception.getMessage()).contains("Cloudflare R2 must be configured");
    assertThat(Files.notExists(localSalesVideosDir)).isTrue();
    verifyNoInteractions(s3Client);
  }
}
