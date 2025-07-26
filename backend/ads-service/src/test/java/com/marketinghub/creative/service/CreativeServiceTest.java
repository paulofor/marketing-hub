package com.marketinghub.creative.service;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.creative.label.repository.VisualProofRepository;
import com.marketinghub.creative.label.repository.EmotionalTriggerRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.FixtureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.marketinghub.test.MySqlContainerBase;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AdsServiceApplication.class)
class CreativeServiceTest extends MySqlContainerBase {

    @Autowired
    CreativeRepository repository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    AngleRepository angleRepository;
    @Autowired
    VisualProofRepository visualProofRepository;
    @Autowired
    EmotionalTriggerRepository emotionalTriggerRepository;
    @Autowired
    FixtureUtils fixtures;

    CreativeService service;

    @BeforeEach
    void setup() {
        HttpClient client = Mockito.mock(HttpClient.class);
        service = new CreativeService(
                repository,
                experimentRepository,
                angleRepository,
                visualProofRepository,
                emotionalTriggerRepository,
                client);
    }

    @Test
    void uploadImageReturnsPath() throws Exception {
        MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1,2});
        String url = service.uploadImage(file);
        assertThat(url).contains("/uploads/");
    }

    @Test
    void previewParsesHtml() throws Exception {
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);
        fixtures.createAndSaveCreative(exp);

        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        when(resp.body()).thenReturn("{\"data\":[{\"body\":\"<div>ok</div>\"}]}");
        when(client.send(any(), any())).thenReturn((HttpResponse) resp);
        System.setProperty("FB_ACCESS_TOKEN", "dummy");
        try {
            service = new CreativeService(
                    repository,
                    experimentRepository,
                    angleRepository,
                    visualProofRepository,
                    emotionalTriggerRepository,
                    client);
            String html = service.preview(1L);
            assertThat(html).contains("ok");
        } finally {
            System.clearProperty("FB_ACCESS_TOKEN");
        }
    }
}
