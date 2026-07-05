package com.marketinghub.experiment.web;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.MetricPresetRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.FixtureUtils;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.ads.CampaignRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Responsabilidade: validar os contratos HTTP de criação e atualização de experimentos.
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
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ExperimentRepository repository;
    @Autowired
    private MarketNicheRepository nicheRepo;
    @Autowired
    private AngleRepository angleRepository;
    @Autowired
    private HypothesisRepository hypothesisRepository;
    @Autowired
    private com.marketinghub.repository.jpa.experiment.MetricPresetRepository metricPresetRepository;
    @Autowired
    private com.marketinghub.repository.jpa.creative.CreativeRepository creativeRepo;
    @Autowired
    private FixtureUtils fixtures;
    @Autowired
    private JourneyTemplateRepository journeyTemplateRepository;
    @Autowired
    private TargetingElementRepository targetingElementRepository;
    @Autowired
    private InstagramAccountRepository instagramAccountRepository;
    @Autowired
    private com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository leadPortalFlowRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;
    @Autowired
    private GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
    @Autowired
    private DeliverablePackageRepository deliverablePackageRepository;
    @Autowired
    private DeliverableRepository deliverableRepository;
    @MockBean
    private LeadPortalFlowPublisher leadPortalFlowPublisher;

    Long nicheId;

    private Long createLeadPortalFlow() {
        MarketNiche niche = nicheRepo.findById(nicheId).orElseThrow();
        String slug = "flow-" + UUID.randomUUID();
        return leadPortalFlowRepository.save(
                com.marketinghub.leadportal.LeadPortalFlow.builder()
                        .name("Fluxo " + slug)
                        .slug(slug)
                        .marketNiche(niche)
                        .build()).getId();
    }

    private void applyStageDefaults(CreateExperimentRequest request) {
        request.setStage(com.marketinghub.experiment.ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    private void applyStageDefaults(UpdateExperimentRequest request) {
        request.setStage(com.marketinghub.experiment.ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    @BeforeEach
    // Limpa dados dependentes antes dos experimentos para preservar as FKs do schema real.
    void cleanDb() {
        creativeRepo.deleteAll();
        campaignRepository.deleteAll();
        geraSalesPagePublicationAuditRepository.deleteAll();
        geraSalesPageStageExecutionRepository.deleteAll();

        var experiments = repository.findAll();
        experiments.forEach(experiment -> experiment.setLeadPortalFlow(null));
        repository.saveAll(experiments);

        var leadPortalFlows = leadPortalFlowRepository.findAll();
        leadPortalFlows.forEach(flow -> flow.setExperiment(null));
        leadPortalFlowRepository.saveAll(leadPortalFlows);

        repository.deleteAll();
        leadPortalFlowRepository.deleteAll();
        journeyTemplateRepository.deleteAll();
        targetingElementRepository.deleteAll();
        instagramAccountRepository.deleteAll();
        var hypotheses = hypothesisRepository.findAll();
        hypotheses.forEach(hypothesis -> hypothesis.setOfferPackage(null));
        hypothesisRepository.saveAll(hypotheses);
        deliverablePackageRepository.deleteAll();
        deliverableRepository.deleteAll();
        hypothesisRepository.deleteAll();
        metricPresetRepository.deleteAll();
        angleRepository.deleteAll();
        nicheRepo.deleteAll();
        MarketNiche niche = fixtures.createAndSaveNiche();
        nicheId = niche.getId();
    }

    @Test
    void createEndpointPersists() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("H")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .mechanism("Mecanismo")
                .entrega("Amostra visual personalizada")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .price(new BigDecimal("27.00"))
                .kpiTargetCpl(BigDecimal.ONE)
                .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
                .build());
        var deliverable = deliverableRepository.save(com.marketinghub.deliverable.Deliverable.builder()
                .niche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Amostra")
                .description("Entrega da amostra")
                .build());
        var deliverablePackage = deliverablePackageRepository.save(com.marketinghub.deliverable.DeliverablePackage.builder()
                .hypothesis(hyp)
                .name("Pacote")
                .description("Pacote de oferta")
                .deliverables(new java.util.LinkedHashSet<>(java.util.List.of(deliverable)))
                .build());
        hyp.setOfferPackage(deliverablePackage);
        hypothesisRepository.save(hyp);
        metricPresetRepository.save(com.marketinghub.experiment.MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        var instagramAccount = fixtures.createAndSaveInstagramAccount();
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(instagramAccount.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow());

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productAiSubtype").value("AI_PERSONALIZED_SAMPLE"));

        var saved = repository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getJourneyTemplate()).isNotNull();
        assertThat(saved.get(0).getJourneyTemplate().getId()).isEqualTo(template.getId());
        assertThat(saved.get(0).getProductAiSubtype()).isEqualTo(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    }

    /** Garante que o preparo de Produto IA expõe rascunho somente quando a hipótese está completa. */
    @Test
    void productAiPreparationReturnsDraftWhenHypothesisIsReady() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AI").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Hipótese AI")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .mechanism("Mecanismo")
                .entrega("Amostra visual personalizada")
                .price(new BigDecimal("37.00"))
                .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
                .build());
        var deliverable = deliverableRepository.save(com.marketinghub.deliverable.Deliverable.builder()
                .niche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Amostra")
                .description("Entrega da amostra")
                .build());
        var deliverablePackage = deliverablePackageRepository.save(com.marketinghub.deliverable.DeliverablePackage.builder()
                .hypothesis(hyp)
                .name("Pacote AI")
                .deliverables(new java.util.LinkedHashSet<>(java.util.List.of(deliverable)))
                .build());
        hyp.setOfferPackage(deliverablePackage);
        hypothesisRepository.save(hyp);

        mockMvc.perform(get("/api/product-ai/experiment-preparations/{hypothesisId}", hyp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.draft.experimentType").value("LOW_TICKET_PRODUCT"))
                .andExpect(jsonPath("$.draft.productAiSubtype").value("AI_PERSONALIZED_SAMPLE"))
                .andExpect(jsonPath("$.draft.stage").value("SAMPLE"))
                .andExpect(jsonPath("$.draft.campaignObjective").value("SALES"));
    }

    /** Garante que o comando sistêmico completa uma hipótese rastreada para o MVP de amostra personalizada. */
    @Test
    void personalizedSamplePreparationCompletesTraceableHypothesis() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AI preparo").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Hipótese para preparo")
                .premiseAngle(angle)
                .promise("Mostrar uma prévia visual personalizada da melhoria")
                .problem("O cliente não enxerga o resultado antes de comprar")
                .persona("Dono de pequeno negócio")
                .mechanism("Prévia visual gerada por IA com dados do lead")
                .build());

        mockMvc.perform(get("/api/product-ai/experiment-preparations/{hypothesisId}", hyp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false));

        mockMvc.perform(post("/api/product-ai/hypotheses/{hypothesisId}/personalized-sample-preparation", hyp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productAiSubtype").value("AI_PERSONALIZED_SAMPLE"))
                .andExpect(jsonPath("$.price").value(27.00))
                .andExpect(jsonPath("$.offerPackageName").value("Pacote inicial de amostra personalizada"))
                .andExpect(jsonPath("$.deliverableTitle").value("Amostra visual personalizada"))
                .andExpect(jsonPath("$.experimentPreparation.ready").value(true))
                .andExpect(jsonPath("$.experimentPreparation.draft.experimentType").value("LOW_TICKET_PRODUCT"))
                .andExpect(jsonPath("$.experimentPreparation.draft.stage").value("SAMPLE"));

        var prepared = hypothesisRepository.findById(hyp.getId()).orElseThrow();
        assertThat(prepared.getProductAiSubtype()).isEqualTo(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        assertThat(prepared.getPrice()).isEqualByComparingTo("27.00");
        assertThat(prepared.getOfferPackage()).isNotNull();
        assertThat(deliverablePackageRepository.findByHypothesisIdOrderByCreatedAtDesc(hyp.getId())).hasSize(1);
        assertThat(deliverableRepository.findAll()).hasSize(1);
        assertThat(prepared.getEntrega()).contains("Amostra visual personalizada");
    }

    /** Garante que o Produto IA cria pelo sistema um funil aprovado para coletar dados de personalização. */
    @Test
    void personalizedSampleFunnelEndpointCreatesApprovedLeadPortalFlow() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AI funil").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("DecoraIA Express")
                .premiseAngle(angle)
                .promise("Gerar um plano visual de decoração por foto")
                .problem("O cliente não sabe como melhorar a sala")
                .persona("Pessoa insatisfeita com a decoração de casa")
                .mechanism("Diagnóstico visual personalizado por foto do ambiente")
                .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
                .build());
        var experiment = repository.save(com.marketinghub.experiment.Experiment.builder()
                .niche(nicheRepo.findById(nicheId).orElseThrow())
                .name("DecoraIA Express - Transforme seu ambiente por foto")
                .hypothesis("Plano de decoração personalizado por foto do ambiente")
                .funnelPromise("Envie uma foto do ambiente e receba uma amostra personalizada")
                .hypothesisRef(hyp)
                .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
                .experimentType(com.marketinghub.experiment.ExperimentType.LOW_TICKET_PRODUCT)
                .build());

        mockMvc.perform(post("/api/product-ai/experiments/{experimentId}/personalized-sample-funnel", experiment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentId").value(experiment.getId()))
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.leadPortalFlowSlug")
                        .value("product-ai-exp-" + experiment.getId() + "-personalized-sample"))
                .andExpect(jsonPath("$.dataKeys[0]").value("nome"))
                .andExpect(jsonPath("$.dataKeys[3]").value("foto_ambiente"))
                .andExpect(jsonPath("$.dataKeys[4]").value("ambiente_a_transformar"))
                .andExpect(jsonPath("$.dataKeys[5]").value("incomodo_principal"));

        var updated = repository.findById(experiment.getId()).orElseThrow();
        assertThat(updated.getLeadPortalFlow()).isNotNull();
        assertThat(updated.getLeadPortalFlow().isApproved()).isTrue();
        var flowWithQuestions = leadPortalFlowRepository.findBySlug(updated.getLeadPortalFlow().getSlug()).orElseThrow();
        assertThat(flowWithQuestions.getQuestions())
                .anySatisfy(question -> {
                    assertThat(question.getDataKey()).isEqualTo("foto_ambiente");
                    assertThat(question.getType()).isEqualTo(com.marketinghub.leadportal.LeadPortalQuestionType.IMAGE_UPLOAD);
                    assertThat(question.isRequired()).isTrue();
                });
        verify(leadPortalFlowPublisher).publish(any(com.marketinghub.leadportal.LeadPortalFlow.class));
    }

    /** Garante que Produto IA incompleto não vira experimento por chamada direta de API. */
    @Test
    void createEndpointRejectsIncompleteProductAiHypothesis() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AI incompleto").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Hipótese incompleta")
                .premiseAngle(angle)
                .promise("Promessa")
                .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
                .build());
        metricPresetRepository.save(com.marketinghub.experiment.MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        var instagramAccount = fixtures.createAndSaveInstagramAccount();
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setDailyBudget(new BigDecimal("10"));
        req.setUnitPrice(new BigDecimal("27"));
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(instagramAccount.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow());

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    /** Garante que o contrato do AI Worker lista e conclui geração pendente de criativos. */
    @Test
    void creativeGenerationWorkerContractListsAndCompletesPendingExperiment() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("Criativo").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Hipótese criativo")
                .premiseAngle(angle)
                .build());
        var experiment = repository.save(com.marketinghub.experiment.Experiment.builder()
                .niche(nicheRepo.findById(nicheId).orElseThrow())
                .name("Experimento Criativo")
                .hypothesisRef(hyp)
                .creativesToGenerate(3)
                .creativeGenerationMode(com.marketinghub.experiment.CreativeGenerationMode.DEFAULT)
                .creativeGenerationStatus(com.marketinghub.experiment.CreativeGenerationStatus.REQUESTED)
                .build());

        mockMvc.perform(get("/api/experiments/creatives/stage-executions/pending")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(experiment.getId()));

        mockMvc.perform(post("/api/experiments/{id}/creatives/stage-execution/complete", experiment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creativeGenerationStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.creativesToGenerate").value(0));
    }

    /** Garante que o start do GeraAnuncio v2 enfileira o experimento na geração real de criativos. */
    @Test
    void geraAnuncioTextoStartEnqueuesExperimentCreativeGeneration() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("Pipeline").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("Hipótese pipeline")
                .premiseAngle(angle)
                .build());
        var experiment = repository.save(com.marketinghub.experiment.Experiment.builder()
                .niche(nicheRepo.findById(nicheId).orElseThrow())
                .name("Experimento Pipeline")
                .hypothesisRef(hyp)
                .adCopy("{\"adCopy\":{\"primaryTextVariants\":[{\"primaryText\":\"Texto\",\"headline\":\"Headline\"}]}}")
                .adImageBriefing("{\"adImageBriefing\":{\"briefings\":[{\"visualBriefing\":\"Imagem\"}]}}")
                .build());

        mockMvc.perform(post("/api/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/experiments/{experimentId}/start",
                        experiment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageExecutionId").value("geracaoanuncios-v1-texto-exp-" + experiment.getId()))
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        mockMvc.perform(get("/api/experiments/creatives/stage-executions/pending")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(experiment.getId()))
                .andExpect(jsonPath("$[0].creativeGenerationMode").value("PIPELINE_ADS"))
                .andExpect(jsonPath("$[0].creativesToGenerate").value(3));
    }

    @Test
    void createEndpointRejectsMissingJourneyTemplate() throws Exception {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(nicheRepo.findById(nicheId).orElseThrow())
                .title("H")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ONE)
                .build());
        metricPresetRepository.save(com.marketinghub.experiment.MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        var instagramAccount = fixtures.createAndSaveInstagramAccount();
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setName("Exp1");
        req.setHypothesisId(hyp.getId());
        req.setHypothesis("H1");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setInstagramAccountId(instagramAccount.getId());
        req.setLeadPortalFlowId(createLeadPortalFlow());

        mockMvc.perform(post("/api/niches/" + nicheId + "/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEndpointUpdatesFields() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        JourneyTemplate startTemplate = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        JourneyTemplate newTemplate = journeyTemplateRepository.save(JourneyTemplate.builder().name("Retarget").build());
        exp.setJourneyTemplate(startTemplate);
        repository.save(exp);
        UpdateExperimentRequest req = new UpdateExperimentRequest();
        applyStageDefaults(req);
        req.setName("Updated");
        req.setHypothesis("Hyp");
        req.setKpiTargetCpl(new BigDecimal("50"));
        req.setMetricPresetId("LEAN_150");
        req.setSampleSize(200);
        req.setMdePercent(new BigDecimal("30"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setJourneyTemplateId(newTemplate.getId());
        req.setLeadPortalFlowId(exp.getLeadPortalFlow().getId());

        mockMvc.perform(patch("/api/experiments/" + exp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getSampleSize()).isEqualTo(200);
        assertThat(updated.getMdePercent()).isEqualByComparingTo("30");
        assertThat(updated.getJourneyTemplate()).isNotNull();
        assertThat(updated.getJourneyTemplate().getId()).isEqualTo(newTemplate.getId());
    }

    @Test
    void requestCreativesEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/creatives-to-generate")
                                .param("quantity", "3"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getCreativesToGenerate()).isEqualTo(3);
    }

    @Test
    void requestInstantFormsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/instant-forms-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getInstantFormsToGenerate()).isEqualTo(2);
    }

    @Test
    void requestEmailsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/emails-to-generate")
                                .param("quantity", "4"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getEmailsToGenerate()).isEqualTo(4);
    }

    @Test
    void requestSampleEmailsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/sample-emails-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getSampleEmailsToGenerate()).isEqualTo(2);
    }

    @Test
    void requestLeadPortalFlowsEndpointUpdatesQuantity() throws Exception {
        var niche = nicheRepo.findById(nicheId).orElseThrow();
        var exp = fixtures.createAndSaveExperiment(niche);
        mockMvc.perform(
                        patch("/api/experiments/" + exp.getId() + "/lead-portal-flows-to-generate")
                                .param("quantity", "2"))
                .andExpect(status().isOk());
        var updated = repository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getLeadPortalFlowsToGenerate()).isEqualTo(2);
    }
}
