package com.marketinghub.repository.jpa.experimentdirectcontact;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.directcontact.v1.ExperimentDirectContact;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e contar contatos da amostra direta por experimento. */
public interface ExperimentDirectContactRepository
    extends JpaRepository<ExperimentDirectContact, Long> {

  /** Bloqueia o experimento durante o registro para não ultrapassar a meta em concorrência. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Experiment e where e.id = :experimentId")
  Optional<Experiment> findExperimentByIdForUpdate(@Param("experimentId") Long experimentId);

  /** Conta contatos únicos já comprovados no experimento. */
  long countByExperimentId(Long experimentId);

  /** Informa se o identificador anonimizado já pertence à amostra. */
  boolean existsByExperimentIdAndContactFingerprint(Long experimentId, String contactFingerprint);

  /** Lista a amostra em ordem de abordagem para auditoria operacional. */
  List<ExperimentDirectContact> findByExperimentIdOrderByContactedAtAscIdAsc(Long experimentId);
}
