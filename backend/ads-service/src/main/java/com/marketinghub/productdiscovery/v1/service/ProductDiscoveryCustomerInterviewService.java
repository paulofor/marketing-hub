package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCustomerInterview;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryInterviewOutcome;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCustomerInterviewRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar a política e a evidência comportamental antes da pesquisa dirigida de
 * lacunas.
 */
@Service
public class ProductDiscoveryCustomerInterviewService {
  public static final String GAP_STAGE_CODE = "candidate-gap-deepening";
  public static final String WAITING_STAGE_CODE = "customer-evidence";
  public static final int MINIMUM_INTERVIEWS = 5;
  public static final int MAXIMUM_INTERVIEWS = 8;
  public static final int MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT = 12;
  public static final int MAXIMUM_DEEPENING_ATTEMPTS = 2;
  public static final int MAXIMUM_MODEL_INVOCATIONS = 4;
  public static final BigDecimal MAXIMUM_SEARCH_COST_USD = new BigDecimal("0.12000000");
  public static final String SEARCH_PRICING_SOURCE = "https://brave.com/search/api/";
  public static final LocalDate SEARCH_PRICING_OBSERVED_ON = LocalDate.of(2026, 9, 23);
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
  private static final Pattern PHONE_PATTERN =
      Pattern.compile(
          "(?<!\\d)(?:\\+?55[ .-]?)?(?:\\(?\\d{2}\\)?[ .-]?)?9?\\d{4}[ .-]?\\d{4}(?!\\d)");

  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryOpportunityRepository opportunityRepository;
  private final ProductDiscoveryCustomerInterviewRepository interviewRepository;
  private final ProductDiscoveryBpmAuditService bpmAuditService;

  /** Inicializa o gate com as fontes persistidas da mesma execução. */
  public ProductDiscoveryCustomerInterviewService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryOpportunityRepository opportunityRepository,
      ProductDiscoveryCustomerInterviewRepository interviewRepository,
      ProductDiscoveryBpmAuditService bpmAuditService) {
    this.cycleRepository = cycleRepository;
    this.opportunityRepository = opportunityRepository;
    this.interviewRepository = interviewRepository;
    this.bpmAuditService = bpmAuditService;
  }

  /** Exibe entrevistas, cobertura das candidatas e critérios necessários para liberar Argos. */
  @Transactional(readOnly = true)
  public ProductDiscoveryGapDeepeningResponse get(Long cycleId) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    return response(cycle);
  }

  /**
   * Adota explicitamente pesquisa pública, preservando tarefas, entrevistas e candidatas do ciclo.
   */
  @Transactional
  public ProductDiscoveryGapDeepeningResponse adoptPublicEvidence(Long cycleId) {
    ProductDiscoveryCycle cycle = findCycleForUpdate(cycleId);
    if (cycle.usesPublicEvidence()) return response(cycle);
    if (!bpmAuditService.supportsCandidateGapDeepening(cycle)
        || cycle.getStatus() != ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE
        || !WAITING_STAGE_CODE.equals(cycle.getStageCode())
        || opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycleId).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A pesquisa pública só pode substituir a espera por entrevistas antes do aprofundamento");
    }
    cycle.setEvidencePolicy("PUBLIC_SOURCES_V1");
    cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    cycle.setStageCode(GAP_STAGE_CODE);
    cycle.setDecisionSummary(
        "Pesquisa pública automatizada adotada explicitamente; entrevistas históricas preservadas, candidatas e limites mantidos. Relatos públicos não comprovam vendas do nosso produto.");
    cycle.setErrorMessage(null);
    cycleRepository.save(cycle);
    return response(cycle);
  }

  /** Registra uma narrativa consentida e libera a etapa quando todos os gates forem atendidos. */
  @Transactional
  public ProductDiscoveryGapDeepeningResponse record(
      Long cycleId, ProductDiscoveryCustomerInterviewRequest request) {
    ProductDiscoveryCycle cycle = findCycleForUpdate(cycleId);
    if (request == null || !request.consentConfirmed() || !request.noPersonalDataConfirmed()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Confirme o consentimento e a remoção de dados pessoais antes de registrar a entrevista");
    }
    if (!bpmAuditService.supportsCandidateGapDeepening(cycle)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Este ciclo pertence a uma versão anterior do processo e não possui aprofundamento de lacunas");
    }
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE
        || !WAITING_STAGE_CODE.equals(cycle.getStageCode())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "As entrevistas só podem ser registradas enquanto o ciclo aguarda evidência de clientes");
    }
    List<ProductDiscoveryCustomerInterview> current =
        interviewRepository.findAllByCycleIdOrderByIdAsc(cycleId);
    if (current.size() >= MAXIMUM_INTERVIEWS) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O limite exploratório de oito entrevistas deste ciclo já foi atingido");
    }
    String participantCode = requiredText(request.anonymousParticipantCode(), "código anônimo");
    validateAnonymousCode(participantCode);
    if (interviewRepository.existsByCycleIdAndAnonymousParticipantCodeIgnoreCase(
        cycleId, participantCode)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Este código anônimo já foi registrado no ciclo");
    }
    if (request.outcome() == null || request.occurredOn() == null) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Informe o resultado passado e a data da situação relatada");
    }
    if (request.occurredOn().isAfter(LocalDate.now())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "A situação relatada não pode estar no futuro");
    }
    validateAmount(request.amountSpent(), request.currency());
    String purchaseSituation = requiredText(request.purchaseSituation(), "situação");
    String desiredResult = requiredText(request.desiredResult(), "resultado desejado");
    String difficulty = requiredText(request.difficulty(), "dificuldade");
    String alternativeTried = requiredText(request.alternativeTried(), "alternativa");
    String remainingDifficulty =
        requiredText(request.remainingDifficulty(), "dificuldade residual");
    validateNoContactData(
        purchaseSituation, desiredResult, difficulty, alternativeTried, remainingDifficulty);
    ProductDiscoveryOpportunity opportunity =
        opportunityRepository
            .findByIdAndCycleId(request.opportunityId(), cycleId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "A candidata informada não pertence a este ciclo"));

    ProductDiscoveryCustomerInterview interview = new ProductDiscoveryCustomerInterview();
    interview.setCycle(cycle);
    interview.setOpportunity(opportunity);
    interview.setAnonymousParticipantCode(participantCode);
    interview.setOutcome(request.outcome());
    interview.setOccurredOn(request.occurredOn());
    interview.setConsentCapturedAt(Instant.now());
    interview.setPurchaseSituation(purchaseSituation);
    interview.setDesiredResult(desiredResult);
    interview.setDifficulty(difficulty);
    interview.setAlternativeTried(alternativeTried);
    interview.setAmountSpent(request.amountSpent());
    interview.setCurrency(normalizeCurrency(request.currency()));
    interview.setRemainingDifficulty(remainingDifficulty);
    interviewRepository.save(interview);

    List<ProductDiscoveryCustomerInterview> updated =
        interviewRepository.findAllByCycleIdOrderByIdAsc(cycleId);
    if (isReady(cycleId, updated)) {
      cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
      cycle.setStageCode(GAP_STAGE_CODE);
      cycle.setDecisionSummary(
          "Entrevistas exploratórias consentidas cobrem compra, desistência e todas as candidatas; Argos aprofundará somente as lacunas declaradas.");
      cycle.setErrorMessage(null);
      cycleRepository.save(cycle);
    }
    return response(cycle);
  }

  /** Monta o contrato persistido usado pela tela e pelo contexto entregue ao worker. */
  private ProductDiscoveryGapDeepeningResponse response(ProductDiscoveryCycle cycle) {
    List<ProductDiscoveryCustomerInterview> interviews =
        interviewRepository.findAllByCycleIdOrderByIdAsc(cycle.getId());
    List<Long> opportunityIds =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId()).stream()
            .map(ProductDiscoveryOpportunity::getId)
            .toList();
    Set<Long> covered = new HashSet<>();
    interviews.forEach(item -> covered.add(item.getOpportunity().getId()));
    List<Long> missing = opportunityIds.stream().filter(id -> !covered.contains(id)).toList();
    int purchased = count(interviews, ProductDiscoveryInterviewOutcome.PURCHASED);
    int abandoned = count(interviews, ProductDiscoveryInterviewOutcome.ABANDONED);
    boolean applicable = bpmAuditService.supportsCandidateGapDeepening(cycle);
    boolean ready =
        applicable
            && interviews.size() >= MINIMUM_INTERVIEWS
            && interviews.size() <= MAXIMUM_INTERVIEWS
            && purchased > 0
            && abandoned > 0
            && missing.isEmpty();
    if (cycle.usesPublicEvidence()) ready = applicable && !opportunityIds.isEmpty();
    return new ProductDiscoveryGapDeepeningResponse(
        cycle.getId(),
        applicable,
        cycle.getStatus(),
        cycle.getStageCode(),
        cycle.usesPublicEvidence() ? 0 : MINIMUM_INTERVIEWS,
        MAXIMUM_INTERVIEWS,
        interviews.size(),
        purchased,
        abandoned,
        covered.stream().sorted().toList(),
        missing,
        ready,
        MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT,
        MAXIMUM_DEEPENING_ATTEMPTS,
        MAXIMUM_MODEL_INVOCATIONS,
        MAXIMUM_SEARCH_COST_USD,
        "ESTIMATED_SEARCH_ONLY",
        "AGENT_TASK_AUDIT_AFTER_CALLBACK",
        SEARCH_PRICING_SOURCE,
        SEARCH_PRICING_OBSERVED_ON,
        cycle.usesPublicEvidence()
            ? "Pesquisa automatizada com fontes públicas, relatos e contrapontos por candidata. Sem entrevistas obrigatórias; lacunas sem evidência permanecem abertas. Os limites de consumo continuam vigentes."
            : guidance(applicable, cycle, interviews.size(), purchased, abandoned, missing),
        interviews.stream().map(this::toResponse).toList(),
        cycle.getEvidencePolicy(),
        applicable
            && !cycle.usesPublicEvidence()
            && cycle.getStatus() == ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE
            && WAITING_STAGE_CODE.equals(cycle.getStageCode())
            && !opportunityIds.isEmpty());
  }

  /** Confirma o conjunto mínimo sem converter cinco relatos em estimativa estatística. */
  private boolean isReady(Long cycleId, List<ProductDiscoveryCustomerInterview> interviews) {
    if (interviews.size() < MINIMUM_INTERVIEWS || interviews.size() > MAXIMUM_INTERVIEWS) {
      return false;
    }
    if (count(interviews, ProductDiscoveryInterviewOutcome.PURCHASED) == 0
        || count(interviews, ProductDiscoveryInterviewOutcome.ABANDONED) == 0) {
      return false;
    }
    Set<Long> covered = new HashSet<>();
    interviews.forEach(item -> covered.add(item.getOpportunity().getId()));
    return opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycleId).stream()
        .allMatch(opportunity -> covered.contains(opportunity.getId()));
  }

  /** Conta relatos por decisão passada sem inferir intenção futura. */
  private int count(
      List<ProductDiscoveryCustomerInterview> interviews,
      ProductDiscoveryInterviewOutcome outcome) {
    return (int) interviews.stream().filter(item -> item.getOutcome() == outcome).count();
  }

  /** Explica a pendência de maneira acionável sem expor decisão no frontend. */
  private String guidance(
      boolean applicable,
      ProductDiscoveryCycle cycle,
      int count,
      int purchased,
      int abandoned,
      List<Long> missing) {
    if (!applicable) {
      return "O ciclo histórico permanece na versão anterior; novas execuções usarão a atividade de aprofundamento.";
    }
    if (cycle.getStatus() == ProductDiscoveryCycleStatus.READY_FOR_RESEARCH
        || cycle.getStatus() == ProductDiscoveryCycleStatus.RESEARCHING) {
      return "Evidência comportamental mínima atendida; a pesquisa dirigida está na fila ou em execução.";
    }
    if (cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED) {
      return "A pesquisa dirigida terminou; entrevistas permanecem como evidência qualitativa, não como estimativa de mercado.";
    }
    return "Registre de 5 a 8 entrevistas consentidas e anônimas, incluindo compra e desistência"
        + (missing.isEmpty() ? "." : ", com ao menos uma situação para cada candidata.")
        + " Atual: "
        + count
        + ", compras "
        + purchased
        + ", desistências "
        + abandoned
        + ".";
  }

  /** Converte a entidade sem carregar dados de identificação pessoal. */
  private ProductDiscoveryCustomerInterviewResponse toResponse(
      ProductDiscoveryCustomerInterview interview) {
    return new ProductDiscoveryCustomerInterviewResponse(
        interview.getId(),
        interview.getCycle().getId(),
        interview.getOpportunity().getId(),
        interview.getOpportunity().getName(),
        interview.getAnonymousParticipantCode(),
        interview.getOutcome(),
        interview.getOccurredOn(),
        interview.getConsentCapturedAt(),
        interview.getPurchaseSituation(),
        interview.getDesiredResult(),
        interview.getDifficulty(),
        interview.getAlternativeTried(),
        interview.getAmountSpent(),
        interview.getCurrency(),
        interview.getRemainingDifficulty(),
        interview.getCreatedAt());
  }

  /** Bloqueia identificadores que parecem nome de contato em vez de código anônimo. */
  private void validateAnonymousCode(String value) {
    if (!value.matches("[A-Za-z]{1,3}[0-9]{1,4}")) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Use um código anônimo curto, como P01; não informe nome, e-mail, telefone ou contato");
    }
  }

  /** Rejeita contatos reconhecíveis nos resumos antes de qualquer persistência. */
  private void validateNoContactData(String... values) {
    for (String value : values) {
      if (EMAIL_PATTERN.matcher(value).find() || PHONE_PATTERN.matcher(value).find()) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Remova e-mail, telefone ou contato pessoal do resumo anônimo");
      }
    }
  }

  /** Preserva gasto desconhecido como nulo e exige moeda quando existe valor. */
  private void validateAmount(BigDecimal amount, String currency) {
    if (amount == null && StringUtils.hasText(currency)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Moeda só pode ser informada quando houver gasto");
    }
    if (amount != null && !StringUtils.hasText(currency)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Informe a moeda do gasto relatado");
    }
    if (amount != null && (amount.signum() < 0 || amount.scale() > 2)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O gasto relatado deve ser positivo e usar no máximo duas casas decimais");
    }
    if (StringUtils.hasText(currency) && !currency.trim().matches("[A-Za-z]{3}")) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Informe uma moeda ISO com três letras");
    }
  }

  /** Normaliza a moeda ISO declarada sem assumir BRL para valor ausente. */
  private String normalizeCurrency(String currency) {
    return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : null;
  }

  /** Exige texto significativo depois da validação estrutural do endpoint. */
  private String requiredText(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Informe " + field + " da entrevista");
    }
    return value.trim();
  }

  /** Busca o ciclo para leitura gerencial. */
  private ProductDiscoveryCycle findCycle(Long cycleId) {
    return cycleRepository
        .findById(cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
  }

  /** Bloqueia concorrentemente a liberação para não criar duas filas de aprofundamento. */
  private ProductDiscoveryCycle findCycleForUpdate(Long cycleId) {
    return cycleRepository
        .findByIdForUpdate(cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
  }
}
