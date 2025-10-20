package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repository for experiments.
 */

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    @Override
    @EntityGraph(attributePaths = {"facebookPage", "instagramAccount"})
    Optional<Experiment> findById(Long id);
    List<Experiment> findByNicheId(Long nicheId);
    boolean existsByNicheAndName(MarketNiche niche, String name);
    List<Experiment> findByStatus(ExperimentStatus status);
    List<Experiment> findByStatusAndPlatform(ExperimentStatus status, ExperimentPlatform platform);
    @Query("""
            select distinct e from Experiment e
            join fetch e.niche n
            join fetch e.hypothesisRef h
            join fetch e.instagramAccount ig
            left join fetch e.facebookPage fp
            left join fetch e.facebookInstantForm fif
            left join fetch fif.page fifp
            left join fetch e.journeyTemplate jt
            left join fetch jt.steps steps
            where e.status = :status
              and e.platform = :platform
              and e.creativeApproved = true
              and ig is not null
              and exists (
                    select 1 from Audience a
                    where a.niche = e.niche
                      and a.approved = true
                      and (a.hypothesis is null or a.hypothesis = e.hypothesisRef)
              )
            """)
    List<Experiment> findReadyForCampaign(@Param("status") ExperimentStatus status,
                                          @Param("platform") ExperimentPlatform platform);
    /**
     * Retrieves experiments configured to generate creatives.
     *
     * <p>Filters are handled in the query so we only fetch the records we
     * actually need.</p>
     */
    @Query("""
            select e from Experiment e
            join fetch e.hypothesisRef
            where e.creativesToGenerate is not null
              and e.creativesToGenerate > 0
            """)
    List<Experiment> findAllToGenerateCreatives();

    @Query("""
            select e from Experiment e
            join fetch e.hypothesisRef
            left join fetch e.facebookPage
            where e.instantFormsToGenerate is not null
              and e.instantFormsToGenerate > 0
            """)
    List<Experiment> findAllToGenerateInstantForms();

    @Query("""
            select e from Experiment e
            join fetch e.hypothesisRef
            left join fetch e.journeyTemplate
            where e.emailsToGenerate is not null
              and e.emailsToGenerate > 0
            """)
    List<Experiment> findAllToGenerateEmails();

    @Query("""
            select distinct e from Experiment e
            join fetch e.niche n
            join fetch e.hypothesisRef h
            where e.platform = :platform
              and e.status in :statuses
              and e.creativeApproved = true
              and exists (
                    select 1 from Audience a
                    where a.niche = e.niche
                      and a.approved = true
                      and (a.hypothesis is null or a.hypothesis = e.hypothesisRef)
              )
            """)
    List<Experiment> findAllReadyForAdSets(@Param("platform") ExperimentPlatform platform,
                                           @Param("statuses") List<ExperimentStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Experiment e set e.facebookPage = null where e.facebookPage.id = :facebookPageId")
    int clearFacebookPageById(@Param("facebookPageId") Long facebookPageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Experiment e set e.facebookInstantForm = null where e.facebookInstantForm.id = :instantFormId")
    int clearFacebookInstantFormById(@Param("instantFormId") Long instantFormId);

    Optional<Experiment> findFirstByFacebookInstantForm_Id(Long facebookInstantFormId);
}
