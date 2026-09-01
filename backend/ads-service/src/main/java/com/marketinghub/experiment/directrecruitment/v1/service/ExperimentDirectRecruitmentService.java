package com.marketinghub.experiment.directrecruitment.v1.service;

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
import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentVisit;
import com.marketinghub.experiment.directrecruitment.v1.service.activate.ActivateDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.campaign.DirectRecruitmentCampaignResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.createdraft.CreateDirectRecruitmentDraftRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.pause.PauseDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.publicview.PublicDirectRecruitmentResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.visit.RegisterDirectRecruitmentVisitRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.visit.RegisterDirectRecruitmentVisitResponse;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentCampaignRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentSubmissionRepository;
import com.marketinghub.repository.jpa.experimentdirectrecruitment.ExperimentDirectRecruitmentVisitRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialAccountRepository;
import com.marketinghub.socialdistribution.SocialAccountStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: orquestrar o convite, a qualificação e a adesão à amostra direta. */
@Service
public class ExperimentDirectRecruitmentService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentDirectRecruitmentService.class);
  private static final String CONTRACT_VERSION = "direct-recruitment-v1";
  private static final String CONSENT_VERSION = "consent-v1";
  private static final String DEFAULT_HEADLINE =
      "Seu atendimento no WhatsApp poderia vender mais sem você virar refém do celular?";
  private static final String DEFAULT_BODY =
      "Estamos selecionando pequenos prestadores de serviços para validar uma implantação prática "
          + "que organiza respostas, perguntas e follow-ups do WhatsApp com revisão humana. "
          + "A participação leva menos de dois minutos e não exige compra.";
  private static final String DEFAULT_CONSENT =
      "Aceito participar desta validação comercial, receber agora a apresentação da oferta e "
          + "autorizo o uso das respostas categóricas para medir o experimento. Meu telefone ou "
          + "e-mail será transformado em fingerprint pseudonimizado no meu navegador antes do envio.";
  private static final String RECORDED_BY = "Marketing Hub / recrutamento inbound";

  private final ExperimentRepository experiments;
  private final ExperimentDirectRecruitmentCampaignRepository campaigns;
  private final ExperimentDirectRecruitmentVisitRepository visits;
  private final ExperimentDirectRecruitmentSubmissionRepository submissions;
  private final ExperimentDirectContactService directContacts;
  private final PublicProductCommercialOfferService offers;
  private final SocialAccountRepository socialAccounts;
  private final Clock clock;

  /** Configura as fontes canônicas do experimento, da oferta e da distribuição orgânica. */
  @Autowired
  public ExperimentDirectRecruitmentService(
      ExperimentRepository experiments,
      ExperimentDirectRecruitmentCampaignRepository campaigns,
      ExperimentDirectRecruitmentVisitRepository visits,
      ExperimentDirectRecruitmentSubmissionRepository submissions,
      ExperimentDirectContactService directContacts,
      PublicProductCommercialOfferService offers,
      SocialAccountRepository socialAccounts) {
    this(
        experiments,
        campaigns,
        visits,
        submissions,
        directContacts,
        offers,
        socialAccounts,
        Clock.systemUTC());
  }

  /** Permite testes determinísticos do fluxo e de seus horários auditáveis. */
  ExperimentDirectRecruitmentService(
      ExperimentRepository experiments,
      ExperimentDirectRecruitmentCampaignRepository campaigns,
      ExperimentDirectRecruitmentVisitRepository visits,
      ExperimentDirectRecruitmentSubmissionRepository submissions,
      ExperimentDirectContactService directContacts,
      PublicProductCommercialOfferService offers,
      SocialAccountRepository socialAccounts,
      Clock clock) {
    this.experiments = experiments;
    this.campaigns = campaigns;
    this.visits = visits;
    this.submissions = submissions;
    this.directContacts = directContacts;
    this.offers = offers;
    this.socialAccounts = socialAccounts;
    this.clock = clock;
  }

  /** Consulta a atividade de aquisição sem criar registros em uma operação de leitura. */
  @Transactional(readOnly = true)
  public DirectRecruitmentCampaignResponse getCampaign(Long experimentId) {
    Experiment experiment = directExperiment(experimentId);
    return campaigns
        .findByExperimentId(experimentId)
        .map(this::campaignResponse)
        .orElseGet(() -> notCreatedResponse(experiment, offer(experiment)));
  }

  /** Cria um rascunho único com a oferta vigente e sem realizar publicação externa. */
  @Transactional
  public DirectRecruitmentCampaignResponse createDraft(
      Long experimentId, CreateDirectRecruitmentDraftRequest request) {
    Experiment experiment = directRunningExperiment(experimentId);
    if (campaigns.findByExperimentId(experimentId).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O experimento já possui uma atividade de recrutamento.");
    }
    PublicProductCommercialOfferResponse offer = offer(experiment);
    Instant now = Instant.now(clock);
    ExperimentDirectRecruitmentCampaign campaign = new ExperimentDirectRecruitmentCampaign();
    campaign.setExperiment(experiment);
    campaign.setPublicToken(UUID.randomUUID().toString());
    campaign.setStatus(DirectRecruitmentCampaignStatus.DRAFT);
    campaign.setContractVersion(CONTRACT_VERSION);
    campaign.setHeadline(DEFAULT_HEADLINE);
    campaign.setBodyText(DEFAULT_BODY);
    campaign.setAudienceSummary(limit(offer.targetAudience(), 1000));
    campaign.setConsentText(DEFAULT_CONSENT);
    campaign.setConsentVersion(CONSENT_VERSION);
    campaign.setOfferUrl(offer.salesPageUrl());
    campaign.setOfferCta(offer.primaryCta());
    campaign.setPrivacyPolicyUrl(offer.privacyUrl());
    campaign.setCreatedBy(request.createdBy().trim());
    campaign.setStatusReason("Rascunho preparado; aguarda aprovação humana.");
    campaign.setCreatedAt(now);
    campaign.setUpdatedAt(now);
    try {
      campaigns.saveAndFlush(campaign);
    } catch (DataIntegrityViolationException ex) {
      LOGGER.error(
          "Falha ao criar recrutamento direto. modulo=backend operacao=createDraft experimentId={}",
          experimentId,
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O experimento já possui uma atividade de recrutamento.", ex);
    }
    LOGGER.info(
        "experiment_direct_recruitment_draft_created experimentId={} campaignId={} createdBy={}",
        experimentId,
        campaign.getId(),
        campaign.getCreatedBy());
    return campaignResponse(campaign);
  }

  /**
   * Ativa somente um convite aprovado, preservando distribuição e gasto como comandos separados.
   */
  @Transactional
  public DirectRecruitmentCampaignResponse activate(
      Long experimentId, ActivateDirectRecruitmentRequest request) {
    ExperimentDirectRecruitmentCampaign campaign = campaignForUpdate(experimentId);
    requireRunning(campaign.getExperiment());
    ExperimentDirectContactSampleResponse sample = directContacts.getSample(experimentId);
    if (sample.readyForHermesReview()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A amostra já está completa e não aceita nova ativação.");
    }
    if (campaign.getStatus() == DirectRecruitmentCampaignStatus.ACTIVE) {
      return campaignResponse(campaign);
    }
    if (campaign.getStatus() == DirectRecruitmentCampaignStatus.COMPLETED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "O recrutamento já foi concluído.");
    }
    if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Confirme a aprovação humana antes de ativar o convite.");
    }
    Instant now = Instant.now(clock);
    campaign.setStatus(DirectRecruitmentCampaignStatus.ACTIVE);
    campaign.setStatusChangedBy(request.approvedBy().trim());
    campaign.setStatusReason(
        "Convite aprovado e disponível; distribuição externa permanece separada.");
    campaign.setActivatedAt(now);
    campaign.setPausedAt(null);
    campaign.setUpdatedAt(now);
    campaigns.save(campaign);
    LOGGER.info(
        "experiment_direct_recruitment_activated experimentId={} campaignId={} approvedBy={}",
        experimentId,
        campaign.getId(),
        campaign.getStatusChangedBy());
    return campaignResponse(campaign);
  }

  /** Pausa novas visitas e adesões sem apagar a auditoria já acumulada. */
  @Transactional
  public DirectRecruitmentCampaignResponse pause(
      Long experimentId, PauseDirectRecruitmentRequest request) {
    ExperimentDirectRecruitmentCampaign campaign = campaignForUpdate(experimentId);
    if (campaign.getStatus() != DirectRecruitmentCampaignStatus.ACTIVE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente um recrutamento ativo pode ser pausado.");
    }
    Instant now = Instant.now(clock);
    campaign.setStatus(DirectRecruitmentCampaignStatus.PAUSED);
    campaign.setStatusChangedBy(request.pausedBy().trim());
    campaign.setStatusReason(request.reason().trim());
    campaign.setPausedAt(now);
    campaign.setUpdatedAt(now);
    campaigns.save(campaign);
    LOGGER.info(
        "experiment_direct_recruitment_paused experimentId={} campaignId={} pausedBy={} reason={}",
        experimentId,
        campaign.getId(),
        campaign.getStatusChangedBy(),
        campaign.getStatusReason());
    return campaignResponse(campaign);
  }

  /** Expõe somente o conteúdo necessário para a pessoa decidir participar. */
  @Transactional(readOnly = true)
  public PublicDirectRecruitmentResponse getPublicCampaign(String token) {
    ExperimentDirectRecruitmentCampaign campaign = publicCampaign(token);
    if (campaign.getStatus() == DirectRecruitmentCampaignStatus.DRAFT) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Convite de participação não encontrado.");
    }
    ExperimentDirectContactSampleResponse sample =
        directContacts.getSample(campaign.getExperiment().getId());
    boolean accepting = accepting(campaign, sample);
    return new PublicDirectRecruitmentResponse(
        campaign.getPublicToken(),
        campaign.getExperiment().getId(),
        campaign.getStatus().name(),
        accepting,
        campaign.getExperiment().getProduct().getName(),
        campaign.getHeadline(),
        campaign.getBodyText(),
        campaign.getAudienceSummary(),
        campaign.getConsentText(),
        campaign.getConsentVersion(),
        campaign.getPrivacyPolicyUrl(),
        sample.targetContacts(),
        sample.remainingContacts(),
        availabilityMessage(campaign, sample, accepting));
  }

  /** Conta uma visita única sem IP, user-agent ou identidade em claro. */
  @Transactional
  public RegisterDirectRecruitmentVisitResponse registerVisit(
      String token, RegisterDirectRecruitmentVisitRequest request) {
    LOGGER.info(
        "experiment_direct_recruitment_visit_received tokenSuffix={} payload={}",
        tokenSuffix(token),
        request);
    ExperimentDirectRecruitmentCampaign campaign = publicCampaignForUpdate(token);
    requireAccepting(campaign);
    String fingerprint = request.visitorFingerprint().trim().toLowerCase(Locale.ROOT);
    if (visits.existsByCampaignIdAndVisitorFingerprint(campaign.getId(), fingerprint)) {
      return new RegisterDirectRecruitmentVisitResponse(
          false, visits.countByCampaignId(campaign.getId()));
    }
    Instant now = Instant.now(clock);
    ExperimentDirectRecruitmentVisit visit = new ExperimentDirectRecruitmentVisit();
    visit.setCampaign(campaign);
    visit.setVisitorFingerprint(fingerprint);
    visit.setUtmSource(optional(request.utmSource()));
    visit.setUtmMedium(optional(request.utmMedium()));
    visit.setUtmCampaign(optional(request.utmCampaign()));
    visit.setUtmContent(optional(request.utmContent()));
    visit.setFirstVisitedAt(now);
    visit.setCreatedAt(now);
    visits.saveAndFlush(visit);
    long total = visits.countByCampaignId(campaign.getId());
    LOGGER.info(
        "experiment_direct_recruitment_visit_recorded campaignId={} uniqueVisits={}",
        campaign.getId(),
        total);
    return new RegisterDirectRecruitmentVisitResponse(true, total);
  }

  /** Qualifica a adesão e contabiliza somente um contato consentido realmente aderente. */
  @Transactional
  public SubmitDirectRecruitmentResponse submit(
      String token, SubmitDirectRecruitmentRequest request) {
    LOGGER.info(
        "experiment_direct_recruitment_submission_received tokenSuffix={} payload={}",
        tokenSuffix(token),
        request);
    ExperimentDirectRecruitmentCampaign campaign = publicCampaignForUpdate(token);
    String submissionKey = request.submissionKey().trim().toLowerCase(Locale.ROOT);
    var existing = submissions.findByCampaignIdAndSubmissionKey(campaign.getId(), submissionKey);
    if (existing.isPresent()) {
      return submissionResponse(campaign, existing.get());
    }
    requireAccepting(campaign);
    requireConsent(campaign, request);
    String fingerprint = request.contactFingerprint().trim().toLowerCase(Locale.ROOT);
    if (submissions.existsByCampaignIdAndContactFingerprint(campaign.getId(), fingerprint)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Esta pessoa já participou desta validação.");
    }
    Instant now = Instant.now(clock);
    boolean qualified = qualifies(request);
    ExperimentDirectRecruitmentSubmission submission =
        buildSubmission(campaign, request, submissionKey, fingerprint, qualified, now);
    try {
      submissions.saveAndFlush(submission);
    } catch (DataIntegrityViolationException ex) {
      LOGGER.warn(
          "Falha ao persistir adesão direta. modulo=backend operacao=submitRecruitment campaignId={}",
          campaign.getId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Esta adesão já foi contabilizada.", ex);
    }
    if (qualified) {
      ExperimentDirectContactSampleResponse sample =
          directContacts.register(
              campaign.getExperiment().getId(),
              new RegisterExperimentDirectContactRequest(
                  fingerprint,
                  "internal://direct-recruitment/"
                      + campaign.getId()
                      + "/submissions/"
                      + submission.getId(),
                  now,
                  now,
                  true,
                  RECORDED_BY));
      if (sample.readyForHermesReview()) {
        completeCampaign(campaign, now);
      }
    }
    LOGGER.info(
        "experiment_direct_recruitment_submission_recorded campaignId={} submissionId={} status={}",
        campaign.getId(),
        submission.getId(),
        submission.getStatus());
    return submissionResponse(campaign, submission);
  }

  /** Monta uma adesão sem carregar qualquer identidade original. */
  private ExperimentDirectRecruitmentSubmission buildSubmission(
      ExperimentDirectRecruitmentCampaign campaign,
      SubmitDirectRecruitmentRequest request,
      String submissionKey,
      String fingerprint,
      boolean qualified,
      Instant now) {
    ExperimentDirectRecruitmentSubmission submission = new ExperimentDirectRecruitmentSubmission();
    submission.setCampaign(campaign);
    submission.setSubmissionKey(submissionKey);
    submission.setContactFingerprint(fingerprint);
    submission.setServiceSegment(request.serviceSegment());
    submission.setWeeklyConversationsRange(request.weeklyConversationsRange());
    submission.setUsesWhatsapp(request.usesWhatsapp());
    submission.setDecisionMaker(request.decisionMaker());
    submission.setWantsPersonalizedImplementation(request.wantsPersonalizedImplementation());
    submission.setConsentAccepted(true);
    submission.setConsentVersion(request.consentVersion());
    submission.setStatus(
        qualified
            ? DirectRecruitmentSubmissionStatus.QUALIFIED
            : DirectRecruitmentSubmissionStatus.NOT_QUALIFIED);
    submission.setQualificationReason(qualificationReason(request));
    submission.setUtmSource(optional(request.utmSource()));
    submission.setUtmMedium(optional(request.utmMedium()));
    submission.setUtmCampaign(optional(request.utmCampaign()));
    submission.setUtmContent(optional(request.utmContent()));
    submission.setSubmittedAt(now);
    submission.setCreatedAt(now);
    return submission;
  }

  /** Marca a atividade concluída quando a amostra oficial alcança exatamente a meta. */
  private void completeCampaign(ExperimentDirectRecruitmentCampaign campaign, Instant now) {
    campaign.setStatus(DirectRecruitmentCampaignStatus.COMPLETED);
    campaign.setStatusReason("A amostra consentida atingiu a meta do experimento.");
    campaign.setCompletedAt(now);
    campaign.setUpdatedAt(now);
    campaigns.save(campaign);
  }

  /** Devolve resultado idempotente e libera a oferta apenas para uma adesão qualificada. */
  private SubmitDirectRecruitmentResponse submissionResponse(
      ExperimentDirectRecruitmentCampaign campaign,
      ExperimentDirectRecruitmentSubmission submission) {
    ExperimentDirectContactSampleResponse sample =
        directContacts.getSample(campaign.getExperiment().getId());
    boolean qualified = submission.getStatus() == DirectRecruitmentSubmissionStatus.QUALIFIED;
    return new SubmitDirectRecruitmentResponse(
        submission.getId(),
        submission.getStatus().name(),
        qualified,
        qualified
            ? "Seu perfil é aderente. A apresentação da oferta já está disponível."
            : "Obrigado. Seu perfil não atende aos critérios deste piloto, e nenhum contato foi contabilizado.",
        qualified ? campaign.getOfferUrl() : null,
        sample.remainingContacts(),
        sample.readyForHermesReview());
  }

  /** Resume conteúdo e métricas persistidas para a tela administrativa. */
  private DirectRecruitmentCampaignResponse campaignResponse(
      ExperimentDirectRecruitmentCampaign campaign) {
    Long campaignId = campaign.getId();
    ExperimentDirectContactSampleResponse sample =
        directContacts.getSample(campaign.getExperiment().getId());
    long connected = socialAccounts.countByStatus(SocialAccountStatus.CONNECTED);
    String acquisitionStatus = acquisitionStatus(campaign, sample, connected);
    return new DirectRecruitmentCampaignResponse(
        campaignId,
        campaign.getExperiment().getId(),
        campaign.getExperiment().getProduct().getName(),
        campaign.getStatus().name(),
        campaign.getContractVersion(),
        campaign.getHeadline(),
        campaign.getBodyText(),
        campaign.getAudienceSummary(),
        campaign.getConsentText(),
        campaign.getConsentVersion(),
        campaign.getOfferUrl(),
        campaign.getOfferCta(),
        campaign.getPrivacyPolicyUrl(),
        "/participar/" + campaign.getPublicToken(),
        sample.targetContacts(),
        sample.remainingContacts(),
        visits.countByCampaignId(campaignId),
        submissions.countByCampaignId(campaignId),
        submissions.countByCampaignIdAndStatus(
            campaignId, DirectRecruitmentSubmissionStatus.QUALIFIED),
        submissions.countByCampaignIdAndStatus(
            campaignId, DirectRecruitmentSubmissionStatus.NOT_QUALIFIED),
        sample.recordedContacts(),
        connected,
        acquisitionStatus,
        distributionGuidance(acquisitionStatus),
        campaign.getCreatedBy(),
        campaign.getStatusChangedBy(),
        campaign.getStatusReason(),
        campaign.getCreatedAt(),
        campaign.getUpdatedAt(),
        campaign.getActivatedAt(),
        campaign.getPausedAt(),
        campaign.getCompletedAt());
  }

  /** Apresenta a próxima atividade sem gravar um rascunho durante o GET. */
  private DirectRecruitmentCampaignResponse notCreatedResponse(
      Experiment experiment, PublicProductCommercialOfferResponse offer) {
    ExperimentDirectContactSampleResponse sample = directContacts.getSample(experiment.getId());
    long connected = socialAccounts.countByStatus(SocialAccountStatus.CONNECTED);
    return new DirectRecruitmentCampaignResponse(
        null,
        experiment.getId(),
        experiment.getProduct().getName(),
        "NOT_CREATED",
        CONTRACT_VERSION,
        DEFAULT_HEADLINE,
        DEFAULT_BODY,
        limit(offer.targetAudience(), 1000),
        DEFAULT_CONSENT,
        CONSENT_VERSION,
        offer.salesPageUrl(),
        offer.primaryCta(),
        offer.privacyUrl(),
        null,
        sample.targetContacts(),
        sample.remainingContacts(),
        0,
        0,
        0,
        0,
        sample.recordedContacts(),
        connected,
        "NOT_CREATED",
        "Crie o rascunho e aprove o convite antes de iniciar a aquisição.",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** Resolve a oferta canônica e impede vincular outro experimento do mesmo produto. */
  private PublicProductCommercialOfferResponse offer(Experiment experiment) {
    Product product = experiment.getProduct();
    if (product == null || !StringUtils.hasText(product.getSlug())) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "O experimento não possui produto comercial vinculado.");
    }
    PublicProductCommercialOfferResponse offer = offers.getOffer(product.getSlug());
    if (!experiment.getId().equals(offer.experimentId())) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED,
          "A oferta pública vigente pertence a outro experimento do produto.");
    }
    if (!offer.salesPageUrl().startsWith("https://")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "A oferta pública precisa usar HTTPS.");
    }
    if (!StringUtils.hasText(offer.privacyUrl()) || !offer.privacyUrl().startsWith("https://")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED,
          "A oferta pública precisa expor uma política de privacidade HTTPS.");
    }
    return offer;
  }

  /** Carrega e valida o experimento do canal individual. */
  private Experiment directExperiment(Long experimentId) {
    Experiment experiment =
        experiments
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado."));
    if (experiment.getPlatform() != ExperimentPlatform.DIRECT_ONE_TO_ONE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O recrutamento consentido existe somente para DIRECT_ONE_TO_ONE.");
    }
    return experiment;
  }

  /** Exige um experimento direto ativo para preparar o convite. */
  private Experiment directRunningExperiment(Long experimentId) {
    Experiment experiment = directExperiment(experimentId);
    requireRunning(experiment);
    return experiment;
  }

  /** Impede aquisição fora da janela comercial vigente. */
  private void requireRunning(Experiment experiment) {
    if (experiment.getStatus() != ExperimentStatus.RUNNING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O experimento precisa estar RUNNING para recrutar a amostra.");
    }
  }

  /** Carrega o convite administrativo com lock para mudanças de estado. */
  private ExperimentDirectRecruitmentCampaign campaignForUpdate(Long experimentId) {
    return campaigns
        .findByExperimentIdForUpdate(experimentId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Atividade de recrutamento não encontrada."));
  }

  /** Carrega o convite público pelo token opaco. */
  private ExperimentDirectRecruitmentCampaign publicCampaign(String token) {
    return campaigns
        .findByPublicToken(normalizeToken(token))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Convite de participação não encontrado."));
  }

  /** Carrega e bloqueia o convite durante visita ou adesão. */
  private ExperimentDirectRecruitmentCampaign publicCampaignForUpdate(String token) {
    return campaigns
        .findByPublicTokenForUpdate(normalizeToken(token))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Convite de participação não encontrado."));
  }

  /** Normaliza e limita o token público ao formato UUID. */
  private String normalizeToken(String token) {
    try {
      return UUID.fromString(token).toString();
    } catch (RuntimeException ex) {
      LOGGER.warn(
          "Token inválido no recrutamento direto. modulo=backend operacao=normalizeToken tokenLength={}",
          token == null ? 0 : token.length(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Convite de participação não encontrado.", ex);
    }
  }

  /** Expõe somente um sufixo do token para correlação segura dos logs de ingestão. */
  private String tokenSuffix(String token) {
    if (token == null) {
      return "missing";
    }
    return token.substring(Math.max(0, token.length() - 8));
  }

  /** Exige convite ativo, experimento aberto e vaga restante. */
  private void requireAccepting(ExperimentDirectRecruitmentCampaign campaign) {
    ExperimentDirectContactSampleResponse sample =
        directContacts.getSample(campaign.getExperiment().getId());
    if (!accepting(campaign, sample)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, availabilityMessage(campaign, sample, false));
    }
  }

  /** Calcula a disponibilidade pública somente a partir do estado persistido. */
  private boolean accepting(
      ExperimentDirectRecruitmentCampaign campaign, ExperimentDirectContactSampleResponse sample) {
    return campaign.getStatus() == DirectRecruitmentCampaignStatus.ACTIVE
        && campaign.getExperiment().getStatus() == ExperimentStatus.RUNNING
        && !sample.readyForHermesReview();
  }

  /** Explica por que o convite aceita ou bloqueia novas adesões. */
  private String availabilityMessage(
      ExperimentDirectRecruitmentCampaign campaign,
      ExperimentDirectContactSampleResponse sample,
      boolean accepting) {
    if (accepting) {
      return "A validação está aberta e possui " + sample.remainingContacts() + " vagas.";
    }
    if (sample.readyForHermesReview()
        || campaign.getStatus() == DirectRecruitmentCampaignStatus.COMPLETED) {
      return "A validação atingiu a amostra planejada e foi encerrada.";
    }
    if (campaign.getExperiment().getStatus() != ExperimentStatus.RUNNING) {
      return "A validação comercial não está em execução.";
    }
    return "Este convite está " + campaign.getStatus().name().toLowerCase(Locale.ROOT) + ".";
  }

  /** Exige a versão de consentimento exibida na página ativa. */
  private void requireConsent(
      ExperimentDirectRecruitmentCampaign campaign, SubmitDirectRecruitmentRequest request) {
    if (!Boolean.TRUE.equals(request.consentAccepted())
        || !campaign.getConsentVersion().equals(request.consentVersion())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O consentimento exibido precisa ser aceito na versão vigente.");
    }
  }

  /** Aplica a qualificação mínima e determinística do piloto Rigel. */
  private boolean qualifies(SubmitDirectRecruitmentRequest request) {
    return Boolean.TRUE.equals(request.usesWhatsapp())
        && Boolean.TRUE.equals(request.decisionMaker())
        && Boolean.TRUE.equals(request.wantsPersonalizedImplementation());
  }

  /** Registra a causa funcional da qualificação sem decisão opaca de IA. */
  private String qualificationReason(SubmitDirectRecruitmentRequest request) {
    if (!Boolean.TRUE.equals(request.usesWhatsapp())) {
      return "Não usa WhatsApp no atendimento comercial.";
    }
    if (!Boolean.TRUE.equals(request.decisionMaker())) {
      return "Não decide sobre a organização do próprio atendimento.";
    }
    if (!Boolean.TRUE.equals(request.wantsPersonalizedImplementation())) {
      return "Não deseja uma implantação personalizada neste momento.";
    }
    return "Usa WhatsApp, decide a compra e deseja implantação personalizada.";
  }

  /** Converte o estado persistido em próximo passo operacional de aquisição. */
  private String acquisitionStatus(
      ExperimentDirectRecruitmentCampaign campaign,
      ExperimentDirectContactSampleResponse sample,
      long connectedAccounts) {
    if (sample.readyForHermesReview()
        || campaign.getStatus() == DirectRecruitmentCampaignStatus.COMPLETED) {
      return "SAMPLE_COMPLETE";
    }
    if (campaign.getStatus() == DirectRecruitmentCampaignStatus.DRAFT) {
      return "DRAFT_REQUIRES_APPROVAL";
    }
    if (campaign.getStatus() == DirectRecruitmentCampaignStatus.PAUSED) {
      return "PAUSED";
    }
    return connectedAccounts > 0 ? "READY_FOR_ORGANIC_DISTRIBUTION" : "ACTIVE_WITHOUT_DISTRIBUTION";
  }

  /** Explica a ação seguinte sem inferir que ativação já gerou alcance. */
  private String distributionGuidance(String acquisitionStatus) {
    return switch (acquisitionStatus) {
      case "DRAFT_REQUIRES_APPROVAL" ->
          "Revise a comunicação e aprove o convite. A ativação não publica conteúdo.";
      case "ACTIVE_WITHOUT_DISTRIBUTION" ->
          "Conecte uma conta orgânica no Marketing Hub antes de distribuir o convite.";
      case "READY_FOR_ORGANIC_DISTRIBUTION" ->
          "Existe canal conectado. Crie a distribuição orgânica como comando separado e auditável.";
      case "PAUSED" -> "Reavalie a causa da pausa antes de reativar o convite.";
      case "SAMPLE_COMPLETE" ->
          "A amostra está pronta para uma única revisão de Hermes com resultados reais.";
      default -> "Crie o rascunho e aprove o convite antes de iniciar a aquisição.";
    };
  }

  /** Normaliza um parâmetro opcional sem persistir espaços vazios. */
  private String optional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Limita texto canônico ao contrato físico sem inventar conteúdo alternativo. */
  private String limit(String value, int maxLength) {
    String normalized =
        StringUtils.hasText(value) ? value.trim() : "Pequenos prestadores de serviços.";
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }
}
