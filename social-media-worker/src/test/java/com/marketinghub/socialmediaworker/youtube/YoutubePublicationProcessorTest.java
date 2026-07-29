package com.marketinghub.socialmediaworker.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.socialmediaworker.dto.YoutubePublicationAction;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationInput;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationOutput;
import com.marketinghub.socialmediaworker.pipeline.StageContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class YoutubePublicationProcessorTest {

    @Test
    void processCreatesManualChannelPlanBecauseYoutubeDoesNotCreateChannelsByApi() {
        YoutubePublicationProcessor processor = new YoutubePublicationProcessor(mock(YoutubeApiClient.class));
        YoutubePublicationInput input = input(YoutubePublicationAction.CREATE_CHANNEL_PLAN);

        YoutubePublicationOutput output = processor.process(StageContext.of("youtube", 1L, "job-1"), input);

        assertEquals("CHANNEL_CREATION_MANUAL_REQUIRED", output.status());
        assertEquals(YoutubePublicationAction.CREATE_CHANNEL_PLAN, output.action());
    }

    @Test
    void processCreatesMarketWarmupPlan() {
        YoutubePublicationProcessor processor = new YoutubePublicationProcessor(mock(YoutubeApiClient.class));
        YoutubePublicationInput input = input(YoutubePublicationAction.MARKET_WARMUP_PLAN);

        YoutubePublicationOutput output = processor.process(StageContext.of("youtube", 1L, "job-1"), input);

        assertEquals("MARKET_WARMUP_PLANNED", output.status());
        assertEquals(3, output.recommendedNextActions().size());
    }

    @Test
    void processPublishesVideoThroughYoutubeClient() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        YoutubePublicationInput input = input(YoutubePublicationAction.PUBLISH_VIDEO);
        when(client.uploadVideo(input)).thenReturn(new YoutubeApiClient.YoutubeUploadResponse("abc123", "https://youtube.com/watch?v=abc123"));
        YoutubePublicationProcessor processor = new YoutubePublicationProcessor(client);

        YoutubePublicationOutput output = processor.process(StageContext.of("youtube", 1L, "job-1"), input);

        assertEquals("VIDEO_PUBLISHED", output.status());
        assertEquals("abc123", output.externalVideoId());
    }

    @Test
    void processRejectsVideoWithoutTitle() {
        YoutubePublicationProcessor processor = new YoutubePublicationProcessor(mock(YoutubeApiClient.class));
        YoutubePublicationInput input = new YoutubePublicationInput(10L, 20L, 30L, "channel", YoutubePublicationAction.PUBLISH_VIDEO, "https://example.com/video.mp4", "", "", List.of(), "private", null, null);

        assertThrows(IllegalArgumentException.class, () -> processor.process(StageContext.of("youtube", 1L, "job-1"), input));
    }

    private YoutubePublicationInput input(YoutubePublicationAction action) {
        return new YoutubePublicationInput(10L, 20L, 30L, "channel", action, "https://example.com/video.mp4", "Titulo", "Descricao", List.of("pde"), "private", "Aulas", "Aquecer PDE");
    }
}
