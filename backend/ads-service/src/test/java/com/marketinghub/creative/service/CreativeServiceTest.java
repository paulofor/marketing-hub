package com.marketinghub.creative.service;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.FixtureUtils;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.creative.dto.AssetUploadResponse;
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

    @MockBean
    AssetStorageService assetStorageService;

    @BeforeEach
    void setup() {
        assetRepository.deleteAll();
    }

    @Test
    void uploadImageReturnsPath() throws Exception {
        MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1,2});
        AssetStorageService.StoredObject stored = new AssetStorageService.StoredObject(
                "experiments/exp-1/test.png",
                "https://cdn.test/assets/test.png",
                file.getSize(),
                "image/png",
                true);
        when(assetStorageService.store(any(), any())).thenReturn(stored);

        AssetUploadResponse response = service.uploadImage(
                file,
                "dall-e-3",
                "prompt text",
                "intermediate prompt",
                AssetUploadCategory.EXPERIMENT_CREATIVE,
                1L,
                null,
                "slug-test");

        assertThat(response.url()).isEqualTo("https://cdn.test/assets/test.png");
        Asset saved = assetRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getUrl()).isEqualTo("https://cdn.test/assets/test.png");
        assertThat(saved.getExternalId()).isEqualTo("experiments/exp-1/test.png");
        assertThat(saved.getType()).isEqualTo(AssetType.IMAGE);
        assertThat(saved.getStatus()).isEqualTo(AssetStatus.READY);
        assertThat(saved.getModel()).isEqualTo("dall-e-3");
        assertThat(saved.getPrompt()).isEqualTo("prompt text");
        assertThat(saved.getPromptIntermediate()).isEqualTo("intermediate prompt");
        assertThat(saved.getPayload()).contains("EXPERIMENT_CREATIVE");
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
