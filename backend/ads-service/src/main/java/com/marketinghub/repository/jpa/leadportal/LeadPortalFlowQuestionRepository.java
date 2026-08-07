package com.marketinghub.repository.jpa.leadportal;

import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: carregar perguntas do Lead Portal com suas opções sem multiplicar fluxos. */
public interface LeadPortalFlowQuestionRepository
    extends JpaRepository<LeadPortalFlowQuestion, Long> {

  /** Inicializa as opções das perguntas pertencentes aos fluxos informados em consulta separada. */
  @Query(
      "select distinct question from LeadPortalFlowQuestion question "
          + "left join fetch question.options where question.flow.id in :flowIds")
  List<LeadPortalFlowQuestion> findWithOptionsByFlowIds(@Param("flowIds") Collection<Long> flowIds);
}
