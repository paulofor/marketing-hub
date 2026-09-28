package com.marketinghub.creative.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.FixtureUtils;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Homologa integridade, segregação, idempotência e gates do controle estático aprovado. */
@SpringBootTest(
    classes = AdsServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.creative.web.ApprovedVisualAssetCreativeControllerTest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false"
    })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApprovedVisualAssetCreativeControllerTest {
  private static final String SHA256 =
      "bd5bd13370ebf69ff022abcbe6b69bf5735dc271cb0bde64cebf68eccad53637";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FixtureUtils fixtures;
  @Autowired ExperimentRepository experiments;
  @Autowired CommercialPlanRepository plans;
  @Autowired CommercialPlanVisualAssetRepository assets;
  @Autowired CreativeRepository creatives;
  @Autowired ExperimentHistoryEventService history;

  @org.springframework.boot.test.mock.mockito.MockBean
  com.marketinghub.experiment.funnel.ExperimentFunnelService commercialMetrics;

  Experiment experiment;
  CommercialPlan plan;
  CommercialPlanVisualAsset approved;

  /** Cria uma imagem aprovada pelos dois revisores em plano ligado ao experimento. */
  @BeforeEach
  void setup() {
    experiment = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setFollowUpActionUrl("https://mira.test/oferta");
    experiment = experiments.save(experiment);
    plan =
        plans.save(
            CommercialPlan.builder()
                .name("Plano Mira")
                .experiment(experiment)
                .niche(experiment.getNiche())
                .build());
    approved = saveAsset(plan, CommercialPlanVisualAssetReviewStatus.APPROVED);
  }

  /** Persiste uma mídia fictícia com integridade e parecer comercial configuráveis. */
  private CommercialPlanVisualAsset saveAsset(
      CommercialPlan owner, CommercialPlanVisualAssetReviewStatus customerReview) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setCommercialPlan(owner);
    asset.setAssetUrl("https://cdn.test/mira-control-" + System.nanoTime() + ".png");
    asset.setMediaType("IMAGE");
    asset.setLabel("Controle estático Mira");
    asset.setPurpose("ADS");
    asset.setPurposesJson("[\"ADS\",\"LANDING\"]");
    asset.setOrigin("Processo criativo aprovado");
    asset.setRightsStatement("Uso comercial autorizado no plano");
    asset.setContentSha256(SHA256);
    asset.setVersionNumber(1);
    asset.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    asset.setCustomerReviewStatus(customerReview);
    return assets.save(asset);
  }

  /** Envia pela rota real a copy curta que acompanhará os pixels aprovados. */
  private String select(long assetId) throws Exception {
    return mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + assetId
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "headline",
                            "Organize sua rotina",
                            "primaryText",
                            "Use os produtos que você já tem em uma rotina simples e individualizada.",
                            "description",
                            "Acesso único por R$ 49"))))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  /** Preserva URL e SHA, cria uma única peça pendente e registra a decisão sem liberar mídia. */
  @Test
  void promotesApprovedPixelsOnceAndKeepsCreativeReviewGates() throws Exception {
    mvc.perform(
            get(
                "/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/eligible"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(approved.getId()))
        .andExpect(jsonPath("$[0].contentSha256").value(SHA256));

    String first = select(approved.getId());
    long creativeId = json.readTree(first).path("id").asLong();
    assertThat(json.readTree(first).path("status").asText()).isEqualTo("DRAFT");
    assertThat(json.readTree(first).path("agentReviewStatus").asText()).isEqualTo("PENDING");
    assertThat(json.readTree(first).path("imageUrl").asText()).isEqualTo(approved.getAssetUrl());
    assertThat(json.readTree(first).path("destinationUrl").asText())
        .isEqualTo(experiment.getFollowUpActionUrl());

    String repeated = select(approved.getId());
    assertThat(json.readTree(repeated).path("id").asLong()).isEqualTo(creativeId);
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"headline\":\"Outra promessa\",\"primaryText\":\"Outro texto\"}"))
        .andExpect(status().isConflict());
    assertThat(creatives.findByExperimentId(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId()).getFirst().evidenceJson()).contains(SHA256);
  }

  /** Não oferece nem materializa imagem sem o parecer de percepção de Psique. */
  @Test
  void rejectsAssetWithoutBothIndependentApprovals() throws Exception {
    approved.setCustomerReviewStatus(null);
    assets.saveAndFlush(approved);

    mvc.perform(
            get(
                "/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/eligible"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"headline\":\"Título\",\"primaryText\":\"Texto\",\"description\":\"Curta\"}"))
        .andExpect(status().isConflict());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Recusa ativo aprovado que pertença ao plano de outro experimento. */
  @Test
  void rejectsAssetFromAnotherExperiment() throws Exception {
    Experiment other = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    CommercialPlan otherPlan =
        plans.save(CommercialPlan.builder().name("Outro plano").experiment(other).build());
    CommercialPlanVisualAsset foreign =
        saveAsset(otherPlan, CommercialPlanVisualAssetReviewStatus.APPROVED);

    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + foreign.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"headline\":\"Título\",\"primaryText\":\"Texto\",\"description\":\"Curta\"}"))
        .andExpect(status().isNotFound());
  }

  /** Não oferece ativo de plano histórico quando já existe um governante mais recente. */
  @Test
  void rejectsAssetFromHistoricalPlanForSameExperiment() throws Exception {
    CommercialPlan current =
        plans.save(
            CommercialPlan.builder()
                .name("Plano Mira atual")
                .experiment(experiment)
                .niche(experiment.getNiche())
                .build());
    CommercialPlanVisualAsset currentAsset =
        saveAsset(current, CommercialPlanVisualAssetReviewStatus.APPROVED);

    mvc.perform(
            get(
                "/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/eligible"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(currentAsset.getId()))
        .andExpect(jsonPath("$[1]").doesNotExist());
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"headline\":\"Título\",\"primaryText\":\"Texto\"}"))
        .andExpect(status().isNotFound());
  }

  /** Exige que a identidade oficial esteja persistida antes de abrir a revisão do controle. */
  @Test
  void rejectsSelectionWithoutInstagramIdentity() throws Exception {
    experiment.setInstagramAccount(null);
    experiments.saveAndFlush(experiment);

    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"headline\":\"Título\",\"primaryText\":\"Texto\",\"description\":\"Curta\"}"))
        .andExpect(status().isConflict());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Bloqueia copy maior que o contrato publicável antes de abrir uma revisão inútil. */
  @Test
  void rejectsCopyOutsidePublicationContract() throws Exception {
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "headline", "12345678901234567890123456789012345678901",
                            "primaryText", "Texto"))))
        .andExpect(status().isBadRequest());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Aceita o limite Unicode exato sem contar cada emoji como dois caracteres. */
  @Test
  void acceptsUnicodePublicationBoundary() throws Exception {
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/commercial-plan-visual-assets/"
                    + approved.getId()
                    + "/creative")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "headline", "😀".repeat(40),
                            "primaryText", "😀".repeat(125),
                            "description", "😀".repeat(25)))))
        .andExpect(status().isOk());
  }
}
