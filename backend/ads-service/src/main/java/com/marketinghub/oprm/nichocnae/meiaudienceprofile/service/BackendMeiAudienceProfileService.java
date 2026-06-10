package com.marketinghub.oprm.nichocnae.meiaudienceprofile.service;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.detailAudienceProfile.MeiAudienceProfileDetailResponse;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileRequest;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serviço único responsável por orquestrar a persistência do perfil de público-alvo MEI/autônomo do OPRM. */
@Service
public class BackendMeiAudienceProfileService {
  private static final String MEI_AUDIENCE_READY_STATUS = "MEI_AUDIENCE_READY";
  private static final List<String> SOLUTION_LANGUAGE_TERMS = List.of(
      " produto", " oferta", " promessa", " campanha", " landing page", " software", " automação", " inteligência artificial", " ia ", " curso", " ferramenta");

  private final OprmMeiAudienceProfileRepository repository;
  private final OprmNicheRoutineCardRepository routineCardRepository;

  /** Inicializa o serviço com os repositórios canônicos de perfil e gate do público MEI/autônomo. */
  public BackendMeiAudienceProfileService(
      OprmMeiAudienceProfileRepository repository, OprmNicheRoutineCardRepository routineCardRepository) {
    this.repository = repository;
    this.routineCardRepository = routineCardRepository;
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
    return repository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).map(profile -> toDetailResponse(profile, latestCard(profile)));
  }

  /** Busca o perfil aprovado para consumo por módulos posteriores sem liberar pesquisas reprovadas ou contaminadas. */
  @Transactional(readOnly = true)
  public Optional<MeiAudienceProfileDetailResponse> approvedDetailByResearchCycleId(Long researchCycleId) {
    return repository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).map(profile -> {
      OprmNicheRoutineCard card = latestCard(profile);
      validateApprovedForConsumption(profile, card);
      return toDetailResponse(profile, card);
    });
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
  private MeiAudienceProfileDetailResponse toDetailResponse(OprmMeiAudienceProfile profile, OprmNicheRoutineCard card) {
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
        card == null ? null : card.getQualityStatus(),
        card == null ? Boolean.FALSE : isApprovedCard(card),
        card == null ? null : card.getQualityCheckedAt(),
        profile.getAutonomousProfessionalFitScore(),
        profile.getBehavioralEvidenceScore(),
        profile.getSourceFreshnessScore(),
        profile.getOutdatedSourceRiskScore(),
        profile.getStructuredBusinessDriftRiskScore(),
        profile.getSolutionLanguageRiskScore(),
        profile.getCreatedAt(),
        profile.getUpdatedAt());
  }

  /** Localiza o cartão de rotina mais recente vinculado ao perfil para consultar a decisão do gate. */
  private OprmNicheRoutineCard latestCard(OprmMeiAudienceProfile profile) {
    if (profile.getRoutineCardId() != null) {
      return routineCardRepository.findById(profile.getRoutineCardId()).orElse(null);
    }
    return routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(profile.getResearchCycleId()).orElse(null);
  }

  /** Valida que o perfil final foi aprovado no gate e não carrega linguagem de solução no payload publicável. */
  private void validateApprovedForConsumption(OprmMeiAudienceProfile profile, OprmNicheRoutineCard card) {
    if (card == null || !isApprovedCard(card)) {
      throw new IllegalStateException("Perfil MEI/autônomo ainda não foi aprovado pelo gate de qualidade");
    }
    rejectSolutionLanguage(profile);
  }

  /** Confirma que a decisão de qualidade autoriza o consumo do perfil por MDS, MOIS e estratégia. */
  private boolean isApprovedCard(OprmNicheRoutineCard card) {
    return Boolean.TRUE.equals(card.getReadyForHypothesis()) && MEI_AUDIENCE_READY_STATUS.equals(card.getQualityStatus());
  }

  /** Bloqueia contaminação por produto, oferta, campanha ou solução no perfil final exposto. */
  private void rejectSolutionLanguage(OprmMeiAudienceProfile profile) {
    String combined = String.join(" ",
        safeText(profile.getAudienceName()),
        safeText(profile.getOccupationTerms()),
        safeText(profile.getWorkMode()),
        safeText(profile.getCustomerAcquisitionBehavior()),
        safeText(profile.getDailyRoutineSummary()),
        safeText(profile.getRecurringTasksSummary()),
        safeText(profile.getOperationalPainsSummary()),
        safeText(profile.getEmotionalPainsSummary()),
        safeText(profile.getDreamsSummary()),
        safeText(profile.getFearsSummary()),
        safeText(profile.getLanguagePatterns()),
        safeText(profile.getChannelsUsed()),
        safeText(profile.getRecentSourceSummary())).toLowerCase(Locale.ROOT);
    String padded = " " + combined + " ";
    for (String term : SOLUTION_LANGUAGE_TERMS) {
      if (padded.contains(term)) {
        throw new IllegalStateException("Perfil MEI/autônomo contém linguagem de solução proibida: " + term.trim());
      }
    }
  }

  /** Normaliza texto nulo para compor a validação de contaminação sem gerar exceção auxiliar. */
  private String safeText(String value) {
    return value == null ? "" : value;
  }

}
