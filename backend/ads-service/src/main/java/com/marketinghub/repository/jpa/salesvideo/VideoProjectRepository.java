package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso aos projetos editáveis do estúdio de vídeos. */
public interface VideoProjectRepository extends JpaRepository<VideoProject, Long> {
  /** Lista os projetos do tenant mais recentes primeiro. */
  List<VideoProject> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

  /** Lista as estrategias de video vinculadas ao experimento mais recentes primeiro. */
  List<VideoProject> findByExperimentIdOrderByUpdatedAtDesc(Long experimentId);
}
