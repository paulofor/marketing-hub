package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Impede que falha de empacotamento seja promovida a entrega HLS pronta. */
class HlsVideoDeliveryTest {
    /** Falha do comando local impede upload e não fabrica uma URL de streaming. */
    @Test void shouldStopBeforeUploadWhenPackagingFails() throws Exception {
        var client = mock(VideoAssetClient.class);
        var properties = new VideoManagementProperties();
        properties.getProviders().getPostProduction().setFfmpegPath("/bin/false");
        var mapper = new ObjectMapper();
        var job = mapper.treeToValue(mapper.createObjectNode().put("id",91010).put("profileId",91001),SalesVideoJob.class);
        var video = new ProviderFile("final.mp4",MediaType.valueOf("video/mp4"),AssetType.VIDEO,ProviderAssetRole.VIDEO,new byte[]{1});
        assertThatThrownBy(() -> new HlsVideoDelivery(client,properties,mapper).deliver(job,video))
                .isInstanceOf(VideoProviderException.class).hasMessageContaining("HLS");
        verifyNoInteractions(client);
    }
}
