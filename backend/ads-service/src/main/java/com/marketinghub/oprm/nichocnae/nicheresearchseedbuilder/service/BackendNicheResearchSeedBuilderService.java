package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.detailStageExecution.NicheResearchSeedBuilderDetailResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution.FailNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.pending.RecordNicheResearchSeedBuilderPending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por persistir o seed operacional e as frases de pesquisa da etapa dois do OPRM nicho CNAE. */
@Service
public class BackendNicheResearchSeedBuilderService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendNicheResearchSeedBuilderService.class);
  private static final String CYCLE_STATUS_RUNNING = "RUNNING";
  private static final String CYCLE_STATUS_FAILED = "FAILED";
  private static final String QUERY_STATUS_PENDING = "PENDING";
  private static final String RETRYABLE_LEGACY_CONTRACT_ERROR = "nicheName is required";
  private static final String COMPLETE_STAGE_PATH_FRAGMENT = "niche-research-seed-builder/stage-executions";
  private static final String DEFAULT_CREATED_BY = "AI";
  private static final int MIN_QUERY_COUNT = 12;
  private static final int MAX_QUERY_COUNT = 15;
  private static final Set<String> ALLOWED_QUERY_GOALS = Set.of(
      "MEI_ROUTINE_DISCOVERY",
      "AUTONOMOUS_WORK_MODE_DISCOVERY",
      "CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY",
      "DAILY_OPERATION_PAIN_DISCOVERY",
      "EMOTIONAL_PAIN_DISCOVERY",
      "DREAM_DISCOVERY",
      "FEAR_DISCOVERY",
      "CHANNEL_BEHAVIOR_DISCOVERY",
      "LANGUAGE_DISCOVERY",
      "SOURCE_FRESHNESS_DISCOVERY");
  private static final Set<String> SOLUTION_TERMS = Set.of(
      "ia",
      "inteligencia artificial",
      "automacao",
      "software",
      "sistema",
      "app",
      "ferramenta",
      "curso",
      "template",
      "produto",
      "oferta",
      "campanha",
      "landing page");
  private static final Set<String> GENERIC_QUERIES = Set.of("como vender mais", "aumentar vendas", "vender mais");
  private static final Set<String> AUDIENCE_MARKERS = Set.of(
      "mei",
      "autonomo",
      "profissional autonomo",
      "trabalhador por conta propria",
      "dono operador",
      "dono-operador");
  private static final Set<String> BRAZIL_MARKERS = Set.of(
      "brasil", "brasileiro", "brasileira", "brasileiros", "pt-br");

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheResearchSeedRepository nicheResearchSeedRepository;
  private final OprmResearchQueryRepository researchQueryRepository;

  /** Inicializa o serviço com os repositórios canônicos da etapa dois do pipeline. */
  public BackendNicheResearchSeedBuilderService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheResearchSeedRepository nicheResearchSeedRepository,
      OprmResearchQueryRepository researchQueryRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.nicheResearchSeedRepository = nicheResearchSeedRepository;
    this.researchQueryRepository = researchQueryRepository;
  }

  /** Lista ciclos em execução ou falhas retryáveis que ainda precisam receber seed e queries. */
  @Transactional(readOnly = true)
  public List<RecordNicheResearchSeedBuilderPending> listPending() {
    return routineResearchCycleRepository
        .findSeedBuilderPendingOrRetryable(
            CYCLE_STATUS_RUNNING,
            CYCLE_STATUS_FAILED,
            RETRYABLE_LEGACY_CONTRACT_ERROR,
            COMPLETE_STAGE_PATH_FRAGMENT,
            PageRequest.of(0, 20))
        .stream()
        .map(this::toPending)
        .toList();
  }

  /** Grava o seed operacional e as queries geradas pela IA para um ciclo de pesquisa. */
  @Transactional
  public CompleteNicheResearchSeedBuilderResponse complete(
      Long researchCycleId, CompleteNicheResearchSeedBuilderRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      validateCompletionRequest(researchCycleId, cycle, request);
      if (nicheResearchSeedRepository.existsByResearchCycleId(researchCycleId)) {
        throw new IllegalStateException("Niche research seed already exists for cycle: " + researchCycleId);
      }

      Instant now = Instant.now();
      OprmNicheResearchSeed savedSeed = nicheResearchSeedRepository.save(createSeed(cycle, request, now));
      List<OprmResearchQuery> savedQueries = researchQueryRepository.saveAll(createQueries(
          researchCycleId, savedSeed.getId(), request.queries(), normalizeCreatedBy(request.createdBy()), now));
      reactivateCycleAfterSuccessfulCompletion(cycle);
      cycle.setTotalQueries(savedQueries.size());
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
      return toResponse(savedSeed, savedQueries);
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa dois do OPRM nichocnae (researchCycleId={}, queryCount={})",
          researchCycleId,
          request == null || request.queries() == null ? null : request.queries().size(),
          ex);
      throw ex;
    }
  }

  /** Registra falha da etapa dois no ciclo de pesquisa para interromper o avanço operacional. */
  @Transactional
  public void fail(Long researchCycleId, FailNicheResearchSeedBuilderRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      Instant now = Instant.now();
      cycle.setStatus(CYCLE_STATUS_FAILED);
      cycle.setErrorMessage(buildFailureMessage(request));
      cycle.setFinishedAt(now);
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa dois do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      throw ex;
    }
  }

  /** Monta a mensagem de falha preservando a causa técnica detalhada quando o coletor enviar essa informação. */
  private String buildFailureMessage(FailNicheResearchSeedBuilderRequest request) {
    String errorMessage = requiredText(request == null ? null : request.errorMessage(), "errorMessage");
    String errorDetail = trimToNull(request.errorDetail());
    if (errorDetail == null || errorDetail.equals(errorMessage)) {
      return errorMessage;
    }
    return errorMessage + " | Detalhe técnico: " + errorDetail;
  }

  /** Retorna o seed e as queries gravados para um ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public NicheResearchSeedBuilderDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    CompleteNicheResearchSeedBuilderResponse seed = nicheResearchSeedRepository
        .findByResearchCycleId(researchCycleId)
        .map(foundSeed -> toResponse(
            foundSeed, researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(researchCycleId)))
        .orElse(null);
    return new NicheResearchSeedBuilderDetailResponse(
        cycle.getId(), cycle.getStatus(), cycle.getTotalQueries(), cycle.getErrorMessage(), seed);
  }

  /** Reabre o ciclo quando uma falha retryável da etapa dois é concluída com sucesso após correção. */
  private void reactivateCycleAfterSuccessfulCompletion(OprmRoutineResearchCycle cycle) {
    if (CYCLE_STATUS_FAILED.equals(cycle.getStatus())) {
      cycle.setStatus(CYCLE_STATUS_RUNNING);
      cycle.setFinishedAt(null);
      cycle.setErrorMessage(null);
    }
  }

  /** Busca o ciclo de pesquisa ou interrompe a operação com erro explícito. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o contrato mínimo da saída de IA antes de gravar artefatos da etapa dois. */
  private void validateCompletionRequest(
      Long researchCycleId, OprmRoutineResearchCycle cycle, CompleteNicheResearchSeedBuilderRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body is required for cycle: " + researchCycleId);
    }
    requiredText(request.nicheName(), "nicheName");
    requiredText(request.businessType(), "businessType");
    requiredText(request.operationType(), "operationType");
    requiredText(request.customerType(), "customerType");
    requiredText(request.commercialObjects(), "commercialObjects");
    requiredText(request.initialAssumptions(), "initialAssumptions");
    requiredText(request.confidenceLevel(), "confidenceLevel");
    validateQueries(cycle, request.queries());
  }

  /** Valida quantidade, objetivos, duplicidade, texto mínimo e ausência de solução nas queries geradas. */
  private void validateQueries(OprmRoutineResearchCycle cycle, List<NicheResearchQueryRequest> queries) {
    if (queries == null || queries.size() < MIN_QUERY_COUNT || queries.size() > MAX_QUERY_COUNT) {
      throw new IllegalArgumentException("queries must contain between 12 and 15 items");
    }
    Set<String> uniqueQueryTexts = new HashSet<>();
    String allowedCnaeText = normalizeForTerms(cycle.getCnaeDescription());
    for (NicheResearchQueryRequest query : queries) {
      String rawQueryText = requiredText(query == null ? null : query.queryText(), "queryText");
      String queryText = normalizeForTerms(rawQueryText);
      if (!uniqueQueryTexts.add(queryText)) {
        throw new IllegalArgumentException("Duplicate queryText is not allowed: " + query.queryText());
      }
      rejectGenericQuery(rawQueryText, queryText);
      rejectSolutionTerms(rawQueryText, allowedCnaeText);
      validateAudienceAndBrazilMarkers(rawQueryText);
      String queryGoal = requiredText(query.queryGoal(), "queryGoal");
      if (!ALLOWED_QUERY_GOALS.contains(queryGoal)) {
        throw new IllegalArgumentException("Unsupported queryGoal: " + queryGoal);
      }
    }
  }

  /** Rejeita queries genéricas que não ajudam a pesquisar a rotina real do MEI/autônomo. */
  private void rejectGenericQuery(String rawQueryText, String normalizedQueryText) {
    if (GENERIC_QUERIES.contains(normalizedQueryText)) {
      throw new IllegalArgumentException("Generic query is not allowed: " + rawQueryText);
    }
  }

  /** Exige marcadores claros de público MEI/autônomo e de contexto brasileiro antes da persistência. */
  private void validateAudienceAndBrazilMarkers(String queryText) {
    String normalizedQuery = normalizeForTerms(queryText);
    Set<String> queryTokens = tokenizeNormalizedText(normalizedQuery);
    boolean hasAudienceMarker = AUDIENCE_MARKERS.stream()
        .anyMatch(marker -> containsTerm(normalizedQuery, queryTokens, marker));
    if (!hasAudienceMarker) {
      throw new IllegalArgumentException("Query must mention MEI/autonomous professional audience: " + queryText);
    }
    boolean hasBrazilMarker = BRAZIL_MARKERS.stream()
        .anyMatch(marker -> containsTerm(normalizedQuery, queryTokens, marker));
    if (!hasBrazilMarker) {
      throw new IllegalArgumentException("Query must mention Brazilian or pt-BR context: " + queryText);
    }
  }

  /** Rejeita queries que nascem procurando solução sem esse termo existir literalmente na descrição CNAE. */
  private void rejectSolutionTerms(String queryText, String allowedCnaeText) {
    String normalizedQuery = normalizeForTerms(queryText);
    Set<String> queryTokens = tokenizeNormalizedText(normalizedQuery);
    for (String term : SOLUTION_TERMS) {
      if (containsTerm(normalizedQuery, queryTokens, term) && !containsAllowedCnaeTerm(allowedCnaeText, term)) {
        throw new IllegalArgumentException("Query contains forbidden solution language: " + queryText);
      }
    }
  }

  /** Verifica se a descrição CNAE autoriza literalmente o uso de um termo sensível. */
  private boolean containsAllowedCnaeTerm(String allowedCnaeText, String term) {
    Set<String> allowedTokens = tokenizeNormalizedText(allowedCnaeText);
    return containsTerm(allowedCnaeText, allowedTokens, term);
  }

  /** Tokeniza texto normalizado sem falhar quando palavras comuns aparecem repetidas no payload da IA. */
  private Set<String> tokenizeNormalizedText(String normalizedText) {
    Set<String> tokens = new HashSet<>();
    for (String token : normalizedText.split("[^a-z0-9]+")) {
      if (StringUtils.hasText(token)) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  /** Detecta termos simples por token e expressões compostas por ocorrência textual normalizada. */
  private boolean containsTerm(String text, Set<String> tokens, String term) {
    return term.matches(".*[^a-z0-9].*") ? text.contains(term) : tokens.contains(term);
  }

  /** Cria a entidade de seed do nicho usando o ciclo canônico como fonte dos dados CNAE. */
  private OprmNicheResearchSeed createSeed(
      OprmRoutineResearchCycle cycle, CompleteNicheResearchSeedBuilderRequest request, Instant now) {
    OprmNicheResearchSeed seed = new OprmNicheResearchSeed();
    seed.setResearchCycleId(cycle.getId());
    seed.setCnaeCode(cycle.getCnaeCode());
    seed.setCnaeDescription(cycle.getCnaeDescription());
    seed.setNicheName(requiredText(request.nicheName(), "nicheName"));
    seed.setBusinessType(requiredText(request.businessType(), "businessType"));
    seed.setOperationType(requiredText(request.operationType(), "operationType"));
    seed.setCustomerType(requiredText(request.customerType(), "customerType"));
    seed.setCommercialObjects(requiredText(request.commercialObjects(), "commercialObjects"));
    seed.setInitialAssumptions(requiredText(request.initialAssumptions(), "initialAssumptions"));
    seed.setConfidenceLevel(requiredText(request.confidenceLevel(), "confidenceLevel"));
    seed.setCreatedBy(normalizeCreatedBy(request.createdBy()));
    seed.setCreatedAt(now);
    return seed;
  }

  /** Cria as entidades de query com status pendente para execução pela próxima etapa do pipeline. */
  private List<OprmResearchQuery> createQueries(
      Long researchCycleId,
      Long nicheResearchSeedId,
      List<NicheResearchQueryRequest> queryRequests,
      String createdBy,
      Instant now) {
    return queryRequests.stream()
        .map(queryRequest -> createQuery(researchCycleId, nicheResearchSeedId, queryRequest, createdBy, now))
        .sorted(Comparator.comparing(OprmResearchQuery::getPriority).thenComparing(OprmResearchQuery::getQueryText))
        .toList();
  }

  /** Cria uma query individual aplicando defaults operacionais definidos para o MVP. */
  private OprmResearchQuery createQuery(
      Long researchCycleId,
      Long nicheResearchSeedId,
      NicheResearchQueryRequest queryRequest,
      String createdBy,
      Instant now) {
    OprmResearchQuery query = new OprmResearchQuery();
    query.setResearchCycleId(researchCycleId);
    query.setNicheResearchSeedId(nicheResearchSeedId);
    query.setQueryText(requiredText(queryRequest.queryText(), "queryText"));
    query.setQueryGoal(requiredText(queryRequest.queryGoal(), "queryGoal"));
    query.setSourceGroup(trimToNull(queryRequest.sourceGroup()));
    query.setPriority(queryRequest.priority() == null ? 100 : queryRequest.priority());
    query.setStatus(QUERY_STATUS_PENDING);
    query.setResultCount(0);
    query.setCreatedBy(createdBy);
    query.setCreatedAt(now);
    query.setUpdatedAt(now);
    return query;
  }

  /** Converte um ciclo pendente no contrato interno de unidade de trabalho da etapa dois. */
  private RecordNicheResearchSeedBuilderPending toPending(OprmRoutineResearchCycle cycle) {
    return new RecordNicheResearchSeedBuilderPending(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getSourceScore(),
        cycle.getStatus(),
        cycle.getStartedAt(),
        cycle.getCreatedAt());
  }

  /** Converte as entidades persistidas no contrato público de resultado da etapa dois. */
  private CompleteNicheResearchSeedBuilderResponse toResponse(
      OprmNicheResearchSeed seed, List<OprmResearchQuery> queries) {
    return new CompleteNicheResearchSeedBuilderResponse(
        seed.getResearchCycleId(),
        seed.getId(),
        seed.getCnaeCode(),
        seed.getCnaeDescription(),
        seed.getNicheName(),
        seed.getBusinessType(),
        seed.getOperationType(),
        seed.getCustomerType(),
        seed.getCommercialObjects(),
        seed.getInitialAssumptions(),
        seed.getConfidenceLevel(),
        seed.getCreatedBy(),
        seed.getCreatedAt(),
        queries.size(),
        queries.stream().map(this::toQueryResponse).toList());
  }

  /** Converte uma query persistida no contrato de leitura da etapa dois. */
  private NicheResearchQueryResponse toQueryResponse(OprmResearchQuery query) {
    return new NicheResearchQueryResponse(
        query.getId(),
        query.getResearchCycleId(),
        query.getNicheResearchSeedId(),
        query.getQueryText(),
        query.getQueryGoal(),
        query.getSourceGroup(),
        query.getPriority(),
        query.getStatus(),
        query.getResultCount(),
        query.getCreatedBy(),
        query.getCreatedAt(),
        query.getUpdatedAt());
  }

  /** Normaliza o autor do artefato para manter o contrato simples quando a origem não for informada. */
  private String normalizeCreatedBy(String createdBy) {
    return StringUtils.hasText(createdBy) ? createdBy.trim() : DEFAULT_CREATED_BY;
  }

  /** Garante que um campo textual obrigatório exista e remove espaços laterais antes da persistência. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }


  /** Normaliza texto removendo acentos para comparar termos de solução de forma determinística. */
  private String normalizeForTerms(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
  }

  /** Converte strings vazias em nulo para campos opcionais persistidos. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
