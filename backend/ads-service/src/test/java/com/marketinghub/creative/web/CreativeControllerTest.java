package com.marketinghub.creative.web;

import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.FixtureUtils;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Responsabilidade: validar os contratos HTTP de criativos.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class CreativeControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    CreativeRepository repository;
    @Autowired
    ExperimentVideoAssetRepository videoAssetRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    MarketNicheRepository marketNicheRepository;
    @Autowired
    HypothesisRepository hypothesisRepository;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    com.marketinghub.repository.jpa.creative.label.AngleRepository angleRepository;
    @Autowired
    com.marketinghub.repository.jpa.creative.label.VisualProofRepository visualProofRepository;
    @Autowired
    com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository emotionalTriggerRepository;
    @Autowired
    com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository leadPortalFlowRepository;

    Long expId;

    @BeforeEach
    void setup() {
        videoAssetRepository.deleteAll();
        repository.deleteAll();
        experimentRepository.deleteAll();
        leadPortalFlowRepository.deleteAll();
        hypothesisRepository.deleteAll();
        marketNicheRepository.deleteAll();
        MarketNiche niche = fixtures.createAndSaveNiche();
        Experiment exp = fixtures.createAndSaveExperiment(niche);
        expId = exp.getId();
    }

    @Test
    void createEndpointPersists() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("H");
        req.setPrimaryText("P");
        req.setImageUrl("img");
        req.setStatus(CreativeStatus.DRAFT);
        mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        assertThat(repository.count()).isEqualTo(1);
    }

    /** Garante que a API exponha a fila de aprovação de vídeos. */
    @Test
    void listVideoReviewEndpointReturnsVideoCreatives() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setFormat("VIDEO");
        req.setHeadline("Video");
        req.setPrimaryText("P");
        req.setVideoUrl("https://cdn.test/video.mp4");
        req.setCostUsd(new BigDecimal("0.0450"));
        req.setStatus(CreativeStatus.DRAFT);
        mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/creatives/video-review").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].headline").value("Video"))
                .andExpect(jsonPath("$[0].sourceType").value("CREATIVE"))
                .andExpect(jsonPath("$[0].funnelSlot").value("AD"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].videoCostUsd").value(0.0450))
                .andExpect(jsonPath("$[0].totalProductionCostUsd").value(0.0450))
                .andExpect(jsonPath("$[0].videoUrl").value("https://cdn.test/video.mp4"));
    }

    /** Garante que vídeos gerados para experimento aparecem na fila comercial de aprovação. */
    @Test
    void listVideoReviewEndpointReturnsExperimentVideoAssets() throws Exception {
        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        videoAssetRepository.save(ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Video do E002 para rodar campanha")
                .primaryMetric("checkout_start_rate")
                .script("Dor, mecanismo MUSA e chamada para diagnostico.")
                .prompt("Usar personagem aprovada e manter presença elegante acessível.")
                .provider("HEYGEN")
                .model("heygen")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/e002.mp4")
                .hasAudio(true)
                .cost(new BigDecimal("0.0800"))
                .audioCost(new BigDecimal("0.0200"))
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

        mockMvc.perform(get("/api/creatives/video-review").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceType").value("EXPERIMENT_VIDEO_ASSET"))
                .andExpect(jsonPath("$[0].funnelSlot").value("LANDING_HERO"))
                .andExpect(jsonPath("$[0].headline").value("Video do E002 para rodar campanha"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].videoCostUsd").value(0.0800))
                .andExpect(jsonPath("$[0].audioCostUsd").value(0.0200))
                .andExpect(jsonPath("$[0].totalProductionCostUsd").value(0.1000))
                .andExpect(jsonPath("$[0].videoUrl").value("https://cdn.test/e002.mp4"));
    }

    /** Garante que a API permita aprovar somente o status do vídeo. */
    @Test
    void updateStatusEndpointApprovesVideoCreative() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setFormat("VIDEO");
        req.setHeadline("Video");
        req.setPrimaryText("P");
        req.setVideoUrl("https://cdn.test/video.mp4");
        req.setStatus(CreativeStatus.DRAFT);
        String resp = mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.marketinghub.creative.dto.CreativeDto created =
                mapper.readValue(resp, com.marketinghub.creative.dto.CreativeDto.class);

        mockMvc.perform(patch("/api/creatives/" + created.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        assertThat(experiment.isCreativeApproved()).isTrue();
    }

    /** Garante que a tela consiga aprovar um vídeo gerado sem converter o ativo em criativo. */
    @Test
    void videoReviewStatusEndpointApprovesExperimentVideoAsset() throws Exception {
        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        ExperimentVideoAsset videoAsset = videoAssetRepository.save(ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Video E002")
                .primaryMetric("checkout_start_rate")
                .script("Roteiro aprovado")
                .prompt("Prompt aprovado")
                .provider("HEYGEN")
                .model("heygen")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/e002.mp4")
                .hasAudio(true)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

        mockMvc.perform(patch("/api/creatives/video-review/EXPERIMENT_VIDEO_ASSET/" + videoAsset.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("EXPERIMENT_VIDEO_ASSET"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        ExperimentVideoAsset updated = videoAssetRepository.findById(videoAsset.getId()).orElseThrow();
        assertThat(updated.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.APPROVED);
        assertThat(updated.getReviewedAt()).isNotNull();
    }

    /** Garante que vídeo sem áudio não passe da qualidade para aprovação humana. */
    @Test
    void listVideoReviewEndpointDoesNotReturnExperimentVideoAssetWithoutAudio() throws Exception {
        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        videoAssetRepository.save(ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Video sem audio nao deve ir para aprovacao")
                .primaryMetric("checkout_start_rate")
                .script("Roteiro aprovado")
                .prompt("Prompt aprovado")
                .provider("HEYGEN")
                .model("heygen")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/sem-audio.mp4")
                .hasAudio(false)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

        mockMvc.perform(get("/api/creatives/video-review").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    /** Garante que a API também bloqueia aprovação direta de vídeo sem áudio validado. */
    @Test
    void videoReviewStatusEndpointRejectsApprovalWithoutAudioQuality() throws Exception {
        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        ExperimentVideoAsset videoAsset = videoAssetRepository.save(ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Video E002")
                .primaryMetric("checkout_start_rate")
                .script("Roteiro aprovado")
                .prompt("Prompt aprovado")
                .provider("HEYGEN")
                .model("heygen")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/e002.mp4")
                .hasAudio(false)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

        mockMvc.perform(patch("/api/creatives/video-review/EXPERIMENT_VIDEO_ASSET/" + videoAsset.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isBadRequest());

        ExperimentVideoAsset updated = videoAssetRepository.findById(videoAsset.getId()).orElseThrow();
        assertThat(updated.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.PENDING);
    }

    /** Garante que a reprovação preserve o motivo para orientar a próxima geração. */
    @Test
    void videoReviewStatusEndpointRejectsExperimentVideoAssetWithReason() throws Exception {
        Experiment experiment = experimentRepository.findById(expId).orElseThrow();
        ExperimentVideoAsset videoAsset = videoAssetRepository.save(ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Video E002")
                .primaryMetric("checkout_start_rate")
                .script("Roteiro aprovado")
                .prompt("Prompt aprovado")
                .provider("HEYGEN")
                .model("heygen")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/e002.mp4")
                .hasAudio(true)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

        mockMvc.perform(patch("/api/creatives/video-review/EXPERIMENT_VIDEO_ASSET/" + videoAsset.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Personagem oscilou entre cenas.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Personagem oscilou entre cenas."))
                .andExpect(jsonPath("$.reviewedAt").exists());

        ExperimentVideoAsset updated = videoAssetRepository.findById(videoAsset.getId()).orElseThrow();
        assertThat(updated.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.REJECTED);
        assertThat(updated.getRejectionReason()).isEqualTo("Personagem oscilou entre cenas.");
        assertThat(updated.getReviewedAt()).isNotNull();
    }

    /** Garante que a API registre motivo quando o criativo de vídeo for reprovado. */
    @Test
    void updateStatusEndpointRejectsVideoCreativeWithReason() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setFormat("VIDEO");
        req.setHeadline("Video");
        req.setPrimaryText("P");
        req.setVideoUrl("https://cdn.test/video.mp4");
        req.setCostUsd(new BigDecimal("0.0330"));
        req.setStatus(CreativeStatus.DRAFT);
        String resp = mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.marketinghub.creative.dto.CreativeDto created =
                mapper.readValue(resp, com.marketinghub.creative.dto.CreativeDto.class);

        mockMvc.perform(patch("/api/creatives/" + created.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Promessa visual fraca.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Promessa visual fraca."))
                .andExpect(jsonPath("$.reviewedAt").exists());

        mockMvc.perform(get("/api/creatives/video-review").param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Promessa visual fraca."))
                .andExpect(jsonPath("$[0].reviewedAt").exists())
                .andExpect(jsonPath("$[0].videoCostUsd").value(0.0330))
                .andExpect(jsonPath("$[0].totalProductionCostUsd").value(0.0330));
    }

    @Test
    void patchLabelsAssignsSingleLabels() throws Exception {
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("H");
        req.setPrimaryText("P");
        req.setImageUrl("img");
        req.setStatus(CreativeStatus.DRAFT);
        String resp = mockMvc.perform(post("/api/experiments/" + expId + "/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.marketinghub.creative.dto.CreativeDto created =
                mapper.readValue(resp, com.marketinghub.creative.dto.CreativeDto.class);

        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var proof = visualProofRepository.save(com.marketinghub.creative.label.VisualProof.builder().name("V").build());
        var trigger = emotionalTriggerRepository.save(com.marketinghub.creative.label.EmotionalTrigger.builder().name("T").build());

        var labels = new com.marketinghub.creative.dto.UpdateCreativeLabelsRequest();
        labels.setAngleId(angle.getId());
        labels.setVisualProofId(proof.getId());
        labels.setEmotionalTriggerId(trigger.getId());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/creatives/" + created.getId() + "/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(labels)))
                .andExpect(status().isOk());

        var found = repository.findById(created.getId()).orElseThrow();
        assertThat(found.getAngles()).hasSize(1);
        assertThat(found.getVisualProofs()).hasSize(1);
        assertThat(found.getEmotionalTriggers()).hasSize(1);
    }
}
