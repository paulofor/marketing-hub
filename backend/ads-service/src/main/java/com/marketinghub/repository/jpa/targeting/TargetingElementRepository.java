package com.marketinghub.repository.jpa.targeting;

import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório principal dos elementos de segmentação.
 */
public interface TargetingElementRepository extends JpaRepository<TargetingElement, Long> {

    /**
     * Lista todos os elementos vinculados a um nicho para sincronização e revisão.
     */
    List<TargetingElement> findByNicheId(Long nicheId);

    /**
     * Busca elemento já materializado pela Meta para manter idempotência da resolução.
     */
    Optional<TargetingElement> findFirstByNicheIdAndTypeAndMetaId(Long nicheId, TargetingElementType type, String metaId);

    /**
     * Lista elementos filtrados para telas administrativas de segmentação.
     */
    @Query("""
            select e from TargetingElement e
            where (:nicheId is null or e.niche.id = :nicheId)
              and (:type is null or e.type = :type)
              and (:status is null or e.status = :status)
            order by e.createdAt desc
            """)
    List<TargetingElement> findByFilters(@Param("nicheId") Long nicheId,
                                         @Param("type") TargetingElementType type,
                                         @Param("status") TargetingElementStatus status);

    /**
     * Lista elementos aprovados para montar segmentação de um experimento.
     */
    @Query("""
            select e from TargetingElement e
            where e.niche.id = :nicheId
              and e.type = :type
              and e.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
              and (:hypothesisId is null or e.hypothesis is null or e.hypothesis.id = :hypothesisId)
            """)
    List<TargetingElement> findApprovedForExperiment(@Param("nicheId") Long nicheId,
                                                     @Param("type") TargetingElementType type,
                                                     @Param("hypothesisId") UUID hypothesisId);

    /**
     * Verifica existência de elementos aprovados para bloquear publicação incompleta.
     */
    @Query("""
            select case when count(e) > 0 then true else false end
            from TargetingElement e
            where e.niche.id = :nicheId
              and e.type = :type
              and e.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
              and (:hypothesisId is null or e.hypothesis is null or e.hypothesis.id = :hypothesisId)
            """)
    boolean existsApprovedForExperiment(@Param("nicheId") Long nicheId,
                                        @Param("type") TargetingElementType type,
                                        @Param("hypothesisId") UUID hypothesisId);

    /**
     * Lista elementos aprovados ou em revisão que ainda precisam de ID e alcance oficial da Meta Ads.
     */
    @Query("""
            select e from TargetingElement e
            where e.status in (
                com.marketinghub.targeting.TargetingElementStatus.APPROVED,
                com.marketinghub.targeting.TargetingElementStatus.NEEDS_REVIEW
              )
              and e.source in (
                com.marketinghub.targeting.TargetingElementSource.MANUAL,
                com.marketinghub.targeting.TargetingElementSource.AI,
                com.marketinghub.targeting.TargetingElementSource.OPRM_NICHE
              )
              and e.hypothesis is null
              and e.type in (
                com.marketinghub.targeting.TargetingElementType.INTEREST,
                com.marketinghub.targeting.TargetingElementType.JOB_TITLE,
                com.marketinghub.targeting.TargetingElementType.BEHAVIOR
              )
              and (e.metaIdUnavailable is null or e.metaIdUnavailable = false)
              and (e.metaId is null or e.metaAudienceSizeLowerBound is null or e.metaAudienceSizeUpperBound is null)
            order by e.updatedAt asc, e.id asc
            """)
    List<TargetingElement> findMetaAdsPending(org.springframework.data.domain.Pageable pageable);

}
