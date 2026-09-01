package com.marketinghub.repository.jpa.experimentdirectrecruitment;

import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentCampaign;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e bloquear convites de recrutamento direto. */
public interface ExperimentDirectRecruitmentCampaignRepository
    extends JpaRepository<ExperimentDirectRecruitmentCampaign, Long> {

  /** Localiza o convite único do experimento. */
  Optional<ExperimentDirectRecruitmentCampaign> findByExperimentId(Long experimentId);

  /** Localiza o convite público pelo token opaco. */
  Optional<ExperimentDirectRecruitmentCampaign> findByPublicToken(String publicToken);

  /** Serializa mudanças e adesões pelo token para preservar estado e limite. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select c from ExperimentDirectRecruitmentCampaign c join fetch c.experiment where c.publicToken = :publicToken")
  Optional<ExperimentDirectRecruitmentCampaign> findByPublicTokenForUpdate(
      @Param("publicToken") String publicToken);

  /** Serializa aprovação e pausa do convite de um experimento. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select c from ExperimentDirectRecruitmentCampaign c join fetch c.experiment where c.experiment.id = :experimentId")
  Optional<ExperimentDirectRecruitmentCampaign> findByExperimentIdForUpdate(
      @Param("experimentId") Long experimentId);
}
