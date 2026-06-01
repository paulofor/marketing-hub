package com.marketinghub.repository.jpa.hypothesis;

import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJob;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStatus;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de HypothesisFrameworkGenerationJob.
 */
public interface HypothesisFrameworkGenerationJobRepository extends JpaRepository<HypothesisFrameworkGenerationJob, UUID> {
    List<HypothesisFrameworkGenerationJob> findByHypothesisIdAndSectionAndStatusInOrderByCreatedAtDesc(
            UUID hypothesisId,
            HypothesisFrameworkSection section,
            Collection<HypothesisFrameworkGenerationJobStatus> statuses);

    List<HypothesisFrameworkGenerationJob> findByStatusOrderByCreatedAtAsc(HypothesisFrameworkGenerationJobStatus status,
                                                                           Pageable pageable);

    List<HypothesisFrameworkGenerationJob> findByHypothesisIdOrderByCreatedAtDesc(UUID hypothesisId, Pageable pageable);
}
