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
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class CreativeServiceTest {

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
    @Autowired
    AssetRepository assetRepository;

    @Autowired
    CreativeService service;

    @MockBean
    HttpClient httpClient;

    @BeforeEach
    void setup() {
        assetRepository.deleteAll();
    }

    @Test
    void uploadImageReturnsPath() throws Exception {
        MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1,2});
        assetRepository.deleteAll();

        String url = service.uploadImage(file, "dall-e-3", "prompt text");

        assertThat(url).contains("/uploads/");
        Asset saved = assetRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getUrl()).isEqualTo(url);
        assertThat(saved.getType()).isEqualTo(AssetType.IMAGE);
        assertThat(saved.getStatus()).isEqualTo(AssetStatus.READY);
        assertThat(saved.getModel()).isEqualTo("dall-e-3");
        assertThat(saved.getPrompt()).isEqualTo("prompt text");
    }

    @Test
    void previewParsesHtml() throws Exception {
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);
        fixtures.createAndSaveCreative(exp);

        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.body()).thenReturn("{\"data\":[{\"body\":\"<div>ok</div>\"}]}");
        when(httpClient.send(any(), any())).thenReturn((HttpResponse) resp);
        System.setProperty("FB_ACCESS_TOKEN", "dummy");
        try {
            String html = service.preview(1L);
            assertThat(html).contains("ok");
        } finally {
            System.clearProperty("FB_ACCESS_TOKEN");
        }
    }

    @Test
    void approvingCreativeMarksExperimentAsReady() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);

        CreateCreativeRequest createRequest = new CreateCreativeRequest();
        createRequest.setHeadline("Headline");
        createRequest.setPrimaryText("Primary");
        createRequest.setImageUrl("/img.png");
        createRequest.setStatus(CreativeStatus.DRAFT);
        Creative creative = service.create(exp.getId(), createRequest);

        Experiment afterCreate = experimentRepository.findById(exp.getId()).orElseThrow();
        assertThat(afterCreate.isCreativeApproved()).isFalse();

        CreateCreativeRequest approveRequest = new CreateCreativeRequest();
        approveRequest.setHeadline("Headline");
        approveRequest.setPrimaryText("Primary");
        approveRequest.setImageUrl("/img.png");
        approveRequest.setStatus(CreativeStatus.READY);
        service.update(creative.getId(), approveRequest);

        Experiment afterApproval = experimentRepository.findById(exp.getId()).orElseThrow();
        assertThat(afterApproval.isCreativeApproved()).isTrue();
    }

    @Test
    void deletingLastApprovedCreativeResetsFlag() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);

        CreateCreativeRequest createRequest = new CreateCreativeRequest();
        createRequest.setHeadline("Headline");
        createRequest.setPrimaryText("Primary");
        createRequest.setImageUrl("/img.png");
        createRequest.setStatus(CreativeStatus.READY);
        Creative creative = service.create(exp.getId(), createRequest);

        Experiment afterApproval = experimentRepository.findById(exp.getId()).orElseThrow();
        assertThat(afterApproval.isCreativeApproved()).isTrue();

        service.delete(creative.getId());

        Experiment afterDelete = experimentRepository.findById(exp.getId()).orElseThrow();
        assertThat(afterDelete.isCreativeApproved()).isFalse();
    }
}
