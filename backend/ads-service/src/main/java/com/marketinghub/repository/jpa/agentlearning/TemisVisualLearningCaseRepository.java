package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.agentlearning.v1.TemisVisualLearningCase;
import com.marketinghub.agentlearning.v1.TemisVisualLearningSourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir casos reais e selecionar amostras segregadas de Têmis. */
public interface TemisVisualLearningCaseRepository
    extends JpaRepository<TemisVisualLearningCase, Long> {
  /** Localiza a tentativa já registrada para preservar idempotência do callback. */
  Optional<TemisVisualLearningCase> findBySourceTypeAndSourceId(
      TemisVisualLearningSourceType sourceType, Long sourceId);

  /** Lista casos ainda não congelados no mesmo contexto. */
  List<TemisVisualLearningCase>
      findByContextKeyAndPlaybookVersionAndLearningRunIdIsNullOrderByIdAsc(
          String contextKey, String playbookVersion);

  /** Lista o histórico de uma versão para medir resultado real após promoção. */
  List<TemisVisualLearningCase> findByContextKeyAndPlaybookVersionOrderByIdAsc(
      String contextKey, String playbookVersion);
}
