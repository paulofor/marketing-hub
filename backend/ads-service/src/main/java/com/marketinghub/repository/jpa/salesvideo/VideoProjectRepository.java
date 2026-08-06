package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso aos projetos editáveis do estúdio de vídeos. */
public interface VideoProjectRepository extends JpaRepository<VideoProject, Long> {
  /** Lista os projetos do tenant mais recentes primeiro. */
  List<VideoProject> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

  /** Lista as estrategias de video vinculadas ao experimento mais recentes primeiro. */
  List<VideoProject> findByExperimentIdOrderByUpdatedAtDesc(Long experimentId);

  /** Recupera o projeto mais recente associado ao perfil de vídeo do job. */
  Optional<VideoProject> findFirstBySalesVideoProfileIdOrderByUpdatedAtDesc(Long profileId);
}
