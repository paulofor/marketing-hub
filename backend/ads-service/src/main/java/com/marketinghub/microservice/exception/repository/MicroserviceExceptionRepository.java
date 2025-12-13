package com.marketinghub.microservice.exception.repository;

import com.marketinghub.microservice.exception.MicroserviceExceptionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MicroserviceExceptionRepository extends JpaRepository<MicroserviceExceptionLog, Long> {
    Page<MicroserviceExceptionLog> findAllByMicroserviceId(Long microserviceId, Pageable pageable);

    Page<MicroserviceExceptionLog> findAllByMicroserviceIdAndSeverityIgnoreCase(Long microserviceId, String severity, Pageable pageable);

    Page<MicroserviceExceptionLog> findAllBySeverityIgnoreCase(String severity, Pageable pageable);

    Optional<MicroserviceExceptionLog> findFirstByMicroserviceIdOrderByOccurredAtDesc(Long microserviceId);

    long countByMicroserviceId(Long microserviceId);

    @Query("""
            SELECT e FROM MicroserviceExceptionLog e
            WHERE e.microservice.id IN :microserviceIds
              AND e.occurredAt = (
                    SELECT MAX(innerLog.occurredAt)
                    FROM MicroserviceExceptionLog innerLog
                    WHERE innerLog.microservice.id = e.microservice.id)
            """)
    List<MicroserviceExceptionLog> findLatestByMicroserviceIds(@Param("microserviceIds") List<Long> microserviceIds);

    @Query("""
            SELECT e.microservice.id AS microserviceId, COUNT(e) AS total
            FROM MicroserviceExceptionLog e
            WHERE e.microservice.id IN :microserviceIds
            GROUP BY e.microservice.id
            """)
    List<MicroserviceExceptionCountView> countByMicroserviceIds(@Param("microserviceIds") List<Long> microserviceIds);

    interface MicroserviceExceptionCountView {
        Long getMicroserviceId();
        Long getTotal();
    }
}
