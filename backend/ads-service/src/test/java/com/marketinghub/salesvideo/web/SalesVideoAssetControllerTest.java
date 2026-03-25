package com.marketinghub.salesvideo.web;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.mapper.AssetMapperImpl;
import com.marketinghub.salesvideo.service.SalesVideoAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesVideoAssetController.class)
@Import(AssetMapperImpl.class)
class SalesVideoAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalesVideoAssetService assetService;

    @Test
    void shouldUploadFileAndReturnAssetDto() throws Exception {
        Asset asset = Asset.builder()
                .id(10L)
                .type(AssetType.VIDEO)
                .provider(MediaProvider.VIDEO_MODULE)
                .status(AssetStatus.READY)
                .url("https://cdn.local/video.mp4")
                .build();
        when(assetService.store(any(), any(), any(), any())).thenReturn(asset);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "video.mp4",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "demo".getBytes());

        mockMvc.perform(multipart("/internal/video/assets")
                        .file(file)
                        .param("assetType", AssetType.VIDEO.name()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.url").value("https://cdn.local/video.mp4"));
    }
}
