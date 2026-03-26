package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositório dos jobs do módulo de vídeos.
 */
public interface SalesVideoJobRepository extends JpaRepository<SalesVideoJob, Long>,
        JpaSpecificationExecutor<SalesVideoJob> {

    List<SalesVideoJob> findByProfileIdOrderByRequestedAtDesc(Long profileId);

    Optional<SalesVideoJob> findFirstByProfileIdOrderByRequestedAtDesc(Long profileId);

    @Query("select case when count(j) > 0 then true else false end from SalesVideoJob j " +
            "where (j.asset.id = :assetId or j.posterAsset.id = :assetId or j.vttAsset.id = :assetId)")
    boolean existsByAnyAssetReference(@Param("assetId") Long assetId);

    List<SalesVideoJob> findByStatusAndFinishedAtBefore(SalesVideoStatus status, Instant finishedAt);

    List<SalesVideoJob> findByStatusAndRequestedAtBeforeAndStartedAtIsNull(SalesVideoStatus status, Instant requestedAt);

}
