package com.marketinghub.repository.jpa.experimentdirectrecruitment;

import com.marketinghub.experiment.directrecruitment.v1.ExperimentDirectRecruitmentVisit;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e contar visitas únicas ao convite de recrutamento. */
public interface ExperimentDirectRecruitmentVisitRepository
    extends JpaRepository<ExperimentDirectRecruitmentVisit, Long> {

  /** Informa se o navegador pseudonimizado já foi contabilizado. */
  boolean existsByCampaignIdAndVisitorFingerprint(Long campaignId, String visitorFingerprint);

  /** Conta visitantes únicos do convite. */
  long countByCampaignId(Long campaignId);
}
