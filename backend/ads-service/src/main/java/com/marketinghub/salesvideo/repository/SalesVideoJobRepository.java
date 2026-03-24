package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repositório dos jobs do módulo de vídeos.
 */
public interface SalesVideoJobRepository extends JpaRepository<SalesVideoJob, Long>,
        JpaSpecificationExecutor<SalesVideoJob> {

    Optional<SalesVideoJob> findFirstByProfileIdOrderByRequestedAtDesc(Long profileId);

}
