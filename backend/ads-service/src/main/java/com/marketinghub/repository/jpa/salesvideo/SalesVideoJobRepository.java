package com.marketinghub.repository.jpa.salesvideo;

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

    /** Lista jobs de um perfil de vídeo do mais recente para o mais antigo. */
    List<SalesVideoJob> findByProfileIdOrderByRequestedAtDesc(Long profileId);

    /** Busca o job mais recente de um perfil de vídeo. */
    Optional<SalesVideoJob> findFirstByProfileIdOrderByRequestedAtDesc(Long profileId);

    /** Verifica se algum job usa o asset informado em qualquer papel de mídia. */
    @Query("select case when count(j) > 0 then true else false end from SalesVideoJob j " +
            "where (j.asset.id = :assetId or j.posterAsset.id = :assetId or j.vttAsset.id = :assetId)")
    boolean existsByAnyAssetReference(@Param("assetId") Long assetId);

    /** Lista jobs falhos ou pendentes por status e data de corte operacional. */
    List<SalesVideoJob> findByStatusAndFinishedAtBefore(SalesVideoStatus status, Instant finishedAt);

    /** Lista jobs pendentes antigos ainda não iniciados. */
    List<SalesVideoJob> findByStatusAndRequestedAtBeforeAndStartedAtIsNull(SalesVideoStatus status, Instant requestedAt);

    /** Verifica se um job já originou outro job de retry. */
    boolean existsByRetryOfJob_Id(Long jobId);

}
