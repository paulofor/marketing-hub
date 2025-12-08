package com.marketinghub.sampleemail.repository;

import com.marketinghub.sampleemail.SampleEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleEmailRepository extends JpaRepository<SampleEmail, Long> {
    List<SampleEmail> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
