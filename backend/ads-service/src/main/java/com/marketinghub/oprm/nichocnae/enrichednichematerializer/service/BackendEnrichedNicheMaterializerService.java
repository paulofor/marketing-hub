package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.detailStageExecution.EnrichedNicheMaterializerDetailResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination.ContaminatedNicheDiagnosticItem;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination.ContaminatedNicheDiagnosticResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.failStageExecution.FailEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.generatedByCnae.GeneratedEnrichedNicheByCnaeResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheMaterializationResult;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheProfileDraft;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheProfileSnapshot;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmMarketNicheDraft;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmMarketNicheSnapshot;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Serviço backend da etapa final que alimenta as tabelas de nicho e nicho enriquecido a partir do NichoCNAE. */
@Service
public class BackendEnrichedNicheMaterializerService {
  private static final int MAX_PENDING = 10;
  private static final String SOURCE_MODULE = "OPRM_NICHO_CNAE";
  private static final String ENRICHED_STATUS = "ENRICHED_NICHE_CREATED";
  private static final String ENRICHED_UPDATED_STATUS = "ENRICHED_NICHE_UPDATED";
  private static final String FAILED_STATUS = "ENRICHED_NICHE_FAILED";
  private static final String CURRENT_STAGE_ENRICHED_NICHE_MATERIALIZER = "enriched-niche-materializer";
  private static final String HISTORICAL_RESEARCH_RECOMMENDATION = "Preferir novo ciclo neutro em vez de editar manualmente o texto antigo.";
  private static final List<String> SOLUTION_LANGUAGE_TERMS = List.of(
      "ia", "inteligência artificial", "automação", "software", "sistema", "app", "ferramenta", "curso", "template", "oferta", "landing page");

  private final OprmRoutineResearchCycleRepository cycleRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;
  private final OprmNicheCandidateRepository nicheCandidateRepository;
  private final OprmNicheResearchSeedRepository seedRepository;
  private final OprmResearchQueryRepository researchQueryRepository;
  private final OprmSourceCandidateRepository sourceCandidateRepository;
  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmEnrichedNicheGateway enrichedNicheGateway;
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository;
  private final OprmEnrichedNicheMetaSignalService metaSignalService;
  private final OprmCurrencyConversionService currencyConversionService;

  /** Inicializa o serviço com os repositórios oficiais do backend usados pela etapa final. */
  public BackendEnrichedNicheMaterializerService(
      OprmRoutineResearchCycleRepository cycleRepository,
      OprmNicheRoutineCardRepository routineCardRepository,
      OprmNicheCandidateRepository nicheCandidateRepository,
      OprmNicheResearchSeedRepository seedRepository,
      OprmResearchQueryRepository researchQueryRepository,
      OprmSourceCandidateRepository sourceCandidateRepository,
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmEnrichedNicheGateway enrichedNicheGateway,
      OprmMeiAudienceProfileRepository meiAudienceProfileRepository,
      OprmEnrichedNicheMetaSignalService metaSignalService,
      OprmCurrencyConversionService currencyConversionService) {
    this.cycleRepository = cycleRepository;
    this.routineCardRepository = routineCardRepository;
    this.nicheCandidateRepository = nicheCandidateRepository;
    this.seedRepository = seedRepository;
    this.researchQueryRepository = researchQueryRepository;
    this.sourceCandidateRepository = sourceCandidateRepository;
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.enrichedNicheGateway = enrichedNicheGateway;
    this.meiAudienceProfileRepository = meiAudienceProfileRepository;
    this.metaSignalService = metaSignalService;
    this.currencyConversionService = currencyConversionService;
  }

  /** Lista cartões aprovados pelo gate que ainda precisam materializar nicho e nicho enriquecido. */
  @Transactional(readOnly = true)
  public List<RecordEnrichedNicheMaterializerPending> listPending() {
    return routineCardRepository.findPendingEnrichedNicheMaterialization(PageRequest.of(0, MAX_PENDING)).stream()
        .filter(this::hasMeiAudienceProfileForPending)
        .map(this::toPending)
        .toList();
  }

  /** Alimenta a tabela de nicho e a tabela de nicho enriquecido para o ciclo informado. */
  @Transactional
  public CompleteEnrichedNicheMaterializerResponse complete(Long researchCycleId, CompleteEnrichedNicheMaterializerRequest request) {
    validateCompletionRequest(researchCycleId, request);
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    OprmNicheRoutineCard card = routineCardRepository.findById(request.routineCardId())
        .orElseThrow(() -> new EntityNotFoundException("Routine card not found: " + request.routineCardId()));
    if (!researchCycleId.equals(card.getResearchCycleId())) {
      throw new IllegalArgumentException("routineCardId does not belong to researchCycleId");
    }
    if (!Boolean.TRUE.equals(card.getReadyForHypothesis())) {
      throw new IllegalArgumentException("routine card is not approved by quality gate");
    }
    OprmNicheCandidate candidate = nicheCandidateRepository.findById(cycle.getSourceNicheId()).orElse(null);
    OprmMeiAudienceProfile meiAudienceProfile = requireApprovedMeiAudienceProfile(cycle);
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage = metaSignalService.buildSignalPackage(cycle, card);
    Optional<OprmMarketNicheSnapshot> existingMarketNicheByCnaeAndNeutralName = findExistingMarketNicheByCnaeAndNeutralName(cycle);
    boolean existingMatchedByCnaeAndNeutralName = existingMarketNicheByCnaeAndNeutralName.isPresent();
    OprmMarketNicheDraft marketNicheDraft = buildMarketNicheDraft(
        existingMarketNicheByCnaeAndNeutralName.map(OprmMarketNicheSnapshot::marketNicheId).orElse(null),
        card,
        cycle,
        metaSignalPackage);
    OprmEnrichedNicheProfileDraft profileDraft = buildProfileDraft(card, cycle, candidate, request);
    OprmEnrichedNicheMaterializationResult materialization = enrichedNicheGateway.materialize(marketNicheDraft, profileDraft);
    updateCycleAndCandidate(
        cycle,
        candidate,
        materialization.marketNicheId(),
        existingMatchedByCnaeAndNeutralName);
    return new CompleteEnrichedNicheMaterializerResponse(
        researchCycleId,
        card.getId(),
        materialization.marketNicheId(),
        materialization.profileId(),
        cycle.getStatus(),
        materialization.createdAt(),
        buildCompletionMessage(existingMatchedByCnaeAndNeutralName));
  }

  /** Registra falha da etapa final no ciclo para manter rastreabilidade operacional. */
  @Transactional
  public void fail(Long researchCycleId, FailEnrichedNicheMaterializerRequest request) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    cycle.setStatus(FAILED_STATUS);
    cycle.setCurrentStageCode(CURRENT_STAGE_ENRICHED_NICHE_MATERIALIZER);
    cycle.setErrorMessage(defaultText(request == null ? null : request.errorMessage(), "Falha na materialização de nicho enriquecido"));
    cycle.setFinishedAt(Instant.now());
    cycle.setUpdatedAt(Instant.now());
    cycleRepository.save(cycle);
  }

  /** Retorna o detalhe da materialização final para acompanhamento na tela do pipeline. */
  @Transactional(readOnly = true)
  public EnrichedNicheMaterializerDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).orElse(null);
    OprmEnrichedNicheProfileSnapshot profile = enrichedNicheGateway.findLatestProfileByResearchCycleId(researchCycleId).orElse(null);
    return buildDetailResponse(cycle, card, profile);
  }

  /** Retorna o detalhe de um perfil enriquecido materializado a partir do identificador do perfil. */
  @Transactional(readOnly = true)
  public EnrichedNicheMaterializerDetailResponse detailByProfileId(Long profileId) {
    OprmEnrichedNicheProfileSnapshot profile = enrichedNicheGateway.requireProfileById(profileId);
    OprmRoutineResearchCycle cycle = findCycle(profile.researchCycleId());
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(profile.researchCycleId()).orElse(null);
    return buildDetailResponse(cycle, card, profile);
  }

  /** Lista os nichos enriquecidos já gerados para o CNAE informado. */
  @Transactional(readOnly = true)
  public List<GeneratedEnrichedNicheByCnaeResponse> listGeneratedByCnae(String cnaeCode, int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 100));
    return enrichedNicheGateway.listGeneratedByCnae(cnaeCode, boundedLimit).stream()
        .map(this::toGeneratedByCnaeResponse)
        .toList();
  }

  /** Gera o documento Markdown de auditoria do pipeline completo a partir do perfil enriquecido final. */
  @Transactional(readOnly = true)
  public String buildPipelineMarkdownByProfileId(Long profileId) {
    OprmEnrichedNicheProfileSnapshot profile = enrichedNicheGateway.requireProfileById(profileId);
    return buildPipelineMarkdownForCycle(profile.researchCycleId(), profile);
  }

  /** Gera o documento Markdown de auditoria do job mesmo quando ele ainda não materializou perfil final. */
  @Transactional(readOnly = true)
  public String buildPipelineMarkdownByResearchCycleId(Long researchCycleId) {
    OprmEnrichedNicheProfileSnapshot profile = enrichedNicheGateway.findLatestProfileByResearchCycleId(researchCycleId).orElse(null);
    return buildPipelineMarkdownForCycle(researchCycleId, profile);
  }

  /** Consolida os artefatos atuais de um job OPRM para download operacional. */
  private String buildPipelineMarkdownForCycle(Long researchCycleId, OprmEnrichedNicheProfileSnapshot profile) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).orElse(null);
    OprmNicheResearchSeed seed = seedRepository.findByResearchCycleId(researchCycleId).orElse(null);
    List<OprmResearchQuery> queries = safeList(researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(researchCycleId));
    List<OprmSourceCandidate> candidates = safeList(sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(
        researchCycleId));
    List<OprmSourceSnapshot> snapshots = safeList(sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId));
    List<OprmExtractedSignal> signals = safeList(extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId));
    return buildPipelineMarkdown(profile, cycle, card, seed, queries, candidates, snapshots, signals);
  }

  /** Garante coleção vazia quando algum repositório mockado ou legado retornar nulo. */
  private <T> List<T> safeList(List<T> items) {
    return items == null ? List.of() : items;
  }

  /** Localiza ciclos e perfis históricos com termos de solução para orientar reprocessamento neutro. */
  @Transactional(readOnly = true)
  public ContaminatedNicheDiagnosticResponse diagnoseHistoricalContamination(int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 50));
    Map<String, ContaminatedNicheDiagnosticItem> uniqueItems = new LinkedHashMap<>();
    for (String term : SOLUTION_LANGUAGE_TERMS) {
      cycleRepository.findPotentiallyContaminatedByTerm(term, PageRequest.of(0, boundedLimit)).forEach(cycle ->
          uniqueItems.putIfAbsent("CYCLE:" + cycle.getId(), toCycleDiagnosticItem(cycle, term)));
      enrichedNicheGateway.findPotentiallyContaminatedByTerm(term, boundedLimit).forEach(profile ->
          uniqueItems.putIfAbsent("PROFILE:" + profile.profileId(), toProfileDiagnosticItem(profile, term)));
    }
    List<ContaminatedNicheDiagnosticItem> items = new ArrayList<>(uniqueItems.values()).stream()
        .limit(boundedLimit)
        .toList();
    int totalCycles = (int) items.stream().filter(item -> "CYCLE".equals(item.recordType())).count();
    int totalProfiles = (int) items.stream().filter(item -> "PROFILE".equals(item.recordType())).count();
    return new ContaminatedNicheDiagnosticResponse(Instant.now(), totalCycles, totalProfiles, items);
  }

  /** Monta o DTO público de detalhe combinando ciclo, card e perfil enriquecido. */
  private EnrichedNicheMaterializerDetailResponse buildDetailResponse(
      OprmRoutineResearchCycle cycle, OprmNicheRoutineCard card, OprmEnrichedNicheProfileSnapshot profile) {
    return new EnrichedNicheMaterializerDetailResponse(
        cycle.getId(),
        cycle.getStatus(),
        card == null ? null : card.getId(),
        profile == null ? null : profile.marketNicheId(),
        profile == null ? null : profile.profileId(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        profile == null ? neutralNicheName(cycle) : profile.neutralNicheName(),
        cycle.getCnaeCode(),
        card == null ? null : card.getQualityStatus(),
        profile == null ? null : profile.routineSummary(),
        profile == null ? (card == null ? null : buildPersonaDailyTasks(card)) : profile.personaDailyTasks(),
        profile == null ? null : profile.painsSummary(),
        profile == null ? null : profile.resultsSummary(),
        profile == null ? null : profile.mechanismOpportunitiesSummary(),
        profile == null ? null : profile.evidenceSummary(),
        profile == null ? null : profile.sourceDomains(),
        profile == null ? scoreOrZero(card == null ? null : card.getRoutineEvidenceScore()) : profile.routineEvidenceScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getDifficultyEvidenceScore()) : profile.difficultyEvidenceScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getSourceDiversityScore()) : profile.sourceDiversityScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getSolutionLanguageRiskScore()) : profile.solutionLanguageRiskScore(),
        profile == null ? null : profile.createdAt());
  }

  /** Converte o perfil enriquecido em resumo para a lista de nichos do CNAE. */
  private GeneratedEnrichedNicheByCnaeResponse toGeneratedByCnaeResponse(OprmEnrichedNicheProfileSnapshot profile) {
    return new GeneratedEnrichedNicheByCnaeResponse(
        profile.profileId(),
        profile.marketNicheId(),
        profile.researchCycleId(),
        profile.cnaeCode(),
        profile.cnaeDescription(),
        profile.neutralNicheName(),
        profile.qualityStatus(),
        profile.routineEvidenceScore(),
        profile.difficultyEvidenceScore(),
        profile.sourceDiversityScore(),
        profile.createdAt());
  }

  /** Confirma se a pendência final tem perfil MEI/autônomo do próprio ciclo antes de expor ao coletor. */
  private boolean hasMeiAudienceProfileForPending(OprmNicheRoutineCard card) {
    return meiAudienceProfileRepository.existsByResearchCycleId(card.getResearchCycleId());
  }

  /** Converte cartão aprovado em unidade de trabalho completa para o coletor OPRM. */
  private RecordEnrichedNicheMaterializerPending toPending(OprmNicheRoutineCard card) {
    OprmRoutineResearchCycle cycle = findCycle(card.getResearchCycleId());
    OprmNicheCandidate candidate = nicheCandidateRepository.findById(cycle.getSourceNicheId()).orElse(null);
    return new RecordEnrichedNicheMaterializerPending(
        card.getId(),
        cycle.getId(),
        cycle.getSourceNicheId(),
        candidate == null ? null : candidate.getMarketNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        defaultText(cycle.getOriginalNicheName(), cycle.getNicheName()),
        neutralNicheName(cycle),
        defaultText(cycle.getResearchMode(), "ROUTINE_REALITY_RESEARCH"),
        cycle.getSourceScore(),
        card.getQualityStatus(),
        card.getSpecificityScore(),
        card.getConfidenceScore(),
        card.getDuplicationScore(),
        scoreOrZero(card.getRoutineEvidenceScore()),
        scoreOrZero(card.getDifficultyEvidenceScore()),
        scoreOrZero(card.getSourceDiversityScore()),
        scoreOrZero(card.getSolutionLanguageRiskScore()),
        card.getRoutineSummary(),
        card.getPainsSummary(),
        card.getCustomerBehaviorSummary(),
        card.getChannelsSummary(),
        card.getResultsSummary(),
        card.getMechanismOpportunitiesSummary(),
        card.getEvidenceSummary(),
        card.getSourceDomains(),
        card.getQualityCheckedAt());
  }

  /** Exige perfil MEI/autônomo do próprio ciclo para impedir mistura entre subnichos do mesmo CNAE. */
  private OprmMeiAudienceProfile requireApprovedMeiAudienceProfile(OprmRoutineResearchCycle cycle) {
    return meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(cycle.getId())
        .orElseThrow(() -> new IllegalStateException(
            "Ciclo OPRM aguardando perfil MEI/autônomo aprovado antes da materialização final"));
  }

  /** Monta o contrato do nicho base que será materializado fora do pacote OPRM. */
  private OprmMarketNicheDraft buildMarketNicheDraft(
      Long marketNicheId,
      OprmNicheRoutineCard card,
      OprmRoutineResearchCycle cycle,
      OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage) {
    BigDecimal identificationCostUsd = totalIdentificationCostUsd(cycle);
    BigDecimal identificationCostBrl =
        identificationCostUsd == null || identificationCostUsd.compareTo(BigDecimal.ZERO) <= 0
            ? null
            : currencyConversionService.usdToBrl(identificationCostUsd);
    return new OprmMarketNicheDraft(
        marketNicheId,
        requiredText(neutralNicheName(cycle), "neutralNicheName"),
        buildMarketNicheDescription(card, cycle),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        "CNAE " + cycle.getCnaeCode() + " · Score OPRM " + cycle.getSourceScore(),
        "Nicho operacional CNAE " + cycle.getCnaeCode() + " - " + cycle.getCnaeDescription(),
        "Público profissional/empreendedor ligado a " + cycle.getCnaeDescription(),
        metaSignalPackage == null ? List.of() : metaSignalPackage.interests(),
        metaSignalPackage == null ? List.of() : metaSignalPackage.roles(),
        metaSignalPackage == null ? List.of() : metaSignalPackage.behaviors(),
        metaSignalPackage == null ? null : metaSignalService.buildReadableSignalSummary(metaSignalPackage),
        buildExtraTips(card),
        identificationCostBrl);
  }

  /** Soma o custo atual do seed com o custo preservado de tentativas anteriores do mesmo job. */
  private BigDecimal totalIdentificationCostUsd(OprmRoutineResearchCycle cycle) {
    BigDecimal currentSeedCost = seedRepository.sumCostUsdByResearchCycleId(cycle.getId());
    BigDecimal preservedReprocessCost = cycle.getReprocessPreservedCostUsd();
    return nullToZero(currentSeedCost).add(nullToZero(preservedReprocessCost));
  }

  /** Normaliza custo nulo para zero antes de somar valores auditáveis. */
  private BigDecimal nullToZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Localiza nicho já vinculado ao mesmo CNAE e ao mesmo nome neutro, permitindo vários nichos diferentes no mesmo CNAE. */
  private Optional<OprmMarketNicheSnapshot> findExistingMarketNicheByCnaeAndNeutralName(OprmRoutineResearchCycle cycle) {
    String normalizedNeutralName = normalizeLookupText(neutralNicheName(cycle));
    if (!StringUtils.hasText(cycle.getCnaeCode()) || !StringUtils.hasText(normalizedNeutralName)) {
      return Optional.empty();
    }
    return enrichedNicheGateway.findByCnaeAndNormalizedNeutralName(cycle.getCnaeCode().trim(), normalizedNeutralName);
  }

  /** Normaliza texto para comparação canônica simples com a consulta do banco. */
  private String normalizeLookupText(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
  }

  /** Monta descrição legível do nicho sem criar hipótese ou oferta. */
  private String buildMarketNicheDescription(OprmNicheRoutineCard card, OprmRoutineResearchCycle cycle) {
    return String.join("\n\n",
        "Nicho materializado pelo OPRM NichoCNAE.",
        "CNAE: " + cycle.getCnaeCode() + " - " + cycle.getCnaeDescription(),
        "Nome original recebido para auditoria: " + defaultText(cycle.getOriginalNicheName(), cycle.getNicheName()),
        "Nome neutro pesquisado: " + neutralNicheName(cycle),
        "Rotina observada:\n" + card.getRoutineSummary(),
        "Tarefas diárias da persona:\n" + buildPersonaDailyTasks(card),
        "Dificuldades observadas:\n" + card.getPainsSummary(),
        "Perguntas observadas:\n" + card.getResultsSummary(),
        "Contexto operacional e linguagem pública:\n" + materializableOperationalContext(card),
        "Evidências e fontes públicas:\n" + card.getEvidenceSummary());
  }

  /** Monta o Markdown final combinando dados de todas as etapas persistidas do pipeline. */
  private String buildPipelineMarkdown(
      OprmEnrichedNicheProfileSnapshot profile,
      OprmRoutineResearchCycle cycle,
      OprmNicheRoutineCard card,
      OprmNicheResearchSeed seed,
      List<OprmResearchQuery> queries,
      List<OprmSourceCandidate> candidates,
      List<OprmSourceSnapshot> snapshots,
      List<OprmExtractedSignal> signals) {
    StringBuilder markdown = new StringBuilder();
    String reportName = profile == null ? neutralNicheName(cycle) : profile.neutralNicheName();
    markdown.append("# Pesquisa OPRM NichoCNAE — ").append(markdownText(reportName)).append("\n\n");
    appendKeyValue(markdown, "Job de pesquisa", cycle.getId());
    appendKeyValue(markdown, "Perfil enriquecido", profile == null ? null : profile.profileId());
    appendKeyValue(markdown, "Nicho", profile == null ? null : profile.marketNicheId());
    appendKeyValue(markdown, "Status atual", cycle.getStatus());
    appendKeyValue(markdown, "Gatilho atual", cycle.getTriggerSource());
    appendKeyValue(markdown, "CNAE", cycle.getCnaeCode() + " - " + cycle.getCnaeDescription());
    appendKeyValue(markdown, "Materializado em", profile == null ? null : profile.createdAt());
    appendTextBlock(markdown, "Reexecuções e observações do job", cycle.getErrorMessage());
    markdown.append("\n");

    appendSection(markdown, "1. Abertura do ciclo", "O ciclo iniciou a pesquisa de rotina real do nicho, preservando o nome original apenas para auditoria e usando nome neutro para evitar contaminação por solução.");
    appendKeyValue(markdown, "Nome original", cycle.getOriginalNicheName());
    appendKeyValue(markdown, "Nome neutro", cycle.getNeutralNicheName());
    appendKeyValue(markdown, "Modo de pesquisa", cycle.getResearchMode());
    appendKeyValue(markdown, "Score OPRM", cycle.getSourceScore());
    appendKeyValue(markdown, "Risco inicial de linguagem de solução", cycle.getSolutionLanguageRiskScore());
    appendKeyValue(markdown, "Início", cycle.getStartedAt());
    appendKeyValue(markdown, "Fim", cycle.getFinishedAt());

    appendSeedSection(markdown, seed);
    appendQueriesSection(markdown, queries);
    appendCandidatesSection(markdown, candidates);
    appendSnapshotsSection(markdown, snapshots);
    appendSignalsSection(markdown, signals);
    appendRoutineCardSection(markdown, card);
    appendFinalConclusionSection(markdown, profile, cycle, card, queries, candidates, snapshots, signals);
    return markdown.toString();
  }

  /** Adiciona a seção de seed operacional quando a etapa inicial foi persistida. */
  private void appendSeedSection(StringBuilder markdown, OprmNicheResearchSeed seed) {
    appendSection(markdown, "2. Seed de pesquisa", "Perfil operacional usado para orientar as buscas sem criar oferta ou hipótese comercial.");
    if (seed == null) {
      markdown.append("Seed não encontrado para este ciclo.\n");
      return;
    }
    appendKeyValue(markdown, "Tipo de negócio", seed.getBusinessType());
    appendKeyValue(markdown, "Tipo de operação", seed.getOperationType());
    appendKeyValue(markdown, "Tipo de cliente", seed.getCustomerType());
    appendKeyValue(markdown, "Objetos comerciais observáveis", seed.getCommercialObjects());
    appendKeyValue(markdown, "Suposições iniciais de rotina", seed.getInitialAssumptions());
    appendKeyValue(markdown, "Nível de confiança", seed.getConfidenceLevel());
  }

  /** Adiciona a seção de queries processadas pela busca de fontes. */
  private void appendQueriesSection(StringBuilder markdown, List<OprmResearchQuery> queries) {
    appendSection(markdown, "3. Frases de pesquisa processadas", "Consultas executáveis usadas para procurar rotina, dificuldades, perguntas, linguagem e contexto operacional.");
    appendKeyValue(markdown, "Total de queries", queries.size());
    queries.forEach(query -> markdown
        .append("- #").append(query.getId())
        .append(" · ").append(markdownText(query.getQueryGoal()))
        .append(" · prioridade ").append(valueText(query.getPriority()))
        .append(" · status ").append(markdownText(query.getStatus()))
        .append(" · resultados ").append(valueText(query.getResultCount()))
        .append("\n  - Query: ").append(markdownText(query.getQueryText()))
        .append("\n"));
  }

  /** Adiciona a seção de fontes candidatas encontradas na pesquisa. */
  private void appendCandidatesSection(StringBuilder markdown, List<OprmSourceCandidate> candidates) {
    appendSection(markdown, "4. Fontes candidatas encontradas", "Fontes localizadas pela busca antes da coleta curta de evidências.");
    appendKeyValue(markdown, "Total de fontes candidatas", candidates.size());
    appendCounts(markdown, "Por status", candidates.stream().map(OprmSourceCandidate::getStatus).toList());
    candidates.forEach(candidate -> markdown
        .append("- #").append(candidate.getId())
        .append(" · ").append(markdownText(candidate.getSourceDomain()))
        .append(" · status ").append(markdownText(candidate.getStatus()))
        .append(" · intenção ").append(markdownText(candidate.getSourceIntent()))
        .append(" · score rotina ").append(valueText(candidate.getRoutineEvidenceScore()))
        .append("\n  - Título: ").append(markdownText(candidate.getSourceTitle()))
        .append("\n  - URL: ").append(markdownText(candidate.getSourceUrl()))
        .append("\n  - Snippet: ").append(markdownText(candidate.getSourceSnippet()))
        .append("\n"));
  }

  /** Adiciona a seção de snapshots curtos coletados das fontes selecionadas. */
  private void appendSnapshotsSection(StringBuilder markdown, List<OprmSourceSnapshot> snapshots) {
    appendSection(markdown, "5. Evidências curtas coletadas", "Trechos curtos e metadados coletados das fontes selecionadas para extração de sinais.");
    appendKeyValue(markdown, "Total de snapshots", snapshots.size());
    appendCounts(markdown, "Por intenção da fonte", snapshots.stream().map(OprmSourceSnapshot::getSourceIntent).toList());
    snapshots.forEach(snapshot -> markdown
        .append("- #").append(snapshot.getId())
        .append(" · ").append(markdownText(snapshot.getSourceDomain()))
        .append(" · fetch ").append(markdownText(snapshot.getFetchStatus()))
        .append(" · extração ").append(markdownText(snapshot.getSignalExtractionStatus()))
        .append(" · score rotina ").append(valueText(snapshot.getRoutineEvidenceScore()))
        .append("\n  - Título: ").append(markdownText(snapshot.getSourceTitle()))
        .append("\n  - Trecho: ").append(markdownText(snapshot.getShortExcerpt()))
        .append("\n"));
  }

  /** Adiciona a seção de sinais estruturados extraídos das evidências. */
  private void appendSignalsSection(StringBuilder markdown, List<OprmExtractedSignal> signals) {
    appendSection(markdown, "6. Sinais extraídos", "Sinais classificados que sustentaram a síntese da rotina, dificuldades, perguntas e linguagem pública.");
    appendKeyValue(markdown, "Total de sinais", signals.size());
    appendCounts(markdown, "Por tipo de sinal", signals.stream().map(OprmExtractedSignal::getSignalType).toList());
    signals.forEach(signal -> markdown
        .append("- #").append(signal.getId())
        .append(" · ").append(markdownText(signal.getSignalType()))
        .append(" · ").append(markdownText(signal.getSourceDomain()))
        .append(" · confiança ").append(valueText(signal.getConfidenceScore()))
        .append("%\n  - Sinal: ").append(markdownText(signal.getSignalText()))
        .append("\n  - Evidência: ").append(markdownText(signal.getEvidenceExcerpt()))
        .append("\n"));
  }

  /** Adiciona a seção do cartão sintetizado e da decisão do gate de qualidade. */
  private void appendRoutineCardSection(StringBuilder markdown, OprmNicheRoutineCard card) {
    appendSection(markdown, "7. Síntese da rotina e gate de qualidade", "Card consolidado com a decisão de qualidade antes da materialização do nicho enriquecido.");
    if (card == null) {
      markdown.append("Cartão de rotina não encontrado para este ciclo.\n");
      return;
    }
    appendKeyValue(markdown, "Card", card.getId());
    appendKeyValue(markdown, "Status de qualidade", card.getQualityStatus());
    appendKeyValue(markdown, "Aprovado para materialização", card.getReadyForHypothesis());
    appendKeyValue(markdown, "Score de evidência de rotina", card.getRoutineEvidenceScore());
    appendKeyValue(markdown, "Score de evidência de dificuldade", card.getDifficultyEvidenceScore());
    appendKeyValue(markdown, "Diversidade de fontes", card.getSourceDiversityScore());
    appendKeyValue(markdown, "Risco de linguagem de solução", card.getSolutionLanguageRiskScore());
    appendTextBlock(markdown, "Rotina observada", card.getRoutineSummary());
    appendTextBlock(markdown, "Tarefas diárias da persona", buildPersonaDailyTasks(card));
    appendTextBlock(markdown, "Dores observadas", card.getPainsSummary());
    appendTextBlock(markdown, "Perguntas/resultados observados", card.getResultsSummary());
    appendTextBlock(markdown, "Contexto operacional e linguagem", card.getMechanismOpportunitiesSummary());
    appendTextBlock(markdown, "Evidências consolidadas", card.getEvidenceSummary());
  }

  /** Adiciona a conclusão final do nicho enriquecido materializado. */
  private void appendFinalConclusionSection(
      StringBuilder markdown,
      OprmEnrichedNicheProfileSnapshot profile,
      OprmRoutineResearchCycle cycle,
      OprmNicheRoutineCard card,
      List<OprmResearchQuery> queries,
      List<OprmSourceCandidate> candidates,
      List<OprmSourceSnapshot> snapshots,
      List<OprmExtractedSignal> signals) {
    appendSection(markdown, "8. Conclusão final do nicho enriquecido", "Resultado final materializado para uso nas próximas etapas, ainda sem criar hipótese, oferta ou campanha.");
    appendKeyValue(markdown, "Status atual", cycle.getStatus());
    appendKeyValue(markdown, "Perfil enriquecido", profile == null ? null : profile.profileId());
    appendKeyValue(markdown, "Nicho operacional", profile == null ? null : profile.marketNicheId());
    appendKeyValue(markdown, "Fontes", profile == null ? null : profile.sourceDomains());
    appendKeyValue(markdown, "Queries processadas", queries.size());
    appendKeyValue(markdown, "Fontes candidatas processadas", candidates.size());
    appendKeyValue(markdown, "Evidências curtas processadas", snapshots.size());
    appendKeyValue(markdown, "Sinais processados", signals.size());
    appendTextBlock(markdown, "Rotina final", profile == null ? null : profile.routineSummary());
    appendTextBlock(markdown, "Tarefas diárias finais da persona", profile == null ? (card == null ? null : buildPersonaDailyTasks(card)) : profile.personaDailyTasks());
    appendTextBlock(markdown, "Dores finais", profile == null ? null : profile.painsSummary());
    appendTextBlock(markdown, "Perguntas/resultados finais", profile == null ? null : profile.resultsSummary());
    appendTextBlock(markdown, "Contexto operacional final", profile == null ? null : profile.mechanismOpportunitiesSummary());
    appendTextBlock(markdown, "Evidências finais", profile == null ? null : profile.evidenceSummary());
    String quality = card == null ? "sem card de qualidade localizado" : card.getQualityStatus();
    markdown.append("\n**Decisão operacional:** o job está em `")
        .append(markdownText(cycle.getStatus()))
        .append("` com qualidade `")
        .append(markdownText(quality))
        .append("`. Use este documento como auditoria da pesquisa de rotina real antes de avançar para hipótese, mecanismo, prova e oferta.\n");
  }

  /** Adiciona cabeçalho de seção com explicação curta de negócio. */
  private void appendSection(StringBuilder markdown, String title, String description) {
    markdown.append("\n## ").append(markdownText(title)).append("\n\n");
    markdown.append(markdownText(description)).append("\n\n");
  }

  /** Adiciona campo simples ao documento Markdown. */
  private void appendKeyValue(StringBuilder markdown, String label, Object value) {
    markdown.append("- **").append(markdownText(label)).append(":** ").append(markdownText(valueText(value))).append("\n");
  }

  /** Adiciona bloco textual preservando quebras de linha úteis para auditoria. */
  private void appendTextBlock(StringBuilder markdown, String label, String value) {
    markdown.append("\n### ").append(markdownText(label)).append("\n\n");
    markdown.append(markdownText(defaultText(value, "Não informado"))).append("\n");
  }

  /** Adiciona contagens agrupadas de valores processados. */
  private void appendCounts(StringBuilder markdown, String label, List<String> values) {
    Map<String, Long> counts = values.stream()
        .filter(Objects::nonNull)
        .map(this::defaultGroupText)
        .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    if (counts.isEmpty()) {
      return;
    }
    markdown.append("- **").append(markdownText(label)).append(":** ");
    markdown.append(counts.entrySet().stream()
        .map(entry -> markdownText(entry.getKey()) + " (" + entry.getValue() + ")")
        .collect(Collectors.joining(", ")));
    markdown.append("\n");
  }

  /** Normaliza textos nulos usados em agrupamentos do relatório. */
  private String defaultGroupText(String value) {
    return StringUtils.hasText(value) ? value.trim() : "Não informado";
  }

  /** Converte valores simples para texto seguro de exibição. */
  private String valueText(Object value) {
    if (value == null) {
      return "Não informado";
    }
    String text = String.valueOf(value);
    return StringUtils.hasText(text) ? text : "Não informado";
  }

  /** Remove caracteres de controle que poderiam quebrar o arquivo Markdown baixado pelo usuário. */
  private String markdownText(String text) {
    if (text == null) {
      return "Não informado";
    }
    return text.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "").trim();
  }

  /** Monta dicas operacionais de uso do nicho para próximas etapas sem acionar hipótese. */
  private String buildExtraTips(OprmNicheRoutineCard card) {
    return String.join("\n\n",
        "Use este registro como auditoria de rotina real antes de qualquer fluxo posterior de hipótese.",
        "Status de qualidade da rotina: " + card.getQualityStatus(),
        "Especificidade: " + card.getSpecificityScore() + "%",
        "Confiança: " + card.getConfidenceScore() + "%",
        "Duplicação: " + card.getDuplicationScore() + "%");
  }

  /** Cria o contrato do perfil enriquecido com whitelist dos campos funcionais do contrato final. */
  private OprmEnrichedNicheProfileDraft buildProfileDraft(
      OprmNicheRoutineCard card,
      OprmRoutineResearchCycle cycle,
      OprmNicheCandidate candidate,
      CompleteEnrichedNicheMaterializerRequest request) {
    Instant now = Instant.now();
    return new OprmEnrichedNicheProfileDraft(
        candidate == null ? cycle.getSourceNicheId() : candidate.getId(),
        cycle.getId(),
        card.getId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getSourceScore(),
        requiredText(card.getQualityStatus(), "qualityStatus"),
        card.getSpecificityScore(),
        card.getConfidenceScore(),
        card.getDuplicationScore(),
        scoreOrZero(card.getRoutineEvidenceScore()),
        scoreOrZero(card.getDifficultyEvidenceScore()),
        scoreOrZero(card.getSourceDiversityScore()),
        scoreOrZero(card.getSolutionLanguageRiskScore()),
        defaultText(cycle.getOriginalNicheName(), cycle.getNicheName()),
        requiredText(neutralNicheName(cycle), "neutralNicheName"),
        defaultText(cycle.getResearchMode(), "ROUTINE_REALITY_RESEARCH"),
        requiredText(card.getRoutineSummary(), "routineSummary"),
        buildPersonaDailyTasks(card),
        requiredText(card.getPainsSummary(), "painsSummary"),
        requiredText(card.getResultsSummary(), "resultsSummary"),
        materializableOperationalContext(card),
        requiredText(card.getEvidenceSummary(), "evidenceSummary"),
        trimOptional(card.getSourceDomains()),
        trimOptional(request.personaSummary()),
        trimOptional(request.languagePatterns()),
        trimOptional(request.commercialTriggers()),
        trimOptional(request.objections()),
        buildPipelineMarkdownForCycle(cycle.getId(), null),
        defaultText(request.materializedBy(), "oprmEnrichedNicheMaterializer"),
        now,
        now);
  }

  /** Monta uma lista objetiva de tarefas diárias da persona a partir da rotina validada. */
  private String buildPersonaDailyTasks(OprmNicheRoutineCard card) {
    List<String> tasks = new ArrayList<>();
    addTaskIfPresent(tasks, card.getRoutineSummary());
    addTaskIfPresent(tasks, card.getCustomerBehaviorSummary());
    addTaskIfPresent(tasks, card.getChannelsSummary());
    addTaskIfPresent(tasks, card.getMechanismOpportunitiesSummary());
    if (tasks.isEmpty()) {
      return "Tarefas diárias não foram individualizadas no cartão aprovado; usar a rotina observada como fonte primária e reprocessar a síntese quando precisar de lista granular.";
    }
    return tasks.stream()
        .distinct()
        .limit(12)
        .map(task -> "- " + task)
        .collect(Collectors.joining("\n"));
  }

  /** Extrai frases operacionais curtas que representem ação cotidiana da persona. */
  private void addTaskIfPresent(List<String> tasks, String source) {
    if (!StringUtils.hasText(source)) {
      return;
    }
    String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
    for (String piece : normalized.split("[\\n.;•]+")) {
      String task = piece.replaceFirst("^\\s*[-–—*0-9.)]+\\s*", "").trim();
      String lower = task.toLowerCase(Locale.ROOT);
      if (task.length() >= 18
          && (lower.contains("atend")
              || lower.contains("cliente")
              || lower.contains("agenda")
              || lower.contains("mensag")
              || lower.contains("orçamento")
              || lower.contains("cobran")
              || lower.contains("venda")
              || lower.contains("divulga")
              || lower.contains("rotina")
              || lower.contains("serviço"))) {
        tasks.add(task);
      }
    }
  }

  /** Preserva apenas o bloco de contexto/linguagem quando ele foi sintetizado como pesquisa de rotina real. */
  private String materializableOperationalContext(OprmNicheRoutineCard card) {
    String context = requiredText(card.getMechanismOpportunitiesSummary(), "operationalContextSummary");
    String normalized = context.toLowerCase(java.util.Locale.ROOT);
    if (normalized.contains("contexto operacional") || normalized.contains("linguagem do nicho")) {
      return context.trim();
    }
    return "Contexto operacional e linguagem pública não disponíveis em formato compatível no cartão aprovado; reprocessar a síntese neutra antes de usar este bloco.";
  }

  /** Normaliza pontuações nulas para zero mantendo escala percentual. */
  private Integer scoreOrZero(Integer value) {
    return value == null ? 0 : Math.max(0, Math.min(100, value));
  }

  /** Atualiza ciclo e candidato para indicar criação ou revisão, preservando múltiplos nichos possíveis por CNAE. */
  private void updateCycleAndCandidate(
      OprmRoutineResearchCycle cycle, OprmNicheCandidate candidate, Long marketNicheId, boolean existingMatchedByCnaeAndNeutralName) {
    Instant now = Instant.now();
    cycle.setStatus(existingMatchedByCnaeAndNeutralName ? ENRICHED_UPDATED_STATUS : ENRICHED_STATUS);
    cycle.setCurrentStageCode(null);
    cycle.setFinishedAt(now);
    cycle.setUpdatedAt(now);
    cycleRepository.save(cycle);
    if (candidate != null) {
      candidate.setMarketNicheId(marketNicheId);
      candidate.setStatus(cycle.getStatus());
      candidate.setRoutineResearchStatus(cycle.getStatus());
      candidate.setLastRoutineResearchCycleId(cycle.getId());
      candidate.setOfferIdea(null);
      candidate.setUpdatedAt(now);
      nicheCandidateRepository.save(candidate);
    }
  }

  /** Monta mensagem operacional deixando claro se houve criação ou revisão de nicho existente. */
  private String buildCompletionMessage(boolean existingMatchedByCnaeAndNeutralName) {
    if (existingMatchedByCnaeAndNeutralName) {
      return "Ciclo materializado como atualização/revisão de market_niche existente para o mesmo CNAE e nome neutro; nenhum novo nicho foi criado.";
    }
    return "Ciclo materializado com criação de novo market_niche para este subnicho; o mesmo CNAE pode ter outros nichos com nomes neutros diferentes.";
  }

  /** Converte um ciclo contaminado em item de diagnóstico operacional. */
  private ContaminatedNicheDiagnosticItem toCycleDiagnosticItem(OprmRoutineResearchCycle cycle, String matchedTerm) {
    return new ContaminatedNicheDiagnosticItem(
        "CYCLE",
        cycle.getId(),
        cycle.getId(),
        null,
        matchedTerm,
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getNicheName(),
        cycle.getStatus(),
        HISTORICAL_RESEARCH_RECOMMENDATION,
        cycle.getStartedAt());
  }

  /** Converte um perfil contaminado em item de diagnóstico operacional. */
  private ContaminatedNicheDiagnosticItem toProfileDiagnosticItem(OprmEnrichedNicheProfileSnapshot profile, String matchedTerm) {
    return new ContaminatedNicheDiagnosticItem(
        "PROFILE",
        profile.profileId(),
        profile.researchCycleId(),
        profile.marketNicheId(),
        matchedTerm,
        null,
        profile.marketNicheName(),
        profile.marketNicheName(),
        profile.qualityStatus(),
        HISTORICAL_RESEARCH_RECOMMENDATION,
        profile.createdAt());
  }

  /** Retorna o nome neutro operacional do ciclo com fallback seguro para ciclos legados. */
  private String neutralNicheName(OprmRoutineResearchCycle cycle) {
    return defaultText(cycle.getNeutralNicheName(), cycle.getNicheName());
  }

  /** Localiza o ciclo de pesquisa de rotina ou falha com erro contratual claro. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return cycleRepository.findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload de conclusão da etapa final. */
  private void validateCompletionRequest(Long researchCycleId, CompleteEnrichedNicheMaterializerRequest request) {
    if (researchCycleId == null) {
      throw new IllegalArgumentException("researchCycleId is required");
    }
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (!researchCycleId.equals(request.researchCycleId())) {
      throw new IllegalArgumentException("researchCycleId must match path");
    }
    if (request.routineCardId() == null) {
      throw new IllegalArgumentException("routineCardId is required");
    }
  }

  /** Exige texto útil para campos funcionais obrigatórios. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Retorna texto padrão quando o valor opcional não veio preenchido. */
  private String defaultText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  /** Normaliza texto opcional para nulo quando não existe conteúdo útil. */
  private String trimOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
