package com.marketinghub.repository.jpa.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Responsabilidade: consultar e persistir criativos vinculados a experimentos.
 */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    /** Lista os criativos vinculados ao experimento informado. */
    List<Creative> findByExperimentId(Long experimentId);

    /** Verifica se existe criativo do experimento no status informado. */
    boolean existsByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

    /** Verifica se existe criativo aprovado com imagem publicável no experimento. */
    @Query("""
            select case when count(c) > 0 then true else false end
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and c.imageUrl is not null
               and trim(c.imageUrl) <> ''
            """)
    boolean existsByExperimentIdAndStatusAndUsableImage(@Param("experimentId") Long experimentId,
                                                        @Param("status") CreativeStatus status);

    /** Busca um criativo carregando também o experimento vinculado. */
    @Query("select c from Creative c join fetch c.experiment where c.id = :id")
    Optional<Creative> findByIdWithExperiment(@Param("id") Long id);

    /** Conta todos os criativos vinculados ao experimento informado. */
    long countByExperimentId(Long experimentId);

    /** Conta os criativos do experimento que estão no status informado. */
    long countByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

    /** Conta criativos aprovados com imagem publicável no experimento informado. */
    @Query("""
            select count(c)
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and c.imageUrl is not null
               and trim(c.imageUrl) <> ''
            """)
    long countByExperimentIdAndStatusAndUsableImage(@Param("experimentId") Long experimentId,
                                                    @Param("status") CreativeStatus status);
}
