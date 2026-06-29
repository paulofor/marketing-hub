package com.marketinghub.experiment;

import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.experiment.MetricPresetRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.experiment.funnel.ExperimentFunnelEvent;
import com.marketinghub.experiment.funnel.ExperimentLandingAnalyticsEvent;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Valida os fluxos principais do serviço de experimentos com persistência em memória. */
@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class ExperimentServiceTest {
    @Autowired
    ExperimentService service;
    @Autowired
    MarketNicheRepository nicheRepository;
    @Autowired
    com.marketinghub.repository.jpa.hypothesis.HypothesisRepository hypothesisRepository;
    @Autowired
    com.marketinghub.repository.jpa.creative.label.AngleRepository angleRepository;
    @Autowired
    com.marketinghub.repository.jpa.experiment.MetricPresetRepository metricPresetRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;
    @Autowired
    TargetingElementRepository targetingElementRepository;
    @Autowired
    InstagramAccountRepository instagramAccountRepository;
    @Autowired
    FacebookAccountRepository facebookAccountRepository;
    @Autowired
    FacebookAdsCampaignRepository facebookAdsCampaignRepository;
    @Autowired
    com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository leadPortalFlowRepository;
    @Autowired
    ExperimentFunnelEventRepository experimentFunnelEventRepository;
    @Autowired
    ExperimentLandingAnalyticsEventRepository experimentLandingAnalyticsEventRepository;

    private InstagramAccount createInstagramAccount() {
        return instagramAccountRepository.save(
                InstagramAccount.builder()
                        .name("Conta Teste")
                        .handle("@contateste")
                        .code("IG-1")
                        .build());
    }

    private JourneyTemplate createJourneyTemplate() {
        return journeyTemplateRepository.save(
                JourneyTemplate.builder()
                        .name("Lifecycle")
                        .build());
    }

    private Long createLeadPortalFlow(MarketNiche niche) {
        String slug = "flow-" + java.util.UUID.randomUUID();
        return leadPortalFlowRepository.save(
                com.marketinghub.leadportal.LeadPortalFlow.builder()
                        .name("Fluxo " + slug)
                        .slug(slug)
                        .marketNiche(niche)
                        .build()).getId();
    }

    @Test
    void requestPipelineCreativesQueuesPipelineAdsForWorker() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp pipeline");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req.setFollowUpActionUrl("https://destino.com");
        Experiment exp = service.create(req);
        exp.setAdCopy("{\"adCopy\":{\"primaryTextVariants\":[{\"label\":\"dor\",\"primaryText\":\"Texto\",\"headline\":\"Headline\",\"description\":\"Descrição\",\"ctaText\":\"Saiba mais\"}]}}");
        exp.setAdImageBriefing("{\"adImageBriefing\":{\"briefings\":[{\"mustMatchAdVariant\":\"dor\",\"visualBriefing\":\"Use contraste simples\",\"assetType\":\"estatico\"}]}}");
        experimentRepository.save(exp);

        Experiment requested = service.requestPipelineCreatives(exp.getId());

        assertThat(requested.getCreativeGenerationMode()).isEqualTo(CreativeGenerationMode.PIPELINE_ADS);
        assertThat(requested.getCreativeGenerationStatus()).isEqualTo(CreativeGenerationStatus.REQUESTED);
        assertThat(requested.getCreativesToGenerate()).isEqualTo(3);
        assertThat(service.listPendingCreativeGeneration(10))
                .extracting(Experiment::getId)
                .contains(requested.getId());
    }

    @Test
    void requestPipelineCreativesRejectsMissingPipelineAssets() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste sem destino").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150_DRAFT")
                .name("Lean-Startup 150 Draft")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp pipeline draft");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150_DRAFT");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        Experiment exp = service.create(req);
        assertThatThrownBy(() -> service.requestPipelineCreatives(exp.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Conclua as etapas de Texto do Anúncio e Prompt da Imagem");
    }


    private void applyStageDefaults(CreateExperimentRequest request) {
        request.setStage(ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    private void applyStageDefaults(UpdateExperimentRequest request) {
        request.setStage(ExperimentStage.AD);
        request.setPrimaryVariable("Ângulo de dor");
        request.setPrimaryMetric("CTR de link (%)");
    }

    @Test
    void createNewExperimentWithExistingNiche() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        var exp = service.create(req);
        assertThat(exp.getId()).isNotNull();
        assertThat(exp.getPlatform()).isEqualTo(ExperimentPlatform.FACEBOOK);
        assertThat(exp.getName()).isEqualTo("T-E001");
    }


    /** Garante que a criação persiste o contrato de promessa única com objetivo Leads. */
    @Test
    void createPersistsSinglePromiseContract() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Contrato promessa").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("Promessa").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese promessa")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_PROMISE")
                .name("Lean Promessa")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp promessa");
        req.setHypothesis("Teste");
        req.setSinglePain("  cliente desmarca horário  ");
        req.setFreeReward("  3 mensagens prontas  ");
        req.setFunnelPromise("Receber as 3 mensagens");
        req.setPrimaryCta("Receber as 3 mensagens");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_PROMISE");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment exp = service.create(req);

        assertThat(exp.getSinglePain()).isEqualTo("cliente desmarca horário");
        assertThat(exp.getFreeReward()).isEqualTo("3 mensagens prontas");
        assertThat(exp.getFunnelPromise()).isEqualTo("Receber as 3 mensagens");
        assertThat(exp.getPrimaryCta()).isEqualTo("Receber as 3 mensagens");
        assertThat(exp.getCampaignObjective()).isEqualTo(ExperimentCampaignObjective.LEADS);
    }

    /** Garante que produto low-ticket nasce com objetivo de venda mesmo com amostra secundária. */
    @Test
    void createLowTicketProductDefaultsToSalesObjective() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Low ticket").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("Venda").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese venda")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.TRIPWIRE)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_LOW_TICKET")
                .name("Lean Low Ticket")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp low ticket");
        req.setHypothesis("Teste");
        req.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        req.setSinglePain("cliente atrasa manutenção");
        req.setFreeReward("ver amostra gratuita");
        req.setFunnelPromise("organizar manutenção em 7 dias");
        req.setPrimaryCta("Comprar por R$ 27");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_LOW_TICKET");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment exp = service.create(req);

        assertThat(exp.getExperimentType()).isEqualTo(ExperimentType.LOW_TICKET_PRODUCT);
        assertThat(exp.getCampaignObjective()).isEqualTo(ExperimentCampaignObjective.SALES);
        assertThat(exp.getFreeReward()).isEqualTo("ver amostra gratuita");
    }

    /** Garante que recompensa gratuita não pode nascer com objetivo Tráfego. */
    @Test
    void createRejectsTrafficObjectiveWithFreeReward() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Contrato tráfego").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("Tráfego").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese tráfego")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_TRAFFIC_BLOCK")
                .name("Lean Bloqueio Tráfego")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp tráfego");
        req.setHypothesis("Teste");
        req.setFreeReward("3 mensagens prontas");
        req.setCampaignObjective(ExperimentCampaignObjective.TRAFFIC);
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_TRAFFIC_BLOCK");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("campaignObjective must be LEADS");
    }

    @Test
    void createAllowsSampleSizeBelowOneHundred() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(5);
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment experiment = service.create(req);

        assertThat(experiment.getSampleSize()).isEqualTo(5);
    }

    @Test
    void createRejectsZeroSampleSize() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(0);
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("sampleSize must be at least 1");
    }

    @Test
    void validateDates() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setStartDate(java.time.LocalDate.of(2024,2,1));
        req.setEndDate(java.time.LocalDate.of(2024,1,1));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void hypothesisAndNicheMustMatch() {
        MarketNiche niche1 = nicheRepository.save(MarketNiche.builder().name("N1").build());
        MarketNiche niche2 = nicheRepository.save(MarketNiche.builder().name("N2").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche1)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche2.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche2));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    /** Verifica que apenas experimentos liberados, criativos aprovados e públicos com metaId entram na fila. */
    @Test
    void listReadyForCampaignRequiresApprovals() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H1")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());

        CreateExperimentRequest req1 = new CreateExperimentRequest();
        applyStageDefaults(req1);
        req1.setMarketNicheId(niche.getId());
        req1.setHypothesisId(hyp.getId());
        req1.setName("ExpA");
        req1.setHypothesis("H");
        req1.setKpiTargetCpl(new BigDecimal("45"));
        req1.setMetricPresetId("LEAN_150");
        req1.setJourneyTemplateId(createJourneyTemplate().getId());
        req1.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req1.setInstagramAccountId(createInstagramAccount().getId());
        var expApproved = service.create(req1);
        expApproved.setCreativeApproved(true);
        experimentRepository.save(expApproved);
        service.releaseForFacebook(expApproved.getId());

        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.INTEREST)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .metaId("meta-interest-1")
                .term("Interest")
                .build());
        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.JOB_TITLE)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .metaId("meta-job-title-1")
                .term("CMO")
                .build());
        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.BEHAVIOR)
                .status(com.marketinghub.targeting.TargetingElementStatus.APPROVED)
                .metaId("meta-behavior-1")
                .term("Engaged")
                .build());

        targetingElementRepository.save(com.marketinghub.targeting.TargetingElement.builder()
                .niche(niche)
                .hypothesis(hyp)
                .type(com.marketinghub.targeting.TargetingElementType.INTEREST)
                .status(com.marketinghub.targeting.TargetingElementStatus.DRAFT)
                .term("Pending")
                .build());

        CreateExperimentRequest req2 = new CreateExperimentRequest();
        applyStageDefaults(req2);
        req2.setMarketNicheId(niche.getId());
        req2.setHypothesisId(hyp.getId());
        req2.setName("ExpB");
        req2.setHypothesis("H");
        req2.setKpiTargetCpl(new BigDecimal("45"));
        req2.setMetricPresetId("LEAN_150");
        req2.setJourneyTemplateId(createJourneyTemplate().getId());
        req2.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req2.setInstagramAccountId(createInstagramAccount().getId());
        var expNotApproved = service.create(req2);
        experimentRepository.save(expNotApproved);

        var result = service.listReadyForCampaign();
        assertThat(result).extracting(Experiment::getId).containsExactly(expApproved.getId());
    }

    @Test
    void updateStatusPreservesReleaseTimestampForBaseline() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche Status").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AS").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("HS")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("ExpStatus");
        req.setHypothesis("H");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req.setInstagramAccountId(createInstagramAccount().getId());
        Experiment experiment = service.create(req);
        experimentRepository.save(experiment);

        Experiment released = service.releaseForFacebook(experiment.getId());
        assertThat(released.getFacebookReleaseRequestedAt()).isNotNull();
        var releaseTimestamp = released.getFacebookReleaseRequestedAt();

        Experiment paused = service.updateStatus(experiment.getId(), ExperimentStatus.PAUSED);
        assertThat(paused.getFacebookReleaseRequestedAt()).isNotNull();
        assertThat(paused.getFacebookReleaseRequestedAt().toEpochMilli())
                .isEqualTo(releaseTimestamp.toEpochMilli());
    }

    @Test
    void releaseForFacebookResetsFunnelAndTimestamp() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche Release").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AR").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("HR")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("ExpRelease");
        req.setHypothesis("H");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req.setInstagramAccountId(createInstagramAccount().getId());
        Experiment experiment = service.create(req);
        experiment.setCreativeApproved(true);
        experimentRepository.save(experiment);

        experimentFunnelEventRepository.save(ExperimentFunnelEvent.builder()
                .experiment(experiment)
                .stage(ExperimentFunnelStage.ENVIO_FORM)
                .source("manual")
                .occurredAt(Instant.now().minusSeconds(60))
                .build());
        ExperimentFunnelEvent landingEvent = experimentFunnelEventRepository.save(ExperimentFunnelEvent.builder()
                .experiment(experiment)
                .stage(ExperimentFunnelStage.VISUALIZACAO_FORM)
                .source(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE)
                .occurredAt(Instant.now().minusSeconds(30))
                .build());
        experimentLandingAnalyticsEventRepository.save(ExperimentLandingAnalyticsEvent.builder()
                .experiment(experiment)
                .funnelEvent(landingEvent)
                .eventType("page_view")
                .occurredAt(landingEvent.getOccurredAt())
                .build());
        FacebookAccount facebookAccount = facebookAccountRepository.save(FacebookAccount.builder()
                .name("Conta Facebook")
                .adAccountId("act_123")
                .build());
        FacebookAdsCampaign previousCampaign = new FacebookAdsCampaign();
        previousCampaign.setId("campaign-previous");
        previousCampaign.setExternalId("meta-previous");
        previousCampaign.setAdAccountId("act_123");
        previousCampaign.setExperiment(experiment);
        previousCampaign.setFacebookAccount(facebookAccount);
        previousCampaign.setName("Campanha anterior");
        previousCampaign.setObjective("OUTCOME_LEADS");
        previousCampaign.setStatus(FacebookAdStatus.ACTIVE);
        previousCampaign.setBudgetMode(BudgetMode.CAMPAIGN);
        facebookAdsCampaignRepository.save(previousCampaign);

        Experiment released = service.releaseForFacebook(experiment.getId());

        assertThat(released.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
        assertThat(released.getFacebookReleaseRequestedAt()).isNotNull();
        assertThat(released.getFunnelResetAt()).isNotNull();
        assertThat(experimentLandingAnalyticsEventRepository.count()).isZero();
        assertThat(experimentFunnelEventRepository.count()).isZero();
        assertThat(facebookAdsCampaignRepository.existsByExperimentId(experiment.getId())).isFalse();
    }

    @Test
    void releaseForFacebookAllowsReleaseWithoutPixel() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche Pixel").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("AP").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("HP")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("ExpNoPixel");
        req.setHypothesis("H");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        req.setInstagramAccountId(createInstagramAccount().getId());
        Experiment experiment = service.create(req);

        Experiment released = service.releaseForFacebook(experiment.getId());

        assertThat(released.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
        MarketNiche refreshed = nicheRepository.findById(niche.getId()).orElseThrow();
        assertThat(refreshed.getFacebookPixelId()).isNull();
        assertThat(released.getFacebookReleaseRequestedAt()).isNotNull();
    }

    @Test
    void listByStatusAndPlatform() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche2").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A2").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T2")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("ExpRun");
        req.setHypothesis("H");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(createJourneyTemplate().getId());
        req.setSampleSize(1500);
        req.setBaselineCvr(new BigDecimal("3"));
        req.setTargetCvr(new BigDecimal("5"));
        req.setMdePercent(new BigDecimal("40"));
        req.setInstagramAccountId(createInstagramAccount().getId());
        var exp = service.create(req);
        exp.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(exp);

        var result = service.listByStatusAndPlatform(ExperimentStatus.RUNNING, ExperimentPlatform.FACEBOOK);
        assertThat(result).extracting(Experiment::getId).containsExactly(exp.getId());
    }

    @Test
    void createAssociatesJourneyTemplateById() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        Experiment exp = service.create(req);

        assertThat(exp.getJourneyTemplate()).isNotNull();
        assertThat(exp.getJourneyTemplate().getId()).isEqualTo(template.getId());
    }

    @Test
    void createRejectsMissingJourneyTemplateId() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("journeyTemplateId required");
    }

    @Test
    void updateChangesJourneyTemplateWhenProvided() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate first = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        JourneyTemplate second = journeyTemplateRepository.save(JourneyTemplate.builder().name("Retarget").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(first.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        applyStageDefaults(updateReq);
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(second.getId());

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getJourneyTemplate()).isNotNull();
        assertThat(updated.getJourneyTemplate().getId()).isEqualTo(second.getId());
    }

    @Test
    void updateAllowsSampleSizeBelowOneHundred() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(createLeadPortalFlow(niche));
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        applyStageDefaults(updateReq);
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(template.getId());
        updateReq.setSampleSize(5);

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getSampleSize()).isEqualTo(5);
    }

    @Test
    void updateRejectsNullJourneyTemplate() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        applyStageDefaults(updateReq);
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(null);

        assertThatThrownBy(() -> service.update(exp.getId(), updateReq))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("journeyTemplateId required");
    }

    @Test
    void updateClearsLeadPortalFlowWhenNullIsProvided() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        Long flowId = createLeadPortalFlow(niche);
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(flowId);
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        applyStageDefaults(updateReq);
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(template.getId());
        updateReq.setLeadPortalFlowId(null);

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getLeadPortalFlow()).isNull();
    }

    @Test
    void updateChangesLeadPortalFlowWhenProvided() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Teste").build());
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        var hyp = hypothesisRepository.save(com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .build());
        metricPresetRepository.save(MetricPreset.builder()
                .id("LEAN_150")
                .name("Lean-Startup 150")
                .sampleSize(150)
                .stopLossFactor(new BigDecimal("2"))
                .defaultMdePp(new BigDecimal("12"))
                .build());
        Long firstFlow = createLeadPortalFlow(niche);
        Long secondFlow = createLeadPortalFlow(niche);
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        CreateExperimentRequest req = new CreateExperimentRequest();
        applyStageDefaults(req);
        req.setMarketNicheId(niche.getId());
        req.setHypothesisId(hyp.getId());
        req.setName("Exp1");
        req.setHypothesis("Teste");
        req.setKpiTargetCpl(new BigDecimal("45"));
        req.setMetricPresetId("LEAN_150");
        req.setJourneyTemplateId(template.getId());
        req.setInstagramAccountId(createInstagramAccount().getId());
        req.setLeadPortalFlowId(firstFlow);
        Experiment exp = service.create(req);

        UpdateExperimentRequest updateReq = new UpdateExperimentRequest();
        applyStageDefaults(updateReq);
        updateReq.setName("Exp1");
        updateReq.setHypothesis("Teste");
        updateReq.setKpiTargetCpl(new BigDecimal("45"));
        updateReq.setMetricPresetId("LEAN_150");
        updateReq.setJourneyTemplateId(template.getId());
        updateReq.setLeadPortalFlowId(secondFlow);

        Experiment updated = service.update(exp.getId(), updateReq);

        assertThat(updated.getLeadPortalFlow()).isNotNull();
        assertThat(updated.getLeadPortalFlow().getId()).isEqualTo(secondFlow);
    }
}
