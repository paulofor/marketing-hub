package com.marketinghub.repository.jpa.systemimprovement;

import com.marketinghub.systemimprovement.SystemImprovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e ordenar as melhorias sugeridas pelos agentes. */
public interface SystemImprovementRepository extends JpaRepository<SystemImprovement, Long> {

  /** Lista as sugestões mais recentes primeiro para priorização administrativa. */
  List<SystemImprovement> findAllByOrderByRequestedAtDescIdDesc();
}
