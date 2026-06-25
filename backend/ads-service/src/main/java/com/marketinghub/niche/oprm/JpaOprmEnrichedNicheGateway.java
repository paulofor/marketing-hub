package com.marketinghub.niche.oprm;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Adaptador JPA que isola o domínio Niche atrás da porta permitida ao OPRM. */
@Component
public class JpaOprmEnrichedNicheGateway implements OprmEnrichedNicheGateway, PersonaRoutineMaterializerNicheGateway {
  private static final String SOURCE_MODULE = "OPRM_NICHO_CNAE";

  private final MarketNicheRepository marketNicheRepository;
  private final MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;

  /** Inicializa o adaptador com os repositórios canônicos do domínio Niche. */
  public JpaOprmEnrichedNicheGateway(
      MarketNicheRepository marketNicheRepository,
      MarketNicheEnrichmentProfileRepository enrichmentProfileRepository) {
    this.marketNicheRepository = marketNicheRepository;
    this.enrichmentProfileRepository = enrichmentProfileRepository;
  }

  /** Lista nomes neutros já gerados para orientar o OPRM sem expor entidades de Niche. */
  @Override
  public List<String> listNeutralNicheNamesByCnae(String cnaeCode, int limit) {
    return enrichmentProfileRepository.findGeneratedByCnaeCode(cnaeCode, PageRequest.of(0, limit)).stream()
        .map(MarketNicheEnrichmentProfile::getNeutralNicheName)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
  }

  /** Busca materialização existente pelo contrato CNAE + nome neutro normalizado. */
  @Override
  public Optional<OprmMarketNicheSnapshot> findByCnaeAndNormalizedNeutralName(
      String cnaeCode, String normalizedNeutralNicheName) {
    return enrichmentProfileRepository
        .findMaterializedByCnaeAndNormalizedNeutralName(cnaeCode, normalizedNeutralNicheName, PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .map(profile -> new OprmMarketNicheSnapshot(profile.getMarketNiche().getId()));
  }

  /** Materializa ou atualiza o nicho e grava o perfil enriquecido solicitado pelo OPRM. */
  @Override
  public OprmEnrichedNicheMaterializationResult materialize(
      OprmMarketNicheDraft nicheDraft, OprmEnrichedNicheProfileDraft profileDraft) {
    MarketNiche niche = nicheDraft.marketNicheId() == null
        ? new MarketNiche()
        : marketNicheRepository.findById(nicheDraft.marketNicheId()).orElseGet(MarketNiche::new);
    applyNicheDraft(niche, nicheDraft);
    MarketNiche savedNiche = marketNicheRepository.save(niche);
    MarketNicheEnrichmentProfile profile = findExistingProfileForUpdate(profileDraft)
        .orElseGet(MarketNicheEnrichmentProfile::new);
    applyProfileDraft(profile, savedNiche, profileDraft);
    MarketNicheEnrichmentProfile savedProfile = enrichmentProfileRepository.save(profile);
    return new OprmEnrichedNicheMaterializationResult(savedNiche.getId(), savedProfile.getId(), savedProfile.getCreatedAt());
  }

  /** Busca materialização existente pelo contrato exclusivo da etapa persona-routine-materializer v3. */
  @Override
  public Optional<PersonaRoutineMaterializerNicheGateway.MarketNicheSnapshot> findPersonaRoutineMaterializedNiche(
      String cnaeCode, String normalizedNeutralNicheName) {
    return enrichmentProfileRepository
        .findMaterializedByCnaeAndNormalizedNeutralName(cnaeCode, normalizedNeutralNicheName, PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .map(profile -> new PersonaRoutineMaterializerNicheGateway.MarketNicheSnapshot(profile.getMarketNiche().getId()));
  }

  /** Materializa ou atualiza o nicho usando o contrato exclusivo da etapa persona-routine-materializer v3. */
  @Override
  public PersonaRoutineMaterializerNicheGateway.NicheMaterializationResult materialize(
      PersonaRoutineMaterializerNicheGateway.MarketNicheDraft nicheDraft,
      PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft profileDraft) {
    OprmEnrichedNicheMaterializationResult result = materialize(toOprmNicheDraft(nicheDraft), toOprmProfileDraft(profileDraft));
    return new PersonaRoutineMaterializerNicheGateway.NicheMaterializationResult(
        result.marketNicheId(), result.profileId(), result.createdAt());
  }

  /** Busca o perfil mais recente do ciclo sem retornar entidade JPA ao OPRM. */
  @Override
  public Optional<OprmEnrichedNicheProfileSnapshot> findLatestProfileByResearchCycleId(Long researchCycleId) {
    return enrichmentProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).map(this::toSnapshot);
  }

  /** Busca o perfil por identificador ou falha com erro claro de contrato. */
  @Override
  public OprmEnrichedNicheProfileSnapshot requireProfileById(Long profileId) {
    return enrichmentProfileRepository
        .findById(profileId)
        .map(this::toSnapshot)
        .orElseThrow(() -> new EntityNotFoundException("Enriched niche profile not found: " + profileId));
  }

  /** Lista perfis materializados por CNAE sem expor entidades internas. */
  @Override
  public List<OprmEnrichedNicheProfileSnapshot> listGeneratedByCnae(String cnaeCode, int limit) {
    return enrichmentProfileRepository.findGeneratedByCnaeCode(cnaeCode, PageRequest.of(0, limit)).stream()
        .map(this::toSnapshot)
        .toList();
  }

  /** Lista perfis possivelmente contaminados por termos de solução para diagnóstico operacional. */
  @Override
  public List<OprmEnrichedNicheProfileSnapshot> findPotentiallyContaminatedByTerm(String term, int limit) {
    return enrichmentProfileRepository.findPotentiallyContaminatedByTerm(term, PageRequest.of(0, limit)).stream()
        .map(this::toSnapshot)
        .toList();
  }

  /** Aplica no nicho somente os campos permitidos pelo contrato do gateway. */
  private void applyNicheDraft(MarketNiche niche, OprmMarketNicheDraft draft) {
    niche.setName(draft.name());
    niche.setDescription(draft.description());
    niche.setDemandVolume(draft.demandVolume());
    niche.setPromises(null);
    niche.setOffers(null);
    niche.setBaseSegmentation(draft.baseSegmentation());
    niche.setDemographicFilters(draft.demographicFilters());
    niche.setInterestList(draft.interestList());
    niche.setRoleList(draft.roleList());
    niche.setBehaviorList(draft.behaviorList());
    niche.setInterests(draft.interests());
    niche.setExtraTips(draft.extraTips());
    applyIdentificationCost(niche, draft.identificationCostBrl());
  }

  /** Aplica custo de identificação apenas quando o nicho ainda não possui custo acumulado. */
  private void applyIdentificationCost(MarketNiche niche, BigDecimal identificationCostBrl) {
    if (identificationCostBrl == null || identificationCostBrl.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    if (niche.getCost() == null || niche.getCost().compareTo(BigDecimal.ZERO) == 0) {
      niche.setCost(identificationCostBrl);
    }
    if (niche.getTotalCost() == null || niche.getTotalCost().compareTo(BigDecimal.ZERO) == 0) {
      niche.setTotalCost(identificationCostBrl);
    }
  }

  /** Localiza perfil enriquecido existente para atualização do mesmo CNAE e nome neutro. */
  private Optional<MarketNicheEnrichmentProfile> findExistingProfileForUpdate(OprmEnrichedNicheProfileDraft draft) {
    if (draft == null || draft.cnaeCode() == null || draft.neutralNicheName() == null) {
      return Optional.empty();
    }
    return enrichmentProfileRepository
        .findMaterializedByCnaeAndNormalizedNeutralName(
            draft.cnaeCode().trim(), draft.neutralNicheName().trim().toLowerCase(java.util.Locale.ROOT), PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  /** Aplica o perfil enriquecido com whitelist dos campos aceitos pelo domínio Niche. */
  private void applyProfileDraft(MarketNicheEnrichmentProfile profile, MarketNiche niche, OprmEnrichedNicheProfileDraft draft) {
    boolean creating = profile.getId() == null;
    profile.setMarketNiche(niche);
    profile.setSourceModule(SOURCE_MODULE);
    profile.setSourceNicheCandidateId(draft.sourceNicheCandidateId());
    profile.setResearchCycleId(draft.researchCycleId());
    profile.setSourceRoutineCardId(draft.sourceRoutineCardId());
    profile.setCnaeCode(draft.cnaeCode());
    profile.setCnaeDescription(draft.cnaeDescription());
    profile.setSourceScore(draft.sourceScore());
    profile.setQualityStatus(draft.qualityStatus());
    profile.setSpecificityScore(draft.specificityScore());
    profile.setConfidenceScore(draft.confidenceScore());
    profile.setDuplicationScore(draft.duplicationScore());
    profile.setRoutineEvidenceScore(draft.routineEvidenceScore());
    profile.setDifficultyEvidenceScore(draft.difficultyEvidenceScore());
    profile.setSourceDiversityScore(draft.sourceDiversityScore());
    profile.setSolutionLanguageRiskScore(draft.solutionLanguageRiskScore());
    profile.setOriginalNicheName(draft.originalNicheName());
    profile.setNeutralNicheName(draft.neutralNicheName());
    profile.setResearchMode(draft.researchMode());
    profile.setRoutineSummary(draft.routineSummary());
    profile.setPersonaDailyTasks(draft.personaDailyTasks());
    profile.setPainsSummary(draft.painsSummary());
    profile.setResultsSummary(draft.resultsSummary());
    profile.setMechanismOpportunitiesSummary(draft.mechanismOpportunitiesSummary());
    profile.setEvidenceSummary(draft.evidenceSummary());
    profile.setSourceDomains(draft.sourceDomains());
    profile.setPersonaSummary(draft.personaSummary());
    profile.setLanguagePatterns(draft.languagePatterns());
    profile.setCommercialTriggers(draft.commercialTriggers());
    profile.setObjections(draft.objections());
    profile.setResearchReportMarkdown(draft.researchReportMarkdown());
    profile.setCreatedBy(draft.createdBy());
    if (creating) {
      profile.setCreatedAt(draft.createdAt());
    }
    profile.setUpdatedAt(draft.updatedAt());
  }

  /** Converte entidade Niche em snapshot de leitura permitido ao OPRM. */
  private OprmEnrichedNicheProfileSnapshot toSnapshot(MarketNicheEnrichmentProfile profile) {
    return new OprmEnrichedNicheProfileSnapshot(
        profile.getId(),
        profile.getMarketNiche().getId(),
        profile.getResearchCycleId(),
        profile.getMarketNiche().getName(),
        profile.getCnaeCode(),
        profile.getCnaeDescription(),
        profile.getNeutralNicheName(),
        profile.getQualityStatus(),
        profile.getRoutineEvidenceScore(),
        profile.getDifficultyEvidenceScore(),
        profile.getSourceDiversityScore(),
        profile.getSolutionLanguageRiskScore(),
        profile.getRoutineSummary(),
        profile.getPersonaDailyTasks(),
        profile.getPainsSummary(),
        profile.getResultsSummary(),
        profile.getMechanismOpportunitiesSummary(),
        profile.getEvidenceSummary(),
        profile.getSourceDomains(),
        profile.getResearchReportMarkdown(),
        profile.getCreatedAt());
  }

  /** Converte o contrato v3 de nicho para o contrato canônico de persistência OPRM. */
  private OprmMarketNicheDraft toOprmNicheDraft(PersonaRoutineMaterializerNicheGateway.MarketNicheDraft draft) {
    return new OprmMarketNicheDraft(
        draft.marketNicheId(),
        draft.name(),
        draft.description(),
        draft.demandVolume(),
        draft.baseSegmentation(),
        draft.demographicFilters(),
        draft.interestList(),
        draft.roleList(),
        draft.behaviorList(),
        draft.interests(),
        draft.extraTips(),
        draft.identificationCostBrl());
  }

  /** Converte o contrato v3 de perfil enriquecido para o contrato canônico de persistência OPRM. */
  private OprmEnrichedNicheProfileDraft toOprmProfileDraft(
      PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft draft) {
    return new OprmEnrichedNicheProfileDraft(
        draft.sourceNicheCandidateId(),
        draft.researchCycleId(),
        draft.sourceRoutineCardId(),
        draft.cnaeCode(),
        draft.cnaeDescription(),
        draft.sourceScore(),
        draft.qualityStatus(),
        draft.specificityScore(),
        draft.confidenceScore(),
        draft.duplicationScore(),
        draft.routineEvidenceScore(),
        draft.difficultyEvidenceScore(),
        draft.sourceDiversityScore(),
        draft.solutionLanguageRiskScore(),
        draft.originalNicheName(),
        draft.neutralNicheName(),
        draft.researchMode(),
        draft.routineSummary(),
        draft.personaDailyTasks(),
        draft.painsSummary(),
        draft.resultsSummary(),
        draft.mechanismOpportunitiesSummary(),
        draft.evidenceSummary(),
        draft.sourceDomains(),
        draft.personaSummary(),
        draft.languagePatterns(),
        draft.commercialTriggers(),
        draft.objections(),
        draft.researchReportMarkdown(),
        draft.createdBy(),
        draft.createdAt(),
        draft.updatedAt());
  }
}
