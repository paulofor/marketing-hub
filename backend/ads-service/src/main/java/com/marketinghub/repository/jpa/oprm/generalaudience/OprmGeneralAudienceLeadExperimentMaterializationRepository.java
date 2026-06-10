package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Repositório responsável por materializar experimentos de lead/isca do fluxo OPRM de público geral. */
@Repository
public class OprmGeneralAudienceLeadExperimentMaterializationRepository {

    private final EntityManager entityManager;

    /** Inicializa o repositório com o EntityManager canônico de persistência JPA. */
    public OprmGeneralAudienceLeadExperimentMaterializationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Cria experimento planejado de lead/isca sem solicitar publicação de campanha. */
    public OprmGeneralAudienceMaterializedLeadExperiment createLeadExperiment(
            Long marketNicheId,
            UUID hypothesisId,
            String name,
            String hypothesisStatement,
            String primaryMetric,
            BigDecimal stopLossCpl,
            BigDecimal dailyBudget,
            Integer durationDays,
            BigDecimal kpiTargetCpl,
            Integer sampleSize,
            String campaignAngle,
            String adCopy,
            String landingPageCopy) {
        Hypothesis hypothesis = entityManager.find(Hypothesis.class, hypothesisId);
        if (hypothesis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesisId não encontrado: " + hypothesisId);
        }
        if (hypothesis.getMarketNiche() == null || !marketNicheId.equals(hypothesis.getMarketNiche().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hipótese não pertence ao MarketNiche do subnicho geral");
        }
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(durationDays - 1L);
        Experiment experiment = new Experiment();
        experiment.setNiche(entityManager.getReference(MarketNiche.class, marketNicheId));
        experiment.setHypothesisRef(hypothesis);
        experiment.setName(name);
        experiment.setHypothesis(truncate(hypothesisStatement, 255));
        experiment.setPrimaryVariable("Isca/lead para Público Geral");
        experiment.setPrimaryMetric(primaryMetric);
        experiment.setStopLossCpl(stopLossCpl);
        experiment.setKpiTargetCpl(kpiTargetCpl);
        experiment.setDailyBudget(dailyBudget);
        experiment.setCost(dailyBudget.multiply(BigDecimal.valueOf(durationDays.longValue())));
        experiment.setTotalCost(experiment.getCost());
        experiment.setSampleSize(sampleSize);
        experiment.setStartDate(startDate);
        experiment.setEndDate(endDate);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setStage(ExperimentStage.LANDING);
        experiment.setCreativeGenerationMode(CreativeGenerationMode.DEFAULT);
        experiment.setCreativeApproved(false);
        experiment.setCampaignAngle(campaignAngle);
        experiment.setAdCopy(adCopy);
        experiment.setLandingPageCopy(landingPageCopy);
        entityManager.persist(experiment);
        return new OprmGeneralAudienceMaterializedLeadExperiment(
                experiment.getId(),
                experiment.getName(),
                experiment.getStatus().name(),
                experiment.getPrimaryMetric(),
                experiment.getStopLossCpl(),
                experiment.getDailyBudget(),
                experiment.getStartDate(),
                experiment.getEndDate());
    }

    /** Limita campos curtos de entidade sem perder o contrato principal do experimento. */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
