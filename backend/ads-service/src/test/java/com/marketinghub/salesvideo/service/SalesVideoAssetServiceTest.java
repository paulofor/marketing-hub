package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalesVideoAssetServiceTest {

    @Mock
    private AssetStorageService storageService;

    @Mock
    private AssetRepository assetRepository;

    @Captor
    private ArgumentCaptor<Asset> assetCaptor;

    private SalesVideoAssetService service;

    @BeforeEach
    void setUp() {
        service = new SalesVideoAssetService(storageService, assetRepository, new ObjectMapper());
    }

    @Test
    void shouldStoreVideoAssetWithMetadata() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "demo".getBytes());
        AssetStorageService.StoredObject storedObject = new AssetStorageService.StoredObject(
                "sales-videos/demo.mp4",
                "https://cdn.local/demo.mp4",
                file.getSize(),
                file.getContentType(),
                true);
        given(storageService.store(eq(file), any(AssetUploadContext.class))).willReturn(storedObject);
        given(assetRepository.save(any(Asset.class))).will(invocation -> {
            Asset asset = invocation.getArgument(0);
            asset.setId(42L);
            return asset;
        });

        Asset result = service.store(file,
                AssetType.VIDEO,
                MediaProvider.RUNWAY,
                "{\"quality\":\"draft\"}");

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getStatus()).isEqualTo(AssetStatus.READY);
        assertThat(result.getUrl()).isEqualTo("https://cdn.local/demo.mp4");
        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getPayload()).contains("quality");
    }

    @Test
    void shouldRejectInvalidMetadataJson() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "demo".getBytes());
        AssetStorageService.StoredObject storedObject = new AssetStorageService.StoredObject(
                "sales-videos/demo.mp4",
                "https://cdn.local/demo.mp4",
                file.getSize(),
                file.getContentType(),
                true);
        given(storageService.store(eq(file), any(AssetUploadContext.class))).willReturn(storedObject);

        assertThrows(VideoModuleException.class, () ->
                service.store(file, AssetType.VIDEO, MediaProvider.OPENAI, "not-json"));
    }

    @Test
    void shouldRejectEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThrows(VideoModuleException.class, () ->
                service.store(emptyFile, AssetType.VIDEO, MediaProvider.OPENAI, null));
    }
}
