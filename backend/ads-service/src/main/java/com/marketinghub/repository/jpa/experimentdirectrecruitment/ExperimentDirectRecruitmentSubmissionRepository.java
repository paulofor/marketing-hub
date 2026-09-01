package com.marketinghub.repository.jpa.experimentdirectrecruitment;

import com.marketinghub.experiment.directrecruitment.v1.DirectRecruitmentSubmissionStatus;
import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentSubmission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e resumir adesões qualificadas do convite direto. */
public interface ExperimentDirectRecruitmentSubmissionRepository
    extends JpaRepository<ExperimentDirectRecruitmentSubmission, Long> {

  /** Localiza uma tentativa idempotente pelo identificador do navegador. */
  Optional<ExperimentDirectRecruitmentSubmission> findByCampaignIdAndSubmissionKey(
      Long campaignId, String submissionKey);

  /** Informa se a mesma pessoa já aderiu ao convite. */
  boolean existsByCampaignIdAndContactFingerprint(Long campaignId, String contactFingerprint);

  /** Conta todas as adesões do convite. */
  long countByCampaignId(Long campaignId);

  /** Conta adesões por resultado de qualificação. */
  long countByCampaignIdAndStatus(Long campaignId, DirectRecruitmentSubmissionStatus status);
}
