package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.FacebookCampaignPublicationJobStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persiste os passos de publicação de campanhas por jobId.
 */
public interface FacebookCampaignPublicationJobStepRepository extends JpaRepository<FacebookCampaignPublicationJobStep, Long> {

    /** Lista os passos registrados para um job em ordem cronológica. */
    List<FacebookCampaignPublicationJobStep> findByJobIdOrderByOccurredAtAscIdAsc(String jobId);

    /** Busca o último passo com erro registrado para o experimento. */
    java.util.Optional<FacebookCampaignPublicationJobStep> findTopByExperimentIdAndErrorMessageIsNotNullOrderByOccurredAtDescIdDesc(Long experimentId);
}
