package com.marketinghub.salesvideo.controller;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.mapper.AssetMapperImpl;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Responsabilidade: validar contratos internos de assets e perfis de vídeo. */
@WebMvcTest(SalesVideoController.class)
@Import(AssetMapperImpl.class)
class SalesVideoAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalesVideoService salesVideoService;

    /** Deve aceitar upload interno e devolver o asset criado. */
    @Test
    void shouldUploadFileAndReturnAssetDto() throws Exception {
        Asset asset = Asset.builder()
                .id(10L)
                .type(AssetType.VIDEO)
                .provider(MediaProvider.VIDEO_MODULE)
                .status(AssetStatus.READY)
                .url("https://cdn.local/video.mp4")
                .build();
        when(salesVideoService.storeAsset(any(), any(), any(), any())).thenReturn(asset);

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

    /** Deve expor perfil por endpoint interno sem exigir header de tenant. */
    @Test
    void shouldGetProfileFromInternalAiEndpointWithoutTenantHeader() throws Exception {
        SalesVideoProfileDto profile = new SalesVideoProfileDto();
        profile.setId(59L);
        profile.setTitle("Manicure em domicílio");
        when(salesVideoService.getProfile(59L)).thenReturn(profile);

        mockMvc.perform(get("/internal/ai/sales-videos/profiles/59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(59L))
                .andExpect(jsonPath("$.title").value("Manicure em domicílio"));

        verify(salesVideoService).getProfile(59L);
    }
}
