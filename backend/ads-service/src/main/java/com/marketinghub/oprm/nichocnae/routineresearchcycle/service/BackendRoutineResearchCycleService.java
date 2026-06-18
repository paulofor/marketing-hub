package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.detailStageExecution.RecordBackendRoutineResearchCycleDetalheDto;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listStageExecutions.RoutineResearchCycleExecutionSummaryResponse;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.pending.RecordRoutineResearchCyclePending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsável por expor a borda backend da etapa de ciclo da pesquisa de rotina de nicho CNAE. */
@Service
public class BackendRoutineResearchCycleService {
  private static final String CYCLE_STATUS_RUNNING = "RUNNING";
  private static final String CURRENT_STAGE_RESEARCH_CYCLE = "routine-research-cycle";

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheResearchSeedRepository nicheResearchSeedRepository;
  private final OprmResearchQueryRepository researchQueryRepository;
  private final OprmSourceCandidateRepository sourceCandidateRepository;
  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository;

  /** Inicializa o serviço com o repositório canônico de ciclos de pesquisa de rotina. */
  public BackendRoutineResearchCycleService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheResearchSeedRepository nicheResearchSeedRepository,
      OprmResearchQueryRepository researchQueryRepository,
      OprmSourceCandidateRepository sourceCandidateRepository,
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmNicheRoutineCardRepository routineCardRepository,
      OprmMeiAudienceProfileRepository meiAudienceProfileRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.nicheResearchSeedRepository = nicheResearchSeedRepository;
    this.researchQueryRepository = researchQueryRepository;
    this.sourceCandidateRepository = sourceCandidateRepository;
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.routineCardRepository = routineCardRepository;
    this.meiAudienceProfileRepository = meiAudienceProfileRepository;
  }

  /** Lista ciclos de pesquisa de rotina pendentes para processamento assíncrono da etapa. */
  @Transactional(readOnly = true)
  public List<RecordRoutineResearchCyclePending> listPending() {
    return routineResearchCycleRepository
        .findByCurrentStageCodeOrderByStartedAtAsc(CURRENT_STAGE_RESEARCH_CYCLE, PageRequest.of(0, 20))
        .stream()
        .map(this::toPending)
        .toList();
  }

  /** Lista execuções do ciclo de pesquisa de rotina associadas ao CNAE informado. */
  @Transactional(readOnly = true)
  public List<RoutineResearchCycleExecutionSummaryResponse> listStageExecutionsByCnae(String cnaeCode) {
    List<OprmRoutineResearchCycle> cycles =
        routineResearchCycleRepository.findByCnaeCodeOrderByStartedAtDesc(cnaeCode);
    BigDecimal cnaeTotalCostUsd =
        cycles.stream()
            .map(cycle -> executionCostUsd(cycle.getId()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return cycles.stream().map(cycle -> toSummary(cycle, cnaeTotalCostUsd)).toList();
  }

  /** Lista execuções do ciclo de pesquisa de rotina associadas a um nicho CNAE de origem. */
  @Transactional(readOnly = true)
  public List<RoutineResearchCycleExecutionSummaryResponse> listStageExecutions(Long sourceNicheId) {
    List<OprmRoutineResearchCycle> cycles =
        routineResearchCycleRepository.findBySourceNicheIdOrderByStartedAtDesc(sourceNicheId);
    BigDecimal totalCostUsd =
        cycles.stream()
            .map(cycle -> executionCostUsd(cycle.getId()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return cycles.stream().map(cycle -> toSummary(cycle, totalCostUsd)).toList();
  }

  /** Retorna os detalhes operacionais de uma execução específica do ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public RecordBackendRoutineResearchCycleDetalheDto detailStageExecution(Long researchCycleId) {
    OprmRoutineResearchCycle cycle =
        routineResearchCycleRepository
            .findById(researchCycleId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Routine research cycle not found: " + researchCycleId));
    return toDetail(cycle);
  }

  /** Monta um relatório Markdown baixável com os dados auditáveis da execução. */
  @Transactional(readOnly = true)
  public String buildMarkdownReport(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    Optional<OprmNicheResearchSeed> seed =
        nicheResearchSeedRepository.findByResearchCycleId(researchCycleId);
    List<OprmResearchQuery> queries =
        researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(researchCycleId);
    List<OprmSourceCandidate> candidates =
        sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(
            researchCycleId);
    List<OprmSourceSnapshot> snapshots =
        sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId);
    List<OprmExtractedSignal> signals =
        extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId);
    Optional<OprmNicheRoutineCard> card =
        routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId);
    Optional<OprmMeiAudienceProfile> meiProfile =
        meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId);

    StringBuilder report = new StringBuilder();
    appendTitle(report, cycle);
    appendCycleSection(report, cycle);
    appendSeedSection(report, seed);
    appendSearchSection(report, queries, candidates);
    appendFetchSection(report, snapshots);
    appendSignalsSection(report, signals);
    appendSynthesisSection(report, card);
    appendMeiSection(report, meiProfile);
    appendQualitySection(report, card);
    appendFooter(report);
    return report.toString();
  }

  /** Busca uma execução de ciclo ou falha com erro de entidade inexistente. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Routine research cycle not found: " + researchCycleId));
  }

  /** Converte um ciclo em unidade de trabalho fechada para processamento interno da etapa. */
  private RecordRoutineResearchCyclePending toPending(OprmRoutineResearchCycle cycle) {
    return new RecordRoutineResearchCyclePending(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getTriggerSource(),
        cycle.getStatus(),
        cycle.getStartedAt(),
        cycle.getCreatedAt());
  }

  /** Converte um ciclo em resumo para listagem operacional. */
  private RoutineResearchCycleExecutionSummaryResponse toSummary(
      OprmRoutineResearchCycle cycle, BigDecimal cnaeTotalCostUsd) {
    return new RoutineResearchCycleExecutionSummaryResponse(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getTriggerSource(),
        cycle.getStatus(),
        cycle.getCurrentStageCode(),
        cycle.getTotalQueries(),
        cycle.getTotalSourceCandidates(),
        cycle.getTotalSourceSnapshots(),
        cycle.getTotalExtractedSignals(),
        executionCostUsd(cycle.getId()),
        cnaeTotalCostUsd,
        cycle.getStartedAt(),
        cycle.getFinishedAt(),
        cycle.getErrorMessage());
  }

  /** Adiciona o título principal do relatório Markdown. */
  private void appendTitle(StringBuilder report, OprmRoutineResearchCycle cycle) {
    report.append("# Relatório da execução NichoCNAE #").append(cycle.getId()).append("\n\n");
    report.append("**Arquivo sugerido:** nicho-cnae").append(cycle.getId()).append(".md\n\n");
  }

  /** Adiciona os metadados gerais do ciclo de pesquisa. */
  private void appendCycleSection(StringBuilder report, OprmRoutineResearchCycle cycle) {
    report.append("## 1. Ciclo\n\n");
    appendField(report, "CNAE", cycle.getCnaeCode());
    appendField(report, "Descrição CNAE", cycle.getCnaeDescription());
    appendField(report, "Nicho", cycle.getNicheName());
    appendField(report, "Subnicho operacional", cycle.getNeutralNicheName());
    appendField(report, "Status", cycle.getStatus());
    appendField(report, "Início", cycle.getStartedAt());
    appendField(report, "Fim", cycle.getFinishedAt());
    appendField(report, "Custo IA registrado", executionCostUsd(cycle.getId()));
    appendField(report, "Erro", cycle.getErrorMessage());
    report.append("\n");
  }

  /** Adiciona a etapa de seed com request e response da IA quando existirem. */
  private void appendSeedSection(StringBuilder report, Optional<OprmNicheResearchSeed> seed) {
    report.append("## 2. Seed de pesquisa (IA)\n\n");
    if (seed.isEmpty()) {
      appendMissing(report);
      return;
    }
    OprmNicheResearchSeed value = seed.get();
    appendField(report, "Modelo", value.getModel());
    appendField(report, "Input tokens", value.getInputTokens());
    appendField(report, "Output tokens", value.getOutputTokens());
    appendField(report, "Custo", value.getCostUsd());
    appendCodeBlock(report, "Request OpenAI", value.getRawOpenAiRequest());
    appendCodeBlock(report, "Response OpenAI", value.getRawOpenAiResponse());
    appendCodeBlock(report, "Resposta modelada", value.getRawModelResponse());
  }

  /** Adiciona a etapa de busca com queries e URLs retornadas. */
  private void appendSearchSection(
      StringBuilder report, List<OprmResearchQuery> queries, List<OprmSourceCandidate> candidates) {
    report.append("## 3. Busca de fontes (Web)\n\n");
    if (queries.isEmpty() && candidates.isEmpty()) {
      appendMissing(report);
      return;
    }
    report.append("### Queries executadas\n\n");
    queries.forEach(
        query ->
            report
                .append("- #")
                .append(query.getId())
                .append(" — ")
                .append(safe(query.getQueryText()))
                .append(" | status: ")
                .append(safe(query.getStatus()))
                .append(" | resultados: ")
                .append(query.getResultCount())
                .append("\n"));
    report.append("\n### URLs retornadas\n\n");
    candidates.forEach(
        candidate ->
            report
                .append("- ")
                .append(safe(candidate.getSourceUrl()))
                .append(" — ")
                .append(safe(candidate.getSourceTitle()))
                .append(" | status: ")
                .append(safe(candidate.getStatus()))
                .append(" | posição: ")
                .append(candidate.getSearchPosition())
                .append("\n  - Snippet: ")
                .append(safe(candidate.getSourceSnippet()))
                .append("\n"));
    report.append("\n");
  }

  /** Adiciona a etapa de coleta com retorno resumido de cada URL acessada. */
  private void appendFetchSection(StringBuilder report, List<OprmSourceSnapshot> snapshots) {
    report.append("## 4. Coleta de URLs (Web)\n\n");
    if (snapshots.isEmpty()) {
      appendMissing(report);
      return;
    }
    snapshots.forEach(
        snapshot -> {
          report.append("### ").append(safe(snapshot.getSourceTitle())).append("\n\n");
          appendField(report, "URL pesquisada", snapshot.getSourceUrl());
          appendField(report, "HTTP", snapshot.getHttpStatus());
          appendField(report, "Status da coleta", snapshot.getFetchStatus());
          appendCodeBlock(report, "Retorno/trecho coletado", snapshot.getShortExcerpt());
          appendCodeBlock(report, "Snippet", snapshot.getSnippet());
        });
  }

  /** Adiciona a etapa de extração de sinais estruturados. */
  private void appendSignalsSection(StringBuilder report, List<OprmExtractedSignal> signals) {
    report.append("## 5. Sinais extraídos\n\n");
    if (signals.isEmpty()) {
      appendMissing(report);
      return;
    }
    signals.forEach(
        signal ->
            report
                .append("- ")
                .append(safe(signal.getSignalType()))
                .append(" — ")
                .append(safe(signal.getSignalText()))
                .append(" | confiança: ")
                .append(signal.getConfidenceScore())
                .append(" | fonte: ")
                .append(safe(signal.getSourceDomain()))
                .append("\n  - Evidência: ")
                .append(safe(signal.getEvidenceExcerpt()))
                .append("\n"));
    report.append("\n");
  }

  /** Adiciona a etapa de síntese de rotina. */
  private void appendSynthesisSection(StringBuilder report, Optional<OprmNicheRoutineCard> card) {
    report.append("## 6. Síntese de rotina\n\n");
    if (card.isEmpty()) {
      appendMissing(report);
      return;
    }
    OprmNicheRoutineCard value = card.get();
    appendCodeBlock(report, "Rotina", value.getRoutineSummary());
    appendCodeBlock(report, "Dores", value.getPainsSummary());
    appendCodeBlock(report, "Resultados", value.getResultsSummary());
    appendCodeBlock(report, "Mecanismos possíveis", value.getMechanismOpportunitiesSummary());
    appendCodeBlock(report, "Evidências", value.getEvidenceSummary());
  }

  /** Adiciona a etapa de perfil MEI/autônomo. */
  private void appendMeiSection(StringBuilder report, Optional<OprmMeiAudienceProfile> profile) {
    report.append("## 7. Perfil MEI/autônomo\n\n");
    if (profile.isEmpty()) {
      appendMissing(report);
      return;
    }
    OprmMeiAudienceProfile value = profile.get();
    appendField(report, "Público", value.getAudienceName());
    appendCodeBlock(report, "Rotina diária", value.getDailyRoutineSummary());
    appendCodeBlock(report, "Tarefas recorrentes", value.getRecurringTasksSummary());
    appendCodeBlock(report, "Dores operacionais", value.getOperationalPainsSummary());
    appendCodeBlock(report, "Linguagem", value.getLanguagePatterns());
  }

  /** Adiciona a etapa de qualidade e materialização quando houver cartão avaliado. */
  private void appendQualitySection(StringBuilder report, Optional<OprmNicheRoutineCard> card) {
    report.append("## 8. Qualidade e materialização\n\n");
    if (card.isEmpty()) {
      appendMissing(report);
      return;
    }
    OprmNicheRoutineCard value = card.get();
    appendField(report, "Status de qualidade", value.getQualityStatus());
    appendField(report, "Pronto para hipótese", value.getReadyForHypothesis());
    appendField(report, "Checado em", value.getQualityCheckedAt());
    appendCodeBlock(report, "Notas de qualidade", value.getQualityNotes());
  }

  /** Adiciona rodapé de rastreabilidade do relatório. */
  private void appendFooter(StringBuilder report) {
    report.append("---\nRelatório gerado pelo backend em ").append(Instant.now()).append(".\n");
  }

  /** Adiciona um campo simples ao Markdown quando existir valor. */
  private void appendField(StringBuilder report, String label, Object value) {
    report.append("- **").append(label).append(":** ").append(safe(value)).append("\n");
  }

  /** Adiciona bloco de código para payloads longos, request e response. */
  private void appendCodeBlock(StringBuilder report, String label, String value) {
    report.append("### ").append(label).append("\n\n```\n").append(safe(value)).append("\n```\n\n");
  }

  /** Registra ausência explícita de dados para uma etapa ainda não executada. */
  private void appendMissing(StringBuilder report) {
    report.append("Dados ainda não registrados para esta etapa.\n\n");
  }

  /** Normaliza valores nulos para texto seguro no relatório. */
  private String safe(Object value) {
    return value == null || value.toString().isBlank() ? "Não registrado" : value.toString();
  }

  /** Soma o custo registrado pelas etapas com telemetria de IA para uma execução do ciclo. */
  private BigDecimal executionCostUsd(Long researchCycleId) {
    return nicheResearchSeedRepository.sumCostUsdByResearchCycleId(researchCycleId);
  }

  /** Converte um ciclo em detalhe operacional completo. */
  private RecordBackendRoutineResearchCycleDetalheDto toDetail(OprmRoutineResearchCycle cycle) {
    return new RecordBackendRoutineResearchCycleDetalheDto(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getStatus(),
        cycle.getCurrentStageCode(),
        cycle.getTotalQueries(),
        cycle.getTotalSourceCandidates(),
        cycle.getTotalSourceSnapshots(),
        cycle.getTotalExtractedSignals(),
        cycle.getStartedAt(),
        cycle.getFinishedAt(),
        cycle.getErrorMessage());
  }
}
