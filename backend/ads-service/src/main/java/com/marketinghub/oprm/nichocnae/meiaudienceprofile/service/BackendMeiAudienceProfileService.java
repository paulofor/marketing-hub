package com.marketinghub.oprm.nichocnae.meiaudienceprofile.service;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.detailAudienceProfile.MeiAudienceProfileDetailResponse;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileRequest;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serviço único responsável por orquestrar a persistência do perfil de público-alvo MEI/autônomo do OPRM. */
@Service
public class BackendMeiAudienceProfileService {
  private final OprmMeiAudienceProfileRepository repository;

  /** Inicializa o serviço com o repositório canônico de perfis MEI/autônomo. */
  public BackendMeiAudienceProfileService(OprmMeiAudienceProfileRepository repository) {
    this.repository = repository;
  }

  /** Grava ou atualiza o perfil de público-alvo MEI/autônomo de um ciclo de pesquisa. */
  @Transactional
  public UpsertMeiAudienceProfileResponse upsertAudienceProfile(UpsertMeiAudienceProfileRequest request) {
    Instant now = Instant.now();
    OprmMeiAudienceProfile profile = repository
        .findFirstByResearchCycleIdOrderByIdDesc(request.researchCycleId())
        .orElseGet(() -> newProfile(request.researchCycleId(), now));
    applyRequest(profile, request, now);
    return toUpsertResponse(repository.save(profile));
  }

  /** Busca o perfil de público-alvo MEI/autônomo mais recente de um ciclo de pesquisa. */
  @Transactional(readOnly = true)
  public Optional<MeiAudienceProfileDetailResponse> detailByResearchCycleId(Long researchCycleId) {
    return repository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).map(this::toDetailResponse);
  }

  /** Cria uma entidade nova com os metadados mínimos de rastreabilidade temporal. */
  private OprmMeiAudienceProfile newProfile(Long researchCycleId, Instant now) {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setResearchCycleId(researchCycleId);
    profile.setCreatedAt(now);
    return profile;
  }

  /** Aplica ao perfil somente campos do contrato oficial de público-alvo MEI/autônomo. */
  private void applyRequest(
      OprmMeiAudienceProfile profile, UpsertMeiAudienceProfileRequest request, Instant updatedAt) {
    profile.setRoutineCardId(request.routineCardId());
    profile.setSourceNicheCandidateId(request.sourceNicheCandidateId());
    profile.setMarketNicheId(request.marketNicheId());
    profile.setCnaeCode(request.cnaeCode());
    profile.setCnaeDescription(request.cnaeDescription());
    profile.setNeutralNicheName(request.neutralNicheName());
    profile.setAudienceName(request.audienceName());
    profile.setOccupationTerms(request.occupationTerms());
    profile.setWorkMode(request.workMode());
    profile.setCustomerAcquisitionBehavior(request.customerAcquisitionBehavior());
    profile.setDailyRoutineSummary(request.dailyRoutineSummary());
    profile.setRecurringTasksSummary(request.recurringTasksSummary());
    profile.setOperationalPainsSummary(request.operationalPainsSummary());
    profile.setEmotionalPainsSummary(request.emotionalPainsSummary());
    profile.setDreamsSummary(request.dreamsSummary());
    profile.setFearsSummary(request.fearsSummary());
    profile.setLanguagePatterns(request.languagePatterns());
    profile.setChannelsUsed(request.channelsUsed());
    profile.setRecentSourceSummary(request.recentSourceSummary());
    profile.setAutonomousProfessionalFitScore(scoreOrZero(request.autonomousProfessionalFitScore()));
    profile.setBehavioralEvidenceScore(scoreOrZero(request.behavioralEvidenceScore()));
    profile.setSourceFreshnessScore(scoreOrZero(request.sourceFreshnessScore()));
    profile.setOutdatedSourceRiskScore(scoreOrZero(request.outdatedSourceRiskScore()));
    profile.setStructuredBusinessDriftRiskScore(scoreOrZero(request.structuredBusinessDriftRiskScore()));
    profile.setSolutionLanguageRiskScore(scoreOrZero(request.solutionLanguageRiskScore()));
    profile.setUpdatedAt(updatedAt);
  }

  /** Normaliza scores ausentes para zero, preservando o contrato não nulo do banco. */
  private Integer scoreOrZero(Integer score) {
    return score == null ? 0 : score;
  }

  /** Converte a entidade persistida na resposta resumida da operação de gravação. */
  private UpsertMeiAudienceProfileResponse toUpsertResponse(OprmMeiAudienceProfile profile) {
    return new UpsertMeiAudienceProfileResponse(
        profile.getId(),
        profile.getResearchCycleId(),
        profile.getRoutineCardId(),
        profile.getSourceNicheCandidateId(),
        profile.getMarketNicheId(),
        profile.getCnaeCode(),
        profile.getAudienceName(),
        profile.getAutonomousProfessionalFitScore(),
        profile.getBehavioralEvidenceScore(),
        profile.getSourceFreshnessScore(),
        profile.getSolutionLanguageRiskScore(),
        profile.getCreatedAt(),
        profile.getUpdatedAt());
  }

  /** Converte a entidade persistida na resposta completa de detalhe do perfil MEI/autônomo. */
  private MeiAudienceProfileDetailResponse toDetailResponse(OprmMeiAudienceProfile profile) {
    return new MeiAudienceProfileDetailResponse(
        profile.getId(),
        profile.getResearchCycleId(),
        profile.getRoutineCardId(),
        profile.getSourceNicheCandidateId(),
        profile.getMarketNicheId(),
        profile.getCnaeCode(),
        profile.getCnaeDescription(),
        profile.getNeutralNicheName(),
        profile.getAudienceName(),
        profile.getOccupationTerms(),
        profile.getWorkMode(),
        profile.getCustomerAcquisitionBehavior(),
        profile.getDailyRoutineSummary(),
        profile.getRecurringTasksSummary(),
        profile.getOperationalPainsSummary(),
        profile.getEmotionalPainsSummary(),
        profile.getDreamsSummary(),
        profile.getFearsSummary(),
        profile.getLanguagePatterns(),
        profile.getChannelsUsed(),
        profile.getRecentSourceSummary(),
        profile.getAutonomousProfessionalFitScore(),
        profile.getBehavioralEvidenceScore(),
        profile.getSourceFreshnessScore(),
        profile.getOutdatedSourceRiskScore(),
        profile.getStructuredBusinessDriftRiskScore(),
        profile.getSolutionLanguageRiskScore(),
        profile.getCreatedAt(),
        profile.getUpdatedAt());
  }
}
