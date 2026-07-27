package com.marketinghub.repository.jpa.sampleemail;

import com.marketinghub.sampleemail.SampleEmail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de SampleEmail. */
public interface SampleEmailRepository extends JpaRepository<SampleEmail, Long> {
  List<SampleEmail> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
