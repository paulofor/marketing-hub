package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for experiments.
 */

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    @Override
    @EntityGraph(attributePaths = {"facebookPage", "instagramAccount", "leadPortalFlow"})
    Optional<Experiment> findById(Long id);
    List<Experiment> findByNicheId(Long nicheId);
    boolean existsByNicheAndName(MarketNiche niche, String name);
    List<Experiment> findByStatus(ExperimentStatus status);
    List<Experiment> findByStatusAndPlatform(ExperimentStatus status, ExperimentPlatform platform);

    /**
     * Busca experimentos reprovados por 100 acessos sem envio de formulário para a mesma hipótese.
     */
    @Query("""
            select distinct e from Experiment e
            where e.hypothesisRef = :hypothesisRef
              and e.id <> :id
              and e.status = com.marketinghub.experiment.ExperimentStatus.INVALIDATED
              and exists (
                    select 1 from FacebookAdsCampaign campaign
                    where campaign.experiment = e
                      and campaign.stopReason = :stopReason
              )
            order by e.updatedAt desc
            """)
    List<Experiment> findFormZeroRuleRejectedByHypothesis(
            @Param("hypothesisRef") Hypothesis hypothesisRef,
            @Param("id") Long id,
            @Param("stopReason") FacebookCampaignStopReason stopReason);

    /**
     * Busca o experimento vinculado diretamente ao slug do fluxo interno do Lead Portal.
     */
    Optional<Experiment> findFirstByLeadPortalFlowSlug(String slug);

    /**
     * Busca o experimento publicado no Lead Portal externo pelo slug presente na URL final da landing.
     */
    @Query("""
            select e from Experiment e
            where e.followUpActionUrl like concat(concat('%/api/flows/', :slug), '/page%')
            """)
    Optional<Experiment> findFirstByFollowUpActionUrlFlowSlug(@Param("slug") String slug);

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
            left join fetch e.leadPortalFlow flow
            where e.status = :status
              and e.platform = :platform
              and e.creativeApproved = true
              and ig is not null
              and exists (
                    select 1 from TargetingElement te
                    where te.niche = e.niche
                      and te.type in (
                          com.marketinghub.targeting.TargetingElementType.INTEREST,
                          com.marketinghub.targeting.TargetingElementType.JOB_TITLE,
                          com.marketinghub.targeting.TargetingElementType.BEHAVIOR
                      )
                      and te.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
                      and te.metaId is not null
                      and te.metaId <> ''
                      and (te.hypothesis is null or te.hypothesis = e.hypothesisRef)
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
            select e from Experiment e
            join fetch e.hypothesisRef
            where e.sampleEmailsToGenerate is not null
              and e.sampleEmailsToGenerate > 0
            """)
    List<Experiment> findAllToGenerateSampleEmails();

    @Query("""
            select e from Experiment e
            join fetch e.hypothesisRef
            where e.leadPortalFlowsToGenerate is not null
              and e.leadPortalFlowsToGenerate > 0
            """)
    List<Experiment> findAllToGenerateLeadPortalFlows();

    @Query("""
            select distinct e from Experiment e
            join fetch e.niche n
            join fetch e.hypothesisRef h
            where e.platform = :platform
              and e.status in :statuses
              and e.creativeApproved = true
              and exists (
                    select 1 from TargetingElement te
                    where te.niche = e.niche
                      and te.type in (
                          com.marketinghub.targeting.TargetingElementType.INTEREST,
                          com.marketinghub.targeting.TargetingElementType.JOB_TITLE,
                          com.marketinghub.targeting.TargetingElementType.BEHAVIOR
                      )
                      and te.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
                      and te.metaId is not null
                      and te.metaId <> ''
                      and (te.hypothesis is null or te.hypothesis = e.hypothesisRef)
              )
            """)
    List<Experiment> findAllReadyForAdSets(@Param("platform") ExperimentPlatform platform,
                                           @Param("statuses") List<ExperimentStatus> statuses);

    /**
     * Busca um experimento específico com público Meta publicável sem depender do status operacional atual.
     */
    @Query("""
            select distinct e from Experiment e
            join fetch e.niche n
            join fetch e.hypothesisRef h
            where e.id = :experimentId
              and e.platform = :platform
              and e.creativeApproved = true
              and exists (
                    select 1 from TargetingElement te
                    where te.niche = e.niche
                      and te.type in (
                          com.marketinghub.targeting.TargetingElementType.INTEREST,
                          com.marketinghub.targeting.TargetingElementType.JOB_TITLE,
                          com.marketinghub.targeting.TargetingElementType.BEHAVIOR
                      )
                      and te.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
                      and te.metaId is not null
                      and te.metaId <> ''
                      and (te.hypothesis is null or te.hypothesis = e.hypothesisRef)
              )
            """)
    Optional<Experiment> findForAdSetTargetingById(@Param("experimentId") Long experimentId,
                                                   @Param("platform") ExperimentPlatform platform);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Experiment e set e.facebookPage = null where e.facebookPage.id = :facebookPageId")
    int clearFacebookPageById(@Param("facebookPageId") Long facebookPageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Experiment e set e.facebookInstantForm = null where e.facebookInstantForm.id = :instantFormId")
    int clearFacebookInstantFormById(@Param("instantFormId") Long instantFormId);

    @Modifying
    @Query("""
            update Experiment e
            set e.totalCost = coalesce(e.totalCost, 0) + :delta
            where e.id = :id
            """)
    void incrementTotalCost(@Param("id") Long id, @Param("delta") BigDecimal delta);

    Optional<Experiment> findFirstByFacebookInstantForm_Id(Long facebookInstantFormId);
}
