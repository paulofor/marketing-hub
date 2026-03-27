package com.marketinghub.hypothesis.repository;

import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJob;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStatus;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HypothesisFrameworkGenerationJobRepository extends JpaRepository<HypothesisFrameworkGenerationJob, UUID> {
    boolean existsByHypothesisIdAndSectionAndStatusIn(UUID hypothesisId,
                                                      HypothesisFrameworkSection section,
                                                      Collection<HypothesisFrameworkGenerationJobStatus> statuses);

    List<HypothesisFrameworkGenerationJob> findByStatusOrderByCreatedAtAsc(HypothesisFrameworkGenerationJobStatus status,
                                                                           Pageable pageable);
}
