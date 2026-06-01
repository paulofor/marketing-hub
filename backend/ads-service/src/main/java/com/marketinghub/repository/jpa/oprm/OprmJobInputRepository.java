package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmJobInput;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmJobInput.
 */
public interface OprmJobInputRepository extends JpaRepository<OprmJobInput, Long> {
    List<OprmJobInput> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
