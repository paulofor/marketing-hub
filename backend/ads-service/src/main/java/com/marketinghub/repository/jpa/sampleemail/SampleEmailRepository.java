package com.marketinghub.repository.jpa.sampleemail;

import com.marketinghub.sampleemail.SampleEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de SampleEmail.
 */
public interface SampleEmailRepository extends JpaRepository<SampleEmail, Long> {
    List<SampleEmail> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
