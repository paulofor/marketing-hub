package com.marketinghub.oprm.nichocnae.sourcesearcher.service;

import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.SourceCandidateRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.SourceCandidateResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.detailStageExecution.SourceSearcherDetailResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.failStageExecution.FailSourceSearcherRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.pending.RecordSourceSearcherPending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import jakarta.persistence.EntityNotFoundException;
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

/** Responsável por persistir os resultados da busca pública da etapa três do OPRM nicho CNAE. */
@Service
public class BackendSourceSearcherService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendSourceSearcherService.class);
  private static final String QUERY_STATUS_PENDING = "PENDING";
  private static final String QUERY_STATUS_COMPLETED = "COMPLETED";
  private static final String QUERY_STATUS_FAILED = "FAILED";
  private static final String CANDIDATE_STATUS_FOUND = "FOUND";
  private static final String DEFAULT_SOURCE_GROUP = "PUBLIC_CONTENT";
  private static final int MAX_RESULTS_PER_QUERY = 20;

  private final OprmResearchQueryRepository researchQueryRepository;
  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmSourceCandidateRepository sourceCandidateRepository;

  /** Inicializa o serviço com os repositórios canônicos da etapa três do pipeline. */
  public BackendSourceSearcherService(
      OprmResearchQueryRepository researchQueryRepository,
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmSourceCandidateRepository sourceCandidateRepository) {
    this.researchQueryRepository = researchQueryRepository;
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.sourceCandidateRepository = sourceCandidateRepository;
  }

  /** Lista frases pendentes para execução em provedor de busca configurável. */
  @Transactional(readOnly = true)
  public List<RecordSourceSearcherPending> listPending() {
    return researchQueryRepository.findByStatusOrderByPriorityAscIdAsc(QUERY_STATUS_PENDING, PageRequest.of(0, 20))
        .stream()
        .map(this::toPending)
        .toList();
  }

  /** Grava os resultados encontrados pelo provedor de busca para uma frase de pesquisa. */
  @Transactional
  public CompleteSourceSearcherResponse complete(Long researchQueryId, CompleteSourceSearcherRequest request) {
    try {
      OprmResearchQuery query = findQuery(researchQueryId);
      OprmRoutineResearchCycle cycle = findCycle(query.getResearchCycleId());
      validateCompletionRequest(researchQueryId, request);
      Instant now = Instant.now();
      List<OprmSourceCandidate> savedCandidates = sourceCandidateRepository.saveAll(
          createCandidates(query, request, now));
      query.setStatus(QUERY_STATUS_COMPLETED);
      query.setResultCount(savedCandidates.size());
      query.setErrorMessage(null);
      query.setUpdatedAt(now);
      researchQueryRepository.save(query);
      cycle.setTotalSourceCandidates(countCycleCandidates(cycle.getId(), savedCandidates.size()));
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
      return toCompleteResponse(query, cycle, savedCandidates);
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa três do OPRM nichocnae (researchQueryId={}, resultCount={})",
          researchQueryId,
          request == null || request.results() == null ? null : request.results().size(),
          ex);
      throw ex;
    }
  }

  /** Registra falha de busca para uma frase sem interromper automaticamente o ciclo inteiro. */
  @Transactional
  public void fail(Long researchQueryId, FailSourceSearcherRequest request) {
    try {
      OprmResearchQuery query = findQuery(researchQueryId);
      Instant now = Instant.now();
      query.setStatus(QUERY_STATUS_FAILED);
      query.setErrorMessage(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      query.setUpdatedAt(now);
      researchQueryRepository.save(query);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa três do OPRM nichocnae (researchQueryId={})", researchQueryId, ex);
      throw ex;
    }
  }

  /** Detalha as fontes candidatas encontradas para um ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public SourceSearcherDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    List<SourceCandidateResponse> candidates = sourceCandidateRepository
        .findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(researchCycleId)
        .stream()
        .map(this::toCandidateResponse)
        .toList();
    return new SourceSearcherDetailResponse(
        cycle.getId(), cycle.getStatus(), cycle.getTotalQueries(), cycle.getTotalSourceCandidates(), candidates);
  }

  /** Localiza a frase de pesquisa ou falha com erro de contrato quando ela não existe. */
  private OprmResearchQuery findQuery(Long researchQueryId) {
    return researchQueryRepository
        .findById(researchQueryId)
        .orElseThrow(() -> new EntityNotFoundException("Research query not found: " + researchQueryId));
  }

  /** Localiza o ciclo de pesquisa ou falha com erro de contrato quando ele não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload de conclusão da etapa três antes de gravar fontes candidatas. */
  private void validateCompletionRequest(Long researchQueryId, CompleteSourceSearcherRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    requiredText(request.searchProvider(), "searchProvider");
    if (request.results() == null) {
      throw new IllegalArgumentException("results is required");
    }
    if (request.results().size() > MAX_RESULTS_PER_QUERY) {
      throw new IllegalArgumentException("results must contain at most " + MAX_RESULTS_PER_QUERY + " items");
    }
    Set<String> urls = new HashSet<>();
    for (SourceCandidateRequest result : request.results()) {
      String url = requiredText(result == null ? null : result.sourceUrl(), "sourceUrl");
      if (!urls.add(url.toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("duplicated sourceUrl in payload: " + url);
      }
      if (sourceCandidateRepository.existsByResearchQueryIdAndSourceUrl(researchQueryId, url)) {
        throw new IllegalStateException("sourceUrl already exists for researchQueryId " + researchQueryId + ": " + url);
      }
      requiredText(result.sourceTitle(), "sourceTitle");
      requiredText(result.sourceDomain(), "sourceDomain");
      if (result.searchPosition() == null || result.searchPosition() < 1) {
        throw new IllegalArgumentException("searchPosition must be greater than zero");
      }
    }
  }

  /** Cria entidades de fontes candidatas normalizadas a partir do retorno da busca. */
  private List<OprmSourceCandidate> createCandidates(
      OprmResearchQuery query, CompleteSourceSearcherRequest request, Instant now) {
    return request.results().stream()
        .map(result -> createCandidate(query, request.searchProvider(), result, now))
        .sorted(Comparator.comparing(OprmSourceCandidate::getSearchPosition).thenComparing(OprmSourceCandidate::getSourceUrl))
        .toList();
  }

  /** Cria uma fonte candidata individual preservando apenas campos contratuais da etapa três. */
  private OprmSourceCandidate createCandidate(
      OprmResearchQuery query, String searchProvider, SourceCandidateRequest result, Instant now) {
    OprmSourceCandidate candidate = new OprmSourceCandidate();
    candidate.setResearchCycleId(query.getResearchCycleId());
    candidate.setResearchQueryId(query.getId());
    candidate.setSourceUrl(requiredText(result.sourceUrl(), "sourceUrl"));
    candidate.setSourceTitle(requiredText(result.sourceTitle(), "sourceTitle"));
    candidate.setSourceSnippet(trimToNull(result.sourceSnippet()));
    candidate.setSourceDomain(requiredText(result.sourceDomain(), "sourceDomain"));
    candidate.setSourceGroup(defaultText(result.sourceGroup(), DEFAULT_SOURCE_GROUP));
    candidate.setSearchProvider(requiredText(searchProvider, "searchProvider"));
    candidate.setSearchPosition(result.searchPosition());
    candidate.setStatus(defaultText(result.status(), CANDIDATE_STATUS_FOUND));
    candidate.setCreatedAt(now);
    candidate.setUpdatedAt(now);
    return candidate;
  }

  /** Calcula o total de fontes do ciclo depois da persistência da query atual. */
  private Integer countCycleCandidates(Long researchCycleId, int fallbackCurrentQueryCount) {
    List<OprmSourceCandidate> cycleCandidates =
        sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(researchCycleId);
    return cycleCandidates.isEmpty() ? fallbackCurrentQueryCount : cycleCandidates.size();
  }

  /** Converte uma frase pendente no contrato interno de unidade de trabalho da etapa três. */
  private RecordSourceSearcherPending toPending(OprmResearchQuery query) {
    return new RecordSourceSearcherPending(
        query.getId(),
        query.getResearchCycleId(),
        query.getNicheResearchSeedId(),
        query.getQueryText(),
        query.getQueryGoal(),
        query.getSourceGroup(),
        query.getPriority(),
        query.getStatus(),
        query.getResultCount(),
        query.getCreatedAt(),
        query.getUpdatedAt());
  }

  /** Converte uma fonte candidata persistida para o contrato de resposta da etapa três. */
  private SourceCandidateResponse toCandidateResponse(OprmSourceCandidate candidate) {
    return new SourceCandidateResponse(
        candidate.getId(),
        candidate.getResearchCycleId(),
        candidate.getResearchQueryId(),
        candidate.getSourceUrl(),
        candidate.getSourceTitle(),
        candidate.getSourceSnippet(),
        candidate.getSourceDomain(),
        candidate.getSourceGroup(),
        candidate.getSearchProvider(),
        candidate.getSearchPosition(),
        candidate.getStatus(),
        candidate.getCreatedAt(),
        candidate.getUpdatedAt());
  }

  /** Monta a resposta de conclusão da etapa três para a query executada. */
  private CompleteSourceSearcherResponse toCompleteResponse(
      OprmResearchQuery query, OprmRoutineResearchCycle cycle, List<OprmSourceCandidate> candidates) {
    return new CompleteSourceSearcherResponse(
        query.getId(),
        query.getResearchCycleId(),
        query.getQueryText(),
        query.getStatus(),
        query.getResultCount(),
        cycle.getTotalSourceCandidates(),
        candidates.stream().map(this::toCandidateResponse).toList());
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa três. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Retorna texto normalizado ou valor padrão quando o campo opcional veio vazio. */
  private String defaultText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  /** Normaliza campos opcionais de texto preservando nulo quando não há conteúdo útil. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
