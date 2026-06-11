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
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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
      if (nicheResearchSeedRepository.existsByResearchCycleId(researchCycleId)) {
        throw new IllegalStateException("Niche research seed already exists for cycle: " + researchCycleId);
      }

      Instant now = Instant.now();
      OprmNicheResearchSeed savedSeed = nicheResearchSeedRepository.save(createSeed(cycle, request, now));
      List<OprmResearchQuery> savedQueries = researchQueryRepository.saveAll(createQueries(
          cycle,
          savedSeed.getId(),
          request == null ? null : request.queries(),
          normalizeCreatedBy(request == null ? null : request.createdBy()),
          now));
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
      LOGGER.error(
          "Erro ao registrar falha da etapa dois do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
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

  /**
   * Mantém a conclusão da etapa dois tolerante a campos ausentes para não bloquear o pipeline por validação do modelo.
   */
  private String textOrDefault(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }

  /** Cria a entidade de seed do nicho usando o ciclo canônico como fonte dos dados CNAE. */
  private OprmNicheResearchSeed createSeed(
      OprmRoutineResearchCycle cycle, CompleteNicheResearchSeedBuilderRequest request, Instant now) {
    OprmNicheResearchSeed seed = new OprmNicheResearchSeed();
    seed.setResearchCycleId(cycle.getId());
    seed.setCnaeCode(cycle.getCnaeCode());
    seed.setCnaeDescription(cycle.getCnaeDescription());
    seed.setNicheName(textOrDefault(request == null ? null : request.nicheName(), cycle.getNicheName()));
    seed.setBusinessType(textOrDefault(request == null ? null : request.businessType(), cycle.getCnaeDescription()));
    seed.setOperationType(textOrDefault(
        request == null ? null : request.operationType(), "Rotina operacional do CNAE " + cycle.getCnaeCode()));
    seed.setCustomerType(
        textOrDefault(request == null ? null : request.customerType(), "Clientes do profissional deste CNAE"));
    seed.setCommercialObjects(
        textOrDefault(request == null ? null : request.commercialObjects(), cycle.getCnaeDescription()));
    seed.setInitialAssumptions(textOrDefault(
        request == null ? null : request.initialAssumptions(),
        "Seed gravado sem validação bloqueante; revisar evidências nas próximas etapas."));
    seed.setConfidenceLevel(textOrDefault(request == null ? null : request.confidenceLevel(), "UNVALIDATED"));
    seed.setCreatedBy(normalizeCreatedBy(request == null ? null : request.createdBy()));
    seed.setCreatedAt(now);
    return seed;
  }

  /** Cria as entidades de query com status pendente para execução pela próxima etapa do pipeline. */
  private List<OprmResearchQuery> createQueries(
      OprmRoutineResearchCycle cycle,
      Long nicheResearchSeedId,
      List<NicheResearchQueryRequest> queryRequests,
      String createdBy,
      Instant now) {
    List<NicheResearchQueryRequest> safeQueryRequests =
        queryRequests == null || queryRequests.isEmpty() ? List.of(defaultQueryRequest(cycle)) : queryRequests;
    return safeQueryRequests.stream()
        .map(queryRequest -> createQuery(cycle, nicheResearchSeedId, queryRequest, createdBy, now))
        .sorted(Comparator.comparing(OprmResearchQuery::getPriority).thenComparing(OprmResearchQuery::getQueryText))
        .toList();
  }

  /** Cria uma query individual aplicando defaults operacionais definidos para o MVP. */
  private OprmResearchQuery createQuery(
      OprmRoutineResearchCycle cycle,
      Long nicheResearchSeedId,
      NicheResearchQueryRequest queryRequest,
      String createdBy,
      Instant now) {
    OprmResearchQuery query = new OprmResearchQuery();
    query.setResearchCycleId(cycle.getId());
    query.setNicheResearchSeedId(nicheResearchSeedId);
    query.setQueryText(
        textOrDefault(queryRequest == null ? null : queryRequest.queryText(), defaultQueryText(cycle)));
    query.setQueryGoal(textOrDefault(queryRequest == null ? null : queryRequest.queryGoal(), "ROUTINE_DISCOVERY"));
    query.setSourceGroup(trimToNull(queryRequest == null ? null : queryRequest.sourceGroup()));
    query.setPriority(queryRequest == null || queryRequest.priority() == null ? 100 : queryRequest.priority());
    query.setStatus(QUERY_STATUS_PENDING);
    query.setResultCount(0);
    query.setCreatedBy(createdBy);
    query.setCreatedAt(now);
    query.setUpdatedAt(now);
    return query;
  }

  /** Cria uma query mínima quando o modelo não envia queries, evitando bloqueio por validação estrutural. */
  private NicheResearchQueryRequest defaultQueryRequest(OprmRoutineResearchCycle cycle) {
    return new NicheResearchQueryRequest(defaultQueryText(cycle), "ROUTINE_DISCOVERY", "WEB", 100);
  }

  /** Monta texto de pesquisa padrão a partir do CNAE para manter o ciclo avançando. */
  private String defaultQueryText(OprmRoutineResearchCycle cycle) {
    return "rotina dificuldades atendimento clientes Brasil " + cycle.getCnaeDescription();
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


  /** Converte strings vazias em nulo para campos opcionais persistidos. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
