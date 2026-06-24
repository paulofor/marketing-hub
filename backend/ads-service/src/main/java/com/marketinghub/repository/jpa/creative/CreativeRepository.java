package com.marketinghub.repository.jpa.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for creatives.
 */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    /** Lista os criativos vinculados ao experimento informado. */
    List<Creative> findByExperimentId(Long experimentId);

    /** Verifica se existe criativo do experimento no status informado. */
    boolean existsByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

    /** Busca um criativo carregando também o experimento vinculado. */
    @Query("select c from Creative c join fetch c.experiment where c.id = :id")
    Optional<Creative> findByIdWithExperiment(@Param("id") Long id);

    /** Conta todos os criativos vinculados ao experimento informado. */
    long countByExperimentId(Long experimentId);

    /** Conta os criativos do experimento que estão no status informado. */
    long countByExperimentIdAndStatus(Long experimentId, CreativeStatus status);
}
