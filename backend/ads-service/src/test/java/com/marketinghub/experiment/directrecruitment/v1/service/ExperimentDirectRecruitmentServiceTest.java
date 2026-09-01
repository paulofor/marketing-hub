package com.marketinghub.experiment.directrecruitment.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.directcontact.v1.ExperimentDirectContactSampleResponse;
import com.marketinghub.experiment.directcontact.v1.ExperimentDirectContactService;
import com.marketinghub.experiment.directcontact.v1.RegisterExperimentDirectContactRequest;
import com.marketinghub.experiment.directrecruitment.v1.DirectRecruitmentCampaignStatus;
import com.marketinghub.experiment.directrecruitment.v1.DirectRecruitmentSubmissionStatus;
import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentCampaign;
import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentSubmission;
import com.marketinghub.experiment.directrecruitment.v1.service.activate.ActivateDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.createdraft.CreateDirectRecruitmentDraftRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.visit.RegisterDirectRecruitmentVisitRequest;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentCampaignRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentSubmissionRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentVisitRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar formação consentida, segregada e limitada da amostra direta. */
class ExperimentDirectRecruitmentServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-01T14:00:00Z");
  private static final String TOKEN = "11111111-2222-4333-8444-555555555555";
  private ExperimentRepository experiments;
  private ExperimentDirectRecruitmentCampaignRepository campaigns;
  private ExperimentDirectRecruitmentVisitRepository visits;
  private ExperimentDirectRecruitmentSubmissionRepository submissions;
  private ExperimentDirectContactService directContacts;
  private PublicProductCommercialOfferService offers;
  private SocialAccountRepository socialAccounts;
  private ExperimentDirectRecruitmentService service;

  /** Prepara dependências isoladas e respostas padrão sem evidência comercial. */
  @BeforeEach
  void setUp() {
    experiments = mock(ExperimentRepository.class);
    campaigns = mock(ExperimentDirectRecruitmentCampaignRepository.class);
    visits = mock(ExperimentDirectRecruitmentVisitRepository.class);
    submissions = mock(ExperimentDirectRecruitmentSubmissionRepository.class);
    directContacts = mock(ExperimentDirectContactService.class);
    offers = mock(PublicProductCommercialOfferService.class);
    socialAccounts = mock(SocialAccountRepository.class);
    service =
        new ExperimentDirectRecruitmentService(
            experiments,
            campaigns,
            visits,
            submissions,
            directContacts,
            offers,
            socialAccounts,
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(directContacts.getSample(89L)).thenReturn(sample(0));
  }

  /** Cria somente um rascunho e preserva publicação como etapa posterior. */
  @Test
  void shouldCreateDraftFromCanonicalOfferWithoutPublishing() {
    Experiment experiment = experiment();
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(campaigns.findByExperimentId(89L)).thenReturn(Optional.empty());
    when(offers.getOffer("kit-whatsapp-pronto")).thenReturn(offer());
    doAnswer(
            invocation -> {
              ExperimentDirectRecruitmentCampaign saved = invocation.getArgument(0);
              saved.setId(10L);
              return saved;
            })
        .when(campaigns)
        .saveAndFlush(any(ExperimentDirectRecruitmentCampaign.class));

    var response = service.createDraft(89L, new CreateDirectRecruitmentDraftRequest("Operador QA"));

    ArgumentCaptor<ExperimentDirectRecruitmentCampaign> persisted =
        ArgumentCaptor.forClass(ExperimentDirectRecruitmentCampaign.class);
    verify(campaigns).saveAndFlush(persisted.capture());
    assertThat(persisted.getValue().getStatus()).isEqualTo(DirectRecruitmentCampaignStatus.DRAFT);
    assertThat(persisted.getValue().getOfferUrl()).isEqualTo("https://kit-whatsapp-pronto.example");
    assertThat(persisted.getValue().getActivatedAt()).isNull();
    assertThat(response.acquisitionStatus()).isEqualTo("DRAFT_REQUIRES_APPROVAL");
    assertThat(response.uniqueVisits()).isZero();
  }

  /** Mantém a leitura do convite persistido independente de mudanças posteriores na oferta. */
  @Test
  void shouldReadExistingCampaignWithoutReloadingTheCommercialOffer() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(experiments.findById(89L)).thenReturn(Optional.of(campaign.getExperiment()));
    when(campaigns.findByExperimentId(89L)).thenReturn(Optional.of(campaign));

    var response = service.getCampaign(89L);

    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.offerUrl()).isEqualTo("https://kit-whatsapp-pronto.example");
    verify(offers, never()).getOffer(any());
  }

  /** Ativa o convite aprovado sem declarar que ele já foi distribuído. */
  @Test
  void shouldActivateWithExplicitApprovalAndReportMissingDistribution() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.DRAFT);
    when(campaigns.findByExperimentIdForUpdate(89L)).thenReturn(Optional.of(campaign));

    var response = service.activate(89L, new ActivateDirectRecruitmentRequest("Operador QA", true));

    assertThat(campaign.getStatus()).isEqualTo(DirectRecruitmentCampaignStatus.ACTIVE);
    assertThat(campaign.getActivatedAt()).isEqualTo(NOW);
    assertThat(response.acquisitionStatus()).isEqualTo("ACTIVE_WITHOUT_DISTRIBUTION");
    assertThat(response.distributionGuidance()).contains("Conecte uma conta orgânica");
  }

  /** Deduplica a mesma visita sem usar endereço IP ou user-agent. */
  @Test
  void shouldNotCountTheSameVisitorTwice() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicTokenForUpdate(TOKEN)).thenReturn(Optional.of(campaign));
    when(visits.existsByCampaignIdAndVisitorFingerprint(10L, "a".repeat(64))).thenReturn(true);
    when(visits.countByCampaignId(10L)).thenReturn(3L);

    var response =
        service.registerVisit(
            TOKEN,
            new RegisterDirectRecruitmentVisitRequest(
                "a".repeat(64), "instagram", "organic", "rigel", null));

    assertThat(response.counted()).isFalse();
    assertThat(response.uniqueVisits()).isEqualTo(3);
    verify(visits, never()).saveAndFlush(any());
  }

  /** Uma adesão qualificada entra na amostra e libera a oferta canônica. */
  @Test
  void shouldConvertQualifiedConsentIntoOfficialDirectContact() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicTokenForUpdate(TOKEN)).thenReturn(Optional.of(campaign));
    when(submissions.findByCampaignIdAndSubmissionKey(10L, TOKEN)).thenReturn(Optional.empty());
    when(submissions.existsByCampaignIdAndContactFingerprint(10L, "b".repeat(64)))
        .thenReturn(false);
    doAnswer(
            invocation -> {
              ExperimentDirectRecruitmentSubmission saved = invocation.getArgument(0);
              saved.setId(20L);
              return saved;
            })
        .when(submissions)
        .saveAndFlush(any(ExperimentDirectRecruitmentSubmission.class));
    when(directContacts.getSample(89L)).thenReturn(sample(0), sample(1));
    when(directContacts.register(any(), any())).thenReturn(sample(1));

    var response = service.submit(TOKEN, qualifiedRequest("b".repeat(64)));

    ArgumentCaptor<RegisterExperimentDirectContactRequest> registered =
        ArgumentCaptor.forClass(RegisterExperimentDirectContactRequest.class);
    verify(directContacts).register(org.mockito.ArgumentMatchers.eq(89L), registered.capture());
    assertThat(registered.getValue().contactFingerprint()).isEqualTo("b".repeat(64));
    assertThat(registered.getValue().consentEvidenceReference())
        .isEqualTo("internal://direct-recruitment/10/submissions/20");
    assertThat(response.qualified()).isTrue();
    assertThat(response.offerUrl()).isEqualTo("https://kit-whatsapp-pronto.example");
    assertThat(response.remainingContacts()).isEqualTo(14);
  }

  /** Uma adesão fora do perfil permanece auditada, mas não vira contato. */
  @Test
  void shouldKeepUnqualifiedSubmissionOutsideOfficialSample() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicTokenForUpdate(TOKEN)).thenReturn(Optional.of(campaign));
    when(submissions.findByCampaignIdAndSubmissionKey(10L, TOKEN)).thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              ExperimentDirectRecruitmentSubmission saved = invocation.getArgument(0);
              saved.setId(21L);
              return saved;
            })
        .when(submissions)
        .saveAndFlush(any(ExperimentDirectRecruitmentSubmission.class));
    SubmitDirectRecruitmentRequest request =
        new SubmitDirectRecruitmentRequest(
            "c".repeat(64),
            TOKEN,
            "CONSULTING",
            "ONE_TO_TEN",
            false,
            true,
            true,
            true,
            "consent-v1",
            null,
            null,
            null,
            null);

    var response = service.submit(TOKEN, request);

    ArgumentCaptor<ExperimentDirectRecruitmentSubmission> persisted =
        ArgumentCaptor.forClass(ExperimentDirectRecruitmentSubmission.class);
    verify(submissions).saveAndFlush(persisted.capture());
    assertThat(persisted.getValue().getStatus())
        .isEqualTo(DirectRecruitmentSubmissionStatus.NOT_QUALIFIED);
    verify(directContacts, never()).register(any(), any());
    assertThat(response.qualified()).isFalse();
    assertThat(response.offerUrl()).isNull();
  }

  /** Rejeita versão de consentimento diferente da apresentada na página. */
  @Test
  void shouldRejectStaleConsentVersion() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicTokenForUpdate(TOKEN)).thenReturn(Optional.of(campaign));
    when(submissions.findByCampaignIdAndSubmissionKey(10L, TOKEN)).thenReturn(Optional.empty());
    SubmitDirectRecruitmentRequest stale =
        new SubmitDirectRecruitmentRequest(
            "d".repeat(64),
            TOKEN,
            "EDUCATION",
            "OVER_THIRTY",
            true,
            true,
            true,
            true,
            "consent-v0",
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> service.submit(TOKEN, stale))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("versão vigente");

    verify(submissions, never()).saveAndFlush(any());
  }

  /** Bloqueia nova adesão quando a amostra oficial já atingiu 15/15. */
  @Test
  void shouldRejectSubmissionAfterSampleCompletion() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicTokenForUpdate(TOKEN)).thenReturn(Optional.of(campaign));
    when(submissions.findByCampaignIdAndSubmissionKey(10L, TOKEN)).thenReturn(Optional.empty());
    when(directContacts.getSample(89L)).thenReturn(sample(15));

    assertThatThrownBy(() -> service.submit(TOKEN, qualifiedRequest("e".repeat(64))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("atingiu a amostra");

    verify(submissions, never()).saveAndFlush(any());
  }

  /** A página pública nunca inclui a URL da oferta antes da adesão. */
  @Test
  void shouldExposePublicCopyWithoutOfferUrl() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.ACTIVE);
    when(campaigns.findByPublicToken(TOKEN)).thenReturn(Optional.of(campaign));

    var response = service.getPublicCampaign(TOKEN);

    assertThat(response.acceptingSubmissions()).isTrue();
    assertThat(response.headline()).contains("WhatsApp");
    assertThat(response.privacyPolicyUrl())
        .isEqualTo("https://kit-whatsapp-pronto.example/privacy");
    assertThat(response.getClass().getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .doesNotContain("offerUrl");
  }

  /** Mantém o token do rascunho inacessível até a aprovação humana. */
  @Test
  void shouldHideDraftFromPublicRoute() {
    ExperimentDirectRecruitmentCampaign campaign = campaign(DirectRecruitmentCampaignStatus.DRAFT);
    when(campaigns.findByPublicToken(TOKEN)).thenReturn(Optional.of(campaign));

    assertThatThrownBy(() -> service.getPublicCampaign(TOKEN))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não encontrado");

    verify(directContacts, never()).getSample(89L);
  }

  /** Cria o experimento direto mínimo com o produto comercial do Rigel. */
  private Experiment experiment() {
    Product product =
        Product.builder().id(9L).slug("kit-whatsapp-pronto").name("Kit WhatsApp Pronto").build();
    return Experiment.builder()
        .id(89L)
        .product(product)
        .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .status(ExperimentStatus.RUNNING)
        .sampleSize(15)
        .build();
  }

  /** Cria um convite persistido em um estado específico. */
  private ExperimentDirectRecruitmentCampaign campaign(DirectRecruitmentCampaignStatus status) {
    ExperimentDirectRecruitmentCampaign campaign = new ExperimentDirectRecruitmentCampaign();
    campaign.setId(10L);
    campaign.setExperiment(experiment());
    campaign.setPublicToken(TOKEN);
    campaign.setStatus(status);
    campaign.setContractVersion("direct-recruitment-v1");
    campaign.setHeadline("Seu atendimento no WhatsApp poderia vender mais?");
    campaign.setBodyText("Convite de validação.");
    campaign.setAudienceSummary("Prestadores de serviços.");
    campaign.setConsentText("Aceito participar.");
    campaign.setConsentVersion("consent-v1");
    campaign.setOfferUrl("https://kit-whatsapp-pronto.example");
    campaign.setOfferCta("Quero organizar meu WhatsApp");
    campaign.setPrivacyPolicyUrl("https://kit-whatsapp-pronto.example/privacy");
    campaign.setCreatedBy("Operador QA");
    campaign.setCreatedAt(NOW.minusSeconds(60));
    campaign.setUpdatedAt(NOW.minusSeconds(60));
    return campaign;
  }

  /** Monta a oferta pública validada usada na preparação do convite. */
  private PublicProductCommercialOfferResponse offer() {
    return new PublicProductCommercialOfferResponse(
        "kit-whatsapp-pronto",
        "pde-v2",
        "assisted-service-v2",
        89L,
        "RUNNING",
        "DIRECT_ONE_TO_ONE",
        "Improviso no atendimento",
        "Demonstração prática",
        "Organizar respostas e follow-ups",
        "Quero organizar meu WhatsApp",
        new BigDecimal("349.00"),
        "https://checkout.example",
        "https://kit-whatsapp-pronto.example",
        "Pequenos prestadores de serviços que atendem clientes pelo WhatsApp.",
        "Implantação",
        "Personalizada",
        "Atendimento organizado",
        "DigiCom",
        "00.000.000/0001-00",
        "suporte@example.com",
        "https://kit-whatsapp-pronto.example/terms",
        "https://kit-whatsapp-pronto.example/privacy",
        "https://kit-whatsapp-pronto.example/refund-policy");
  }

  /** Monta uma adesão válida e qualificada sem identidade em claro. */
  private SubmitDirectRecruitmentRequest qualifiedRequest(String fingerprint) {
    return new SubmitDirectRecruitmentRequest(
        fingerprint,
        TOKEN,
        "CONSULTING",
        "ELEVEN_TO_THIRTY",
        true,
        true,
        true,
        true,
        "consent-v1",
        "instagram",
        "organic",
        "rigel-pilot",
        "story-1");
  }

  /** Cria o placar oficial sem inventar checkout, pagamento ou venda. */
  private ExperimentDirectContactSampleResponse sample(long recorded) {
    return new ExperimentDirectContactSampleResponse(
        89L,
        "DIRECT_ONE_TO_ONE",
        "RUNNING",
        15,
        recorded,
        Math.max(0, 15 - recorded),
        recorded >= 15,
        recorded >= 15 ? "READY_FOR_HERMES_REVIEW" : "ACCUMULATING_CONSENTED_SAMPLE",
        List.of());
  }
}
