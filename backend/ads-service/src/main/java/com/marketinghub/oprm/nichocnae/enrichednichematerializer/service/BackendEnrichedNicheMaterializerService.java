package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.detailStageExecution.EnrichedNicheMaterializerDetailResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination.ContaminatedNicheDiagnosticItem;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination.ContaminatedNicheDiagnosticResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.failStageExecution.FailEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private static final String FAILED_STATUS = "ENRICHED_NICHE_FAILED";
  private static final String HISTORICAL_RESEARCH_RECOMMENDATION = "Preferir novo ciclo neutro em vez de editar manualmente o texto antigo.";
  private static final List<String> SOLUTION_LANGUAGE_TERMS = List.of(
      "ia", "inteligência artificial", "automação", "software", "sistema", "app", "ferramenta", "curso", "template", "oferta", "landing page");

  private final OprmRoutineResearchCycleRepository cycleRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;
  private final OprmNicheCandidateRepository nicheCandidateRepository;
  private final MarketNicheRepository marketNicheRepository;
  private final MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;

  /** Inicializa o serviço com os repositórios oficiais do backend usados pela etapa final. */
  public BackendEnrichedNicheMaterializerService(
      OprmRoutineResearchCycleRepository cycleRepository,
      OprmNicheRoutineCardRepository routineCardRepository,
      OprmNicheCandidateRepository nicheCandidateRepository,
      MarketNicheRepository marketNicheRepository,
      MarketNicheEnrichmentProfileRepository enrichmentProfileRepository) {
    this.cycleRepository = cycleRepository;
    this.routineCardRepository = routineCardRepository;
    this.nicheCandidateRepository = nicheCandidateRepository;
    this.marketNicheRepository = marketNicheRepository;
    this.enrichmentProfileRepository = enrichmentProfileRepository;
  }

  /** Lista cartões aprovados pelo gate que ainda precisam materializar nicho e nicho enriquecido. */
  @Transactional(readOnly = true)
  public List<RecordEnrichedNicheMaterializerPending> listPending() {
    return routineCardRepository.findPendingEnrichedNicheMaterialization(PageRequest.of(0, MAX_PENDING)).stream()
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
    if (enrichmentProfileRepository.existsBySourceRoutineCardId(card.getId())) {
      MarketNicheEnrichmentProfile existing = enrichmentProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId)
          .orElseThrow(() -> new IllegalStateException("routine card already materialized without retrievable profile"));
      return new CompleteEnrichedNicheMaterializerResponse(
          researchCycleId, card.getId(), existing.getMarketNiche().getId(), existing.getId(), cycle.getStatus(), existing.getCreatedAt());
    }

    OprmNicheCandidate candidate = nicheCandidateRepository.findById(cycle.getSourceNicheId()).orElse(null);
    MarketNiche marketNiche = resolveMarketNiche(card, cycle, candidate);
    MarketNicheEnrichmentProfile profile = buildProfile(card, cycle, candidate, marketNiche, request);
    MarketNicheEnrichmentProfile savedProfile = enrichmentProfileRepository.save(profile);
    updateCycleAndCandidate(cycle, candidate, marketNiche.getId());
    return new CompleteEnrichedNicheMaterializerResponse(
        researchCycleId, card.getId(), marketNiche.getId(), savedProfile.getId(), cycle.getStatus(), savedProfile.getCreatedAt());
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
        card == null ? neutralNicheName(cycle) : card.getNicheName(),
        cycle.getCnaeCode(),
        card == null ? null : card.getQualityStatus(),
        profile == null ? null : profile.getRoutineSummary(),
        profile == null ? null : profile.getPainsSummary(),
        profile == null ? null : profile.getResultsSummary(),
        profile == null ? null : profile.getMechanismOpportunitiesSummary(),
        profile == null ? null : profile.getEvidenceSummary(),
        profile == null ? null : profile.getSourceDomains(),
        profile == null ? null : profile.getCreatedAt());
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
        neutralNicheName(cycle),
        cycle.getSourceScore(),
        card.getQualityStatus(),
        card.getSpecificityScore(),
        card.getConfidenceScore(),
        card.getDuplicationScore(),
        card.getRoutineSummary(),
        card.getPainsSummary(),
        card.getResultsSummary(),
        card.getMechanismOpportunitiesSummary(),
        card.getEvidenceSummary(),
        card.getSourceDomains(),
        card.getQualityCheckedAt());
  }

  /** Reaproveita nicho existente do candidato ou cria um nicho base com dados enriquecidos do card. */
  private MarketNiche resolveMarketNiche(OprmNicheRoutineCard card, OprmRoutineResearchCycle cycle, OprmNicheCandidate candidate) {
    MarketNiche marketNiche = candidate != null && candidate.getMarketNicheId() != null
        ? marketNicheRepository.findById(candidate.getMarketNicheId()).orElseGet(MarketNiche::new)
        : new MarketNiche();
    marketNiche.setName(requiredText(neutralNicheName(cycle), "neutralNicheName"));
    marketNiche.setDescription(buildMarketNicheDescription(card, cycle));
    marketNiche.setDemandVolume("CNAE " + cycle.getCnaeCode() + " · Score OPRM " + cycle.getSourceScore());
    marketNiche.setPromises(null);
    marketNiche.setOffers(null);
    marketNiche.setBaseSegmentation("Nicho operacional CNAE " + cycle.getCnaeCode() + " - " + cycle.getCnaeDescription());
    marketNiche.setDemographicFilters("Público profissional/empreendedor ligado a " + cycle.getCnaeDescription());
    marketNiche.setInterests(card.getSourceDomains());
    marketNiche.setExtraTips(buildExtraTips(card));
    return marketNicheRepository.save(marketNiche);
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
        "Contexto operacional:\n" + card.getResultsSummary(),
        "Linguagem e evidências públicas:\n" + card.getEvidenceSummary());
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
    profile.setRoutineSummary(requiredText(card.getRoutineSummary(), "routineSummary"));
    profile.setPainsSummary(requiredText(card.getPainsSummary(), "painsSummary"));
    profile.setResultsSummary(requiredText(card.getResultsSummary(), "resultsSummary"));
    profile.setMechanismOpportunitiesSummary(requiredText(card.getMechanismOpportunitiesSummary(), "mechanismOpportunitiesSummary"));
    profile.setEvidenceSummary(requiredText(card.getEvidenceSummary(), "evidenceSummary"));
    profile.setSourceDomains(trimOptional(card.getSourceDomains()));
    profile.setPersonaSummary(trimOptional(request.personaSummary()));
    profile.setLanguagePatterns(trimOptional(request.languagePatterns()));
    profile.setCommercialTriggers(null);
    profile.setObjections(null);
    profile.setCreatedBy(defaultText(request.materializedBy(), "oprmEnrichedNicheMaterializer"));
    profile.setCreatedAt(now);
    profile.setUpdatedAt(now);
    return profile;
  }

  /** Atualiza ciclo e candidato para indicar que o nicho enriquecido foi materializado. */
  private void updateCycleAndCandidate(OprmRoutineResearchCycle cycle, OprmNicheCandidate candidate, Long marketNicheId) {
    Instant now = Instant.now();
    cycle.setStatus(ENRICHED_STATUS);
    cycle.setFinishedAt(now);
    cycle.setUpdatedAt(now);
    cycleRepository.save(cycle);
    if (candidate != null) {
      candidate.setMarketNicheId(marketNicheId);
      candidate.setStatus(ENRICHED_STATUS);
      candidate.setRoutineResearchStatus(ENRICHED_STATUS);
      candidate.setLastRoutineResearchCycleId(cycle.getId());
      candidate.setUpdatedAt(now);
      nicheCandidateRepository.save(candidate);
    }
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
