package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.detailStageExecution.NicheResearchSeedBuilderDetailResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution.FailNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.pending.RecordNicheResearchSeedBuilderPending;
import com.marketinghub.repository.jpa.oprm.market.OprmMarketSizeByCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
  private static final String RETRYABLE_QUERY_GOAL_LENGTH_ERROR = "Data too long for column 'query_goal'";
  private static final String COMPLETE_STAGE_PATH_FRAGMENT = "niche-research-seed-builder/stage-executions";
  private static final String DEFAULT_CREATED_BY = "AI";
  private static final int MIN_SPECIFIC_SUBNICHE_TOKENS = 4;
  private static final int MIN_COMMERCIAL_CRITERIA_SCORE = 4;
  private static final int MIN_COMMERCIAL_QUERY_FAMILY_SCORE = 4;
  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheResearchSeedRepository nicheResearchSeedRepository;
  private final OprmResearchQueryRepository researchQueryRepository;
  private final OprmNicheResearchSeedBuilderConfigurationGateway configurationGateway;
  private final OprmMarketSizeByCnaeRepository marketSizeByCnaeRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;

  /** Inicializa o serviço com os repositórios canônicos da etapa dois do pipeline. */
  public BackendNicheResearchSeedBuilderService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheResearchSeedRepository nicheResearchSeedRepository,
      OprmResearchQueryRepository researchQueryRepository,
      OprmNicheResearchSeedBuilderConfigurationGateway configurationGateway,
      OprmMarketSizeByCnaeRepository marketSizeByCnaeRepository,
      OprmNicheRoutineCardRepository routineCardRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.nicheResearchSeedRepository = nicheResearchSeedRepository;
    this.researchQueryRepository = researchQueryRepository;
    this.configurationGateway = configurationGateway;
    this.marketSizeByCnaeRepository = marketSizeByCnaeRepository;
    this.routineCardRepository = routineCardRepository;
  }

  /** Lista ciclos em execução ou falhas retryáveis que ainda precisam receber seed e queries. */
  @Transactional(readOnly = true)
  public List<RecordNicheResearchSeedBuilderPending> listPending() {
    return routineResearchCycleRepository
        .findSeedBuilderPendingOrRetryable(
            CYCLE_STATUS_RUNNING,
            CYCLE_STATUS_FAILED,
            RETRYABLE_LEGACY_CONTRACT_ERROR,
            RETRYABLE_QUERY_GOAL_LENGTH_ERROR,
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
      validateCommercialPreGate(cycle, request);
      OprmNicheResearchSeed savedSeed = nicheResearchSeedRepository.save(createSeed(cycle, request, now));
      List<OprmResearchQuery> savedQueries = researchQueryRepository.saveAll(createQueries(
          cycle,
          savedSeed.getId(),
          request == null ? null : request.queries(),
          normalizeCreatedBy(request == null ? null : request.createdBy()),
          now));
      reactivateCycleAfterSuccessfulCompletion(cycle);
      applyWinningSubnicheToCycle(cycle, savedSeed);
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
    String winningSubnicheName = resolveWinningSubnicheName(cycle, request);
    OprmNicheResearchSeed seed = new OprmNicheResearchSeed();
    seed.setResearchCycleId(cycle.getId());
    seed.setCnaeCode(cycle.getCnaeCode());
    seed.setCnaeDescription(cycle.getCnaeDescription());
    seed.setNicheName(winningSubnicheName);
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
    seed.setModel(trimToNull(request == null ? null : request.model()));
    seed.setRawModelResponse(trimToNull(request == null ? null : request.rawModelResponse()));
    seed.setRawOpenAiRequest(trimToNull(request == null ? null : request.rawOpenAiRequest()));
    seed.setRawOpenAiResponse(trimToNull(request == null ? null : request.rawOpenAiResponse()));
    seed.setInputTokens(request == null ? null : request.inputTokens());
    seed.setOutputTokens(request == null ? null : request.outputTokens());
    seed.setCostUsd(estimateCostUsd(request));
    seed.setOpenAiResponseId(trimToNull(request == null ? null : request.openAiResponseId()));
    return seed;
  }

  /** Resolve o subnicho vencedor e bloqueia a persistência quando a IA retornar apenas o CNAE amplo. */
  private String resolveWinningSubnicheName(
      OprmRoutineResearchCycle cycle, CompleteNicheResearchSeedBuilderRequest request) {
    String selectedName = requiredText(request == null ? null : request.nicheName(), "nicheName");
    if (isBroadCnaeName(cycle, selectedName)) {
      throw new IllegalArgumentException(
          "nicheName must be a specific winning subniche, not the broad CNAE name: " + selectedName);
    }
    return selectedName;
  }

  /** Executa o pré-gate comercial antes de criar queries profundas e gastar as próximas etapas do pipeline. */
  private void validateCommercialPreGate(
      OprmRoutineResearchCycle cycle, CompleteNicheResearchSeedBuilderRequest request) {
    String selectedName = resolveWinningSubnicheName(cycle, request);
    String assumptions = requiredText(request == null ? null : request.initialAssumptions(), "initialAssumptions");
    String commercialText = commercialPreGateText(selectedName, request);
    int criteriaScore = commercialCriteriaScore(assumptions);
    int queryFamilyScore = commercialQueryFamilyScore(commercialText);
    if (criteriaScore < MIN_COMMERCIAL_CRITERIA_SCORE || queryFamilyScore < MIN_COMMERCIAL_QUERY_FAMILY_SCORE) {
      throw new IllegalArgumentException(
          "Commercial pre-gate rejected seed: selected subniche must show recurrence, pain urgency, ability to pay, "
              + "clear result, digital product compatibility and query coverage before deep research. "
              + "criteriaScore=" + criteriaScore + ", queryFamilyScore=" + queryFamilyScore);
    }
  }

  /** Junta campos do seed e queries para avaliar rapidamente se há foco comercial suficiente. */
  private String commercialPreGateText(String selectedName, CompleteNicheResearchSeedBuilderRequest request) {
    StringBuilder text = new StringBuilder();
    appendPreGateText(text, selectedName);
    if (request != null) {
      appendPreGateText(text, request.businessType());
      appendPreGateText(text, request.operationType());
      appendPreGateText(text, request.customerType());
      appendPreGateText(text, request.commercialObjects());
      appendPreGateText(text, request.initialAssumptions());
      if (request.queries() != null) {
        request.queries().forEach(query -> {
          appendPreGateText(text, query.queryText());
          appendPreGateText(text, query.queryGoal());
        });
      }
    }
    return normalizeComparableText(text.toString());
  }

  /** Adiciona um campo textual opcional ao texto avaliado pelo pré-gate comercial. */
  private void appendPreGateText(StringBuilder text, String value) {
    if (StringUtils.hasText(value)) {
      text.append(' ').append(value);
    }
  }

  /** Pontua se a justificativa comparativa do seed cobre os cinco critérios comerciais obrigatórios. */
  private int commercialCriteriaScore(String assumptions) {
    String text = normalizeComparableText(assumptions);
    int score = 0;
    score += containsAny(text, "recorrencia", "recorrente", "fidelizacao", "retencao", "pacote", "mensal") ? 1 : 0;
    score += containsAny(text, "urgencia", "urgente", "dor", "perda", "cancelamento", "agenda vazia", "retrabalho") ? 1 : 0;
    score += containsAny(text, "capacidade de pagar", "pagar", "pagamento", "cobranca", "preco", "sinal") ? 1 : 0;
    score += containsAny(text, "clareza do resultado", "resultado", "agenda cheia", "menos faltas", "renda", "faturamento") ? 1 : 0;
    score += containsAny(text, "produto digital", "digital", "guia", "aula", "planilha", "checklist", "conteudo") ? 1 : 0;
    return score;
  }

  /** Pontua se as queries cobrem famílias mínimas para validar demanda antes da pesquisa profunda. */
  private int commercialQueryFamilyScore(String text) {
    int score = 0;
    score += containsAny(text, "recorrencia", "recorrente", "fidelizacao", "reativacao", "pacote", "mensal") ? 1 : 0;
    score += containsAny(text, "dor", "falta", "remarcacao", "cliente some", "agenda vazia", "retrabalho") ? 1 : 0;
    score += containsAny(text, "pagar", "pagamento", "cobranca", "preco", "sinal", "orcamento") ? 1 : 0;
    score += containsAny(text, "resultado", "agenda cheia", "ganhar mais", "renda", "faturamento", "tempo") ? 1 : 0;
    score += containsAny(text, "whatsapp", "instagram", "indicacao", "cliente", "aquisicao", "captacao") ? 1 : 0;
    return score;
  }

  /** Verifica a presença de qualquer termo comercial já normalizado no texto avaliado. */
  private boolean containsAny(String text, String... terms) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    for (String term : terms) {
      if (text.contains(normalizeComparableText(term))) {
        return true;
      }
    }
    return false;
  }

  /** Atualiza o ciclo para que todas as etapas posteriores pesquisem e materializem o subnicho vencedor. */
  private void applyWinningSubnicheToCycle(OprmRoutineResearchCycle cycle, OprmNicheResearchSeed seed) {
    String winningSubnicheName = requiredText(seed == null ? null : seed.getNicheName(), "winningSubnicheName");
    if (!StringUtils.hasText(cycle.getOriginalNicheName())) {
      cycle.setOriginalNicheName(textOrDefault(cycle.getNicheName(), cycle.getCnaeDescription()));
    }
    cycle.setNicheName(winningSubnicheName);
    cycle.setNeutralNicheName(winningSubnicheName);
  }

  /** Verifica se o nome retornado ainda representa o CNAE amplo em vez de um subnicho comercial específico. */
  private boolean isBroadCnaeName(OprmRoutineResearchCycle cycle, String selectedName) {
    String normalizedSelectedName = normalizeComparableText(selectedName);
    if (!StringUtils.hasText(normalizedSelectedName)) {
      return true;
    }
    if (tokenCount(normalizedSelectedName) < MIN_SPECIFIC_SUBNICHE_TOKENS) {
      return true;
    }
    return normalizedSelectedName.equals(normalizeComparableText(cycle.getNicheName()))
        || normalizedSelectedName.equals(normalizeComparableText(cycle.getNeutralNicheName()))
        || normalizedSelectedName.equals(normalizeComparableText(cycle.getOriginalNicheName()))
        || normalizedSelectedName.equals(normalizeComparableText(cycle.getCnaeDescription()));
  }

  /** Normaliza texto para comparar nomes ignorando acentos, caixa e pontuação. */
  private String normalizeComparableText(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return withoutAccents.toLowerCase(Locale.ROOT)
        .replace("&", " e ")
        .replaceAll("[^a-z0-9]+", " ")
        .trim();
  }

  /** Conta tokens úteis depois da normalização usada na comparação com o CNAE amplo. */
  private int tokenCount(String normalizedText) {
    return normalizedText.isBlank() ? 0 : normalizedText.split("\\s+").length;
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

  /** Cria uma query mínima comercial-operacional quando o modelo não envia queries. */
  private NicheResearchQueryRequest defaultQueryRequest(OprmRoutineResearchCycle cycle) {
    return new NicheResearchQueryRequest(defaultQueryText(cycle), "COMMERCIAL_OPERATION_DISCOVERY", "WEB", 100);
  }

  /** Monta texto de pesquisa padrão a partir do CNAE com foco em clientes, agenda, cobrança e retrabalho. */
  private String defaultQueryText(OprmRoutineResearchCycle cycle) {
    return "WhatsApp Instagram indicação agenda faltas preço cobrança materiais retrabalho Brasil "
        + cycle.getCnaeDescription();
  }

  /** Converte um ciclo pendente no contrato interno de unidade de trabalho da etapa dois com aprendizado do gate anterior. */
  private RecordNicheResearchSeedBuilderPending toPending(OprmRoutineResearchCycle cycle) {
    OprmNicheResearchSeedBuilderModel configuredModel = resolveConfiguredOpenAiModel();
    OprmNicheRoutineCard previousCard = findPreviousCheckedCard(cycle);
    String previousNotes = previousCard == null ? null : previousCard.getQualityNotes();
    return new RecordNicheResearchSeedBuilderPending(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getSourceScore(),
        resolveMeiVolume(cycle),
        configuredModel == null ? null : configuredModel.code(),
        configuredModel == null ? null : configuredModel.name(),
        cycle.getTriggerSource(),
        previousCard == null ? null : previousCard.getQualityStatus(),
        extractQualityNote(previousNotes, "proximoMovimentoCodigo"),
        extractQualityNote(previousNotes, "proximoMovimento"),
        compactLearningNotes(previousNotes),
        cycle.getStatus(),
        cycle.getStartedAt(),
        cycle.getCreatedAt());
  }

  /** Localiza o último bloqueio de qualidade do mesmo candidato para evitar repetir a causa dominante. */
  private OprmNicheRoutineCard findPreviousCheckedCard(OprmRoutineResearchCycle cycle) {
    return routineCardRepository
        .findLatestCheckedCardForLearning(cycle.getSourceNicheId(), cycle.getId(), PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .orElse(null);
  }

  /** Extrai um par chave-valor das notas determinísticas do gate para orientar o próximo seed. */
  private String extractQualityNote(String notes, String key) {
    String cleanKey = key + "=";
    if (!StringUtils.hasText(notes) || !notes.contains(cleanKey)) {
      return null;
    }
    for (String part : notes.split(";")) {
      String trimmed = part.trim();
      if (trimmed.startsWith(cleanKey)) {
        return trimToNull(trimmed.substring(cleanKey.length()));
      }
    }
    return null;
  }

  /** Compacta as notas do gate anterior para expor aprendizado operacional sem payload longo no prompt. */
  private String compactLearningNotes(String notes) {
    String trimmed = trimToNull(notes);
    if (trimmed == null) {
      return null;
    }
    return trimmed.length() <= 900 ? trimmed : trimmed.substring(0, 900);
  }

  /** Busca o volume MEI mais recente do CNAE para orientar a quebra em subnichos vendáveis. */
  private Long resolveMeiVolume(OprmRoutineResearchCycle cycle) {
    return marketSizeByCnaeRepository
        .findLatestSnapshotByCnaeCode(cycle.getCnaeCode())
        .map(volume -> volume.totalEmpresasMei())
        .orElse(null);
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
        seed.getModel(),
        seed.getRawModelResponse(),
        seed.getRawOpenAiRequest(),
        seed.getRawOpenAiResponse(),
        seed.getInputTokens(),
        seed.getOutputTokens(),
        seed.getCostUsd(),
        seed.getOpenAiResponseId(),
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

  /** Calcula o custo padrão da chamada OpenAI sem bloquear a persistência quando o catálogo estiver incompleto. */
  private BigDecimal estimateCostUsd(CompleteNicheResearchSeedBuilderRequest request) {
    if (request == null || !StringUtils.hasText(request.model())) {
      return null;
    }
    try {
      return configurationGateway.estimateCostUsd(request.model(), request.inputTokens(), request.outputTokens());
    } catch (RuntimeException ex) {
      LOGGER.warn(
          "Não foi possível calcular custo da etapa dois OPRM nichocnae (model={}, inputTokens={}, outputTokens={})",
          request.model(),
          request.inputTokens(),
          request.outputTokens(),
          ex);
      return null;
    }
  }

  /** Recupera o modelo de IA configurado pela abstração canônica permitida para o OPRM. */
  private OprmNicheResearchSeedBuilderModel resolveConfiguredOpenAiModel() {
    return configurationGateway.findConfiguredModel().orElse(null);
  }

  /** Converte strings vazias em nulo para campos opcionais persistidos. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
