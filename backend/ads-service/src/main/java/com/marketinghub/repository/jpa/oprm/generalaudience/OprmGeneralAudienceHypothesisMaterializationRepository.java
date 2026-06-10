package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Repository;

/** Repositório responsável por materializar hipóteses criadas pelo fluxo OPRM de público geral. */
@Repository
public class OprmGeneralAudienceHypothesisMaterializationRepository {

    private final EntityManager entityManager;

    /** Inicializa o repositório com o EntityManager canônico de persistência JPA. */
    public OprmGeneralAudienceHypothesisMaterializationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Cria a hipótese em backlog vinculada ao MarketNiche materializado do subnicho geral. */
    public OprmGeneralAudienceMaterializedHypothesis createHypothesis(
            Long marketNicheId,
            String title,
            String statement,
            String pain,
            String persona,
            String mechanism,
            String leadMagnet,
            String successRule,
            BigDecimal kpiTargetCpl,
            String promptAudit) {
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setMarketNiche(entityManager.getReference(MarketNiche.class, marketNicheId));
        hypothesis.setTitle(title);
        hypothesis.setPromise(statement);
        hypothesis.setProblem(pain);
        hypothesis.setPersona(persona);
        hypothesis.setMechanism(mechanism);
        hypothesis.setUniqueMechanism(mechanism);
        hypothesis.setEntrega(leadMagnet);
        hypothesis.setSuccessRule(successRule);
        hypothesis.setImageFilterTitle(title);
        hypothesis.setPrompt(promptAudit);
        hypothesis.setKpiTargetCpl(kpiTargetCpl);
        hypothesis.setGeneratedAt(Instant.now());
        hypothesis.setStatus(HypothesisStatus.BACKLOG);
        entityManager.persist(hypothesis);
        return new OprmGeneralAudienceMaterializedHypothesis(
                hypothesis.getId(),
                hypothesis.getTitle(),
                hypothesis.getStatus().name(),
                hypothesis.getGeneratedAt());
    }
}
