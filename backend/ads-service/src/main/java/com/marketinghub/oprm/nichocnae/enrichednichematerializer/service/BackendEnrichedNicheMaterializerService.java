package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
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
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
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
  private final MarketNicheRepository marketNicheRepository;
  private final MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;
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
      MarketNicheRepository marketNicheRepository,
      MarketNicheEnrichmentProfileRepository enrichmentProfileRepository,
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
    this.marketNicheRepository = marketNicheRepository;
    this.enrichmentProfileRepository = enrichmentProfileRepository;
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
    Optional<MarketNiche> existingMarketNicheByCnaeAndNeutralName = findExistingMarketNicheByCnaeAndNeutralName(cycle);
    boolean existingMatchedByCnaeAndNeutralName = existingMarketNicheByCnaeAndNeutralName.isPresent();
    MarketNiche marketNiche = existingMarketNicheByCnaeAndNeutralName
        .orElseGet(MarketNiche::new);
    applyRoutineCardToMarketNiche(marketNiche, card, cycle, metaSignalPackage);
    applyIdentificationCostToMarketNiche(marketNiche, cycle);
    MarketNiche savedMarketNiche = marketNicheRepository.save(marketNiche);
    MarketNicheEnrichmentProfile profile = buildProfile(card, cycle, candidate, savedMarketNiche, request);
    MarketNicheEnrichmentProfile savedProfile = enrichmentProfileRepository.save(profile);
    updateCycleAndCandidate(
        cycle,
        candidate,
        savedMarketNiche.getId(),
        existingMatchedByCnaeAndNeutralName);
    return new CompleteEnrichedNicheMaterializerResponse(
        researchCycleId,
        card.getId(),
        savedMarketNiche.getId(),
        savedProfile.getId(),
        cycle.getStatus(),
        savedProfile.getCreatedAt(),
        buildCompletionMessage(existingMatchedByCnaeAndNeutralName));
  }

  /** Registra falha da etapa final no ciclo para manter rastreabilidade operacional. */
  @Transactional
  public void fail(Long researchCycleId, FailEnrichedNicheMaterializerRequest request) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    cycle.setStatus(FAILED_STATUS);
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
    MarketNicheEnrichmentProfile profile = enrichmentProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).orElse(null);
    return buildDetailResponse(cycle, card, profile);
  }

  /** Retorna o detalhe de um perfil enriquecido materializado a partir do identificador do perfil. */
  @Transactional(readOnly = true)
  public EnrichedNicheMaterializerDetailResponse detailByProfileId(Long profileId) {
    MarketNicheEnrichmentProfile profile = enrichmentProfileRepository.findById(profileId)
        .orElseThrow(() -> new EntityNotFoundException("Enriched niche profile not found: " + profileId));
    OprmRoutineResearchCycle cycle = findCycle(profile.getResearchCycleId());
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(profile.getResearchCycleId()).orElse(null);
    return buildDetailResponse(cycle, card, profile);
  }

  /** Lista os nichos enriquecidos já gerados para o CNAE informado. */
  @Transactional(readOnly = true)
  public List<GeneratedEnrichedNicheByCnaeResponse> listGeneratedByCnae(String cnaeCode, int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 100));
    return enrichmentProfileRepository.findGeneratedByCnaeCode(cnaeCode, PageRequest.of(0, boundedLimit)).stream()
        .map(this::toGeneratedByCnaeResponse)
        .toList();
  }

  /** Gera o documento Markdown de auditoria do pipeline completo a partir do perfil enriquecido final. */
  @Transactional(readOnly = true)
  public String buildPipelineMarkdownByProfileId(Long profileId) {
    MarketNicheEnrichmentProfile profile = enrichmentProfileRepository.findById(profileId)
        .orElseThrow(() -> new EntityNotFoundException("Enriched niche profile not found: " + profileId));
    OprmRoutineResearchCycle cycle = findCycle(profile.getResearchCycleId());
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(profile.getResearchCycleId()).orElse(null);
    OprmNicheResearchSeed seed = seedRepository.findByResearchCycleId(profile.getResearchCycleId()).orElse(null);
    List<OprmResearchQuery> queries = researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(profile.getResearchCycleId());
    List<OprmSourceCandidate> candidates = sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(
        profile.getResearchCycleId());
    List<OprmSourceSnapshot> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(profile.getResearchCycleId());
    List<OprmExtractedSignal> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(profile.getResearchCycleId());
    return buildPipelineMarkdown(profile, cycle, card, seed, queries, candidates, snapshots, signals);
  }

  /** Localiza ciclos e perfis históricos com termos de solução para orientar reprocessamento neutro. */
  @Transactional(readOnly = true)
  public ContaminatedNicheDiagnosticResponse diagnoseHistoricalContamination(int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 50));
    Map<String, ContaminatedNicheDiagnosticItem> uniqueItems = new LinkedHashMap<>();
    for (String term : SOLUTION_LANGUAGE_TERMS) {
      cycleRepository.findPotentiallyContaminatedByTerm(term, PageRequest.of(0, boundedLimit)).forEach(cycle ->
          uniqueItems.putIfAbsent("CYCLE:" + cycle.getId(), toCycleDiagnosticItem(cycle, term)));
      enrichmentProfileRepository.findPotentiallyContaminatedByTerm(term, PageRequest.of(0, boundedLimit)).forEach(profile ->
          uniqueItems.putIfAbsent("PROFILE:" + profile.getId(), toProfileDiagnosticItem(profile, term)));
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
      OprmRoutineResearchCycle cycle, OprmNicheRoutineCard card, MarketNicheEnrichmentProfile profile) {
    return new EnrichedNicheMaterializerDetailResponse(
        cycle.getId(),
        cycle.getStatus(),
        card == null ? null : card.getId(),
        profile == null ? null : profile.getMarketNiche().getId(),
        profile == null ? null : profile.getId(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        profile == null ? neutralNicheName(cycle) : profile.getNeutralNicheName(),
        cycle.getCnaeCode(),
        card == null ? null : card.getQualityStatus(),
        profile == null ? null : profile.getRoutineSummary(),
        profile == null ? null : profile.getPainsSummary(),
        profile == null ? null : profile.getResultsSummary(),
        profile == null ? null : profile.getMechanismOpportunitiesSummary(),
        profile == null ? null : profile.getEvidenceSummary(),
        profile == null ? null : profile.getSourceDomains(),
        profile == null ? scoreOrZero(card == null ? null : card.getRoutineEvidenceScore()) : profile.getRoutineEvidenceScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getDifficultyEvidenceScore()) : profile.getDifficultyEvidenceScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getSourceDiversityScore()) : profile.getSourceDiversityScore(),
        profile == null ? scoreOrZero(card == null ? null : card.getSolutionLanguageRiskScore()) : profile.getSolutionLanguageRiskScore(),
        profile == null ? null : profile.getCreatedAt());
  }

  /** Converte o perfil enriquecido em resumo para a lista de nichos do CNAE. */
  private GeneratedEnrichedNicheByCnaeResponse toGeneratedByCnaeResponse(MarketNicheEnrichmentProfile profile) {
    return new GeneratedEnrichedNicheByCnaeResponse(
        profile.getId(),
        profile.getMarketNiche().getId(),
        profile.getResearchCycleId(),
        profile.getCnaeCode(),
        profile.getCnaeDescription(),
        profile.getNeutralNicheName(),
        profile.getQualityStatus(),
        profile.getRoutineEvidenceScore(),
        profile.getDifficultyEvidenceScore(),
        profile.getSourceDiversityScore(),
        profile.getCreatedAt());
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

  /** Aplica os dados finais do cartão de rotina ao nicho base antes de salvar a materialização. */
  private void applyRoutineCardToMarketNiche(
      MarketNiche marketNiche,
      OprmNicheRoutineCard card,
      OprmRoutineResearchCycle cycle,
      OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage) {
    marketNiche.setName(requiredText(neutralNicheName(cycle), "neutralNicheName"));
    marketNiche.setDescription(buildMarketNicheDescription(card, cycle));
    marketNiche.setDemandVolume("CNAE " + cycle.getCnaeCode() + " · Score OPRM " + cycle.getSourceScore());
    marketNiche.setPromises(null);
    marketNiche.setOffers(null);
    marketNiche.setBaseSegmentation("Nicho operacional CNAE " + cycle.getCnaeCode() + " - " + cycle.getCnaeDescription());
    marketNiche.setDemographicFilters("Público profissional/empreendedor ligado a " + cycle.getCnaeDescription());
    applyMetaSignalsToNiche(marketNiche, metaSignalPackage);
    marketNiche.setExtraTips(buildExtraTips(card));
  }

  /** Aplica o custo de identificação do NichoCNAE ao nicho base sem duplicar reprocessamentos. */
  private void applyIdentificationCostToMarketNiche(MarketNiche marketNiche, OprmRoutineResearchCycle cycle) {
    BigDecimal identificationCostUsd = seedRepository.sumCostUsdByResearchCycleId(cycle.getId());
    if (identificationCostUsd == null || identificationCostUsd.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    BigDecimal identificationCostBrl = currencyConversionService.usdToBrl(identificationCostUsd);
    if (identificationCostBrl == null || identificationCostBrl.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    if (marketNiche.getCost() == null || marketNiche.getCost().compareTo(BigDecimal.ZERO) == 0) {
      marketNiche.setCost(identificationCostBrl);
    }
    if (marketNiche.getTotalCost() == null || marketNiche.getTotalCost().compareTo(BigDecimal.ZERO) == 0) {
      marketNiche.setTotalCost(identificationCostBrl);
    }
  }

  /** Localiza nicho já vinculado ao mesmo CNAE e ao mesmo nome neutro, permitindo vários nichos diferentes no mesmo CNAE. */
  private Optional<MarketNiche> findExistingMarketNicheByCnaeAndNeutralName(OprmRoutineResearchCycle cycle) {
    String normalizedNeutralName = normalizeLookupText(neutralNicheName(cycle));
    if (!StringUtils.hasText(cycle.getCnaeCode()) || !StringUtils.hasText(normalizedNeutralName)) {
      return Optional.empty();
    }
    List<MarketNicheEnrichmentProfile> existingProfiles = enrichmentProfileRepository.findMaterializedByCnaeAndNormalizedNeutralName(
        cycle.getCnaeCode().trim(), normalizedNeutralName, PageRequest.of(0, 1));
    if (existingProfiles == null || existingProfiles.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(existingProfiles.getFirst().getMarketNiche());
  }

  /** Normaliza texto para comparação canônica simples com a consulta do banco. */
  private String normalizeLookupText(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
  }

  /** Aplica sinais Meta Ads apenas nos campos do backend que serão consultados pelo Facebook Ads. */
  private void applyMetaSignalsToNiche(
      MarketNiche marketNiche,
      OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage) {
    if (marketNiche == null || metaSignalPackage == null) {
      return;
    }
    marketNiche.setInterestList(metaSignalPackage.interests());
    marketNiche.setRoleList(metaSignalPackage.roles());
    marketNiche.setBehaviorList(metaSignalPackage.behaviors());
    marketNiche.setInterests(metaSignalService.buildReadableSignalSummary(metaSignalPackage));
  }

  /** Monta descrição legível do nicho sem criar hipótese ou oferta. */
  private String buildMarketNicheDescription(OprmNicheRoutineCard card, OprmRoutineResearchCycle cycle) {
    return String.join("\n\n",
        "Nicho materializado pelo OPRM NichoCNAE.",
        "CNAE: " + cycle.getCnaeCode() + " - " + cycle.getCnaeDescription(),
        "Nome original recebido para auditoria: " + defaultText(cycle.getOriginalNicheName(), cycle.getNicheName()),
        "Nome neutro pesquisado: " + neutralNicheName(cycle),
        "Rotina observada:\n" + card.getRoutineSummary(),
        "Dificuldades observadas:\n" + card.getPainsSummary(),
        "Perguntas observadas:\n" + card.getResultsSummary(),
        "Contexto operacional e linguagem pública:\n" + materializableOperationalContext(card),
        "Evidências e fontes públicas:\n" + card.getEvidenceSummary());
  }

  /** Monta o Markdown final combinando dados de todas as etapas persistidas do pipeline. */
  private String buildPipelineMarkdown(
      MarketNicheEnrichmentProfile profile,
      OprmRoutineResearchCycle cycle,
      OprmNicheRoutineCard card,
      OprmNicheResearchSeed seed,
      List<OprmResearchQuery> queries,
      List<OprmSourceCandidate> candidates,
      List<OprmSourceSnapshot> snapshots,
      List<OprmExtractedSignal> signals) {
    StringBuilder markdown = new StringBuilder();
    markdown.append("# Pesquisa OPRM NichoCNAE — ").append(markdownText(profile.getNeutralNicheName())).append("\n\n");
    appendKeyValue(markdown, "Perfil enriquecido", profile.getId());
    appendKeyValue(markdown, "Nicho", profile.getMarketNiche().getId());
    appendKeyValue(markdown, "Ciclo de pesquisa", cycle.getId());
    appendKeyValue(markdown, "Status final", cycle.getStatus());
    appendKeyValue(markdown, "CNAE", profile.getCnaeCode() + " - " + profile.getCnaeDescription());
    appendKeyValue(markdown, "Materializado em", profile.getCreatedAt());
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
    appendTextBlock(markdown, "Dores observadas", card.getPainsSummary());
    appendTextBlock(markdown, "Perguntas/resultados observados", card.getResultsSummary());
    appendTextBlock(markdown, "Contexto operacional e linguagem", card.getMechanismOpportunitiesSummary());
    appendTextBlock(markdown, "Evidências consolidadas", card.getEvidenceSummary());
  }

  /** Adiciona a conclusão final do nicho enriquecido materializado. */
  private void appendFinalConclusionSection(
      StringBuilder markdown,
      MarketNicheEnrichmentProfile profile,
      OprmRoutineResearchCycle cycle,
      OprmNicheRoutineCard card,
      List<OprmResearchQuery> queries,
      List<OprmSourceCandidate> candidates,
      List<OprmSourceSnapshot> snapshots,
      List<OprmExtractedSignal> signals) {
    appendSection(markdown, "8. Conclusão final do nicho enriquecido", "Resultado final materializado para uso nas próximas etapas, ainda sem criar hipótese, oferta ou campanha.");
    appendKeyValue(markdown, "Status final", cycle.getStatus());
    appendKeyValue(markdown, "Perfil enriquecido", profile.getId());
    appendKeyValue(markdown, "Nicho operacional", profile.getMarketNiche().getId());
    appendKeyValue(markdown, "Fontes", profile.getSourceDomains());
    appendKeyValue(markdown, "Queries processadas", queries.size());
    appendKeyValue(markdown, "Fontes candidatas processadas", candidates.size());
    appendKeyValue(markdown, "Evidências curtas processadas", snapshots.size());
    appendKeyValue(markdown, "Sinais processados", signals.size());
    appendTextBlock(markdown, "Rotina final", profile.getRoutineSummary());
    appendTextBlock(markdown, "Dores finais", profile.getPainsSummary());
    appendTextBlock(markdown, "Perguntas/resultados finais", profile.getResultsSummary());
    appendTextBlock(markdown, "Contexto operacional final", profile.getMechanismOpportunitiesSummary());
    appendTextBlock(markdown, "Evidências finais", profile.getEvidenceSummary());
    String quality = card == null ? "sem card de qualidade localizado" : card.getQualityStatus();
    markdown.append("\n**Decisão operacional:** o nicho foi materializado como `")
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

  /** Cria o perfil enriquecido com whitelist dos campos funcionais do contrato final. */
  private MarketNicheEnrichmentProfile buildProfile(
      OprmNicheRoutineCard card,
      OprmRoutineResearchCycle cycle,
      OprmNicheCandidate candidate,
      MarketNiche marketNiche,
      CompleteEnrichedNicheMaterializerRequest request) {
    Instant now = Instant.now();
    MarketNicheEnrichmentProfile profile = new MarketNicheEnrichmentProfile();
    profile.setMarketNiche(marketNiche);
    profile.setSourceModule(SOURCE_MODULE);
    profile.setSourceNicheCandidateId(candidate == null ? cycle.getSourceNicheId() : candidate.getId());
    profile.setResearchCycleId(cycle.getId());
    profile.setSourceRoutineCardId(card.getId());
    profile.setCnaeCode(cycle.getCnaeCode());
    profile.setCnaeDescription(cycle.getCnaeDescription());
    profile.setSourceScore(cycle.getSourceScore());
    profile.setQualityStatus(requiredText(card.getQualityStatus(), "qualityStatus"));
    profile.setSpecificityScore(card.getSpecificityScore());
    profile.setConfidenceScore(card.getConfidenceScore());
    profile.setDuplicationScore(card.getDuplicationScore());
    profile.setRoutineEvidenceScore(scoreOrZero(card.getRoutineEvidenceScore()));
    profile.setDifficultyEvidenceScore(scoreOrZero(card.getDifficultyEvidenceScore()));
    profile.setSourceDiversityScore(scoreOrZero(card.getSourceDiversityScore()));
    profile.setSolutionLanguageRiskScore(scoreOrZero(card.getSolutionLanguageRiskScore()));
    profile.setOriginalNicheName(defaultText(cycle.getOriginalNicheName(), cycle.getNicheName()));
    profile.setNeutralNicheName(requiredText(neutralNicheName(cycle), "neutralNicheName"));
    profile.setResearchMode(defaultText(cycle.getResearchMode(), "ROUTINE_REALITY_RESEARCH"));
    profile.setRoutineSummary(requiredText(card.getRoutineSummary(), "routineSummary"));
    profile.setPainsSummary(requiredText(card.getPainsSummary(), "painsSummary"));
    profile.setResultsSummary(requiredText(card.getResultsSummary(), "resultsSummary"));
    profile.setMechanismOpportunitiesSummary(materializableOperationalContext(card));
    profile.setEvidenceSummary(requiredText(card.getEvidenceSummary(), "evidenceSummary"));
    profile.setSourceDomains(trimOptional(card.getSourceDomains()));
    profile.setPersonaSummary(trimOptional(request.personaSummary()));
    profile.setLanguagePatterns(trimOptional(request.languagePatterns()));
    profile.setCommercialTriggers(trimOptional(request.commercialTriggers()));
    profile.setObjections(trimOptional(request.objections()));
    profile.setCreatedBy(defaultText(request.materializedBy(), "oprmEnrichedNicheMaterializer"));
    profile.setCreatedAt(now);
    profile.setUpdatedAt(now);
    return profile;
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
    cycle.setFinishedAt(now);
    cycle.setUpdatedAt(now);
    cycleRepository.save(cycle);
    if (candidate != null) {
      candidate.setMarketNicheId(marketNicheId);
      candidate.setStatus(cycle.getStatus());
      candidate.setRoutineResearchStatus(cycle.getStatus());
      candidate.setLastRoutineResearchCycleId(cycle.getId());
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
  private ContaminatedNicheDiagnosticItem toProfileDiagnosticItem(MarketNicheEnrichmentProfile profile, String matchedTerm) {
    return new ContaminatedNicheDiagnosticItem(
        "PROFILE",
        profile.getId(),
        profile.getResearchCycleId(),
        profile.getMarketNiche().getId(),
        matchedTerm,
        null,
        profile.getMarketNiche().getName(),
        profile.getMarketNiche().getName(),
        profile.getQualityStatus(),
        HISTORICAL_RESEARCH_RECOMMENDATION,
        profile.getCreatedAt());
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
