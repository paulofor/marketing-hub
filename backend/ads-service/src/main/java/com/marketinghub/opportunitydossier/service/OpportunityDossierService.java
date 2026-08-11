package com.marketinghub.opportunitydossier.service;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.opportunitydossier.*;
import com.marketinghub.opportunitydossier.service.convert.ConvertOpportunityRequest;
import com.marketinghub.opportunitydossier.service.create.CreateOpportunityDossierRequest;
import com.marketinghub.opportunitydossier.service.detail.OpportunityDossierResponse;
import com.marketinghub.opportunitydossier.service.evidence.AddOpportunityEvidenceRequest;
import com.marketinghub.opportunitydossier.service.review.SubmitOpportunityReviewRequest;
import com.marketinghub.opportunitydossier.service.status.UpdateOpportunityStatusRequest;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.productdiscovery.v1.service.CreateProductDiscoveryCycleRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryService;
import com.marketinghub.repository.jpa.opportunitydossier.*;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar pesquisa, pareceres e conversão de oportunidades em planos. */
@Service
public class OpportunityDossierService {
  private static final Set<String> REQUIRED_REVIEWERS =
      Set.of("ATENA", "PSIQUE", "PLUTUS", "HERMES");
  private final OpportunityDossierRepository dossierRepository;
  private final OpportunityEvidenceRepository evidenceRepository;
  private final OpportunityAgentReviewRepository reviewRepository;
  private final CommercialPlanService commercialPlanService;
  private final ProductDiscoveryService productDiscoveryService;
  private final ProductDiscoveryCycleRepository productDiscoveryCycleRepository;
  private final AgentTaskService agentTaskService;

  /** Configura persistência, conversão comercial e execução de pesquisa de Argos. */
  public OpportunityDossierService(
      OpportunityDossierRepository dossierRepository,
      OpportunityEvidenceRepository evidenceRepository,
      OpportunityAgentReviewRepository reviewRepository,
      CommercialPlanService commercialPlanService,
      ProductDiscoveryService productDiscoveryService,
      ProductDiscoveryCycleRepository productDiscoveryCycleRepository,
      AgentTaskService agentTaskService) {
    this.dossierRepository = dossierRepository;
    this.evidenceRepository = evidenceRepository;
    this.reviewRepository = reviewRepository;
    this.commercialPlanService = commercialPlanService;
    this.productDiscoveryService = productDiscoveryService;
    this.productDiscoveryCycleRepository = productDiscoveryCycleRepository;
    this.agentTaskService = agentTaskService;
  }

  /** Cadastra uma oportunidade sob responsabilidade de Argos ou agente informado. */
  @Transactional
  public OpportunityDossierResponse create(CreateOpportunityDossierRequest request) {
    require(request.title(), "title");
    require(request.ownerAgentKey(), "ownerAgentKey");
    require(request.targetAudience(), "targetAudience");
    require(request.mainPain(), "mainPain");
    require(request.referenceProduct(), "referenceProduct");
    require(request.aiAdvantage(), "aiAdvantage");
    if (request.preliminaryPrice() != null && request.preliminaryPrice().signum() < 0)
      bad("preliminaryPrice não pode ser negativo");
    OpportunityDossier saved =
        dossierRepository.save(
            OpportunityDossier.builder()
                .title(request.title().trim())
                .ownerAgentKey(request.ownerAgentKey().trim().toUpperCase())
                .targetAudience(request.targetAudience())
                .mainPain(request.mainPain())
                .referenceProduct(request.referenceProduct())
                .aiAdvantage(request.aiAdvantage())
                .proposedOffer(request.proposedOffer())
                .preliminaryPrice(request.preliminaryPrice())
                .deliveryModel(request.deliveryModel())
                .knownRisks(request.knownRisks())
                .experimentRecommendation(request.experimentRecommendation())
                .build());
    var cycle =
        productDiscoveryService.createCycle(
            new CreateProductDiscoveryCycleRequest(
                saved.getTitle(),
                saved.getTargetAudience(),
                "BR",
                "pt-BR",
                null,
                "Modelar produto comprovado melhorado por IA sem copiar ativos nem assumir vendas.",
                saved.getKnownRisks(),
                "Pesquisar evidências para o dossiê de oportunidade #" + saved.getId()));
    saved.setProductDiscoveryCycle(productDiscoveryCycleRepository.getReferenceById(cycle.id()));
    saved = dossierRepository.save(saved);
    agentTaskService.createByHuman(
        new CreateAgentTaskRequest(
            "market-radar",
            "Marketing Hub",
            "Pesquisar oportunidade: " + saved.getTitle(),
            "Produzir evidências auditáveis de demanda, lacunas, referência e vantagem por IA.",
            "HIGH",
            "opportunity-dossier:" + saved.getId()));
    return response(saved);
  }

  /** Lista o portfólio de oportunidades sem misturá-lo com planos comerciais. */
  @Transactional(readOnly = true)
  public List<OpportunityDossierResponse> list() {
    return dossierRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::response).toList();
  }

  /** Retorna o detalhe auditável do dossiê. */
  @Transactional(readOnly = true)
  public OpportunityDossierResponse get(Long id) {
    return response(find(id));
  }

  /** Acrescenta evidência verificável enquanto o dossiê ainda está em pesquisa. */
  @Transactional
  public OpportunityDossierResponse addEvidence(Long id, AddOpportunityEvidenceRequest request) {
    OpportunityDossier dossier = find(id);
    if (dossier.getStatus() != OpportunityDossierStatus.RESEARCHING)
      conflict("Evidências só podem ser adicionadas durante a pesquisa");
    require(request.sourceUrl(), "sourceUrl");
    require(request.summary(), "summary");
    require(request.createdBy(), "createdBy");
    evidenceRepository.save(
        OpportunityEvidence.builder()
            .dossier(dossier)
            .sourceUrl(request.sourceUrl())
            .summary(request.summary())
            .createdBy(request.createdBy())
            .build());
    return response(dossier);
  }

  /** Move o dossiê entre estados permitidos e abre os pareceres obrigatórios. */
  @Transactional
  public OpportunityDossierResponse updateStatus(Long id, UpdateOpportunityStatusRequest request) {
    OpportunityDossier dossier = find(id);
    if (request.status() == null) bad("status é obrigatório");
    OpportunityDossierStatus current = dossier.getStatus();
    if (current == OpportunityDossierStatus.RESEARCHING
        && request.status() == OpportunityDossierStatus.UNDER_REVIEW) {
      if (evidenceRepository.findByDossierIdOrderByCreatedAtAsc(id).isEmpty())
        conflict("Inclua ao menos uma evidência antes dos pareceres");
      REQUIRED_REVIEWERS.forEach(
          agent ->
              reviewRepository
                  .findByDossierIdAndAgentKey(id, agent)
                  .orElseGet(
                      () ->
                          reviewRepository.save(
                              OpportunityAgentReview.builder()
                                  .dossier(dossier)
                                  .agentKey(agent)
                                  .requestedAt(Instant.now())
                                  .build())));
    } else if (current == OpportunityDossierStatus.READY_FOR_TEST
        && request.status() == OpportunityDossierStatus.APPROVED) {
      require(request.decidedBy(), "decidedBy");
      dossier.setHumanDecisionBy(request.decidedBy());
      dossier.setHumanDecisionAt(Instant.now());
    } else if (request.status() == OpportunityDossierStatus.DISCARDED
        && current != OpportunityDossierStatus.CONVERTED_TO_PLAN) {
      require(request.decidedBy(), "decidedBy");
      dossier.setHumanDecisionBy(request.decidedBy());
      dossier.setHumanDecisionAt(Instant.now());
    } else {
      conflict("Transição de estado não permitida: " + current + " -> " + request.status());
    }
    dossier.setStatus(request.status());
    return response(dossierRepository.save(dossier));
  }

  /** Registra o parecer do agente solicitado e libera o teste quando todos concluírem. */
  @Transactional
  public OpportunityDossierResponse submitReview(
      Long id, String agentKey, SubmitOpportunityReviewRequest request) {
    OpportunityDossier dossier = find(id);
    if (dossier.getStatus() != OpportunityDossierStatus.UNDER_REVIEW)
      conflict("Dossiê não está recebendo pareceres");
    OpportunityAgentReview review =
        reviewRepository
            .findByDossierIdAndAgentKey(id, agentKey.toUpperCase())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Agente não solicitado para este dossiê"));
    if (review.getCompletedAt() != null) conflict("Parecer do agente já foi concluído");
    if (request.decision() == null) bad("decision é obrigatória");
    require(request.rationale(), "rationale");
    require(request.recommendation(), "recommendation");
    review.setDecision(request.decision());
    review.setRationale(request.rationale());
    review.setRisks(request.risks());
    review.setRecommendation(request.recommendation());
    review.setCompletedAt(Instant.now());
    reviewRepository.save(review);
    if (reviewRepository.findByDossierIdOrderByAgentKeyAsc(id).stream()
        .allMatch(item -> item.getCompletedAt() != null)) {
      dossier.setStatus(OpportunityDossierStatus.READY_FOR_TEST);
      dossierRepository.save(dossier);
    }
    return response(dossier);
  }

  /** Converte uma oportunidade aprovada uma única vez em plano comercial segregado. */
  @Transactional
  public OpportunityDossierResponse convert(Long id, ConvertOpportunityRequest request) {
    OpportunityDossier dossier = find(id);
    require(request.decidedBy(), "decidedBy");
    if (dossier.getStatus() != OpportunityDossierStatus.APPROVED)
      conflict("Somente dossiê aprovado pode ser convertido");
    if (dossier.getConvertedPlan() != null) conflict("Dossiê já convertido");
    CommercialPlan plan =
        commercialPlanService.create(
            new CreateCommercialPlanRequest(
                dossier.getTitle(),
                null,
                null,
                null,
                "Validar comercialmente a oportunidade originada no dossiê " + dossier.getId(),
                dossier.getTargetAudience(),
                dossier.getMainPain(),
                dossier.getProposedOffer(),
                null,
                null,
                "Vendas aprovadas e margem",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                1,
                1,
                1,
                0,
                dossier.getExperimentRecommendation(),
                "Definir experimento mínimo",
                dossier.getKnownRisks()));
    dossier.setConvertedPlan(plan);
    dossier.setStatus(OpportunityDossierStatus.CONVERTED_TO_PLAN);
    return response(dossierRepository.save(dossier));
  }

  /** Localiza o dossiê ou responde que ele não existe. */
  private OpportunityDossier find(Long id) {
    return dossierRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossiê não encontrado: " + id));
  }

  /** Monta o contrato completo a partir da verdade persistida. */
  private OpportunityDossierResponse response(OpportunityDossier dossier) {
    var evidence =
        evidenceRepository.findByDossierIdOrderByCreatedAtAsc(dossier.getId()).stream()
            .map(
                item ->
                    new OpportunityDossierResponse.Evidence(
                        item.getId(),
                        item.getSourceUrl(),
                        item.getSummary(),
                        item.getCreatedBy(),
                        item.getCreatedAt()))
            .toList();
    var reviews =
        reviewRepository.findByDossierIdOrderByAgentKeyAsc(dossier.getId()).stream()
            .map(
                item ->
                    new OpportunityDossierResponse.Review(
                        item.getId(),
                        item.getAgentKey(),
                        item.getDecision(),
                        item.getRationale(),
                        item.getRisks(),
                        item.getRecommendation(),
                        item.getRequestedAt(),
                        item.getCompletedAt()))
            .toList();
    return new OpportunityDossierResponse(
        dossier.getId(),
        dossier.getTitle(),
        dossier.getOwnerAgentKey(),
        dossier.getStatus(),
        dossier.getTargetAudience(),
        dossier.getMainPain(),
        dossier.getReferenceProduct(),
        dossier.getAiAdvantage(),
        dossier.getProposedOffer(),
        dossier.getPreliminaryPrice(),
        dossier.getDeliveryModel(),
        dossier.getKnownRisks(),
        dossier.getExperimentRecommendation(),
        dossier.getHumanDecisionBy(),
        dossier.getHumanDecisionAt(),
        dossier.getConvertedPlan() == null ? null : dossier.getConvertedPlan().getId(),
        dossier.getProductDiscoveryCycle() == null
            ? null
            : dossier.getProductDiscoveryCycle().getId(),
        dossier.getCreatedAt(),
        dossier.getUpdatedAt(),
        evidence,
        reviews);
  }

  /** Valida texto obrigatório do contrato. */
  private void require(String value, String field) {
    if (value == null || value.isBlank()) bad(field + " é obrigatório");
  }

  /** Interrompe uma requisição inválida. */
  private void bad(String message) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Interrompe uma ação incompatível com o estado persistido. */
  private void conflict(String message) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
