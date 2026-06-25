package com.marketinghub.oprm.nichocnae.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Porta canônica usada pelo OPRM para materializar e consultar nichos sem depender do domínio Niche. */
public interface OprmEnrichedNicheGateway {
  /** Lista subnichos já materializados para o CNAE informado. */
  List<String> listNeutralNicheNamesByCnae(String cnaeCode, int limit);

  /** Busca materialização existente pelo CNAE e nome neutro normalizado. */
  Optional<OprmMarketNicheSnapshot> findByCnaeAndNormalizedNeutralName(String cnaeCode, String normalizedNeutralNicheName);

  /** Materializa ou atualiza o nicho e grava o perfil enriquecido. */
  OprmEnrichedNicheMaterializationResult materialize(OprmMarketNicheDraft nicheDraft, OprmEnrichedNicheProfileDraft profileDraft);

  /** Busca o último perfil enriquecido de um ciclo. */
  Optional<OprmEnrichedNicheProfileSnapshot> findLatestProfileByResearchCycleId(Long researchCycleId);

  /** Busca o perfil enriquecido pelo identificador. */
  OprmEnrichedNicheProfileSnapshot requireProfileById(Long profileId);

  /** Lista perfis já gerados para um CNAE. */
  List<OprmEnrichedNicheProfileSnapshot> listGeneratedByCnae(String cnaeCode, int limit);

  /** Lista perfis com possível contaminação por termo de solução. */
  List<OprmEnrichedNicheProfileSnapshot> findPotentiallyContaminatedByTerm(String term, int limit);

  /** Representa os campos funcionais do nicho base que o OPRM pode solicitar por contrato. */
  record OprmMarketNicheDraft(
      Long marketNicheId,
      String name,
      String description,
      String demandVolume,
      String baseSegmentation,
      String demographicFilters,
      List<String> interestList,
      List<String> roleList,
      List<String> behaviorList,
      String interests,
      String extraTips,
      BigDecimal identificationCostBrl) {}

  /** Representa os campos funcionais do perfil enriquecido que o OPRM pode solicitar por contrato. */
  record OprmEnrichedNicheProfileDraft(
      Long sourceNicheCandidateId,
      Long researchCycleId,
      Long sourceRoutineCardId,
      String cnaeCode,
      String cnaeDescription,
      BigDecimal sourceScore,
      String qualityStatus,
      Integer specificityScore,
      Integer confidenceScore,
      Integer duplicationScore,
      Integer routineEvidenceScore,
      Integer difficultyEvidenceScore,
      Integer sourceDiversityScore,
      Integer solutionLanguageRiskScore,
      String originalNicheName,
      String neutralNicheName,
      String researchMode,
      String routineSummary,
      String personaDailyTasks,
      String painsSummary,
      String resultsSummary,
      String mechanismOpportunitiesSummary,
      String evidenceSummary,
      String sourceDomains,
      String personaSummary,
      String languagePatterns,
      String commercialTriggers,
      String objections,
      String researchReportMarkdown,
      String createdBy,
      Instant createdAt,
      Instant updatedAt) {}

  /** Retorna o identificador do nicho encontrado sem expor entidade Niche ao OPRM. */
  record OprmMarketNicheSnapshot(Long marketNicheId) {}

  /** Retorna o resultado da materialização sem expor entidades Niche ao OPRM. */
  record OprmEnrichedNicheMaterializationResult(Long marketNicheId, Long profileId, Instant createdAt) {}

  /** Projeção de leitura do perfil enriquecido materializado. */
  record OprmEnrichedNicheProfileSnapshot(
      Long profileId,
      Long marketNicheId,
      Long researchCycleId,
      String marketNicheName,
      String cnaeCode,
      String cnaeDescription,
      String neutralNicheName,
      String qualityStatus,
      Integer routineEvidenceScore,
      Integer difficultyEvidenceScore,
      Integer sourceDiversityScore,
      Integer solutionLanguageRiskScore,
      String routineSummary,
      String personaDailyTasks,
      String painsSummary,
      String resultsSummary,
      String mechanismOpportunitiesSummary,
      String evidenceSummary,
      String sourceDomains,
      String researchReportMarkdown,
      Instant createdAt) {}
}
