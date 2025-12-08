package com.marketinghub.worker.experiment;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository focused on read-only queries used by the worker when
 * the packaged {@code ExperimentRepository} does not expose dedicated
 * finder methods yet.
 */
@Repository
public class ExperimentGenerationRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Experiment> findAllToGenerateEmails() {
        TypedQuery<Experiment> query = entityManager.createQuery(
                "select e from Experiment e " +
                        "join fetch e.hypothesisRef " +
                        "left join fetch e.journeyTemplate " +
                        "where e.emailsToGenerate is not null " +
                        "and e.emailsToGenerate > 0",
                Experiment.class
        );
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<Experiment> findAllToGenerateSampleEmails() {
        TypedQuery<Experiment> query = entityManager.createQuery(
                "select e from Experiment e " +
                        "join fetch e.hypothesisRef " +
                        "where e.sampleEmailsToGenerate is not null " +
                        "and e.sampleEmailsToGenerate > 0",
                Experiment.class
        );
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<Experiment> findAllToGenerateInstantForms() {
        TypedQuery<Experiment> query = entityManager.createQuery(
                "select e from Experiment e " +
                        "join fetch e.hypothesisRef " +
                        "left join fetch e.facebookPage " +
                        "where e.instantFormsToGenerate is not null " +
                        "and e.instantFormsToGenerate > 0",
                Experiment.class
        );
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<Experiment> findAllToGenerateDeliverables() {
        TypedQuery<Experiment> query = entityManager.createQuery(
                "select e from Experiment e " +
                        "join fetch e.niche " +
                        "join fetch e.hypothesisRef " +
                        "where e.deliverablesToGenerate is not null " +
                        "and e.deliverablesToGenerate > 0",
                Experiment.class
        );
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<Experiment> findAllToGenerateLeadPortalFlows() {
        TypedQuery<Experiment> query = entityManager.createQuery(
                "select e from Experiment e " +
                        "join fetch e.hypothesisRef " +
                        "where e.leadPortalFlowsToGenerate is not null " +
                        "and e.leadPortalFlowsToGenerate > 0",
                Experiment.class
        );
        return query.getResultList();
    }
}
