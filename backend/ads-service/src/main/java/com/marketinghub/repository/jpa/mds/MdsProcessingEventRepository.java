package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsEventType;
import com.marketinghub.mds.MdsProcessingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de MdsProcessingEvent.
 */
public interface MdsProcessingEventRepository extends JpaRepository<MdsProcessingEvent, Long> {
    List<MdsProcessingEvent> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    Optional<MdsProcessingEvent> findTopByRequestIdOrderByCreatedAtDescIdDesc(Long requestId);

    Optional<MdsProcessingEvent> findTopByRequestIdAndEventTypeOrderByCreatedAtDescIdDesc(Long requestId,
                                                                                          MdsEventType eventType);

    long countByRequestIdAndEventType(Long requestId, MdsEventType eventType);
}
