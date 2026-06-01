package com.marketinghub.experiment.frameworkimage.repository;

import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsável por consultar e remover jobs de geração de imagem do framework por experimento e status operacional. */
public interface FrameworkImageGenerationJobRepository extends JpaRepository<FrameworkImageGenerationJob, UUID> {
    /** Lista jobs por status em ordem de criação para consumo pelos workers. */
    List<FrameworkImageGenerationJob> findByStatusOrderByCreatedAtAsc(FrameworkImageGenerationJobStatus status,
                                                                      Pageable pageable);

    /** Busca o job ativo mais recente para um item planejado de um experimento. */
    Optional<FrameworkImageGenerationJob> findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(
            Long experimentId,
            String planningItemKey,
            Collection<FrameworkImageGenerationJobStatus> statuses);

    /** Lista todos os jobs de um experimento do mais recente para o mais antigo. */
    List<FrameworkImageGenerationJob> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    /** Lista jobs de itens planejados específicos por experimento e status. */
    List<FrameworkImageGenerationJob> findByExperimentIdAndPlanningItemKeyInAndStatusOrderByCreatedAtDesc(
            Long experimentId,
            Collection<String> planningItemKeys,
            FrameworkImageGenerationJobStatus status);

    /** Lista assets concluídos que ainda precisam receber URL web definitiva. */
    List<FrameworkImageGenerationJob> findByStatusAndStageInAndAssetIdIsNotNullAndSourceUrlIsNotNullAndWebUrlIsNullOrderByUpdatedAtAsc(
            FrameworkImageGenerationJobStatus status,
            Collection<FrameworkImageGenerationJobStage> stages,
            Pageable pageable);

    /** Busca o job mais recente associado a um asset gerado. */
    Optional<FrameworkImageGenerationJob> findFirstByAssetIdOrderByCreatedAtDesc(Long assetId);

    /** Lista jobs em processamento iniciados antes de uma data para marcação de timeout. */
    List<FrameworkImageGenerationJob> findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            FrameworkImageGenerationJobStatus status,
            java.time.Instant startedAt,
            Pageable pageable);

    /** Remove todos os jobs de geração de imagens vinculados a um experimento. */
    @Modifying
    @Query("delete from FrameworkImageGenerationJob job where job.experiment.id = :experimentId")
    void deleteByExperimentId(@Param("experimentId") Long experimentId);
}
