package com.marketinghub.mds.repository;

import com.marketinghub.mds.MdsProcessingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MdsProcessingEventRepository extends JpaRepository<MdsProcessingEvent, Long> {
}
