package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmJobInput;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmJobInputRepository extends JpaRepository<OprmJobInput, Long> {
    List<OprmJobInput> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
