package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.agentlearning.v1.ApolloLearningObservation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar observações sombra usadas no aprendizado governado de Apolo. */
public interface ApolloLearningObservationRepository
    extends JpaRepository<ApolloLearningObservation, Long> {
  /** Evita registrar duas vezes o mesmo job. */
  Optional<ApolloLearningObservation> findByJobId(Long jobId);

  /** Recupera a amostra homogênea que pode formar um experimento. */
  List<ApolloLearningObservation> findByScopeIdAndBaselineVersionAndCandidateVersionOrderByIdAsc(
      String scopeId, String baselineVersion, String candidateVersion);
}
