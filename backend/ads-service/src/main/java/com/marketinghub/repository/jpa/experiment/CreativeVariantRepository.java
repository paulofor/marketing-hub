package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.CreativeVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for creative variants. */
public interface CreativeVariantRepository extends JpaRepository<CreativeVariant, Long> {
  List<CreativeVariant> findByExperimentId(Long experimentId);
}
